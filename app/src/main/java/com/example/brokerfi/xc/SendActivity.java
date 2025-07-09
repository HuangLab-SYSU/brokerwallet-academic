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
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class SendActivity extends AppCompatActivity {

    private ImageView menu;
    private RelativeLayout action_bar;
    private EditText edt_sendfrom;
    private EditText edt_sendto;
    private EditText edt_amount;
    private EditText edt_fee;
    private NavigationHelper navigationHelper;
    private Button button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        intView();
        intEvent();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(SendActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }


    private void intView() {
        menu = findViewById(R.id.menu);
        action_bar = findViewById(R.id.action_bar);
        edt_sendfrom = findViewById(R.id.edt_sendfrom);



        String account = StorageUtil.getPrivateKey(this);
        String acc = StorageUtil.getCurrentAccount(this);
        int i;
        if (acc == null){
            i=0;
        }else {
            i = Integer.parseInt(acc);
        }
        if (account != null) {
            String[] split = account.split(";");
            String privatekey = split[i];
            String fromaddr = SecurityUtil.GetAddress(privatekey);
            edt_sendfrom.setText(fromaddr);
        }



        edt_sendfrom.setEnabled(false);
        edt_sendto = findViewById(R.id.edt_sendto);

        edt_amount=findViewById(R.id.edt_amount);
        edt_fee= findViewById(R.id.edt_amount2);

        button=findViewById(R.id.btn_send);
    }

    private void intEvent(){
        navigationHelper = new NavigationHelper(menu, action_bar,this);

        String scannedData = getIntent().getStringExtra("scannedData");
        if(scannedData != null){
            edt_sendto.setText(scannedData);
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendtx2network();
            }
        });

    }

    private volatile boolean tx = false;
    private void sendtx2network(){
        if(tx){
            Toast.makeText(SendActivity.this,"Do not resubmit the transaction!",Toast.LENGTH_LONG).show();
            return;
        }
        tx = true;
        String sendTo = edt_sendto.getText().toString();
        String amount = edt_amount.getText().toString();
        String fee = edt_fee.getText().toString();

        String account = StorageUtil.getPrivateKey(this);
        String acc = StorageUtil.getCurrentAccount(this);
        int i;
        if (acc == null){
            i=0;
        }else {
            i = Integer.parseInt(acc);
        }
        if (account != null) {
            String[] split = account.split(";");
            String privatekey = split[i];
            new Thread(()->{
                runOnUiThread(()->{
                    Toast.makeText(SendActivity.this,"Submit transaction successfully! Please wait for the result.",Toast.LENGTH_LONG).show();
                });
                try {
                    String s = MyUtil.SendTX(privatekey,sendTo,amount,fee);
                    if(s!=null &&s.contains("success")){
                        runOnUiThread(()->{
                            Toast.makeText(SendActivity.this,"Send successfully",Toast.LENGTH_LONG).show();
                        });

                    }else{
                        runOnUiThread(()->{
                            Toast.makeText(SendActivity.this,"Send failed："+ s,Toast.LENGTH_LONG).show();
                        });

                    }
                }finally {
                    tx=false;
                }
            }).start();

        }

    }
//    public void onBackPressed() {
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
//        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
//    }
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