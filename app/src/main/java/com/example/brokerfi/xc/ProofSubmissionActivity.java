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

public class ProofSubmissionActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    
    private EditText authorInfoEditText;
    private Spinner eventTypeSpinner;
    private EditText eventDescriptionEditText;
    private Spinner contributionLevelSpinner;
    private Button selectFileButton;
    private Button submitButton;
    private TextView selectedFileText;
    private TextView nftMintButton;
    
    private Uri selectedFileUri;
    private String selectedFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proof_submission);

        intView();
        intEvent();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        
        authorInfoEditText = findViewById(R.id.authorInfoEditText);
        eventTypeSpinner = findViewById(R.id.eventTypeSpinner);
        eventDescriptionEditText = findViewById(R.id.eventDescriptionEditText);
        contributionLevelSpinner = findViewById(R.id.contributionLevelSpinner);
        selectFileButton = findViewById(R.id.selectFileButton);
        submitButton = findViewById(R.id.submitButton);
        selectedFileText = findViewById(R.id.selectedFileText);
        nftMintButton = findViewById(R.id.nftMintButton);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
        
        selectFileButton.setOnClickListener(v -> selectFile());
        submitButton.setOnClickListener(v -> submitProof());
        nftMintButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NFTMintingActivity.class);
            startActivity(intent);
        });
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择文件"), 1001);
    }

    private void submitProof() {
        String authorInfo = authorInfoEditText.getText().toString().trim();
        String eventDescription = eventDescriptionEditText.getText().toString().trim();
        
        if (authorInfo.isEmpty()) {
            Toast.makeText(this, "请输入作者信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (eventDescription.isEmpty()) {
            Toast.makeText(this, "请输入事件描述", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedFileUri == null) {
            Toast.makeText(this, "请选择证明文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示提交中状态
        submitButton.setEnabled(false);
        submitButton.setText("提交中...");
        
        new Thread(() -> {
            try {
                // 上传文件到服务器
                String result = ProofApiUtil.submitProof(
                    authorInfo,
                    eventTypeSpinner.getSelectedItem().toString(),
                    eventDescription,
                    contributionLevelSpinner.getSelectedItem().toString(),
                    selectedFilePath
                );
                
                runOnUiThread(() -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("提交证明材料");
                    
                    if (result != null && result.contains("success")) {
                        Toast.makeText(this, "证明材料提交成功！", Toast.LENGTH_LONG).show();
                        clearForm();
                    } else {
                        Toast.makeText(this, "提交失败: " + result, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e("ProofSubmission", "提交失败", e);
                runOnUiThread(() -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("提交证明材料");
                    Toast.makeText(this, "提交失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void clearForm() {
        authorInfoEditText.setText("");
        eventDescriptionEditText.setText("");
        selectedFileText.setText("未选择文件");
        selectedFileUri = null;
        selectedFilePath = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                try {
                    // 复制文件到应用内部存储
                    selectedFilePath = copyFileToInternalStorage(selectedFileUri);
                    selectedFileText.setText("已选择文件: " + getFileName(selectedFileUri));
                } catch (Exception e) {
                    Log.e("ProofSubmission", "文件处理失败", e);
                    Toast.makeText(this, "文件处理失败", Toast.LENGTH_SHORT).show();
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

    private String copyFileToInternalStorage(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File file = new File(getFilesDir(), "proof_" + System.currentTimeMillis() + ".tmp");
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

    private String getFileName(Uri uri) {
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




