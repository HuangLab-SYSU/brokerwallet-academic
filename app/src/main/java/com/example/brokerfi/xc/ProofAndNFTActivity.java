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
        
        // 初始化OpenCV
        DocumentScannerUtil.initOpenCV(this);
        
        intView();
        intEvent();
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
        builder.setTitle("💡 文件选择提示");
        builder.setMessage("如需从微信等应用中选择文件，请按以下步骤操作：\n\n" +
                "1️⃣ 在微信中找到要上传的文件\n" +
                "2️⃣ 长按文件，选择「转发」\n" +
                "3️⃣ 选择「保存到文件」或「更多」\n" +
                "4️⃣ 将文件保存到手机存储\n" +
                "5️⃣ 返回此页面，点击「选择证明文件」即可找到保存的文件\n\n" +
                "💡 提示：大部分应用的文件都可以通过「分享→保存到本地」的方式进行选择。");
        
        builder.setPositiveButton("我知道了", (dialog, which) -> {
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
            Toast.makeText(this, "最多只能选择 " + MAX_FILE_COUNT + " 个文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // 支持所有文件类型
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // 支持多选
        startActivityForResult(Intent.createChooser(intent, "选择证明文件"), REQUEST_CODE_SELECT_FILE);
    }
    
    /**
     * 显示NFT图片帮助对话框
     */
    private void showImageHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("💡 NFT图片说明");
        builder.setMessage("图片不是必选项，如果不上传图片，您也许会收获一张由DAO组织为您精心铸造的独特NFT！\n\n" +
                "💎 上传图片：使用您的照片作为NFT\n" +
                "🎨 不上传图片：获得DAO组织设计的专属NFT\n\n" +
                "两种方式都很棒，选择您喜欢的方式即可！");
        
        builder.setPositiveButton("我知道了", (dialog, which) -> {
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
            Toast.makeText(this, "只能上传1张图片，请先删除现有图片", Toast.LENGTH_SHORT).show();
            return;
        }
        showImageSourceDialog();
    }
    
    /**
     * 显示图片来源选择对话框
     */
    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择图片来源");
        builder.setMessage("请选择您希望从哪里选择NFT照片：");
        
        // 从图库选择
        builder.setPositiveButton("🖼️ 图库", (dialog, which) -> {
            selectImageFromGallery();
        });
        
        // 拍照（带文档扫描）
        builder.setNeutralButton("📷 拍照扫描", (dialog, which) -> {
            checkCameraPermissionAndTakePhoto();
        });
        
        // 取消
        builder.setNegativeButton("取消", (dialog, which) -> {
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
        startActivityForResult(Intent.createChooser(intent, "从图库选择NFT照片"), REQUEST_CODE_SELECT_IMAGE);
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
            Toast.makeText(this, "无法访问摄像头", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "请先选择证明文件（必填项）", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取当前钱包地址
        String walletAddress = getCurrentWalletAddress();
        if (walletAddress == null) {
            Toast.makeText(this, "无法获取当前钱包地址，请检查钱包状态", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取用户输入的个人信息
        String displayName = displayNameEditText.getText().toString().trim();
        String representativeWork = representativeWorkEditText.getText().toString().trim();
        boolean showRepresentativeWork = showRepresentativeWorkYes.isChecked();
        
        // 显示加载状态
        submitProofButton.setText("提交中...");
        submitProofButton.setEnabled(false);
        
        Log.d("ProofSubmit", "一体化提交 - 钱包地址: " + walletAddress + ", 花名: " + displayName + ", 展示代表作: " + showRepresentativeWork);
        
        // 使用一体化提交API（多个证明文件 + NFT图片 + 用户信息一次性提交）
        SubmissionUtil.submitComplete(this, selectedFileUris, selectedImageUri, 
            walletAddress, displayName, representativeWork, showRepresentativeWork,
            new SubmissionUtil.SubmissionCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProofAndNFTActivity.this, "提交成功！", Toast.LENGTH_SHORT).show();
                        resetSubmitButton();
                        showSuccessMessage();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProofAndNFTActivity.this, "提交失败: " + error, Toast.LENGTH_LONG).show();
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
            // 这里调用NFT图片上传API
            Toast.makeText(this, "NFT图片上传中...", Toast.LENGTH_SHORT).show();
            
            // 模拟上传成功
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 模拟网络延时
                    runOnUiThread(() -> {
                        Toast.makeText(ProofAndNFTActivity.this, "NFT图片上传成功！", Toast.LENGTH_SHORT).show();
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
        String message = "提交完成！\n\n";
        message += "📄 证明文件已上传，等待管理员审核\n";
        
        if (selectedImageUri != null) {
            message += "🖼️ NFT图片已上传，等待管理员批准铸造\n";
        }
        
        String displayName = displayNameEditText.getText().toString().trim();
        String representativeWork = representativeWorkEditText.getText().toString().trim();
        boolean showRepresentativeWork = showRepresentativeWorkYes.isChecked();
        
        if (!displayName.isEmpty() || !representativeWork.isEmpty()) {
            message += "👤 个人信息已更新\n";
            if (showRepresentativeWork) {
                message += "🏆 代表作将在管理员审核后显示在排行榜\n";
            }
        }
        
        message += "\n请耐心等待审核结果！";
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("✅ 提交成功");
        builder.setMessage(message);
        builder.setPositiveButton("确定", (dialog, which) -> {
            dialog.dismiss();
            // 可以选择返回主页面或清空表单
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
                Log.e("WalletAddress", "无法获取当前私钥");
                Toast.makeText(this, "无法获取当前钱包地址，请检查钱包状态", Toast.LENGTH_SHORT).show();
                return null;
            }
        } catch (Exception e) {
            Log.e("WalletAddress", "获取当前钱包地址失败", e);
            Toast.makeText(this, "获取钱包地址失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    /**
     * 重置提交按钮状态
     */
    private void resetSubmitButton() {
        submitProofButton.setText("提交");
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
        
        // 检查文件数量限制
        int totalCount = selectedFileUris.size() + newFiles.size();
        if (totalCount > MAX_FILE_COUNT) {
            int allowedCount = MAX_FILE_COUNT - selectedFileUris.size();
            Toast.makeText(this, "最多只能选择 " + MAX_FILE_COUNT + " 个文件，当前可添加 " + allowedCount + " 个", 
                    Toast.LENGTH_SHORT).show();
            // 只添加允许的文件数量
            for (int i = 0; i < allowedCount && i < newFiles.size(); i++) {
                selectedFileUris.add(newFiles.get(i));
            }
        } else {
            selectedFileUris.addAll(newFiles);
        }
        
        updateFileDisplay();
        Toast.makeText(this, "已选择 " + newFiles.size() + " 个文件", Toast.LENGTH_SHORT).show();
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
        // 显示处理提示
        Toast.makeText(this, "正在优化图片...", Toast.LENGTH_SHORT).show();
        
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
                            Toast.makeText(this, "📄 文档扫描优化完成", Toast.LENGTH_SHORT).show();
                        } else {
                            setSelectedImage(imageUri);
                            Toast.makeText(this, "扫描优化失败，使用原图", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        setSelectedImage(imageUri);
                        Toast.makeText(this, "扫描优化失败，使用原图", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("DocumentScan", "Error processing image", e);
                runOnUiThread(() -> {
                    setSelectedImage(imageUri);
                    Toast.makeText(this, "扫描处理出错，使用原图", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "NFT图片选择成功", Toast.LENGTH_SHORT).show();
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
        builder.setTitle("图片预览");
        
        // 创建ImageView用于显示图片
        ImageView imageView = new ImageView(this);
        imageView.setImageURI(selectedImageUri);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setAdjustViewBounds(true);
        
        // 设置最大尺寸
        int maxSize = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);
        imageView.setMaxWidth(maxSize);
        imageView.setMaxHeight(maxSize);
        
        builder.setView(imageView);
        builder.setPositiveButton("关闭", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * 删除图片
     */
    private void removeImage() {
        selectedImageUri = null;
        updateImageDisplay();
        Toast.makeText(this, "图片已删除", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "文件已删除", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 更新文件数量提示
     */
    private void updateFileCountHint() {
        int currentCount = selectedFileUris.size();
        if (currentCount == 0) {
            fileCountHint.setText("💡 最多可选择 " + MAX_FILE_COUNT + " 个文件");
        } else {
            fileCountHint.setText("💡 已选择 " + currentCount + "/" + MAX_FILE_COUNT + " 个文件");
        }
    }
    
    /**
     * 获取文件名
     */
    private String getFileName(Uri uri) {
        String fileName = uri.getLastPathSegment();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "未知文件";
        }
        return fileName;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(this, "需要摄像头权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        }
    }
}