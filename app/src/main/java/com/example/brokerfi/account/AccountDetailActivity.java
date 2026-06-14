package com.example.brokerfi.account;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.example.brokerfi.main.MainActivity;


public class AccountDetailActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private TextView accountNameTextView;
    private ImageView qrCodeImageView;
    private TextView addressTextView;
    private Button copyAddressBtn;
    private Button showPrivateKeyBtn;
    private NavigationHelper navigationHelper;
    private int qrcode_height;
    boolean hasExecuted = false;
    private String accountIndex;
    private String accountAddress;
    private String accountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);

        accountIndex = getIntent().getStringExtra("accountIndex");
        accountAddress = getIntent().getStringExtra("accountAddress");
        accountName = getIntent().getStringExtra("accountName");

        intView();
        intEvent();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        accountNameTextView = findViewById(R.id.accountName);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        addressTextView = findViewById(R.id.addressTextView);
        copyAddressBtn = findViewById(R.id.copyAddressBtn);
        showPrivateKeyBtn = findViewById(R.id.showPrivateKeyBtn);
        // Get wallet logo view
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(AccountDetailActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);
        accountNameTextView.setText(accountName);
        addressTextView.setText(accountAddress);

        ViewTreeObserver vto = qrCodeImageView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(!hasExecuted){
                    qrCodeImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    qrcode_height = qrCodeImageView.getWidth();
                    hasExecuted = true;
                    generateQRCode();
                }
            }
        });

        copyAddressBtn.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Account Address", accountAddress);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(AccountDetailActivity.this, R.string.account_detail_toast_address_copied, Toast.LENGTH_SHORT).show();
        });

        showPrivateKeyBtn.setOnClickListener(view -> {
            Intent intent = new Intent(AccountDetailActivity.this, ConfirmPasswordActivity.class);
            intent.putExtra("accountIndex", accountIndex);
            startActivity(intent);
        });
    }

    private void generateQRCode() {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(accountAddress, BarcodeFormat.QR_CODE, qrcode_height, qrcode_height);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(matrix);
            qrCodeImageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
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
