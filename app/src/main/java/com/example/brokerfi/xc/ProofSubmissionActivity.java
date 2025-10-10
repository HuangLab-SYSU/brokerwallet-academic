package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

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
import com.example.brokerfi.config.ServerConfig;
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
    
    private EditText displayNameEditText;
    private EditText representativeWorkEditText;
    private SwitchCompat showRepresentativeWorkSwitch;
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
    private String selectedFileName;  // 保存原始文件名

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proof_submission);

        intView();
        intEvent();
        loadUserInfo();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        
        displayNameEditText = findViewById(R.id.displayNameEditText);
        representativeWorkEditText = findViewById(R.id.representativeWorkEditText);
        showRepresentativeWorkSwitch = findViewById(R.id.showRepresentativeWorkSwitch);
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
        String displayName = displayNameEditText.getText().toString().trim();
        String representativeWork = representativeWorkEditText.getText().toString().trim();
        boolean showRepresentativeWork = showRepresentativeWorkSwitch.isChecked();
        String authorInfo = authorInfoEditText.getText().toString().trim();
        String eventDescription = eventDescriptionEditText.getText().toString().trim();
        
        // 只验证原有的必填项
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
                String myAddress = getMyAddress();
                
                // 使用新的后端API提交，花名、代表作、展示设置都是可选的
                String result = ProofUploadUtil.uploadProofWithUserInfo(
                    selectedFilePath,
                    selectedFileName,  // 传递原始文件名
                    myAddress,
                    displayName,
                    representativeWork,
                    showRepresentativeWork
                );
                
                runOnUiThread(() -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("提交证明材料");
                    
                    if (result != null && result.contains("success")) {
                        Toast.makeText(this, "证明材料提交成功！", Toast.LENGTH_LONG).show();
                        clearForm();
                        // 重新加载用户信息，恢复花名、代表作等字段
                        loadUserInfo();
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
        // 不清空花名、代表作和展示设置，因为这些是用户的持久信息
        authorInfoEditText.setText("");
        eventDescriptionEditText.setText("");
        selectedFileText.setText("未选择文件");
        selectedFileUri = null;
        selectedFilePath = null;
        selectedFileName = null;
    }
    
    /**
     * 加载用户信息（花名、代表作、是否展示代表作）
     */
    private void loadUserInfo() {
        new Thread(() -> {
            try {
                String myAddress = getMyAddress();
                Log.d("ProofSubmission", "==== 开始加载用户信息 ====");
                Log.d("ProofSubmission", "当前地址: " + myAddress);
                
                // 检查地址是否有效
                if (myAddress == null || myAddress.equals("0000000000000000000000000000000000000000")) {
                    Log.e("ProofSubmission", "地址无效，跳过加载用户信息");
                    return;
                }
                
                // 构建API请求URL - 使用ServerConfig配置
                String apiUrl = ServerConfig.USER_INFO_API + "/" + myAddress;
                Log.d("ProofSubmission", "请求URL: " + apiUrl);
                Log.d("ProofSubmission", "BASE_URL: " + ServerConfig.BASE_URL);
                
                // 发送HTTP GET请求
                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                Log.d("ProofSubmission", "开始连接...");
                int responseCode = connection.getResponseCode();
                Log.d("ProofSubmission", "响应码: " + responseCode);
                
                if (responseCode == 200) {
                    // 读取响应
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // 解析JSON响应
                    org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                    Log.d("ProofSubmission", "用户信息响应: " + response.toString());
                    
                    if (jsonResponse.optBoolean("success", false)) {
                        org.json.JSONObject data = jsonResponse.optJSONObject("data");
                        if (data != null) {
                            String displayName = data.optString("displayName", "");
                            String representativeWork = data.optString("representativeWork", "");
                            boolean showRepresentativeWork = data.optBoolean("showRepresentativeWork", false);
                            
                            // 在UI线程更新界面
                            runOnUiThread(() -> {
                                if (!displayName.isEmpty() && !"null".equals(displayName)) {
                                    displayNameEditText.setText(displayName);
                                    Log.d("ProofSubmission", "已填充花名: " + displayName);
                                }
                                if (!representativeWork.isEmpty() && !"null".equals(representativeWork)) {
                                    representativeWorkEditText.setText(representativeWork);
                                    Log.d("ProofSubmission", "已填充代表作: " + representativeWork);
                                }
                                showRepresentativeWorkSwitch.setChecked(showRepresentativeWork);
                                Log.d("ProofSubmission", "已填充展示设置: " + showRepresentativeWork);
                            });
                        }
                    } else {
                        Log.d("ProofSubmission", "用户信息不存在或加载失败，响应: " + response.toString());
                    }
                } else {
                    Log.e("ProofSubmission", "加载用户信息失败，响应码: " + responseCode);
                    
                    // 读取错误响应
                    try {
                        java.io.BufferedReader errorReader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(connection.getErrorStream()));
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        errorReader.close();
                        Log.e("ProofSubmission", "错误响应: " + errorResponse.toString());
                    } catch (Exception ex) {
                        Log.e("ProofSubmission", "无法读取错误响应");
                    }
                }
            } catch (java.net.ConnectException e) {
                Log.e("ProofSubmission", "连接失败: 无法连接到服务器，请检查服务器是否运行", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "无法连接到服务器，请检查网络设置", Toast.LENGTH_SHORT).show();
                });
            } catch (java.net.SocketTimeoutException e) {
                Log.e("ProofSubmission", "连接超时: 服务器响应超时", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "服务器响应超时", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("ProofSubmission", "加载用户信息异常: " + e.getClass().getName() + " - " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "加载用户信息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
            Log.d("ProofSubmission", "==== 用户信息加载流程结束 ====");
        }).start();
    }
    
    /**
     * 获取当前用户地址
     */
    private String getMyAddress() {
        try {
            String privateKey = StorageUtil.getCurrentPrivatekey(this);
            if (privateKey != null) {
                String address = SecurityUtil.GetAddress(privateKey);
                // 确保地址有0x前缀
                if (!address.startsWith("0x")) {
                    address = "0x" + address;
                }
                return address;
            } else {
                Log.e("ProofSubmission", "无法获取私钥");
                return "0000000000000000000000000000000000000000";
            }
        } catch (Exception e) {
            Log.e("ProofSubmission", "获取地址失败", e);
            return "0000000000000000000000000000000000000000";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                try {
                    // 获取原始文件名
                    selectedFileName = getFileName(selectedFileUri);
                    // 复制文件到应用内部存储
                    selectedFilePath = copyFileToInternalStorage(selectedFileUri);
                    selectedFileText.setText("已选择文件: " + selectedFileName);
                    Log.d("ProofSubmission", "选择文件: " + selectedFileName);
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
        
        Log.d("ProofSubmission", "开始获取文件名，URI: " + uri.toString());
        Log.d("ProofSubmission", "URI Scheme: " + uri.getScheme());
        
        // 优先使用ContentResolver获取文件名
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            android.database.Cursor cursor = null;
            try {
                String[] projection = {android.provider.OpenableColumns.DISPLAY_NAME};
                cursor = getContentResolver().query(uri, projection, null, null, null);
                
                if (cursor != null) {
                    Log.d("ProofSubmission", "Cursor列数: " + cursor.getColumnCount());
                    if (cursor.getColumnCount() > 0) {
                        String[] columnNames = cursor.getColumnNames();
                        Log.d("ProofSubmission", "Cursor列名: " + java.util.Arrays.toString(columnNames));
                    }
                    
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        Log.d("ProofSubmission", "DISPLAY_NAME列索引: " + nameIndex);
                        
                        if (nameIndex >= 0) {
                            result = cursor.getString(nameIndex);
                            Log.d("ProofSubmission", "从ContentResolver获取文件名: " + result);
                        } else {
                            Log.w("ProofSubmission", "DISPLAY_NAME列不存在");
                        }
                    } else {
                        Log.w("ProofSubmission", "Cursor为空");
                    }
                }
            } catch (Exception e) {
                Log.e("ProofSubmission", "从ContentResolver获取文件名失败", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        
        // 如果仍然为空，尝试从URI路径获取
        if (result == null || result.isEmpty()) {
            Log.d("ProofSubmission", "尝试从URI路径获取文件名");
            result = uri.getLastPathSegment();
            Log.d("ProofSubmission", "LastPathSegment: " + result);
            
            if (result == null || result.isEmpty()) {
                result = uri.getPath();
                Log.d("ProofSubmission", "Path: " + result);
                if (result != null) {
                    int cut = result.lastIndexOf('/');
                    if (cut != -1) {
                        result = result.substring(cut + 1);
                    }
                }
            }
        }
        
        // 如果还是为空或者是document:xxx格式，使用默认名称
        if (result == null || result.isEmpty() || result.startsWith("document:")) {
            Log.w("ProofSubmission", "无法获取有效文件名，当前值: " + result);
            result = "文件_" + System.currentTimeMillis() + ".file";
            Log.w("ProofSubmission", "使用默认名称: " + result);
        }
        
        Log.d("ProofSubmission", "最终文件名: " + result);
        return result;
    }
}




