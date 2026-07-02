package com.example.brokerfi.proof;

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
import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import com.example.brokerfi.core.security.SecurityUtil;
import com.example.brokerfi.core.storage.StorageUtil;
import com.example.brokerfi.nft.NFTMintingActivity;
import com.example.brokerfi.send.SendActivity;


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
    private String selectedFileName;  // Save original file name

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
        startActivityForResult(Intent.createChooser(intent, getString(R.string.proof_submission_chooser_select_file)), 1001);
    }

    private void submitProof() {
        String displayName = displayNameEditText.getText().toString().trim();
        String representativeWork = representativeWorkEditText.getText().toString().trim();
        boolean showRepresentativeWork = showRepresentativeWorkSwitch.isChecked();
        String authorInfo = authorInfoEditText.getText().toString().trim();
        String eventDescription = eventDescriptionEditText.getText().toString().trim();

        // Only verify the original required fields
        if (authorInfo.isEmpty()) {
            Toast.makeText(this, R.string.proof_submission_toast_enter_author, Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventDescription.isEmpty()) {
            Toast.makeText(this, R.string.proof_submission_toast_enter_description, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFileUri == null) {
            Toast.makeText(this, R.string.proof_submission_toast_select_file, Toast.LENGTH_SHORT).show();
            return;
        }

        //Show submission status
        submitButton.setEnabled(false);
        submitButton.setText(R.string.proof_submission_submitting);

        new Thread(() -> {
            try {
                String myAddress = getMyAddress();

                // Submit using the new back-end API. Name, masterpiece, and display settings are all optional.
                String result = ProofUploadUtil.uploadProofWithUserInfo(
                    selectedFilePath,
                    selectedFileName, // Pass the original file name
                    myAddress,
                    displayName,
                    representativeWork,
                    showRepresentativeWork
                );

                runOnUiThread(() -> {
                    submitButton.setEnabled(true);
                    submitButton.setText(R.string.activity_proof_submission_submit_materials);

                    if (result != null && result.contains("success")) {
                        Toast.makeText(this, R.string.proof_submission_toast_success, Toast.LENGTH_LONG).show();
                        clearForm();
                        // Reload user information and restore fields such as name, masterpiece, etc.
                        loadUserInfo();
                    } else {
                        Toast.makeText(this, getString(R.string.proof_submission_toast_failed_prefix) + " " + result, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e("ProofSubmission", "提交失败", e);
                runOnUiThread(() -> {
                    submitButton.setEnabled(true);
                    submitButton.setText(R.string.activity_proof_submission_submit_materials);
                    Toast.makeText(this, getString(R.string.proof_submission_toast_failed_prefix) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void clearForm() {
        // Do not clear the name, masterpiece and display settings, because these are the user's persistent information.
        authorInfoEditText.setText("");
        eventDescriptionEditText.setText("");
        selectedFileText.setText(R.string.activity_proof_submission_no_file);
        selectedFileUri = null;
        selectedFilePath = null;
        selectedFileName = null;
    }

    /**
     * Load user information (name, masterpieces, whether to display masterpieces)
     */
    private void loadUserInfo() {
        new Thread(() -> {
            try {
                String myAddress = getMyAddress();
                Log.d("ProofSubmission", "==== 开始加载用户信息 ====");
                Log.d("ProofSubmission", "当前地址: " + myAddress);

                // Check if the address is valid
                if (myAddress == null || myAddress.equals("0000000000000000000000000000000000000000")) {
                    Log.e("ProofSubmission", "地址无效，跳过加载用户信息");
                    return;
                }

                // Build API request URL - configured using ServerConfig.
                String apiUrl = ApiConfig.API_USER_INFO + "/" + myAddress;
                Log.d("ProofSubmission", "请求URL: " + apiUrl);
                Log.d("ProofSubmission", "BASE_URL: " + ApiConfig.BASE_URL);

                // Send HTTP GET request
                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                Log.d("ProofSubmission", "开始连接...");
                int responseCode = connection.getResponseCode();
                Log.d("ProofSubmission", "响应码: " + responseCode);

                if (responseCode == 200) {
                    // Read response
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse the JSON response
                    org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                    Log.d("ProofSubmission", "用户信息响应: " + response.toString());

                    if (jsonResponse.optBoolean("success", false)) {
                        org.json.JSONObject data = jsonResponse.optJSONObject("data");
                        if (data != null) {
                            String displayName = data.optString("displayName", "");
                            String representativeWork = data.optString("representativeWork", "");
                            boolean showRepresentativeWork = data.optBoolean("showRepresentativeWork", false);

                            // Update the interface in the UI thread
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

                    // Read error response
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
                    Toast.makeText(this, R.string.proof_submission_toast_network_failed, Toast.LENGTH_SHORT).show();
                });
            } catch (java.net.SocketTimeoutException e) {
                Log.e("ProofSubmission", "连接超时: 服务器响应超时", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.proof_and_nft_toast_response_timeout, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("ProofSubmission", "加载用户信息异常: " + e.getClass().getName() + " - " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.proof_submission_toast_user_info_failed) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
            Log.d("ProofSubmission", "==== 用户信息加载流程结束 ====");
        }).start();
    }

    /**
     * Get current user address / 获取当前用户地址
     */
    private String getMyAddress() {
        try {
            String privateKey = StorageUtil.getCurrentPrivatekey(this);
            if (privateKey != null) {
                String address = SecurityUtil.GetAddress(privateKey);
                // Make sure the address has a 0x prefix.
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
                    // Get the original file name
                    selectedFileName = getFileName(selectedFileUri);
                    // Copy files to app internal storage
                    selectedFilePath = copyFileToInternalStorage(selectedFileUri);
                    selectedFileText.setText(selectedFileText.getContext().getString(R.string.proof_submission_file_selected) + " " + selectedFileName);
                    Log.d("ProofSubmission", "选择文件: " + selectedFileName);
                } catch (Exception e) {
                    Log.e("ProofSubmission", "文件处理失败", e);
                    Toast.makeText(this, R.string.proof_submission_toast_file_failed, Toast.LENGTH_SHORT).show();
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

        // Prioritize using ContentResolver to get the file name.
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

        // If still empty, try to get from URI path.
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

        // If it is still empty or in document:xxx format, use the default name.
        if (result == null || result.isEmpty() || result.startsWith("document:")) {
            Log.w("ProofSubmission", "无法获取有效文件名，当前值: " + result);
            result = getString(R.string.proof_submission_default_file_prefix) + System.currentTimeMillis() + ".file";
            Log.w("ProofSubmission", "使用默认名称: " + result);
        }

        Log.d("ProofSubmission", "最终文件名: " + result);
        return result;
    }

    @Override
    public void onBackPressed() {
        if (navigationHelper != null && navigationHelper.isPopupVisible()) {
            navigationHelper.hidePopup();
        } else {
            super.onBackPressed();
        }
    }// abnormal


}
