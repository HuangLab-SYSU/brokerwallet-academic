package com.example.brokerfi.token;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.xc.ChainAddressUtil;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 灞曠ず褰撳墠閫変腑浠ｅ竵鐨勫悎绾﹀湴鍧€涓庯紙鍐呯疆 wBKC锛夋簮鐮併€?*/
public class TokenInfoActivity extends AppCompatActivity {

    public static final String EXTRA_CONTRACT = TokenSelection.EXTRA_CONTRACT;
    public static final String EXTRA_NAME = TokenSelection.EXTRA_NAME;
    public static final String EXTRA_SYMBOL = TokenSelection.EXTRA_SYMBOL;
    public static final String EXTRA_BUILT_IN = TokenSelection.EXTRA_BUILT_IN;

    private static final String CONTRACT_ASSET = "wrapped_bkc_contract.sol";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_info);

        TokenTopBarHelper.bind(this);
        TokenSubHeaderHelper.bindInfoButton(this);

        TextView title = findViewById(R.id.token_page_title);
        if (title != null) {
            title.setText(R.string.token_info_title);
        }

        TextView introView = findViewById(R.id.token_info_intro);
        TextView addressView = findViewById(R.id.token_info_address);
        TextView copyBtn = findViewById(R.id.token_info_copy_address);
        TextView sourceView = findViewById(R.id.token_info_source);
        View sourceLabel = findViewById(R.id.token_info_source_label);

        boolean builtIn = getIntent().getBooleanExtra(EXTRA_BUILT_IN, false);
        String tokenName = getIntent().getStringExtra(EXTRA_NAME);
        String address = getIntent().getStringExtra(EXTRA_CONTRACT);
        if (TextUtils.isEmpty(address)) {
            address = TokenSelection.getSelectedContract(this);
        }
        if (TextUtils.isEmpty(address)) {
            address = wrappedBkcContractHelper.resolveContractAddress(this);
        }

        if (builtIn) {
            introView.setText(R.string.token_info_intro_body);
            if (sourceLabel != null) {
                sourceLabel.setVisibility(View.VISIBLE);
            }
            sourceView.setVisibility(View.VISIBLE);
            sourceView.setText(loadContractSource());
        } else {
            String intro = getString(R.string.token_info_custom_intro, tokenName != null ? tokenName : "");
            introView.setText(intro);
            if (sourceLabel != null) {
                sourceLabel.setVisibility(View.GONE);
            }
            sourceView.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(address)) {
            addressView.setText(R.string.token_contract_missing_short);
            copyBtn.setEnabled(false);
        } else {
            String display = ChainAddressUtil.displayAddress(address);
            addressView.setText(display);
            copyBtn.setEnabled(true);
            copyBtn.setOnClickListener(v -> TokenContractUiHelper.copyContractAddress(this, display));
            addressView.setOnClickListener(v -> TokenContractUiHelper.copyContractAddress(this, display));
        }
    }

    private String loadContractSource() {
        try (InputStream in = getAssets().open(CONTRACT_ASSET);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return "// " + getString(R.string.token_info_source_load_failed);
        }
    }
}


