package com.example.brokerfi.xc;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class MainActivity extends AppCompatActivity {
    private ImageView menu;
    private RelativeLayout action_bar;
    private ImageView buy;
    private ImageView send;
    private ImageView swap;
    private ImageView broker;
    private ImageView nft;
    private LinearLayout support;
    private NavigationHelper navigationHelper;
    private RelativeLayout sendlist;
    private RelativeLayout receivelist;
    private RelativeLayout activitylist;
    private RelativeLayout setlist;
    private RelativeLayout supportlist;
    private RelativeLayout locklist;
    private TextView accountstate;
    private TextView tsv_dollar;
    private volatile boolean flag = false;



    @Override
    protected void onDestroy() {
        super.onDestroy();
        flag = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intView();
        intEvent();
        new Thread(()->{
            while (true){
                if(flag){
                    break;
                }
                fetchAccountStatus();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        action_bar = findViewById(R.id.action_bar);
        buy = findViewById(R.id.buy);
        send = findViewById(R.id.send);
        swap = findViewById(R.id.swap);
        broker = findViewById(R.id.broker);
        support = findViewById(R.id.support);
        accountstate=findViewById(R.id.WTextview);
        tsv_dollar=findViewById(R.id.tsv_dollar);
        nft = findViewById(R.id.nft);
    }

    private void intEvent(){

        navigationHelper = new NavigationHelper(menu, action_bar,this);
        buy.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,FaucetActivity.class);
            //跳转
            startActivity(intent);
        });

        send.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,SendActivity.class);
            //跳转
            startActivity(intent);
        });

        swap.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,SwapActivity.class);
            //跳转
            startActivity(intent);
        });

        broker.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,BrokerActivity.class);
            //跳转
            startActivity(intent);
        });
        nft.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,NFTMainActivity.class);
            //跳转
            startActivity(intent);
        });

    }

    private void fetchAccountStatus() {
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
                ReturnAccountState returnAccountState = null;
                try {
                    returnAccountState=MyUtil.GetAddrAndBalance(privatekey);
                    ReturnAccountState finalReturnAccountState = returnAccountState;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (finalReturnAccountState !=null){
                                String balance = finalReturnAccountState.getBalance();
                                Log.d("balance:",balance);
                                updateAccountStatusText(balance);
                            }
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }finally {

                }



            }).start();


        }
    }

    private void updateAccountStatusText(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String status1 =status;
                if(status1.length()>=10){
//                    accountstate.setTextSize(24);
                    status1 = status1.substring(0,10);
                }
                accountstate.setText(status1+" BKC");
                tsv_dollar.setText("");
            }
        });
    }

    @Override
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

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("你确定要退出应用吗？")
                .setCancelable(false)
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishAffinity();
                    }
                })
                .setNegativeButton("否", null)
                .show();
    }
}