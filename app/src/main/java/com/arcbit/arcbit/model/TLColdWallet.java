package com.arcbit.arcbit.model;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TLColdWallet {
    public static class TLColdWalletException extends Exception {
        public TLColdWalletException(String message) {
            super(message);
        }
    }

    public static class TLInvalidScannedData extends TLColdWalletException {
        public TLInvalidScannedData(String message) {
            super(message);
        }
    }

    public static class TLColdWalletInvalidKey extends TLColdWalletException {
        public TLColdWalletInvalidKey(String message) {
            super(message);
        }
    }

    public static class TLColdWalletMisMatchExtendedPublicKey extends TLColdWalletException {
        public TLColdWalletMisMatchExtendedPublicKey(String message) {
            super(message);
        }
    }

    static String AIR_GAP_DATA_VERSION = "1";

    private static String createUnsignedTxAipGapData(String unSignedTx, String extendedPublicKey,
                                                     JSONArray inputScripts, JSONArray txInputsAccountHDIdxes,
                                                     boolean isJUnitTest) {
        JSONObject dataDictionaryToAirGapPass = new JSONObject();
        try {
            dataDictionaryToAirGapPass.put("v", AIR_GAP_DATA_VERSION);
            dataDictionaryToAirGapPass.put("account_public_key", extendedPublicKey);
            byte[] bytes = TLWalletUtils.hexStringToData(unSignedTx);
            String base64Encoded;
            if (isJUnitTest) {
                base64Encoded = org.spongycastle.util.encoders.Base64.toBase64String(bytes);
            } else {
                base64Encoded = Base64.encodeToString(bytes, Base64.DEFAULT);
            }
            dataDictionaryToAirGapPass.put("unsigned_tx_base64", base64Encoded);
            dataDictionaryToAirGapPass.put("input_scripts", inputScripts); //inputScripts in hex
            dataDictionaryToAirGapPass.put("tx_inputs_account_hd_idxes", txInputsAccountHDIdxes); //[["idx":123, "is_change":false], ["idx":124, "is_change":true]]
            String jsonString = dataDictionaryToAirGapPass.toString();
            return jsonString;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String createSerializedUnsignedTxAipGapData(String unSignedTx,
                                                              String extendedPublicKey,
                                                              JSONArray inputScripts,
                                                              JSONArray txInputsAccountHDIdxes,
                                                              boolean isJUnitTest) {
        String aipGapDataJSONString = TLColdWallet.createUnsignedTxAipGapData(unSignedTx, extendedPublicKey,
                inputScripts, txInputsAccountHDIdxes, isJUnitTest);
        byte[] data = aipGapDataJSONString.getBytes();
        if (isJUnitTest) {
            return org.spongycastle.util.encoders.Base64.toBase64String(data);
        } else {
            return Base64.encodeToString(data, Base64.DEFAULT);
        }
    }

    public static String createSerializedUnsignedTxAipGapData(String unSignedTx, String extendedPublicKey, JSONArray inputScripts, JSONArray txInputsAccountHDIdxes) {
        return createSerializedUnsignedTxAipGapData(unSignedTx, extendedPublicKey, inputScripts, txInputsAccountHDIdxes, false);
    }

    public static List<String> splitStringToArray(String str) {
        List<String> partsArray = new ArrayList<>();
        int idx = 0;
        int SPLIT_SUB_STRING_LENGTH = 100;
        int partCount = 0;
        while (true) {
            String subString;
            partCount += 1;
            if (idx+SPLIT_SUB_STRING_LENGTH >= str.length()) {
                subString = str.substring(idx, str.length());
                partsArray.add(subString+":"+partCount);

                break;
            } else {
                subString = str.substring(idx, idx+SPLIT_SUB_STRING_LENGTH);
                partsArray.add(subString+":"+partCount);
            }
            idx += SPLIT_SUB_STRING_LENGTH;
        }
        for (int i = 0; i < partsArray.size(); i++) {
            partsArray.set(i, partsArray.get(i)+"."+partCount);
        }
        return partsArray;
    }

    public static List<Object> parseScannedPart(String str) {
        List<String> parts = Arrays.asList(str.split(":"));
        String data = parts.get(0);
        String partCountAndTotal = parts.get(1);
        List<String> partCountAndTotalArray = Arrays.asList(partCountAndTotal.split("\\."));
        String partCount = partCountAndTotalArray.get(0);
        String totalParts = partCountAndTotalArray.get(1);
        return Arrays.asList(data, Integer.parseInt(partCount), Integer.parseInt(totalParts));
    }

    public static String createSerializedSignedTxAipGapData(String aipGapDataBase64,
                                                            String mnemonicOrExtendedPrivateKey,
                                                            boolean isTestnet, boolean isJUnitTest) throws TLColdWalletException {
        String signedTxHexAndTxHash = TLColdWallet.createSignedTxAipGapData(aipGapDataBase64,
                mnemonicOrExtendedPrivateKey, isTestnet, isJUnitTest);
        byte[] data = signedTxHexAndTxHash.getBytes();
        if (isJUnitTest) {
            return org.spongycastle.util.encoders.Base64.toBase64String(data);
        } else {
            return Base64.encodeToString(data, Base64.DEFAULT);
        }
    }

    public static String createSerializedSignedTxAipGapData(String aipGapDataBase64,
                                                            String mnemonicOrExtendedPrivateKey,
                                                            boolean isTestnet) throws TLColdWalletException {
        return createSerializedSignedTxAipGapData(aipGapDataBase64, mnemonicOrExtendedPrivateKey, isTestnet, false);
    }

    private static String createSignedTxAipGapData(String aipGapDataBase64, String mnemonicOrExtendedPrivateKey,
                                                   boolean isTestnet, boolean isJUnitTest) throws TLColdWalletException {
        byte[] data;
        if (isJUnitTest) {
            data = org.spongycastle.util.encoders.Base64.decode(aipGapDataBase64);
        } else {
            data = Base64.decode(aipGapDataBase64, Base64.DEFAULT);
        }
        try {
            String text = new String(data, "UTF-8");
            JSONObject result = new JSONObject(text);
            String extendedPublicKey = result.getString("account_public_key");
            JSONArray txInputsAccountHDIdxes = result.getJSONArray("tx_inputs_account_hd_idxes");

            Integer accountIdx = TLHDWalletWrapper.getAccountIdxForExtendedKey(extendedPublicKey, isTestnet);

            String mnemonicExtendedPrivateKey;
            if (TLHDWalletWrapper.phraseIsValid(mnemonicOrExtendedPrivateKey)) {
                String masterHex = TLHDWalletWrapper.getMasterHex(mnemonicOrExtendedPrivateKey);
                mnemonicExtendedPrivateKey = TLHDWalletWrapper.getExtendPrivKey(masterHex, accountIdx, isTestnet);
            } else if (TLHDWalletWrapper.isValidExtendedPrivateKey(mnemonicOrExtendedPrivateKey, isTestnet)) {
                mnemonicExtendedPrivateKey = mnemonicOrExtendedPrivateKey;
            } else {
                throw new TLColdWalletInvalidKey("");
            }
            String mnemonicExtendedPublicKey = TLHDWalletWrapper.getExtendPubKey(mnemonicExtendedPrivateKey, isTestnet);
            if (!extendedPublicKey.equals(mnemonicExtendedPublicKey)) {
                throw new TLColdWalletMisMatchExtendedPublicKey("");
            }

            List<String> privateKeysArray = new ArrayList<>(txInputsAccountHDIdxes.length());
            for (int i = 0; i < txInputsAccountHDIdxes.length(); i++) {
                JSONObject txInputsAccountHDIdx = txInputsAccountHDIdxes.getJSONObject(i);
                int HDIndexNumber = txInputsAccountHDIdx.getInt("idx");
                boolean isChange = txInputsAccountHDIdx.getBoolean("is_change");
                ArrayList<Integer> addressSequence = new ArrayList<Integer>(Arrays.asList(isChange ? (Integer)TLWalletJSONKeys.TLAddressType.Change.getValue() : (Integer)TLWalletJSONKeys.TLAddressType.Main.getValue(), HDIndexNumber));
                String privateKey = TLHDWalletWrapper.getPrivateKey(mnemonicExtendedPrivateKey, addressSequence, isTestnet);
                privateKeysArray.add(privateKey);
            }
            String base64UnsignedTx = result.getString("unsigned_tx_base64");
            byte[] txData;
            if (isJUnitTest) {
                txData = org.spongycastle.util.encoders.Base64.decode(base64UnsignedTx);
            } else {
                txData = Base64.decode(base64UnsignedTx, Base64.DEFAULT);
            }

            JSONArray inputHexScriptsArray = result.getJSONArray("input_scripts");
            List<String> inputScriptsArray = new ArrayList<>(inputHexScriptsArray.length());
            for (int i = 0; i < inputHexScriptsArray.length(); i++) {
                String hexScript = inputHexScriptsArray.getString(i);
                inputScriptsArray.add(hexScript);
            }

            JSONObject txHexAndTxHash = TLBitcoinjWrapper.createSignedSerializedTransactionHex(txData, inputScriptsArray, privateKeysArray, isTestnet);
            if (txHexAndTxHash != null) {
                return txHexAndTxHash.toString();
            }
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getSignedTxData(String aipGapDataBase64) {
        return getSignedTxData(aipGapDataBase64, false);
    }

    public static JSONObject getSignedTxData(String aipGapDataBase64, boolean isJUnitTest) {
        byte[] data;
        if (isJUnitTest) {
            data = org.spongycastle.util.encoders.Base64.decode(aipGapDataBase64);
        } else {
            data = Base64.decode(aipGapDataBase64, Base64.DEFAULT);
        }
        try {
            String text = new String(data, "UTF-8");
            return new JSONObject(text);
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
