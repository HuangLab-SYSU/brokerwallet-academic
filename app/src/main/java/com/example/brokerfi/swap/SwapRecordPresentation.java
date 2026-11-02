package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxRecord;

/** Estimated amounts and chain-confirmed amounts are deliberately separate. */
public final class SwapRecordPresentation {
    private SwapRecordPresentation() {}
    public static String status(String state) {
        switch (state) {
            case "SUBMITTING": return "Sending / awaiting verification";
            case "UNKNOWN": return "Outcome unknown. Do not send again.";
            case "PENDING": return "Waiting for confirmation";
            case "SUCCESS": return "Confirmed on-chain";
            case "FAILED": return "Failed on-chain";
            case "NOT_SENT": return "Not sent";
            default: return "Manually reviewed";
        }
    }
    public static String details(TokenTxRecord r) {
        if (r.isLiquidity()) return liquidityDetails(r)+SwapSendDiagnostics.details(r)+TradeRecoveryPolicy.details(r);
        boolean approval = "APPROVAL".equals(r.type);
        return status(r.status) + "\n" + (approval ? "Approval limit: " : "You pay: ") + r.amountDisplay + " " + r.fromSymbol
                + (approval ? "\nSpender: " + r.routerAddress : "\nEstimated received: " + r.estimatedOutput + " " + r.toSymbol
                + "\nMinimum received: " + r.minimumOutput + " " + r.toSymbol
                + "\nActual received: " + (r.actualOutput == null ? "Unverified (not replaced with an estimate)" : r.actualOutput + " " + r.toSymbol)
                + "\nSlippage: " + r.slippageBps / 100.0 + "%  Price impact: " + r.impactBps / 100.0 + "%")
                + "\nNetwork: " + r.chainId + "\nRouter：" + r.routerAddress
                + "\nPayment asset contract: " + (r.contractAddress == null || r.contractAddress.isEmpty() ? "Native BKC" : r.contractAddress)
                + (approval ? "" : "\nReceiving asset contract: " + (r.toContractAddress == null || r.toContractAddress.isEmpty() ? "Native BKC" : r.toContractAddress))
                + "\nNetwork fee: " + (r.gasCost == null ? "Unverified" : r.gasCost + " BKC")
                + "\nTransaction hash: " + (r.txHash == null ? "Not available. Check the on-chain outcome first." : r.txHash)
                + SwapSendDiagnostics.details(r)+TradeRecoveryPolicy.details(r);
    }
    public static String label(TokenTxRecord r) {
        if (r.isLiquidity()) return r.liquidityAction.startsWith("APPROVE") ? "Liquidity approval" : "ADD".equals(r.liquidityAction) ? "Add Liquidity" : "Remove Liquidity";
        return "APPROVAL".equals(r.type) ? "Approve" : "Swap";
    }
    private static String liquidityDetails(TokenTxRecord r) {
        String amounts=r.liquidityAction.startsWith("APPROVE") ? "Approval limit: "+r.amountDisplay+" "+r.fromSymbol
                : "Input: "+r.amountDisplay+"\nEstimated LP: "+(r.estimatedLp==null?"—":r.estimatedLp)
                +"\nActual LP: "+(r.actualLp==null?"Unverified":r.actualLp)
                +"\nActual BKC: "+(r.actualBkc==null?"Unverified":r.actualBkc)
                +"\nActual mUSDT: "+(r.actualMusdt==null?"Unverified":r.actualMusdt)
                +"\nMinimum BKC: "+units(r.minimumBkcUnits,18)+"\nMinimum mUSDT: "+units(r.minimumMusdtUnits,6)
                + ("ADD".equals(r.liquidityAction)?"\nMinimum LP: "+units(r.minimumLpUnits,18):"");
        return label(r)+" · "+status(r.status)+"\n"+amounts+"\nNetwork: "+r.chainId+"\nRouter："+r.routerAddress
                +"\nNetwork fee: "+(r.gasCost==null?"Unverified":r.gasCost+" BKC")
                +"\nTransaction hash: "+(r.txHash==null?"Not available. Check the on-chain outcome first.":r.txHash);
    }
    private static String units(String value,int decimals) {
        return value==null ? "—" : SwapQuoteMath.format(new java.math.BigInteger(value),decimals);
    }
}
