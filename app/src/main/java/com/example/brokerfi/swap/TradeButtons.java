package com.example.brokerfi.swap;

import android.widget.Button;
import com.example.brokerfi.R;

/** Disabled states remain readable, without changing the actual transaction guard. */
final class TradeButtons {
    static void state(Button button,String text,boolean enabled,boolean blocked) {
        button.setText(text); button.setEnabled(enabled); button.setTextColor(0xffFFFFFF);
        button.setBackgroundResource(enabled ? R.drawable.video_button : R.drawable.video_button_disabled);
    }
    private TradeButtons() {}
}
