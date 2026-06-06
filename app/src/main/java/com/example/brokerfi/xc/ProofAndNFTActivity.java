package com.example.brokerfi.xc;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.brokerfi.R;
import com.example.brokerfi.config.ApiConfig;
import com.example.brokerfi.config.ServerConfig;
import com.example.brokerfi.xc.menu.NavigationHelper;

public class ProofAndNFTActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    
    // 证明提交相关
    private TextView selectFileButton;
    private TextView selectImageButton;
    private ImageView previewImageView;
    private EditText displayNameEditText;
    private EditText representativeWorkEditText;
    private RadioGroup showRepresentativeWorkGroup;
    private RadioButton showRepresentativeWorkYes;
    private RadioButton showRepresentativeWorkNo;
    private TextView submitProofButton;
    private TextView fileHelpIcon;
    private TextView imageHelpIcon;
    private TextView fileCountHint;
    private LinearLayout selectedFilesContainer;
    private LinearLayout selectedImageContainer;
    
    // 文件选择相关
    private List<Uri> selectedFileUris;
    private Uri selectedImageUri;
    private Uri currentPhotoUri;
    private static final int REQUEST_CODE_SELECT_FILE = 1001;
    private static final int REQUEST_CODE_SELECT_IMAGE = 1002;
    private static final int REQUEST_CODE_CAMERA = 1003;
    private static final int REQUEST_CAMERA_PERMISSION = 1004;
    private static final int MAX_FILE_COUNT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proof_and_nft);

        // 初始化文件列表
        selectedFileUris = new ArrayList<>();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(ProofAndNFTActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        // 初始化OpenCV
        DocumentScannerUtil.initOpenCV(this);
        
        intView();
        intEvent();
        loadUserInfo();  // 加载用户信息（花名、代表作等）
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        
        // 证明提交相关
        selectFileButton = findViewById(R.id.selectFileButton);
        selectImageButton = findViewById(R.id.selectImageButton);
        previewImageView = findViewById(R.id.previewImageView);
        displayNameEditText = findViewById(R.id.displayNameEditText);
        representativeWorkEditText = findViewById(R.id.representativeWorkEditText);
        showRepresentativeWorkGroup = findViewById(R.id.showRepresentativeWorkGroup);
        showRepresentativeWorkYes = findViewById(R.id.showRepresentativeWorkYes);
        showRepresentativeWorkNo = findViewById(R.id.showRepresentativeWorkNo);
        submitProofButton = findViewById(R.id.submitProofButton);
        fileHelpIcon = findViewById(R.id.fileHelpIcon);
        imageHelpIcon = findViewById(R.id.imageHelpIcon);
        fileCountHint = findViewById(R.id.fileCountHint);
        selectedFilesContainer = findViewById(R.id.selectedFilesContainer);
        selectedImageContainer = findViewById(R.id.selectedImageContainer);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
        
        selectFileButton.setOnClickListener(v -> selectFile());
        
        selectImageButton.setOnClickListener(v -> selectImage());
        
        submitProofButton.setOnClickListener(v -> submitProof());
        
        fileHelpIcon.setOnClickListener(v -> showFileHelpDialog());
        
        imageHelpIcon.setOnClickListener(v -> showImageHelpDialog());
    }
    
    /**
     * 显示文件选择帮助对话框
     */
    private void showFileHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.proof_and_nft_title_file_selection_tips);
        builder.setMessage(getString(R.string.proof_and_nft_message_file_steps_intro) + "\n\n" +
                getString(R.string.proof_and_nft_message_file_step_find) + "\n" +
                getString(R.string.proof_and_nft_message_file_step_forward) + "\n" +
                getString(R.string.proof_and_nft_message_choose_save_files) + "\n" +
                getString(R.string.proof_and_nft_message_save_phone_storage) + "\n" +
                getString(R.string.proof_and_nft_message_file_step_select) + "\n\n" +
                getString(R.string.proof_and_nft_message_save_locally_tip));
        
        builder.setPositiveButton(R.string.proof_and_nft_button_got_it, (dialog, which) -> {
            dialog.dismiss();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * 选择证明文件
     */
    private void selectFile() {
        if (selectedFileUris.size() >= MAX_FILE_COUNT) {
            Toast.makeText(this, getString(R.string.proof_and_nft_toast_maximum) + " " + MAX_FILE_COUNT + " " + getString(R.string.proof_and_nft_files_allowed), Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Support all file types
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Support multiple selection
        startActivityForResult(Intent.createChooser(intent, getString(R.string.proof_and_nft_chooser_select_proof_file)), REQUEST_CODE_SELECT_FILE);
    }
    
    /**
     * 显示NFT图片帮助对话框
     */
    private void showImageHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.proof_and_nft_title_nft_image_guide);
        builder.setMessage(getString(R.string.proof_and_nft_message_optional_image) + "\n\n" +
                getString(R.string.proof_and_nft_message_upload_image_tip) + "\n" +
                getString(R.string.proof_and_nft_message_dao_image_tip) + "\n\n" +
                getString(R.string.proof_and_nft_message_choose_option_tip));
        
        builder.setPositiveButton(R.string.proof_and_nft_button_got_it, (dialog, which) -> {
            dialog.dismiss();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * 选择NFT照片 - 显示选择方式弹窗
     */
    private void selectImage() {
        if (selectedImageUri != null) {
            Toast.makeText(this, R.string.proof_and_nft_toast_one_image_limit, Toast.LENGTH_SHORT).show();
            return;
        }
        showImageSourceDialog();
    }
    
    /**
     * 显示图片来源选择对话框
     */
    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.proof_and_nft_title_select_image_source);
        builder.setMessage(R.string.proof_and_nft_message_select_source);
        
        // From gallery
        builder.setPositiveButton(R.string.proof_and_nft_button_gallery, (dialog, which) -> {
            selectImageFromGallery();
        });
        
        // Take photo (with document scanning)
        builder.setNeutralButton(R.string.proof_and_nft_button_camera_scan, (dialog, which) -> {
            checkCameraPermissionAndTakePhoto();
        });
        
        // Cancel
        builder.setNegativeButton(R.string.proof_and_nft_button_cancel, (dialog, which) -> {
            dialog.dismiss();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * 从图库选择图片
     */
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.proof_and_nft_chooser_select_nft_photo)), REQUEST_CODE_SELECT_IMAGE);
    }
    
    /**
     * 检查摄像头权限并拍照
     */
    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            // 请求摄像头权限
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            takePhoto();
        }
    }
    
    /**
     * 拍照
     */
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // 创建临时文件保存照片
            File photoFile = createImageFile();
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                        "com.example.brokerfi.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA);
            }
        } else {
            Toast.makeText(this, R.string.proof_and_nft_toast_camera_access_failed, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 创建图片文件
     */
    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "NFT_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Log.e("CreateImageFile", "Error creating image file", e);
            return null;
        }
    }
    
    /**
     * 提交证明
     */
    private void submitProof() {
        if (selectedFileUris.isEmpty()) {
            Toast.makeText(this, R.string.proof_and_nft_toast_select_proof_file, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取当前钱包地址
        String walletAddress = getCurrentWalletAddress();
        if (walletAddress == null) {
            Toast.makeText(this, R.string.proof_and_nft_toast_wallet_status, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取用户输入的个人信息
        String displayName = displayNameEditText.getText().toString().trim();
        String representativeWork = representativeWorkEditText.getText().toString().trim();
        boolean showRepresentativeWork = showRepresentativeWorkYes.isChecked();
        
        // Show loading state
        submitProofButton.setText(R.string.proof_and_nft_submitting);
        submitProofButton.setEnabled(false);
        
        Log.d("ProofSubmit", "一体化提交 - 钱包地址: " + walletAddress + ", 花名: " + displayName + ", 展示代表作: " + showRepresentativeWork);
        
        // 使用一体化提交API（多个证明文件 + NFT图片 + 用户信息一次性提交）
        SubmissionUtil.submitComplete(this, selectedFileUris, selectedImageUri, 
            walletAddress, displayName, representativeWork, showRepresentativeWork,
            new SubmissionUtil.SubmissionCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        handleSubmissionSuccess(response);
                        resetSubmitButton();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        handleSubmissionError(error);
                        resetSubmitButton();
                    });
                }
            });
    }
    
    /**
     * 更新用户个人信息
     */
    private void updateUserProfile(String displayName, String representativeWork, boolean showRepresentativeWork) {
        // 获取当前钱包地址
        String walletAddress = getCurrentWalletAddress();
        
        // 这里可以调用后端API更新用户信息
        // UserProfileUtil.updateProfile(walletAddress, displayName, representativeWork, showRepresentativeWork);
        
        Log.d("UserProfile", "更新用户信息: " + displayName + ", 代表作: " + representativeWork + ", 展示: " + showRepresentativeWork);
    }
    
    /**
     * 上传NFT图片
     */
    private void uploadNftImage() {
        if (selectedImageUri != null) {
            // Call NFT image upload API here
            Toast.makeText(this, R.string.proof_and_nft_toast_uploading_nft_image, Toast.LENGTH_SHORT).show();
            
            // Simulate upload success
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Simulate network delay
                    runOnUiThread(() -> {
                        Toast.makeText(ProofAndNFTActivity.this, R.string.proof_and_nft_toast_image_uploaded, Toast.LENGTH_SHORT).show();
                        resetSubmitButton();
                        showSuccessMessage();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    /**
     * 显示成功提示信息
     */
    private void showSuccessMessage() {
        String message = "Submission completed!\n\n";
        message += "📄 Proof files uploaded, waiting for admin review\n";
        
        if (selectedImageUri != null) {
            message += "🖼️ NFT image uploaded, waiting for admin approval to mint\n";
        }
        
        String displayName = displayNameEditText.getText().toString().trim();
        String representativeWork = representativeWorkEditText.getText().toString().trim();
        boolean showRepresentativeWork = showRepresentativeWorkYes.isChecked();
        
        if (!displayName.isEmpty() || !representativeWork.isEmpty()) {
            message += "👤 Profile updated\n";
            if (showRepresentativeWork) {
                message += "🏆 Representative work will be displayed on ranking after admin approval\n";
            }
        }
        
        message += "\nPlease wait patiently for the review result!";
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.proof_and_nft_title_submission_success);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.main_button_ok, (dialog, which) -> {
            dialog.dismiss();
            // Option to return to main page or clear form
            clearForm();
        });
        builder.show();
    }
    
    /**
     * 清空表单
     */
    private void clearForm() {
        selectedFileUris.clear();
        selectedImageUri = null;
        displayNameEditText.setText("");
        representativeWorkEditText.setText("");
        showRepresentativeWorkNo.setChecked(true);
        updateFileDisplay();
        updateImageDisplay();
    }
    
    /**
     * 获取当前钱包地址
     */
    private String getCurrentWalletAddress() {
        try {
            // 获取当前私钥
            String privateKey = StorageUtil.getCurrentPrivatekey(this);
            
            if (privateKey != null) {
                // 从私钥生成钱包地址
                return SecurityUtil.GetAddress(privateKey);
            } else {
                Log.e("WalletAddress", "Cannot get current private key");
                Toast.makeText(this, R.string.proof_and_nft_toast_wallet_failed, Toast.LENGTH_SHORT).show();
                return null;
            }
        } catch (Exception e) {
            Log.e("WalletAddress", "Failed to get wallet address", e);
            Toast.makeText(this, getString(R.string.community_toast_wallet_address_failed) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    /**
     * 重置提交按钮状态
     */
    private void resetSubmitButton() {
        submitProofButton.setText(R.string.proof_and_nft_submit);
        submitProofButton.setEnabled(true);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_SELECT_FILE && data != null) {
                handleFileSelection(data);
            } else if (requestCode == REQUEST_CODE_SELECT_IMAGE && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    handleImageSelection(uri, false);
                }
            } else if (requestCode == REQUEST_CODE_CAMERA) {
                if (currentPhotoUri != null) {
                    handleImageSelection(currentPhotoUri, true);
                }
            }
        }
    }
    
    /**
     * 处理文件选择结果
     */
    private void handleFileSelection(Intent data) {
        List<Uri> newFiles = new ArrayList<>();
        
        if (data.getClipData() != null) {
            // 多个文件选择
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                if (uri != null) {
                    newFiles.add(uri);
                }
            }
        } else if (data.getData() != null) {
            // 单个文件选择
            newFiles.add(data.getData());
        }
        
        // Check file count limit
        int totalCount = selectedFileUris.size() + newFiles.size();
        if (totalCount > MAX_FILE_COUNT) {
            int allowedCount = MAX_FILE_COUNT - selectedFileUris.size();
            Toast.makeText(this, getString(R.string.proof_and_nft_toast_maximum) + " " + MAX_FILE_COUNT + " " + getString(R.string.proof_and_nft_toast_files_remaining) + " " + allowedCount + " " + getString(R.string.proof_and_nft_toast_more),
                    Toast.LENGTH_SHORT).show();
            // Only add allowed number of files
            for (int i = 0; i < allowedCount && i < newFiles.size(); i++) {
                selectedFileUris.add(newFiles.get(i));
            }
        } else {
            selectedFileUris.addAll(newFiles);
        }
        
        updateFileDisplay();
        Toast.makeText(this, getString(R.string.proof_and_nft_toast_selected) + " " + newFiles.size() + " " + getString(R.string.proof_and_nft_file_s), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 处理图片选择结果
     * @param uri 图片URI
     * @param isFromCamera 是否来自摄像头拍照
     */
    private void handleImageSelection(Uri uri, boolean isFromCamera) {
        if (isFromCamera && DocumentScannerUtil.isOpenCVInitialized()) {
            // 拍照的图片使用OpenCV进行文档扫描优化
            processImageWithDocumentScanning(uri);
        } else {
            // 直接使用选择的图片
            setSelectedImage(uri);
        }
    }
    
    /**
     * 使用OpenCV进行文档扫描处理
     */
    private void processImageWithDocumentScanning(Uri imageUri) {
        // Show processing hint
        Toast.makeText(this, R.string.proof_and_nft_toast_optimizing_image, Toast.LENGTH_SHORT).show();
        
        // 在后台线程处理图片
        new Thread(() -> {
            try {
                Bitmap scannedBitmap = DocumentScannerUtil.scanDocument(this, imageUri);
                
                if (scannedBitmap != null) {
                    // 保存扫描后的图片
                    Uri scannedUri = saveBitmapToFile(scannedBitmap);
                    
                    runOnUiThread(() -> {
                        if (scannedUri != null) {
                            setSelectedImage(scannedUri);
                            Toast.makeText(this, R.string.proof_and_nft_toast_scan_completed, Toast.LENGTH_SHORT).show();
                        } else {
                            setSelectedImage(imageUri);
                            Toast.makeText(this, R.string.proof_and_nft_toast_scan_opt_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        setSelectedImage(imageUri);
                        Toast.makeText(this, R.string.proof_and_nft_toast_scan_opt_failed, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("DocumentScan", "Error processing image", e);
                runOnUiThread(() -> {
                    setSelectedImage(imageUri);
                    Toast.makeText(this, R.string.proof_and_nft_toast_scan_error, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    /**
     * 保存Bitmap到文件
     */
    private Uri saveBitmapToFile(Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "scanned_" + timeStamp + ".jpg";
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
            
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            
            return FileProvider.getUriForFile(this, "com.example.brokerfi.fileprovider", file);
        } catch (IOException e) {
            Log.e("SaveBitmap", "Error saving bitmap", e);
            return null;
        }
    }
    
    /**
     * 设置选中的图片
     */
    private void setSelectedImage(Uri uri) {
        selectedImageUri = uri;
        updateImageDisplay();
        Toast.makeText(this, R.string.proof_and_nft_toast_image_selected, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 更新图片显示
     */
    private void updateImageDisplay() {
        selectedImageContainer.removeAllViews();
        
        if (selectedImageUri != null) {
            String fileName = getFileName(selectedImageUri);
            
            // 创建图片项视图
            View imageItemView = LayoutInflater.from(this).inflate(R.layout.item_selected_image, selectedImageContainer, false);
            
            TextView imageNameText = imageItemView.findViewById(R.id.imageNameText);
            TextView previewButton = imageItemView.findViewById(R.id.previewImageButton);
            TextView deleteButton = imageItemView.findViewById(R.id.deleteImageButton);
            
            imageNameText.setText(fileName);
            
            // 设置预览按钮点击事件
            previewButton.setOnClickListener(v -> showImagePreviewDialog());
            
            // 设置删除按钮点击事件
            deleteButton.setOnClickListener(v -> removeImage());
            
            selectedImageContainer.addView(imageItemView);
            selectedImageContainer.setVisibility(View.VISIBLE);
        } else {
            selectedImageContainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示图片预览对话框
     */
    private void showImagePreviewDialog() {
        if (selectedImageUri == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.proof_and_nft_title_image_preview);
        
        // Create ImageView to display image
        ImageView imageView = new ImageView(this);
        imageView.setImageURI(selectedImageUri);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setAdjustViewBounds(true);
        
        // Set maximum size
        int maxSize = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);
        imageView.setMaxWidth(maxSize);
        imageView.setMaxHeight(maxSize);
        
        builder.setView(imageView);
        builder.setPositiveButton(R.string.proof_and_nft_button_close, (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * 删除图片
     */
    private void removeImage() {
        selectedImageUri = null;
        updateImageDisplay();
        Toast.makeText(this, R.string.proof_and_nft_toast_image_deleted, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 更新文件显示列表
     */
    private void updateFileDisplay() {
        selectedFilesContainer.removeAllViews();
        
        for (int i = 0; i < selectedFileUris.size(); i++) {
            Uri uri = selectedFileUris.get(i);
            String fileName = getFileName(uri);
            
            // 创建文件项视图
            View fileItemView = LayoutInflater.from(this).inflate(R.layout.item_selected_file, selectedFilesContainer, false);
            
            TextView fileNameText = fileItemView.findViewById(R.id.fileNameText);
            TextView deleteButton = fileItemView.findViewById(R.id.deleteFileButton);
            
            fileNameText.setText(fileName);
            
            // 设置删除按钮点击事件
            final int fileIndex = i;
            deleteButton.setOnClickListener(v -> removeFile(fileIndex));
            
            selectedFilesContainer.addView(fileItemView);
        }
        
        // 更新提示信息
        updateFileCountHint();
    }
    
    /**
     * 删除指定位置的文件
     */
    private void removeFile(int index) {
        if (index >= 0 && index < selectedFileUris.size()) {
            selectedFileUris.remove(index);
            updateFileDisplay();
            Toast.makeText(this, R.string.proof_and_nft_toast_file_deleted, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 更新文件数量提示
     */
    private void updateFileCountHint() {
        int currentCount = selectedFileUris.size();
        if (currentCount == 0) {
            fileCountHint.setText(fileCountHint.getContext().getString(R.string.proof_and_nft_maximum) + " " + MAX_FILE_COUNT + " " + fileCountHint.getContext().getString(R.string.proof_and_nft_files_allowed));
        } else {
            fileCountHint.setText(fileCountHint.getContext().getString(R.string.proof_and_nft_selected) + " " + currentCount + "/" + MAX_FILE_COUNT + " " + fileCountHint.getContext().getString(R.string.proof_and_nft_file_s));
        }
    }
    
    /**
     * 获取文件名（支持content://和file://两种URI）
     */
    private String getFileName(Uri uri) {
        String fileName = null;
        
        if ("content".equals(uri.getScheme())) {
            // 对于content://类型的URI，查询文件名
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } else if ("file".equals(uri.getScheme())) {
            // 对于file://类型的URI，直接从路径获取文件名
            fileName = new File(uri.getPath()).getName();
        }
        
        // 如果还是获取不到，使用最后的路径段作为文件名
        if (fileName == null || fileName.isEmpty()) {
            fileName = uri.getLastPathSegment();
        }
        
        // Final fallback
        if (fileName == null || fileName.isEmpty()) {
            fileName = "Unknown File";
        }
        
        Log.d("ProofAndNFT", "获取文件名: " + fileName + " (URI: " + uri + ")");
        return fileName;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(this, R.string.proof_and_nft_toast_camera_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 处理提交成功的响应
     */
    private void handleSubmissionSuccess(String response) {
        try {
            // 尝试解析JSON响应
            org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
            
            if (jsonResponse.getBoolean("success")) {
                // 获取提交详情
                org.json.JSONObject data = jsonResponse.optJSONObject("data");
                if (data != null) {
                    String submissionId = data.optString("submissionId", "Unknown");
                    String status = data.optString("status", "PENDING");
                    String message = data.optString("message", "Submission successful");
                    
                    // 显示详细成功信息
                    showDetailedSuccessDialog(submissionId, status, message);
                    
                    // 保存提交记录到本地
                    saveSubmissionToLocal(submissionId, status);
                    
                    // 重置表单
                    resetForm();
                } else {
                    // If no detailed data, show simple success message
                    String message = jsonResponse.optString("message", "Submission successful");
                    showSimpleSuccessDialog(message);
                    resetForm();
                }
            } else {
                // Server returned failure status
                String errorMessage = jsonResponse.optString("message", "Submission failed");
                showErrorDialog("Submission Failed", errorMessage);
            }
            
        } catch (org.json.JSONException e) {
            Log.e("ProofSubmit", "Failed to parse server response", e);
            // JSON parsing failed, might be a simple string response
            if (response.toLowerCase().contains("success")) {
                showSimpleSuccessDialog("Submission successful! Please wait for admin review.");
                resetForm();
            } else {
                showErrorDialog("Response Parse Error", "Server returned an unparseable response format");
            }
        }
    }
    
    /**
     * 处理提交失败的响应
     */
    private void handleSubmissionError(String error) {
        Log.e("ProofSubmit", "提交失败: " + error);
        
        // Analyze error type and provide corresponding suggestions
        String userFriendlyMessage;
        String suggestion = "";
        
        if (error.contains("网络") || error.contains("Network") || error.contains("timeout")) {
            userFriendlyMessage = "Network Connection Issue";
            suggestion = "Please check network connection and retry";
        } else if (error.contains("文件") || error.contains("File")) {
            userFriendlyMessage = "File Processing Error";
            suggestion = "Please check file format and size";
        } else if (error.contains("服务器") || error.contains("Server") || error.contains("500")) {
            userFriendlyMessage = "Server Temporarily Unavailable";
            suggestion = "Please try again later";
        } else if (error.contains("权限") || error.contains("Permission") || error.contains("401")) {
            userFriendlyMessage = "Permission Verification Failed";
            suggestion = "Please check account status";
        } else {
            userFriendlyMessage = "Submission Failed";
            suggestion = "Please check input and retry";
        }
        
        showErrorDialog(userFriendlyMessage, suggestion + "\n\nDetailed error: " + error);
    }
    
    /**
     * 显示详细成功对话框
     */
    private void showDetailedSuccessDialog(String submissionId, String status, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.proof_and_nft_title_submission_success)
               .setMessage(getString(R.string.proof_and_nft_message_submission_id) + " " + submissionId + "\n" +
                          getString(R.string.proof_and_nft_message_current_status) + " " + getStatusDescription(status) + "\n" +
                          getString(R.string.proof_and_nft_message_details) + " " + message + "\n\n" +
                          getString(R.string.proof_and_nft_message_check_ranking))
               .setPositiveButton(R.string.proof_and_nft_button_view_ranking, (dialog, which) -> {
                   // Jump to medal ranking page
                   openMedalRankingPage();
               })
               .setNegativeButton(R.string.main_button_ok, null)
               .setCancelable(false)
               .show();
    }
    
    /**
     * 显示简单成功对话框
     */
    private void showSimpleSuccessDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.proof_and_nft_title_submission_success)
               .setMessage(message)
               .setPositiveButton(R.string.proof_and_nft_button_view_ranking, (dialog, which) -> {
                   openMedalRankingPage();
               })
               .setNegativeButton(R.string.main_button_ok, null)
               .setCancelable(false)
               .show();
    }
    
    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.proof_and_nft_error_title_prefix, title))
               .setMessage(message)
               .setPositiveButton(R.string.proof_and_nft_button_retry, (dialog, which) -> {
                   // Can trigger resubmission here
                   dialog.dismiss();
               })
               .setNegativeButton(R.string.proof_and_nft_button_cancel, null)
               .setCancelable(true)
               .show();
    }
    
    /**
     * 获取状态描述
     */
    private String getStatusDescription(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Pending Review";
            case "APPROVED":
                return "Approved";
            case "REJECTED":
                return "Rejected";
            case "PROCESSING":
                return "Processing";
            default:
                return status;
        }
    }
    
    /**
     * 保存提交记录到本地
     */
    private void saveSubmissionToLocal(String submissionId, String status) {
        try {
            // 使用SharedPreferences保存提交记录
            android.content.SharedPreferences prefs = getSharedPreferences("submissions", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            
            // 保存提交记录（简单的键值对格式）
            long timestamp = System.currentTimeMillis();
            String key = "submission_" + submissionId;
            String value = status + "|" + timestamp + "|" + getCurrentWalletAddress();
            
            editor.putString(key, value);
            editor.apply();
            
            Log.d("ProofSubmit", "提交记录已保存: " + key + " = " + value);
        } catch (Exception e) {
            Log.e("ProofSubmit", "保存提交记录失败", e);
        }
    }
    
    /**
     * 重置表单
     */
    private void resetForm() {
        selectedFileUris.clear();
        selectedImageUri = null;
        displayNameEditText.setText("");
        representativeWorkEditText.setText("");
        if (showRepresentativeWorkNo != null) {
            showRepresentativeWorkNo.setChecked(true); // 默认选择"不展示"
        }
        
        updateFileDisplay();
        updateImageDisplay();
        updateFileCountHint();
        
        Toast.makeText(this, R.string.proof_and_nft_toast_form_reset, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 打开勋章排行榜页面
     */
    private void openMedalRankingPage() {
        Intent intent = new Intent(this, MedalRankingActivity.class);
        startActivity(intent);
    }
    
    /**
     * 加载用户信息（花名、代表作、是否展示代表作）
     */
    private void loadUserInfo() {
        new Thread(() -> {
            try {
                String myAddress = getCurrentWalletAddress();
                Log.d("ProofAndNFT", "==== 开始加载用户信息 ====");
                Log.d("ProofAndNFT", "当前地址: " + myAddress);
                
                // 检查地址是否有效
                if (myAddress == null || myAddress.equals("0000000000000000000000000000000000000000")) {
                    Log.e("ProofAndNFT", "地址无效，跳过加载用户信息");
                    return;
                }
                
                // 构建API请求URL - 使用ServerConfig配置
                String apiUrl = ServerConfig.USER_INFO_API + "/" + myAddress;
                Log.d("ProofAndNFT", "请求URL: " + apiUrl);
                Log.d("ProofAndNFT", "BASE_URL: " + ApiConfig.BASE_URL);
                
                // 发送HTTP GET请求
                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                Log.d("ProofAndNFT", "开始连接...");
                int responseCode = connection.getResponseCode();
                Log.d("ProofAndNFT", "响应码: " + responseCode);
                
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
                    Log.d("ProofAndNFT", "用户信息响应: " + response.toString());
                    
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
                                    Log.d("ProofAndNFT", "已填充花名: " + displayName);
                                }
                                if (!representativeWork.isEmpty() && !"null".equals(representativeWork)) {
                                    representativeWorkEditText.setText(representativeWork);
                                    Log.d("ProofAndNFT", "已填充代表作: " + representativeWork);
                                }
                                if (showRepresentativeWork && showRepresentativeWorkYes != null) {
                                    showRepresentativeWorkYes.setChecked(true);
                                    Log.d("ProofAndNFT", "已填充展示设置: true");
                                } else if (showRepresentativeWorkNo != null) {
                                    showRepresentativeWorkNo.setChecked(true);
                                    Log.d("ProofAndNFT", "已填充展示设置: false");
                                }
                            });
                        }
                    } else {
                        Log.d("ProofAndNFT", "用户信息不存在或加载失败，响应: " + response.toString());
                    }
                } else {
                    Log.e("ProofAndNFT", "加载用户信息失败，响应码: " + responseCode);
                    
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
                        Log.e("ProofAndNFT", "错误响应: " + errorResponse.toString());
                    } catch (Exception ex) {
                        Log.e("ProofAndNFT", "无法读取错误响应");
                    }
                }
            } catch (java.net.ConnectException e) {
                Log.e("ProofAndNFT", "连接失败: 无法连接到服务器", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.proof_and_nft_toast_server_connect_failed, Toast.LENGTH_SHORT).show();
                });
            } catch (java.net.SocketTimeoutException e) {
                Log.e("ProofAndNFT", "连接超时: 服务器响应超时", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.proof_and_nft_toast_response_timeout, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("ProofAndNFT", "加载用户信息异常: " + e.getClass().getName() + " - " + e.getMessage(), e);
            }
            Log.d("ProofAndNFT", "==== 用户信息加载流程结束 ====");
        }).start();
    }
}
