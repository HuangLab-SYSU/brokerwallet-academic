package com.example.brokerfi.swap;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.brokerfi.R;
import com.example.brokerfi.token.TokenTxRecord;

/** Native views matching the reference video. These views never read or submit transactions. */
public final class TradeVideoUi {
    private TradeVideoUi() {}
    private static final int INK=0xff111827, MUTED=0xff6B7280;
    private static int dp(Context c,int n){return TradeUi.dp(c,n);}
    private static LinearLayout column(Context c,int padding) {
        LinearLayout body=new LinearLayout(c);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(c,padding),dp(c,padding),dp(c,padding),dp(c,padding));return body;
    }
    private static TextView text(Context c,String s,int size,int color,boolean bold) {
        TextView t=TradeUi.text(c,s,size,color,bold);t.setLineSpacing(dp(c,2),1);return t;
    }
    private static void append(LinearLayout body,View v,int top) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(body.getContext(),top);body.addView(v,p);
    }
    private static void rounded(View v,int fill,int border,int radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(v.getContext(),radius));
        if(border!=0)d.setStroke(dp(v.getContext(),1),border);v.setBackground(d);
    }
    private static Button button(Context c,String title,boolean primary) {
        Button b=new Button(c);b.setText(title);b.setTextSize(14);b.setTypeface(null,Typeface.BOLD);b.setAllCaps(false);
        b.setMinHeight(dp(c,46));b.setMinimumHeight(dp(c,46));b.setPadding(dp(c,10),dp(c,10),dp(c,10),dp(c,10));
        b.setBackgroundTintList(null);b.setStateListAnimator(null);
        rounded(b,primary?Color.BLACK:Color.WHITE,primary?0:0xffD1D5DB,10);b.setTextColor(primary?Color.WHITE:INK);return b;
    }
    static View confirmationView(Context c,String title,String details) {
        LinearLayout body=column(c,20);rounded(body,Color.WHITE,0,12);
        append(body,text(c,title,20,INK,true),0);
        append(body,text(c,"Review the details below before submitting this on-chain transaction.",13,MUTED,false),8);
        append(body,text(c,"Transaction details",13,INK,true),16);
        TextView fields=text(c,details,15,INK,false);fields.setLineSpacing(dp(c,4),1);fields.setTextIsSelectable(true);int p=dp(c,16);fields.setPadding(p,p,p,p);
        rounded(fields,Color.WHITE,0xffD1D5DB,14);append(body,fields,8);
        append(body,text(c,"This action cannot be canceled after it is sent to the network.",12,0xff8B663F,false),12);
        LinearLayout actions=new LinearLayout(c);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel=button(c,"Cancel",false),confirm=button(c,"Confirm",true);
        cancel.setTag("video-cancel");confirm.setTag("video-confirm");
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1);left.rightMargin=dp(c,8);
        actions.addView(cancel,left);actions.addView(confirm,new LinearLayout.LayoutParams(0,-2,1));append(body,actions,16);
        return body;
    }
    static AlertDialog confirmation(Context c,String title,String details,DialogInterface.OnClickListener positive) {
        View content=confirmationView(c,title,details);
        ScrollView scroll=new ScrollView(c);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(c).setView(scroll).create();
        content.findViewWithTag("video-cancel").setOnClickListener(v -> dialog.dismiss());
        content.findViewWithTag("video-confirm").setOnClickListener(v -> {positive.onClick(dialog,DialogInterface.BUTTON_POSITIVE);dialog.dismiss();});
        return dialog;
    }
    static AlertDialog approval(Context c,String token,String amount,DialogInterface.OnClickListener positive) {
        return new AlertDialog.Builder(c).setTitle("Approve "+token)
                .setMessage("Allow the router to spend up to "+amount+" "+token+" from your wallet.\n\nThis is a one-time approval for this amount.")
                .setNegativeButton("Cancel",null).setPositiveButton("Confirm",positive).create();
    }
    static void showApproval(AlertDialog dialog) {
        dialog.show();
        TextView message=dialog.findViewById(android.R.id.message);
        if(message!=null)message.setTextSize(18);
        TextView title=dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if(title!=null)title.setTextSize(21);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextSize(18);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextSize(18);
    }
    public static View recordView(Context c,TokenTxRecord r) {
        LinearLayout body=column(c,18);rounded(body,Color.WHITE,0,12);
        TextView title=text(c,"Transaction details",17,INK,true);title.setGravity(Gravity.CENTER);append(body,title,0);
        append(body,text(c,"Type",12,MUTED,false),10);
        append(body,text(c,VideoTradePresentation.label(r),15,INK,false),2);
        TextView amounts=text(c,VideoTradePresentation.amounts(r),17,Color.BLACK,true);amounts.setGravity(Gravity.CENTER);amounts.setTextIsSelectable(true);append(body,amounts,10);
        TextView time=text(c,VideoTradePresentation.time(r,false),13,MUTED,false);time.setGravity(Gravity.CENTER);append(body,time,2);
        String status=VideoTradePresentation.status(r.status);
        if(!status.isEmpty())append(body,text(c,status,12,0xff991B1B,false),8);
        if(TradeRecoveryPolicy.deferred(r))append(body,text(c,"The previous outcome is still unknown. Continuation risk was accepted; the old request was not canceled or resent.",12,0xff991B1B,false),8);
        append(body,text(c,"Transaction hash",12,MUTED,false),12);
        TextView hash=text(c,SwapReceipt.validHash(r.txHash)?r.txHash:"Not available",12,INK,false);
        hash.setTag("video-hash");
        hash.setTypeface(Typeface.MONOSPACE);hash.setPadding(dp(c,10),dp(c,10),dp(c,10),dp(c,10));rounded(hash,0xffF3F4F6,0,12);
        View.OnClickListener copy=v -> {if(SwapReceipt.validHash(r.txHash)){
            ClipboardManager clipboard=(ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);
            if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("Transaction hash",r.txHash));
            Toast.makeText(c,"Transaction hash copied",Toast.LENGTH_SHORT).show();
        }};
        hash.setOnClickListener(copy);append(body,hash,2);
        if(SwapReceipt.validHash(r.txHash)) {TextView hint=text(c,"Tap hash to copy",12,0xff4285A5,false);hint.setOnClickListener(copy);append(body,hint,2);}
        Button close=button(c,"Confirm",true);close.setMinHeight(dp(c,42));close.setMinimumHeight(dp(c,42));rounded(close,Color.BLACK,0,24);close.setTag("video-close");append(body,close,16);
        return body;
    }
    public static AlertDialog.Builder recordBuilder(Context c,TokenTxRecord r) {
        ScrollView scroll=new ScrollView(c);scroll.addView(recordView(c,r));
        return new AlertDialog.Builder(c).setView(scroll);
    }
    public static void show(AlertDialog dialog) {
        dialog.show();
        if(dialog.getWindow()!=null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.4f);
            int width=dialog.getContext().getResources().getDisplayMetrics().widthPixels-dp(dialog.getContext(),24);
            dialog.getWindow().setLayout(width,-2);
            View close=dialog.getWindow().getDecorView().findViewWithTag("video-close");
            if(close!=null)close.setOnClickListener(v -> dialog.dismiss());
        }
    }
    static LinearLayout historyRow(Context c,TokenTxRecord r,View.OnClickListener open) {
        LinearLayout row=column(c,0);row.setPadding(0,dp(c,r.isLiquidity()?4:14),0,dp(c,r.isLiquidity()?0:14));
        if(r.isLiquidity()) {
            boolean add="ADD".equals(r.liquidityAction);
            append(row,text(c,(add?"+ ":"- ")+VideoTradePresentation.label(r)+"  ·  "+VideoTradePresentation.time(r,true),12,add?0xff36B992:0xffB85B68,false),0);
            append(row,text(c,VideoTradePresentation.amounts(r),11,MUTED,false),2);
            View line=new View(c);line.setBackgroundColor(0xffECEEF2);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(c,1));lp.topMargin=dp(c,4);row.addView(line,lp);
        } else {
            LinearLayout header=new LinearLayout(c);header.setGravity(Gravity.CENTER_VERTICAL);
            TextView label=text(c,VideoTradePresentation.label(r),16,INK,true);
            header.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            TextView amount=text(c,VideoTradePresentation.amount(r.amountDisplay)+" "+r.fromSymbol,16,INK,true);amount.setGravity(Gravity.END);header.addView(amount,new LinearLayout.LayoutParams(0,-2,1));
            append(row,header,0);append(row,text(c,VideoTradePresentation.time(r,false),12,0xff9CA3AF,false),6);
        }
        String status=VideoTradePresentation.status(r.status);
        if(!status.isEmpty())append(row,text(c,status,12,0xff991B1B,false),4);
        row.setOnClickListener(open);row.setFocusable(true);row.setContentDescription(VideoTradePresentation.label(r)+" transaction details");return row;
    }
}
