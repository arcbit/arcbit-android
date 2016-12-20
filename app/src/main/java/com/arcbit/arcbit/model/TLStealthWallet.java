package com.arcbit.arcbit.model;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.support.v4.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.arcbit.arcbit.APIs.TLNetworking;
import com.arcbit.arcbit.APIs.TLStealthExplorerAPI;
import com.arcbit.arcbit.model.TLWalletJSONKeys.TLStealthPaymentStatus;


public class TLStealthWallet {
    private static final String TAG = TLStealthWallet.class.getName();
    public class GetAndStoreStealthPaymentsResults {
        boolean gotOldestPaymentAddresses;
        long latestTxTime;
        List<String> stealthPayments;
    }

    TLAppDelegate appDelegate;

    static public String challenge = "";
    static public boolean challengeNeedsRefreshing = true;

    static public int MAX_CONSECUTIVE_INVALID_SIGNATURES = 4;
    static public long PREVIOUS_TX_CONFIRMATIONS_TO_COUNT_AS_SPENT = 12;
    static public long TIME_TO_WAIT_TO_CHECK_FOR_SPENT_TX = 86400; // 1 day in seconds

    private JSONObject stealthWalletDict;
    private Map<String, String> unspentPaymentAddress2PaymentTxid = new HashMap<String, String>();
    private Map<String, String> paymentAddress2PrivateKeyDict = new HashMap<String, String>();
    private Map<String, String> paymentTxid2PaymentAddressDict = new HashMap<String, String>();
    private String scanPublicKey = null;
    private String spendPublicKey = null;
    private TLAccountObject accountObject;
    public boolean hasUpdateStealthPaymentStatuses = false;
    boolean isListeningToStealthPayment = false;

    TLStealthWallet(TLAppDelegate appDelegate, JSONObject stealthDict, TLAccountObject accountObject, boolean updateStealthPaymentStatuses) {
        this.appDelegate = appDelegate;
        this.stealthWalletDict = stealthDict;
        this.accountObject = accountObject;
        this.setUpStealthPaymentAddresses(updateStealthPaymentStatuses, true);
    }

