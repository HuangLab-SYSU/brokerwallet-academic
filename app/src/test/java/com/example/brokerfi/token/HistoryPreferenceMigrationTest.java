package com.example.brokerfi.token;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class HistoryPreferenceMigrationTest {
    private final String wallet="09ba20fa6eddfdcad5518f518242b22288165869";
    @Test public void prefixedMixedCaseAndUnprefixedWalletUseTheSameStorageKey() {
        assertEquals(wallet,HistoryPreferenceMigration.canonicalWallet(" 0x"+wallet.toUpperCase()+" "));
        assertEquals(wallet,HistoryPreferenceMigration.canonicalWallet(wallet));
    }
    @Test public void onlyAFullAddressIsCanonicalized() {
        assertEquals("0xabc",HistoryPreferenceMigration.canonicalWallet("0xabc"));
        assertEquals("",HistoryPreferenceMigration.canonicalWallet(null));
    }
    @Test public void validLegacyAliasesCanBeReadWithoutDeletingOrFabricatingRows() {
        assertEquals("list_0x"+wallet,HistoryPreferenceMigration.aliasKey("list_"+wallet));
        assertEquals("list_"+wallet,HistoryPreferenceMigration.aliasKey("list_0x"+wallet));
        assertEquals("list_abc",HistoryPreferenceMigration.aliasKey("list_abc"));
    }
    @Test public void anotherWalletAlreadyHavingCurrentHistoryDoesNotHideLegacyHistory() {
        Map<String,Object> current=new HashMap<>(),legacy=new HashMap<>();
        current.put("list_current","[]");legacy.put("list_old","[{\"status\":\"UNKNOWN\"}]");
        Map<String,String> copy=HistoryPreferenceMigration.missingLists(current,legacy);
        assertEquals(legacy.get("list_old"),copy.get("list_old"));
        assertEquals(1,current.size());assertEquals(1,legacy.size());
    }
    @Test public void existingCurrentRowsAreNeverOverwrittenByOldUnknownOrSuccess() {
        Map<String,Object> current=new HashMap<>(),legacy=new HashMap<>();
        current.put("list_same","[{\"status\":\"UNKNOWN\"}]");legacy.put("list_same","[{\"status\":\"SUCCESS\"}]");
        assertTrue(HistoryPreferenceMigration.missingLists(current,legacy).isEmpty());
    }
    @Test public void migrationOnlyCopiesHistoryStringsAndDoesNotChangeTheirContents() {
        Map<String,Object> legacy=new HashMap<>();legacy.put("list_old","[{\"status\":\"UNKNOWN\"}]");
        legacy.put("list_bad",12);legacy.put("unrelated","ignored");
        Map<String,String> copy=HistoryPreferenceMigration.missingLists(new HashMap<>(),legacy);
        assertEquals(1,copy.size());assertEquals(legacy.get("list_old"),copy.get("list_old"));
    }
    @Test public void oldAliasCannotShadowCurrentHistoryForTheSameWallet() {
        Map<String,Object> current=new HashMap<>(),legacy=new HashMap<>();
        current.put("list_0x"+wallet,"current");legacy.put("list_"+wallet,"legacy");
        assertTrue(HistoryPreferenceMigration.missingLists(current,legacy).isEmpty());
    }
}
