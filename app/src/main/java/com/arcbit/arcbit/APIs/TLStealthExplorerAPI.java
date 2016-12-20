package com.arcbit.arcbit.APIs;

import com.arcbit.arcbit.model.TLAppDelegate;

import org.json.JSONObject;

import java.net.URLEncoder;

public class TLStealthExplorerAPI {
    public static long STEALTH_PAYMENTS_FETCH_COUNT = 50;

    public static long UNEXPECTED_ERROR = -1000;
    public static long DATABASE_ERROR = -1001;
    public static long INVALID_STEALTH_ADDRESS_ERROR = -1002;
    public static long INVALID_SIGNATURE_ERROR = -1003;
    public static long INVALID_SCAN_KEY_ERROR = -1004;
    public static long TX_DECODE_FAILED_ERROR = -1005;
    public static long INVALID_PARAMETER_ERROR = -1006;
    public static long SEND_TX_ERROR = -1007;

    public static String SERVER_ERROR_CODE = "error_code";
    public static String SERVER_ERROR_MSG = "error_msg";

    TLAppDelegate appDelegate;
    private String baseURL;
    private TLNetworking networking;
    
    public TLStealthExplorerAPI(TLAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
        this.appDelegate.preferences.resetStealthExplorerAPIURL();
        this.appDelegate.preferences.resetStealthServerPort();
        String baseURL = this.appDelegate.stealthServerConfig.getWebServerProtocol()+"://"+
                this.appDelegate.preferences.getStealthExplorerURL()+":"+this.appDelegate.preferences.getStealthServerPort();
        this.baseURL = baseURL;
        this.networking = new TLNetworking();
    }

    public JSONObject ping() throws Exception {
        return this.networking.getURL(this.baseURL+"/ping");
    }

    public JSONObject getChallengeSynchronous() throws Exception {
        return this.networking.getURL(this.baseURL+"/challenge");
    }

    public JSONObject getStealthPaymentsSynchronous(String stealthAddress, String signature, int offset) throws Exception {
        return this.networking.getURL(this.baseURL+"/payments?"+"addr="+stealthAddress+"&sig="+URLEncoder.encode(signature, "UTF-8")+"&offset="+offset);
    }

    public JSONObject watchStealthAddressSynchronous(String stealthAddress, String scanPriv, String signature) throws Exception {
        return this.networking.getURL(this.baseURL+"/watch?"+"addr="+stealthAddress+"&sig="+URLEncoder.encode(signature, "UTF-8")+"&scan_key="+scanPriv);
    }

    public JSONObject lookupTx(String stealthAddress, String txid) throws Exception {
        return this.networking.getURL(this.baseURL+"/lookuptx?"+"addr="+stealthAddress+"&txid="+txid);
    }
}
