package com.example.brokerfi.account;

import androidx.appcompat.app.AppCompatActivity;

// syh_work_01 new: pop-up for changing nickname
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
// syh_work_01 new: textbox for changing nickname
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
// syh_work_01 new: read & write nicknames
import com.example.brokerfi.core.storage.StorageUtil;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.example.brokerfi.main.MainActivity;

// syh_work_02 new: avatar upload & display
import android.net.Uri;
import android.provider.MediaStore;
import android.util.TypedValue;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.brokerfi.core.util.AvatarUtil;


public class AccountDetailActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private TextView accountNameTextView;
    private ImageView qrCodeImageView;
    private TextView addressTextView;
    private Button copyAddressBtn;
    private Button showPrivateKeyBtn;
    // syh_work_01 new: button for changing nickname
    private Button renameAccountBtn;
    // syh_work_02 new: avatar view & change-avatar button
    private ImageView avatarImageView;
    private Button changeAvatarBtn;
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
        // syh_work_01 new
        renameAccountBtn = findViewById(R.id.renameAccountBtn);
        // syh_work_02 new
        avatarImageView = findViewById(R.id.avatarImageView);
        changeAvatarBtn = findViewById(R.id.changeAvatarBtn);
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
        // syh_work_01 replace: nickname first, default name as fallback
        // accountNameTextView.setText(accountName);
        String nickname = StorageUtil.getAccountNickname(this, accountAddress);
        accountNameTextView.setText(nickname != null ? nickname : accountName);
        // syh_work_02 new: show avatar (custom file first, else generated identicon)
        int avatarSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        avatarImageView.setImageBitmap(AvatarUtil.loadAvatar(this, accountAddress, avatarSize));
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

        // syh_work_01 new: button
        renameAccountBtn.setOnClickListener(view -> showRenameDialog());

        // syh_work_02 new: change avatar button
        changeAvatarBtn.setOnClickListener(view -> showAvatarDialog());
    }

    // syh_work_01 new: name changing method
    private void showRenameDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.activity_account_detail_rename_hint);
        String current = StorageUtil.getAccountNickname(this, accountAddress);
        input.setText(current != null ? current : accountName);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
            .setTitle(R.string.activity_account_detail_rename_title)
            .setView(input)
            .setPositiveButton(R.string.activity_account_detail_rename_save, (dialog, which) -> {
                String nickname = input.getText().toString().trim();        // read
                if (nickname.isEmpty()) {                                   // judge：null ?
                    Toast.makeText(this, R.string.activity_account_detail_rename_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (nickname.length() > 24) {                               // judge：length limited
                    Toast.makeText(this, R.string.activity_account_detail_rename_too_long, Toast.LENGTH_SHORT).show();
                    return;
                }
                StorageUtil.saveAccountNickname(this, accountAddress, nickname);  // write
                accountNameTextView.setText(nickname);
                Toast.makeText(this, R.string.activity_account_detail_rename_saved, Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton(R.string.activity_account_detail_rename_reset, (dialog, which) -> {
                StorageUtil.removeAccountNickname(this, accountAddress);     // erase
                accountNameTextView.setText(accountName);                    // fallback to default name
                Toast.makeText(this, R.string.activity_account_detail_rename_reset_done, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    // syh_work_02 new: avatar change dialog (choose from gallery / reset to default)
    private void showAvatarDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.activity_account_detail_avatar_title)
            .setItems(new CharSequence[]{
                getString(R.string.activity_account_detail_avatar_choose),
                getString(R.string.activity_account_detail_avatar_reset)
            }, (dialog, which) -> {
                if (which == 0) {
                    galleryLauncher.launch("image/*");
                } else {
                    AvatarUtil.removeAvatar(this, accountAddress);
                    refreshAvatar();
                    Toast.makeText(this, R.string.activity_account_detail_avatar_reset_done, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void refreshAvatar() {
        int avatarSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        avatarImageView.setImageBitmap(AvatarUtil.loadAvatar(this, accountAddress, avatarSize));
    }

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null) {
                startImageCrop(uri);
            }
        }
    );

    private void startImageCrop(Uri sourceUri) {
        try {
            Bitmap original = MediaStore.Images.Media.getBitmap(getContentResolver(), sourceUri);
            int w = original.getWidth();
            int h = original.getHeight();
            int size = Math.min(w, h);
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            Bitmap cropped = Bitmap.createBitmap(original, x, y, size, size);
            Bitmap scaled = Bitmap.createScaledBitmap(cropped, 512, 512, true);
            AvatarUtil.saveAvatar(this, accountAddress, scaled);
            refreshAvatar();
            Toast.makeText(this, R.string.activity_account_detail_avatar_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.activity_account_detail_avatar_failed, Toast.LENGTH_SHORT).show();
        }
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
