package com.example.brokerfi.xc;


import static com.example.brokerfi.config.ApiConfig.API_NOTIFICATION_NEWS2_doGET2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.method.LinkMovementMethod;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.brokerfi.xc.net.SharedPrefsUtil;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    // For hidden accounts functionality
    private static final String PREF_HIDDEN_ACCOUNTS = "hidden_accounts";
    private static final String PREFS_NAME = "MyPrefs";
    private static final String DISCLAIMER_TEXT = "BrokerChain（academic）仅供学术交流使用，用户不得使用BrokerChain（academic）从事任何非法活动。用户使用BrokerChain所产生的任何直接或间接后果，均与BrokerChain创始团队无关。BrokerChain（academic）创始团队保留随时修改、更新或终止BrokerChain（academic）的权利，且无需事先通知用户。用户在使用BrokerChain（academic）时，应自行承担风险，并同意放弃对创始团队的任何索赔权利。本免责声明受中华人民共和国法律管辖，并按照其解释。";
    private ImageView menu;
    private ImageView notificationBtn;
    private View action_bar;
    private ImageView buy;
    private ImageView send;
    private ImageView swap;
    private ImageView broker;
    private ImageView nft;
    private ImageView news;
    // EmulatorDAO functionality commented out
    // private ImageView medalSystem;
    private ImageView community;
    private LinearLayout support;
    private NavigationHelper navigationHelper;
    private RelativeLayout sendlist;
    private AlertDialog disclaimerDialog;
    private RelativeLayout receivelist;
    private RelativeLayout activitylist;
    private RelativeLayout setlist;
    private RelativeLayout supportlist;
    private RelativeLayout locklist;
    private ImageView receive;
    private ImageView accounts;
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

        //保存Community token
        SharedPrefsUtil.init(this);






