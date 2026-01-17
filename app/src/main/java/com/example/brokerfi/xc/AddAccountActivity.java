package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.kenai.jffi.Main;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import java.math.BigInteger;

public class AddAccountActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private Button btn_cancel;
    private Button btn_save;
    private NavigationHelper navigationHelper;

    private EditText editacc;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(AddAccountActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        intView();
        intEvent();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_save = findViewById(R.id.btn_saveacc);
        editacc = findViewById(R.id.edt_account);
        tvError = findViewById(R.id.tv_error);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this,notificationBtn);

        btn_cancel.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(AddAccountActivity.this, SelectAccountActivity.class);

            startActivity(intent);
        });

        btn_save.setOnClickListener(view -> {
            String acc = editacc.getText().toString();
            
            // Verify the format of the PrivateKey
            if (!SecurityUtil.isNewPrivateKeyFormat(acc)) {
                tvError.setVisibility(View.VISIBLE);
                return;
            } else {
                tvError.setVisibility(View.GONE);
            }
            
            String existAcc = StorageUtil.getPrivateKey(this);
            if (existAcc != null) {
                if(!existAcc.contains(acc)){
                    existAcc += (";" + acc);
                }
            } else {
                existAcc = acc;
            }
            StorageUtil.savePrivateKey(this, existAcc);

            MainActivity.flag2 = true;
            SelectAccountActivity.flag2 = true;
            Intent intent = new Intent();
            intent.setClass(AddAccountActivity.this, SelectAccountActivity.class);
            startActivity(intent);
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