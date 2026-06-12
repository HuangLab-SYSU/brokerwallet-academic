package com.example.brokerfi.token;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;

/** Token sub-header helper: opens contract info for the currently selected token. */
public final class TokenSubHeaderHelper {

    private TokenSubHeaderHelper() {
    }

    public static void bindInfoButton(AppCompatActivity activity) {
        if (activity == null) {
            return;
        }
        ImageView infoBtn = activity.findViewById(R.id.token_info_button);
        if (infoBtn == null) {
            return;
        }
        if (activity instanceof TokenInfoActivity
                || activity instanceof TokenSwapActivity
                || activity instanceof TokenManageActivity
                || activity instanceof TokenAddActivity) {
            infoBtn.setVisibility(View.GONE);
            return;
        }
        infoBtn.setVisibility(View.VISIBLE);
        infoBtn.setOnClickListener(v -> {
            TokenItem item = TokenSelection.getSelected(activity);
            Intent intent = new Intent(activity, TokenInfoActivity.class);
            if (item != null) {
                intent.putExtra(TokenInfoActivity.EXTRA_CONTRACT, item.getContractAddress());
                intent.putExtra(TokenInfoActivity.EXTRA_NAME, item.getName());
                intent.putExtra(TokenInfoActivity.EXTRA_SYMBOL, item.getSymbol());
                intent.putExtra(TokenInfoActivity.EXTRA_BUILT_IN, item.isBuiltIn());
            }
            activity.startActivity(intent);
        });
    }
}
