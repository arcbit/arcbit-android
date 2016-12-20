package com.arcbit.arcbit.model;


public class TLSendFormData {
    public enum TLSelectObjectType { Unknown, Account, Address;}

    public boolean useAllFunds = false;
    public TLCoin beforeSendBalance;
    public String fromLabel;
    public TLCoin toAmount;
    public TLCoin feeAmount;
    private String address = null;
    private String amount = null;
    private String fiatAmount = null;

    TLSendFormData() {
    }
    
    public void setAddress(String address){
        this.address = address;
    }

    public String getAddress(){
        return address;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getAmount(){
        return amount;
    }

    public void setFiatAmount(String fiatAmount){ this.fiatAmount = fiatAmount; }

    public String getFiatAmount(){
        return fiatAmount;
    }
}

