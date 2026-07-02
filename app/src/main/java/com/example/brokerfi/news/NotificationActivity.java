package com.example.brokerfi.news;

import static com.example.brokerfi.core.config.ApiConfig.API_NOTIFICATION_NEWS2;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.example.brokerfi.core.config.ApiConfig;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.example.brokerfi.send.SendActivity;


public class NotificationActivity extends AppCompatActivity {

    // Notification function


//    private ImageView menu;
//    private RelativeLayout action_bar;
//    private NavigationHelper navigationHelper;
    private WebView webView;


    private boolean networkErrorShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        intView();
        intEvent();
        webView = findViewById(R.id.webview);


        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);


        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!request.isForMainFrame()) {
                    return;
                }

                String customErrorHtml = "<html>" +
                        "<head>" +
                        "<meta charset='utf-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<title>" + getString(R.string.webview_offline_title) + "</title>" +
                        "</head>" +
                        "<body style='font-family: sans-serif; text-align: center; margin-top: 40vh; color: #555;'>" +
                        "<h3>" + getString(R.string.webview_offline_heading) + "</h3>" +
                        "<p>" + getString(R.string.webview_offline_message) + "</p>" +
                        "</body>" +
                        "</html>";


                view.loadDataWithBaseURL("file:///android_asset/", customErrorHtml, "text/html", "utf-8", null);


                if (!networkErrorShown) {
                    Toast.makeText(NotificationActivity.this, R.string.emulator_toast_network_failed, Toast.LENGTH_SHORT).show();
                    networkErrorShown = true;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                return false;
            }
        });

        webView.loadUrl(API_NOTIFICATION_NEWS2);
    }
    /// ////////
    @Override
    public void onBackPressed() {
        // When an error page is displayed, exit the activity directly.
        if (networkErrorShown) {
            super.onBackPressed();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    /// ////////
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
