package com.example.brokerfi.xc.model;

import java.util.List;

public class TransactionResponse {
    private List<Transaction> data;

    public List<Transaction> getData() {
        return data;
    }

    public void setData(List<Transaction> data) {
        this.data = data;
    }
}