package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class StakeMoreActivity extends AppCompatActivity {

    private TextView stakemoretext;
    private EditText edt_sendfrom;
    private EditText edt_sendto;
    private EditText stakeamount;
    private Button btn_stake;
    private ImageView menu;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;

    boolean flag = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stake_more);

        Intent intent = getIntent();
        String receivedData = intent.getStringExtra("extra_data");
        if(receivedData!=null){
            flag=true;
        }else {

        }
        intView();
        intEvent();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(StakeMoreActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void intView() {
        edt_sendfrom = findViewById(R.id.edt_sendfrom);
        edt_sendto = findViewById(R.id.edt_sendto);
        btn_stake = findViewById(R.id.stakemorebtn);
        menu = findViewById(R.id.menu);
        action_bar = findViewById(R.id.action_bar);
        stakeamount = findViewById(R.id.stakeamount);
        stakemoretext = findViewById(R.id.stakemoretext);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this);
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
            edt_sendfrom.setText(SecurityUtil.GetAddress(privatekey));
        }



        edt_sendfrom.setEnabled(false);
        edt_sendto.setEnabled(false);
        edt_sendto.setText("Broker2Earn");
        if(flag){
            runOnUiThread(()->{
                stakemoretext.setText("Stake");
            });
        }else {
            runOnUiThread(()->{
                stakemoretext.setText("Stake more");
            });
        }


        btn_stake.setOnClickListener(view -> {
            String account1 = StorageUtil.getPrivateKey(this);
            String acc1 = StorageUtil.getCurrentAccount(this);
            int i1;
            if (acc1 == null){
                i1=0;
            }else {
                i1 = Integer.parseInt(acc1);
            }
            if (account1 != null) {
                String[] split = account1.split(";");
                String privatekey = split[i1];
                AtomicReference<String> s = new AtomicReference<>();
                CountDownLatch latch = new CountDownLatch(1);
                new Thread(()->{
                    s.set(MyUtil.stake(privatekey, stakeamount.getText().toString()));
                    latch.countDown();
                }).start();
                try {
                    latch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if(s.get() !=null&& s.get().contains("Successfully")){
                    Toast.makeText(StakeMoreActivity.this, s.get(), Toast.LENGTH_LONG).show();
                    Handler h = new Handler();
                    h.postDelayed(()->{
                        //创建意图对象
                        Intent intent = new Intent();
                        intent.setClass(StakeMoreActivity.this, AfterBrokerActivity.class);
                        //跳转
                        startActivity(intent);
                    },3000);
                }else {
                    if(s.get() ==null){
                        Toast.makeText(StakeMoreActivity.this, "failed", Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(StakeMoreActivity.this, s.get(), Toast.LENGTH_LONG).show();
                    }
                }

            }else {
                Toast.makeText(StakeMoreActivity.this, "failed!", Toast.LENGTH_LONG).show();
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