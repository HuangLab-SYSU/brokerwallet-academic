package com.example.brokerfi.token;

import static org.junit.Assert.assertEquals;

import com.example.brokerfi.token.wrappedbkc.wrappedBkcConfig;

import org.junit.Test;

public class wrappedBkcConfigTest {

    @Test
    public void symbol_usesConfiguredWalletDisplaySymbol() {
        assertEquals("wBKC", wrappedBkcConfig.SYMBOL);
    }
}



