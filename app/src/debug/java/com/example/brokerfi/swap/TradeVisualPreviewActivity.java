package com.example.brokerfi.swap;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** Empty debug-only host for isolated visual tests. No wallet, signing or network access. */
public class TradeVisualPreviewActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
