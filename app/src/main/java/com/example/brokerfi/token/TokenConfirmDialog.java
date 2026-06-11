package com.example.brokerfi.token;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;

public final class TokenConfirmDialog {

    public interface OnConfirmListener {
        void onConfirm();
    }

    private TokenConfirmDialog() {
    }

    public static void show(AppCompatActivity activity, String title, String message,
                            OnConfirmListener listener) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(R.layout.dialog_token_confirm)
                .create();

        dialog.setOnShowListener(d -> {
            TextView titleView = dialog.findViewById(R.id.token_dialog_title);
            TextView messageView = dialog.findViewById(R.id.token_dialog_message);
            Button cancelBtn = dialog.findViewById(R.id.token_dialog_cancel);
            Button confirmBtn = dialog.findViewById(R.id.token_dialog_confirm);

            if (titleView != null) {
                titleView.setText(title);
            }
            if (messageView != null) {
                messageView.setText(message);
            }
            if (cancelBtn != null) {
                cancelBtn.setOnClickListener(v -> dialog.dismiss());
            }
            if (confirmBtn != null) {
                confirmBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (listener != null) {
                        listener.onConfirm();
                    }
                });
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.86f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setDimAmount(0.45f);
        }

        dialog.setCancelable(true);
        dialog.show();
    }
}


