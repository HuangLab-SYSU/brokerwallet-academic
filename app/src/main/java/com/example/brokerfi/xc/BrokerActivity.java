package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BrokerActivity extends AppCompatActivity implements View.OnTouchListener{
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    private Button btn;
    AtomicBoolean atomicBoolean = new AtomicBoolean();
    EditText text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broker);
        intView();
        intEvent();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(BrokerActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //触摸的是EditText并且当前EditText可以滚动则将事件交给EditText处理；否则将事件交由其父类处理
        if ((view.getId() == R.id.editTextTextMultiLine && canVerticalScroll(text))) {
            view.getParent().requestDisallowInterceptTouchEvent(true);
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
        }
        return false;
    }


    /**
     * EditText竖直方向是否可以滚动
     * @param editText  需要判断的EditText
     * @return  true：可以滚动   false：不可以滚动
     */
    private boolean canVerticalScroll(EditText editText) {
        //滚动的距离
        int scrollY = editText.getScrollY();
        //控件内容的总高度
        int scrollRange = editText.getLayout().getHeight();
        //控件实际显示的高度
        int scrollExtent = editText.getHeight() - editText.getCompoundPaddingTop() -editText.getCompoundPaddingBottom();
        //控件内容总高度与实际显示高度的差值
        int scrollDifference = scrollRange - scrollExtent;

        if(scrollDifference == 0) {
            return false;
        }

        return (scrollY > 0) || (scrollY < scrollDifference - 1);
    }


    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        btn = findViewById(R.id.button);
        text = findViewById(R.id.editTextTextMultiLine);
    }

    private void intEvent() {
        text.setOnTouchListener(this);
        navigationHelper = new NavigationHelper(menu, action_bar, this,notificationBtn);

        List<String> keywords = Arrays.asList("contract", "require", "emit", "function","returns","address","struct","mapping","payable","false","true","event","constructor");
        int keywordColor = Color.parseColor("#FF8C00"); // 深橙色
        int defaultColor = Color.BLACK; // 黑色

        String code = "contract Broker {\n" +
                "    // define broker struct\n" +
                "    struct Broker {\n" +
                "        address brokerAddress;\n" +
                "        uint256 stakeAmount;\n" +
                "        bool isActive;\n" +
                "    }\n" +
                "\n" +
                "    // a map to store all brokers\n" +
                "    mapping(address => Broker) public brokers;\n" +
                "\n" +
                "    // B2E protocol address\n" +
                "    address public B2EAddr;\n" +
                "\n" +
                "    // event to record the register and deregister of broker\n" +
                "    event BrokerRegistered(address indexed brokerAddress);\n" +
                "    event BrokerDeregistered(address indexed brokerAddress);\n" +
                "\n" +
                "    // B2E manager initialize the B2E address\n" +
                "    constructor() {\n" +
                "        B2EAddr = msg.sender;\n" +
                "    }\n" +
                "\n" +
                "    // user apply to become broker\n" +
                "    function requestBroker() external payable {\n" +
                "        //require not a breoker before\n" +
                "        require(brokers[msg.sender].isActive == false, \"Address is already a broker\");\n" +
                "\n" +
                "        // update broker status\n" +
                "        brokers[msg.sender] = Broker({\n" +
                "            brokerAddress: msg.sender,\n" +
                "            collateralAmount: 0,\n" +
                "            isActive: true\n" +
                "        });\n" +
                "\n" +
                "        // emit the register event\n" +
                "        emit BrokerRegistered(msg.sender);\n" +
                "    }\n" +
                "\n" +
                "    // withdraw broker\n" +
                "    function deregisterBroker(address _brokerAddress, uint256 _collateralAmount) external {\n" +
                "        //only the B2E manager can invoke this function\n" +
                "        require(msg.sender == B2EAddr, \"Only the B2E manager can call this function\");\n" +
                "        Broker storage broker = brokers[_brokerAddress];\n" +
                "\n" +
                "        require(broker.isActive, \"Address is not a broker\");\n" +
                "        require(broker.stakeAmount == _collateralAmount, \"Collateral amount does not match\");\n" +
                "\n" +
                "\n" +
                "        // update broker status\n" +
                "        broker.isActive = false;\n" +
                "\n" +
                "        // transfer the principal and profit from B2E to user\n" +
                "        _brokerAddress.call{value: _collateralAmount}(\"\");\n" +
                "\n" +
                "        // emit the broker deregister event\n" +
                "        emit BrokerDeregistered(_brokerAddress);\n" +
                "    }\n" +
                "\n" +
                "    // get broker information\n" +
                "    function getBrokerInfo(address _brokerAddress) external view returns (address, uint256, bool) {\n" +
                "        Broker storage broker = brokers[_brokerAddress];\n" +
                "        return (broker.brokerAddress, broker.stakeAmount, broker.isActive);\n" +
                "    }\n" +
                "}";

        SpannableString spannableString = new SpannableString(code);
        for (int i = 0; i < code.length(); ) {
            // 查找下一个空格或代码结束的位置
            int end = code.indexOf(' ', i);
            int end2 = code.indexOf("(",i);
            if(end2 == -1){
                end2 = 9999;
            }
            int end3 = code.indexOf("{",i);
            if(end3 == -1){
                end3 = 9999;
            }
            int end4 = code.indexOf(".",i);
            if(end4 == -1){
                end4 = 9999;
            }
            end = Math.min(Math.min(end,end2),Math.min(end3,end4));
            if (end == -1) {
                end = code.length();
            }
            String word = code.substring(i, end).trim();
            if (keywords.contains(word)) {
                spannableString.setSpan(new ForegroundColorSpan(keywordColor), i, i + word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            i = end + 1;
            while (i < code.length() && Character.isWhitespace(code.charAt(i))) {
                i++;
            }
        }



        text.setText(spannableString);


        new Thread(()->{
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
                String s = MyUtil.queryisbroker(privatekey);
                if (s!=null){
                    try {
                        JSONObject jsonResponse = new JSONObject(s);

                        String is_broker = jsonResponse.getString("is_broker");
                        if ("true".equals(is_broker)) {
                            atomicBoolean.set(true);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btn.setText("View My Profit");
                                }
                            });
                        } else {
                            atomicBoolean.set(false);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btn.setText("Apply to Become Broker");
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

        }).start();


        btn.setOnClickListener(view -> {
            //创建意图对象
            Intent intent = new Intent();
            if (atomicBoolean.get()) {
                intent.setClass(BrokerActivity.this, AfterBrokerActivity.class);
                startActivity(intent);
            } else {
                new Thread(()->{
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
                        String s = MyUtil.applybroker(privatekey);
                        if (s!=null &&s.contains("成功")){
                            runOnUiThread(()->{
                                Toast.makeText(BrokerActivity.this,"申请成为Broker成功！",Toast.LENGTH_LONG).show();
                                intent.setClass(BrokerActivity.this, StakeMoreActivity.class);
                                intent.putExtra("extra_data", "applysuccess");
                                startActivity(intent);
                            });
                        }else {
                            runOnUiThread(()->{
                                Toast.makeText(BrokerActivity.this,"申请成为Broker失败，请稍后重试",Toast.LENGTH_LONG).show();
                            });
                        }
                    }else {
                        runOnUiThread(()->{
                            Toast.makeText(BrokerActivity.this,"申请成为Broker失败，请稍后重试",Toast.LENGTH_LONG).show();
                        });
                    }

                }).start();


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