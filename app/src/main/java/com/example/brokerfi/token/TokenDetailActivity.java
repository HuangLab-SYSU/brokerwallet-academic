package com.example.brokerfi.token;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.brokerfi.R;
import com.example.brokerfi.core.security.SecurityUtil;
import com.example.brokerfi.swap.BrokerSwapClient;
import com.example.brokerfi.swap.BrokerSwapConfig;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Full asset balance table and token management entry point. */
public class TokenDetailActivity extends AppCompatActivity {
    private final ExecutorService balanceReads = Executors.newSingleThreadExecutor();
    private volatile int generation;
    private SwipeRefreshLayout refresh;
    private LinearLayout assetTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_detail);
        TokenTopBarHelper.bind(this);
        findViewById(R.id.token_manage_tokens_button).setOnClickListener(v ->
                startActivity(new Intent(this, TokenManageActivity.class)));
        assetTable = findViewById(R.id.wallet_asset_table);
        refresh = findViewById(R.id.token_refresh);
        TokenSwipeRefreshHelper.bind(refresh, this::reloadWallet);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadWallet();
    }

    @Override
    protected void onPause() {
        generation++;
        TokenSwipeRefreshHelper.stop(refresh);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        balanceReads.shutdownNow();
        super.onDestroy();
    }

    private void reloadWallet() {
        int request = ++generation;
        String key = TokenWalletHelper.getCurrentPrivateKey(this);
        String wallet = TokenWalletHelper.getWalletAddress(this);
        List<TokenItem> assets = TokenStore.getWalletAssets(this);
        List<View> rows = new ArrayList<>();
        assetTable.removeAllViews();
        for (TokenItem asset : assets) {
            View row = getLayoutInflater().inflate(R.layout.wallet_asset_row, assetTable, false);
            showAsset(row, asset, wallet);
            if (!asset.isNative()) {
                row.setBackgroundResource(android.R.drawable.list_selector_background);
                row.setFocusable(true);
                row.setOnClickListener(v -> openContractInfo(asset));
            }
            assetTable.addView(row);
            rows.add(row);
        }
        if (TextUtils.isEmpty(wallet) || TextUtils.isEmpty(key) || !SecurityUtil.isNewPrivateKeyFormat(key)) {
            TokenSwipeRefreshHelper.stop(refresh);
            return;
        }

        balanceReads.execute(() -> {
            for (int i = 0; i < assets.size() && request == generation; i++) {
                TokenItem asset = assets.get(i);
                View row = rows.get(i);
                try {
                    BigInteger units = asset.isNative()
                            ? new BrokerSwapClient(BrokerSwapConfig.load(this)).balance("", wallet, "")
                            : TokenContractHelper.readBalance(asset.getContractAddress(), wallet, key);
                    if (units != null) {
                        runOnUiThread(() -> {
                            if (isCurrent(request, wallet)) {
                                TokenBalanceCache.storeAssetBalance(this, wallet, asset, units);
                                showAsset(row, asset, wallet);
                            }
                        });
                    }
                } catch (Exception ignored) {
                    // Retain a previously fetched balance when a read fails.
                }
            }
            runOnUiThread(() -> {
                if (isCurrent(request, wallet)) TokenSwipeRefreshHelper.stop(refresh);
            });
        });
    }

    private boolean isCurrent(int request, String wallet) {
        return request == generation && !isFinishing()
                && TextUtils.equals(wallet, TokenWalletHelper.getWalletAddress(this));
    }

    private void showAsset(View row, TokenItem asset, String wallet) {
        AssetBalanceRow.show(row, asset, AssetBalanceRow.cachedBalance(this, wallet, asset));
        if (!TextUtils.isEmpty(asset.getName()) && !asset.getName().equalsIgnoreCase(asset.getSymbol())) {
            TextView name = row.findViewById(R.id.asset_row_name);
            name.setText(getString(R.string.wallet_asset_name, asset.getName(), asset.getSymbol()));
        }
    }

    private void openContractInfo(TokenItem asset) {
        startActivity(new Intent(this, TokenInfoActivity.class)
                .putExtra(TokenInfoActivity.EXTRA_CONTRACT, asset.getContractAddress())
                .putExtra(TokenInfoActivity.EXTRA_NAME, asset.getName())
                .putExtra(TokenInfoActivity.EXTRA_SYMBOL, asset.getSymbol())
                .putExtra(TokenInfoActivity.EXTRA_BUILT_IN, asset.isBuiltIn()));
    }
}
