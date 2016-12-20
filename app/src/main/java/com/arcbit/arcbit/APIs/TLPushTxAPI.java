package com.arcbit.arcbit.APIs;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLCallback;
import com.arcbit.arcbit.model.TLStealthAddress;

import org.json.JSONException;
import org.json.JSONObject;

public class TLPushTxAPI {
    private static final String TAG = TLPushTxAPI.class.getName();

    private TLAppDelegate appDelegate;

    public TLPushTxAPI(TLAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
    }

    public void sendTx(String txHex, String txHash, String toAddress, TLCallback callback) {
        Log.d(TAG, "sendTx");

        if (!TLStealthAddress.isStealthAddress(toAddress, appDelegate.appWallet.walletConfig.isTestnet)) {
            Log.d(TAG, "sendTx !isStealthAddress");
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    JSONObject jsonData = (JSONObject) msg.obj;
                    Log.d(TAG, "sendTx doInBackground");
                    if (jsonData == null) {
                        callback.onFail(TLNetworking.HTTP_LOCAL_ERROR_CODE, TLNetworking.HTTP_LOCAL_ERROR_MSG);
                        return;
                    }
                    if (jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                        try {
                            Log.d(TAG, "sendTx onFail");
                            callback.onFail(jsonData.getInt(TLNetworking.HTTP_ERROR_CODE), jsonData.getString(TLNetworking.HTTP_ERROR_MSG));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    Log.d(TAG, "sendTx onSuccess");
                    callback.onSuccess(null);
                }
            };

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "sendTx doInBackground");
                        JSONObject jsonData = appDelegate.blockExplorerAPI.pushTx(txHex, txHash);
                        Message message = Message.obtain();
                        message.obj = jsonData;
                        handler.sendMessage(Message.obtain(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else{
            Log.d(TAG, "sendTx isStealthAddress");
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    JSONObject jsonData = (JSONObject) msg.obj;
                    try {
                        if (jsonData == null) {
                            callback.onFail(TLNetworking.HTTP_LOCAL_ERROR_CODE, TLNetworking.HTTP_LOCAL_ERROR_MSG);
                            return;
                        }
                        if (jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                            callback.onFail(jsonData.getInt(TLNetworking.HTTP_ERROR_CODE), jsonData.getString(TLNetworking.HTTP_ERROR_MSG));
                            return;
                        }
                        if (jsonData.has("txid")) {
                            String txid = jsonData.getString("txid");
                            Log.d(TAG, "TLPushTxAPI onPostExecute insightAPI.pushTx txid " + txid);
                            lookupTx(toAddress, txid, callback);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "sendTx doInBackground 2");
                        JSONObject jsonData = appDelegate.blockExplorerAPI.insightAPI.pushTx(txHex, txHash);
                        Message message = Message.obtain();
                        message.obj = jsonData;
                        handler.sendMessage(Message.obtain(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    void lookupTx(String toAddress, String txid, TLCallback callback) {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                JSONObject jsonData = (JSONObject) msg.obj;
                try {
                    Log.d(TAG, "TLPushTxAPI lookupTx jsonData " + jsonData);
                    if (jsonData == null) {
                        callback.onFail(TLNetworking.HTTP_LOCAL_ERROR_CODE, TLNetworking.HTTP_LOCAL_ERROR_MSG);
                        return;
                    }
                    if (jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                        callback.onFail(jsonData.getInt(TLNetworking.HTTP_ERROR_CODE), jsonData.getString(TLNetworking.HTTP_ERROR_MSG));
                        return;
                    }
                    if (jsonData.has(TLStealthExplorerAPI.SERVER_ERROR_CODE)) {
                        int errorCode = jsonData.getInt(TLStealthExplorerAPI.SERVER_ERROR_CODE);
                        String errorMsg = jsonData.getString(TLStealthExplorerAPI.SERVER_ERROR_MSG);
                        if (errorCode == TLStealthExplorerAPI.SEND_TX_ERROR) {
                            Log.d(TAG, "TLPushTxAPI TLStealthExplorerAPI SEND_TX_ERROR " + errorMsg);
                            callback.onFail(errorCode, errorMsg);
                        }
                    } else {
                        Log.d(TAG, "TLPushTxAPI TLStealthExplorerAPI success ");
                        callback.onSuccess(txid);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonData = appDelegate.stealthExplorerAPI.lookupTx(toAddress, txid);
                    Log.d(TAG, "TLPushTxAPI lookupTx 22 " + jsonData);
                    Message message = Message.obtain();
                    message.obj = jsonData;
                    handler.sendMessage(Message.obtain(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
