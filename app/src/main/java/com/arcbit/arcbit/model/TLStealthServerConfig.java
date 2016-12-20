package com.arcbit.arcbit.model;

public class TLStealthServerConfig {
    private String stealthServerUrl = "www.arcbit.net";
    private int stealthServerPort = 443;
    private int webSocketServerPort = 443;
    private String webServerProtocol = "https";
    private String webSocketProtocol = "wss";
    private String webSocketEndpoint = "/inv";

    public TLStealthServerConfig() {}

    public String getWebServerProtocol() {
        return this.webServerProtocol;
    }

    public String getWebSocketProtocol() {
        return this.webSocketProtocol;
    }

    public String getWebSocketEndpoint() {
        return this.webSocketEndpoint;
    }

    public String getStealthServerUrl() { return this.stealthServerUrl; }

    public int getStealthServerPort() {
        return this.stealthServerPort;
    }

    public int getWebSocketServerPort() {
        return this.webSocketServerPort;
    }
}
