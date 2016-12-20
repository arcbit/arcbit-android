package com.arcbit.arcbit.APIs;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.utils.TLOSUtil;

public class TLStealthWebSocket {
    private static final String TAG = TLStealthWebSocket.class.getName();

    static public String challenge = "0";

    public enum TLTxListenerEvent {
        ON_DATA, ON_CONNECT, ON_DISCONNECT, ON_ERROR;

        public static int getTxListenerEvent(TLTxListenerEvent event) {
            if (event == ON_DATA) {
                return 0;
            }
            if (event == ON_CONNECT) {
                return 1;
            }
            if (event == ON_DISCONNECT) {
                return 2;
            }
            return 3;
        }

        public static TLTxListenerEvent getTxListenerEvent(int idx) {
            if (idx == 0) {
                return ON_DATA;
            }
            if (idx == 1) {
                return ON_CONNECT;
            }
            if (idx == 2) {
                return ON_DISCONNECT;
            }
            return ON_ERROR;
        }
    }

    public static class MessageHandler extends Handler {
        TLAppDelegate appDelegate;
        public MessageHandler(TLAppDelegate appDelegate) {
            super(Looper.getMainLooper());
            this.appDelegate = appDelegate;
            this.appDelegate.preferences.resetStealthExplorerAPIURL();
            this.appDelegate.preferences.resetStealthWebSocketPort();
        }

        @Override
        public void handleMessage(Message message) {
            TLTxListenerEvent state = TLTxListenerEvent.getTxListenerEvent(message.arg1);
            Log.d(TAG, "TLStealthWebSocket: state " + state);
            if (state == TLTxListenerEvent.ON_DATA) {
                JSONObject jsonObject = (JSONObject) message.obj;
                Log.d(TAG, "TLStealthWebSocket: jsonObject " + jsonObject.toString());
                try {
                    String op = jsonObject.getString("op");
                    if (op.equals("challenge") && jsonObject.has("x")) {
                        appDelegate.respondToStealthChallegeNotification(jsonObject.getJSONObject("x"));
                    } else if (op.equals("addr_sub") && jsonObject.has("x")) {
                        appDelegate.respondToStealthAddressSubscription(jsonObject.getJSONObject("x"));
                    } else if (op.equals("tx") && jsonObject.has("x")) {
                        appDelegate.respondToStealthPayment(jsonObject.getJSONObject("x"));
                    }

                }catch (JSONException e) {

                }

            } else if (state == TLTxListenerEvent.ON_CONNECT) {
            } else if (state == TLTxListenerEvent.ON_DISCONNECT) {
                appDelegate.setAccountsListeningToStealthPaymentsToFalse();
            } else if (state == TLTxListenerEvent.ON_ERROR) {

            }
        }
    }

    public Handler messageHandler;
    TLAppDelegate appDelegate;

    public TLStealthWebSocket(TLAppDelegate appDelegate){
        this.appDelegate = appDelegate;
        this.messageHandler = new MessageHandler(appDelegate);
    }

    public void reconnect() {
        if (isWebSocketOpen()) {
            close();
            start();
        } else {
            start();
        }
    }

    public boolean isWebSocketOpen() {
        return TLOSUtil.getInstance(appDelegate.context).isServiceRunning(TLStealthWebSocketService.class);
    }

    public void sendMessageGetChallenge() {
        Intent intent = new Intent(TLStealthWebSocketService.ACTION_INTENT_GET_CHALLENGE);
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(intent);
    }

    public void sendMessageSubscribeToStealthAddress(String stealthAddress, String signature) {
        Intent intent = new Intent(TLStealthWebSocketService.ACTION_INTENT_SUBSCRIBE_TO_STEALTH_ADDRESS);
        intent.putExtra("address", stealthAddress);
        intent.putExtra("signature", signature);
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(intent);
    }

    private void start() {
        Context context = appDelegate.context;
        Intent startService = new Intent(context, TLStealthWebSocketService.class);
        String urlString = String.format("%s://%s:%d%s", appDelegate.stealthServerConfig.getWebSocketProtocol(),
                appDelegate.preferences.getStealthExplorerURL(), appDelegate.preferences.getStealthWebSocketPort(),
                appDelegate.stealthServerConfig.getWebSocketEndpoint());
        startService.putExtra("URL", urlString);
        startService.putExtra("MESSENGER", new Messenger(messageHandler));
        context.startService(startService);
    }

    public void close() {
        appDelegate.context.stopService(new Intent(appDelegate.context, TLStealthWebSocketService.class));
    }
}