    public String getStealthAddress() {
        try {
            return this.stealthWalletDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESS);
        } catch (JSONException e) {
            return null;
        }
    }

    public String getStealthAddressScanKey() {
        try {
            return this.stealthWalletDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESS_SCAN_KEY);
        } catch (JSONException e) {
            return null;
        }
    }

    public String getStealthAddressSpendKey() {
        try {
            return this.stealthWalletDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESS_SPEND_KEY);
        } catch (JSONException e) {
            return null;
        }
    }

    public long getStealthAddressLastTxTime() {
        try {
            return this.stealthWalletDict.getLong(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LAST_TX_TIME);
        } catch (JSONException e) {
            return 0;
        }
    }

    JSONObject getStealthAddressServers() {
        try {
            return this.stealthWalletDict.getJSONObject(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_SERVERS);
        } catch (JSONException e) {
            return null;
        }
    }

    public boolean paymentTxidExist(String txid) {
        return this.paymentTxid2PaymentAddressDict.get(txid) != null;
    }

    public boolean isPaymentAddress(String address) {
        return this.paymentAddress2PrivateKeyDict.get(address) != null;
    }

    public String getPaymentAddressPrivateKey(String address) {
        return this.paymentAddress2PrivateKeyDict.get(address);
    }

    public void setPaymentAddressPrivateKey(String address, String privateKey) {
        this.paymentAddress2PrivateKeyDict.put(address, privateKey);
    }

    public JSONArray getStealthAddressPayments() {
        try {
            return this.stealthWalletDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS);
        } catch (JSONException e) {
            return null;
        }
    }

    public String getPaymentAddressForIndex(int index) {
        try {
            JSONObject paymentDict = this.stealthWalletDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS).getJSONObject(index);
            return paymentDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
        } catch (JSONException e) {
            return null;
        }
    }

    public int getStealthAddressPaymentsCount() {
        try {
            return this.stealthWalletDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS).length();
        } catch (JSONException e) {
            return 0;
        }
    }

    List<String> getPaymentAddresses() {
        return new ArrayList<String>(this.paymentAddress2PrivateKeyDict.keySet());
    }

    List<String> getUnspentPaymentAddresses() {
        return new ArrayList<String>(this.unspentPaymentAddress2PaymentTxid.keySet());
    }

    String getStealthAddressSpendPublicKey() {
        if (spendPublicKey == null) {
            Pair<String, String> publicKeys = TLStealthAddress.getScanPublicKeyAndSpendPublicKey(this.getStealthAddress(),
                    this.accountObject.appWallet.walletConfig.isTestnet);
            scanPublicKey = publicKeys.first;
            spendPublicKey = publicKeys.second;
        }
        return spendPublicKey;
    }

    String getStealthAddressScanPublicKey() {
        if (scanPublicKey == null) {
            Pair<String, String> publicKeys = TLStealthAddress.getScanPublicKeyAndSpendPublicKey(this.getStealthAddress(),
                    this.accountObject.appWallet.walletConfig.isTestnet);
            scanPublicKey = publicKeys.first;
            spendPublicKey = publicKeys.second;
        }
        return scanPublicKey;
    }

    void setUpStealthPaymentAddresses(boolean updateStealthPaymentStatuses, boolean isSetup) {
        this.setUpStealthPaymentAddresses(updateStealthPaymentStatuses, isSetup, true);
    }

    void setUpStealthPaymentAddresses(boolean updateStealthPaymentStatuses, boolean isSetup, boolean async) {
        if (isSetup) {
            this.accountObject.removeOldStealthPayments();
        }
        JSONArray paymentsArray = this.getStealthAddressPayments();

        if (isSetup) {
            this.unspentPaymentAddress2PaymentTxid = new HashMap<String, String>();
            this.paymentAddress2PrivateKeyDict = new HashMap<String, String>();
            this.paymentTxid2PaymentAddressDict = new HashMap<String, String>();
        }

        List<String> possiblyClaimedTxidArray = new ArrayList<String>();
        List<String> possiblyClaimedAddressArray = new ArrayList<String>();
        List<Long> possiblyClaimedTxTimeArray = new ArrayList<Long>();

        long nowTime = new Date().getTime()/1000;

        for (int i = 0; i < paymentsArray.length(); i++) {
            try {
                JSONObject paymentDict = paymentsArray.getJSONObject(i);

                String address = paymentDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
                String txid = paymentDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_TXID);
                String privateKey = paymentDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_KEY);
                if (isSetup) {
                    this.paymentTxid2PaymentAddressDict.put(txid, address);
                    this.paymentAddress2PrivateKeyDict.put(address, privateKey);
                }

                int stealthPaymentStatus = paymentDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS);

                if (isSetup) {
                    if (stealthPaymentStatus == TLStealthPaymentStatus.Unspent.getValue()) {
                        this.unspentPaymentAddress2PaymentTxid.put(address, txid);
                    }
                }

                // dont check to remove last STEALTH_PAYMENTS_FETCH_COUNT payment addresses
                if (i >= paymentsArray.length() - TLStealthExplorerAPI.STEALTH_PAYMENTS_FETCH_COUNT) {
                    continue;
                }

                if (stealthPaymentStatus == TLStealthPaymentStatus.Claimed.getValue() || stealthPaymentStatus == TLStealthPaymentStatus.Unspent.getValue()) {

                    long lastCheckTime = paymentDict.getLong(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHECK_TIME);

                    if ((nowTime - lastCheckTime) > TIME_TO_WAIT_TO_CHECK_FOR_SPENT_TX) {
                        possiblyClaimedTxidArray.add(txid);
                        possiblyClaimedAddressArray.add(address);
                        possiblyClaimedTxTimeArray.add(lastCheckTime);
                    }
                }
            } catch (JSONException e) {
            }
        }
        if (updateStealthPaymentStatuses) {
            hasUpdateStealthPaymentStatuses = true;
            if (async) {
                this.addOrSetStealthPaymentsWithStatus(possiblyClaimedTxidArray, possiblyClaimedAddressArray,
                        possiblyClaimedTxTimeArray, false, false);
            } else {
                this.addOrSetStealthPaymentsWithStatus(possiblyClaimedTxidArray, possiblyClaimedAddressArray,
                        possiblyClaimedTxTimeArray, false, true);
            }
        }
    }

    public void updateStealthPaymentStatusesAsync() {
        this.setUpStealthPaymentAddresses(true, false);
    }

    String getPrivateKeyForAddress(String expectedAddress, String script) {
        String scanKey = this.getStealthAddressScanKey();
        String spendKey = this.getStealthAddressSpendKey();
        String secret = TLStealthAddress.getPaymentAddressPrivateKeySecretFromScript(script, scanKey, spendKey);
        if (secret != null) {
            String outputAddress = TLBitcoinjWrapper.getAddressFromSecret(secret, this.accountObject.appWallet.walletConfig.isTestnet);
            if (outputAddress.equals(expectedAddress)) {
                return TLBitcoinjWrapper.privateKeyFromSecret(secret, this.accountObject.appWallet.walletConfig.isTestnet);
            }
        }
        return null;
    }

    boolean isCurrentServerWatching() {
        String currentServerURL = this.appDelegate.preferences.getStealthExplorerURL();
        JSONObject stealthAddressServersDict = this.getStealthAddressServers();
        try {
            JSONObject stealthServerDict = stealthAddressServersDict.getJSONObject(currentServerURL);
            return stealthServerDict.getBoolean(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_WATCHING);
        } catch (JSONException e) {
            this.accountObject.setStealthAddressServerStatus(currentServerURL, false);

            JSONObject serverAttributesDict = new JSONObject();
            try {
                serverAttributesDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_WATCHING, false);
                stealthAddressServersDict.put(currentServerURL, serverAttributesDict);
            } catch (JSONException e1) {
            }
            return false;
        }
    }

    boolean checkIfHaveStealthPayments() {
        String stealthAddress = this.getStealthAddress();
        String scanKey = this.getStealthAddressScanKey();
        String spendKey = this.getStealthAddressSpendKey();
        String scanPublicKey = this.getStealthAddressScanPublicKey();
        boolean success = this.watchStealthAddress(stealthAddress, scanKey, spendKey, scanPublicKey);
        if (success) {
            JSONObject gotOldestPaymentAddressesAndPayments = this.getStealthPayments(stealthAddress,
                    scanKey, spendKey, scanPublicKey, 0);
            try {
                JSONArray stealhPayments = gotOldestPaymentAddressesAndPayments.getJSONArray("stealthPayments");
                if (gotOldestPaymentAddressesAndPayments != null &&
                        stealhPayments != null && stealhPayments.length() > 0) {
                    return true;
                }
            } catch (JSONException e) {
            }
        }

        return false;
    }

    void checkToWatchStealthAddress() {
        String stealthAddress = this.getStealthAddress();
        if (this.isCurrentServerWatching() != true) {
            String scanKey = this.getStealthAddressScanKey();
            String spendKey = this.getStealthAddressSpendKey();
            String scanPublicKey = this.getStealthAddressScanPublicKey();
            boolean success = this.watchStealthAddress(stealthAddress, scanKey, spendKey, scanPublicKey);
            if (success) {
                JSONObject stealthAddressServersDict = this.getStealthAddressServers();
                String currentServerURL = this.appDelegate.preferences.getStealthExplorerURL();
                try {
                    stealthAddressServersDict.getJSONObject(currentServerURL).put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_WATCHING, true);
                    this.accountObject.setStealthAddressServerStatus(currentServerURL, true);
                } catch (JSONException e) {
                }
            }
        }
    }

    Pair<String, String> getStealthAddressAndSignatureFromChallenge(String challenge) {
        String privKey = this.getStealthAddressScanKey();
        String signature = TLBitcoinjWrapper.getSignature(privKey, challenge, this.accountObject.appWallet.walletConfig.isTestnet);
        String stealthAddress = this.getStealthAddress();
        return new Pair<String, String>(stealthAddress, signature);
    }

   String getChallengeAndSign(String stealthAddress, String privKey, String pubKey) {
       try {
           if (TLStealthWallet.challengeNeedsRefreshing == true) {
               JSONObject jsonData = this.appDelegate.stealthExplorerAPI.getChallengeSynchronous();
               if (jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                   return null;
               }
               try {
                   TLStealthWallet.challenge = jsonData.getString("challenge");
                   TLStealthWallet.challengeNeedsRefreshing = false;
               } catch (JSONException e) {
               }
           }

           String challenge = TLStealthWallet.challenge;

           return TLBitcoinjWrapper.getSignature(privKey, challenge, this.accountObject.appWallet.walletConfig.isTestnet);
       } catch (Exception e) {
           return null;
       }
    }

    private void addOrSetStealthPaymentsWithStatusTx(boolean txidHasUnspentOutputs,
                                                     boolean waitForCompletion, boolean isAddingPayments,
                                                     long nowTime, String txid, String paymentAddress,
                                                     long txTime) throws Exception {
        if (!txidHasUnspentOutputs) {
            // means blockexplorer has not seen tx yet OR stealth payment already been spent

            // cant figure out whether stealth payments has been spent by getting unspent outputs because
            // blockexplorer api might receive tx yet, if we are pushing tx from a source that is not the blockexplorer api
            JSONObject jsonData = appDelegate.blockExplorerAPI.getTx(txid);
            if (jsonData == null) {
                return;
            }
            Pair<String, List<String>> stealthDataScriptAndOutputAddresses = getStealthDataScriptAndOutputAddresses(jsonData);

            if (stealthDataScriptAndOutputAddresses == null || stealthDataScriptAndOutputAddresses.first == null) {
                return;
            }
            if (stealthDataScriptAndOutputAddresses.second.indexOf(paymentAddress) != -1) {
                TLTxObject txObject = new TLTxObject(appDelegate, jsonData);

                //Note: this confirmation count is not the confirmations for the tx that spent the stealth payment
                long confirmations = txObject.getConfirmations();

                if (confirmations >= PREVIOUS_TX_CONFIRMATIONS_TO_COUNT_AS_SPENT) {
                    if (isAddingPayments) {
                        String privateKey = generateAndAddStealthAddressPaymentKey(stealthDataScriptAndOutputAddresses.first, paymentAddress,
                                txid, txTime, TLStealthPaymentStatus.Spent);
                        if (privateKey != null) {
                            setPaymentAddressPrivateKey(paymentAddress, privateKey);
                        } else {
                            Log.d(TAG, "no privateKey for " + paymentAddress);
                        }
                    } else {
                        accountObject.setStealthPaymentStatus(txid, TLStealthPaymentStatus.Spent, nowTime);
                    }
                } else {
                    if (isAddingPayments) {
                        String privateKey = generateAndAddStealthAddressPaymentKey(stealthDataScriptAndOutputAddresses.first, paymentAddress,
                                txid, txTime, TLStealthPaymentStatus.Claimed);
                        if (privateKey != null) {
                            setPaymentAddressPrivateKey(paymentAddress, privateKey);
                        } else {
                            Log.d(TAG, "no privateKey for " + paymentAddress);
                        }
                    } else {
                        accountObject.setStealthPaymentStatus(txid, TLStealthPaymentStatus.Claimed, nowTime);
                    }
                }

            }
        } else {
            JSONObject jsonData = appDelegate.blockExplorerAPI.getTx(txid);
            if (jsonData == null) {
                return;
            }
            Pair<String, List<String>> stealthDataScriptAndOutputAddresses = getStealthDataScriptAndOutputAddresses(jsonData);

            if (stealthDataScriptAndOutputAddresses != null) {
                if (stealthDataScriptAndOutputAddresses.first == null) {
                    return;
                }
                if (stealthDataScriptAndOutputAddresses.second.indexOf(paymentAddress) != -1) {
                    if (isAddingPayments) {
                        String privateKey = generateAndAddStealthAddressPaymentKey(stealthDataScriptAndOutputAddresses.first, paymentAddress,
                                txid, txTime, TLStealthPaymentStatus.Unspent);
                        if (privateKey != null) {
                            setPaymentAddressPrivateKey(paymentAddress, privateKey);
                        } else {
                            Log.d(TAG, "no privateKey for " + paymentAddress);
                        }
                    } else {
                        accountObject.setStealthPaymentStatus(txid, TLStealthPaymentStatus.Unspent, nowTime);
                    }
                }
            }
        }
    }

    void addOrSetStealthPaymentsWithStatus(List<String> txidArray, List<String> addressArray,
                                           List<Long> txTimeArray, boolean isAddingPayments, boolean waitForCompletion) {

        if (!waitForCompletion) {
            TLStealthWallet self = this;
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Boolean success = (Boolean) msg.obj;
                }
            };

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Message message = Message.obtain();
                    try {
                        addOrSetStealthPaymentsWithStatus(txidArray, addressArray, txTimeArray, isAddingPayments, false, self);
                        message.obj = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        message.obj = false;
                    }
                    handler.sendMessage(Message.obtain(message));
                }
            }).start();
        } else {
            try {
                this.addOrSetStealthPaymentsWithStatus(txidArray, addressArray, txTimeArray, isAddingPayments, waitForCompletion, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void addOrSetStealthPaymentsWithStatus(List<String> txidArray, List<String> addressArray,
                                           List<Long> txTimeArray, boolean isAddingPayments, boolean waitForCompletion, TLStealthWallet self) throws Exception {
        JSONObject unspentOutputsJsonData = null;
        if (txidArray.size() > 0) {
            try {
                unspentOutputsJsonData = appDelegate.blockExplorerAPI.getUnspentOutputs(addressArray);
                if (unspentOutputsJsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                    String msg = unspentOutputsJsonData.getString(TLNetworking.HTTP_ERROR_MSG);
                    if (msg.equals("No free outputs to spend")) {
                        return;
                    } else {
                        unspentOutputsJsonData = new JSONObject();
                        unspentOutputsJsonData.put("unspent_outputs", new JSONArray());
                    }
                }
            } catch (JSONException e) {
                unspentOutputsJsonData = new JSONObject();
                unspentOutputsJsonData.put("unspent_outputs", new JSONArray());
            } catch (Exception e) {
                unspentOutputsJsonData = new JSONObject();
                unspentOutputsJsonData.put("unspent_outputs", new JSONArray());
            }
        }

        Map<String, Boolean> txid2hasUnspentOutputs = new HashMap<String, Boolean>();
        for (String txid : txidArray) {
            txid2hasUnspentOutputs.put(txid, false);
        }

        if (unspentOutputsJsonData != null) {
            try {
                JSONArray unspentOutputs = unspentOutputsJsonData.getJSONArray("unspent_outputs");
                for (int i = 0; i < unspentOutputs.length(); i++) {
                    JSONObject unspentOutput = unspentOutputs.getJSONObject(i);
                    String unspentOutputTxid = unspentOutput.getString("tx_hash_big_endian");
                    txid2hasUnspentOutputs.put(unspentOutputTxid, true);
                }
            } catch (JSONException e) {
            }
        }

        long nowTime = new Date().getTime()/1000;
        ExecutorService executorService = Executors.newCachedThreadPool();

        for (int i = 0; i < txidArray.size(); i++) {
            final String txid = txidArray.get(i);
            final String paymentAddress = addressArray.get(i);
            final long txTime = txTimeArray.get(i);
            final boolean txidHasUnspentOutputs = txid2hasUnspentOutputs.get(txid);
            final int idx = i;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        addOrSetStealthPaymentsWithStatusTx(txidHasUnspentOutputs, waitForCompletion, isAddingPayments, nowTime, txid, paymentAddress, txTime);
                    } catch (Exception e) {
                    }
                }
            });
        }
        executorService.shutdown();
        if (waitForCompletion) {
            boolean finished = executorService.awaitTermination(5, TimeUnit.MINUTES);
        }
    }

    GetAndStoreStealthPaymentsResults getAndStoreStealthPayments(int offset) {
        String stealthAddress = this.getStealthAddress();
        String scanKey = this.getStealthAddressScanKey();
        String spendKey = this.getStealthAddressSpendKey();
        String scanPublicKey = this.getStealthAddressScanPublicKey();

        JSONObject ret = this.getStealthPayments(stealthAddress, scanKey, spendKey, scanPublicKey, offset);

        if (ret == null) {
            return null;
        }

        try {
            boolean gotOldestPaymentAddresses = ret.getBoolean("gotOldestPaymentAddresses");
            long latestTxTime = ret.getLong("latestTxTime");
            JSONArray payments = ret.getJSONArray("stealthPayments");

            if (payments == null) {
                GetAndStoreStealthPaymentsResults returnObject = new GetAndStoreStealthPaymentsResults();
                returnObject.gotOldestPaymentAddresses = gotOldestPaymentAddresses;
                returnObject.latestTxTime = latestTxTime;
                returnObject.stealthPayments = new ArrayList<String>();
                return returnObject;
            }

            List<String> txidArray = new ArrayList<String>();
            List<String> addressArray = new ArrayList<String>();
            List<Long> txTimeArray = new ArrayList<Long>();

            for (int i = payments.length()-1; i >= 0; i--) {
                JSONObject payment = payments.getJSONObject(i);
                String txid = payment.getString("txid");
                if (this.paymentTxidExist(txid) == true) {
                    continue;
                }
                String addr = payment.getString("addr");
                txidArray.add(txid);
                addressArray.add(addr);
                txTimeArray.add(payment.getLong("time"));
            }
            if (txidArray.size() == 0) {
                GetAndStoreStealthPaymentsResults returnObject = new GetAndStoreStealthPaymentsResults();
                returnObject.gotOldestPaymentAddresses = gotOldestPaymentAddresses;
                returnObject.latestTxTime = latestTxTime;
                returnObject.stealthPayments = new ArrayList<String>();
                return returnObject;
            }

            // must check if txids exist and are stealth payments that belong to this account before storing it
            this.addOrSetStealthPaymentsWithStatus(txidArray, addressArray, txTimeArray, true, true);
            GetAndStoreStealthPaymentsResults returnObject = new GetAndStoreStealthPaymentsResults();
            returnObject.gotOldestPaymentAddresses = gotOldestPaymentAddresses;
            returnObject.latestTxTime = latestTxTime;
            returnObject.stealthPayments = addressArray;
            return returnObject;
        } catch (JSONException e) {
            return null;
        }
    }

    static public Pair<String, List<String>> getStealthDataScriptAndOutputAddresses(JSONObject jsonTxData) {
        try {
            JSONArray outsArray = jsonTxData.getJSONArray("out");
            if (outsArray != null) {
                List<String> outputAddresses = new ArrayList<String>();
                String stealthDataScript = null;

                for (int i = 0; i < outsArray.length(); i++) {
                    try {
                        JSONObject output = outsArray.getJSONObject(i);
                        try {
                            String addr = output.getString("addr");
                            outputAddresses.add(addr);
                        } catch (JSONException e) {
                            String script = output.getString("script");
                            if (script.length() == 80) {
                                stealthDataScript = script;
                            }
                        }
                    } catch (JSONException e) {
                    }
                }
                return new Pair<String, List<String>>(stealthDataScript, outputAddresses);
            }
        } catch (JSONException e) {
        }
        return null;
    }

    String generateAndAddStealthAddressPaymentKey(String stealthAddressDataScript, String expectedAddress,
                                                  String txid, long txTime, TLStealthPaymentStatus stealthPaymentStatus) {

        if (this.paymentTxidExist(txid) == true) {
            return null;
        }
        String privateKey = this.getPrivateKeyForAddress(expectedAddress, stealthAddressDataScript);
        if (privateKey != null) {
            this.unspentPaymentAddress2PaymentTxid.put(expectedAddress, txid);
            this.paymentTxid2PaymentAddressDict.put(txid,expectedAddress);
            this.setPaymentAddressPrivateKey(expectedAddress, privateKey);

            this.accountObject.addStealthAddressPaymentKey(privateKey, expectedAddress, txid, txTime, stealthPaymentStatus);
            return privateKey;
        } else {
            Log.d(TAG, "error key not found for address " + expectedAddress);
            return null;
        }
    }

   public boolean addStealthAddressPaymentKey(String privateKey, String paymentAddress, String txid, long txTime, TLStealthPaymentStatus stealthPaymentStatus) {
        this.unspentPaymentAddress2PaymentTxid.put(paymentAddress, txid);
        this.paymentTxid2PaymentAddressDict.put(txid,paymentAddress);
        this.setPaymentAddressPrivateKey(paymentAddress, privateKey);
        this.accountObject.addStealthAddressPaymentKey(privateKey, paymentAddress, txid, txTime, stealthPaymentStatus);
        return true;
    }

    JSONObject getStealthPayments(String stealthAddress, String scanPriv, String spendPriv,
                            String scanPublicKey, int offset) {
        String signature = this.getChallengeAndSign(stealthAddress, scanPriv, scanPublicKey);

        if (signature == null) {
            return null;
        }

        boolean gotOldestPaymentAddresses = false;

        JSONObject jsonData = null;
        int consecutiveInvalidSignatures = 0;

        try {
            while (true) {
                jsonData = this.appDelegate.stealthExplorerAPI.getStealthPaymentsSynchronous(stealthAddress, signature, offset);
                try {
                    jsonData.getInt(TLNetworking.HTTP_ERROR_CODE);
                    return null;
                } catch (JSONException e) {
                    try {
                        int errorCode = jsonData.getInt(TLStealthExplorerAPI.SERVER_ERROR_CODE);
                        if (errorCode == TLStealthExplorerAPI.INVALID_SIGNATURE_ERROR) {
                            consecutiveInvalidSignatures += 1;
                            if (consecutiveInvalidSignatures > MAX_CONSECUTIVE_INVALID_SIGNATURES) {
                                return null;
                            }
                            TLStealthWallet.challengeNeedsRefreshing = true;
                            signature = this.getChallengeAndSign(stealthAddress, scanPriv, scanPublicKey);
                            if (signature == null) {
                                return null;
                            }
                        }
                        continue;
                    } catch (JSONException e1) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        try {
            JSONArray stealthPayments = jsonData.getJSONArray("payments");

            if (stealthPayments.length() == 0) {
                gotOldestPaymentAddresses = true;
                JSONObject returnObject = new JSONObject();
                returnObject.put("gotOldestPaymentAddresses", gotOldestPaymentAddresses);
                returnObject.put("latestTxTime", 0);
                returnObject.put("stealthPayments", stealthPayments);
                return returnObject;
            }

            long txTimeLowerBound = this.getStealthAddressLastTxTime();
            long olderTxTime = stealthPayments.getJSONObject(stealthPayments.length()-1).getLong("time");
            if (olderTxTime < txTimeLowerBound || stealthPayments.length() < TLStealthExplorerAPI.STEALTH_PAYMENTS_FETCH_COUNT) {
                gotOldestPaymentAddresses = true;
            }

            long latestTxTime = stealthPayments.getJSONObject(0).getLong("time");

            JSONObject returnObject = new JSONObject();
            returnObject.put("gotOldestPaymentAddresses", gotOldestPaymentAddresses);
            returnObject.put("latestTxTime", latestTxTime);
            returnObject.put("stealthPayments", stealthPayments);
            return returnObject;
        } catch (JSONException e) {
            return null;
        }
    }

    boolean watchStealthAddress(String stealthAddress, String scanPriv, String spendPriv, String scanPublicKey) {
        String signature = getChallengeAndSign(stealthAddress, scanPriv, scanPublicKey);
        if (signature == null) {
            return false;
        }

        int consecutiveInvalidSignatures = 0;
        try {
            while (true) {
                JSONObject jsonData = this.appDelegate.stealthExplorerAPI.watchStealthAddressSynchronous(stealthAddress, scanPriv, signature);
                if (jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                    return false;
                }
                try {
                    if (jsonData.has(TLStealthExplorerAPI.SERVER_ERROR_CODE) &&
                            jsonData.getInt(TLStealthExplorerAPI.SERVER_ERROR_CODE) == TLStealthExplorerAPI.INVALID_SIGNATURE_ERROR) {
                        TLStealthWallet.challengeNeedsRefreshing = true;
                        signature = this.getChallengeAndSign(stealthAddress, scanPriv, scanPublicKey);
                        if (signature == null) {
                            return false;
                        }
                        consecutiveInvalidSignatures += 1;
                        if (consecutiveInvalidSignatures > MAX_CONSECUTIVE_INVALID_SIGNATURES) {
                            return false;
                        } else {
                            continue;
                        }
                    } else {
                        try {
                            boolean success = jsonData.getBoolean("success");
                            return success;
                        } catch (JSONException e1) {
                            return false;
                        }
                    }
                } catch (JSONException e2) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }
}
