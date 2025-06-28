package com.example.brokerfi.xc;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
    public static volatile boolean flag2 = true;

    private volatile int position=0;

    public  Spinner accountSpinner;
//    private TextView balanceTextView;

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
        flag2 = true;
        accountSpinner = findViewById(R.id.accountSpinner);
//        balanceTextView = findViewById(R.id.balanceTextView);


        StorageUtil.saveCurrentAccount(MainActivity.this,"0");
        accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 例如：balanceTextView.setText("Selected: " + accounts[position]);
//                    Toast.makeText(MainActivity.this,"position is "+position+",id is "+ id,Toast.LENGTH_LONG).show();
                String s = String.valueOf(position);
                MainActivity.this.position=position;
                StorageUtil.saveCurrentAccount(MainActivity.this,s);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 没有选择时的处理
            }
        });
//        new Thread(()->{
//            while (true) {
//                try {
//                    Thread.sleep(1000L);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                if(flag){
//                    break;
//                }
//                String acc = StorageUtil.getCurrentAccount(this);
//                try {
//
//                    if(acc!=null){
//                        int i = Integer.parseInt(acc);
//                        if(accountSpinner.getAdapter() != null && accountSpinner.getAdapter().getCount() > i){
//                            accountSpinner.setSelection(i);
//                        }
//
//                    }
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//
//
//            }
//        }).start();
        new Thread(()->{
            while (true){
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(flag){
                    break;
                }
//                if (!flag2){
//                    continue;
//                }
//                flag2 = false;

                String account = StorageUtil.getPrivateKey(this);
                if (account != null) {
                    String[] split = account.split(";");
                    String[] arr = new String[split.length];
                    for (int i = 0; i < arr.length; i++) {
//                        arr[i] = SecurityUtil.GetAddress(split[i]);
                        arr[i] = "Account " + (i + 1);
                    }
                    runOnUiThread(()->{
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, arr);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        accountSpinner.setAdapter(adapter);
                        accountSpinner.setSelection(MainActivity.this.position,false);
                    });
                }

            }

        }).start();



        // 设置适配器

        // 设置选择监听器



        new Thread(()->{
            while (true){
                if(flag){
                    break;
                }
                fetchAccountStatus();
                try {
                    Thread.sleep(1000);
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