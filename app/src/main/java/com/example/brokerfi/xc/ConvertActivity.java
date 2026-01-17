package com.example.brokerfi.xc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;

public class ConvertActivity extends AppCompatActivity {

    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    private EditText oldAddressInput;
    private EditText newAddressInput;
    private Button convertButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convert);

        intView();
        intEvent();

        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(ConvertActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        oldAddressInput = findViewById(R.id.oldAddressInput);
        newAddressInput = findViewById(R.id.newAddressInput);
        convertButton = findViewById(R.id.convertButton);

        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldAddress = oldAddressInput.getText().toString().trim();
                String newAddress = newAddressInput.getText().toString().trim();
                
                if (oldAddress.isEmpty() || newAddress.isEmpty()) {
                    Toast.makeText(ConvertActivity.this, "请填写旧账户地址和新账户地址", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // The Convert function can add below

                Toast.makeText(ConvertActivity.this, "转换功能待实现", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
    }
}