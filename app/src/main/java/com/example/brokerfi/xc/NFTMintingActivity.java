package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class NFTMintingActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    
    private EditText nftNameEditText;
    private EditText nftDescriptionEditText;
    private Spinner imageTypeSpinner;
    private Button selectImageButton;
    private Button mintButton;
    private TextView selectedImageText;
    private ImageView previewImageView;
    
    private Uri selectedImageUri;
    private String selectedImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nft_minting);

        intView();
        intEvent();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        
        nftNameEditText = findViewById(R.id.nftNameEditText);
        nftDescriptionEditText = findViewById(R.id.nftDescriptionEditText);
        imageTypeSpinner = findViewById(R.id.imageTypeSpinner);
        selectImageButton = findViewById(R.id.selectImageButton);
        mintButton = findViewById(R.id.mintButton);
        selectedImageText = findViewById(R.id.selectedImageText);
        previewImageView = findViewById(R.id.previewImageView);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
        
        selectImageButton.setOnClickListener(v -> selectImage());
        mintButton.setOnClickListener(v -> mintNFT());
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), 1002);
    }

    private void mintNFT() {
        String nftName = nftNameEditText.getText().toString().trim();
        String nftDescription = nftDescriptionEditText.getText().toString().trim();
        String imageType = imageTypeSpinner.getSelectedItem().toString();
        
        if (nftName.isEmpty()) {
            Toast.makeText(this, "请输入NFT名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (nftDescription.isEmpty()) {
            Toast.makeText(this, "请输入NFT描述", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if ("自定义图片".equals(imageType) && selectedImageUri == null) {
            Toast.makeText(this, "请选择图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示铸造中状态
        mintButton.setEnabled(false);
        mintButton.setText("铸造中...");
        
        new Thread(() -> {
            try {
                // 调用NFT铸造API
                String result = NFTApiUtil.mintNFT(
                    nftName,
                    nftDescription,
                    imageType,
                    selectedImagePath
                );
                
                runOnUiThread(() -> {
                    mintButton.setEnabled(true);
                    mintButton.setText("铸造NFT");
                    
                    if (result != null && result.contains("success")) {
                        Toast.makeText(this, "NFT铸造成功！", Toast.LENGTH_LONG).show();
                        clearForm();
                    } else {
                        Toast.makeText(this, "铸造失败: " + result, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e("NFTMinting", "铸造失败", e);
                runOnUiThread(() -> {
                    mintButton.setEnabled(true);
                    mintButton.setText("铸造NFT");
                    Toast.makeText(this, "铸造失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void clearForm() {
        nftNameEditText.setText("");
        nftDescriptionEditText.setText("");
        selectedImageText.setText("未选择图片");
        previewImageView.setImageResource(android.R.color.transparent);
        selectedImageUri = null;
        selectedImagePath = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1002 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    // 复制图片到应用内部存储
                    selectedImagePath = copyImageToInternalStorage(selectedImageUri);
                    selectedImageText.setText("已选择图片: " + getImageName(selectedImageUri));
                    
                    // 显示预览
                    previewImageView.setImageURI(selectedImageUri);
                } catch (Exception e) {
                    Log.e("NFTMinting", "图片处理失败", e);
                    Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
        
        IntentResult intentResult = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, data
        );
        if (intentResult.getContents() != null) {
            String scannedData = intentResult.getContents();
            Intent intent = new Intent(this, SendActivity.class);
            intent.putExtra("scannedData", scannedData);
            startActivity(intent);
        }
    }

    private String copyImageToInternalStorage(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File file = new File(getFilesDir(), "nft_image_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream outputStream = new FileOutputStream(file);
        
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        
        inputStream.close();
        outputStream.close();
        
        return file.getAbsolutePath();
    }

    private String getImageName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}




