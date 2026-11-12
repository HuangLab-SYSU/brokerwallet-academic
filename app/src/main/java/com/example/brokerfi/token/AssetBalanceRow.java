package com.example.brokerfi.token;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.brokerfi.R;
import com.example.brokerfi.swap.SwapQuoteMath;

/** Shared balance row for the home summary and the full asset list. */
public final class AssetBalanceRow {
    private AssetBalanceRow() {
    }

    public static void show(View row, TokenItem token, @Nullable String balance) {
        TextView nameView = row.findViewById(R.id.asset_row_name);
        TextView balanceView = row.findViewById(R.id.asset_row_balance);
        nameView.setText(token.getSymbol());
        if (balance == null || balance.isEmpty()) {
            balanceView.setText(R.string.wallet_balance_unavailable);
        } else {
            balanceView.setText(row.getContext().getString(
                    R.string.wallet_asset_balance, balance, token.getSymbol()));
        }
    }

    @Nullable
    public static String cachedBalance(Context context, String wallet, TokenItem token) {
        TokenBalanceCache.Snapshot snapshot =
                TokenBalanceCache.resolveSnapshot(context, wallet, token.getContractAddress());
        if (snapshot != null) {
            return SwapQuoteMath.format(snapshot.tokenBalance, token.getDecimals());
        }
        return token.isNative() && TokenBalanceCache.hasNativeBalance(wallet)
                ? TokenBalanceCache.getNativeBalanceDisplay(wallet) : null;
    }
}
