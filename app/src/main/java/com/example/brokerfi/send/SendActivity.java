package com.example.brokerfi.send;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.example.brokerfi.core.security.SecurityUtil;
import com.example.brokerfi.core.util.MyUtil;
import com.example.brokerfi.main.MainActivity;
import com.example.brokerfi.core.blockchain.ChainAddressUtil;
import com.example.brokerfi.core.blockchain.ChainTxHelper;
import com.example.brokerfi.token.AssetBalanceRow;
import com.example.brokerfi.token.TokenAddressInputHelper;
import com.example.brokerfi.token.TokenAmountUtil;
import com.example.brokerfi.token.TokenBalanceCache;
import com.example.brokerfi.token.TokenConfig;
import com.example.brokerfi.token.TokenContractHelper;
import com.example.brokerfi.token.TokenItem;
import com.example.brokerfi.token.TokenOutboundNotifyStore;
import com.example.brokerfi.token.TokenPickerDialog;
import com.example.brokerfi.token.TokenQrScanHelper;
import com.example.brokerfi.token.TokenSelection;
import com.example.brokerfi.token.TokenStore;
import com.example.brokerfi.token.TokenTxHistoryStore;
import com.example.brokerfi.token.TokenTxRecord;
import com.example.brokerfi.token.TokenWalletHelper;

import java.math.BigDecimal;
import java.math.BigInteger;


public class SendActivity extends AppCompatActivity {
    private static final String STATE_ASSET = "send_asset";
    private TokenItem asset;
    private String walletAddress;

    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private EditText edt_sendfrom;
    private EditText edt_sendto;
    private EditText edt_amount;
    private SeekBar seekBarFee;
    private EditText edtFeeValue;
    private NavigationHelper navigationHelper;
    private Button button;

    // Gas price presets: Slow(20), Medium(50), Fast(100) in Gwei
    private static final int[] FEE_PRESETS = {20, 50, 100};
    private String currentFee = "50"; // default to Medium

    // Gas limit fields
    private EditText edtGasLimit;
    private Button btnGasAdvanced;
    private LinearLayout gasLimitStepper;
    private Button btnGasPlus;
    private Button btnGasMinus;
    private TextView tvGasWarning;
    private String currentGasLimit = "21000";
    private boolean gasLimitAdvanced = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        String contract = savedInstanceState == null
                ? getIntent().getStringExtra(TokenSelection.EXTRA_CONTRACT)
                : savedInstanceState.getString(STATE_ASSET);
        asset = TokenPickerDialog.initialAsset(this, contract);
        intView();
        intEvent();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(SendActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }


    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        edt_sendfrom = findViewById(R.id.edt_sendfrom);



        edt_sendfrom.setEnabled(false);
        edt_sendto = findViewById(R.id.edt_sendto);

        edt_amount=findViewById(R.id.edt_amount);
        edtFeeValue = findViewById(R.id.edt_fee_value);
        seekBarFee = findViewById(R.id.seekbar_fee);

        // Setup gas price slider
        setupFeeSlider();

        // Setup gas limit section
        setupGasLimit();

