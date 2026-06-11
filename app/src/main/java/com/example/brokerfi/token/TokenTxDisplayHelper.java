package com.example.brokerfi.token;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.example.brokerfi.R;
import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;

/** 浜ゆ槗鍘嗗彶鍒楄〃 / 璇︽儏涓殑閲戦灞曠ず锛坰wap 浣跨敤 {@code youjiantou} 绠ご锛夈€?*/
public final class TokenTxDisplayHelper {

    private TokenTxDisplayHelper() {
    }

    public static void bindType(Context context, TextView typeText, TokenTxRecord record) {
        if (context == null || typeText == null || record == null) {
            return;
        }
        typeText.setVisibility(View.VISIBLE);
        typeText.setText(typeLabel(context, record));
    }

    public static String typeLabel(Context context, TokenTxRecord record) {
        if (context == null || record == null || record.type == null) {
            return "";
        }
        if (TokenTxRecord.isSwapType(record.type)) {
            if (isWrapRecord(record)) {
                return context.getString(R.string.token_tx_type_wrap);
            }
            if (isUnwrapRecord(record)) {
                return context.getString(R.string.token_tx_type_unwrap);
            }
            return context.getString(R.string.token_tx_type_swap);
        }
        if (TokenTxRecord.TYPE_SEND.equals(record.type)) {
            return context.getString(R.string.token_tx_type_send);
        }
        if (TokenTxRecord.TYPE_RECEIVE.equals(record.type)) {
            return context.getString(R.string.token_tx_type_receive);
        }
        return record.type;
    }

    public static void bindAmount(
            Context context,
            TextView simpleAmountView,
            View swapAmountRow,
            TextView swapFromView,
            TextView swapToView,
            TokenTxRecord record) {
        bindAmount(context, simpleAmountView, swapAmountRow, swapFromView, swapToView, record, true);
    }

    public static void bindAmount(
            Context context,
            TextView simpleAmountView,
            View swapAmountRow,
            TextView swapFromView,
            TextView swapToView,
            TokenTxRecord record,
            boolean showDetailedSwapAmounts) {
        if (context == null || record == null || simpleAmountView == null) {
            return;
        }
        if (record.isSwap()
                && showDetailedSwapAmounts
                && swapAmountRow != null
                && swapFromView != null
                && swapToView != null) {
            simpleAmountView.setVisibility(View.GONE);
            swapAmountRow.setVisibility(View.VISIBLE);
            swapFromView.setText(legLabel(record.amountDisplay, record.resolveFromSymbol()));
            swapToView.setText(legLabel(record.amountDisplay, record.resolveToSymbol()));
            return;
        }
        if (swapAmountRow != null) {
            swapAmountRow.setVisibility(View.GONE);
        }
        simpleAmountView.setVisibility(View.VISIBLE);
        simpleAmountView.setText(simpleAmountLabel(context, record, showDetailedSwapAmounts));
    }

    public static String simpleAmountLabel(Context context, TokenTxRecord record) {
        return simpleAmountLabel(context, record, true);
    }

    public static String simpleAmountLabel(
            Context context,
            TokenTxRecord record,
            boolean showDetailedSwapAmounts) {
        if (context == null || record == null) {
            return "";
        }
        if (!showDetailedSwapAmounts && record.isSwap()) {
            return legLabel(record.amountDisplay, record.resolveFromSymbol());
        }
        String wrappedBkcContract = wrappedBkcContractHelper.resolveContractAddress(context);
        String contract = TextUtils.isEmpty(record.contractAddress)
                ? wrappedBkcContract
                : record.contractAddress;
        String symbol = TokenStore.resolveSymbol(context, contract, wrappedBkcContract);
        return record.amountDisplay + " " + symbol;
    }

    private static String legLabel(String amount, String symbol) {
        return amount + " " + symbol;
    }

    private static boolean isWrapRecord(TokenTxRecord record) {
        return TokenConfig.NATIVE_SYMBOL.equalsIgnoreCase(record.resolveFromSymbol())
                && "wBKC".equalsIgnoreCase(record.resolveToSymbol());
    }

    private static boolean isUnwrapRecord(TokenTxRecord record) {
        return "wBKC".equalsIgnoreCase(record.resolveFromSymbol())
                && TokenConfig.NATIVE_SYMBOL.equalsIgnoreCase(record.resolveToSymbol());
    }
}


