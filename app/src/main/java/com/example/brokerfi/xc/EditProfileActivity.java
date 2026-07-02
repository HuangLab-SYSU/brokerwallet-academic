package com.example.brokerfi.xc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.brokerfi.R;
import com.example.brokerfi.xc.api.CosApi;
import com.example.brokerfi.xc.api.ProfileApi;
import com.example.brokerfi.xc.dto.CosCredentialDTO;
import com.example.brokerfi.xc.dto.ProfileHeaderDTO;
import com.example.brokerfi.xc.manager.CosServiceFactory;
import com.example.brokerfi.xc.manager.CosUploadManager;
import com.example.brokerfi.xc.manager.UploadCallback;
import com.example.brokerfi.core.network.ApiCallback;
import com.tencent.cos.xml.CosXmlService;

import java.io.File;
import java.util.List;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etUsername;
    private ImageView ivAvatar;
    private Button btnSave;

    private Long userId;
    private Uri selectedAvatarUri; // Only the selected image URI is saved and is not uploaded immediately.
    private String finalAvatarUrl = null; // The final avatar URL to be submitted
    private static final int REQUEST_AVATAR = 3001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initView();
        initListener();
        loadDataFromIntent();
    }

    private void initView() {
        etUsername = findViewById(R.id.et_username);
        ivAvatar = findViewById(R.id.iv_avatar);
        btnSave = findViewById(R.id.btn_save);
    }

    private void initListener() {
        ivAvatar.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    // Open photo album and select avatar
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        galleryLauncher.launch("image/*");
    }

    private void loadDataFromIntent() {
        String username = getIntent().getStringExtra("userName");
        etUsername.setText(username);
        userId = getIntent().getLongExtra("userId", -1);
        finalAvatarUrl = getIntent().getStringExtra("avatarUrl");

        if (!TextUtils.isEmpty(finalAvatarUrl)) {
            ivAvatar.setImageURI(Uri.parse(finalAvatarUrl));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQUEST_AVATAR) {
            Uri uri = data.getData();
            if (uri != null) {
                // After selecting the image, jump directly to cropping.
                startImageCrop(uri);
            }
        }
    }

    // Click Save
    private void saveProfile() {
        String username = etUsername.getText().toString().trim();

        //Verify username
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, R.string.edit_profile_toast_empty_username, Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        // If no new avatar is selected → update directly.
        if (selectedAvatarUri == null) {
            updateUserInfo(username, finalAvatarUrl);
            return;
        }

        // If a new avatar is selected → upload first, then update.
        uploadAvatarThenUpdate(username);
    }

    // Upload avatar → Update user information after success.
    private void uploadAvatarThenUpdate(String username) {
        runOnUiThread(() -> Toast.makeText(this, R.string.edit_profile_toast_uploading_avatar, Toast.LENGTH_SHORT).show());

        new CosApi().getTempCredential(new ApiCallback<CosCredentialDTO>() {
            @Override
            public void onSuccess(CosCredentialDTO data) {
                CosXmlService cosService = CosServiceFactory.create(
                        EditProfileActivity.this,
                        data.getTmpSecretId(),
                        data.getTmpSecretKey(),
                        data.getSessionToken(),
                        data.getExpiredTime()
                );

                CosUploadManager uploadManager = new CosUploadManager(
                        EditProfileActivity.this, cosService);

                uploadManager.uploadSingle(selectedAvatarUri, new UploadCallback() {
                    @Override
                    public void onSuccess(List<String> urls) {
                        if (!urls.isEmpty()) {
                            finalAvatarUrl = urls.get(0);
                            // Upload successful → Submit username + avatar.
                            Log.d("COS_DEBUG", "图片上传成功：" + finalAvatarUrl);
                            updateUserInfo(username, finalAvatarUrl);
                        } else {
                            onFail(getString(R.string.edit_profile_toast_avatar_failed));
                        }
                    }

                    @Override
                    public void onFail(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(EditProfileActivity.this, EditProfileActivity.this.getString(R.string.edit_profile_toast_upload_failed) + error, Toast.LENGTH_SHORT).show();
                            btnSave.setEnabled(true);
                        });
                    }
                });
            }

            @Override
            public void onFail(String msg) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, EditProfileActivity.this.getString(R.string.edit_profile_toast_failed_to_get_key) + msg, Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
            }
        });
    }

    // Final submission: update username + avatar to server.
    private void updateUserInfo(String username, String avatarUrl) {
        new ProfileApi().updateProfile(userId, username, avatarUrl, new ApiCallback<ProfileHeaderDTO>() {
            @Override
            public void onSuccess(ProfileHeaderDTO data) {
                runOnUiThread(() -> {
                    Log.d("COS_DEBUG", "图片已入库");
                    Toast.makeText(EditProfileActivity.this, R.string.edit_profile_toast_save_successful, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onFail(String errorMsg) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, EditProfileActivity.this.getString(R.string.edit_profile_toast_save_failed) + errorMsg, Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
            }
        });
    }

    // Start cropping (force 1:1 square)
    private void startImageCrop(Uri sourceUri) {
        try {
            // 1. Get the Bitmap from the Uri.
            Bitmap original = MediaStore.Images.Media.getBitmap(getContentResolver(), sourceUri);

            // 2. Cut the center into a square.
            int width = original.getWidth();
            int height = original.getHeight();
            int size = Math.min(width, height);
            int x = (width - size) / 2;
            int y = (height - size) / 2;
            Bitmap cropped = Bitmap.createBitmap(original, x, y, size, size);

            // 3. Compress the Bitmap as JPEG.
            File tempFile = new File(getCacheDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile);
            cropped.compress(Bitmap.CompressFormat.JPEG, 90, out); // Compression quality: 90
            out.flush();
            out.close();

            // 4. Update the UI display.
            Glide.with(this)
                    .load(tempFile)
                    .into(ivAvatar);

            Toast.makeText(this, R.string.edit_profile_toast_avatar_processed, Toast.LENGTH_SHORT).show();

            // 5. Set as the Uri to be uploaded.
            selectedAvatarUri = Uri.fromFile(tempFile);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.edit_profile_toast_avatar_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    startImageCrop(uri); // Crop directly
                }
            }
    );
}
