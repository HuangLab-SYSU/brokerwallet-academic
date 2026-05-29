package com.example.brokerfi.xc;

import static com.example.brokerfi.config.ApiConfig.BASE_URL_HTTP;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.api.CosApi;
import com.example.brokerfi.xc.api.PostApi;
import com.example.brokerfi.xc.dto.CosCredentialDTO;
import com.example.brokerfi.xc.dto.PostDTO;
import com.example.brokerfi.xc.manager.CosServiceFactory;
import com.example.brokerfi.xc.manager.CosUploadManager;
import com.example.brokerfi.xc.manager.UploadCallback;
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

import android.widget.RelativeLayout;
import com.example.brokerfi.xc.menu.NavigationHelper;

public class PostPublishActivity extends AppCompatActivity {
    private CosUploadManager cosUploadManager;
    private static final int REQUEST_IMAGE = 2001;
    private EditText etTitle, etContent;
    private TextView tvContentCount;
    private GridLayout gridImages;
    private Button btnSubmit;
    private final List<String> imageList = new ArrayList<>();
    private final List<String> cosImageUrls = new ArrayList<>();
    private CosXmlService cosXmlService;

    // 临时密钥
    private String tmpSecretId;
    private String tmpSecretKey;
    private String sessionToken;
    private long expiredTime;
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout actionBar;
    private NavigationHelper navigationHelper;

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

        new CosApi().getTempCredential(new ApiCallback<CosCredentialDTO>() {
            @Override
            public void onSuccess(CosCredentialDTO data) {
                tmpSecretId = data.getTmpSecretId();
                tmpSecretKey = data.getTmpSecretKey();
                sessionToken = data.getSessionToken();
                expiredTime = data.getExpiredTime();

                Log.d("COS_DEBUG", "==================== 获取密钥成功 ====================");
                Log.d("COS_DEBUG", "tmpSecretId: " + tmpSecretId);
                Log.d("COS_DEBUG", "tmpSecretKey: " + tmpSecretKey);
                Log.d("COS_DEBUG", "sessionToken: " + sessionToken);
                Log.d("COS_DEBUG", "expiredTime: " + expiredTime);

                //初始化 COS
                cosXmlService = CosServiceFactory.create(PostPublishActivity.this, tmpSecretId, tmpSecretKey, sessionToken, expiredTime);
                cosUploadManager = new CosUploadManager(PostPublishActivity.this, cosXmlService);

            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("COS_DEBUG", "==================== 获取密钥失败 ====================");
                Log.e("COS_DEBUG", "错误信息：" + errorMsg);
                Toast.makeText(PostPublishActivity.this, "获取密钥失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        actionBar = findViewById(R.id.action_bar);
        navigationHelper = new NavigationHelper(menu, actionBar, this, notificationBtn);
        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        tvContentCount = findViewById(R.id.tv_content_count);
        gridImages = findViewById(R.id.grid_images);
        btnSubmit = findViewById(R.id.btn_submit);

        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(PostPublishActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
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

        // 无图片，直接发布
        if (imageList.isEmpty()) {
            publishToServer(title, content);
            return;
        }

        // 有图片,上传COS
        Toast.makeText(this, "正在上传图片...", Toast.LENGTH_SHORT).show();
        List<Uri> uriList = new ArrayList<>();
        for (String uriStr : imageList) {
            uriList.add(Uri.parse(uriStr));
        }

        cosUploadManager.uploadMultiple(uriList, new UploadCallback() {
            @Override
            public void onSuccess(List<String> urls) {
                runOnUiThread(() -> {
                    Log.d("COS_DEBUG", "全部图片上传完成：" + urls.size());

                    cosImageUrls.clear();
                    cosImageUrls.addAll(urls);

                    publishToServer(title, content);
                });
            }

            @Override
            public void onFail(String error) {
                runOnUiThread(() -> {
                    Log.e("COS_DEBUG", "上传失败：" + error);
                    Toast.makeText(PostPublishActivity.this, "上传失败：" + error, Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                });
            }
        });
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

//        Log.d("COS_DEBUG", "用户ID：" + postDTO.getUserId());
//        Log.d("COS_DEBUG", "入库的图片URL：" + postDTO.getImages());

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