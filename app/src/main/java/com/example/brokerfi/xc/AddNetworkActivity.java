package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class AddNetworkActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private Button btn_cancel;
    private Button btn_save;
    private EditText edt_name;
    private EditText edt_url;
    private EditText edt_chainID;
    private EditText edt_symbol;
    private EditText edt_optional;
    private NavigationHelper navigationHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_network);

        intView();
        intEvent();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_save = findViewById(R.id.btn_save);
        edt_name = findViewById(R.id.edittext1);
        edt_url = findViewById(R.id.edittext2);
        edt_chainID = findViewById(R.id.edittext3);
        edt_symbol = findViewById(R.id.edittext4);
        edt_optional = findViewById(R.id.edittext5);
    }

    private void intEvent(){
        navigationHelper = new NavigationHelper(menu, action_bar,this,notificationBtn);

        btn_cancel.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(AddNetworkActivity.this, SelectNetworkActivity.class);

            startActivity(intent);
        });

        btn_save.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(AddNetworkActivity.this, SelectNetworkActivity.class);

            startActivity(intent);
        });

    }
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(
                requestCode,resultCode,data
        );
        if (intentResult.getContents() != null){
            String scannedData = intentResult.getContents();
            Intent intent = new Intent(this,SendActivity.class);
            intent.putExtra("scannedData",scannedData);
            startActivity(intent);

        }
    }
}