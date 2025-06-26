package com.example.brokerfi.xc;

//import static com.example.brokerfi.xc.MainActivity.accountSpinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SelectAccountActivity extends AppCompatActivity {
    private ImageView menu;
    private RelativeLayout action_bar;
    private Button btn_add;
    private Button btn_add2;
    private NavigationHelper navigationHelper;
    private RelativeLayout currentLayout = null;
    private ImageView currentcheck ;
    private RelativeLayout.LayoutParams layoutParams;
    private LinearLayout acclinear;
    private volatile boolean flag = false;
    private Lock lock = new ReentrantLock(false);
    public static volatile  boolean flag2 = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_account);

        acclinear = findViewById(R.id.acclinear);
        intView();
        intEvent();

    }


    private void intView() {
        menu = findViewById(R.id.menu);
        action_bar = findViewById(R.id.action_bar);
        btn_add = findViewById(R.id.btn_add);
        btn_add2 = findViewById(R.id.btn_add2);
        layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);


        new Thread(()->{
            while (true) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(flag){
                    break;
                }
                if (!flag2){
                    continue;
                }
//                flag2 = false;
                service.execute(()->{
                    refresh();
                });

            }
        }).start();


    }

    static ExecutorService service = Executors.newCachedThreadPool();

    Lock lock1  =new ReentrantLock(false);
    private void refresh() {

        String account = StorageUtil.getPrivateKey(this);
        List<ReturnAccountState> list = new ArrayList<>();
//        List<String> validlist = new ArrayList<>();
        if (account != null) {
            String[] split = account.split(";");


            ConcurrentHashMap<Integer,ReturnAccountState> map = new ConcurrentHashMap<>();

            CountDownLatch latch = new CountDownLatch(split.length);
            for (int i = 0;i<split.length;i++) {
                String s = split[i];
                Integer finali = i;
                service.execute(() -> {
                    ReturnAccountState state = MyUtil.GetAddrAndBalance(s);
                    if(state!= null){
                        map.put(finali,state);
                    }
                    latch.countDown();
                });

//                if (returnAccountState.get() != null) {
//                    list.add(returnAccountState.get());
//                    validlist.add(s);
//                }
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0;i<split.length;i++) {
                if(map.containsKey(i)){
                    ReturnAccountState state = map.get(i);
                    list.add(state);
                }
            }


        }
//        StringBuilder saveA = new StringBuilder();
//        for (int i = 0; i < validlist.size(); i++) {
//            saveA.append(validlist.get(i));
//            if (i != validlist.size() - 1) {
//                saveA.append(";");
//            }
//        }
//        StorageUtil.savePrivateKey(this, saveA.toString());

        lock1.lock();
        try {


        runOnUiThread(() -> {
            acclinear.removeAllViews();
        });

        for (int i = 0; i < list.size(); i++) {
            ReturnAccountState accountState = list.get(i);
            RelativeLayout relativeLayout = new RelativeLayout(SelectAccountActivity.this);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            relativeLayout.setLayoutParams(layoutParams);
            int paddingInDp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            relativeLayout.setPadding(paddingInDp, paddingInDp, paddingInDp, paddingInDp);
            relativeLayout.setGravity(Gravity.CENTER_VERTICAL);
            relativeLayout.setClickable(true);
            relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onRelativeLayoutClick(view);
                }
            });
            ImageView imageView = new ImageView(SelectAccountActivity.this);
            RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams2.addRule(RelativeLayout.CENTER_VERTICAL);
            imageView.setId(R.id.sendicon);
            imageView.setLayoutParams(layoutParams2);
            imageView.setImageResource(R.drawable.user_icon);

            LinearLayout linearLayout = new LinearLayout(SelectAccountActivity.this);
            RelativeLayout.LayoutParams layoutParams3 = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            int marginLeftInDp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            layoutParams.leftMargin = marginLeftInDp;
            layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.sendicon);
            linearLayout.setLayoutParams(layoutParams3);
            linearLayout.setOrientation(LinearLayout.VERTICAL);


            TextView textView = new TextView(this);
            textView.setText("Account " + String.valueOf(i + 1));
            float textSizeInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18, getResources().getDisplayMetrics());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPx);
            textView.setTextColor(Color.parseColor("#000000"));
            ViewGroup.LayoutParams layoutParams4 = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textView.setLayoutParams(layoutParams4);

            TextView textView2 = new TextView(this);
            textView2.setText(accountState.getAccountAddr());
            float textSizeInPx2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics());
            textView2.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPx2);
            ViewGroup.LayoutParams layoutParams5 = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textView2.setLayoutParams(layoutParams5);
            linearLayout.addView(textView);
            linearLayout.addView(textView2);
            relativeLayout.addView(linearLayout);
            TextView textView3 = new TextView(this);
            textView3.setText(accountState.getBalance() + "BKC");
            float textSizeInPx3 = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
            textView3.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPx3);
            textView3.setTextColor(getResources().getColor(R.color.black));
            RelativeLayout.LayoutParams layoutParams6 = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams6.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            layoutParams6.addRule(RelativeLayout.CENTER_VERTICAL);
            textView3.setLayoutParams(layoutParams6);
            relativeLayout.addView(textView3);
            runOnUiThread(() -> {
                acclinear.addView(relativeLayout);
            });

        }
        }finally {
            lock1.unlock();
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        flag = true;
    }
    private void intEvent(){
        navigationHelper = new NavigationHelper(menu, action_bar,this);

        btn_add.setOnClickListener(view -> {
            //创建意图对象
            Intent intent = new Intent();
            intent.setClass(SelectAccountActivity.this, AddAccountActivity.class);
            //跳转
            startActivity(intent);
        });
        btn_add2.setOnClickListener(view -> {
            //创建意图对象
            Intent intent = new Intent();
            intent.setClass(SelectAccountActivity.this, GenerateAccountActivity.class);
            //跳转
            startActivity(intent);
        });
    }
    public void onRelativeLayoutClick(View view) {
        RelativeLayout relativeLayout = (RelativeLayout) view;

        if (currentLayout != null) {
            int childCount = currentLayout.getChildCount();
            // 恢复上一个布局的默认状态
            currentLayout.setBackgroundColor(Color.WHITE);
            TextView textView=(TextView) currentLayout.getChildAt(childCount - 2);




            currentLayout.removeView(currentcheck);
            RelativeLayout.LayoutParams params=(RelativeLayout.LayoutParams) textView.getLayoutParams();
            params.removeRule(RelativeLayout.LEFT_OF);
            params.setMargins(0, 0, 0, 0);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            textView.setLayoutParams(params);
            if(currentLayout == relativeLayout){
                currentLayout = null;
            }else{
                int grayColor = Color.rgb(229, 231, 235);
                relativeLayout.setBackgroundColor(grayColor);
                currentLayout = relativeLayout;
                LinearLayout layout = (LinearLayout) currentLayout.getChildAt(0);
                TextView textView0 = (TextView) layout.getChildAt(0);
                System.out.println(textView0.getText().toString());
                int i = Integer.parseInt(textView0.getText().toString().split(" ")[1]);
                int cur = i-1;
                String s = String.valueOf(cur);
                StorageUtil.saveCurrentAccount(this,s);
//                accountSpinner.setSelection(cur);
                TextView textView2 = (TextView) getLastView(relativeLayout);
                ImageView imageView = new ImageView(SelectAccountActivity.this);
                imageView.setImageResource(R.drawable.check);
                imageView.setId(View.generateViewId());
                currentcheck=imageView;
                imageView.setLayoutParams(layoutParams);
                relativeLayout.addView(imageView);
                RelativeLayout.LayoutParams params2=(RelativeLayout.LayoutParams) textView2.getLayoutParams();
                params2.addRule(RelativeLayout.LEFT_OF,imageView.getId());
                params2.setMargins(0, 0, 8, 0);
                params2.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                textView2.setLayoutParams(params2);

            }

        }else{
            int grayColor = Color.rgb(229, 231, 235);
            relativeLayout.setBackgroundColor(grayColor);
            currentLayout = relativeLayout;


            LinearLayout layout = (LinearLayout) currentLayout.getChildAt(0);
            TextView textView0 = (TextView) layout.getChildAt(0);
            System.out.println(textView0.getText().toString());
            int i = Integer.parseInt(textView0.getText().toString().split(" ")[1]);
            int cur = i-1;
            String s = String.valueOf(cur);
            StorageUtil.saveCurrentAccount(this,s);
//            accountSpinner.setSelection(cur);
            TextView textView = (TextView) getLastView(relativeLayout);
            ImageView imageView = new ImageView(SelectAccountActivity.this);
            imageView.setImageResource(R.drawable.check);
            imageView.setId(View.generateViewId());
            currentcheck=imageView;
            imageView.setLayoutParams(layoutParams);
            relativeLayout.addView(imageView);
            RelativeLayout.LayoutParams params=(RelativeLayout.LayoutParams) textView.getLayoutParams();
            params.addRule(RelativeLayout.LEFT_OF,imageView.getId());
            params.setMargins(0, 0, 8, 0);
            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            textView.setLayoutParams(params);

        }

    }

    private View getLastView(ViewGroup viewGroup) {
        int childCount = viewGroup.getChildCount();
        if (childCount > 0) {
            return viewGroup.getChildAt(childCount - 1);
        }
        return null;
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