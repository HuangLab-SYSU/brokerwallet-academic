package com.example.brokerfi.token;

import com.example.brokerfi.xc.ChainAddressUtil;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 閸樺棗褰舵禍銈嗘鐠囷附鍎忓鍦崶閿涘牏绨跨粻鈧€涙顔岄敍澶堚偓?*/
public final class TokenTxDetailDialog {

    private TokenTxDetailDialog() {
    }

    public static void show(
            AppCompatActivity activity,
            TokenTxRecord record,
            String walletAddress,
            String contractAddress) {
        if (activity == null || activity.isFinishing() || record == null) {
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(R.layout.dialog_token_tx_detail)
                .create();

        dialog.setOnShowListener(d -> {
            TextView typeView = dialog.findViewById(R.id.token_tx_dialog_type);
            TextView amountView = dialog.findViewById(R.id.token_tx_dialog_amount);
            View swapAmountRow = dialog.findViewById(R.id.token_tx_dialog_swap_amount);
            TextView swapFromView = dialog.findViewById(R.id.token_tx_dialog_swap_from);
            TextView swapToView = dialog.findViewById(R.id.token_tx_dialog_swap_to);
            TextView timeView = dialog.findViewById(R.id.token_tx_dialog_time);
            TextView toLabel = dialog.findViewById(R.id.token_tx_dialog_to_label);
            TextView toView = dialog.findViewById(R.id.token_tx_dialog_to);
            TextView hashView = dialog.findViewById(R.id.token_tx_dialog_hash);
            TextView hashHint = dialog.findViewById(R.id.token_tx_dialog_hash_copy_hint);
            Button closeBtn = dialog.findViewById(R.id.token_tx_dialog_close);

            TokenTxDisplayHelper.bindType(activity, typeView, record);
            TokenTxDisplayHelper.bindAmount(
                    activity,
                    amountView,
                    swapAmountRow,
                    swapFromView,
                    swapToView,
                    record);
            if (timeView != null) {
                SimpleDateFormat format =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                timeView.setText(format.format(new Date(record.timestampMs)));
            }

            if (!TextUtils.isEmpty(record.detail) && toLabel != null && toView != null) {
                toLabel.setVisibility(View.VISIBLE);
                toView.setVisibility(View.VISIBLE);
                toView.setText(ChainAddressUtil.displayAddress(record.detail));
            }

            if (hashView != null) {
                if (TextUtils.isEmpty(record.txHash)) {
                    hashView.setText("-");
                    if (hashHint != null) {
                        hashHint.setVisibility(View.GONE);
                    }
                    hashView.setOnClickListener(null);
                } else {
                    hashView.setText(record.txHash);
                    Runnable copy = () -> copyHash(activity, record.txHash);
                    hashView.setOnClickListener(v -> copy.run());
                    if (hashHint != null) {
                        hashHint.setVisibility(View.VISIBLE);
                        hashHint.setOnClickListener(v -> copy.run());
                    }
                }
            }

            if (closeBtn != null) {
                closeBtn.setOnClickListener(v -> dialog.dismiss());
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int maxWidthPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    320,
                    activity.getResources().getDisplayMetrics());
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            int width = Math.min(maxWidthPx, (int) (screenWidth * 0.82f));
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setDimAmount(0.45f);
        }
        dialog.setCancelable(true);
        dialog.show();
    }

    private static void copyHash(AppCompatActivity activity, String hash) {
        ClipboardManager clipboard =
                (ClipboardManager) activity.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("wBKC tx hash", hash));
        }
        Toast.makeText(activity, R.string.token_tx_hash_copied, Toast.LENGTH_SHORT).show();
    }
}