        button=findViewById(R.id.btn_send);
    }

    private boolean updatingFee = false; // prevent infinite EditText <-> SeekBar loop

    private void setupFeeSlider() {
        edtFeeValue.setText(currentFee);

        // SeekBar -> EditText
        seekBarFee.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (updatingFee) return;
                if (fromUser) {
                    currentFee = String.valueOf(progress);
                    updatingFee = true;
                    edtFeeValue.setText(currentFee);
                    updatingFee = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // EditText -> SeekBar
        edtFeeValue.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (updatingFee) return;
                String val = s.toString().trim();
                if (val.isEmpty()) return;
                try {
                    double d = Double.parseDouble(val);
                    currentFee = val;
                    int progress = (int) Math.round(d);
                    if (progress < 0) progress = 0;
                    if (progress > seekBarFee.getMax()) progress = seekBarFee.getMax();
                    updatingFee = true;
                    seekBarFee.setProgress(progress);
                    updatingFee = false;
                } catch (NumberFormatException ignored) {}
            }
        });

        // Preset label clicks snap slider + edittext
        findViewById(R.id.label_slow).setOnClickListener(v -> snapToPreset(0));
        findViewById(R.id.label_medium).setOnClickListener(v -> snapToPreset(1));
        findViewById(R.id.label_fast).setOnClickListener(v -> snapToPreset(2));
    }

    private void snapToPreset(int index) {
        int value = FEE_PRESETS[index];
        currentFee = String.valueOf(value);
        updatingFee = true;
        seekBarFee.setProgress(value);
        edtFeeValue.setText(currentFee);
        updatingFee = false;
    }

    private void setupGasLimit() {
        edtGasLimit = findViewById(R.id.edt_gas_limit);
        btnGasAdvanced = findViewById(R.id.btn_gas_advanced);
        gasLimitStepper = findViewById(R.id.gas_limit_stepper);
        btnGasPlus = findViewById(R.id.btn_gas_plus);
        btnGasMinus = findViewById(R.id.btn_gas_minus);
        tvGasWarning = findViewById(R.id.tv_gas_warning);

        edtGasLimit.setText(currentGasLimit);

        // Advanced button: toggle editability and stepper visibility
        btnGasAdvanced.setOnClickListener(v -> {
            gasLimitAdvanced = !gasLimitAdvanced;
            if (gasLimitAdvanced) {
                edtGasLimit.setEnabled(true);
                edtGasLimit.setFocusable(true);
                edtGasLimit.setFocusableInTouchMode(true);
                gasLimitStepper.setVisibility(View.VISIBLE);
                btnGasAdvanced.setText("Simple");
                validateGasLimit(currentGasLimit);
            } else {
                edtGasLimit.setEnabled(false);
                edtGasLimit.setFocusable(false);
                edtGasLimit.setFocusableInTouchMode(false);
                gasLimitStepper.setVisibility(View.GONE);
                btnGasAdvanced.setText("Advanced");
                tvGasWarning.setVisibility(View.GONE);
                edtGasLimit.setTextColor(Color.parseColor("#999999"));
                currentGasLimit = "21000";
                edtGasLimit.setText(currentGasLimit);
            }
        });

        // EditText change listener: validate and update currentGasLimit
        edtGasLimit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) return;
                currentGasLimit = val;
                if (gasLimitAdvanced) {
                    validateGasLimit(val);
                }
            }
        });

        // +/- stepper buttons
        btnGasPlus.setOnClickListener(v -> adjustGasLimit(1));
        btnGasMinus.setOnClickListener(v -> adjustGasLimit(-1));
    }

    private void adjustGasLimit(int delta) {
        try {
            long val = Long.parseLong(currentGasLimit);
            val += delta;
            if (val < 0) val = 0;
            currentGasLimit = String.valueOf(val);
            edtGasLimit.setText(currentGasLimit);
            validateGasLimit(currentGasLimit);
        } catch (NumberFormatException ignored) {}
    }

    private void validateGasLimit(String val) {
        try {
            long limit = Long.parseLong(val);
            if (limit < 21000) {
                edtGasLimit.setTextColor(Color.RED);
                tvGasWarning.setVisibility(View.VISIBLE);
            } else {
                edtGasLimit.setTextColor(Color.BLACK);
                tvGasWarning.setVisibility(View.GONE);
            }
        } catch (NumberFormatException e) {
            edtGasLimit.setTextColor(Color.RED);
            tvGasWarning.setVisibility(View.VISIBLE);
        }
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);

        String scannedData = getIntent().getStringExtra("scannedData");
        if (scannedData != null) applyScannedAddress(scannedData);
        findViewById(R.id.send_scan).setOnClickListener(v -> TokenQrScanHelper.startCameraScan(this));
        findViewById(R.id.send_gallery).setOnClickListener(v -> TokenQrScanHelper.startGalleryPicker(this));
        findViewById(R.id.send_asset_picker).setOnClickListener(v -> {
            if (tx) return;
            TokenPickerDialog.show(this, TokenStore.getWalletAssets(this),
                    asset.getContractAddress(), item -> {
                        if (asset.getContractAddress().equals(item.getContractAddress())) return;
                        asset = item;
                        edt_amount.setText("");
                        updateAsset();
                    });
        });
        button.setOnClickListener(v -> prepareSend());
    }

    @Override
    protected void onResume() {
        super.onResume();
        String current = TokenWalletHelper.getWalletAddress(this);
        if (walletAddress != null && !TextUtils.equals(walletAddress, current)) {
            edt_amount.setText("");
        }
        walletAddress = current;
        edt_sendfrom.setText(ChainAddressUtil.displayAddress(walletAddress));
        updateAsset();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ASSET, asset.getContractAddress());
    }

    private void updateAsset() {
        TextView picker = findViewById(R.id.send_asset_picker);
        picker.setText(getString(R.string.wallet_asset_picker, asset.getSymbol()));
        TextView amountLabel = findViewById(R.id.send_amount_label);
        amountLabel.setText(getString(R.string.wallet_amount_for_asset, asset.getSymbol()));
        findViewById(R.id.send_native_gas).setVisibility(asset.isNative() ? View.VISIBLE : View.GONE);
        showAvailableBalance();
        if (asset.isNative() || TextUtils.isEmpty(walletAddress)) return;
        String wallet = walletAddress;
        String contract = asset.getContractAddress();
        TokenBalanceCache.prefetchTokenBalance(getApplicationContext(), contract, true,
                () -> runOnUiThread(() -> {
                    if (!isDestroyed() && TextUtils.equals(wallet, walletAddress)
                            && asset.getContractAddress().equals(contract)) {
                        showAvailableBalance();
                    }
                }));
    }

    private void showAvailableBalance() {
        String balance = AssetBalanceRow.cachedBalance(this, walletAddress, asset);
        String value = balance == null ? getString(R.string.wallet_balance_unavailable)
                : getString(R.string.wallet_asset_balance, balance, asset.getSymbol());
        TextView available = findViewById(R.id.send_available_balance);
        available.setText(getString(R.string.wallet_send_available, value));
    }

    private void prepareSend() {
        if (tx) return;
        String key = TokenWalletHelper.getCurrentPrivateKey(this);
        if (TextUtils.isEmpty(key) || !SecurityUtil.isNewPrivateKeyFormat(key)) {
            Toast.makeText(this, R.string.token_no_account, Toast.LENGTH_SHORT).show();
            return;
        }
        String wallet = SecurityUtil.GetAddress(key);
        if (!TextUtils.equals(wallet, walletAddress)) {
            Toast.makeText(this, R.string.wallet_send_account_changed, Toast.LENGTH_SHORT).show();
            return;
        }
        String recipient = edt_sendto.getText().toString().trim();
        if (!SecurityUtil.isAddressFormatValid(recipient)) {
            Toast.makeText(this, R.string.token_send_to_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        TokenItem selected = asset;
        BigInteger units = TokenAmountUtil.toChainUnits(edt_amount.getText().toString(), selected.getDecimals());
        if (units == null || units.signum() <= 0 || units.bitLength() > 256) {
            Toast.makeText(this, R.string.token_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }
        String fee = currentFee;
        String gasLimit = currentGasLimit;
        if (selected.isNative()) {
            try {
                if (new BigDecimal(fee).signum() < 0 || Long.parseLong(gasLimit) <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.wallet_send_check_gas, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            TokenBalanceCache.Snapshot balance = TokenBalanceCache.resolveSnapshot(this, wallet,
                    selected.getContractAddress());
            if (balance == null || balance.tokenBalance.compareTo(units) < 0) {
                Toast.makeText(this, balance == null ? R.string.token_balance_fetch_failed
                        : R.string.token_button_insufficient_funds, Toast.LENGTH_SHORT).show();
                if (balance == null) updateAsset();
                return;
            }
        }
        showConfirmDialog(key, wallet, ChainAddressUtil.displayAddress(recipient), selected,
                units, fee, gasLimit);
    }

    private void applyScannedAddress(String raw) {
        String address = ChainAddressUtil.parseAddressFromQr(raw);
        if (address != null) TokenAddressInputHelper.applyAddressToField(edt_sendto, address);
    }

    private volatile boolean tx = false;
    private AlertDialog confirmDialog;
    // Dialog view references kept as fields so the post-send UI can be updated in place.
    private Button btnConfirm;
    private Button btnCancel;
    private LinearLayout hashRow;
    private TextView tvHash;
    private Button btnCopyHash;

    private void showConfirmDialog(String key, String wallet, String toAddress, TokenItem selected,
            BigInteger units, String fee, String gasLimit) {
        String amount = TokenAmountUtil.fromChainUnits(units, selected.getDecimals());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_transaction, null);

        TextView tvFrom = dialogView.findViewById(R.id.tv_from);
        TextView tvTo = dialogView.findViewById(R.id.tv_to);
        TextView tvAmount = dialogView.findViewById(R.id.tv_amount);
        TextView tvFee = dialogView.findViewById(R.id.tv_fee);
        TextView tvGasLimit = dialogView.findViewById(R.id.tv_gas_limit);
        TextView tvTotal = dialogView.findViewById(R.id.tv_total);

        tvFrom.setText(getString(R.string.dialog_confirm_transaction_dialog_from) + " "
                + ChainAddressUtil.displayAddress(wallet));
        tvTo.setText(getString(R.string.dialog_confirm_transaction_dialog_to) + " " + toAddress);
        tvAmount.setText(getString(R.string.dialog_confirm_transaction_amount) + " "
                + amount + " " + selected.getSymbol());
        if (selected.isNative()) {
            BigDecimal maxFee = new BigDecimal(fee).multiply(new BigDecimal(gasLimit)).movePointLeft(9);
            String total = new BigDecimal(amount).add(maxFee).stripTrailingZeros().toPlainString();
            tvFee.setText(getString(R.string.dialog_confirm_transaction_gas_price) + " "
                    + fee + " " + getString(R.string.send_activity_gwei));
            tvGasLimit.setText("Gas Limit: " + gasLimit);
            tvTotal.setText(getString(R.string.wallet_send_max_total, total));
        } else {
            tvFee.setVisibility(View.GONE);
            tvGasLimit.setVisibility(View.GONE);
            tvTotal.setText(getString(R.string.wallet_asset_contract,
                    ChainAddressUtil.displayAddress(selected.getContractAddress())));
        }

        // Hash row references (hidden until the tx is broadcast)
        hashRow = dialogView.findViewById(R.id.hash_row);
        tvHash = dialogView.findViewById(R.id.tv_hash);
        btnCopyHash = dialogView.findViewById(R.id.btn_copy_hash);
        hashRow.setVisibility(View.GONE);

        // Copy the displayed hash to the system clipboard.
        btnCopyHash.setOnClickListener(v -> {
            String label = "Hash: ";
            String content = tvHash.getText().toString();
            String hash = content.startsWith(label) ? content.substring(label.length()) : content;
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && !hash.isEmpty()) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Transaction Hash", hash));
                Toast.makeText(SendActivity.this, "Hash copied", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(dialogView);

        btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        btnCancel.setOnClickListener(v -> {
            confirmDialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            if (tx) return;
            if (!TextUtils.equals(key, TokenWalletHelper.getCurrentPrivateKey(this))) {
                confirmDialog.dismiss();
                Toast.makeText(this, R.string.wallet_send_account_changed, Toast.LENGTH_SHORT).show();
                return;
            }
            tx = true;
            confirmDialog.setCancelable(false);
            btnConfirm.setText(R.string.token_submitting);
            btnConfirm.setEnabled(false);
            btnCancel.setEnabled(false);
            new Thread(() -> submitTransfer(key, wallet, toAddress, selected, units, amount, fee, gasLimit),
                    "wallet-send").start();
        });

        confirmDialog = builder.create();
        confirmDialog.getWindow().setBackgroundDrawableResource(R.color.black);
        confirmDialog.show();
    }

    private void submitTransfer(String key, String wallet, String recipient, TokenItem selected,
            BigInteger units, String amount, String fee, String gasLimit) {
        try {
            String hash;
            boolean pending = false;
            String contract = selected.getContractAddress();
            if (selected.isNative()) {
                String response = MyUtil.SendTX(key, SecurityUtil.removeAddressPrefix(recipient),
                        amount, fee, gasLimit);
                if (response == null || !response.startsWith("success:")) {
                    throw new IllegalStateException(response);
                }
                hash = response.substring("success:".length());
            } else {
                hash = ChainTxHelper.sendContractCall(key, contract,
                        TokenContractHelper.encodeTransfer(recipient, units), "0x0",
                        TokenConfig.DEMO_GAS_LIMIT, TokenConfig.DEMO_GAS_PRICE);
                if (TextUtils.isEmpty(hash)) {
                    throw new IllegalStateException(getString(R.string.token_tx_failed)
                            + "\n" + ChainTxHelper.getSendFailureHint(this));
                }
                ChainTxHelper.ReceiptStatus receipt = ChainTxHelper.waitReceiptStatus(hash, key);
                if (receipt == ChainTxHelper.ReceiptStatus.FAILED) {
                    throw new IllegalStateException(getString(R.string.token_tx_failed));
                }
                pending = receipt == ChainTxHelper.ReceiptStatus.PENDING;
                String displayAmount = TokenAmountUtil.formatDisplayAmount(units, selected.getDecimals());
                TokenTxHistoryStore.add(this, wallet,
                        TokenTxRecord.send(displayAmount, recipient, hash, contract));
                TokenOutboundNotifyStore.register(this, wallet, recipient, contract, hash, displayAmount);
            }
            TokenBalanceCache.invalidateAfterWrite(wallet, contract);
            String message = pending ? getString(R.string.token_tx_submitted)
                    : getString(R.string.token_send_success, selected.getSymbol());
            runOnUiThread(() -> showTransferResult(hash, message));
        } catch (Exception e) {
            String message = TextUtils.isEmpty(e.getMessage()) ? getString(R.string.token_tx_failed) : e.getMessage();
            runOnUiThread(() -> showTransferResult(null, message));
        }
    }

    private void showTransferResult(String hash, String message) {
        tx = false;
        if (isFinishing() || isDestroyed()) return;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        confirmDialog.setCancelable(true);
        if (hash == null) {
            confirmDialog.dismiss();
            return;
        }
        edt_amount.setText("");
        updateAsset();
        tvHash.setText("Hash: " + hash);
        hashRow.setVisibility(View.VISIBLE);
        btnConfirm.setText(android.R.string.ok);
        btnConfirm.setEnabled(true);
        btnConfirm.setOnClickListener(v -> confirmDialog.dismiss());
    }

    @Override
    public void onBackPressed() {
        if (tx) return;
      
        if (confirmDialog != null && confirmDialog.isShowing()) {
            confirmDialog.dismiss();
        } 
        
        else if (navigationHelper != null && navigationHelper.isPopupVisible()) {
            navigationHelper.hidePopup();
        } 
        
        else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TokenQrScanHelper.handleActivityResult(this, requestCode, resultCode, data,
                address -> TokenAddressInputHelper.applyAddressToField(edt_sendto, address));
    }
}
