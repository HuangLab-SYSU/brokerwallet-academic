package com.example.brokerfi.swap;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.brokerfi.token.TokenTxHistoryStore;
import com.example.brokerfi.token.TokenTxRecord;

/** Reuses the durable journal. Opening a row may query its hash, but never resends it. */
final class TradeHistory {
    private final AppCompatActivity activity;
    private final Button toggle;
    private final LinearLayout rows;
    private final boolean liquidity;
    private boolean expanded;
    TradeHistory(AppCompatActivity activity,int toggleId,int rowsId,boolean liquidity) {
        this.activity=activity; this.liquidity=liquidity;
        toggle=activity.findViewById(toggleId); rows=activity.findViewById(rowsId);
        toggle.setVisibility(View.VISIBLE);
    }
    void bind(BrokerSwapConfig config,String wallet,Runnable changed) {
        toggle.setOnClickListener(v -> { expanded=!expanded; refresh(config,wallet,changed); });
        refresh(config,wallet,changed);
    }
    private void refresh(BrokerSwapConfig config,String wallet,Runnable changed) {
        java.util.List<TokenTxRecord> records=new java.util.ArrayList<>();
        for(TokenTxRecord record:TokenTxHistoryStore.getAll(activity,wallet))
            if(record.status!=null && record.isLiquidity()==liquidity
                    && !(VideoTradePresentation.approval(record) && "SUCCESS".equals(record.status)))records.add(record);
        toggle.setText(liquidity?"LP History":"Swap History");
        toggle.setCompoundDrawablesWithIntrinsicBounds(0,0,expanded?com.example.brokerfi.R.drawable.video_arrow_up:com.example.brokerfi.R.drawable.video_arrow_down,0);
        rows.setVisibility(expanded?View.VISIBLE:View.GONE); rows.removeAllViews();
        if (!expanded) return;
        int count=0;
        for (TokenTxRecord r:records) {
            count++;
            LinearLayout row=recordCard(activity,r,v -> showRecord(activity,config,wallet,r,changed));
            LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);
            rows.addView(row,rowParams);
        }
        if (count==0) { TextView empty=TradeUi.text(activity,"No transactions yet",12,0xff9CA3AF,false); rows.addView(empty); }
    }
    static LinearLayout recordCard(Context context,TokenTxRecord r,View.OnClickListener open) {
        return TradeVideoUi.historyRow(context,r,open);
    }
    static void showRecord(AppCompatActivity activity,BrokerSwapConfig config,String wallet,TokenTxRecord r,Runnable changed) {
        AlertDialog.Builder dialog=TradeVideoUi.recordBuilder(activity,r);
        if(!SwapReceipt.validHash(r.txHash) && r.sendRequestId!=null && r.sendRequestId.matches("[0-9a-fA-F-]{36}"))
            dialog.setNeutralButton("Copy request ID",(d,w) -> {
                ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("Gateway request ID (not a transaction hash)",r.sendRequestId));
            });
        if(TokenTxHistoryStore.isUnresolvedSwap(r) && SwapReceipt.validHash(r.txHash) && config!=null)
            dialog.setPositiveButton("Check result (no resend)",(d,w) -> {
                SwapOperationRunner.resume(activity,config,wallet,r,record -> { if(!activity.isDestroyed())changed.run(); }); changed.run();
            });
        else if(TradeRecoveryPolicy.canAcceptRisk(r,SwapOperationRunner.active(wallet)) && !TradeRecoveryPolicy.deferred(r))
            dialog.setPositiveButton("Start a new transaction...",(d,w) -> new AlertDialog.Builder(activity)
                    .setTitle("Review the risk of continuing")
                    .setMessage("A missing transaction hash does not mean the previous request was not sent. It may execute later, causing duplicate spending or extra network fees. Continuing will not cancel or resend that request, or mark its outcome as failed. Keep the lock and continue checking if you do not accept this risk.")
                    .setNegativeButton("Keep locked",null).setPositiveButton("Accept risk and continue",(d2,w2) -> {
                        if(!wallet.equalsIgnoreCase(BrokerSwapConfig.walletId(com.example.brokerfi.token.TokenWalletHelper.getWalletAddress(activity)))) {
                            Toast.makeText(activity,"Account changed. The record was not modified.",Toast.LENGTH_LONG).show();return;
                        }
                        long previous=r.continuationRiskAcceptedMs;
                        if(!TradeRecoveryPolicy.acceptRisk(r,SwapOperationRunner.active(wallet),System.currentTimeMillis()))return;
                        if(!TokenTxHistoryStore.upsertSwap(activity,wallet,r)) {
                            r.continuationRiskAcceptedMs=previous;
                            TokenTxHistoryStore.upsertSwap(activity,wallet,r);
                            Toast.makeText(activity,"Could not save. The lock remains in place.",Toast.LENGTH_LONG).show();
                        }
                        changed.run();
                    }).show());
        TradeVideoUi.show(dialog.create());
    }
}
