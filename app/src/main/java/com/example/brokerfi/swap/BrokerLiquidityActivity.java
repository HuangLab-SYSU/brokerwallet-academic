package com.example.brokerfi.swap;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.example.brokerfi.token.TokenTopBarHelper;
import com.example.brokerfi.token.TokenWalletHelper;

/** Direct entry point for the configured liquidity pool. */
public class BrokerLiquidityActivity extends AppCompatActivity {
    private LiquidityPanelController liquidity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broker_liquidity);
        TokenTopBarHelper.bind(this);
        TextView title = findViewById(R.id.token_page_title);
        title.setText(R.string.trade_liquidity_title);
        findViewById(R.id.token_info_button).setVisibility(View.GONE);
        View header = findViewById(R.id.token_picker_trigger);
        header.setClickable(false);
        header.setFocusable(false);

        BrokerSwapConfig config = null;
        try {
            config = BrokerSwapConfig.load(this);
        } catch (Exception ignored) {
            // The controller disables pool operations when the deployment is unavailable.
        }
        liquidity = new LiquidityPanelController(this, config);
    }

    @Override
    protected void onResume() {
        super.onResume();
        liquidity.onResume(BrokerSwapConfig.walletId(TokenWalletHelper.getWalletAddress(this)));
    }

    @Override
    protected void onPause() {
        liquidity.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        liquidity.onDestroy();
        super.onDestroy();
    }
}
