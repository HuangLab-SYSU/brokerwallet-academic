package com.example.brokerfi.swap;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

/** Presentation widgets only. Opening information never starts reads or financial operations. */
public final class TradeUi {
    private TradeUi() {}
    static int dp(Context context,int value) { return Math.round(value*context.getResources().getDisplayMetrics().density); }
    static TextView text(Context context,String value,int size,int color,boolean bold) {
        TextView view=new TextView(context); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setLineSpacing(dp(context,3),1); if(bold)view.setTypeface(null,Typeface.BOLD); return view;
    }
    public static void status(TextView view,String value,boolean warning) {
        value=value.trim();
        view.setText(value); view.setVisibility(value.isEmpty()?View.GONE:View.VISIBLE);
        view.setTextColor(warning?0xff991B1B:0xff475569);
        GradientDrawable background=new GradientDrawable(); background.setCornerRadius(dp(view.getContext(),12));
        background.setColor(warning?0xffFEF2F2:0xffEFF6FF); view.setBackground(background);
        int pad=dp(view.getContext(),12); view.setPadding(pad,pad,pad,pad);
    }
    static void hint(TextView view,String value,boolean warning) {
        view.setText(value); view.setVisibility(value.trim().isEmpty()?View.GONE:View.VISIBLE);
        view.setTextColor(warning?0xff991B1B:0xff64748B);
    }
    static void help(Context context,String title,String explanation) {
        new AlertDialog.Builder(context).setTitle(title).setMessage(explanation).setPositiveButton("Got it",null).show();
    }
}
