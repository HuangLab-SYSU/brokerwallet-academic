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
    private Uri selectedAvatarUri; // 只存选择的图片Uri，不立即上传
    private String finalAvatarUrl = null; // 最终要提交的头像URL
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

    // 打开相册选择头像
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
                // 选完图直接跳裁剪
                startImageCrop(uri);
            }
        }
    }

    // 点击 Save
    private void saveProfile() {
        String username = etUsername.getText().toString().trim();

        // 校验用户名
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, R.string.edit_profile_toast_empty_username, Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        // 如果没有选择新头像 → 直接更新
        if (selectedAvatarUri == null) {
            updateUserInfo(username, finalAvatarUrl);
            return;
        }

        // 如果选择了新头像 → 先上传，再更新
        uploadAvatarThenUpdate(username);
    }

    // 上传头像 → 成功后更新用户信息
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
                            // 上传成功 → 提交用户名 + 头像
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

    // 最终提交：更新用户名 + 头像到服务器
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

    // 启动裁剪（强制1:1正方形）
    private void startImageCrop(Uri sourceUri) {
        try {
            // 1. 从Uri获取Bitmap
            Bitmap original = MediaStore.Images.Media.getBitmap(getContentResolver(), sourceUri);

            // 2. 中心裁剪成正方形
            int width = original.getWidth();
            int height = original.getHeight();
            int size = Math.min(width, height);
            int x = (width - size) / 2;
            int y = (height - size) / 2;
            Bitmap cropped = Bitmap.createBitmap(original, x, y, size, size);

            // 3. 压缩Bitmap为JPEG
            File tempFile = new File(getCacheDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile);
            cropped.compress(Bitmap.CompressFormat.JPEG, 90, out); // 压缩质量90
            out.flush();
            out.close();

            // 4. 更新UI显示
            Glide.with(this)
                    .load(tempFile)
                    .into(ivAvatar);

            Toast.makeText(this, R.string.edit_profile_toast_avatar_processed, Toast.LENGTH_SHORT).show();

            // 5. 设置为待上传Uri
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
                    startImageCrop(uri); // 直接裁剪
                }
            }
    );
}
