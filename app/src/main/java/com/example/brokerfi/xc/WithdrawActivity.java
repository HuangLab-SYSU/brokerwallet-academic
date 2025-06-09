package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class WithdrawActivity extends AppCompatActivity {
    private ImageView menu;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    private Button btn_withdraw;
    private TextView edt_sendfrom;
    private TextView edt_sendto;
    private TextView edt_amount;
    private volatile boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);
        intView();
        intEvent();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        action_bar = findViewById(R.id.action_bar);
        btn_withdraw = findViewById(R.id.btn_withdraw);
        edt_sendfrom= findViewById(R.id.edt_sendfrom);
        edt_sendto= findViewById(R.id.edt_sendto);
        edt_amount=findViewById(R.id.edt_amount);
    }
    private void intEvent(){
        edt_sendfrom.setText("Broker2Earn");
        edt_sendfrom.setEnabled(false);
        String account1 = StorageUtil.getPrivateKey(this);
        String acc1 = StorageUtil.getCurrentAccount(this);
        int i1;
        if (acc1 == null) {
            i1 = 0;
        } else {
            i1 = Integer.parseInt(acc1);
        }
        if (account1 != null) {
            String[] split1 = account1.split(";");
            String privatekey = split1[i1];
            String addr = SecurityUtil.GetAddress(privatekey);
            edt_sendto.setText(addr);
        }

        edt_sendto.setEnabled(false);
        edt_amount.setText("ALL");
        edt_amount.setEnabled(false);
        navigationHelper = new NavigationHelper(menu, action_bar,this);
        btn_withdraw.setOnClickListener(view -> {

            new Thread(()->{


            String account2 = StorageUtil.getPrivateKey(this);
            String acc2 = StorageUtil.getCurrentAccount(this);
            int i2;
            if (acc2 == null) {
                i2 = 0;
            } else {
                i2 = Integer.parseInt(acc2);
            }
            if (account2 != null) {
                String[] split1 = account2.split(";");
                String privatekey = split1[i2];
                String s = MyUtil.withdraw(privatekey);
                if (s!=null){
                    runOnUiThread(()->{
                        Handler h = new Handler();
                        h.postDelayed(()->{

                            Intent intent = new Intent();
                            intent.setClass(WithdrawActivity.this, MainActivity.class);

                            startActivity(intent);
                        },3000);
                        if(s.contains("success")) {
                            runOnUiThread(()->{
                                Toast.makeText(WithdrawActivity.this, "Withdraw successfully", Toast.LENGTH_LONG).show();
                            });
                        }else {
                            runOnUiThread(()->{
                                Toast.makeText(WithdrawActivity.this, "Withdraw failed:"+s, Toast.LENGTH_LONG).show();
                            });
                        }
                    });

                }else {
                    runOnUiThread(()->{
                        Toast.makeText(WithdrawActivity.this, "Withdraw failed", Toast.LENGTH_LONG).show();
                    });
                }
            }
            }).start();
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