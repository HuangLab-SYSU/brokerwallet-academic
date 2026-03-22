package com.example.brokerfi.xc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.brokerfi.xc.dto.PostDTO;

import java.util.ArrayList;
import java.util.List;

public class PostPublishActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE = 2001;

    private EditText etTitle, etContent;
    private TextView tvContentCount;
    private GridLayout gridImages;
    private Button btnSubmit;

    // 图片列表（统一用String，兼容本地URI & 网络URL）
    private final List<String> imageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_publish); // ❗换成你的XML名字

        initView();
        initListener();
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
            //addBtn.setBackgroundColor(Color.parseColor("#EEEEEE"));
            addBtn.setScaleType(ImageView.ScaleType.CENTER);

            addBtn.setOnClickListener(v -> openImagePicker());

            gridImages.addView(addBtn);
        }
    }

    /**
     * 发布帖子
     */
    private void submitPost() {

        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        // 基础校验
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构造 PostDTO
        PostDTO post = new PostDTO();
//        post.setUserId(1L);
//        post.setUsername("北风");
//        post.setTitle(title);
//        post.setContent(content);
//        post.setImageUrls(new ArrayList<>(imageList));
//        post.setLikeCount(0);
//        post.setCreateTime(String.valueOf(System.currentTimeMillis()));

        // ⭐ 本地模拟存储（后续替换为API）
        PostApiUtil.getInstance().addPost(post);

        Toast.makeText(this, "发布成功", Toast.LENGTH_SHORT).show();

        // 返回首页刷新
        setResult(RESULT_OK);
        finish();
    }
}