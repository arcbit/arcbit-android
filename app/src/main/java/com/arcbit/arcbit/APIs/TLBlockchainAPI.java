package com.arcbit.arcbit.APIs;

import com.arcbit.arcbit.model.TLBitcoinjWrapper;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.List;

public class TLBlockchainAPI {
    private String baseURL;
    private TLNetworking networking;

    public TLBlockchainAPI(String baseURL) {
        this.baseURL = baseURL;
        this.networking = new TLNetworking();
    }

    public Object getBlockHeight() throws Exception {
        return this.networking.getURLNotJSON(this.baseURL+"q/getblockcount");
    }

    public JSONObject getAddressData(String address) throws Exception {
        return this.networking.getURL(this.baseURL+"address/"+address);
    }

    public JSONObject getTx(String txHash) throws Exception {
        return this.networking.getURL(this.baseURL + "tx/" + txHash+"?format=json");
    }

    public JSONObject pushTx(String txHex, String txHash) throws Exception {
        JSONObject obj = this.networking.postURLNotJSON(this.baseURL + "pushtx", "tx=" + txHex);
        if (obj.has(TLNetworking.HTTP_ERROR_CODE)) {
            return obj;
        } else {
            JSONObject txidObj = new JSONObject();
            txidObj.put("txid", TLBitcoinjWrapper.reverseHexString(txHash));
            return txidObj;
        }
    }

    public JSONObject getUnspentOutputs(List<String> addressArray) throws Exception {
        String params = "?active="+StringUtils.join(addressArray, "|");
        return this.networking.getURL(this.baseURL+"unspent"+params);
    }

    public JSONObject getAddressesInfo(List<String> addressArray) throws Exception {
        String params = "?no_button=true&active="+StringUtils.join(addressArray, "|");
        return this.networking.getURL(this.baseURL+"multiaddr"+params);
    }
}
