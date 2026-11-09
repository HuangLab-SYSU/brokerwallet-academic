package com.example.brokerfi.token;

import com.example.brokerfi.receive.ReceiveActivity;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.brokerfi.R;

import java.math.BigInteger;
import java.util.List;
import com.example.brokerfi.swap.BrokerSwapConfig;
import com.example.brokerfi.swap.BrokerSwapClient;
import com.example.brokerfi.swap.SwapQuoteMath;
import com.example.brokerfi.swap.TradeDisplay;
import com.example.brokerfi.core.config.ChainConfig;
import com.example.brokerfi.send.SendActivity;

public class TokenDetailActivity extends AppCompatActivity {

    private static final String TAG = "TokenDetailActivity";
    private static final long BALANCE_AUTO_REFRESH_MS = 20_000L;
    private static final long PULL_REFRESH_SPINNER_MAX_MS = 10_000L;

    private final Handler balanceHandler = new Handler(Looper.getMainLooper());
    private Runnable pullRefreshHideSpinner;
    private boolean pullRefreshDeferredToBackground;
    private boolean pullRefreshHadDisplay;
    private final Runnable balanceAutoRefresh = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) {
                return;
            }
            fetchBalanceInBackground(true, true);
            balanceHandler.postDelayed(this, BALANCE_AUTO_REFRESH_MS);
        }
    };
    private TextView balanceText;
    private TextView pageTitle;
    private TextView tokenIcon;
    private View swapAction;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptySection;
    private View spacer;
    private RecyclerView txList;
    private TokenTxAdapter txAdapter;

    private TokenItem selectedToken;
    private String walletAddress;
    private String privateKey;
    private BigInteger tokenBalance = BigInteger.ZERO;
    private final java.util.concurrent.ExecutorService assetReads=java.util.concurrent.Executors.newSingleThreadExecutor();
    private boolean nativeLoading;
    private boolean nativeBalanceKnown;
    private int assetGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_detail);

        balanceText = findViewById(R.id.token_balance_text);
        pageTitle = findViewById(R.id.token_page_title);
        tokenIcon = findViewById(R.id.token_icon);
        swapAction = findViewById(R.id.token_action_wrap);
        swipeRefresh = findViewById(R.id.token_refresh);
        emptySection = findViewById(R.id.token_empty_section);
        spacer = findViewById(R.id.token_spacer);
        txList = findViewById(R.id.token_tx_list);

        txAdapter = new TokenTxAdapter();
        txAdapter.setOnRecordClickListener(record ->
                TokenTxDetailDialog.show(
                        this,
                        record,
                        walletAddress,
                        selectedToken != null ? selectedToken.getContractAddress() : ""));
        txList.setLayoutManager(new LinearLayoutManager(this));
        txList.setAdapter(txAdapter);

        TokenSwipeRefreshHelper.bind(swipeRefresh, this::onPullRefresh);
        TokenTopBarHelper.bind(this);
        TokenSubHeaderHelper.bindInfoButton(this);

        View pickerTrigger = findViewById(R.id.token_picker_trigger);
        View.OnClickListener openPicker = v -> openTokenPicker();
        if (pickerTrigger != null) {
            bindActionWithHaptics(pickerTrigger, openPicker);
        } else {
            bindActionWithHaptics(pageTitle, openPicker);
        }
        tokenIcon.setOnClickListener(openPicker);
        ImageView pickerArrow = findViewById(R.id.token_picker_arrow);
        if (pickerArrow != null) {
            pickerArrow.setVisibility(View.VISIBLE);
        }

        bindActionWithHaptics(R.id.token_action_wrap, v -> openSwap(false));
        findViewById(R.id.token_manage_tokens_button).setOnClickListener(v ->
                startActivity(new Intent(this, TokenManageActivity.class)));
        bindActionWithHaptics(R.id.token_action_send, v -> openSend());
        bindActionWithHaptics(R.id.token_action_receive, v -> openReceive());

        resolveAccount();
        importConfiguredAssets();
        reloadSelectedToken();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resolveAccount();
        reloadSelectedToken();
        balanceHandler.removeCallbacks(balanceAutoRefresh);
        balanceHandler.postDelayed(balanceAutoRefresh, BALANCE_AUTO_REFRESH_MS);
        syncIncomingTxHistory(() -> runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            refreshTxHistory();
        }));
    }

    @Override
    protected void onPause() {
        super.onPause();
        balanceHandler.removeCallbacks(balanceAutoRefresh);
        cancelPullRefreshSpinnerTimeout();
    }

    private void resolveAccount() {
        privateKey = TokenWalletHelper.getCurrentPrivateKey(this);
        String current=TokenWalletHelper.getWalletAddress(this);
        if(!TextUtils.equals(walletAddress,current)) { assetGeneration++; nativeBalanceKnown=false; tokenBalance=BigInteger.ZERO; }
        walletAddress=current;
    }

    private void reloadSelectedToken() {
        String previous=selectedToken==null?"":selectedToken.getContractAddress();
        selectedToken = TokenSelection.ensureValidSelection(this);
        if(selectedToken!=null && !selectedToken.getContractAddress().equals(previous)) {
            assetGeneration++; nativeBalanceKnown=false; tokenBalance=BigInteger.ZERO;
        }
        applyCachedBalance();
        applySelectedTokenUi();
        refreshTxHistory();
        fetchBalanceInBackground(true, true);
    }

    private void applySelectedTokenUi() {
        if (selectedToken == null) {
            return;
        }
        String symbol = selectedToken.getSymbol();
        String title=symbol;
        try {
            if(BrokerSwapConfig.load(this).musdt().equalsIgnoreCase(selectedToken.getContractAddress()))title="mock USDT";
        } catch(Exception ignored) { }
        pageTitle.setText(title);
        balanceText.setText(
                selectedToken.isNative() && !nativeBalanceKnown ? getString(R.string.token_balance_loading)
                        : TradeDisplay.amount(tokenBalance, selectedToken.getDecimals())+" "+symbol);

        tokenIcon.setText(TokenItem.iconLetter(symbol));

        swapAction.setVisibility(View.VISIBLE);
        findViewById(R.id.token_info_button).setVisibility(selectedToken.isNative()?View.GONE:View.VISIBLE);
    }

    private void openTokenPicker() {
        String wrappedBkcContract = wrappedBkcContractHelper.resolveContractAddress(this);
        List<TokenItem> enabled = TokenStore.getEnabledAssets(this, wrappedBkcContract);
        if (enabled.isEmpty()) {
            Toast.makeText(this, R.string.token_list_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String current = selectedToken != null ? selectedToken.getContractAddress() : "";
        TokenPickerDialog.show(this, enabled, current, item -> {
            TokenSelection.setSelected(this, item);
            reloadSelectedToken();
        });
    }

    private void refreshTxHistory() {
        if (TextUtils.isEmpty(walletAddress) || selectedToken == null) {
            showEmptyHistory();
            return;
        }
        List<TokenTxRecord> records = TokenTxHistoryStore.getForContract(
                this, walletAddress, selectedToken.getContractAddress());
        if (records.isEmpty()) {
            showEmptyHistory();
        } else {
            emptySection.setVisibility(View.GONE);
            spacer.setVisibility(View.GONE);
            txList.setVisibility(View.VISIBLE);
            txAdapter.setRecords(records);
        }
    }

    private void showEmptyHistory() {
        emptySection.setVisibility(View.VISIBLE);
        spacer.setVisibility(View.VISIBLE);
        txList.setVisibility(View.GONE);
        txAdapter.setRecords(null);
    }

    private boolean applyCachedBalance() {
        if (selectedToken == null) {
            return false;
        }
        if (selectedToken.isNative()) {
            TokenBalanceCache.Snapshot nativeSnapshot=TokenBalanceCache.getSnapshot(walletAddress,TokenItem.NATIVE_BKC);
            if(nativeSnapshot!=null) {
                tokenBalance=nativeSnapshot.tokenBalance; nativeBalanceKnown=true;
                balanceText.setText(TradeDisplay.amount(tokenBalance,18)+" BKC");return true;
            }
            String cached=TokenBalanceCache.hasNativeBalance(walletAddress)?TokenBalanceCache.getNativeBalanceDisplay(walletAddress):null;
            if (cached==null) return false;
            try { tokenBalance=new java.math.BigDecimal(cached).movePointRight(18).toBigIntegerExact();
                nativeBalanceKnown=true;
                balanceText.setText(TradeDisplay.amount(tokenBalance,18)+" BKC");return true; }
            catch (RuntimeException e) { return false; }
        }
        TokenBalanceCache.Snapshot snapshot = TokenBalanceCache.resolveSnapshot(
                this, walletAddress, selectedToken.getContractAddress());
        if (snapshot == null) {
            return false;
        }
        tokenBalance = snapshot.tokenBalance;
        balanceText.setText(
                TradeDisplay.amount(tokenBalance, selectedToken.getDecimals())
                        + " " + selectedToken.getSymbol());
        return true;
    }

    private void fetchBalanceInBackground(boolean force, boolean silent) {
        if (selectedToken!=null && selectedToken.isNative()) { fetchNativeBalance(silent); return; }
        if (TextUtils.isEmpty(walletAddress) || selectedToken == null
                || TextUtils.isEmpty(selectedToken.getContractAddress())) {
            showZeroBalance();
            return;
        }
        final boolean hadDisplay = applyCachedBalance();
        final int generation=assetGeneration;
        if (!hadDisplay) {
            if (silent) {
                showZeroBalance();
            } else {
                showBalanceLoading();
            }
        }
        TokenBalanceCache.prefetchTokenBalance(
                this,
                selectedToken.getContractAddress(),
                force,
                () -> runOnUiThread(() -> {
                    if (isFinishing() || generation!=assetGeneration) {
                        return;
                    }
                    if (!applyCachedBalance() && !hadDisplay) {
                        showZeroBalance();
                    }
                }),
                () -> runOnUiThread(() -> {
                    if (isFinishing() || generation!=assetGeneration) {
                        return;
                    }
                    if (!applyCachedBalance() && !hadDisplay) {
                        showZeroBalance();
                    }
                    if (!silent) {
                        notifyBalanceFetchFailed();
                    }
                }));
    }

    private void showBalanceLoading() {
        if (selectedToken != null) {
            balanceText.setText(getString(R.string.token_balance_loading));
        }
    }

    private void onPullRefresh() {
        Log.i(TAG, "onPullRefresh balance only");
        resolveAccount();
        if (selectedToken!=null && selectedToken.isNative()) { fetchNativeBalance(false);syncIncomingTxHistory(this::refreshTxHistory); return; }
        if (selectedToken == null) {
            reloadSelectedToken();
            TokenSwipeRefreshHelper.stop(swipeRefresh);
            return;
        }
        if (TextUtils.isEmpty(walletAddress)
                || TextUtils.isEmpty(selectedToken.getContractAddress())) {
            TokenSwipeRefreshHelper.stop(swipeRefresh);
            return;
        }
        cancelPullRefreshSpinnerTimeout();
        schedulePullRefreshSpinnerTimeout();
        pullRefreshHadDisplay = applyCachedBalance();
        if (!pullRefreshHadDisplay) {
            showBalanceLoading();
        }
        final String contract = selectedToken.getContractAddress();
        Log.i(TAG, "onPullRefresh prefetch contract=" + contract + " wallet=" + walletAddress);
        TokenBalanceCache.prefetchTokenBalance(
                this,
                contract,
                true,
                () -> runOnUiThread(this::finishPullRefreshBalanceSuccess),
                () -> runOnUiThread(this::finishPullRefreshBalanceFailed));
    }

    private void finishPullRefreshBalanceSuccess() {
        if (isFinishing()) {
            return;
        }
        cancelPullRefreshSpinnerTimeout();
        TokenSwipeRefreshHelper.stop(swipeRefresh);
        if (!applyCachedBalance() && !pullRefreshHadDisplay) {
            showZeroBalance();
        }
    }

    private void finishPullRefreshBalanceFailed() {
        if (isFinishing()) {
            return;
        }
        boolean wasDeferred = pullRefreshDeferredToBackground;
        cancelPullRefreshSpinnerTimeout();
        TokenSwipeRefreshHelper.stop(swipeRefresh);
        if (!applyCachedBalance() && !pullRefreshHadDisplay) {
            showZeroBalance();
        }
        if (!wasDeferred) {
            notifyBalanceFetchFailed();
        }
    }

    private void schedulePullRefreshSpinnerTimeout() {
        pullRefreshDeferredToBackground = false;
        pullRefreshHideSpinner = () -> {
            if (isFinishing()) {
                return;
            }
            pullRefreshDeferredToBackground = true;
            TokenSwipeRefreshHelper.stop(swipeRefresh);
            Toast.makeText(this, R.string.token_balance_refresh_background, Toast.LENGTH_SHORT).show();
            pullRefreshHideSpinner = null;
        };
        balanceHandler.postDelayed(pullRefreshHideSpinner, PULL_REFRESH_SPINNER_MAX_MS);
    }

    private void cancelPullRefreshSpinnerTimeout() {
        if (pullRefreshHideSpinner != null) {
            balanceHandler.removeCallbacks(pullRefreshHideSpinner);
            pullRefreshHideSpinner = null;
        }
    }

    private void notifyBalanceFetchFailed() {
        runOnUiThread(() -> {
            if (!isFinishing()) {
                Toast.makeText(this, R.string.token_balance_fetch_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncIncomingTxHistory(@Nullable Runnable afterSync) {
        if (TextUtils.isEmpty(walletAddress)) {
            if (afterSync != null) {
                afterSync.run();
            }
            return;
        }
        if (selectedToken!=null && selectedToken.isNative()) NativeAssetHistorySync.sync(this,walletAddress,afterSync);
        else TokenIncomingTxSync.sync(this, afterSync);
    }

    private void showZeroBalance() {
        if (selectedToken != null) {
            balanceText.setText("0 " + selectedToken.getSymbol());
        }
    }

    private void openReceive() {
        if (TextUtils.isEmpty(walletAddress)) {
            Toast.makeText(this, R.string.token_no_account, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ReceiveActivity.class);
        if (selectedToken != null) {
            intent.putExtra(ReceiveActivity.EXTRA_TOKEN_SYMBOL, selectedToken.getSymbol());
            intent.putExtra(ReceiveActivity.EXTRA_TOKEN_NAME, selectedToken.getName());
        }
        startActivity(intent);
    }

    private void bindActionWithHaptics(int viewId, View.OnClickListener delegate) {
        View actionView = findViewById(viewId);
        bindActionWithHaptics(actionView, delegate);
    }

    private void bindActionWithHaptics(View actionView, View.OnClickListener delegate) {
        if (actionView == null || delegate == null) {
            return;
        }
        actionView.setClickable(true);
        actionView.setFocusable(true);
        actionView.setHapticFeedbackEnabled(true);
        actionView.setOnTouchListener(new View.OnTouchListener() {
            private boolean didHapticForPress;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event == null) {
                    return false;
                }
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    didHapticForPress = v.performHapticFeedback(
                            HapticFeedbackConstants.VIRTUAL_KEY,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                } else if (action == MotionEvent.ACTION_UP
                        || action == MotionEvent.ACTION_CANCEL) {
                    didHapticForPress = false;
                }
                return false;
            }
        });
        actionView.setOnClickListener(v -> {
            if (!v.isHapticFeedbackEnabled()) {
                v.performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
            }
            delegate.onClick(v);
        });
    }

    private void openSend() {
        if (TextUtils.isEmpty(privateKey)) {
            Toast.makeText(this, R.string.token_no_account, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedToken!=null && selectedToken.isNative()) { startActivity(new Intent(this,SendActivity.class)); return; }
        if (selectedToken == null || TextUtils.isEmpty(selectedToken.getContractAddress())) {
            Toast.makeText(this, R.string.token_contract_missing, Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(new Intent(this, TokenSendActivity.class));
    }

    private void openSwap(boolean unwrap) {
        if (TextUtils.isEmpty(privateKey)) {
            Toast.makeText(this, R.string.token_no_account, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(wrappedBkcContractHelper.resolveContractAddress(this))) {
            Toast.makeText(this, R.string.token_contract_missing, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            BrokerSwapConfig cfg=BrokerSwapConfig.load(this);
            String selected=selectedToken==null?"":selectedToken.getContractAddress();
            if (selectedToken==null || !(selectedToken.isNative() || selected.equalsIgnoreCase(cfg.musdt())
                    || selected.equalsIgnoreCase(cfg.wbkc()) || selected.equalsIgnoreCase(wrappedBkcContractHelper.resolveContractAddress(this)))) {
                Toast.makeText(this,"This asset is not supported by Trade.",Toast.LENGTH_LONG).show();return;
            }
        } catch (Exception e) { Toast.makeText(this,"Trade configuration unavailable.",Toast.LENGTH_LONG).show();return; }
        Intent intent = new Intent(this, com.example.brokerfi.swap.BrokerSwapActivity.class);
        intent.putExtra(TokenSwapActivity.EXTRA_UNWRAP, unwrap);
        if (selectedToken != null && !TextUtils.isEmpty(selectedToken.getContractAddress())) {
            intent.putExtra(TokenSwapActivity.EXTRA_PAY_CONTRACT, selectedToken.getContractAddress());
        }
        startActivity(intent);
    }
    private void importConfiguredAssets() {
        if (ChainConfig.useLocalBrokerChainNode()) return;
        try {
            BrokerSwapConfig cfg=BrokerSwapConfig.load(this);
            for (boolean musdt:new boolean[]{false,true}) {
                String contract=musdt?cfg.musdt():cfg.wbkc(); if(TokenStore.contains(this,contract))continue;
                TokenItem item=new TokenItem();item.setContractAddress(contract);item.setSymbol(musdt?"mUSDT":"wBKC (Swap)");
                item.setName(musdt?"mock USDT (test)":"BrokerSwap wrapped BKC");item.setDecimals(musdt?6:18);item.setEnabled(true);
                TokenStore.addCustom(this,item);
            }
        } catch (Exception ignored) { }
    }
    private void fetchNativeBalance(boolean silent) {
        if(nativeLoading || TextUtils.isEmpty(walletAddress)) { TokenSwipeRefreshHelper.stop(swipeRefresh);return; }
        final int id=assetGeneration;final String address=walletAddress;
        if(!applyCachedBalance())balanceText.setText(getString(R.string.token_balance_loading));
        nativeLoading=true;
        assetReads.execute(() -> {
            try {
                BigInteger units=new BrokerSwapClient(BrokerSwapConfig.load(this)).balance("",address,"");
                runOnUiThread(() -> {
                    nativeLoading=false;TokenSwipeRefreshHelper.stop(swipeRefresh);
                    if(isFinishing() || id!=assetGeneration || selectedToken==null || !selectedToken.isNative()
                            || !address.equalsIgnoreCase(TokenWalletHelper.getWalletAddress(this)))return;
                    tokenBalance=units;String exact=SwapQuoteMath.format(units,18);
                    nativeBalanceKnown=true;
                    TokenBalanceCache.store(address,TokenItem.NATIVE_BKC,exact,units);
                    balanceText.setText(TradeDisplay.amount(units,18)+" BKC");
                });
            } catch(Exception e) {
                runOnUiThread(() -> { nativeLoading=false;TokenSwipeRefreshHelper.stop(swipeRefresh);
                    if(!isFinishing() && id==assetGeneration && selectedToken!=null && selectedToken.isNative()) {
                        if(!applyCachedBalance())balanceText.setText("Could not load balance");
                        if(!silent)notifyBalanceFetchFailed();
                    }
                });
            }
        });
    }
    @Override protected void onDestroy() { assetGeneration++;assetReads.shutdown();super.onDestroy(); }
}
