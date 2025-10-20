package com.example.brokerfi.xc;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class NewsActivity extends AppCompatActivity {

//    private ImageView menu;
//    private RelativeLayout action_bar;
//    private NavigationHelper navigationHelper;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        intView();
        intEvent();
        webView = findViewById(R.id.webview);

        // 启用 JavaScript（可选，如果网站需要）
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // 设置 WebViewClient，确保在 App 内打开页面，而不是调用外部浏览器
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
//                if (true) {
//                    return;
//                }
                if (!request.isForMainFrame()) {
                    return;
                }

                    String customErrorHtml = "<html>" +
                        "<head>" +
                        "<meta charset='utf-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<title>离线</title>" +
                        "</head>" +
                        "<body style='font-family: sans-serif; text-align: center; margin-top: 40vh; color: #555;'>" +
                        "<h3>网络连接失败</h3>" +
                        "<p>请检查网络设置</p>" +
//                        "<button onclick='window.location.reload()'>重新加载</button>" +
                        "</body>" +
                        "</html>";

                // ✅ 使用 loadDataWithBaseURL
                view.loadDataWithBaseURL("file:///android_asset/", customErrorHtml, "text/html", "utf-8", null);
                Toast.makeText(NewsActivity.this, "网络连接失败", Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent();
//                intent.setClass(NewsActivity.this, MainActivity.class);
//                startActivity(intent);
                if (true) {
                    return;
                }

//                String customErrorHtml = "<html><head><meta charset='utf-8'>" +
//                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
//                        "<title>网络错误</title>" +
//                        "</head><body style='font-family: sans-serif; text-align: center; margin-top: 40vh; color: #555;'>" +
//                        "<h3>网络连接失败</h3>" +
//                        "<p>请检查网络设置</p>" +
//                        "<button onclick='window.location.reload()'>重新加载</button>" +
//                        "</body></html>";
//                view.loadData(customErrorHtml, "text/html", "utf-8");
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 确保页面内跳转也在 WebView 中打开
                return false;
            }
        });

        // 加载指定网站
        webView.loadUrl("https://academic.broker-chain.com:444/news");
    }
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void intView() {
//        menu = findViewById(R.id.menu);
//        action_bar = findViewById(R.id.action_bar);
    }

    private void intEvent(){
//        navigationHelper = new NavigationHelper(menu, action_bar,this,notificationBtn);

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