package com.example.brokerfi.token;

import com.example.brokerfi.xc.ReceiveActivity;

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
        walletAddress = TokenWalletHelper.getWalletAddress(this);
    }

    private void reloadSelectedToken() {
        selectedToken = TokenSelection.ensureValidSelection(this);
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
        String name = selectedToken.getName();
        pageTitle.setText(name);
        balanceText.setText(
                TokenAmountUtil.formatDisplayAmount(tokenBalance, selectedToken.getDecimals())
                        + " " + symbol);

        tokenIcon.setText(TokenItem.iconLetter(symbol));

        swapAction.setVisibility(View.VISIBLE);
    }

    private void openTokenPicker() {
        String wrappedBkcContract = wrappedBkcContractHelper.resolveContractAddress(this);
        List<TokenItem> enabled = TokenStore.getEnabledTokens(this, wrappedBkcContract);
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
        TokenBalanceCache.Snapshot snapshot = TokenBalanceCache.resolveSnapshot(
                this, walletAddress, selectedToken.getContractAddress());
        if (snapshot == null) {
            return false;
        }
        tokenBalance = snapshot.tokenBalance;
        balanceText.setText(
                TokenAmountUtil.formatDisplayAmount(tokenBalance, selectedToken.getDecimals())
                        + " " + selectedToken.getSymbol());
        return true;
    }

    private void fetchBalanceInBackground(boolean force, boolean silent) {
        if (TextUtils.isEmpty(walletAddress) || selectedToken == null
                || TextUtils.isEmpty(selectedToken.getContractAddress())) {
            showZeroBalance();
            return;
        }
        final boolean hadDisplay = applyCachedBalance();
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
                    if (isFinishing()) {
                        return;
                    }
                    if (!applyCachedBalance() && !hadDisplay) {
                        showZeroBalance();
                    }
                }),
                () -> runOnUiThread(() -> {
                    if (isFinishing()) {
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
        TokenIncomingTxSync.sync(this, afterSync);
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
        Intent intent = new Intent(this, TokenSwapActivity.class);
        intent.putExtra(TokenSwapActivity.EXTRA_UNWRAP, unwrap);
        if (selectedToken != null && !TextUtils.isEmpty(selectedToken.getContractAddress())) {
            intent.putExtra(TokenSwapActivity.EXTRA_PAY_CONTRACT, selectedToken.getContractAddress());
        }
        startActivity(intent);
    }
}


