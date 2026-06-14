package com.example.brokerfi.token;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcConfig;
import com.example.brokerfi.core.blockchain.ChainAddressUtil;
import android.text.TextUtils;

import com.example.brokerfi.token.TokenConfig;

import java.util.Locale;

/** Token entry used by the manage-tokens list and persisted user imports. */
public class TokenItem {

    public static final String CATEGORY_POPULAR = "popular";
    public static final String CATEGORY_STABLE = "stable";

    private String contractAddress;
    private String name;
    private String symbol;
    private boolean enabled;
    private boolean builtIn;
    private String category;
    private int decimals;

    public TokenItem() {
    }

    public static TokenItem builtInwrappedBkc(String contractAddress) {
        TokenItem item = new TokenItem();
        item.contractAddress = ChainAddressUtil.normalizeAddress(contractAddress);
        item.name = "wrapped BKC";
        item.symbol = wrappedBkcConfig.SYMBOL;
        item.enabled = true;
        item.builtIn = true;
        item.category = CATEGORY_POPULAR;
        item.decimals = TokenConfig.TOKEN_DECIMALS;
        return item;
    }

    /** Matches the official wrapped BKC deployment address without relying on {@code builtIn}. */
    public boolean matchesOfficialwrappedBkc(String officialContractAddress) {
        if (TextUtils.isEmpty(contractAddress) || TextUtils.isEmpty(officialContractAddress)) {
            return false;
        }
        return ChainAddressUtil.normalizeAddress(contractAddress)
                .equalsIgnoreCase(ChainAddressUtil.normalizeAddress(officialContractAddress));
    }

    public static String iconLetter(String symbol) {
        if (TextUtils.isEmpty(symbol)) {
            return "?";
        }
        if (wrappedBkcConfig.SYMBOL.equalsIgnoreCase(symbol)) {
            return wrappedBkcConfig.SYMBOL.substring(0, 1);
        }
        return symbol.substring(0, 1).toUpperCase(Locale.US);
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getDecimals() {
        return decimals > 0 ? decimals : TokenConfig.TOKEN_DECIMALS;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public String displaySubtitle() {
        return symbol != null ? symbol : "";
    }
}
