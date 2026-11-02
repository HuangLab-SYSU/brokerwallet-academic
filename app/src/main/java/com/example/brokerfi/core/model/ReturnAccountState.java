package com.example.brokerfi.core.model;

import com.google.gson.annotations.SerializedName;

public class ReturnAccountState {
    @SerializedName("account")
    private String AccountAddr;
    @SerializedName("balance")
    private String Balance;
    private boolean isHidden = false;
    private String accountName;
    private boolean isNewPrivateKeyFormat;
    // syh_work_01 new: the original subscript of account in private key string（replace “Account N”）
    private int accountIndex;

    public String getAccountAddr() {
        return AccountAddr;
    }

    public void setAccountAddr(String accountAddr) {
        AccountAddr = accountAddr;
    }

    public String getBalance() {
        return Balance;
    }

    public void setBalance(String balance) {
        Balance = balance;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public boolean isNewPrivateKeyFormat() {
        return isNewPrivateKeyFormat;
    }

    public void setNewPrivateKeyFormat(boolean newPrivateKeyFormat) {
        isNewPrivateKeyFormat = newPrivateKeyFormat;
    }

    // syh_work_01 new：the setter & getter of accountIndex
    public int getAccountIndex() {
        return accountIndex;
    }

    public void setAccountIndex(int accountIndex) {
        this.accountIndex = accountIndex;
    }
}
