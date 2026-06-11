package com.example.brokerfi.token;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;

public final class TokenContractUiHelper {

    private TokenContractUiHelper() {
    }

    public static String resolveDisplayAddress(Context context) {
        String address = TokenSelection.getSelectedContract(context);
        if (TextUtils.isEmpty(address)) {
            address = wrappedBkcContractHelper.resolveContractAddress(context);
        }
        return TextUtils.isEmpty(address) ? null : address;
    }

    /**
     * 缂佹垵鐣鹃崥鍫㈠閸楋紕澧栭敍姘勾閸р偓 + 婢跺秴鍩楅幐澶愭尦閿涙硰@code compact} 娑?true 閺冨爼娈ｉ挊蹇氼嚛閺勫骸鑻熺紓鈺冪叚閸︽澘娼冪仦鏇犮仛閵?
     */
    public static void bindContractCard(
            AppCompatActivity activity,
            View root,
            boolean compact) {
        if (activity == null || root == null) {
            return;
        }
        TextView addressView = root.findViewById(R.id.token_contract_address_text);
        TextView copyBtn = root.findViewById(R.id.token_contract_copy_btn);
        TextView hintView = root.findViewById(R.id.token_contract_hint);
        if (addressView == null) {
            return;
        }

        if (hintView != null) {
            hintView.setVisibility(compact ? View.GONE : View.VISIBLE);
        }

        String address = resolveDisplayAddress(activity);
        if (TextUtils.isEmpty(address)) {
            addressView.setText(R.string.token_contract_missing_short);
            if (copyBtn != null) {
                copyBtn.setVisibility(View.GONE);
            }
            return;
        }

        addressView.setText(compact ? formatShortAddress(address) : address);
        if (copyBtn != null) {
            copyBtn.setVisibility(View.VISIBLE);
            copyBtn.setOnClickListener(v -> copyContractAddress(activity, address));
        }
        addressView.setOnClickListener(v -> copyContractAddress(activity, address));
    }

    public static String formatShortAddress(String address) {
        if (TextUtils.isEmpty(address) || address.length() < 14) {
            return address;
        }
        return address.substring(0, 10) + "..." + address.substring(address.length() - 8);
    }

    public static void copyContractAddress(Context context, String address) {
        if (context == null || TextUtils.isEmpty(address)) {
            return;
        }
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("wBKC contract", address));
        }
        Toast.makeText(context, R.string.token_address_copied, Toast.LENGTH_SHORT).show();
    }
}


