package com.arcbit.arcbit.model;

public class TLTransactionFee {

    public static long DEFAULT_FIXED_FEE_AMOUNT = 10000;
    public static long MAX_FIXED_FEE_AMOUNT = 1000000;
    public static long MIN_FIXED_FEE_AMOUNT = 10000;

    public static boolean isTransactionFeeTooHigh(TLCoin amount) {
        TLCoin maxFeeAmount = new TLCoin(MAX_FIXED_FEE_AMOUNT);
        if (amount.greater(maxFeeAmount)) {
            return true;
        }
        return false;
    }

    public static boolean isTransactionFeeTooLow(TLCoin amount) {
        TLCoin minFeeAmount = new TLCoin(MIN_FIXED_FEE_AMOUNT);
        if (amount.less(minFeeAmount)) {
            return true;
        }
        return false;
    }

    public static boolean isValidInputTransactionFee(TLCoin amount) {
        TLCoin maxFeeAmount = new TLCoin(MAX_FIXED_FEE_AMOUNT);
        TLCoin minFeeAmount = new TLCoin(MIN_FIXED_FEE_AMOUNT);
        if (amount.greater(maxFeeAmount)) {
            return false;
        }
        if (amount.less(minFeeAmount)) {
            return false;
        }

        return true;
    }
}
