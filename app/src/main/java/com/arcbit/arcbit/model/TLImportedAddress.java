package com.arcbit.arcbit.model;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.support.v4.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.arcbit.arcbit.APIs.TLNetworking;
import com.arcbit.arcbit.model.TLWalletJSONKeys.TLAddressStatus;
import com.arcbit.arcbit.model.TLWalletUtils.TLAccountTxType;
import com.arcbit.arcbit.model.TLOperationsManager.TLDownloadState;

public class TLImportedAddress {

    TLAppDelegate appDelegate;
    private TLWallet appWallet;
    private JSONObject addressDict;
    public boolean haveUpDatedUTXOs = false;
    public int unspentOutputsCount = 0;
    private JSONArray unspentOutputs;
    private TLCoin unspentOutputsSum;
    public TLCoin balance = TLCoin.zero();
    private boolean fetchedAccountData = false;
    boolean listeningToIncomingTransactions = false;
    private boolean watchOnly = false;
    private boolean archived = false;
    private int positionInWalletArray;
    private ArrayList<TLTxObject> txObjectArray;
    private Map<String, TLCoin> txidToAccountAmountDict;
    private Map<String, TLAccountTxType> txidToAccountAmountTypeDict;
    Set<String> processedTxSet;
    private String privateKey;
    private String importedAddress;
    public TLDownloadState downloadState = TLDownloadState.NotDownloading;

