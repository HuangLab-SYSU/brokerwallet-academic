package com.example.brokerfi.token;

import com.example.brokerfi.xc.ChainTxHelper;

import com.example.brokerfi.xc.ChainAddressUtil;
import com.example.brokerfi.token.TokenContractHelper;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.example.brokerfi.token.wrappedbkc.wrappedBkcConfig;
import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;

import java.math.BigInteger;

public class TokenSendActivity extends AppCompatActivity {

    private static final String BUTTON_STATE_INSUFFICIENT = "BUTTON_STATE_INSUFFICIENT";

    private TextView fromInput;
    private EditText toInput;
    private EditText amountInput;
    private TextView balanceText;
    private TextView amountSymbolText;
    private TextView hintText;
    private Button confirmButton;
    private SwipeRefreshLayout swipeRefresh;

    private String contractAddress;
    private String walletAddress;
    private String privateKey;
    private BigInteger tokenBalance = BigInteger.ZERO;
    private boolean submitting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_send);

        fromInput = findViewById(R.id.token_send_from);
        toInput = findViewById(R.id.token_send_to);
        amountInput = findViewById(R.id.token_send_amount);
        balanceText = findViewById(R.id.token_send_balance);
        amountSymbolText = findViewById(R.id.token_send_amount_symbol);
        hintText = findViewById(R.id.token_send_hint);
        confirmButton = findViewById(R.id.token_send_confirm);

        swipeRefresh = findViewById(R.id.token_refresh);
        TokenSwipeRefreshHelper.bind(swipeRefresh, this::onPullRefresh);
        TokenTopBarHelper.bind(this);
        updateSendTitle();

        findViewById(R.id.token_send_scan).setOnClickListener(v ->
                TokenQrScanHelper.startCameraScan(this));
        findViewById(R.id.token_send_gallery).setOnClickListener(v ->
                TokenQrScanHelper.startGalleryPicker(this));
        confirmButton.setOnClickListener(v -> showSendConfirm());

        amountInput.addTextChangedListener(simpleWatcher());
        TokenAddressInputHelper.bindAddressField(toInput, this::updateConfirmState);
        TokenAddressInputHelper.bindPasteButton(
                findViewById(R.id.token_send_to_paste), toInput, this, this::updateConfirmState);

        privateKey = TokenWalletHelper.getCurrentPrivateKey(this);
        walletAddress = TokenWalletHelper.getWalletAddress(this);
        contractAddress = TokenSelection.getSelectedContract(this);

        if (!TextUtils.isEmpty(walletAddress)) {
            fromInput.setText(ChainAddressUtil.displayAddress(walletAddress));
        }

        String scanned = getIntent().getStringExtra("scannedData");
        applyScannedAddress(scanned);

        applyCachedBalance();
        TokenBalanceCache.prefetchTokenOnly(this, false, () -> runOnUiThread(() -> {
            if (!isFinishing()) {
                applyCachedBalance();
            }
        }), null);
        updateConfirmState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        contractAddress = TokenSelection.getSelectedContract(this);
        updateSendTitle();
        applyCachedBalance();
    }

    private void updateSendTitle() {
        TextView pageTitle = findViewById(R.id.token_page_title);
        if (pageTitle == null) {
            return;
        }
        TokenItem token = TokenSelection.getSelected(this);
        String symbol = token != null && !TextUtils.isEmpty(token.getSymbol())
                ? token.getSymbol()
                : wrappedBkcConfig.SYMBOL;
        pageTitle.setText(getString(R.string.token_send_title_format, symbol));
        if (amountSymbolText != null) {
            amountSymbolText.setText(symbol);
        }
    }

    private void onPullRefresh() {
        privateKey = TokenWalletHelper.getCurrentPrivateKey(this);
        walletAddress = TokenWalletHelper.getWalletAddress(this);
        contractAddress = TokenSelection.getSelectedContract(this);
        TokenSwipeRefreshHelper.refreshBalances(this, swipeRefresh, () -> {
            if (!TextUtils.isEmpty(walletAddress)) {
                fromInput.setText(ChainAddressUtil.displayAddress(walletAddress));
            }
            if (!applyCachedBalance()) {
                balanceText.setText(formatBalanceLine(BigInteger.ZERO));
                updateConfirmState();
            }
        }, this::notifyBalanceFetchFailed);
    }

    private TextWatcher simpleWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateConfirmState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    private void applyScannedAddress(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return;
        }
        String address = TokenAddressInputHelper.normalizePastedText(raw);
        if (address != null) {
            TokenAddressInputHelper.applyAddressToField(toInput, address);
            updateConfirmState();
        } else {
            Toast.makeText(this, R.string.token_scan_invalid_address, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean applyCachedBalance() {
        TokenBalanceCache.Snapshot snapshot =
                TokenBalanceCache.getSnapshot(walletAddress, contractAddress);
        if (snapshot == null) {
            return false;
        }
        tokenBalance = snapshot.tokenBalance;
        balanceText.setText(formatBalanceLine(tokenBalance));
        updateConfirmState();
        return true;
    }

    private String formatBalanceLine(BigInteger balance) {
        TokenItem token = TokenSelection.getSelected(this);
        String symbol = token != null && !TextUtils.isEmpty(token.getSymbol())
                ? token.getSymbol()
                : wrappedBkcConfig.SYMBOL;
        int decimals = token != null ? token.getDecimals() : TokenConfig.TOKEN_DECIMALS;
        return TokenSendUiHelper.formatAvailableBalance(balance, symbol, decimals);
    }

    private void updateConfirmState() {
        if (submitting) {
            setDisabled(null);
            return;
        }
        if (TextUtils.isEmpty(contractAddress)) {
            setDisabled(getString(R.string.token_hint_no_contract));
            return;
        }
        String to = toInput.getText() != null ? toInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(to) || !ChainAddressUtil.isValidAddress(to)) {
            setDisabled(getString(R.string.token_send_to_invalid));
            return;
        }
        TokenItem token = TokenSelection.getSelected(this);
        int decimals = token != null ? token.getDecimals() : TokenConfig.TOKEN_DECIMALS;
        BigInteger amount = TokenAmountUtil.toChainUnits(
                amountInput.getText() != null ? amountInput.getText().toString() : null,
                decimals);
        if (amount == null || amount.signum() <= 0) {
            setDisabled(getString(R.string.token_button_enter_amount));
            return;
        }
        if (TokenSendUiHelper.isInsufficientFunds(tokenBalance, amount)) {
            setDisabled(BUTTON_STATE_INSUFFICIENT);
            return;
        }
        setEnabled();
    }

    private void notifyBalanceFetchFailed() {
        runOnUiThread(() -> {
            if (!isFinishing()) {
                Toast.makeText(this, R.string.token_balance_fetch_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setDisabled(@Nullable String hint) {
        confirmButton.setEnabled(false);
        hintText.setVisibility(View.GONE);
        if (BUTTON_STATE_INSUFFICIENT.equals(hint)) {
            confirmButton.setText(R.string.token_button_insufficient_funds);
            confirmButton.setBackgroundResource(R.drawable.token_btn_danger_disabled);
            confirmButton.setTextColor(getResources().getColor(R.color.token_btn_danger_text));
            return;
        }
        confirmButton.setBackgroundResource(R.drawable.token_btn_primary_selector);
        confirmButton.setTextColor(getResources().getColor(R.color.white));
        confirmButton.setText(TextUtils.isEmpty(hint) ? getString(R.string.token_send) : hint);
    }

    private void setEnabled() {
        hintText.setVisibility(View.GONE);
        confirmButton.setEnabled(true);
        confirmButton.setText(R.string.token_send);
        confirmButton.setBackgroundResource(R.drawable.token_btn_primary_selector);
        confirmButton.setTextColor(getResources().getColor(R.color.white));
    }

    private void showSendConfirm() {
        if (submitting) {
            return;
        }
        String to = toInput.getText().toString().trim();
        if (!ChainAddressUtil.isValidAddress(to)) {
            Toast.makeText(this, R.string.token_send_to_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        TokenItem token = TokenSelection.getSelected(this);
        int decimals = token != null ? token.getDecimals() : TokenConfig.TOKEN_DECIMALS;
        BigInteger amount = TokenAmountUtil.toChainUnits(amountInput.getText().toString(), decimals);
        if (amount == null || amount.signum() <= 0) {
            Toast.makeText(this, R.string.token_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        String amountDisplay = TokenAmountUtil.formatDisplayAmount(amount, decimals);
        String symbol = resolveSelectedSymbol();
        String fromDisplay = ChainAddressUtil.displayAddress(walletAddress);
        String toDisplay = ChainAddressUtil.displayAddress(to);
        String contractDisplay = ChainAddressUtil.displayAddress(contractAddress);

        TokenConfirmDialog.show(
                this,
                getString(R.string.token_confirm_title_send),
                getString(R.string.token_confirm_send_message,
                        fromDisplay, toDisplay, amountDisplay, symbol, contractDisplay),
                () -> executeSend(to, amount, amountDisplay, symbol));
    }

    private String resolveSelectedSymbol() {
        TokenItem token = TokenSelection.getSelected(this);
        if (token != null && !TextUtils.isEmpty(token.getSymbol())) {
            return token.getSymbol();
        }
        return TokenStore.resolveSymbol(
                this,
                contractAddress,
                wrappedBkcContractHelper.resolveContractAddress(this));
    }

    private void executeSend(String to, BigInteger amount, String amountDisplay, String symbol) {
        submitting = true;
        confirmButton.setEnabled(false);
        confirmButton.setText(R.string.token_submitting);
        hintText.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                String data = TokenContractHelper.encodeTransfer(to, amount);
                String hash = ChainTxHelper.sendContractCall(
                        privateKey,
                        contractAddress,
                        data,
                        "0x0",
                        TokenConfig.DEMO_GAS_LIMIT,
                        TokenConfig.DEMO_GAS_PRICE);
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
                            TokenTxRecord.send(amountDisplay, to, txHash, contractAddress));
                    TokenOutboundNotifyStore.register(
                            this,
                            walletAddress,
                            to,
                            contractAddress,
                            txHash,
                            amountDisplay);
                    TokenBalanceCache.invalidateAfterWrite(walletAddress, contractAddress);
                    Toast.makeText(
                            this,
                            pending
                                    ? getString(R.string.token_tx_submitted)
                                    : getString(R.string.token_send_success, symbol),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    submitting = false;
                    confirmButton.setText(R.string.token_send);
                    String msg = e.getMessage();
                    Toast.makeText(
                            this,
                            TextUtils.isEmpty(msg) ? getString(R.string.token_tx_failed) : msg,
                            Toast.LENGTH_LONG).show();
                    updateConfirmState();
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TokenQrScanHelper.handleActivityResult(
                this,
                requestCode,
                resultCode,
                data,
                address -> {
                    String normalized = TokenAddressInputHelper.normalizePastedText(address);
                    if (normalized != null) {
                        TokenAddressInputHelper.applyAddressToField(toInput, normalized);
                        updateConfirmState();
                    } else {
                        Toast.makeText(this, R.string.token_scan_invalid_address,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}


