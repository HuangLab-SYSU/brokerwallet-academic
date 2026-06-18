package com.example.brokerfi.core.blockchain.model;

import com.google.gson.annotations.SerializedName;

public class SendETHTXReq {
    private String PublicKey;
    private String RandomStr;
    private String To;
    @SerializedName("data")
    private String Data;
    @SerializedName("value")
    private String Value;
    private String Gas;
    private String Sign1;
    private String Sign2;

    public String getPublicKey() {
        return PublicKey;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getRandomStr() {
        return RandomStr;
    }

    public void setRandomStr(String randomStr) {
        RandomStr = randomStr;
    }

    public String getTo() {
        return To;
    }

    public void setTo(String to) {
        To = to;
    }

    public String getData() {
        return Data;
    }

    public void setData(String data) {
        this.Data = data;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        this.Value = value;
    }

    public String getGas() {
        return Gas;
    }

    public void setGas(String gas) {
        Gas = gas;
    }

    public String getSign1() {
        return Sign1;
    }

    public void setSign1(String sign1) {
        Sign1 = sign1;
    }

    public String getSign2() {
        return Sign2;
    }

    public void setSign2(String sign2) {
        Sign2 = sign2;
    }
}
