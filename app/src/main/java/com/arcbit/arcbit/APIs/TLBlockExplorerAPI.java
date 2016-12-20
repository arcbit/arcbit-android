package com.arcbit.arcbit.APIs;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

import com.arcbit.arcbit.model.TLAppDelegate;

public class TLBlockExplorerAPI {

    public enum TLBlockExplorer {
        Blockchain, Insight;

        public static TLBlockExplorer toMyEnum (String myEnumString) {
            try {
                return valueOf(myEnumString);
            } catch (Exception ex) {
                return Blockchain;
            }
        }

        public static String getBlockExplorerAPIString(TLBlockExplorer blockExplorerAPI) {
            if (blockExplorerAPI == Blockchain) {
                return "blockchain.info";
            }
            if (blockExplorerAPI == Insight) {
                return "Bitpay's Insight";
            }
            return "blockchain.info";
        }
    }

    public TLBlockExplorer blockExplorerAPI = TLBlockExplorer.Blockchain;
    private String BLOCKEXPLORER_BASE_URL = "https://blockchain.info/";

    private TLBlockchainAPI blockchainAPI = null;
    public TLInsightAPI insightAPI = null;

    public TLBlockExplorerAPI(TLAppDelegate appDelegate) {
        String blockExplorerURL = appDelegate.preferences.getBlockExplorerURL(appDelegate.preferences.getBlockExplorerAPI());
        if (blockExplorerURL == null) {
            appDelegate.preferences.resetBlockExplorerAPIURL();
            blockExplorerURL = appDelegate.preferences.getBlockExplorerURL(appDelegate.preferences.getBlockExplorerAPI());
        }
        BLOCKEXPLORER_BASE_URL = blockExplorerURL;
        blockExplorerAPI = appDelegate.preferences.getBlockExplorerAPI();

        if (blockExplorerAPI == TLBlockExplorer.Blockchain) {
            this.blockchainAPI = new TLBlockchainAPI(BLOCKEXPLORER_BASE_URL);
            //needed for push tx api for stealth addresses
            this.insightAPI = new TLInsightAPI("https://insight.bitpay.com/");
        } else if (blockExplorerAPI == TLBlockExplorer.Insight) {
            this.insightAPI = new TLInsightAPI(BLOCKEXPLORER_BASE_URL);
        }
    }

    public JSONObject getBlockHeight() throws Exception {
        if (blockExplorerAPI == TLBlockExplorer.Insight) {
            return null;
        } else {
            Object obj = this.blockchainAPI.getBlockHeight();
            if (obj instanceof String) {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("height", obj);
                    return ret;
                } catch (JSONException e) {
                    return null;
                }
            } else if (obj instanceof JSONObject) {
                return (JSONObject) obj;
            }
        }
        return null;
    }

    public JSONObject getAddressesInfo(List<String> addressArray) throws Exception {
        if (blockExplorerAPI == TLBlockExplorer.Insight) {
            return this.insightAPI.getAddressesInfo(addressArray);
        } else {
            return this.blockchainAPI.getAddressesInfo(addressArray);
        }
    }

    public JSONObject getUnspentOutputs(List<String> addressArray) throws Exception {
        if (blockExplorerAPI == TLBlockExplorer.Insight) {
            return this.insightAPI.getUnspentOutputs(addressArray);
        } else {
            return this.blockchainAPI.getUnspentOutputs(addressArray);
        }
    }

    public JSONObject getAddressData(String address) throws Exception {
        if (blockExplorerAPI == TLBlockExplorer.Insight) {
            return this.insightAPI.getAddressData(address);
        } else {
            return this.blockchainAPI.getAddressData(address);
        }
    }

    public JSONObject getTx(String txHash) throws Exception {
        if (blockExplorerAPI == TLBlockExplorer.Insight) {
            return this.insightAPI.getTx(txHash);
        } else {
            return this.blockchainAPI.getTx(txHash);
        }
    }

    public JSONObject pushTx(String txHex, String txHash) throws Exception {
        if (blockExplorerAPI == TLBlockExplorer.Insight) {
            return this.insightAPI.pushTx(txHex, txHash);
        } else {
            return this.blockchainAPI.pushTx(txHex, txHash);
        }
    }

    public String getURLForWebViewAddress(String address) {
        return BLOCKEXPLORER_BASE_URL+"address/" + address;
    }

    public String getURLForWebViewTx(String tx) {
        return BLOCKEXPLORER_BASE_URL+"tx/" + tx;
    }
}
