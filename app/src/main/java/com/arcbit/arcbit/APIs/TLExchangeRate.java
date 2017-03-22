package com.arcbit.arcbit.APIs;

import android.util.Log;

import com.arcbit.arcbit.model.TLCoin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;

public class TLExchangeRate {
    private static final String TAG = TLExchangeRate.class.getName();
    private TLNetworking networking;
    private  static JSONObject exchangeRateDict;
    private NumberFormat formatter;
    public TLExchangeRate() {
        this.networking = new TLNetworking();
        exchangeRateDict = null;
        formatter = NumberFormat.getCurrencyInstance();
        DecimalFormatSymbols decimalFormatSymbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol("");
        ((DecimalFormat) formatter).setDecimalFormatSymbols(decimalFormatSymbols);
    }

    public void getExchangeRates() {
        exchangeRateDict = new JSONObject();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Object obj = networking.getURLNotJSON("https://bitpay.com/api/rates");
                    if (obj == null) {
                        return;
                    }
                    if (obj instanceof String) {
                        try {
                            JSONArray array = new JSONArray((String) obj);
                            for(int i = 0; i < array.length(); i++) {
                                JSONObject dict = array.getJSONObject(i);
                                exchangeRateDict.put(dict.getString("code"), dict);
                            }
                        } catch (JSONException e) {
                            Log.d(TAG, "getExchangeRates onPostExecute String: " + e.getLocalizedMessage());
                        }
                    } else if (obj instanceof JSONObject) {
                        JSONObject jsonObject = (JSONObject) obj;
                        try {
                            Log.d(TAG, "getExchangeRates onFail status: "
                                    + jsonObject.get(TLNetworking.HTTP_ERROR_CODE) + " error: " +
                                    jsonObject.get(TLNetworking.HTTP_ERROR_MSG));
                        } catch (JSONException e) {
                            Log.d(TAG, "getExchangeRates onPostExecute JSONObject: " + e.getLocalizedMessage());
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "getExchangeRates doInBackground: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private double getExchangeRate(String currency) {
        try {
            if (exchangeRateDict != null) {
                JSONObject c = exchangeRateDict.getJSONObject(currency);
                return c.getDouble("rate");
            }
        } catch (JSONException e) {
        }
        return 0;
    }

    private double fiatAmountFromBitcoin(String currency, TLCoin bitcoinAmount) {
        double exchangeRate = getExchangeRate(currency);
        return bitcoinAmount.bigIntegerToBitcoin() * exchangeRate;
    }

    public TLCoin bitcoinAmountFromFiat(String currency, double fiatAmount) {
        double exchangeRate = getExchangeRate(currency);
        return new TLCoin((long)(fiatAmount/exchangeRate*100000000));
    }

    public String fiatAmountStringFromBTC(String currency, TLCoin bitcoinAmount) {
        double money = fiatAmountFromBitcoin(currency, bitcoinAmount);
        String moneyString = formatter.format(money);
        try {
            return String.valueOf(new BigDecimal(formatter.parse(moneyString).toString()).doubleValue());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
