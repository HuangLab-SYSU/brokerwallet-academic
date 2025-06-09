package com.example.brokerfi.xc.tool;

import android.content.Context;

public class UnitConverter {
    public static int dpToSp(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static int spToDp(Context context, float sp) {
        return (int) (sp / context.getResources().getDisplayMetrics().scaledDensity);
    }
}
