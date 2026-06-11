package com.example.brokerfi.token;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import com.example.brokerfi.xc.ChainAddressUtil;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;

import java.util.ArrayList;
import java.util.List;

/** Manage-tokens screen. Starts with built-in wBKC and allows additional imports. */
public class TokenManageActivity extends AppCompatActivity {

    public static final int TAB_ALL = 0;
    public static final int TAB_POPULAR = 1;

    private static final int REQUEST_ADD = 8801;

    private EditText searchInput;
    private TextView tabAll;
    private TextView tabPopular;
    private TextView emptyView;
    private RecyclerView tokenList;
    private TokenListAdapter adapter;

    private String wrappedBkcContractAddress = "";
    private final List<TokenItem> allTokens = new ArrayList<>();
    private int activeTab = TAB_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_manage);

        TokenTopBarHelper.bind(this);
        setupToolbar();

        searchInput = findViewById(R.id.token_search);
        tabAll = findViewById(R.id.token_tab_all);
        tabPopular = findViewById(R.id.token_tab_popular);
        emptyView = findViewById(R.id.token_empty);
        tokenList = findViewById(R.id.token_list);

        wrappedBkcContractAddress = wrappedBkcContractHelper.resolveContractAddress(this);

        adapter = new TokenListAdapter();
        adapter.setListener((item, enabled) -> {
            if (item.isBuiltIn()) {
                return;
            }
            syncEnabledInAllTokens(item.getContractAddress(), enabled);
            TokenStore.setEnabled(TokenManageActivity.this, item.getContractAddress(), enabled);
            if (!enabled) {
                TokenItem current = TokenSelection.getSelected(TokenManageActivity.this);
                if (current != null
                        && item.getContractAddress().equals(current.getContractAddress())) {
                    TokenItem fallback =
                            TokenSelection.ensureValidSelection(TokenManageActivity.this);
                    TokenSelection.setSelected(TokenManageActivity.this, fallback);
                }
                String wallet = TokenWalletHelper.getWalletAddress(TokenManageActivity.this);
                if (!TextUtils.isEmpty(wallet)) {
                    TokenBalanceCache.invalidateTokenSnapshot(wallet, item.getContractAddress());
                }
            }
            applyFilters();
        });

        tokenList.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.token_list_divider));
        tokenList.addItemDecoration(divider);
        tokenList.setAdapter(adapter);

        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                applyFilters();
            }
        });

        tabAll.setOnClickListener(v -> selectTab(TAB_ALL));
        tabPopular.setOnClickListener(v -> selectTab(TAB_POPULAR));

        styleTabs();
        reloadTokens();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD && resultCode == RESULT_OK) {
            reloadTokens();
        }
    }

    private void setupToolbar() {
        TextView title = findViewById(R.id.token_toolbar_title);
        title.setText(R.string.token_manage_title);

        LinearLayout actions = findViewById(R.id.token_toolbar_actions);
        TextView addBtn = createAddButton();
        addBtn.setOnClickListener(v ->
                startActivityForResult(new Intent(this, TokenAddActivity.class), REQUEST_ADD));
        actions.addView(addBtn);
    }

    private TextView createAddButton() {
        TextView button = new TextView(this);
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (40 * density);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(size, size);
        lp.setMarginStart((int) (8 * density));
        button.setLayoutParams(lp);
        button.setBackgroundResource(android.R.color.transparent);
        button.setText("+");
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        button.setTypeface(Typeface.DEFAULT);
        button.setTextColor(ContextCompat.getColor(this, R.color.black));
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(getString(R.string.token_add));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private void reloadTokens() {
        allTokens.clear();
        allTokens.addAll(TokenStore.loadAll(this, wrappedBkcContractAddress));
        applyFilters();
    }

    private void syncEnabledInAllTokens(String contractAddress, boolean enabled) {
        String target = ChainAddressUtil.normalizeAddress(contractAddress);
        for (TokenItem token : allTokens) {
            if (target.equals(ChainAddressUtil.normalizeAddress(token.getContractAddress()))) {
                token.setEnabled(enabled);
                return;
            }
        }
    }

    private void selectTab(int tab) {
        activeTab = tab;
        styleTabs();
        applyFilters();
    }

    private void styleTabs() {
        styleTab(tabAll, activeTab == TAB_ALL, true);
        styleTab(tabPopular, activeTab == TAB_POPULAR, false);
    }

    private void styleTab(TextView tab, boolean selected, boolean leftSide) {
        int backgroundRes;
        if (leftSide) {
            backgroundRes = selected
                    ? R.drawable.token_tab_left_selected
                    : R.drawable.token_tab_left_normal;
        } else {
            backgroundRes = selected
                    ? R.drawable.token_tab_right_selected
                    : R.drawable.token_tab_right_normal;
        }
        tab.setBackgroundResource(backgroundRes);
        tab.setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.black));
    }

    private void applyFilters() {
        String query = searchInput.getText() != null ? searchInput.getText().toString() : "";
        List<TokenItem> filtered = applyVisibleList(query);
        adapter.submitList(filtered);
        updateEmptyState(filtered.isEmpty(), query);
        tokenList.setVisibility(filtered.isEmpty() ? RecyclerView.GONE : RecyclerView.VISIBLE);
    }

    private List<TokenItem> applyVisibleList(String query) {
        List<TokenItem> filtered = new ArrayList<>();
        String normalized = query == null ? "" : query.trim().toLowerCase();
        for (TokenItem item : allTokens) {
            if (activeTab == TAB_POPULAR
                    && !TokenItem.CATEGORY_POPULAR.equals(item.getCategory())
                    && !item.isBuiltIn()) {
                continue;
            }
            if (!TextUtils.isEmpty(normalized)
                    && !matchesSearch(item, normalized)) {
                continue;
            }
            filtered.add(item);
        }
        filtered.sort((a, b) -> {
            int byName = safeLower(a.getName()).compareTo(safeLower(b.getName()));
            if (byName != 0) {
                return byName;
            }
            return safeLower(a.getSymbol()).compareTo(safeLower(b.getSymbol()));
        });
        return filtered;
    }

    private void updateEmptyState(boolean empty, String query) {
        if (!empty) {
            emptyView.setVisibility(TextView.GONE);
            return;
        }
        emptyView.setVisibility(TextView.VISIBLE);
        boolean hasSearch = !TextUtils.isEmpty(query != null ? query.trim() : "");
        if (hasSearch || activeTab != TAB_ALL) {
            emptyView.setText(R.string.token_list_empty_filtered);
        } else {
            emptyView.setText(R.string.token_list_empty);
        }
    }

    private static boolean matchesSearch(TokenItem item, String query) {
        String name = safeLower(item.getName());
        String symbol = safeLower(item.getSymbol());
        String address = safeLower(item.getContractAddress());
        return name.contains(query)
                || symbol.contains(query)
                || address.contains(query);
    }

    private static String safeLower(String value) {
        return value != null ? value.toLowerCase() : "";
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
