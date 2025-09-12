package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

public class SelectNetworkActivity extends AppCompatActivity {

    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private RelativeLayout main_network;
    private RelativeLayout third_network;
    private RelativeLayout test_network;
    private RelativeLayout dev_network;
    private Button add_network;
    private RelativeLayout currentLayout = null;
    private ImageView currentcheck;
    private NavigationHelper navigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_network);

        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        main_network = findViewById(R.id.main_network);
        third_network = findViewById(R.id.third_network);
        test_network = findViewById(R.id.test_network);
        dev_network = findViewById(R.id.dev_network);
        add_network = findViewById(R.id.add_network);


        int grayColor = Color.rgb(229, 231, 235);


        navigationHelper = new NavigationHelper(menu, action_bar, this,notificationBtn);

        add_network.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(SelectNetworkActivity.this, AddNetworkActivity.class);
            startActivity(intent);
        });

        main_network.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLayout != null) {
                    currentLayout.setBackgroundColor(Color.WHITE);
                    currentLayout.removeView(currentcheck);
                    if (currentLayout == main_network) {
                        currentLayout = null;
                    } else {
                        main_network.setBackgroundColor(grayColor);
                        currentLayout = main_network;
                        ImageView imageView = new ImageView(SelectNetworkActivity.this);
                        imageView.setImageResource(R.drawable.check);
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                        imageView.setLayoutParams(layoutParams);
                        main_network.addView(imageView);
                        currentcheck = imageView;
                    }

                } else {
                    main_network.setBackgroundColor(grayColor);
                    currentLayout = main_network;
                    ImageView imageView = new ImageView(SelectNetworkActivity.this);
                    imageView.setImageResource(R.drawable.check);
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                    imageView.setLayoutParams(layoutParams);
                    main_network.addView(imageView);
                    currentcheck = imageView;

                }


            }
        });
        third_network.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLayout != null) {
                    currentLayout.setBackgroundColor(Color.WHITE);
                    currentLayout.removeView(currentcheck);
                    if (currentLayout == third_network) {
                        currentLayout = null;
                    } else {

                        third_network.setBackgroundColor(grayColor);
                        currentLayout = third_network;

                        ImageView imageView = new ImageView(SelectNetworkActivity.this);

                        imageView.setImageResource(R.drawable.check);

                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                        imageView.setLayoutParams(layoutParams);
                        third_network.addView(imageView);
                        currentcheck = imageView;
                    }

                } else {
                    third_network.setBackgroundColor(grayColor);
                    currentLayout = third_network;
                    ImageView imageView = new ImageView(SelectNetworkActivity.this);
                    imageView.setImageResource(R.drawable.check);
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                    imageView.setLayoutParams(layoutParams);
                    third_network.addView(imageView);
                    currentcheck = imageView;

                }
            }
        });

        test_network.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLayout != null) {

                    currentLayout.setBackgroundColor(Color.WHITE);
                    currentLayout.removeView(currentcheck);
                    if (currentLayout == test_network) {
                        currentLayout = null;
                    } else {

                        test_network.setBackgroundColor(grayColor);
                        currentLayout = test_network;

                        ImageView imageView = new ImageView(SelectNetworkActivity.this);

                        imageView.setImageResource(R.drawable.check);

                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                        imageView.setLayoutParams(layoutParams);
                        test_network.addView(imageView);
                        currentcheck = imageView;
                    }

                } else {

                    test_network.setBackgroundColor(grayColor);
                    currentLayout = test_network;
                    ImageView imageView = new ImageView(SelectNetworkActivity.this);
                    imageView.setImageResource(R.drawable.check);
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                    imageView.setLayoutParams(layoutParams);
                    test_network.addView(imageView);
                    currentcheck = imageView;

                }
            }
        });

        dev_network.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLayout != null) {

                    currentLayout.setBackgroundColor(Color.WHITE);
                    currentLayout.removeView(currentcheck);
                    if (currentLayout == dev_network) {
                        currentLayout = null;
                    } else {
                        // 切换当前布局的背景
                        dev_network.setBackgroundColor(grayColor);
                        currentLayout = dev_network;

                        ImageView imageView = new ImageView(SelectNetworkActivity.this);

                        imageView.setImageResource(R.drawable.check);

                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                        imageView.setLayoutParams(layoutParams);

                        dev_network.addView(imageView);
                        currentcheck = imageView;
                    }

                } else {

                    dev_network.setBackgroundColor(grayColor);
                    currentLayout = dev_network;

                    ImageView imageView = new ImageView(SelectNetworkActivity.this);

                    imageView.setImageResource(R.drawable.check);

                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                    imageView.setLayoutParams(layoutParams);

                    dev_network.addView(imageView);
                    currentcheck = imageView;

                }
            }
        });

    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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


}