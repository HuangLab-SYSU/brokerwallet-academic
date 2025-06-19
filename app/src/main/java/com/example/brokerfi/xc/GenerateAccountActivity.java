package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class GenerateAccountActivity extends AppCompatActivity {
    private ImageView menu;
    private RelativeLayout action_bar;
    private Button btn_cancel;
    private Button btn_save;
    private NavigationHelper navigationHelper;

    private EditText editacc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_account);

        intView();
        intEvent();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        action_bar = findViewById(R.id.action_bar);
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_save = findViewById(R.id.btn_saveacc);
        editacc = findViewById(R.id.edt_account);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this);

        btn_cancel.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(GenerateAccountActivity.this, SelectAccountActivity.class);

            startActivity(intent);
        });

        btn_save.setOnClickListener(view -> {

            String acc = SecurityUtil.generatePrivateKey();

//            String acc = editacc.getText().toString();
            String existAcc = StorageUtil.getPrivateKey(this);
            if (existAcc != null) {
                if(!existAcc.contains(acc)){
                    existAcc += (";" + acc);
                }
            } else {
                existAcc = acc;
            }
            StorageUtil.savePrivateKey(this, existAcc);

            editacc.setText(acc);
            Toast.makeText(this, "✅ Successfully generate a new account!", Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent();
//            intent.setClass(GenerateAccountActivity.this, SelectAccountActivity.class);
//            startActivity(intent);
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