package com.arcbit.arcbit.APIs;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class TLTxFeeAPI {
    private final String TAG = getClass().getSimpleName();
    private TLAppDelegate appDelegate;
    private TLNetworking networking;

    private JSONObject cachedDynamicFees;
    private long cachedDynamicFeesTime = 0;

    public TLTxFeeAPI(TLAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
        this.networking = new TLNetworking();
    }

    public enum TLDynamicFeeSetting {
        FastestFee, HalfHourFee, HourFee;

        public static TLDynamicFeeSetting getDynamicFeeOption(int idx) {
            if (idx == 0) {
                return FastestFee;
            }
            if (idx == 1) {
                return HalfHourFee;
            }
            if (idx == 2) {
                return HourFee;
            }
            return FastestFee;
        }

        public static String getAPIValue(TLDynamicFeeSetting dynamicFeeOption) {
            if (dynamicFeeOption == FastestFee) {
                return "fastestFee";
            }
            if (dynamicFeeOption == HalfHourFee) {
                return "halfHourFee";
            }
            if (dynamicFeeOption == HourFee) {
                return "hourFee";
            }
            return "";
        }

        public static int getDynamicFeeOptionIdx(TLDynamicFeeSetting dynamicFeeOption) {
            if (dynamicFeeOption == FastestFee) {
                return 0;
            }
            if (dynamicFeeOption == HalfHourFee) {
                return 1;
            }
            if (dynamicFeeOption == HourFee) {
                return 2;
            }
            return 0;
        }

        public static TLDynamicFeeSetting toMyEnum(String myEnumString) {
            try {
                return valueOf(myEnumString);
            } catch (Exception ex) {
                return FastestFee;
            }
        }
    }

    public Long getCachedDynamicFee() {
        if (cachedDynamicFees != null) {
            TLDynamicFeeSetting dynamicFeeSetting = appDelegate.preferences.getDynamicFeeOption();
            try {
                if (dynamicFeeSetting == TLDynamicFeeSetting.FastestFee) {
                    return cachedDynamicFees.getLong(TLDynamicFeeSetting.getAPIValue(TLDynamicFeeSetting.FastestFee));
                } else if (dynamicFeeSetting == TLDynamicFeeSetting.HalfHourFee) {
                    return cachedDynamicFees.getLong(TLDynamicFeeSetting.getAPIValue(TLDynamicFeeSetting.HalfHourFee));
                } else if (dynamicFeeSetting == TLDynamicFeeSetting.HourFee) {
                    return cachedDynamicFees.getLong(TLDynamicFeeSetting.getAPIValue(TLDynamicFeeSetting.HourFee));
                }
            } catch (JSONException e) {
            }
        }
        return null;
    }

    public boolean haveUpdatedCachedDynamicFees() {
        long nowUnixTime = new Date().getTime()/1000;
        long tenMinutesInSeconds = 600;
        if (cachedDynamicFeesTime == 0 || nowUnixTime - cachedDynamicFeesTime > tenMinutesInSeconds) {
            return false;
        }
        return true;
    }

    public void getDynamicTxFee(TLCallback callback) {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                JSONObject jsonData = (JSONObject) msg.obj;
                try {
                    if (jsonData == null || jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                        callback.onFail(jsonData.getInt(TLNetworking.HTTP_ERROR_CODE), jsonData.getString(TLNetworking.HTTP_ERROR_MSG));
                        cachedDynamicFeesTime = 0;
                        return;
                    }
                    cachedDynamicFeesTime = new Date().getTime()/1000;
                    cachedDynamicFees = jsonData;
                    callback.onSuccess(null);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject object = networking.getURL("https://bitcoinfees.21.co/api/v1/fees/recommended");
                    Message message = Message.obtain();
                    message.obj = object;
                    handler.sendMessage(Message.obtain(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