//        balanceTextView = findViewById(R.id.balanceTextView);

        //Save CurrentAccount
        StorageUtil.saveCurrentAccount(MainActivity.this,"0");

        accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                // Get hidden account list from local
                String hiddenAccountsStr = getHiddenAccounts();
                Set<String> hiddenAccounts = new HashSet<>();
                if (!hiddenAccountsStr.isEmpty()) {
                    hiddenAccounts.addAll(Arrays.asList(hiddenAccountsStr.split(";")));
                }
                
                // Get private keys
                String account = StorageUtil.getPrivateKey(MainActivity.this);
                if (account != null) {
                    String[] split = account.split(";");
                    
                    // Find position and index
                    int originalIndex = -1;
                    int visibleCount = 0;
                    for (int i = 0; i < split.length; i++) {
                        String address = SecurityUtil.GetAddress(split[i]);
                        if (!hiddenAccounts.contains(address)) {
                            if (visibleCount == position) {
                                originalIndex = i;
                                break;
                            }
                            visibleCount++;
                        }
                    }
                    
                    if (originalIndex != -1) {
                        // Save position and index
                        MainActivity.this.position = position;
                        StorageUtil.saveCurrentAccount(MainActivity.this, String.valueOf(originalIndex));

                        String privatekey = split[originalIndex];
                        final int currentOriginalIndex = originalIndex; // Save Index
                        
                        // IsNewPrivateKey？
                        boolean isOldFormat = !SecurityUtil.isNewPrivateKeyFormat(privatekey);
                        
                        if (isOldFormat) {
                            // 旧账户直接显示升级提示，不需要获取余额和地址
                            // 主要是 GetAddrAndBalance 缺少错误处理方法，为了不影响其他功能暂时先这样处理
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String currentAccountStr = StorageUtil.getCurrentAccount(MainActivity.this);
                                    int currentOriginal = (currentAccountStr != null) ? Integer.parseInt(currentAccountStr) : 0;

                                    if (currentOriginalIndex == currentOriginal) {
                                        updateAccountStatusText("0", "", true);
                                    }
                                }
                            });
                        } else {
                            // 新账户正常获取余额和地址
                            new Thread(()->{
                                ReturnAccountState returnAccountState = null;
                                try {
                                    returnAccountState=MyUtil.GetAddrAndBalance(privatekey);
                                    ReturnAccountState finalReturnAccountState = returnAccountState;
                                    final int reqOriginalIndex = currentOriginalIndex; // 保存到最终变量以在lambda中使用
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Get the origin index
                                            String currentAccountStr = StorageUtil.getCurrentAccount(MainActivity.this);
                                            int currentOriginal = (currentAccountStr != null) ? Integer.parseInt(currentAccountStr) : 0;
                                            
                                            // 只有当返回结果对应当前选择的账户时才更新UI
                                            if (finalReturnAccountState !=null && reqOriginalIndex == currentOriginal){
                                                String balance = finalReturnAccountState.getBalance();
                                                //Log.d("balance:",balance);调试用
                                                updateAccountStatusText(balance,finalReturnAccountState.getAccountAddr(), false);
                                            }
                                        }
                                    });
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    }
            }
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
            while (true) {
                if(flag){
                    break;
                }
                reward();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();


        new Thread(()->{
            while (true) {
                if (flag) {
                    break;
                }
                try {
                    //byte[] bytes = HTTPUtil.doGet2("https://academic.broker-chain.com/user/news2", null);
                    byte[] bytes = HTTPUtil.doGet2(API_NOTIFICATION_NEWS2_doGET2, null);

                    //Get the update tip message from database in Server
                    //FOr Developer:Use the "title" keyword-This can be edited in the database in Server

                    String noticestr = new String(bytes);
                    JSONObject j = new JSONObject(noticestr);
                    JSONArray array = (JSONArray) j.get("data");
                    Integer maxid = 0;
                    String title = "";
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject jsonObject = (JSONObject) array.get(i);
                        Integer id = (Integer) jsonObject.get("id");

                        if (id>maxid) {
                            title = jsonObject.getString("title");
                            maxid = id;
                        }
                    }
                    String currnoticeIdstr = StorageUtil.getNoticeId(this);
                    Integer currnoticeId=0;
                    if(currnoticeIdstr != null) {
                        currnoticeId = Integer.parseInt(currnoticeIdstr);
                    }
                    if(currnoticeId < maxid) {
                        Integer finalMaxid = maxid;
                        String finalTitle = title;
                        runOnUiThread(()->{
                            new AlertDialog.Builder(this)
                                    .setTitle("")  // 标题
                                    .setMessage(getString(R.string.main_message_you_have_a_new_message) + " " + finalTitle + " " + getString(R.string.main_message_please_check_it_out))  // 内容
                                    .setPositiveButton(R.string.main_button_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            StorageUtil.saveNoticeId(MainActivity.this,String.valueOf(finalMaxid));
                                            Intent intent = new Intent();
                                            intent.setClass(MainActivity.this, NotificationActivity.class);
                                            //跳转
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        });


                    }else {

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }


                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }).start();


        new Thread(()->{
            while (true){

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
                    
                    // Get HiddenAccount list
                    String hiddenAccountsStr = getHiddenAccounts();
                    Set<String> hiddenAccounts = new HashSet<>();
                    if (!hiddenAccountsStr.isEmpty()) {
                        hiddenAccounts.addAll(Arrays.asList(hiddenAccountsStr.split(";")));
                    }
                    
                    // Render visible account
                    List<String> visibleAccountNames = new ArrayList<>();
                    List<Integer> originalIndices = new ArrayList<>();
                    
                    for (int i = 0; i < split.length; i++) {
                        String address = SecurityUtil.GetAddress(split[i]);
                        if (!hiddenAccounts.contains(address)) {
                            String AccountName = getString(R.string.Account,(i + 1));
                            visibleAccountNames.add(AccountName);
                            originalIndices.add(i); // 保存可见账户的原始索引
                        }
                    }
                    
                    // 保存可见账户的原始索引映射
                    final List<Integer> finalOriginalIndices = new ArrayList<>(originalIndices);
                    
                    runOnUiThread(()->{
                        String[] arr = visibleAccountNames.toArray(new String[0]);
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arr){
                            @NonNull
                            @Override
                            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView textView = (TextView) view;
                                textView.setTextSize(17);
                                return view;
                            }

                            @Override
                            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                                View view = super.getDropDownView(position, convertView, parent);
                                TextView textView = (TextView) view;
                                textView.setTextSize(15);
                                return view;
                            }
                        };
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        accountSpinner.setAdapter(adapter);
                        
                        // Update the position
                        String currentAccountStr = StorageUtil.getCurrentAccount(MainActivity.this);
                        if (currentAccountStr != null) {
                            int currentAccount = Integer.parseInt(currentAccountStr);
                            int newPosition = finalOriginalIndices.indexOf(currentAccount);
                            if (newPosition != -1) {
                                MainActivity.this.position = newPosition;
                                accountSpinner.setSelection(newPosition, false);
                            } else if (!finalOriginalIndices.isEmpty()) {
                                // If isHidden choose the first
                                MainActivity.this.position = 0;
                                accountSpinner.setSelection(0, false);
                                // Update index
                                StorageUtil.saveCurrentAccount(MainActivity.this, String.valueOf(finalOriginalIndices.get(0)));
                            }
                        }
                    });
                }
                try {
                    Thread.sleep(30000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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

        fetchAccountStatus();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        buy = findViewById(R.id.buy);
        send = findViewById(R.id.send);
        swap = findViewById(R.id.swap);
        broker = findViewById(R.id.broker);
        support = findViewById(R.id.support);
        accountstate=findViewById(R.id.WTextview);
        tsv_dollar=findViewById(R.id.tsv_dollar);
        nft = findViewById(R.id.nft);
        news = findViewById(R.id.news);
        // EmulatorDAO functionality commented out
        // medalSystem = findViewById(R.id.medalSystem);
        receive = findViewById(R.id.receiveicon);
        accounts = findViewById(R.id.accounts);
        community = findViewById(R.id.community);
        //ImageView convertBtn = findViewById(R.id.convertBtn);
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

    private void intEvent(){

        navigationHelper = new NavigationHelper(menu, action_bar,this,notificationBtn);

        TextView tvDisclaimer = findViewById(R.id.tv_disclaimer);
        
        setupClickableDisclaimerText(tvDisclaimer);

        accounts.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,SelectAccountActivity.class);
            //跳转
            startActivity(intent);
        });
        receive.setOnClickListener(view -> {
            //IsNewPrivateKeyFormat
            if (!SecurityUtil.isNewPrivateKeyFormat(StorageUtil.getCurrentPrivatekey(MainActivity.this))) {
                Toast.makeText(MainActivity.this, R.string.main_toast_receive_old_account, Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,ReceiveActivity.class);
            //跳转
            startActivity(intent);
        });

        buy.setOnClickListener(view -> {
            if (!SecurityUtil.isNewPrivateKeyFormat(StorageUtil.getCurrentPrivatekey(MainActivity.this))) {
                Toast.makeText(MainActivity.this, R.string.main_toast_faucet_old_account, Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,FaucetActivity.class);
            //跳转
            startActivity(intent);
        });

        send.setOnClickListener(view -> {
            if (!SecurityUtil.isNewPrivateKeyFormat(StorageUtil.getCurrentPrivatekey(MainActivity.this))) {
                Toast.makeText(MainActivity.this, R.string.main_toast_send_old_account, Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,SendActivity.class);
            //跳转
            startActivity(intent);
        });

        swap.setOnClickListener(view -> {
            if (!SecurityUtil.isNewPrivateKeyFormat(StorageUtil.getCurrentPrivatekey(MainActivity.this))) {
                Toast.makeText(MainActivity.this, R.string.main_toast_swap_old_account, Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,SwapActivity.class);
            //跳转
            startActivity(intent);
        });

        broker.setOnClickListener(view -> {
            if (!SecurityUtil.isNewPrivateKeyFormat(StorageUtil.getCurrentPrivatekey(MainActivity.this))) {
                Toast.makeText(MainActivity.this, R.string.main_toast_broker_old_account, Toast.LENGTH_SHORT).show();
                return;
            }
            
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
        news.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,NewsActivity.class);
            //跳转
            startActivity(intent);
        });

        // EmulatorDAO functionality commented out
        /*
        medalSystem.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,MedalRankingActivity.class);
            startActivity(intent);
        });
        */

        community.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,CommunityActivity.class);
            startActivity(intent);
        });
        
        //findViewById(R.id.convertBtn).setOnClickListener(view -> {
            //Intent intent = new Intent();
            //intent.setClass(MainActivity.this,ConvertActivity.class);
            //startActivity(intent);
        //});
    }

    private void fetchAccountStatus() {
        String account = StorageUtil.getPrivateKey(this);
        String acc = StorageUtil.getCurrentAccount(this);
        int originalIndex;
        if (acc == null){
            originalIndex=0;
        }else {
            originalIndex = Integer.parseInt(acc);// 获取原始索引
        }
        
        //Is the account hidden?
        String hiddenAccountsStr = getHiddenAccounts();
        Set<String> hiddenAccounts = new HashSet<>();
        if (!hiddenAccountsStr.isEmpty()) {
            hiddenAccounts.addAll(Arrays.asList(hiddenAccountsStr.split(";")));
        }
        
        if (account != null) {
            String[] split = account.split(";");
            if (originalIndex < split.length) {
                String address = SecurityUtil.GetAddress(split[originalIndex]);
                // Only refresh the status when the account is not hidden.
                if (!hiddenAccounts.contains(address)) {
                    String privatekey = split[originalIndex];
                    final int currentOriginalIndex = originalIndex;
                    
                    // IsOldFormat？
                    boolean isOldFormat = !SecurityUtil.isNewPrivateKeyFormat(privatekey);
                    
                    if (isOldFormat) {
                        //Just Show the upadate tip to the old account(暂定方案)
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String acc = StorageUtil.getCurrentAccount(MainActivity.this);
                                int currentAccountPos = (acc == null) ? 0 : Integer.parseInt(acc);
                                if (currentOriginalIndex == currentAccountPos) {
                                    updateAccountStatusText("0", "", true);
                                }
                            }
                        });
                    } else {
                        new Thread(()->{
                            ReturnAccountState returnAccountState = null;
                            try {
                                returnAccountState=MyUtil.GetAddrAndBalance(privatekey);
                                ReturnAccountState finalReturnAccountState = returnAccountState;
                                final int reqOriginalIndex = currentOriginalIndex; // 保存到最终变量以在lambda中使用
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String acc = StorageUtil.getCurrentAccount(MainActivity.this);
                                        int currentAccountPos = (acc == null) ? 0 : Integer.parseInt(acc);
                                        if (finalReturnAccountState !=null && reqOriginalIndex == currentAccountPos){
                                            String balance = finalReturnAccountState.getBalance();
                                            Log.d("balance:",balance);
                                            updateAccountStatusText(balance, finalReturnAccountState.getAccountAddr(), false);
                                        }
                                    }
                                });
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }
            }
        }
    }
    private void reward() {
        String account = StorageUtil.getPrivateKey(this);
        String acc = StorageUtil.getCurrentAccount(this);
        int originalIndex;
        if (acc == null){
            originalIndex=0;
        }else {
            originalIndex = Integer.parseInt(acc);
        }

        String hiddenAccountsStr = getHiddenAccounts();
        Set<String> hiddenAccounts = new HashSet<>();
        if (!hiddenAccountsStr.isEmpty()) {
            hiddenAccounts.addAll(Arrays.asList(hiddenAccountsStr.split(";")));
        }
        
        if (account != null) {
            String[] split = account.split(";");
            if (originalIndex < split.length) {
                String address = SecurityUtil.GetAddress(split[originalIndex]);
                // Only refresh the status when the account is not hidden.
                if (!hiddenAccounts.contains(address)) {
                    String privatekey = split[originalIndex];

                    new Thread(()->{
                        try {
                          MyUtil.Getreward(privatekey);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        }
    }

    private void updateAccountStatusText(final String status,final  String addr, final boolean isOldFormat) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tsv_dollar.setTextSize(10);
                accountstate.setTextSize(28);
                tsv_dollar.setTextColor(Color.BLACK);
                accountstate.setTextColor(Color.BLACK);
                if (isOldFormat) {
                    // Old account type
                    accountstate.setTextSize(19);
                    accountstate.setText(R.string.Old_Account_Warning_MainPage_FirstLine);
                    tsv_dollar.setTextSize(12);
                    tsv_dollar.setTextColor(Color.RED);
                    tsv_dollar.setText(R.string.Old_Account_Warning_MainPage_SecondLine);
                } else {
                    // New account type
                    String status1 =status;
                    if(status1.length()>=10){
                        status1 = status1.substring(0,10);
                    }
                    accountstate.setText(status1+" " + accountstate.getContext().getString(R.string.after_broker_bkc));
                    tsv_dollar.setText(tsv_dollar.getContext().getString(R.string.main_address_0x)+addr);
                }

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

    private void setupClickableDisclaimerText(TextView textView) {
        String fullText = "使用 BrokerChain Wallet 默认您同意以下声明:\n免责声明";
        String clickableText = "免责声明";
        
        SpannableString spannableString = new SpannableString(fullText);
        int start = fullText.indexOf(clickableText);
        int end = start + clickableText.length();
        
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                showDisclaimerDialog();
            }
            
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#2196F3"));
                ds.setUnderlineText(true);
            }
        };
        
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showDisclaimerDialog() {
        if (disclaimerDialog != null && disclaimerDialog.isShowing()) {
            return;
        }
        
        disclaimerDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.main_title)
                .setMessage(DISCLAIMER_TEXT)
                .setCancelable(false)
                .setPositiveButton(R.string.main_button_confirm, (dialog, which) -> {
                    disclaimerDialog = null;
                })
                .create();
        disclaimerDialog.show();
    }

    @Override
    public void onBackPressed() {
        // 检查 NavigationHelper 是否弹出
        if (navigationHelper != null && navigationHelper.isPopupVisible()) {
            // 是则隐藏菜单
            navigationHelper.hidePopup();
        } else {
            // 显示退出确认对话框
            new AlertDialog.Builder(this)
                    .setMessage(R.string.Exit_Confirm)
                    .setCancelable(false)
                    .setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finishAffinity();
                        }
                    })
                    .setNegativeButton(R.string.No, null)
                    .show();
        }
    }
}
