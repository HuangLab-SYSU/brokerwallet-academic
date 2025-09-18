package com.example.brokerfi.xc.dto;

public class MedalQueryResult {
    private String address;
    private int goldMedals;
    private int silverMedals;
    private int bronzeMedals;
    private int totalMedals;
    
    public MedalQueryResult() {}
    
    public MedalQueryResult(String address, int goldMedals, int silverMedals, int bronzeMedals) {
        this.address = address;
        this.goldMedals = goldMedals;
        this.silverMedals = silverMedals;
        this.bronzeMedals = bronzeMedals;
        this.totalMedals = goldMedals + silverMedals + bronzeMedals;
    }
    
    // Getters and Setters
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public int getGoldMedals() {
        return goldMedals;
    }
    
    public void setGoldMedals(int goldMedals) {
        this.goldMedals = goldMedals;
    }
    
    public int getSilverMedals() {
        return silverMedals;
    }
    
    public void setSilverMedals(int silverMedals) {
        this.silverMedals = silverMedals;
    }
    
    public int getBronzeMedals() {
        return bronzeMedals;
    }
    
    public void setBronzeMedals(int bronzeMedals) {
        this.bronzeMedals = bronzeMedals;
    }
    
    public int getTotalMedals() {
        return totalMedals;
    }
    
    public void setTotalMedals(int totalMedals) {
        this.totalMedals = totalMedals;
    }
}




