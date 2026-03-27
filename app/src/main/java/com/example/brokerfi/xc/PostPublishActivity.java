package com.example.brokerfi.xc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.api.PostApi;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.manager.UserManager;
import com.example.brokerfi.xc.net.ApiCallback;
import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.model.object.PutObjectRequest;
import com.tencent.cos.xml.model.object.PutObjectResult;
import com.tencent.qcloud.core.auth.BasicLifecycleCredentialProvider;
import com.tencent.qcloud.core.auth.QCloudLifecycleCredentials;
import com.tencent.qcloud.core.auth.SessionQCloudCredentials;
import com.tencent.qcloud.core.common.QCloudClientException;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PostPublishActivity extends AppCompatActivity {

    // ====================== 【你只需要改这里 4 个】======================
    private static final String REGION = "ap-guangzhou";        // COS 地域
    private static final String BUCKET = "wallet-community-1416742399";   // 存储桶名称
    private static final String COS_TEMP_KEY_URL = "http://172.27.71.58:5001/cos/temp-credential"; // 后端密钥接口
    private static final int UPLOAD_EXPIRE_SECONDS = 1800;   // 密钥有效期
    // =================================================================

    private static final int REQUEST_IMAGE = 2001;
    private EditText etTitle, etContent;
    private TextView tvContentCount;
    private GridLayout gridImages;
    private Button btnSubmit;
    private final List<String> imageList = new ArrayList<>();
    private final List<String> cosImageUrls = new ArrayList<>();
    private CosXmlService cosXmlService;
    private int uploadCount = 0;

    // 临时密钥
    private String tmpSecretId;
    private String tmpSecretKey;
    private String sessionToken;
    private long expiredTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_publish);

        initView();
        initListener();
        getTempCredentialFromServer();
    }

    /**
     * 从后端获取临时密钥
     */
    private void getTempCredentialFromServer() {
        new Thread(() -> {
            try {

                Log.d("COS_DEBUG", "==================== 开始请求后端临时密钥 ====================");
                Log.d("COS_DEBUG", "请求地址：" + COS_TEMP_KEY_URL);

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(COS_TEMP_KEY_URL)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                String json = response.body().string();
                JSONObject obj = new JSONObject(json);

                tmpSecretId = obj.getString("tmpSecretId");
                tmpSecretKey = obj.getString("tmpSecretKey");
                sessionToken = obj.getString("sessionToken");
                expiredTime = obj.getLong("expiredTime");

                Log.d("COS_DEBUG", "==================== 获取密钥成功 ====================");
                Log.d("COS_DEBUG", "tmpSecretId: " + tmpSecretId);
                Log.d("COS_DEBUG", "tmpSecretKey: " + tmpSecretKey);
                Log.d("COS_DEBUG", "sessionToken: " + sessionToken);
                Log.d("COS_DEBUG", "expiredTime: " + expiredTime);

                // 初始化 COS
                initCOS();

            } catch (Exception e) {
                Log.e("COS_DEBUG", "==================== 获取密钥失败 ====================");
                Log.e("COS_DEBUG", "错误信息：" + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(PostPublishActivity.this, "获取密钥失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /**
     * 初始化 COS（使用临时密钥）
     */
    private void initCOS() {
        CosXmlServiceConfig serviceConfig = new CosXmlServiceConfig.Builder()
                .setRegion(REGION)
                .isHttps(true)
                .builder();

        // 临时密钥提供器
        BasicLifecycleCredentialProvider provider = new BasicLifecycleCredentialProvider() {
            @Override
            protected QCloudLifecycleCredentials fetchNewCredentials() throws QCloudClientException {
                return new SessionQCloudCredentials(
                        tmpSecretId,
                        tmpSecretKey,
                        sessionToken,
                        expiredTime
                );
            }
        };

        cosXmlService = new CosXmlService(this, serviceConfig, provider);

        // 日志：COS 初始化成功
        Log.d("COS_DEBUG", "==================== COS 初始化成功 ====================");
        Log.d("COS_DEBUG", "cosXmlService 初始化完成：" + (cosXmlService != null));
    }

    private void initView() {
        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        tvContentCount = findViewById(R.id.tv_content_count);
        gridImages = findViewById(R.id.grid_images);
        btnSubmit = findViewById(R.id.btn_submit);
    }

    private void initListener() {
        // 字数统计
        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvContentCount.setText(s.length() + "/3000");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        // 添加图片
        findViewById(R.id.btn_add_image).setOnClickListener(v -> openImagePicker());
        // 发布
        btnSubmit.setOnClickListener(v -> submitPost());
    }

    /**
     * 打开相册
     */
    private void openImagePicker() {
        if (imageList.size() >= 9) {
            Toast.makeText(this, "最多选择9张图片", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    /**
     * 接收图片选择结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                imageList.add(uri.toString());
                refreshImageGrid();
            }
        }
    }

    /**
     * 刷新图片网格
     */
    private void refreshImageGrid() {
        gridImages.removeAllViews();
        int size = getResources().getDisplayMetrics().widthPixels / 3 - 24;

        // 已选图片
        for (int i = 0; i < imageList.size(); i++) {
            String path = imageList.get(i);
            FrameLayout container = new FrameLayout(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(8, 8, 8, 8);
            container.setLayoutParams(params);

            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageURI(Uri.parse(path));

            // 删除按钮
            ImageView deleteBtn = new ImageView(this);
            FrameLayout.LayoutParams delParams = new FrameLayout.LayoutParams(60, 60);
            delParams.gravity = Gravity.END | Gravity.TOP;
            deleteBtn.setLayoutParams(delParams);
            deleteBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);

            int index = i;
            deleteBtn.setOnClickListener(v -> {
                imageList.remove(index);
                refreshImageGrid();
            });

            container.addView(imageView);
            container.addView(deleteBtn);
            gridImages.addView(container);
        }

        // 添加按钮
        if (imageList.size() < 9) {
            ImageView addBtn = new ImageView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(8, 8, 8, 8);
            addBtn.setLayoutParams(params);
            addBtn.setImageResource(android.R.drawable.ic_input_add);
            addBtn.setScaleType(ImageView.ScaleType.CENTER);
            addBtn.setOnClickListener(v -> openImagePicker());
            gridImages.addView(addBtn);
        }
    }

    private void submitPost() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        Log.d("COS_DEBUG", "==================== 开始提交帖子 ====================");
        Log.d("COS_DEBUG", "标题：" + title);
        Log.d("COS_DEBUG", "内容：" + content);
        Log.d("COS_DEBUG", "待上传图片数量：" + imageList.size());

        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cosXmlService == null) {
            Log.e("COS_DEBUG", "cosXmlService 未初始化，无法上传");
            Toast.makeText(this, "正在初始化上传服务，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        cosImageUrls.clear();
        uploadCount = 0;

        // 无图片，直接发布
        if (imageList.isEmpty()) {
            publishToServer(title, content);
            return;
        }

        // 有图片，逐个上传COS
        Toast.makeText(this, "正在上传图片...", Toast.LENGTH_SHORT).show();
        for (String uriStr : imageList) {
            uploadImageToCOS(Uri.parse(uriStr));
        }
    }

    /**
     * 上传单张图片到COS
     */
    private void uploadImageToCOS(Uri uri) {
        try {
            Log.d("COS_DEBUG", "==================== 开始处理单张图片 ====================");
            Log.d("COS_DEBUG", "图片Uri：" + uri.toString());

            // 把Uri转为本地文件（适配相册选择）
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("cos_" + UUID.randomUUID(), ".jpg", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            inputStream.close();
            Log.d("COS_DEBUG", "临时文件创建成功：" + tempFile.getAbsolutePath());
            Log.d("COS_DEBUG", "文件大小：" + tempFile.length() + " byte");

            // COS上传路径（随机文件名，防止重名）
            String cosPath = "community/post/" + UUID.randomUUID() + ".jpg";
            Log.d("COS_DEBUG", "COS存储路径：" + cosPath);

            PutObjectRequest request = new PutObjectRequest(BUCKET, cosPath, tempFile.getAbsolutePath());

            cosXmlService.putObjectAsync(request, new CosXmlResultListener() {
                @Override
                public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                    // 获取云端URL
                    String url = ((PutObjectResult) result).accessUrl;
                    cosImageUrls.add(url);
                    uploadCount++;
                    Log.d("COS_DEBUG", "✅ 图片上传成功");
                    Log.d("COS_DEBUG", "COS URL：" + url);

                    // 所有图片上传完成，提交帖子
                    if (uploadCount == imageList.size()) {
                        publishToServer(etTitle.getText().toString().trim(), etContent.getText().toString().trim());
                    }
                    // 删除临时文件
                    tempFile.delete();
                }

                @Override
                public void onFail(CosXmlRequest request, CosXmlClientException clientException, CosXmlServiceException serviceException) {
                    runOnUiThread(() -> {
                        String errMsg = "";
                        if (clientException != null) {
                            errMsg = clientException.getMessage();
                            Log.e("COS_DEBUG", "❌ 图片上传客户端异常：" + clientException.toString());
                        }
                        if (serviceException != null) {
                            errMsg = serviceException.getMessage();
                            Log.e("COS_DEBUG", "❌ 图片上传服务异常：" + serviceException.toString());
                        }

                        Log.e("COS_DEBUG", "❌ 最终上传失败：" + errMsg);
                        Toast.makeText(PostPublishActivity.this, "上传失败：" + errMsg, Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                        tempFile.delete();
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
            });
        }
    }

    /**
     * 发布帖子
     */
    private void publishToServer(String title, String content) {
        PostDTO postDTO = new PostDTO();
        postDTO.setUserId(UserManager.getInstance().getUserId());
        postDTO.setTitle(title);
        postDTO.setContent(content);
        postDTO.setImages(new ArrayList<>(cosImageUrls));

        Log.d("COS_DEBUG", "用户ID：" + postDTO.getUserId());
        Log.d("COS_DEBUG", "入库的图片URL：" + postDTO.getImages());

        new PostApi().addPost(postDTO, new ApiCallback<PostDTO>() {
            @Override
            public void onSuccess(PostDTO result) {
                runOnUiThread(() -> {
                    Toast.makeText(PostPublishActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.putExtra("newPost", result);
                    setResult(RESULT_OK, intent);
                    finish();
                });
            }

            @Override
            public void onFail(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(PostPublishActivity.this, "发布失败：" + error, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cosXmlService != null) {
            cosXmlService.release();
        }
    }
}