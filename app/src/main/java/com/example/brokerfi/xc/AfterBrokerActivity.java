package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AfterBrokerActivity extends AppCompatActivity {
    private ImageView menu;
    private RelativeLayout action_bar;
    private ImageView buy;
    private ImageView send;
    private ImageView swap;
    private ImageView broker;
    private LinearLayout support;
    private Button btn_stake;
    private Button btn_withdraw;
    private LinearLayout brokerprofit;
    private NavigationHelper navigationHelper;
    private TextView bkctextview;
    private TextView bkcprofittextview;

    boolean hasExecuted_btn1 = false;// 保证按钮1只被初始化一次
    boolean hasExecuted_btn2 = false;// 保证按钮2只被初始化一次
    private volatile boolean flag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_broker);
        intView();
        intEvent();

        new Thread(() -> {

            while (true) {
                if (flag) {
                    break;
                }
                CountDownLatch latch = new CountDownLatch(1);
                initbrokerprofitview(latch);

                try {
                    latch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        flag = true;
    }

    private void initbrokerprofitview(CountDownLatch latch) {
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
            String result = MyUtil.querybrokerprofit(privatekey);
            if (result != null) {
                JSONObject jsonResponse = null;
                try {
                    jsonResponse = new JSONObject(result);
                    BigDecimal d1 = new BigDecimal(0);
                    BigDecimal d2 = new BigDecimal(0);
                    Double sum = 0.0;
                    Double sumBalance = 0.0;
                    for (int i = 0; i < 100000; i++) {
                        String s;
                        try {
                            s = jsonResponse.getString(String.valueOf(i));
                            if (s == null) {
                                break;
                            }
                        } catch (Exception e) {
                            break;
                        }
                        String[] split = s.split("/");
                        BigDecimal bigDecimal1 = new BigDecimal(split[0]);
                        BigDecimal bigDecimal2 = new BigDecimal(split[1]);
                        d1=d1.add(bigDecimal1);
                        d2=d2.add(bigDecimal2);

//                        sum += Double.parseDouble(split[0]);
//                        sumBalance += Double.parseDouble(split[1]);
                    }

//                    Double finalSum = sum;
                    String finalSum1 = d1.toString();
                    String finalSumBalance1 = d2.toString();
                    JSONObject finalJsonResponse = jsonResponse;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String finalSumBalance = finalSumBalance1;
                            String finalSum = finalSum1;
                            if(finalSum!=null &&finalSum.length()>6){
                                finalSum = finalSum.substring(0,6);
                                bkcprofittextview.setTextSize(16);
                            }
                            bkcprofittextview.setText("+" + (finalSum) + " BKC");
                            if(finalSumBalance!=null &&finalSumBalance.length()>8){
                                finalSumBalance = finalSumBalance.substring(0,8);
                                bkctextview.setTextSize(20);
                            }
                            bkctextview.setText(finalSumBalance + " BKC");
                            brokerprofit.removeAllViews();

                            for (int i = 0; i < 100; i++) {
                                String s;
                                try {
                                    s = finalJsonResponse.getString(String.valueOf(i));
                                    if (s == null) {
                                        break;
                                    }
                                } catch (Exception e) {
                                    break;
                                }

                                String[] split = s.split("/");


                                RelativeLayout relativeLayout = new RelativeLayout(AfterBrokerActivity.this);
                                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.MATCH_PARENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT
                                );
                                relativeLayout.setLayoutParams(layoutParams);

                                TextView shardTextView = new TextView(AfterBrokerActivity.this);
                                shardTextView.setText("SHARD " + i);
                                shardTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                                shardTextView.setTextColor(ContextCompat.getColor(AfterBrokerActivity.this, R.color.white));
                                RelativeLayout.LayoutParams shardParams = new RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT
                                );
                                shardParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                                shardTextView.setLayoutParams(shardParams);
                                relativeLayout.addView(shardTextView);


                                TextView shardTextView2 = new TextView(AfterBrokerActivity.this);
                                shardTextView2.setText(split[0]);
                                shardTextView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                                shardTextView2.setTextColor(ContextCompat.getColor(AfterBrokerActivity.this, R.color.white));
                                RelativeLayout.LayoutParams shardParams2 = new RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT
                                );
                                shardParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                                shardTextView2.setLayoutParams(shardParams2);
                                relativeLayout.addView(shardTextView2);

                                brokerprofit.addView(relativeLayout);

                                ProgressBar progressBar = new ProgressBar(AfterBrokerActivity.this, null, android.R.attr.progressBarStyleHorizontal);
                                progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics())
                                ));

                                progressBar.setProgressDrawable(ContextCompat.getDrawable(AfterBrokerActivity.this, R.drawable.layer_list_progress_drawable));
                                progressBar.setBackgroundColor(Color.WHITE);
//                            progressBar.setMax(max.intValue());
                                progressBar.setMax((int) Double.parseDouble(split[1]));
                                progressBar.setProgress((int) Double.parseDouble(split[0]));
                                progressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(AfterBrokerActivity.this, R.color.green)));
                                progressBar.setSecondaryProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(AfterBrokerActivity.this, R.color.grey)));
                                brokerprofit.addView(progressBar);

                            }
                        }
                    });


                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }
        latch.countDown();
    }


    private void intView() {
        menu = findViewById(R.id.menu);
        action_bar = findViewById(R.id.action_bar);
        buy = findViewById(R.id.buy);
        send = findViewById(R.id.send);
        swap = findViewById(R.id.swap);
        broker = findViewById(R.id.broker);
        support = findViewById(R.id.support);
        btn_stake = findViewById(R.id.btn_stake);
        btn_withdraw = findViewById(R.id.btn_withdraw);
        brokerprofit = findViewById(R.id.BrokerLinearLayout);

        bkctextview = findViewById(R.id.bkctextview);
        bkcprofittextview = findViewById(R.id.bkcprofittextview);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this);

        buttonWithIconAndText();
        buy.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(AfterBrokerActivity.this, BuyActivity.class);

            startActivity(intent);
        });

        send.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(AfterBrokerActivity.this, SendActivity.class);

            startActivity(intent);
        });

        swap.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(AfterBrokerActivity.this, SwapActivity.class);

            startActivity(intent);
        });

        broker.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(AfterBrokerActivity.this, BrokerActivity.class);

            startActivity(intent);
        });
        btn_stake.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(AfterBrokerActivity.this, StakeMoreActivity.class);

            startActivity(intent);
        });
        btn_withdraw.setOnClickListener(view -> {

            Intent intent = new Intent();
            intent.setClass(AfterBrokerActivity.this, WithdrawActivity.class);

            startActivity(intent);
        });

    }

    private void buttonWithIconAndText() {
        ViewTreeObserver vto_btn1 = btn_stake.getViewTreeObserver();
        vto_btn1.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!hasExecuted_btn1) {
                    btn_stake.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    hasExecuted_btn1 = true; // 设置标志为已执行
                    Drawable[] drawables = btn_stake.getCompoundDrawables();
                    Drawable leftDrawable = drawables[0]; // 获取左侧图标
                    String buttonText = btn_stake.getText().toString(); // 获取按钮文本
                    int totalWidth = 0;
                    if (leftDrawable != null) {
                        totalWidth += leftDrawable.getIntrinsicWidth();
                    }
                    totalWidth += (int) btn_stake.getPaint().measureText(buttonText);
                    int buttonWidth = btn_stake.getWidth();
                    int paddingLeft = (buttonWidth - totalWidth) / 2;
                    btn_stake.setPadding(paddingLeft, btn_stake.getPaddingTop(), btn_stake.getPaddingRight(), btn_stake.getPaddingBottom());


                }

            }
        });
        ViewTreeObserver vto_btn2 = btn_withdraw.getViewTreeObserver();
        vto_btn2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!hasExecuted_btn2) {
                    btn_withdraw.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    hasExecuted_btn2 = true;
                    Drawable[] drawables = btn_withdraw.getCompoundDrawables();
                    Drawable leftDrawable = drawables[0];
                    String buttonText = btn_withdraw.getText().toString();
                    int totalWidth = 0;
                    if (leftDrawable != null) {
                        totalWidth += leftDrawable.getIntrinsicWidth();
                    }
                    totalWidth += (int) btn_withdraw.getPaint().measureText(buttonText);
                    int buttonWidth = btn_withdraw.getWidth();
                    int paddingLeft = (buttonWidth - totalWidth) / 2;
                    btn_withdraw.setPadding(paddingLeft, btn_withdraw.getPaddingTop(), btn_withdraw.getPaddingRight(), btn_withdraw.getPaddingBottom());


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