    public TLImportedAddress(TLAppDelegate appDelegate, TLWallet appWallet, JSONObject dict) {
        this.appDelegate = appDelegate;
        this.appWallet = appWallet;
        addressDict = dict;
        try {
            importedAddress = addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
            unspentOutputs = new JSONArray();
            processedTxSet = new HashSet<String>();
            if (addressDict.has(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_KEY)) {
                this.watchOnly = false;
            } else {
                this.watchOnly = true;
            }

            this.archived = addressDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS) == TLAddressStatus.Archived.getValue();
            resetAccountBalances();
        } catch (JSONException e) {
        }
    }

    public boolean hasSetPrivateKeyInMemory() {
        return privateKey != null;
    }

    public boolean setPrivateKeyInMemory(String privKey) {
        if (TLBitcoinjWrapper.getAddress(privKey, this.appWallet.walletConfig.isTestnet).equals(getAddress())) {
            privateKey = privKey;
            return true;
        }
        return false;
    }

    public void clearPrivateKeyFromMemory() {
        privateKey = null;
    }

    public String getDefaultAddressLabel() {
        return importedAddress;
    }

    public void setHasFetchedAccountData(boolean fetched) {
        this.fetchedAccountData = fetched;
        if (fetched) {
            this.downloadState = TLDownloadState.Downloaded;
        }
        if (this.fetchedAccountData == true && this.listeningToIncomingTransactions == false) {
            this.listeningToIncomingTransactions = true;
            String address = this.getAddress();
            appDelegate.transactionListener.listenToIncomingTransactionForAddress(address);
        }
    }

    public void setFetchedAccountData(boolean fetched) {
        fetchedAccountData = fetched;
    }

    public boolean hasFetchedAccountData(){
        return this.fetchedAccountData;
    }

    public JSONArray getUnspentArray() {
        return unspentOutputs;
    }

    public TLCoin getUnspentSum() {
        if (unspentOutputsSum != null) {
            return unspentOutputsSum;
        }

        if (unspentOutputs == null) {
            return TLCoin.zero();
        }

        long unspentOutputsSumTemp = 0;
        for (int i = 0; i < unspentOutputs.length(); i++) {
            try {
                JSONObject unspentOutput = unspentOutputs.getJSONObject(i);
                long amount = unspentOutput.getLong("value");
                unspentOutputsSumTemp += amount;
            } catch (JSONException e) {
            }
        }

        unspentOutputsSum = new TLCoin(unspentOutputsSumTemp);
        return unspentOutputsSum;
    }

    public int getInputsNeededToConsume(TLCoin amountNeeded) {
        long valueSelected = 0;
        int inputCount = 0;
        for (int i = 0; i < unspentOutputs.length(); i++) {
            try {
                JSONObject unspentOutput = unspentOutputs.getJSONObject(i);
                long amount = unspentOutput.getLong("value");
                valueSelected += amount;
                inputCount++;
                if (valueSelected >= amountNeeded.toNumber()) {
                    return inputCount;
                }
            } catch (JSONException e) {
            }
        }
        return inputCount;
    }

    public void setUnspentOutputs(JSONArray unspentOuts) {
        unspentOutputs = unspentOuts;
    }

    public TLCoin getBalance() {
        return this.balance;
    }

    public boolean isWatchOnly() {
        return this.watchOnly;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isArchived() {
        return this.archived;
    }

    public int getPositionInWalletArray() {
        return positionInWalletArray;
    }

    public int getPositionInWalletArrayNumber() {
        return positionInWalletArray;
    }


    public void setPositionInWalletArray(int idx) {
        positionInWalletArray = idx;
    }

    public boolean isPrivateKeyEncrypted() {
        if (this.watchOnly) {
            return false;
        }
        try {
            if (TLBitcoinjWrapper.isBIP38EncryptedKey(addressDict.getString("key"), this.appWallet.walletConfig.isTestnet)) {
                return true;
            }
        } catch (JSONException e) {
        }
        return false;
    }

    public String getAddress() {
        try {
            return addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
        } catch (JSONException e) {
            return null;
        }
    }

    public String getEitherPrivateKeyOrEncryptedPrivateKey() {
        if (this.watchOnly) {
            return privateKey;
        } else {
            try {
                return addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_KEY);
            } catch (JSONException e) {
                return null;
            }
        }
    }

    public String getPrivateKey() {
        if (this.watchOnly) {
            return privateKey;
        }
        else if (isPrivateKeyEncrypted()) {
            return privateKey;
        }
        else {
            try {
                return addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_KEY);
            } catch (JSONException e) {
                return null;
            }
        }
    }

    public String getEncryptedPrivateKey() {
        if (isPrivateKeyEncrypted()) {
            try {
                return addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_KEY);
            } catch (JSONException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public String getLabel() {
        try {
            if (addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL) == null ||
                    addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL) == "") {
                return addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
            }
            else {
                return addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL);
            }
        } catch (JSONException e) {
            return null;
        }
    }

    public int getTxObjectCount() {
        return txObjectArray.size();
    }

    public TLTxObject getTxObject(int txIdx) {
        return txObjectArray.get(txIdx);
    }

    public TLCoin getAccountAmountChangeForTx(String txHash) {
        return txidToAccountAmountDict.get(txHash);
    }

    public TLAccountTxType getAccountAmountChangeTypeForTx(String txHash) {
        return txidToAccountAmountTypeDict.get(txHash);
    }


    public TLCoin processNewTx(TLTxObject txObject) {
        if (processedTxSet.contains(txObject.getHash())) {
            // happens when you send coins to the same account, so you get the same tx from the websockets more then once
            return null;
        }
        Pair<Boolean, TLCoin> doesTxInvolveAddressAndReceivedAmount = processTx(txObject, true);

        txObjectArray.add(0, txObject);
        return doesTxInvolveAddressAndReceivedAmount.second;
    }

    void processTxArray(JSONArray txArray, boolean shouldUpdateAccountBalance) {
        resetAccountBalances();

        for (int i = 0; i < txArray.length(); i++) {
            try {
                JSONObject tx = txArray.getJSONObject(i);
                TLTxObject txObject = new TLTxObject(this.appDelegate, tx);
                Pair<Boolean, TLCoin> doesTxInvolveAddressAndReceivedAmount = processTx(txObject, shouldUpdateAccountBalance);
                if (doesTxInvolveAddressAndReceivedAmount.first) {
                    txObjectArray.add(txObject);
                }
            } catch (JSONException e) {
            }
        }
    }

    private Pair<Boolean, TLCoin> processTx(TLTxObject txObject, boolean shouldUpdateAccountBalance) {
        haveUpDatedUTXOs = false;
        processedTxSet.add(txObject.getHash());
        long currentTxSubtract = 0;
        long currentTxAdd = 0;
        boolean doesTxInvolveAddress = false;
        JSONArray outputAddressToValueArray = txObject.getOutputAddressToValueArray();
        for (int i = 0; i < outputAddressToValueArray.length(); i++) {
            try {
                JSONObject output = outputAddressToValueArray.getJSONObject(i);
                long value = 0;
                if (output.has("value")) {
                    value = output.getLong("value");
                }
                String address = output.getString("addr");
                if (address != null && address.equals(importedAddress)) {
                    currentTxAdd += value;
                    doesTxInvolveAddress = true;
                }
            } catch (JSONException e) {
            }
        }

        JSONArray inputAddressToValueArray = txObject.getInputAddressToValueArray();
        for (int i = 0; i < inputAddressToValueArray.length(); i++) {
            try {
                JSONObject input = inputAddressToValueArray.getJSONObject(i);
                long value = 0;
                if (input.has("value")) {
                    value = input.getLong("value");
                }
                String address = input.getString("addr");
                if (address != null && address.equals(importedAddress)) {
                    currentTxSubtract += value;
                    doesTxInvolveAddress = true;
                }
            } catch (JSONException e) {
            }
        }

        if (shouldUpdateAccountBalance) {
            this.balance = new TLCoin(this.balance.toNumber() + currentTxAdd - currentTxSubtract);
        }

        TLCoin receivedAmount;
        if (currentTxSubtract > currentTxAdd) {
            TLCoin amountChangeToAccountFromTx = new TLCoin(currentTxSubtract - currentTxAdd);
            txidToAccountAmountDict.put(txObject.getHash(), amountChangeToAccountFromTx);
            txidToAccountAmountTypeDict.put(txObject.getHash(), TLAccountTxType.Send);
            receivedAmount = null;
        } else if (currentTxSubtract < currentTxAdd) {
            TLCoin amountChangeToAccountFromTx = new TLCoin(currentTxAdd - currentTxSubtract);
            txidToAccountAmountDict.put(txObject.getHash(), amountChangeToAccountFromTx);
            txidToAccountAmountTypeDict.put(txObject.getHash(), TLAccountTxType.Receive);
            receivedAmount = amountChangeToAccountFromTx;
        } else {
            TLCoin amountChangeToAccountFromTx = TLCoin.zero();
            txidToAccountAmountDict.put(txObject.getHash(), amountChangeToAccountFromTx);
            txidToAccountAmountTypeDict.put(txObject.getHash(), TLAccountTxType.MoveBetweenAccount);
            receivedAmount = null;
        }
        return new Pair<Boolean, TLCoin>(doesTxInvolveAddress, receivedAmount);
    }

    public void getSingleAddressDataO(boolean fetchDataAgain) {
        this.getSingleAddressDataO(fetchDataAgain, null);
    }

    public void getSingleAddressDataO(boolean fetchDataAgain, TLCallback callback) {
        if (this.fetchedAccountData == true && !fetchDataAgain) {
            this.downloadState = TLDownloadState.Downloaded;
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                JSONObject jsonData = (JSONObject) msg.obj;
                try {
                    if (jsonData == null || jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                        downloadState = TLDownloadState.Failed;
                        if (callback != null) {
                            callback.onFail(jsonData.getInt(TLNetworking.HTTP_ERROR_CODE), jsonData.getString(TLNetworking.HTTP_ERROR_MSG));
                        }
                        return;
                    }
                    JSONArray addressesArray = jsonData.getJSONArray("addresses");
                    for (int i = 0; i < addressesArray.length(); i++) {
                        JSONObject addressDict = addressesArray.getJSONObject(i);
                        long addressBalance = addressDict.getLong("final_balance");
                        balance = new TLCoin(addressBalance);
                        processTxArray(jsonData.getJSONArray("txs"), false);
                    }
                    setHasFetchedAccountData(true);
                    Intent intent = new Intent(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA);
                    LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(intent);
                    if (callback != null) {
                        callback.onSuccess(jsonData);
                    }
                } catch (JSONException e) {
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonData = appDelegate.blockExplorerAPI.getAddressesInfo(new ArrayList<String>(Arrays.asList(importedAddress)));
                    Message message = Message.obtain();
                    message.obj = jsonData;
                    handler.sendMessage(Message.obtain(message));
                } catch (Exception e) {
                    Log.d("TLAccountObject", e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void setLabel(String label){
        try {
            addressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL, label);
        } catch (JSONException e) {
        }
    }

    private void resetAccountBalances() {
        txObjectArray = new ArrayList<TLTxObject>();
        txidToAccountAmountDict = new HashMap<String, TLCoin>();
        txidToAccountAmountTypeDict = new HashMap<String, TLAccountTxType>();
    }
}
