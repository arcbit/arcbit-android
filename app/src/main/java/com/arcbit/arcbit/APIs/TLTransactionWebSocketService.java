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

public class TLTransactionWebSocketService extends android.app.Service {

    public static final String ACTION_INTENT = "io.arcbit.wallet.WebSocketService.SUBSCRIBE_TO_ADDRESS";
    private final IBinder mBinder = new LocalBinder();
    private TLTransactionWebSocketHandler webSocketHandler = null;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {

            if (ACTION_INTENT.equals(intent.getAction())) {
                webSocketHandler.subscribeToAddress(intent.getStringExtra("address"));
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
        TLBlockExplorerAPI.TLBlockExplorer blockExplorerAPI = (TLBlockExplorerAPI.TLBlockExplorer) extras.get("BLOCK_EXPLORER_API");
        String url = null;
        if (blockExplorerAPI == TLBlockExplorerAPI.TLBlockExplorer.Insight) {
            url = (String) extras.get("URL");
        }
        webSocketHandler = new TLTransactionWebSocketHandler(messageHandler);
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

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        if(webSocketHandler != null) webSocketHandler.stop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public TLTransactionWebSocketService getService() {
            return TLTransactionWebSocketService.this;
        }
    }
}