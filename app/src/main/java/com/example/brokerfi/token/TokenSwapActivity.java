package com.example.brokerfi.token;

import com.example.brokerfi.xc.ChainTxHelper;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.xc.ChainAddressUtil;
import com.example.brokerfi.token.TokenContractHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.brokerfi.R;
import com.example.brokerfi.token.TokenConfig;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class TokenSwapActivity extends AppCompatActivity {

    private static final String TAG = "TokenSwapActivity";
    public static final String EXTRA_UNWRAP = "unwrap";
    /** 浠庤鎯呴〉杩涘叆鏃讹紝棰勫～銆屾敮浠樸€嶄晶浠ｅ竵鍚堢害鍦板潃銆?*/
    public static final String EXTRA_PAY_CONTRACT = "pay_contract";
    private static final long REFRESH_SPINNER_MAX_MS = 10_000L;

    private EditText payAmountInput;
    private TextView receiveAmountText;
    private TextView paySymbolText;
    private TextView receiveSymbolText;
    private TextView payTokenIcon;
    private TextView receiveTokenIcon;
    private TextView payBalanceText;
    private TextView receiveBalanceText;
    private TextView payMaxButton;
    private Button confirmButton;
    private SwipeRefreshLayout swipeRefresh;

    private SwapTokenOption payOption = SwapTokenOption.nativeBkc();
    private SwapTokenOption receiveOption;

    private String walletAddress;
    private String privateKey;
    private String nativeBalanceDisplay = "0";
    private BigInteger payTokenBalance = BigInteger.ZERO;
    private BigInteger receiveTokenBalance = BigInteger.ZERO;
    private boolean submitting;
    private boolean balancesLoading = true;
    private boolean refreshAfterFetch;
    private boolean balanceFetchFailedNotified;
    private int balanceFetchId;
    private List<TokenItem> enabledTokens;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable clearMaxHighlight = () -> {
        if (payMaxButton != null) {
            payMaxButton.setSelected(false);
        }
    };
    private Runnable hideRefreshSpinnerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_swap);

        payAmountInput = findViewById(R.id.token_pay_amount);
        receiveAmountText = findViewById(R.id.token_receive_amount);
        paySymbolText = findViewById(R.id.token_pay_symbol);
        receiveSymbolText = findViewById(R.id.token_receive_symbol);
        payTokenIcon = findViewById(R.id.token_pay_token_icon);
        receiveTokenIcon = findViewById(R.id.token_receive_token_icon);
        payBalanceText = findViewById(R.id.token_pay_balance);
        receiveBalanceText = findViewById(R.id.token_receive_balance);
        payMaxButton = findViewById(R.id.token_pay_max);
        confirmButton = findViewById(R.id.token_confirm_button);
        swipeRefresh = findViewById(R.id.token_refresh);
        TokenTopBarHelper.bind(this);

        TextView pageTitle = findViewById(R.id.token_page_title);
        if (pageTitle != null) {
            pageTitle.setText(R.string.token_wrap_title);
        }

        resolveAccount();
        reloadEnabledTokens();
        applyIntentTokenSelection();

        findViewById(R.id.token_swap_direction).setOnClickListener(v -> swapPayReceive());
        findViewById(R.id.token_pay_token_selector).setOnClickListener(v -> openPayPicker());
        findViewById(R.id.token_receive_token_selector).setOnClickListener(v -> openReceivePicker());
        if (payMaxButton != null) {
            payMaxButton.setOnClickListener(v -> applyMaxAmount());
        }
        confirmButton.setOnClickListener(v -> showSubmitConfirm());
        TokenSwipeRefreshHelper.bind(swipeRefresh, this::onPullRefresh);

        payAmountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateReceivePreview();
                updateConfirmState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        applyTokenUi();
        fetchBalancesInBackground(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resolveAccount();
        reloadEnabledTokens();
        readBalancesFromCache();
        applyTokenUi();
        if (swipeRefresh == null || !swipeRefresh.isRefreshing()) {
            refreshAfterFetch = false;
        }
        fetchBalancesInBackground(true);
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(clearMaxHighlight);
        cancelRefreshSpinnerTimeout();
        super.onDestroy();
    }

    private void applyIntentTokenSelection() {
        boolean unwrapMode = getIntent().getBooleanExtra(EXTRA_UNWRAP, false);
        String payContract = getIntent().getStringExtra(EXTRA_PAY_CONTRACT);
        SwapTokenOption payFromIntent = findOptionForContract(payContract);
        if (payFromIntent != null) {
            payOption = payFromIntent;
            receiveOption = defaultReceiveForPay(payOption, unwrapMode);
            return;
        }
        if (unwrapMode) {
            payOption = defaultwrappedBkcOption();
            receiveOption = SwapTokenOption.nativeBkc();
        } else {
            payOption = SwapTokenOption.nativeBkc();
            receiveOption = defaultwrappedBkcOption();
        }
    }

    @Nullable
    private SwapTokenOption findOptionForContract(@Nullable String contractAddress) {
        if (TextUtils.isEmpty(contractAddress) || enabledTokens == null) {
            return null;
        }
        String target = ChainAddressUtil.normalizeAddress(contractAddress);
        for (TokenItem item : enabledTokens) {
            if (target.equals(ChainAddressUtil.normalizeAddress(item.getContractAddress()))) {
                return SwapTokenOption.fromToken(item);
            }
        }
        return null;
    }

    private SwapTokenOption defaultReceiveForPay(SwapTokenOption pay, boolean unwrapMode) {
        if (unwrapMode) {
            return SwapTokenOption.nativeBkc();
        }
        if (pay.isNativeBkc()) {
            return defaultwrappedBkcOption();
        }
        if (pay.isOfficialwrappedBkc(this)) {
            return SwapTokenOption.nativeBkc();
        }
        return SwapTokenOption.nativeBkc();
    }

    private SwapTokenOption defaultwrappedBkcOption() {
        String wrappedBkcContract = wrappedBkcContractHelper.resolveContractAddress(this);
        if (enabledTokens != null) {
            for (TokenItem item : enabledTokens) {
                if (item.matchesOfficialwrappedBkc(wrappedBkcContract)) {
                    return SwapTokenOption.fromToken(item);
                }
            }
        }
        return SwapTokenOption.fromToken(TokenItem.builtInwrappedBkc(wrappedBkcContract));
    }

    private void reloadEnabledTokens() {
        String wrappedBkcContract = wrappedBkcContractHelper.resolveContractAddress(this);
        enabledTokens = TokenStore.getEnabledTokens(this, wrappedBkcContract);
    }

    private void resolveAccount() {
        privateKey = TokenWalletHelper.getCurrentPrivateKey(this);
        walletAddress = TokenWalletHelper.getWalletAddress(this);
    }

    private void onPullRefresh() {
        Log.i(TAG, "onPullRefresh");
        resolveAccount();
        refreshAfterFetch = true;
        scheduleRefreshSpinnerTimeout();
        fetchBalancesInBackground(true);
    }

    /** 杞湀鏈€澶氭樉绀?2 绉掞紝涔嬪悗闅愯棌鍔ㄧ敾浣嗗悗鍙扮户缁媺浣欓銆?*/
    private void scheduleRefreshSpinnerTimeout() {
        cancelRefreshSpinnerTimeout();
        hideRefreshSpinnerRunnable = () -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
        };
        mainHandler.postDelayed(hideRefreshSpinnerRunnable, REFRESH_SPINNER_MAX_MS);
    }

    private void cancelRefreshSpinnerTimeout() {
        if (hideRefreshSpinnerRunnable != null) {
            mainHandler.removeCallbacks(hideRefreshSpinnerRunnable);
            hideRefreshSpinnerRunnable = null;
        }
    }

    private void swapPayReceive() {
        String currentAmount = payAmountInput.getText() != null
                ? payAmountInput.getText().toString()
                : "";
        SwapTokenOption previousPay = payOption;
        payOption = receiveOption;
        receiveOption = previousPay;
        payAmountInput.setText(currentAmount);
        if (payAmountInput.getText() != null) {
            payAmountInput.setSelection(payAmountInput.getText().length());
        }
        applyTokenUi();
        fetchBalancesInBackground(false);
    }

    private void openPayPicker() {
        String official = wrappedBkcContractHelper.resolveContractAddress(this);
        TokenPickerDialog.showForSwap(
                this,
                enabledTokens,
                official,
                payOption.selectionKey(),
                option -> {
                    if (SwapTokenOption.sameOption(option, receiveOption)) {
                        receiveOption = payOption;
                    }
                    payOption = option;
                    payAmountInput.setText("");
                    applyTokenUi();
                    fetchBalancesInBackground(false);
                });
    }

    private void openReceivePicker() {
        String official = wrappedBkcContractHelper.resolveContractAddress(this);
        TokenPickerDialog.showForSwap(
                this,
                enabledTokens,
                official,
                receiveOption.selectionKey(),
                option -> {
                    if (SwapTokenOption.sameOption(option, payOption)) {
                        payOption = receiveOption;
                    }
                    receiveOption = option;
                    payAmountInput.setText("");
                    applyTokenUi();
                    fetchBalancesInBackground(false);
                });
    }

    private void applyTokenUi() {
        bindTokenChip(payOption, paySymbolText, payTokenIcon, payBalanceText);
        bindTokenChip(receiveOption, receiveSymbolText, receiveTokenIcon, receiveBalanceText);
        updateReceivePreview();
        updateConfirmState();
    }

    private void bindTokenChip(
            SwapTokenOption option,
            TextView symbolView,
            TextView iconView,
            TextView balanceView) {
        symbolView.setText(option.getSymbol());
        iconView.setText(TokenItem.iconLetter(option.getSymbol()));
        if (balancesLoading && !hasCachedBalancesForOptions()) {
            balanceView.setText(R.string.token_balance_loading);
        } else {
            balanceView.setText(getString(
                    R.string.token_balance_token_format,
                    getDisplayBalance(option),
                    option.getSymbol()));
        }
        if (payMaxButton != null && symbolView == paySymbolText) {
            payMaxButton.setEnabled(!balancesLoading);
            payMaxButton.setAlpha(balancesLoading ? 0.45f : 1f);
        }
    }

    private void applyMaxAmount() {
        String maxAmount = resolveMaxAmountForPay();
        if (TextUtils.isEmpty(maxAmount) || "0".equals(maxAmount)) {
            return;
        }
        payAmountInput.setText(maxAmount);
        payAmountInput.post(() -> {
            if (payAmountInput.getText() == null) {
                return;
            }
            payAmountInput.setSelection(0);
            payAmountInput.bringPointIntoView(0);
            payAmountInput.scrollTo(0, 0);
        });
        if (payMaxButton != null) {
            mainHandler.removeCallbacks(clearMaxHighlight);
            payMaxButton.setSelected(true);
            mainHandler.postDelayed(clearMaxHighlight, 520);
        }
    }

    private String resolveMaxAmountForPay() {
        if (payOption.isNativeBkc()) {
            return normalizeMaxAmount(nativeBalanceDisplay);
        }
        return normalizeMaxAmount(TokenAmountUtil.fromChainUnits(payTokenBalance, payOption.getDecimals()));
    }

    private String normalizeMaxAmount(String rawAmount) {
        if (TextUtils.isEmpty(rawAmount)) {
            return "0";
        }
        try {
            BigDecimal amount = new BigDecimal(rawAmount.trim());
            if (amount.signum() <= 0) {
                return "0";
            }
            return amount.stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return rawAmount.trim();
        }
    }

    private boolean hasCachedBalancesForOptions() {
        if (TextUtils.isEmpty(walletAddress)) {
            return false;
        }
        String official = wrappedBkcContractHelper.resolveContractAddress(this);
        if (!TextUtils.isEmpty(nativeBalanceDisplay) && !"0".equals(nativeBalanceDisplay)) {
            return true;
        }
        if (!TextUtils.isEmpty(official)
                && TokenBalanceCache.getSnapshot(walletAddress, official) != null) {
            return true;
        }
        if (!payOption.isNativeBkc()
                && TokenBalanceCache.getSnapshot(walletAddress, payOption.getContractAddress()) != null) {
            return true;
        }
        return !receiveOption.isNativeBkc()
                && TokenBalanceCache.getSnapshot(walletAddress, receiveOption.getContractAddress()) != null;
    }

    private String getDisplayBalance(SwapTokenOption option) {
        if (option.isNativeBkc()) {
            return TokenAmountUtil.formatDisplayAmount(nativeBalanceDisplay);
        }
        if (SwapTokenOption.sameOption(option, payOption)) {
            return TokenAmountUtil.formatDisplayAmount(payTokenBalance, option.getDecimals());
        }
        return TokenAmountUtil.formatDisplayAmount(receiveTokenBalance, option.getDecimals());
    }

    private void fetchBalancesInBackground(boolean force) {
        final int fetchId = ++balanceFetchId;
        if (TextUtils.isEmpty(walletAddress)) {
            Log.i(TAG, "fetchBalances skipped: walletAddress empty, privateKey="
                    + (TextUtils.isEmpty(privateKey) ? "missing" : "present"));
            balancesLoading = false;
            applyTokenUi();
            finishRefreshIfNeeded(fetchId);
            return;
        }
        Log.i(TAG, "fetchBalances force=" + force + " pay=" + payOption.getSymbol()
                + " receive=" + receiveOption.getSymbol());
        if (!hasCachedBalancesForOptions()) {
            balancesLoading = true;
            applyTokenUi();
        }

        if (force) {
            balanceFetchFailedNotified = false;
        }
        Runnable onFetchFailed = force ? this::notifyBalanceFetchFailed : null;

        String payContract = contractForOption(payOption);
        String receiveContract = contractForOption(receiveOption);
        Runnable onBothFetched = () -> runOnUiThread(() -> onBalanceFetchComplete(fetchId));

        if (TextUtils.equals(
                ChainAddressUtil.normalizeAddress(payContract),
                ChainAddressUtil.normalizeAddress(receiveContract))) {
            TokenBalanceCache.prefetchTokenBalance(
                    this, payContract, force, onBothFetched, onFetchFailed);
            return;
        }
        TokenBalanceCache.prefetchTokenBalance(this, payContract, force, () ->
                TokenBalanceCache.prefetchTokenBalance(
                        this, receiveContract, force, onBothFetched, onFetchFailed),
                onFetchFailed);
    }

    private void notifyBalanceFetchFailed() {
        runOnUiThread(() -> {
            if (isFinishing() || balanceFetchFailedNotified) {
                return;
            }
            balanceFetchFailedNotified = true;
            Toast.makeText(this, R.string.token_balance_fetch_failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void onBalanceFetchComplete(int fetchId) {
        if (isFinishing() || fetchId != balanceFetchId) {
            return;
        }
        readBalancesFromCache();
        balancesLoading = false;
        applyTokenUi();
        updateConfirmState();
        finishRefreshIfNeeded(fetchId);
    }

    private void finishRefreshIfNeeded(int fetchId) {
        if (fetchId != balanceFetchId) {
            return;
        }
        if (refreshAfterFetch) {
            refreshAfterFetch = false;
            cancelRefreshSpinnerTimeout();
            TokenSwipeRefreshHelper.stop(swipeRefresh);
        }
    }

    private String contractForOption(SwapTokenOption option) {
        if (option.isNativeBkc()) {
            return wrappedBkcContractHelper.resolveContractAddress(this);
        }
        return option.getContractAddress();
    }

    private void readBalancesFromCache() {
        if (TextUtils.isEmpty(walletAddress)) {
            return;
        }
        String homeNativeBal = TokenBalanceCache.getNativeBalanceDisplay(walletAddress);
        if (!TextUtils.isEmpty(homeNativeBal)) {
            nativeBalanceDisplay = homeNativeBal;
        }
        if (payOption.isNativeBkc()) {
            payTokenBalance = BigInteger.ZERO;
        } else {
            TokenBalanceCache.Snapshot snap =
                    TokenBalanceCache.getSnapshot(walletAddress, payOption.getContractAddress());
            payTokenBalance = snap != null ? snap.tokenBalance : BigInteger.ZERO;
            if (snap != null
                    && TextUtils.isEmpty(homeNativeBal)
                    && !TextUtils.isEmpty(snap.nativeBalanceDisplay)) {
                nativeBalanceDisplay = snap.nativeBalanceDisplay;
            }
        }
        if (receiveOption.isNativeBkc()) {
            receiveTokenBalance = BigInteger.ZERO;
        } else {
            TokenBalanceCache.Snapshot snap =
                    TokenBalanceCache.getSnapshot(walletAddress, receiveOption.getContractAddress());
            receiveTokenBalance = snap != null ? snap.tokenBalance : BigInteger.ZERO;
            if (snap != null
                    && TextUtils.isEmpty(homeNativeBal)
                    && !TextUtils.isEmpty(snap.nativeBalanceDisplay)) {
                nativeBalanceDisplay = snap.nativeBalanceDisplay;
            }
        }
    }

    private boolean isUnwrapMode() {
        return payOption.isOfficialwrappedBkc(this) && receiveOption.isNativeBkc();
    }

    private boolean isWrapMode() {
        return payOption.isNativeBkc() && receiveOption.isOfficialwrappedBkc(this);
    }

    private boolean isSupportedSwapPair() {
        return isWrapMode() || isUnwrapMode();
    }

    private String resolveWrapContract() {
        if (isWrapMode()) {
            return receiveOption.getContractAddress();
        }
        if (isUnwrapMode()) {
            return payOption.getContractAddress();
        }
        return wrappedBkcContractHelper.resolveContractAddress(this);
    }

    private void updateReceivePreview() {
        if (!isSupportedSwapPair()) {
            receiveAmountText.setText(R.string.token_swap_receive_unsupported);
            receiveAmountText.setTextColor(0xFF94A3B8);
            return;
        }
        String raw = payAmountInput.getText() != null ? payAmountInput.getText().toString().trim() : "";
        receiveAmountText.setText(raw.isEmpty() ? "0" : raw);
        receiveAmountText.setTextColor(raw.isEmpty() ? 0xFF94A3B8 : 0xFF0F172A);
    }

    private void updateConfirmState() {
        if (submitting) {
            confirmButton.setEnabled(false);
            confirmButton.setText(R.string.token_submitting);
            return;
        }

        if (!isSupportedSwapPair()) {
            setConfirmDisabled(R.string.token_swap_pair_only);
            return;
        }

        String contractAddress = resolveWrapContract();
        if (TextUtils.isEmpty(contractAddress)) {
            setConfirmDisabled(R.string.token_hint_no_contract);
            return;
        }

        String raw = payAmountInput.getText() != null ? payAmountInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(raw)) {
            setConfirmDisabled(R.string.token_button_enter_amount);
            return;
        }

        BigInteger amount = TokenAmountUtil.toChainUnits(raw, payOption.getDecimals());
        if (amount == null || amount.signum() <= 0) {
            setConfirmDisabled(R.string.token_button_enter_amount);
            return;
        }

        if (isAmountExceedsPayBalance(amount)) {
            setConfirmDisabled(R.string.token_button_insufficient_funds);
            return;
        }

        confirmButton.setEnabled(true);
        confirmButton.setText(resolveActionLabelRes());
    }

    private void setConfirmDisabled(int buttonTextResId) {
        confirmButton.setEnabled(false);
        confirmButton.setText(buttonTextResId);
    }

    private int resolveActionLabelRes() {
        if (isWrapMode()) {
            return R.string.token_wrap;
        }
        if (isUnwrapMode()) {
            return R.string.token_unwrap_short;
        }
        return R.string.token_swap;
    }

    private boolean isAmountExceedsPayBalance(BigInteger amount) {
        try {
            if (payOption.isNativeBkc()) {
                BigDecimal nativeBal = new BigDecimal(nativeBalanceDisplay);
                BigDecimal pay = new BigDecimal(
                        TokenAmountUtil.fromChainUnits(amount, payOption.getDecimals()));
                return nativeBal.compareTo(pay) < 0;
            }
            return payTokenBalance.compareTo(amount) < 0;
        } catch (Exception e) {
            return true;
        }
    }

    private void showSubmitConfirm() {
        if (submitting) {
            return;
        }
        if (TextUtils.isEmpty(privateKey)) {
            Toast.makeText(this, R.string.token_no_account, Toast.LENGTH_SHORT).show();
            return;
        }
        String contractAddress = resolveWrapContract();
        if (TextUtils.isEmpty(contractAddress)) {
            Toast.makeText(this, R.string.token_contract_missing, Toast.LENGTH_LONG).show();
            return;
        }
        BigInteger amount = TokenAmountUtil.toChainUnits(
                payAmountInput.getText() != null ? payAmountInput.getText().toString() : null,
                payOption.getDecimals());
        if (amount == null || amount.signum() <= 0) {
            Toast.makeText(this, R.string.token_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        String amountDisplay = TokenAmountUtil.formatDisplayAmount(amount, payOption.getDecimals());
        String receiveDisplay = TokenAmountUtil.formatDisplayAmount(amount, receiveOption.getDecimals());
        String contractDisplay = ChainAddressUtil.displayAddress(contractAddress);
        boolean unwrap = isUnwrapMode();

        TokenConfirmDialog.show(
                this,
                getString(R.string.token_confirm_title_swap),
                getString(
                        R.string.token_confirm_swap_message,
                        amountDisplay,
                        payOption.getSymbol(),
                        receiveDisplay,
                        receiveOption.getSymbol(),
                        contractDisplay),
                () -> executeSubmit(amount, amountDisplay, contractAddress, unwrap));
    }

    private void executeSubmit(BigInteger amount, String amountDisplay, String contractAddress, boolean unwrap) {
        submitting = true;
        confirmButton.setEnabled(false);
        confirmButton.setText(R.string.token_submitting);

        new Thread(() -> {
            try {
                String hash;
                if (unwrap) {
                    if (payTokenBalance.compareTo(amount) < 0) {
                        throw new IllegalStateException(getString(R.string.token_insufficient_wbkc));
                    }
                    String data = wrappedBkcContractHelper.encodeWithdraw(amount);
                    hash = ChainTxHelper.sendContractCall(
                            privateKey,
                            contractAddress,
                            data,
                            "0x0",
                            TokenConfig.DEMO_GAS_LIMIT,
                            TokenConfig.DEMO_GAS_PRICE);
                } else {
                    BigDecimal nativeBal = new BigDecimal(nativeBalanceDisplay);
                    BigDecimal pay = new BigDecimal(
                            TokenAmountUtil.fromChainUnits(amount, payOption.getDecimals()));
                    if (nativeBal.compareTo(pay) < 0) {
                        throw new IllegalStateException(getString(R.string.token_insufficient_bkc));
                    }
                    String data = wrappedBkcContractHelper.encodeDeposit();
                    hash = ChainTxHelper.sendContractCall(
                            privateKey,
                            contractAddress,
                            data,
                            TokenContractHelper.toValueHex(amount),
                            TokenConfig.DEMO_GAS_LIMIT,
                            TokenConfig.DEMO_GAS_PRICE);
                }

                if (TextUtils.isEmpty(hash)) {
                    throw new IllegalStateException(
                            getString(R.string.token_tx_failed) + "\n" + ChainTxHelper.getSendFailureHint(this));
                }

                ChainTxHelper.ReceiptStatus status =
                        ChainTxHelper.waitReceiptStatus(hash, privateKey);
                if (status == ChainTxHelper.ReceiptStatus.FAILED) {
                    throw new IllegalStateException(getString(R.string.token_tx_failed));
                }

                final String txHash = hash;
                final boolean pending = status == ChainTxHelper.ReceiptStatus.PENDING;
                runOnUiThread(() -> {
                    submitting = false;
                    TokenTxHistoryStore.add(
                            this,
                            walletAddress,
                            TokenTxRecord.swap(
                                    amountDisplay,
                                    payOption.getSymbol(),
                                    receiveOption.getSymbol(),
                                    txHash,
                                    contractAddress));
                    Toast.makeText(
                            this,
                            pending ? R.string.token_tx_submitted : R.string.token_tx_success,
                            Toast.LENGTH_LONG).show();
                    payAmountInput.setText("");
                    applyTokenUi();
                    TokenBalanceCache.invalidateAfterWrite(walletAddress, contractAddress);
                    fetchBalancesInBackground(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    submitting = false;
                    applyTokenUi();
                    String msg = e.getMessage();
                    Toast.makeText(
                            this,
                            TextUtils.isEmpty(msg) ? getString(R.string.token_tx_failed) : msg,
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}


