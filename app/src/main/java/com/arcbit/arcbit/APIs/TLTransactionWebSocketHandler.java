package com.arcbit.arcbit.APIs;

import android.os.AsyncTask;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public class TLTransactionWebSocketHandler {

    private final long pingInterval = 20000L;
    private final long pongTimeout = 5000L;
    private WebSocket webSocketConnection = null;
    private Set<String> sentMessageSet = new HashSet<String>();
    private Timer pingTimer = null;
    private boolean pingPongSuccess = false;
    private Messenger messageHandler;

    public TLTransactionWebSocketHandler(Messenger messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void send(String message) {
        if (!sentMessageSet.contains(message)) {
            try {
                if (webSocketConnection != null && webSocketConnection.isOpen()) {
                    webSocketConnection.sendText(message);
                    sentMessageSet.add(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void initialSubscribe() {
        send("{\"op\":\"blocks_sub\"}");
//        send("{\"op\":\"unconfirmed_sub\"}");
    }

    public void sendMessage(TLTransactionListener.TLTxListenerEvent event) {
        Message message = Message.obtain();
        message.arg1 = event.getTxListenerEvent(event);
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    public void sendMessage(TLTransactionListener.TLTxListenerEvent event,  JSONObject jsonObject) {
        Message message = Message.obtain();
        message.arg1 = event.getTxListenerEvent(event);
        message.obj = jsonObject;
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribeToAddress(String address) {
        if(address!= null && !address.isEmpty()) {
            send("{\"op\":\"addr_sub\", \"addr\":\"" + address + "\"}");
        }
    }

    public void stop() {
        stopPingTimer();
        if (webSocketConnection != null && webSocketConnection.isOpen()) {
            webSocketConnection.disconnect();
        }
    }

    public void start() {
        try {
            stop();
            connect();
            startPingTimer();
        } catch (IOException | com.neovisionaries.ws.client.WebSocketException e) {
            e.printStackTrace();
        }
    }

    private void startPingTimer() {
        if(pingTimer != null) {
            pingTimer.cancel();
        }
        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (webSocketConnection != null) {
                    pingTimer = null;
                    if (webSocketConnection.isOpen()) {
                        pingPongSuccess = false;
                        webSocketConnection.sendPing();
                        startPongTimer();
                    } else {
                        start();
                    }
                }
            }
        }, pingInterval, pingInterval);
    }

    private void stopPingTimer() {
        if (pingTimer != null) pingTimer.cancel();
    }

    private void startPongTimer() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!pingPongSuccess) {
                    start();
                }
            }
        }, pongTimeout);
    }

    private void connect() throws IOException, WebSocketException {
        new ConnectionTask().execute();
    }

    private class ConnectionTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... args) {

            try {
                sentMessageSet.clear();

                webSocketConnection = new WebSocketFactory()
                        .createSocket("wss://ws.blockchain.info/inv")
                        .addListener(new WebSocketAdapter() {

                            @Override
                            public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                                super.onPongFrame(websocket, frame);
                                pingPongSuccess = true;
                            }

                            @Override
                            public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                                sendMessage(TLTransactionListener.TLTxListenerEvent.ON_ERROR);
                            }

                            @Override
                            public void onDisconnected(WebSocket websocket,
                                                       WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
                                                       boolean closedByServer) throws Exception {
                                sendMessage(TLTransactionListener.TLTxListenerEvent.ON_DISCONNECT);
                            }

                            @Override
                            public void onTextMessage(WebSocket websocket, String message) {
                                try {
                                    JSONObject jsonObject = null;
                                    try {
                                        jsonObject = new JSONObject(message);
                                    } catch (JSONException e) {
                                    }
                                    if (jsonObject == null) {
                                        return;
                                    }
                                    sendMessage(TLTransactionListener.TLTxListenerEvent.ON_DATA, jsonObject);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                webSocketConnection.connect();
                sendMessage(TLTransactionListener.TLTxListenerEvent.ON_CONNECT);

                initialSubscribe();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
