package com.arcbit.arcbit.model;

import android.util.Log;

import com.arcbit.arcbit.utils.TLUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class TLTxObject {
    TLAppDelegate appDelegate;
    private JSONObject txDict;
    private JSONArray inputAddressToValueArray;
    private JSONArray outputAddressToValueArray;
    private ArrayList<String> addresses;
    private String txid;

    public TLTxObject(TLAppDelegate appDelegate, JSONObject dict) {
        this.appDelegate = appDelegate;
        txDict = dict;
        buildTxObject(txDict);
    }

    private void buildTxObject(JSONObject tx) {

        this.inputAddressToValueArray = new JSONArray();
        try {
            JSONArray inputsArray = tx.getJSONArray("inputs");

            for (int j = 0; j < inputsArray.length(); j++) {
                JSONObject input = inputsArray.getJSONObject(j);
                try {
                    JSONObject prevOut = input.getJSONObject("prev_out");
                    JSONObject inp = new JSONObject();
                    try {
                        String addr = prevOut.getString("addr");
                        inp.put("addr", addr);
                        inp.put("value", prevOut.getLong("value"));
                    } catch (JSONException e) {
                        Log.d("TLTxObject", "buildTxObject " + e.getLocalizedMessage());
                    }
                    this.inputAddressToValueArray.put(inp);
                } catch (JSONException e) {
                    Log.d("TLTxObject", "buildTxObject " + e.getLocalizedMessage());
                }
            }
        } catch (JSONException e) {
            Log.d("TLTxObject", "buildTxObject " + e.getLocalizedMessage());
        }

        this.outputAddressToValueArray = new JSONArray();
        try {
            JSONArray outsArray = tx.getJSONArray("out");

            for (int j = 0; j < outsArray.length(); j++) {
                JSONObject output = outsArray.getJSONObject(j);
                try {
                    JSONObject outt = new JSONObject();
                    outt.put("script", output.getString("script"));
                    try {
                        String addr = output.getString("addr");
                        outt.put("addr", addr);
                        outt.put("value", output.getLong("value"));
                    } catch (JSONException e) {
                        Log.d("TLTxObject", "buildTxObject " + e.getLocalizedMessage());
                    }
                    outputAddressToValueArray.put(outt);
                } catch (JSONException e) {
                    Log.d("TLTxObject", "buildTxObject " + e.getLocalizedMessage());
                }
            }
        } catch (JSONException e) {
            Log.d("TLTxObject", "buildTxObject " + e.getLocalizedMessage());
        }
    }

    public List<String> getAddresses() {
        if (addresses != null)
        {
            return addresses;
        }

        addresses = new ArrayList<String>();

        for (int i = 0; i < inputAddressToValueArray.length(); i++) {
            try {
                JSONObject addressTovalueDict = inputAddressToValueArray.getJSONObject(i);
                String address =  addressTovalueDict.getString("addr");
                addresses.add(address);
            } catch (JSONException e) {
            }
        }
        for (int i = 0; i < outputAddressToValueArray.length(); i++) {
            try {
                JSONObject addressTovalueDict = outputAddressToValueArray.getJSONObject(i);
                String address =  addressTovalueDict.getString("addr");
                addresses.add(address);
            } catch (JSONException e) {
            }
        }
        return addresses;
    }

    public JSONArray getInputAddressToValueArray() {
        return inputAddressToValueArray;
    }

    public List<String> getInputAddressArray() {
        ArrayList<String> addresses = new ArrayList<String>();
        for (int i = 0; i < inputAddressToValueArray.length(); i++) {
            try {
                JSONObject addressTovalueDict = inputAddressToValueArray.getJSONObject(i);
                String address =  addressTovalueDict.getString("addr");
                addresses.add(address);
            } catch (JSONException e) {
            }
        }
        return addresses;
    }

    public List<String> getOutputAddressArray() {
        ArrayList<String> addresses = new ArrayList<String>();
        for (int i = 0; i < outputAddressToValueArray.length(); i++) {
            try {
                JSONObject addressTovalueDict = outputAddressToValueArray.getJSONObject(i);
                String address =  addressTovalueDict.getString("addr");
                addresses.add(address);
            } catch (JSONException e) {
            }
        }
        return addresses;
    }

    public List<String> getPossibleStealthDataScripts() {
        ArrayList<String> possibleStealthDataScripts = new ArrayList<String>();
        for (int i = 0; i < outputAddressToValueArray.length(); i++) {
            try {
                JSONObject addressTovalueDict = outputAddressToValueArray.getJSONObject(i);
                String script =  addressTovalueDict.getString("script");
                if (script.length() == 80) {
                    possibleStealthDataScripts.add(script);
                }
            } catch (JSONException e) {
            }
        }
        return possibleStealthDataScripts;
    }

    public JSONArray getOutputAddressToValueArray() {
        return outputAddressToValueArray;
    }

    public String getHash() {
        try {
            return txDict.getString("hash");
        } catch (JSONException e) {
            return null;
        }
    }

    public String getTxid() {
        if (txid == null) {
            try {
                txid = TLBitcoinjWrapper.reverseHexString(txDict.getString("hash"));
            } catch (JSONException e) {
            }
        }
        return txid;
    }

    public long getTxUnixTime() {
        try {
            return txDict.getLong("time");
        } catch (JSONException e) {
            return 0;
        }
    }

    public String getTime() {
        long interval = getTxUnixTime();
        //FIXME: specific to insight api, later dont use confirmations but block_height for all apis
        try {
            txDict.getLong("confirmations");
        } catch (JSONException e) {
            if (interval <= 0) {
                return "";
            }
        }
        return TLUtils.getFormattedDate(interval*1000);
    }

    public long getConfirmations() {
        //FIXME: specific to insight api, later dont use confirmations but block_height for all apis
        try {
            long conf = txDict.getLong("confirmations");
            return conf;
        } catch (JSONException e) {
            try {
                long blockHeight = txDict.getLong("block_height");
                if (blockHeight > 0) {
                    return this.appDelegate.blockchainStatus.blockHeight - blockHeight + 1;
                }
            } catch (JSONException ee) {
            }
        }
        return 0;
    }
}
