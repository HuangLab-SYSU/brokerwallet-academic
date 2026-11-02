package com.example.brokerfi.token;

import org.junit.Test;
import static org.junit.Assert.*;

public class AssetRecordFilterTest {
    private static final String OLD="0x1111111111111111111111111111111111111111",M="0x2222222222222222222222222222222222222222",PAIR="0x3333333333333333333333333333333333333333";
    private TokenTxRecord swap() { TokenTxRecord r=new TokenTxRecord();r.type="SWAP";r.status="SUCCESS";r.fromAsset="BKC";r.toAsset="MUSDT";r.contractAddress="";r.toContractAddress=M;return r; }
    @Test public void nativeSwapDoesNotAppearAsOldWrappedToken() { assertFalse(AssetRecordFilter.matches(swap(),OLD,OLD)); }
    @Test public void nativeSwapIsVisibleOnBothActualAssets() { assertTrue(AssetRecordFilter.matches(swap(),TokenItem.NATIVE_BKC,OLD));assertTrue(AssetRecordFilter.matches(swap(),M,OLD)); }
    @Test public void reverseUnknownStillAppearsOnBothAssetsWithoutHash() {
        TokenTxRecord r=swap();r.status="UNKNOWN";r.fromAsset="MUSDT";r.toAsset="BKC";r.contractAddress=M;r.toContractAddress="";
        assertTrue(AssetRecordFilter.matches(r,TokenItem.NATIVE_BKC,OLD));assertTrue(AssetRecordFilter.matches(r,M,OLD));assertFalse(AssetRecordFilter.matches(r,OLD,OLD));
    }
    @Test public void approvalIsNotAnExchangeToNativeBkc() {
        TokenTxRecord r=swap();r.type="APPROVAL";r.fromAsset="MUSDT";r.toAsset="BKC";r.contractAddress=M;r.toContractAddress=null;
        assertTrue(AssetRecordFilter.matches(r,M,OLD));assertFalse(AssetRecordFilter.matches(r,TokenItem.NATIVE_BKC,OLD));
    }
    @Test public void legacyWrappedHistoryStillUsesOldContract() { TokenTxRecord r=swap();r.status=null;r.toContractAddress=null;assertTrue(AssetRecordFilter.matches(r,OLD,OLD));assertFalse(AssetRecordFilter.matches(r,TokenItem.NATIVE_BKC,OLD)); }
    @Test public void nativeTransfersStayNativeWithNumericIds() { TokenTxRecord r=new TokenTxRecord();r.type="SEND";r.contractAddress=TokenItem.NATIVE_BKC;r.txHash="123";assertTrue(AssetRecordFilter.matches(r,TokenItem.NATIVE_BKC,OLD));assertFalse(AssetRecordFilter.matches(r,OLD,OLD)); }
    @Test public void liquidityAffectsBkcMusdtAndLp() { TokenTxRecord r=swap();r.type="LIQUIDITY";r.liquidityAction="ADD";r.fromAsset=null;r.toAsset=null;r.contractAddress=PAIR;r.toContractAddress=M;assertTrue(AssetRecordFilter.matches(r,TokenItem.NATIVE_BKC,OLD));assertTrue(AssetRecordFilter.matches(r,M,OLD));assertTrue(AssetRecordFilter.matches(r,PAIR,OLD));assertFalse(AssetRecordFilter.matches(r,OLD,OLD)); }
    @Test public void lpApprovalOnlyBelongsToLp() { TokenTxRecord r=swap();r.type="APPROVAL";r.liquidityAction="APPROVE_LP";r.fromAsset=null;r.toAsset=null;r.contractAddress=PAIR;r.toContractAddress=null;assertTrue(AssetRecordFilter.matches(r,PAIR,OLD));assertFalse(AssetRecordFilter.matches(r,M,OLD));assertFalse(AssetRecordFilter.matches(r,TokenItem.NATIVE_BKC,OLD)); }
}
