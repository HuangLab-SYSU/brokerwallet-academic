package com.example.brokerfi.xc;

import com.example.brokerfi.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class AboutActivity extends AppCompatActivity {

    public static String VersionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView textVersion = findViewById(R.id.text_version);
        Button btnCheckUpdate = findViewById(R.id.btn_check_update);

        // 显示版本号
        String versionName = getAppVersionName();
        VersionName = versionName;
        textVersion.setText("Version " + versionName);

        // 检查更新按钮点击事件
        btnCheckUpdate.setOnClickListener(v -> {
            // 这里可以实现检查更新逻辑
            // 例如：请求服务器版本号，对比本地版本
            Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show();


                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<String> sv = new AtomicReference<>();
                new Thread(()->{
                    try {
//                        byte[] bytes = HTTPUtil.doPost2("https://academic.broker-chain.com/appversion", null);
                        byte[] bytes = HTTPUtil.doPost2("http://academic.broker-chain.com/appversion", null);
                        sv.set(new String(bytes));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    latch.countDown();
                }).start();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                String sve = sv.get();
                if(sve == null){
                    Toast.makeText(this, "Failed to get the newest version.", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject j = null;
                try {
                    j = new JSONObject(sve);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                try {
                    String v1 = (String) j.get("data");
                    if(v1.equals(VersionName)){
                        Toast.makeText(this, "Your APP is already the latest version.", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(this, "Update available: Version "+ v1+", please visit the GitHub to update.", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }




            // 示例：跳转到应用市场（可替换为你的包名）
//            openAppInPlayStore();
        });
    }

    // 获取 App 版本号
    private String getAppVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    // 打开应用在 Play 商店的页面（可用于更新）
    private void openAppInPlayStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            // Play 商店未安装，跳转网页
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }
}