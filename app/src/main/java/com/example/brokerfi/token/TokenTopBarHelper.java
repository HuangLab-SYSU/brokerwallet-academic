package com.example.brokerfi.token;

import com.example.brokerfi.xc.MainActivity;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;

/** Binds the shared BrokerChain top bar on token sub-pages. */
public final class TokenTopBarHelper {

    private TokenTopBarHelper() {
    }

    public static void bind(AppCompatActivity activity) {
        ImageView menu = activity.findViewById(R.id.menu);
        ImageView notificationBtn = activity.findViewById(R.id.notificationBtn);
        RelativeLayout actionBar = activity.findViewById(R.id.action_bar);
        if (menu == null || actionBar == null) {
            return;
        }
        new NavigationHelper(menu, actionBar, activity, notificationBtn);
        TokenSubHeaderHelper.bindInfoButton(activity);

        View logo = activity.findViewById(R.id.dashedBorderView);
        if (logo != null) {
            logo.setOnClickListener(v -> {
                Intent home = new Intent(activity, MainActivity.class);
                activity.startActivity(home);
            });
        }
    }
}
