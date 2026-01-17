package com.example.brokerfi.xc;

//import static com.example.brokerfi.xc.MainActivity.accountSpinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SelectAccountActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private Button btn_add;
    private Button btn_add2;
    private RelativeLayout currentLayout = null;
    private LinearLayout acclinear;
    private volatile boolean flag = false;
    public static volatile boolean flag2 = true;
    //For determining whether to hide the collapse panel
    private boolean hiddenAccountsExpanded = false;
    private static final String PREF_HIDDEN_ACCOUNTS = "hidden_accounts";
    private static final String PREFS_NAME = "MyPrefs"; // 添加PREFS_NAME常量定义

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_account);

        acclinear = findViewById(R.id.acclinear);
        intView();
        intEvent();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(SelectAccountActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        service.execute(this::refresh);
    }

    // Load hidden account status from local storage (SharedPreference).
    private String getHiddenAccounts() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getString(PREF_HIDDEN_ACCOUNTS, "");
    }

    // Save hidden account status to local storage (SharedPreference).
    private void saveHiddenAccounts(String hiddenAccounts) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_HIDDEN_ACCOUNTS, hiddenAccounts);
        editor.apply();
    }


    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        btn_add = findViewById(R.id.btn_add);
        btn_add2 = findViewById(R.id.btn_add2);
        Button btn_export_private_key = findViewById(R.id.btn_export_private_key);


        new Thread(() -> {
            while (true) {

                if (flag) {
                    break;
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (!flag2) {
                    continue;
                }
//                flag2 = false;
                service.execute(this::refresh);
                try {
                    Thread.sleep(20000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();


    }

    static ExecutorService service = Executors.newCachedThreadPool();

    Lock lock1 = new ReentrantLock(false);


    private void refresh() {

        String account = StorageUtil.getPrivateKey(this);
        List<ReturnAccountState> list = new ArrayList<>();
//        List<String> validlist = new ArrayList<>();
        if (account != null) {
            String[] split = account.split(";");


            ConcurrentHashMap<Integer, ReturnAccountState> map = new ConcurrentHashMap<>();

            CountDownLatch latch = new CountDownLatch(1);
            List<String> list1 = new ArrayList<>();
            //                Integer finali = i;
            //                service.execute(() -> {
            //                    ReturnAccountState state = MyUtil.GetAddrAndBalance2(s);
            //                    if(state!= null){
            //                        map.put(finali,state);
            //                    }
            //                    latch.countDown();
            //                });
            //                if (returnAccountState.get() != null) {
            //                    list.add(returnAccountState.get());
            //                    validlist.add(s);
            //                }
            list1.addAll(Arrays.asList(split));

            String[] pks =list1.toArray(new String[0]);
            String[] addrs = new String[pks.length];
            for (int i = 0; i < pks.length; i++) {
                addrs[i] = SecurityUtil.GetAddress(pks[i]);
            }
            service.execute(() -> {
                ReturnAccountState[] state = MyUtil.GetAddrAndBalance2(addrs);

                if (state != null) {
                    for (int i = 0; i < state.length; i++) {
                        map.put(i, state[i]);
                    }
                }
                latch.countDown();
            });


            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(()->{

                //【 SHOW COST TIME 】For test
                //Toast.makeText(SelectAccountActivity.this,"Cost Time: "+((l2-l))+" ms",Toast.LENGTH_LONG).show();
            });

            for (int i = 0; i < split.length; i++) {
                if (map.containsKey(i)) {
                    //In the previous version of the code, 【accountState】 was named 【state】.
                    ReturnAccountState accountState = map.get(i);
                    // Set a fixed 【AccountName】 based on the original index.
                    String AccountName = getString(R.string.Account, i + 1);
                    accountState.setAccountName(AccountName);//Ignore the 'NullPointerException' warning
                    // Set the IsHidden AccountState
                    accountState.setNewPrivateKeyFormat(SecurityUtil.isNewPrivateKeyFormat(split[i]));
                    list.add(accountState);
                }
            }


        }
//        StringBuilder saveA = new StringBuilder();
//        for (int i = 0; i < validlist.size(); i++) {
//            saveA.append(validlist.get(i));
//            if (i != validlist.size() - 1) {
//                saveA.append("; ");
//            }
//        }
//        StorageUtil.savePrivateKey(this, saveA.toString());

        lock1.lock();
        try {


            // The account hidden accoutstate
            String hiddenAccountsStr = getHiddenAccounts();
            Set<String> hiddenAccounts = new HashSet<>();
            if (!TextUtils.isEmpty(hiddenAccountsStr)) {
                hiddenAccounts.addAll(Arrays.asList(hiddenAccountsStr.split(";")));
            }

            // 根据隐藏状态过滤账户  
            List<ReturnAccountState> visibleAccounts = new ArrayList<>();
            List<ReturnAccountState> hiddenAccountList = new ArrayList<>();

            for (ReturnAccountState accountState : list) {
                if (hiddenAccounts.contains(accountState.getAccountAddr())) {
                    accountState.setHidden(true);
                    hiddenAccountList.add(accountState);
                } else {
                    accountState.setHidden(false);
                    visibleAccounts.add(accountState);
                }
            }

            runOnUiThread(() -> {
                acclinear.removeAllViews();
                
                //Visible account list
                if (visibleAccounts.isEmpty()) {
                    // The empty list notice
                    TextView emptyText = new TextView(SelectAccountActivity.this);
                    emptyText.setText(R.string.Empty_Account_Warning);
                    emptyText.setTextColor(getResources().getColor(R.color.grey_60));
                    emptyText.setTextSize(16);
                    emptyText.setGravity(Gravity.CENTER);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 100, 0, 0);
                    emptyText.setLayoutParams(params);
                    acclinear.addView(emptyText);
                } else {
                    for (int i = 0; i < visibleAccounts.size(); i++) {
                        ReturnAccountState accountState = visibleAccounts.get(i);
                        renderAccountItem(accountState, i + 1, false);
                    }
                }

                // Render the hidden account list if there have hidden accounts.
                if (!hiddenAccountList.isEmpty()) {
                    renderHiddenAccountsSection(hiddenAccountList);
                }
            });

        } finally {
            lock1.unlock();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        flag = true;
    }

    private void intEvent() {
        NavigationHelper navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
        Button btn_export_private_key = findViewById(R.id.btn_export_private_key);

        btn_add.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(SelectAccountActivity.this, AddAccountActivity.class);
            startActivity(intent);
        });
        btn_add2.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(SelectAccountActivity.this, GenerateAccountActivity.class);
            startActivity(intent);
        });
        btn_export_private_key.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(SelectAccountActivity.this, ExportPrivateKeyActivity.class);
            startActivity(intent);
        });
    }

    public void onRelativeLayoutClick(View view) {
        RelativeLayout relativeLayout = (RelativeLayout) view;

        if (currentLayout != null) {
            // Reset the previous layout's default state
            currentLayout.setBackgroundColor(Color.WHITE);
            
            if (currentLayout == relativeLayout) {
                currentLayout = null;
            } else {
                int grayColor = Color.rgb(229, 231, 235);
                relativeLayout.setBackgroundColor(grayColor);
                currentLayout = relativeLayout;
                LinearLayout layout = (LinearLayout) currentLayout.getChildAt(1);
                if (layout.getChildAt(0) instanceof LinearLayout) {
                    LinearLayout accountNameLayout = (LinearLayout) layout.getChildAt(0);
                    if (accountNameLayout.getChildAt(0) instanceof TextView) {
                        TextView textView0 = (TextView) accountNameLayout.getChildAt(0);
                        System.out.println(textView0.getText().toString());
                        int i = Integer.parseInt(textView0.getText().toString().split(" ")[1]);
                        int cur = i - 1;
                        String s = String.valueOf(cur);
                        StorageUtil.saveCurrentAccount(this, s);
//                accountSpinner.setSelection(cur);
                    }
                }
            }

        } else {
            int grayColor = Color.rgb(229, 231, 235);
            relativeLayout.setBackgroundColor(grayColor);
            currentLayout = relativeLayout;

            LinearLayout layout = (LinearLayout) currentLayout.getChildAt(1);
            if (layout.getChildAt(0) instanceof LinearLayout) {
                LinearLayout accountNameLayout = (LinearLayout) layout.getChildAt(0);
                if (accountNameLayout.getChildAt(0) instanceof TextView) {
                    TextView textView0 = (TextView) accountNameLayout.getChildAt(0);
                    System.out.println(textView0.getText().toString());
                    int i = Integer.parseInt(textView0.getText().toString().split(" ")[1]);
                    int cur = i - 1;
                    String s = String.valueOf(cur);
                    StorageUtil.saveCurrentAccount(this, s);
//            accountSpinner.setSelection(cur);
                }
            }
        }
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



    // Render Account Item
    private void renderAccountItem(ReturnAccountState accountState, int index, boolean isHiddenItem) {
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
        
        // The transparency
        if (isHiddenItem) {
            relativeLayout.setAlpha(0.7f);
        }
        
        relativeLayout.setOnClickListener(this::onRelativeLayoutClick);

        ImageView imageView = new ImageView(SelectAccountActivity.this);
        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams2.addRule(RelativeLayout.CENTER_VERTICAL);
        imageView.setId(R.id.sendicon);
        imageView.setLayoutParams(layoutParams2);
        imageView.setImageResource(R.drawable.user_icon);
        relativeLayout.addView(imageView);

        LinearLayout leftInfoLayout = new LinearLayout(SelectAccountActivity.this);
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        leftParams.leftMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        leftParams.addRule(RelativeLayout.RIGHT_OF, R.id.sendicon);
        leftInfoLayout.setLayoutParams(leftParams);
        leftInfoLayout.setOrientation(LinearLayout.VERTICAL);

        //Use the existing private key format flag in the account state to avoid processing the private key directly in the UI layer.
        boolean isNewFormat = accountState.isNewPrivateKeyFormat();

        LinearLayout accountNameLayout = new LinearLayout(SelectAccountActivity.this);
        accountNameLayout.setOrientation(LinearLayout.HORIZONTAL);
        accountNameLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView textView = new TextView(this);
        // Use a unique ID to identify the account
        textView.setText(accountState.getAccountName());
        float textSizeInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18, getResources().getDisplayMetrics());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPx);
        textView.setTextColor(getResources().getColor(R.color.black));
        ViewGroup.LayoutParams nameParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textView.setLayoutParams(nameParams);

        // Account Name Strikethrough
        if (!isNewFormat) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            textView.setTextColor(getResources().getColor(R.color.red_127));
        }

        accountNameLayout.addView(textView);
        //Eye Button
        ImageView eyeButton = new ImageView(SelectAccountActivity.this);
        LinearLayout.LayoutParams eyeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        eyeParams.leftMargin = paddingInDp;
        
        //Eye Icon
        if (isHiddenItem) {
            eyeButton.setImageResource(R.drawable.ic_eye_closed);
        } else {
            eyeButton.setImageResource(R.drawable.ic_eye_open);
        }
        eyeButton.setId(View.generateViewId());
        eyeButton.setLayoutParams(eyeParams);
        eyeButton.setClickable(true);
        eyeButton.setOnClickListener(v -> toggleAccountVisibility(accountState));
        
        //Transition animation
        eyeButton.setScaleType(ImageView.ScaleType.CENTER);
        eyeButton.setAdjustViewBounds(true);
        eyeButton.setMaxWidth(42); 
        eyeButton.setMaxHeight(42); 
        
        accountNameLayout.addView(eyeButton);
        leftInfoLayout.addView(accountNameLayout);

        TextView textView2 = new TextView(this);
        // Account Address 
        String address = accountState.getAccountAddr();
        if (isNewFormat && !address.startsWith("0x")) {
            String Address = getString(R.string.Address_0x,address);
            textView2.setText(Address);
        } else {
            textView2.setText(address);
        }
        float textSizeInPx2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics());
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPx2);
        ViewGroup.LayoutParams addressParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textView2.setLayoutParams(addressParams);
        leftInfoLayout.addView(textView2);

        // The old type account warning
        if (!isNewFormat) {
            TextView warningText = new TextView(this);
            warningText.setText(R.string.Old_Account_Warning);
            float warningTextSizeInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8, getResources().getDisplayMetrics());
            warningText.setTextSize(TypedValue.COMPLEX_UNIT_PX, warningTextSizeInPx);
            warningText.setTextColor(getResources().getColor(R.color.red_127));
            ViewGroup.LayoutParams warningLayoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            warningText.setLayoutParams(warningLayoutParams);
            leftInfoLayout.addView(warningText);
        }

        relativeLayout.addView(leftInfoLayout);
        
        // Account Balance (Use the FormatUtil to format)
        String balance = accountState.getBalance();
        String formattedBalance = FormatUtil.formatBalance(balance);
        TextView textView3 = new TextView(this);
        textView3.setId(View.generateViewId()); // Set ID
        String FormatBalance = getString(R.string.BKC,formattedBalance);
        textView3.setText(FormatBalance);
        float textSizeInPx3 = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        textView3.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPx3);
        textView3.setTextColor(getResources().getColor(R.color.black));
        RelativeLayout.LayoutParams balanceParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        balanceParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        balanceParams.addRule(RelativeLayout.CENTER_VERTICAL);
        balanceParams.rightMargin = paddingInDp;
        textView3.setLayoutParams(balanceParams);
        relativeLayout.addView(textView3);

        acclinear.addView(relativeLayout);
    }

    //Format Balance

    // Render the hiddent account
    private void renderHiddenAccountsSection(List<ReturnAccountState> hiddenAccounts) {


        // 前端 Layout:隐藏账户列表标题（可折叠）
        RelativeLayout hiddenSectionHeader = new RelativeLayout(SelectAccountActivity.this);
        RelativeLayout.LayoutParams headerParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        hiddenSectionHeader.setLayoutParams(headerParams);
        
        int paddingInDp = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        hiddenSectionHeader.setPadding(paddingInDp, paddingInDp, paddingInDp, paddingInDp);
        hiddenSectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        hiddenSectionHeader.setClickable(true);
        hiddenSectionHeader.setBackgroundColor(getResources().getColor(R.color.grey_light)); // 浅灰色背景
        
        // Title: Hidden Accounts   
        TextView hiddenSectionTitle = new TextView(SelectAccountActivity.this);
        String HiddenSectionTitle = getString(R.string.Hidden_Account,hiddenAccounts.size());
        hiddenSectionTitle.setText(HiddenSectionTitle);
        float textSizeInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        hiddenSectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPx);
        hiddenSectionTitle.setTextColor(getResources().getColor(R.color.grey_price_disable));
        RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.addRule(RelativeLayout.CENTER_VERTICAL);
        hiddenSectionTitle.setLayoutParams(titleParams);
        hiddenSectionTitle.setId(View.generateViewId());
        
        //  Fold/Expand Icon
        ImageView expandIcon = new ImageView(SelectAccountActivity.this);
        RelativeLayout.LayoutParams iconParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        iconParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        iconParams.addRule(RelativeLayout.CENTER_VERTICAL);
        
        // Set the icon based on the current expanded state.
        if (hiddenAccountsExpanded) {
            expandIcon.setImageResource(R.drawable.up_circle); // 展开图标
        } else {
            expandIcon.setImageResource(R.drawable.right); // 折叠图标
        }
        
        expandIcon.setId(View.generateViewId());
        expandIcon.setLayoutParams(iconParams);
        
        hiddenSectionHeader.addView(hiddenSectionTitle);
        hiddenSectionHeader.addView(expandIcon);
        
        // Add click event to toggle the expanded state.
        hiddenSectionHeader.setOnClickListener(v -> {
            hiddenAccountsExpanded = !hiddenAccountsExpanded;
            // Re-render the list to update the expanded state.
            refresh();
        });
        
        acclinear.addView(hiddenSectionHeader);
        
        // If the hidden accounts list is expanded, render the hidden account items.
        if (hiddenAccountsExpanded) {
            // Add a container to hold the hidden account items.
            LinearLayout hiddenAccountsContainer = new LinearLayout(SelectAccountActivity.this);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            hiddenAccountsContainer.setLayoutParams(containerParams);
            hiddenAccountsContainer.setOrientation(LinearLayout.VERTICAL);
            
            // Render each hidden account item.
            for (int i = 0; i < hiddenAccounts.size(); i++) {
                ReturnAccountState accountState = hiddenAccounts.get(i);
                renderAccountItem(accountState, i + 1, true);
            }
            
            acclinear.addView(hiddenAccountsContainer);
        }
    }
    
    // Toggle the visibility of an account.
    private void toggleAccountVisibility(ReturnAccountState accountState) {
        // Get the current hidden account state.
        String hiddenAccountsStr = getHiddenAccounts();
        Set<String> hiddenAccounts = new HashSet<>();
        if (!TextUtils.isEmpty(hiddenAccountsStr)) {
            hiddenAccounts.addAll(Arrays.asList(hiddenAccountsStr.split(";")));
        }
        
        String address = accountState.getAccountAddr();
        
        // Toggle the hidden state.
        if (hiddenAccounts.contains(address)) {
            // Show the account.
            hiddenAccounts.remove(address);
            accountState.setHidden(false);
        } else {
            // Hide the account.
            hiddenAccounts.add(address);
            accountState.setHidden(true);
        }
        
        // Save the hidden account state.
        saveHiddenAccounts(hiddenAccounts);
        
        // Re-render the list.
        refresh();
    }
    
    // Save the hiddent accountstate in the local sharedpreference to persist the hidden state.
    private void saveHiddenAccounts(Set<String> hiddenAccounts) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        StringBuilder sb = new StringBuilder();
        for (String address : hiddenAccounts) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(address);
        }
        
        editor.putString(PREF_HIDDEN_ACCOUNTS, sb.toString());
        editor.apply();
    }

}