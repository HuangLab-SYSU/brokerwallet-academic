package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;

public class ShowPrivateKeyActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private TextView privateKeyTextView;
    private Button revealButton;
    private Button copyButton;
    private NavigationHelper navigationHelper;
    private String accountIndex;
    private String privateKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_private_key);

        accountIndex = getIntent().getStringExtra("accountIndex");
        privateKey = getPrivateKeyFromStorage();

        intView();
        intEvent();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        privateKeyTextView = findViewById(R.id.privateKeyTextView);
        revealButton = findViewById(R.id.revealButton);
        copyButton = findViewById(R.id.copyButton);
        // Get wallet logo view
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(ShowPrivateKeyActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);

        revealButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                privateKeyTextView.setText(privateKey);
                copyButton.setVisibility(View.VISIBLE);
                return true;
            }
        });

        copyButton.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Private Key", privateKey);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(ShowPrivateKeyActivity.this, R.string.show_private_key_toast_copied, Toast.LENGTH_SHORT).show();
        });
    }

    private String getPrivateKeyFromStorage() {
        String account = StorageUtil.getPrivateKey(this);
        if (account != null) {
            String[] split = account.split(";");
            int index = Integer.parseInt(accountIndex);
            if (index >= 0 && index < split.length) {
                return split[index];
            }
        }
        return "";
    }

    @Override
    public void onBackPressed() {
        if (navigationHelper != null && navigationHelper.isPopupVisible()) {
            navigationHelper.hidePopup();
        } else {
            super.onBackPressed();
        }
    }
}