package com.example.brokerfi.token;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.core.blockchain.ChainAddressUtil;
import com.example.brokerfi.token.TokenContractHelper;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;

public class TokenAddActivity extends AppCompatActivity {

    private EditText contractInput;
    private Button importButton;
    private boolean importing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_add);

        TokenTopBarHelper.bind(this);
        setupToolbar();

        contractInput = findViewById(R.id.token_contract_input);
        importButton = findViewById(R.id.token_import_button);
        ImageButton pasteBtn = findViewById(R.id.token_contract_paste);
        ImageButton scanBtn = findViewById(R.id.token_contract_scan);

        TokenAddressInputHelper.bindAddressField(contractInput, this::updateImportEnabled);
        TokenAddressInputHelper.bindPasteButton(pasteBtn, contractInput, this, this::updateImportEnabled);
        scanBtn.setOnClickListener(v -> TokenQrScanHelper.startCameraScan(this));
        importButton.setOnClickListener(v -> importToken());

        updateImportEnabled();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TokenQrScanHelper.handleActivityResult(this, requestCode, resultCode, data, address -> {
            String normalized = TokenAddressInputHelper.normalizePastedText(address);
            if (!TextUtils.isEmpty(normalized)) {
                TokenAddressInputHelper.applyAddressToField(contractInput, normalized);
                updateImportEnabled();
            }
        });
    }

    private void setupToolbar() {
        TextView title = findViewById(R.id.token_toolbar_title);
        title.setText(R.string.token_add_title);

        LinearLayout actions = findViewById(R.id.token_toolbar_actions);
        ImageButton helpBtn = createToolbarHelpButton();
        helpBtn.setOnClickListener(v ->
                startActivity(new Intent(this, TokenAddHelpActivity.class)));
        actions.addView(helpBtn);
    }

    private ImageButton createToolbarHelpButton() {
        ImageButton button = new ImageButton(this);
        int size = (int) (40 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        button.setLayoutParams(lp);
        button.setBackgroundResource(android.R.color.transparent);
        button.setImageResource(R.drawable.ic_token_help);
        button.setContentDescription(getString(R.string.token_add_help_desc));
        button.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        button.setPadding(pad, pad, pad, pad);
        return button;
    }

    private void updateImportEnabled() {
        if (importing) {
            importButton.setEnabled(false);
            return;
        }
        String raw = contractInput.getText() != null ? contractInput.getText().toString().trim() : "";
        importButton.setEnabled(ChainAddressUtil.isValidAddress(raw));
    }

    private void importToken() {
        String raw = contractInput.getText() != null ? contractInput.getText().toString().trim() : "";
        if (!ChainAddressUtil.isValidAddress(raw)) {
            Toast.makeText(this, R.string.token_invalid_contract, Toast.LENGTH_SHORT).show();
            return;
        }
        String address = ChainAddressUtil.normalizeAddress(raw);
        if (TokenStore.contains(this, address)) {
            Toast.makeText(this, R.string.token_already_added, Toast.LENGTH_SHORT).show();
            return;
        }

        importing = true;
        importButton.setEnabled(false);
        importButton.setText(R.string.token_importing);

        String privateKey = TokenWalletHelper.getCurrentPrivateKey(this);
        TokenContractHelper.runAsync(() -> {
            TokenMetadataHelper.Erc20Metadata metadata =
                    TokenMetadataHelper.fetchValidatedMetadata(address, privateKey);
            if (metadata == null) {
                throw new IllegalStateException("not_erc20");
            }
            TokenItem item = new TokenItem();
            item.setContractAddress(address);
            item.setSymbol(metadata.symbol);
            item.setName(metadata.name);
            item.setEnabled(true);
            item.setBuiltIn(false);
            item.setCategory("");
            item.setDecimals(metadata.decimals);
            TokenStore.addCustom(TokenAddActivity.this, item);
            return metadata;
        }, new TokenContractHelper.Callback<TokenMetadataHelper.Erc20Metadata>() {
            @Override
            public void onSuccess(TokenMetadataHelper.Erc20Metadata metadata) {
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    selectImportedToken(address);
                    String wallet = TokenWalletHelper.getWalletAddress(TokenAddActivity.this);
                    if (!TextUtils.isEmpty(wallet)) {
                        TokenBalanceCache.invalidateTokenSnapshot(wallet, address);
                    }
                    Toast.makeText(TokenAddActivity.this,
                            R.string.token_import_success, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    importing = false;
                    importButton.setText(R.string.token_import);
                    updateImportEnabled();
                    int toastRes = R.string.token_balance_fetch_failed;
                    if ("not_erc20".equals(message)) {
                        toastRes = R.string.token_not_erc20;
                    } else if ("chain_read_timeout".equals(message)) {
                        toastRes = R.string.token_balance_fetch_failed;
                    }
                    Toast.makeText(TokenAddActivity.this, toastRes, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void selectImportedToken(String address) {
        String wrappedBkcContract = wrappedBkcContractHelper.resolveContractAddress(this);
        for (TokenItem item : TokenStore.loadAll(this, wrappedBkcContract)) {
            if (address.equals(item.getContractAddress())) {
                TokenSelection.setSelected(this, item);
                return;
            }
        }
    }

}
