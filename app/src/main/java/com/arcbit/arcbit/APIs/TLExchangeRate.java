package com.arcbit.arcbit.APIs;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.arcbit.arcbit.model.TLCallback;
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

    public void getExchangeRates(TLCallback callback) {
        exchangeRateDict = new JSONObject();
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Object obj = msg.obj;
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
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } catch (JSONException e) {
                        Log.d(TAG, "getExchangeRates onPostExecute String: " + e.getLocalizedMessage());
                        if (callback != null) {
                            callback.onFail(-1001, e.getLocalizedMessage());
                        }
                    }
                } else if (obj instanceof JSONObject) {
                    JSONObject jsonObject = (JSONObject) obj;
                    try {
                        Log.d(TAG, "getExchangeRates onFail status: "
                                + jsonObject.get(TLNetworking.HTTP_ERROR_CODE) + " error: " +
                                jsonObject.get(TLNetworking.HTTP_ERROR_MSG));
                        if (callback != null) {
                            callback.onFail(jsonObject.getInt(TLNetworking.HTTP_ERROR_CODE), jsonObject.getString(TLNetworking.HTTP_ERROR_MSG));
                        }
                    } catch (JSONException e) {
                        Log.d(TAG, "getExchangeRates onPostExecute JSONObject: " + e.getLocalizedMessage());
                        if (callback != null) {
                            callback.onFail(-1001, e.getLocalizedMessage());
                        }
                    }
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Object obj = networking.getURLNotJSON("https://bitpay.com/api/rates");
                    Message message = Message.obtain();
                    message.obj = obj;
                    handler.sendMessage(Message.obtain(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Object obj = networking.getURLNotJSON("https://bitpay.com/api/rates");

                } catch (Exception e) {
                    Log.d(TAG, "getExchangeRates doInBackground: " + e.getLocalizedMessage());
                    e.printStackTrace();
                    if (callback != null) {
                        callback.onFail(-1001, e.getLocalizedMessage());
                    }
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
