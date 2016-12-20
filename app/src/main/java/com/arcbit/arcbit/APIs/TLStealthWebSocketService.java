package com.arcbit.arcbit.APIs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;

public class TLStealthWebSocketService extends android.app.Service {

    public static final String ACTION_INTENT_SUBSCRIBE_SEND_PING = "io.arcbit.wallet.WebSocketService.SUBSCRIBE_SEND_PING";
    public static final String ACTION_INTENT_GET_CHALLENGE = "io.arcbit.wallet.WebSocketService.GET_CHALLENGE";
    public static final String ACTION_INTENT_SUBSCRIBE_TO_STEALTH_ADDRESS = "io.arcbit.wallet.WebSocketService.SUBSCRIBE_TO_STEALTH_ADDRESS";

    private final IBinder mBinder = new LocalBinder();
    private TLStealthWebSocketHandler webSocketHandler = null;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (ACTION_INTENT_SUBSCRIBE_TO_STEALTH_ADDRESS.equals(intent.getAction())) {
                if(webSocketHandler != null) webSocketHandler.sendMessageSubscribeToStealthAddress(intent.getStringExtra("address"), intent.getStringExtra("signature"));
            } else if (ACTION_INTENT_GET_CHALLENGE.equals(intent.getAction())) {
                if(webSocketHandler != null) webSocketHandler.getChallenge();
            } else if (ACTION_INTENT_SUBSCRIBE_SEND_PING.equals(intent.getAction())) {
                if(webSocketHandler != null) webSocketHandler.sendPing();
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        Bundle extras = intent.getExtras();
        Messenger messageHandler = (Messenger) extras.get("MESSENGER");
        String url = (String) extras.get("URL");
        webSocketHandler = new TLStealthWebSocketHandler(url, messageHandler);
        webSocketHandler.start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        IntentFilter filterSendPing = new IntentFilter(ACTION_INTENT_SUBSCRIBE_SEND_PING);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filterSendPing);
        IntentFilter filterGetChallege = new IntentFilter(ACTION_INTENT_GET_CHALLENGE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filterGetChallege);
        IntentFilter filterSubscribeToStealthAddress = new IntentFilter(ACTION_INTENT_SUBSCRIBE_TO_STEALTH_ADDRESS);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filterSubscribeToStealthAddress);
    }

    @Override
    public void onDestroy() {
        if(webSocketHandler != null) webSocketHandler.stop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public TLStealthWebSocketService getService() {
            return TLStealthWebSocketService.this;
        }
    }
}