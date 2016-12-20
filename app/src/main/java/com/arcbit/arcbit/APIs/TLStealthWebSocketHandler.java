package com.arcbit.arcbit.APIs;

import android.os.AsyncTask;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import info.guardianproject.netcipher.client.TlsOnlySocketFactory;

public class TLStealthWebSocketHandler {
    private static final String TAG = TLStealthWebSocketHandler.class.getName();

    private final long pingInterval = 45000L;
    private final long pongTimeout = 5000L;
    private WebSocket webSocketConnection = null;
    private Timer pingTimer = null;
    private boolean pingPongSuccess = false;
    private Messenger messageHandler;
    private String url;

    public TLStealthWebSocketHandler(String url, Messenger messageHandler) {
        this.url = url;
        this.messageHandler = messageHandler;
    }

    public void send(String message) {
        try {
            if (webSocketConnection != null && webSocketConnection.isOpen()) {
                Log.i(TAG, "send " + message);
                webSocketConnection.sendText(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void initialSubscribe() {
        getChallenge();
    }

    public void sendMessage(TLStealthWebSocket.TLTxListenerEvent event) {
        Message message = Message.obtain();
        message.arg1 = event.getTxListenerEvent(event);
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    public void sendMessage(TLStealthWebSocket.TLTxListenerEvent event,  JSONObject jsonObject) {
        Message message = Message.obtain();
        message.arg1 = event.getTxListenerEvent(event);
        message.obj = jsonObject;
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendPing() {
        send("{\"op\":\"ping\"}");
    }

    public synchronized void getChallenge() {
        send("{\"op\":\"challenge\"}");
    }

    public synchronized void sendMessageSubscribeToStealthAddress(String stealthAddress, String signature) {
        send("{\"op\":\"addr_sub\", \"x\":{\"addr\":\""+ stealthAddress +"\",\"sig\":\""+signature+"\"}}");
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
        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (webSocketConnection != null) {
                    if (webSocketConnection.isOpen()) {
                        pingPongSuccess = false;
                        sendPing();
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
                WebSocketFactory factory = new WebSocketFactory();
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, null);
                factory.setSSLSocketFactory(new TlsOnlySocketFactory(sslContext.getSocketFactory(), false) {

                    private Method method;

                    {
                        method = TlsOnlySocketFactory.class.getDeclaredMethod("makeSocketSafe", Socket.class);
                        method.setAccessible(true);
                    }

                    @Override
                    public Socket createSocket() throws IOException {
                        Socket socket = HttpsURLConnection.getDefaultSSLSocketFactory().createSocket();
                        try {
                            return (Socket) method.invoke(this, socket);
                        } catch (IllegalAccessException e) {
                            Log.w(TAG, "Failed to create socket", e);
                        } catch (InvocationTargetException e) {
                            Log.w(TAG, "Failed to create socket", e);
                        }
                        return socket;
                    }
                });

                webSocketConnection = factory
                        .createSocket(url)
                        .addListener(new WebSocketAdapter() {

                            @Override
                            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                                super.onConnected(websocket, headers);
                                Log.d(TAG, "onConnected");
                            }

                            @Override
                            public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                                super.onPongFrame(websocket, frame);
                                pingPongSuccess = true;
                                Log.d(TAG, "onPongFrame");
                            }

                            @Override
                            public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                                Log.d(TAG, "onConnectError " + exception.getLocalizedMessage());
                                sendMessage(TLStealthWebSocket.TLTxListenerEvent.ON_ERROR);
                            }

                            @Override
                            public void onDisconnected(WebSocket websocket,
                                                       WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
                                                       boolean closedByServer) throws Exception {
                                sendMessage(TLStealthWebSocket.TLTxListenerEvent.ON_DISCONNECT);
                                Log.d(TAG, "onDisconnected " + closedByServer);
                            }

                            @Override
                            public void onTextMessage(WebSocket websocket, String message) {
                                try {
                                    JSONObject jsonObject = null;
                                    try {
                                        Log.d(TAG, "onTextMessage " + message);
                                        jsonObject = new JSONObject(message);
                                    } catch (JSONException e) {
                                        jsonObject = null;
                                    }

                                    if (jsonObject == null) {
                                        return;
                                    }
                                    sendMessage(TLStealthWebSocket.TLTxListenerEvent.ON_DATA, jsonObject);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                webSocketConnection.connect();
                sendMessage(TLStealthWebSocket.TLTxListenerEvent.ON_CONNECT);

                initialSubscribe();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
