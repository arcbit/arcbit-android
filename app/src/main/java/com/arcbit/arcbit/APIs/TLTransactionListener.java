package com.arcbit.arcbit.APIs;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.utils.TLOSUtil;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class TLTransactionListener {
    private static final String TAG = TLTransactionListener.class.getName();
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
        }

        @Override
        public void handleMessage(Message message) {
            TLTxListenerEvent state = TLTxListenerEvent.getTxListenerEvent(message.arg1);
            Log.d(TAG, "TLTransactionListener: state " + state);
            if (state == TLTxListenerEvent.ON_DATA) {
                JSONObject jsonObject = (JSONObject) message.obj;
                Log.d(TAG, "TLTransactionListener: jsonObject " + jsonObject.toString());
                try {
                    String op = jsonObject.getString("op");
                    if (op.equals("utx") && jsonObject.has("x")) {
                        appDelegate.updateModelWithNewTransaction(jsonObject.getJSONObject("x"));
                    } else if (op.equals("block") && jsonObject.has("x")) {
                        appDelegate.updateModelWithNewBlock(jsonObject.getJSONObject("x"));
                    }

                }catch (JSONException e) {

                }
            } else if (state == TLTxListenerEvent.ON_CONNECT) {
                appDelegate.listenToIncomingTransactionForWallet();
            } else if (state == TLTxListenerEvent.ON_DISCONNECT) {
                appDelegate.setWalletTransactionListenerClosed();

            } else if (state == TLTxListenerEvent.ON_ERROR) {

            }
        }
    }

    public Handler messageHandler;
    TLAppDelegate appDelegate;
    public TLBlockExplorerAPI.TLBlockExplorer blockExplorerAPI = TLBlockExplorerAPI.TLBlockExplorer.Blockchain;

    int MAX_CONSECUTIVE_FAILED_CONNECTIONS = 5;
    private Socket mSocket;
    int consecutiveFailedConnections = 0;

    public TLTransactionListener(TLAppDelegate appDelegate){
        this.appDelegate = appDelegate;
        this.messageHandler = new MessageHandler(appDelegate);
        blockExplorerAPI = appDelegate.preferences.getBlockExplorerAPI();
    }

    public void reconnect() {
        if (blockExplorerAPI == TLBlockExplorerAPI.TLBlockExplorer.Blockchain) {
            if (isWebSocketOpen()) {
                close();
                start();
            } else {
                start();
            }
        } else {
            try {
                String blockExplorerURL = appDelegate.preferences.getBlockExplorerURL(blockExplorerAPI);
                mSocket = IO.socket(blockExplorerURL);
                mSocket.on(mSocket.EVENT_CONNECT, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {
                        mSocket.emit("subscribe", "inv");
                        consecutiveFailedConnections = 0;
                        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_TRANSACTION_LISTENER_OPEN));
//                        mSocket.on("tx", new Emitter.Listener() {
//                            @Override
//                            public void call(Object... args) {
//                                JSONObject obj = (JSONObject)args[0];
//                                Log.d(TAG, "mSocket TLTransactionListener tx " + obj);
//                            }
//                        });
                        mSocket.on("block", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                Log.d(TAG, "mSocket TLTransactionListener block args " + args);
                                Log.d(TAG, "mSocket TLTransactionListener block args[0] " + args[0]);
                                if (args[0] instanceof String) {
                                    try {
                                        JSONObject obj = new JSONObject((String)args[0]);
                                        Log.d(TAG, "mSocket TLTransactionListener block JSONObject " + obj);
                                    } catch (JSONException e) {
                                        Log.d(TAG, "mSocket TLTransactionListener block e " + e.getLocalizedMessage());
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }

                }).on(mSocket.EVENT_DISCONNECT, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {
                        Log.d(TAG, "mSocket EVENT_DISCONNECT " + args);
                        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_TRANSACTION_LISTENER_CLOSE));
                        if (consecutiveFailedConnections++ < MAX_CONSECUTIVE_FAILED_CONNECTIONS) {
                            reconnect();
                        }
                    }

                });
                mSocket.connect();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isWebSocketOpen() {
        if (blockExplorerAPI == TLBlockExplorerAPI.TLBlockExplorer.Blockchain) {
            return TLOSUtil.getInstance(appDelegate.context).isServiceRunning(TLTransactionWebSocketService.class);
        } else {
            return mSocket.connected();
        }
    }

//    private boolean sendWebSocketMessage(String msg) {
//        return true;
//    }

    public void listenToIncomingTransactionForAddress(final String address) {
        if (blockExplorerAPI == TLBlockExplorerAPI.TLBlockExplorer.Blockchain) {
            //Log.d(TAG, "listenToIncomingTransactionForAddress Blockchain " + address);
            Intent intent = new Intent(TLTransactionWebSocketService.ACTION_INTENT);
            intent.putExtra("address", address);
            LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(intent);
        } else {
            JSONArray addresses = new JSONArray();
            addresses.put(address);
            //Log.d(TAG, "mSocket listenToIncomingTransactionForAddress Insight addresses " + addresses);
            mSocket.emit("unsubscribe", "bitcoind/addresstxid", addresses);
            mSocket.emit("subscribe", "bitcoind/addresstxid", addresses);

            mSocket.on("bitcoind/addresstxid", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject obj = (JSONObject)args[0];
                    Log.d(TAG, "mSocket listenToIncomingTransactionForAddress " + obj);
                    getAndBroadcastTxObject(obj, address);
                }
            });
        }
    }

    private void getAndBroadcastTxObject(JSONObject obj, String address) {
        if (!obj.has("address")) {
            return;
        }
        try {
            String addr = obj.getString("address");
            if (!addr.equals(address) || !obj.has("txid")) {
                return;
            }
            String txid = obj.getString("txid");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "mSocket getAndBroadcastTxObject " + txid);
                        JSONObject jsonData = appDelegate.blockExplorerAPI.getTx(txid);
                        if (jsonData != null && !jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                            appDelegate.updateModelWithNewTransaction(jsonData);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "mSocket getAndBroadcastTxObject e " + e.getLocalizedMessage());
                    }
                }
            }).start();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void start() {
        Context context = appDelegate.context;

        Intent startService = new Intent(context, TLTransactionWebSocketService.class);
        TLBlockExplorerAPI.TLBlockExplorer blockExplorerAPI = appDelegate.preferences.getBlockExplorerAPI();
        startService.putExtra("BLOCK_EXPLORER_API", blockExplorerAPI);
        if (blockExplorerAPI == TLBlockExplorerAPI.TLBlockExplorer.Insight) {
            startService.putExtra("URL", appDelegate.preferences.getBlockExplorerURL(TLBlockExplorerAPI.TLBlockExplorer.Insight));
        }
        startService.putExtra("MESSENGER", new Messenger(messageHandler));
        context.startService(startService);
    }

    public void close() {
        if (blockExplorerAPI == TLBlockExplorerAPI.TLBlockExplorer.Blockchain) {
            appDelegate.context.stopService(new Intent(appDelegate.context, TLTransactionWebSocketService.class));
        } else {
            mSocket.disconnect();
        }
    }
}
