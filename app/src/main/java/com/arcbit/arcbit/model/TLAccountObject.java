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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.arcbit.arcbit.APIs.TLNetworking;
import com.arcbit.arcbit.APIs.TLStealthExplorerAPI;
import com.arcbit.arcbit.model.TLWalletUtils.TLAccountTxType;
import com.arcbit.arcbit.model.TLWalletUtils.TLAccountType;
import com.arcbit.arcbit.model.TLOperationsManager.TLDownloadState;
import com.arcbit.arcbit.model.TLWalletJSONKeys.TLStealthPaymentStatus;
import com.arcbit.arcbit.model.TLStealthWallet.GetAndStoreStealthPaymentsResults;

public class TLAccountObject {
    private final String TAG = getClass().getSimpleName();

    public static int ACCOUNT_UNUSED_ACTIVE_MAIN_ADDRESS_AHEAD_OF_LATEST_USED_ONE_MINIMUM_COUNT = 5;
    public static int ACCOUNT_UNUSED_ACTIVE_CHANGE_ADDRESS_AHEAD_OF_LATEST_USED_ONE_MINIMUM_COUNT = 5;
    public static int GAP_LIMIT = 20;
    public static int MAX_ACTIVE_MAIN_ADDRESS_TO_HAVE = 55;
    public static int MAX_ACTIVE_CHANGE_ADDRESS_TO_HAVE = 55;
    public static int EXTENDED_KEY_DEFAULT_ACCOUNT_NAME_LENGTH = 50;
    public static int MAX_CONSOLIDATE_STEALTH_PAYMENT_UTXOS_COUNT = 12;

    TLAppDelegate appDelegate;
    public TLWallet appWallet;
    private JSONObject accountDict;
    public boolean haveUpDatedUTXOs = false;
    public int unspentOutputsCount = 0;
    public int stealthPaymentUnspentOutputsCount = 0;
    public JSONArray unspentOutputs;
    private JSONArray stealthPaymentUnspentOutputs;

    public List<String> mainActiveAddresses = new ArrayList<String>();
    public List<String> changeActiveAddresses = new ArrayList<String>();
    private Set<String> activeAddressesDict = new HashSet<String>();
    public List<String> mainArchivedAddresses = new ArrayList<String>();
    public List<String> changeArchivedAddresses = new ArrayList<String>();
    private Map<String, TLCoin> address2BalanceDict = new HashMap<String, TLCoin>();
    private Map<String, Integer> address2HDIndexDict = new HashMap<String, Integer>();
    private Map<String, Boolean> address2IsMainAddress = new HashMap<String, Boolean>();
    private Map<String, Integer> address2NumberOfTransactions = new HashMap<String, Integer>();
    private Map<Integer, String> HDIndexToArchivedMainAddress = new HashMap<Integer, String>();
    private Map<Integer, String> HDIndexToArchivedChangeAddress = new HashMap<Integer, String>();
    private List<TLTxObject> txObjectArray = new ArrayList<TLTxObject>();
    private Map<String, TLCoin> txidToAccountAmountDict = new HashMap<String, TLCoin>();
    private Map<String, TLAccountTxType> txidToAccountAmountTypeDict = new HashMap<String, TLAccountTxType>();
    private List<String> receivingAddressesArray = new ArrayList<String>();
    private Set<String> processedTxDict = new HashSet<String>();
    private TLAccountType accountType;
    public TLCoin accountBalance = TLCoin.zero();
    private TLCoin totalUnspentOutputsSum;
    private boolean fetchedAccountData = false;
    boolean  listeningToIncomingTransactions = false;
    private int positionInWalletArray = 0;
    private String extendedPrivateKey;
    public TLStealthWallet stealthWallet;
    public TLDownloadState downloadState = TLDownloadState.NotDownloading;

    public static int MAX_ACCOUNT_WAIT_TO_RECEIVE_ADDRESS() {
        return 5;
    }

    public int NUM_ACCOUNT_STEALTH_ADDRESSES() {
        return 1;
    }

    private void setUpActiveMainAddresses() {
        mainActiveAddresses = new ArrayList<String>();
        try {
            JSONArray addressesArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES);
            int minAddressIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX);
            int startIdx;
            if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
                startIdx = minAddressIdx;
            } else {
                startIdx = 0;
            }
            for (int i = startIdx; i < addressesArray.length(); i++) {
                JSONObject addressDict = addressesArray.getJSONObject(i);
                int HDIndex = addressDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_INDEX);
                String address = addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
                address2HDIndexDict.put(address, HDIndex);
                address2IsMainAddress.put(address, true);
                address2BalanceDict.put(address, TLCoin.zero());
                address2NumberOfTransactions.put(address, 0);
                mainActiveAddresses.add(address);
                activeAddressesDict.add(address);
            }
        } catch (JSONException e) {
        }
    }

    private void setUpActiveChangeAddresses(){
        changeActiveAddresses = new ArrayList<String>();
        try {
            JSONArray addressesArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES);
            int minAddressIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX);

            int startIdx;
            if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
                startIdx = minAddressIdx;
            } else {
                startIdx = 0;
            }
            for (int i = startIdx; i < addressesArray.length(); i++) {
                JSONObject addressDict = addressesArray.getJSONObject(i);
                int HDIndex = addressDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_INDEX);
                String address = addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
                address2HDIndexDict.put(address, HDIndex);
                address2IsMainAddress.put(address, false);
                address2BalanceDict.put(address, TLCoin.zero());
                address2NumberOfTransactions.put(address, 0);
                changeActiveAddresses.add(address);
                activeAddressesDict.add(address);
            }
        } catch (JSONException e) {
        }
    }

    private void setUpArchivedMainAddresses() {
        mainArchivedAddresses = new ArrayList<String>();
        try {
            JSONArray addressesArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES);
            int maxAddressIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX);

            for (int i = 0; i < maxAddressIdx; i++) {
                JSONObject addressDict = addressesArray.getJSONObject(i);
                assert(addressDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS) == TLWalletJSONKeys.TLAddressStatus.Archived.getValue());
                int HDIndex = addressDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_INDEX);
                String address = addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
                address2HDIndexDict.put(address, HDIndex);
                address2IsMainAddress.put(address, true);
                address2BalanceDict.put(address, TLCoin.zero());
                address2NumberOfTransactions.put(address, 0);
                mainArchivedAddresses.add(address);
            }
        } catch (JSONException e) {
        }
    }

    private void setUpArchivedChangeAddresses() {
        changeArchivedAddresses = new ArrayList<String>();
        try {
            JSONArray addressesArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES);
            int maxAddressIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX);

            for (int i = 0; i < maxAddressIdx; i++) {
                JSONObject addressDict = addressesArray.getJSONObject(i);
                assert(addressDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS) == TLWalletJSONKeys.TLAddressStatus.Archived.getValue());
                int HDIndex = addressDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_INDEX);
                String address = addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
                address2HDIndexDict.put(address, HDIndex);
                address2IsMainAddress.put(address, false);
                address2BalanceDict.put(address, TLCoin.zero());
                address2NumberOfTransactions.put(address, 0);
                changeArchivedAddresses.add(address);
            }
        } catch (JSONException e) {
        }
    }

    TLAccountObject(TLAppDelegate appDelegate, TLWallet appWallet, JSONObject dict, TLAccountType accountType) {
        this.appDelegate = appDelegate;
        this.appWallet = appWallet;
        this.accountType = accountType;
        accountDict = dict;
        unspentOutputs = null;
        totalUnspentOutputsSum = null;
        extendedPrivateKey = null;

        txidToAccountAmountTypeDict = new HashMap<String, TLAccountTxType>();
        address2BalanceDict = new HashMap<String, TLCoin>();

        setUpActiveMainAddresses();
        setUpActiveChangeAddresses();
        if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
            setUpArchivedMainAddresses();
            setUpArchivedChangeAddresses();
        } else {
            HDIndexToArchivedMainAddress = new HashMap<Integer, String>();
            HDIndexToArchivedChangeAddress = new HashMap<Integer, String>();
        }

        if (accountType == TLAccountType.HDWallet) {
            positionInWalletArray = getAccountIdxNumber();
        } else if (accountType == TLAccountType.ColdWallet) {
        } else if (accountType == TLAccountType.Imported) {
            //set later in accounts
        } else if (accountType == TLAccountType.ImportedWatch) {
            //set later in accounts
        }
        try {
            if (accountType != TLAccountType.ImportedWatch && accountType != TLAccountType.ColdWallet) {
                JSONArray stealthAddressArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESSES);
                JSONObject stealthWalletDict = stealthAddressArray.getJSONObject(0);
                this.stealthWallet = new TLStealthWallet(this.appDelegate, stealthWalletDict, this, !this.isArchived());


                //add default zero balance so that if user goes to address list view before account data downloaded,
                // then accountObject will return a 0 balance for payment address instead of getting optional unwrap null error
                for (int i = 0; i < this.stealthWallet.getStealthAddressPaymentsCount(); i++) {
                    String address = this.stealthWallet.getPaymentAddressForIndex(i);
                    address2BalanceDict.put(address, TLCoin.zero());
                }
            }
        } catch (JSONException e) {
        }
    }

    public boolean isWatchOnly() {
        return accountType == TLAccountType.ImportedWatch;
    }

    public boolean isColdWalletAccount() {
        return accountType == TLAccountType.ColdWallet;
    }

    public boolean hasSetExtendedPrivateKeyInMemory() {
        assert(accountType == TLAccountType.ImportedWatch);
        return extendedPrivateKey != null;
    }

    public boolean setExtendedPrivateKeyInMemory(String extendedPrivKey) {
        assert(accountType == TLAccountType.ImportedWatch);
        assert(TLHDWalletWrapper.isValidExtendedPrivateKey(extendedPrivKey, this.appWallet.walletConfig.isTestnet)); //"extendedPrivKey isValidExtendedPrivateKey"
        if (TLHDWalletWrapper.getExtendPubKey(extendedPrivKey, this.appWallet.walletConfig.isTestnet).equals(getExtendedPubKey())) {
            extendedPrivateKey = extendedPrivKey;
            return true;
        }
        return false;
    }

    public void clearExtendedPrivateKeyFromMemory() {
        assert(accountType == TLAccountType.ImportedWatch);
        extendedPrivateKey = null;
    }

    public void setFetchedAccountData(boolean fetched) {
        fetchedAccountData = fetched;
    }

    public boolean hasFetchedAccountData() {
        return fetchedAccountData;
    }

    public boolean renameAccount(String accountName) {
        try {
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_NAME, accountName);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public String getAccountName() {
        try {
            return accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_NAME);
        } catch (JSONException e) {
            return null;
        }
    }

    public String getAccountNameOrAccountPublicKey() {
        try {
            String accountName = accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_NAME);
            return accountName.length() != 0 ? accountName : getExtendedPubKey();
        } catch (JSONException e) {
            return getExtendedPubKey();
        }
    }

    boolean archiveAccount(boolean enabled) {
        TLWalletJSONKeys.TLAddressStatus status = enabled ? TLWalletJSONKeys.TLAddressStatus.Archived : TLWalletJSONKeys.TLAddressStatus.Active;
        try {
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, status.getValue());
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean isArchived() {
        try {
            return accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS) == TLWalletJSONKeys.TLAddressStatus.Archived.getValue();
        } catch (JSONException e) {
            return false;
        }
    }

    String getAccountID() {
        try {
            int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);
            return String.valueOf(accountIdx);
        } catch (JSONException e) {
            return null;
        }
    }

    public int getAccountIdxNumber() {
        try {
            return accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);
        } catch (JSONException e) {
            return 0;
        }
    }

    public int getAccountHDIndex() {
        return TLHDWalletWrapper.getAccountIdxForExtendedKey(getExtendedPubKey(), this.appWallet.walletConfig.isTestnet);
    }

    public String getExtendedPubKey() {
        try {
            return accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PUBLIC_KEY);
        } catch (JSONException e) {
            return null;
        }
    }

    public String getExtendedPrivKey() {
        try {
            if (accountType == TLAccountType.HDWallet) {
                return accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PRIVATE_KEY);
            } else if (accountType == TLAccountType.Imported) {
                return accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PRIVATE_KEY);
            } else if (accountType == TLAccountType.ImportedWatch) {
                return extendedPrivateKey;
            }
        } catch (JSONException e) {
        }
        return null;
    }


    public TLCoin getAddressBalance(String address) {
        TLCoin amount = address2BalanceDict.get(address);
        if (amount != null) {
            return amount;
        } else {
            return TLCoin.zero();
        }
    }

    public int getNumberOfTransactionsForAddress(String address) {
        assert(this.isHDWalletAddress(address));
        if (address2NumberOfTransactions.get(address) == null) {
            return 0;
        }
        return address2NumberOfTransactions.get(address);
    }

    public boolean isMainAddress(String address) {
        return address2IsMainAddress.get(address);
    }

    public int getAddressHDIndex(String address) {
        return address2HDIndexDict.get(address);
    }

    public String getAccountPrivateKey(String address) {
        if (this.isHDWalletAddress(address)) {
            if (address2IsMainAddress.get(address) == true) {
                return getMainPrivateKey(address);
            } else {
                return getChangePrivateKey(address);
            }
        }

        return null;
    }

    public String getMainPrivateKey(String address) {
        try {
            int HDIndexNumber = address2HDIndexDict.get(address);
            ArrayList<Integer> addressSequence = new ArrayList<Integer>(Arrays.asList((Integer)TLWalletJSONKeys.TLAddressType.Main.getValue(), HDIndexNumber));
            if (accountType == TLAccountType.ImportedWatch) {
                assert(extendedPrivateKey != null);
                return TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence, this.appWallet.walletConfig.isTestnet);
            } else {
                return TLHDWalletWrapper.getPrivateKey(accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PRIVATE_KEY),
                        addressSequence, this.appWallet.walletConfig.isTestnet);
            }
        } catch (JSONException e) {
            return null;
        }
    }

    public String getChangePrivateKey(String address) {
        try {
            int HDIndexNumber = address2HDIndexDict.get(address);
            ArrayList<Integer> addressSequence = new ArrayList<Integer>(Arrays.asList((Integer)TLWalletJSONKeys.TLAddressType.Change.getValue(), HDIndexNumber));
            if (accountType == TLAccountType.ImportedWatch) {
                assert(extendedPrivateKey != null);
                return TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence, this.appWallet.walletConfig.isTestnet);
            } else {
                return TLHDWalletWrapper.getPrivateKey(accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PRIVATE_KEY),
                        addressSequence, this.appWallet.walletConfig.isTestnet);
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

    public boolean isAddressPartOfAccountActiveChangeAddresses(String address) {
        return changeActiveAddresses.indexOf(address) != -1;
    }
    
    private boolean isAddressPartOfAccountActiveMainAddresses(String address){
        return mainActiveAddresses.indexOf(address) != -1;
    }

    private boolean isActiveAddress(String address){
        return activeAddressesDict.contains(address);
    }

    private boolean isHDWalletAddress(String address){
        return address2HDIndexDict.get(address) != null;
    }

    boolean isAddressPartOfAccount(String address) {
        if (this.stealthWallet == null) {
            return this.isHDWalletAddress(address);
        } else {
            return this.isHDWalletAddress(address) || this.stealthWallet.isPaymentAddress(address);
        }
    }

    public TLCoin getBalance() {
        return this.accountBalance;
    }

    public TLAccountType getAccountType() {
        return accountType;
    }

    TLCoin getAccountAmountChangeForTx(String txHash) {
        return txidToAccountAmountDict.get(txHash);
    }

    TLAccountTxType getAccountAmountChangeTypeForTx(String txHash) {
        return txidToAccountAmountTypeDict.get(txHash);
    }

    private void addToAddressBalance(String address, TLCoin amount) {
        TLCoin addressBalance = address2BalanceDict.get(address);

        if (addressBalance == null) {
            addressBalance = amount;
            address2BalanceDict.put(address, addressBalance);
        } else {
            addressBalance = addressBalance.add(amount);
            address2BalanceDict.put(address, addressBalance);
        }
    }

    private void subtractToAddressBalance(String address, TLCoin amount) {
        TLCoin addressBalance = address2BalanceDict.get(address);
        if (addressBalance == null) {
            addressBalance = amount;
            address2BalanceDict.put(address, addressBalance);
        } else {
            addressBalance = addressBalance.subtract(amount);
            address2BalanceDict.put(address, addressBalance);
        }
    }

    TLCoin processNewTx(TLTxObject txObject) {
        if (processedTxDict.contains(txObject.getHash())) {
            return null;
        }
        TLCoin receivedAmount = processTx(txObject, true, true);
        txObjectArray.add(0, txObject);
        checkToArchiveAddresses();
        updateReceivingAddresses();
        updateChangeAddresses();
        return receivedAmount;
    }

    private TLCoin processTx(TLTxObject txObject, boolean shouldCheckToAddressesNTxsCount, boolean shouldUpdateAccountBalance) {
        haveUpDatedUTXOs = false;
        processedTxDict.add(txObject.getHash());
        long currentTxSubtract = 0;
        long currentTxAdd = 0;

        Set<String> address2hasUpdatedNTxCount = new HashSet<String>();

        Log.d("TLAccountObject", "processTx: " + shouldUpdateAccountBalance + " " + this.getAccountID() + " " + txObject.getTxid());

        JSONArray outputAddressToValueArray = txObject.getOutputAddressToValueArray();
        for (int i = 0; i < outputAddressToValueArray.length(); i++) {
            try {
                JSONObject output = outputAddressToValueArray.getJSONObject(i);
                long value = 0;
                if (output.has("value")) {
                    value = output.getLong("value");
                }
                if (output.has("addr")) {
                    String address = output.getString("addr");
                    if (isActiveAddress(address)) {

                        currentTxAdd += value;
                        //Log.d("TLAccountObject", "addToAddressBalance: " + address + " " + value);
                        if (shouldUpdateAccountBalance) {
                            //Log.d("TLAccountObject", "addToAddressBalance shouldUpdateAccountBalance: " + address + " " + value);
                            addToAddressBalance(address, new TLCoin(value));
                        }

                        if (shouldCheckToAddressesNTxsCount &&
                                !address2hasUpdatedNTxCount.contains(address)) {

                            address2hasUpdatedNTxCount.add(address);

                            int ntxs = getNumberOfTransactionsForAddress(address);
                            address2NumberOfTransactions.put(address, ntxs + 1);
                        }
                    } else if (this.stealthWallet != null && this.stealthWallet.isPaymentAddress(address)) {
                        currentTxAdd += value;
                        //Log.d("TLAccountObject", "addToAddressBalance stealth: " + address + " " + value);
                        if (shouldUpdateAccountBalance) {
                            //Log.d("TLAccountObject", "addToAddressBalance stealth shouldUpdateAccountBalance: " + address + " " + value);
                            addToAddressBalance(address, new TLCoin(value));
                        }
                    } else {
                    }
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
                if (input.has("addr")) {
                    String address = input.getString("addr");
                    if (isActiveAddress(address)) {

                        currentTxSubtract += value;
                        //Log.d("TLAccountObject", "subtractToAddressBalance: " + address + " " + value);
                        if (shouldUpdateAccountBalance) {
                            subtractToAddressBalance(address, new TLCoin(value));
                        }

                        if (shouldCheckToAddressesNTxsCount &&
                                !address2hasUpdatedNTxCount.contains(address)) {

                            address2hasUpdatedNTxCount.add(address);

                            int ntxs = getNumberOfTransactionsForAddress(address);
                            address2NumberOfTransactions.put(address, ntxs + 1);
                        }
                    } else if (this.stealthWallet != null && this.stealthWallet.isPaymentAddress(address)) {
                        currentTxSubtract += value;
                        //Log.d("TLAccountObject", "subtractToAddressBalance stealth: " + address + " " + value);
                        if (shouldUpdateAccountBalance) {
                            subtractToAddressBalance(address, new TLCoin(value));
                        }
                    } else {
                    }
                }
            } catch (JSONException e) {
            }
        }

        //DLog("current processTxprocessTx \(this.accountBalance.toUInt64()) + \(currentTxAdd) - \(currentTxSubtract)")
        if (shouldUpdateAccountBalance) {
            this.accountBalance = new TLCoin(this.accountBalance.toNumber() + currentTxAdd - currentTxSubtract);
        }

        if (currentTxSubtract > currentTxAdd) {
            TLCoin amountChangeToAccountFromTx = new TLCoin(currentTxSubtract - currentTxAdd);
            txidToAccountAmountDict.put(txObject.getHash(), amountChangeToAccountFromTx);
            txidToAccountAmountTypeDict.put(txObject.getHash(), TLAccountTxType.Send);
            return null;
        } else if (currentTxSubtract < currentTxAdd) {
            TLCoin amountChangeToAccountFromTx = new TLCoin(currentTxAdd - currentTxSubtract);
            txidToAccountAmountDict.put(txObject.getHash(), amountChangeToAccountFromTx);
            txidToAccountAmountTypeDict.put(txObject.getHash(), TLAccountTxType.Receive);
            return amountChangeToAccountFromTx;
        } else {
            TLCoin amountChangeToAccountFromTx = TLCoin.zero();
            txidToAccountAmountDict.put(txObject.getHash(), amountChangeToAccountFromTx);
            txidToAccountAmountTypeDict.put(txObject.getHash(), TLAccountTxType.MoveBetweenAccount);
            return null;
        }
    }

    int getReceivingAddressesCount() {
        return receivingAddressesArray.size();
    }

    String getReceivingAddress(int idx) {
        return receivingAddressesArray.get(idx);
    }

    private void updateReceivingAddresses() {
        receivingAddressesArray = new ArrayList<String>();

        int addressIdx = 0;
        for (addressIdx = 0; addressIdx < mainActiveAddresses.size(); addressIdx++) {
            String address = mainActiveAddresses.get(addressIdx);
            if (getNumberOfTransactionsForAddress(address) == 0) {
                break;
            }
        }

        boolean lookedAtAllAddresses = false;
        long receivingAddressesStartIdx = -1;
        for (; addressIdx < addressIdx + TLAccountObject.MAX_ACCOUNT_WAIT_TO_RECEIVE_ADDRESS(); addressIdx++) {
            if (addressIdx >= getMainActiveAddressesCount()) {
                lookedAtAllAddresses = true;
                break;
            }

            String address = mainActiveAddresses.get(addressIdx);
            if (getNumberOfTransactionsForAddress(address) == 0) {
                receivingAddressesArray.add(address);;
                if (receivingAddressesStartIdx == -1) {
                    receivingAddressesStartIdx = addressIdx;
                }
            }
            if (receivingAddressesArray.size() >= TLAccountObject.MAX_ACCOUNT_WAIT_TO_RECEIVE_ADDRESS() ||
                    addressIdx - receivingAddressesStartIdx >= TLAccountObject.MAX_ACCOUNT_WAIT_TO_RECEIVE_ADDRESS()) {
                break;
            }
        }

        while (lookedAtAllAddresses && receivingAddressesArray.size() < TLAccountObject.MAX_ACCOUNT_WAIT_TO_RECEIVE_ADDRESS()) {
            String address = getNewMainAddress(getMainAddressesCount());
            addressIdx++;
            if (addressIdx - receivingAddressesStartIdx < TLAccountObject.MAX_ACCOUNT_WAIT_TO_RECEIVE_ADDRESS()) {
                receivingAddressesArray.add(address);
            } else {
                break;
            }
        }

        while (getMainActiveAddressesCount() - addressIdx < ACCOUNT_UNUSED_ACTIVE_MAIN_ADDRESS_AHEAD_OF_LATEST_USED_ONE_MINIMUM_COUNT) {
            getNewMainAddress(getMainAddressesCount());
        }
        LocalBroadcastManager.getInstance(this.appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_UPDATED_RECEIVING_ADDRESSES));
    }


    private void updateChangeAddresses() {
        int addressIdx = 0;
        for (; addressIdx < changeActiveAddresses.size(); addressIdx++) {
            String address = changeActiveAddresses.get(addressIdx);
            if (getNumberOfTransactionsForAddress(address) == 0) {
                break;
            }
        }
        while (getChangeActiveAddressesCount() - addressIdx < ACCOUNT_UNUSED_ACTIVE_CHANGE_ADDRESS_AHEAD_OF_LATEST_USED_ONE_MINIMUM_COUNT) {
            getNewChangeAddress(getChangeAddressesCount());
        }
    }


    private void checkToArchiveAddresses() {
        this.checkToArchiveMainAddresses();
        this.checkToArchiveChangeAddresses();
    }

    private void checkToArchiveMainAddresses() {
        if (getMainActiveAddressesCount() <= MAX_ACTIVE_MAIN_ADDRESS_TO_HAVE) {
            return;
        }

        ArrayList<String> activeMainAddresses = new ArrayList<String>(getActiveMainAddresses());

        for (String address : activeMainAddresses) {
            if (getAddressBalance(address).lessOrEqual(TLCoin.zero()) &&
                    getNumberOfTransactionsForAddress(address) > 0) {

                int addressIdx = address2HDIndexDict.get(address);
                try {
                    int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);

                    if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {

                        assert(addressIdx == mainArchivedAddresses.size());
                        mainArchivedAddresses.add(address);
                    } else {
                        if (accountType == TLAccountType.HDWallet) {
                            assert(addressIdx == this.appWallet.getMinMainAddressIdxFromHDWallet(accountIdx));
                        } else if (accountType == TLAccountType.ColdWallet) {
                            assert(addressIdx == this.appWallet.getMinMainAddressIdxFromColdWalletAccount(getPositionInWalletArray()));
                        } else if (accountType == TLAccountType.Imported) {
                            assert(addressIdx == this.appWallet.getMinMainAddressIdxFromImportedAccount(getPositionInWalletArray()));
                        } else {
                            assert(addressIdx == this.appWallet.getMinMainAddressIdxFromImportedWatchAccount(getPositionInWalletArray()));
                        }
                    }
                    assert(mainActiveAddresses.get(0) == address);
                    mainActiveAddresses.remove(0);
                    activeAddressesDict.remove(address);
                    if (accountType == TLAccountType.HDWallet) {
                        this.appWallet.updateMainAddressStatusFromHDWallet(accountIdx,
                                addressIdx,
                                TLWalletJSONKeys.TLAddressStatus.Archived);
                    } else if (accountType == TLAccountType.ColdWallet) {
                        this.appWallet.updateMainAddressStatusFromColdWalletAccount(getPositionInWalletArray(),
                                addressIdx,
                                TLWalletJSONKeys.TLAddressStatus.Archived);
                    } else if (accountType == TLAccountType.Imported) {
                        this.appWallet.updateMainAddressStatusFromImportedAccount(getPositionInWalletArray(),
                                addressIdx,
                                TLWalletJSONKeys.TLAddressStatus.Archived);
                    } else {
                        this.appWallet.updateMainAddressStatusFromImportedWatchAccount(getPositionInWalletArray(),
                                addressIdx,
                                TLWalletJSONKeys.TLAddressStatus.Archived);
                    }
                } catch (JSONException e) {
                }
            } else {
                return;
            }
            if (getMainActiveAddressesCount() <= MAX_ACTIVE_MAIN_ADDRESS_TO_HAVE) {
                return;
            }
        }
    }

    private void checkToArchiveChangeAddresses() {
        if (getChangeActiveAddressesCount() <= MAX_ACTIVE_CHANGE_ADDRESS_TO_HAVE) {
            return;
        }

        ArrayList<String> activeChangeAddresses = new ArrayList<String>(getActiveMainAddresses());

        for (String address : activeChangeAddresses) {
            if (getAddressBalance(address).lessOrEqual(TLCoin.zero()) &&
                    getNumberOfTransactionsForAddress(address) > 0) {

                int addressIdx = address2HDIndexDict.get(address);
                try {
                    int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);

                    if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {

                        assert(addressIdx == changeArchivedAddresses.size());
                        changeArchivedAddresses.add(address);
                    } else {
                        if (accountType == TLAccountType.HDWallet) {
                            assert(addressIdx == this.appWallet.getMinChangeAddressIdxFromHDWallet(accountIdx));
                        } else if (accountType == TLAccountType.ColdWallet) {
                            assert(addressIdx == this.appWallet.getMinChangeAddressIdxFromColdWalletAccount(getPositionInWalletArray()));
                        } else if (accountType == TLAccountType.Imported) {
                            assert(addressIdx == this.appWallet.getMinChangeAddressIdxFromImportedAccount(getPositionInWalletArray()));
                        } else {
                            assert(addressIdx == this.appWallet.getMinChangeAddressIdxFromImportedWatchAccount(getPositionInWalletArray()));
                        }
                    }
                    assert(changeActiveAddresses.get(0) == address);
                    changeActiveAddresses.remove(0);
                    activeAddressesDict.remove(address);
                    if (accountType == TLAccountType.HDWallet) {
                        this.appWallet.updateChangeAddressStatusFromHDWallet(accountIdx,
                                addressIdx,
                                TLWalletJSONKeys.TLAddressStatus.Archived);
                    } else if (accountType == TLAccountType.ColdWallet) {
                        this.appWallet.updateChangeAddressStatusFromColdWalletAccount(getPositionInWalletArray(),
                                addressIdx,
                                TLWalletJSONKeys.TLAddressStatus.Archived);
                    } else if (accountType == TLAccountType.Imported) {
                        this.appWallet.updateChangeAddressStatusFromImportedAccount(getPositionInWalletArray(),
                                addressIdx,
                                TLWalletJSONKeys.TLAddressStatus.Archived);
                    } else {
                        this.appWallet.updateChangeAddressStatusFromImportedWatchAccount(getPositionInWalletArray(),
                                addressIdx,
                                TLWalletJSONKeys.TLAddressStatus.Archived);
                    }
                } catch (JSONException e) {
                }
            } else {
                return;
            }
            if (getChangeActiveAddressesCount() <= MAX_ACTIVE_MAIN_ADDRESS_TO_HAVE) {
                return;
            }
        }
    }

    private void processTxArray(JSONArray txArray, boolean shouldResetAccountBalance) {
        for (int i = 0; i < txArray.length(); i++) {
            try {
                JSONObject tx = txArray.getJSONObject(i);
                TLTxObject txObject = new TLTxObject(this.appDelegate, tx);
                processTx(txObject, true, false);
                txObjectArray.add(txObject);
            } catch (JSONException e) {
                Log.d("TLAccountObject", "processTxArray " + e.getLocalizedMessage());
            }
        }
        if (shouldResetAccountBalance) {
            checkToArchiveAddresses();
            updateReceivingAddresses();
            updateChangeAddresses();
        }
    }
    
    public int getPositionInWalletArray() {
        return positionInWalletArray;
    }

    void setPositionInWalletArray(int idx) {
        positionInWalletArray = idx;
    }

    public String getNewMainAddress(int expectedAddressIndex) {
        JSONObject addressDict;
        try {
            if (accountType == TLAccountType.HDWallet) {
                int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);
                addressDict = this.appWallet.getNewMainAddressFromHDWallet(accountIdx, expectedAddressIndex);
            } else if (accountType == TLAccountType.ColdWallet) {
                addressDict = this.appWallet.getNewMainAddressFromColdWalletAccount(positionInWalletArray, expectedAddressIndex);
            } else if (accountType == TLAccountType.Imported) {
                addressDict = this.appWallet.getNewMainAddressFromImportedAccount(positionInWalletArray, expectedAddressIndex);
            } else {
                addressDict = this.appWallet.getNewMainAddressFromImportedWatchAccount(positionInWalletArray, expectedAddressIndex);
            }

            String address = addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
            int HDIndex = addressDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_INDEX);
            address2HDIndexDict.put(address, HDIndex);
            address2IsMainAddress.put(address, true);
            address2BalanceDict.put(address, TLCoin.zero());
            address2NumberOfTransactions.put(address, 0);
            mainActiveAddresses.add(address);
            activeAddressesDict.add(address);
            if (appDelegate.transactionListener != null) {
                appDelegate.transactionListener.listenToIncomingTransactionForAddress(address);
            }
            return address;
        } catch (JSONException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getNewChangeAddress(int expectedAddressIndex) {
        JSONObject addressDict;
        try {
            if (accountType == TLAccountType.HDWallet) {
                int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);
                addressDict =  this.appWallet.getNewChangeAddressFromHDWallet(accountIdx, expectedAddressIndex);
            } else if (accountType == TLAccountType.ColdWallet) {
                addressDict =  this.appWallet.getNewChangeAddressFromColdWalletAccount(positionInWalletArray, expectedAddressIndex);
            } else if (accountType == TLAccountType.Imported) {
                addressDict =  this.appWallet.getNewChangeAddressFromImportedAccount(positionInWalletArray, expectedAddressIndex);
            } else {
                addressDict =  this.appWallet.getNewChangeAddressFromImportedWatchAccount(positionInWalletArray, expectedAddressIndex);
            }

            String address = addressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
            int HDIndex = addressDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_INDEX);
            address2HDIndexDict.put(address, HDIndex);
            address2IsMainAddress.put(address, false);
            address2BalanceDict.put(address, TLCoin.zero());
            address2NumberOfTransactions.put(address, 0);
            changeActiveAddresses.add(address);
            activeAddressesDict.add(address);
            if (appDelegate.transactionListener != null) {
                appDelegate.transactionListener.listenToIncomingTransactionForAddress(address);
            }
            return address;
        } catch (JSONException e) {
            Log.d("TLAccountObject", "getNewChangeAddress JSONException " + e.getLocalizedMessage());
            return null;
        } catch (Exception e) {
            Log.d("TLAccountObject", "getNewChangeAddress Exception " + e.getLocalizedMessage());
            return null;
        }
    }

    private boolean removeTopMainAddress() {
        if (accountType == TLAccountType.HDWallet) {
            try {
                int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);
                this.appWallet.removeTopMainAddressFromHDWallet(accountIdx);
            } catch (JSONException e) {
            }
        } else if (accountType == TLAccountType.ColdWallet) {
            this.appWallet.removeTopMainAddressFromColdWalletAccount(positionInWalletArray);
        } else if (accountType == TLAccountType.Imported) {
             this.appWallet.removeTopMainAddressFromImportedAccount(positionInWalletArray);
        } else if (accountType == TLAccountType.ImportedWatch) {
             this.appWallet.removeTopMainAddressFromImportedWatchAccount(positionInWalletArray);
        }
        if (mainActiveAddresses.size() > 0) {
            String address = mainActiveAddresses.get(mainActiveAddresses.size()-1);
            address2HDIndexDict.remove(address);
            address2BalanceDict.remove(address);
            address2NumberOfTransactions.remove(address);
            mainActiveAddresses.remove(mainActiveAddresses.size()-1);
            activeAddressesDict.remove(address);
            return true;
        } else if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
            if (mainArchivedAddresses.size() > 0) {
                String address = mainArchivedAddresses.get(mainArchivedAddresses.size()-1);
                address2HDIndexDict.remove(address);
                address2BalanceDict.remove(address);
                address2NumberOfTransactions.remove(address);
                mainArchivedAddresses.remove(mainArchivedAddresses.size()-1);
                activeAddressesDict.remove(address);
            }
            return true;
        }

        return false;
    }

    private boolean removeTopChangeAddress() {
        if (accountType == TLAccountType.HDWallet) {
            try {
                int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);
                this.appWallet.removeTopChangeAddressFromHDWallet(accountIdx);
            } catch (JSONException e) {
            }
        } else if (accountType == TLAccountType.ColdWallet) {
            this.appWallet.removeTopChangeAddressFromColdWalletAccount(positionInWalletArray);
        } else if (accountType == TLAccountType.Imported) {
            this.appWallet.removeTopChangeAddressFromImportedAccount(positionInWalletArray);
        } else if (accountType == TLAccountType.ImportedWatch) {
            this.appWallet.removeTopChangeAddressFromImportedWatchAccount(positionInWalletArray);
        }
        if (changeActiveAddresses.size() > 0) {
            String address = changeActiveAddresses.get(changeActiveAddresses.size()-1);
            address2HDIndexDict.remove(address);
            address2BalanceDict.remove(address);
            address2NumberOfTransactions.remove(address);
            changeActiveAddresses.remove(changeActiveAddresses.size()-1);
            activeAddressesDict.remove(address);
            return true;
        } else if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
            if (changeArchivedAddresses.size() > 0) {
                String address = changeArchivedAddresses.get(changeArchivedAddresses.size()-1);
                address2HDIndexDict.remove(address);
                address2BalanceDict.remove(address);
                address2NumberOfTransactions.remove(address);
                changeArchivedAddresses.remove(changeArchivedAddresses.size()-1);
                activeAddressesDict.remove(address);
            }
            return true;
        }

        return false;
    }

    String getCurrentChangeAddress() {
        for (String address : changeActiveAddresses) {
            if (getNumberOfTransactionsForAddress(address) == 0 && this.getAddressBalance(address).equalTo(TLCoin.zero())) {
                return address;
            }
        }
        return getNewChangeAddress(getChangeAddressesCount());
    }

    List<String> getActiveMainAddresses() {
        return mainActiveAddresses;
    }

    List<String> getActiveChangeAddresses() {
        return changeActiveAddresses;
    }

    public int getMainActiveAddressesCount() {
        return mainActiveAddresses.size();
    }

    public int getMainArchivedAddressesCount() {
        if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
            return mainArchivedAddresses.size();
        } else {
            try {
                if (accountType == TLAccountType.HDWallet) {
                    int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);
                    return this.appWallet.getMinMainAddressIdxFromHDWallet(accountIdx);
                } else if (accountType == TLAccountType.ColdWallet) {
                    return this.appWallet.getMinMainAddressIdxFromColdWalletAccount(getPositionInWalletArray());
                } else if (accountType == TLAccountType.Imported) {
                    return this.appWallet.getMinMainAddressIdxFromImportedAccount(getPositionInWalletArray());
                } else {
                    return this.appWallet.getMinMainAddressIdxFromImportedWatchAccount(getPositionInWalletArray());
                }
            } catch (JSONException e) {
                return 0;
            }
        }
    }

    private int getMainAddressesCount() {
        return getMainActiveAddressesCount() + getMainArchivedAddressesCount();
    }

    public int getChangeActiveAddressesCount() {
        return changeActiveAddresses.size();
    }

    public int getChangeArchivedAddressesCount() {
        if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
            return changeArchivedAddresses.size();
        } else {
            try {
                if (accountType == TLAccountType.HDWallet) {
                    int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);
                    return this.appWallet.getMinChangeAddressIdxFromHDWallet(accountIdx);
                } else if (accountType == TLAccountType.ColdWallet) {
                    return this.appWallet.getMinChangeAddressIdxFromColdWalletAccount(getPositionInWalletArray());
                } else if (accountType == TLAccountType.Imported) {
                    return this.appWallet.getMinChangeAddressIdxFromImportedAccount(getPositionInWalletArray());
                } else {
                    return this.appWallet.getMinChangeAddressIdxFromImportedWatchAccount(getPositionInWalletArray());
                }
            } catch (JSONException e) {
                return 0;
            }
        }
    }

    private int getChangeAddressesCount() {
        return getChangeActiveAddressesCount() + getChangeArchivedAddressesCount();
    }

    public String getMainActiveAddress(int idx) {
        return mainActiveAddresses.get(idx);
    }

    public String getChangeActiveAddress(int idx) {
        return changeActiveAddresses.get(idx);
    }

    public String getMainArchivedAddress(int idx) {
        if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
            return mainArchivedAddresses.get(idx);
        } else {
            String address = HDIndexToArchivedMainAddress.get(idx);
            if (address == null) {
                try {
                    String extendedPublicKey = accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PUBLIC_KEY);
                    ArrayList<Integer> mainAddressSequence = new ArrayList<Integer>(Arrays.asList((Integer)TLWalletJSONKeys.TLAddressType.Main.getValue(), idx));
                    address = TLHDWalletWrapper.getAddress(extendedPublicKey, mainAddressSequence, this.appWallet.walletConfig.isTestnet);
                    HDIndexToArchivedMainAddress.put(idx, address);

                    address2HDIndexDict.put(address, idx);
                    address2IsMainAddress.put(address, true);
                } catch (JSONException e) {
                }
            }
            return address;
        }
    }

    public String getChangeArchivedAddress(int idx) {
        if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
            return changeArchivedAddresses.get(idx);
        } else {
            String address = HDIndexToArchivedChangeAddress.get(idx);
            if (address == null) {
                try {
                    String extendedPublicKey = accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PUBLIC_KEY);
                    ArrayList<Integer> changeAddressSequence = new ArrayList<Integer>(Arrays.asList((Integer)TLWalletJSONKeys.TLAddressType.Change.getValue(), idx));
                    address = TLHDWalletWrapper.getAddress(extendedPublicKey, changeAddressSequence, this.appWallet.walletConfig.isTestnet);
                    HDIndexToArchivedChangeAddress.put(idx, address);

                    address2HDIndexDict.put(address, idx);
                    address2IsMainAddress.put(address, false);
                } catch (JSONException e) {
                }
            }
            return address;
        }
    }

    int recoverAccountMainAddresses(boolean shouldResetAccountBalance) {
        int lookAheadOffset = 0;
        boolean continueLookingAheadAddress = true;
        Log.d("TLAccountObject", "recoverAccountMainAddresses: getAccountID: " + getAccountID());
        int accountAddressIdx = -1;

        while (continueLookingAheadAddress) {
            ArrayList<String> addresses = new ArrayList<String>(GAP_LIMIT);

            Map<String, Integer> addressToIdxDict = new HashMap<String, Integer>(GAP_LIMIT);

            for (int i = lookAheadOffset; i < lookAheadOffset + GAP_LIMIT; i++) {
                String address = getNewMainAddress(i);
//                Log.d("TLAccountObject", "getNewMainAddress HDIdx: "+ i +" address: %@" + address);
                addresses.add(address);
                addressToIdxDict.put(address, i);
            }

            try {
                JSONObject jsonData = this.appDelegate.blockExplorerAPI.getAddressesInfo(addresses);
                if (jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                    Log.d("TLAccountObject", "getAddressesInfo failed " + jsonData.getString(TLNetworking.HTTP_ERROR_MSG));
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
                JSONArray addressesArray = jsonData.getJSONArray("addresses");
                long balance = 0;
                for (int i = 0; i < addressesArray.length(); i++) {
                    JSONObject addressDict = addressesArray.getJSONObject(i);
                    int n_tx = addressDict.getInt("n_tx");
                    String address = addressDict.getString("address");
                    address2NumberOfTransactions.put(address, n_tx);
                    long addressBalance = addressDict.getLong("final_balance");
                    balance += addressBalance;
                    address2BalanceDict.put(address, new TLCoin(addressBalance));

                    int HDIdx = addressToIdxDict.get(address);
                    if (n_tx > 0 && HDIdx > accountAddressIdx) {
                        accountAddressIdx = HDIdx;
                    }
                }
                this.accountBalance = new TLCoin(this.accountBalance.toNumber() + balance);
                if (accountAddressIdx < lookAheadOffset) {
                    continueLookingAheadAddress = false;
                }

                lookAheadOffset += GAP_LIMIT;
            } catch (JSONException e) {
                Log.d("TLAccountObject", "recoverAccountMainAddresses failed " + e.getLocalizedMessage());
            } catch (Exception e) {
                Log.d("TLAccountObject", "recoverAccountMainAddresses failed 2 " + e.getLocalizedMessage());
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }

        while (getMainAddressesCount() > accountAddressIdx + 1) {
            removeTopMainAddress();
        }

        while (getMainAddressesCount() < accountAddressIdx + 1 + ACCOUNT_UNUSED_ACTIVE_MAIN_ADDRESS_AHEAD_OF_LATEST_USED_ONE_MINIMUM_COUNT) {
            getNewMainAddress(getMainAddressesCount());
        }

        return accountAddressIdx;
    }

    int recoverAccountChangeAddresses(boolean shouldResetAccountBalance) {
        int lookAheadOffset = 0;
        boolean continueLookingAheadAddress = true;
        Log.d("TLAccountObject", "recoverAccountChangeAddresses: getAccountID: " + getAccountID());
        int accountAddressIdx = -1;

        while (continueLookingAheadAddress) {
            ArrayList<String> addresses = new ArrayList<String>(GAP_LIMIT);

            Map<String, Integer> addressToIdxDict = new HashMap<String, Integer>(GAP_LIMIT);

            for (int i = lookAheadOffset; i < lookAheadOffset + GAP_LIMIT; i++) {
                String address = getNewChangeAddress(i);
                addresses.add(address);
                addressToIdxDict.put(address, i);
            }

            try {
                JSONObject jsonData = this.appDelegate.blockExplorerAPI.getAddressesInfo(addresses);
                if (jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                    Log.d("TLAccountObject", "getAddressesInfo failed " + jsonData.getString(TLNetworking.HTTP_ERROR_MSG));
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
                JSONArray addressesArray = jsonData.getJSONArray("addresses");
                long balance = 0;
                for (int i = 0; i < addressesArray.length(); i++) {
                    JSONObject addressDict = addressesArray.getJSONObject(i);
                    int n_tx = addressDict.getInt("n_tx");
                    String address = addressDict.getString("address");
                    address2NumberOfTransactions.put(address, n_tx);
                    long addressBalance = addressDict.getLong("final_balance");
                    balance += addressBalance;
                    address2BalanceDict.put(address, new TLCoin(addressBalance));


                    int HDIdx = addressToIdxDict.get(address);
                    if (n_tx > 0 && HDIdx > accountAddressIdx) {
                        accountAddressIdx = HDIdx;
                    }
                }
                this.accountBalance = new TLCoin(this.accountBalance.toNumber() + balance);
                if (accountAddressIdx < lookAheadOffset) {
                    continueLookingAheadAddress = false;
                }

                lookAheadOffset += GAP_LIMIT;
            } catch (JSONException e) {
                Log.d("TLAccountObject", "recoverAccountChangeAddresses failed 1 " + e.getLocalizedMessage());
            } catch (Exception e) {
                Log.d("TLAccountObject", "recoverAccountChangeAddresses failed 2 " + e.getLocalizedMessage());
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }

        while (getChangeAddressesCount() > accountAddressIdx + 1) {
            removeTopChangeAddress();
        }

        while (getChangeAddressesCount() < accountAddressIdx + 1 + ACCOUNT_UNUSED_ACTIVE_CHANGE_ADDRESS_AHEAD_OF_LATEST_USED_ONE_MINIMUM_COUNT) {
            getNewChangeAddress(getChangeAddressesCount());
        }

        return accountAddressIdx;
    }

    public int recoverAccount(boolean shouldResetAccountBalance) {
        return recoverAccount(shouldResetAccountBalance, false);
    }

    public int recoverAccount(boolean shouldResetAccountBalance, boolean recoverStealthPayments) {
        int accountMainAddressMaxIdx = recoverAccountMainAddresses(shouldResetAccountBalance);
        int accountChangeAddressMaxIdx = recoverAccountChangeAddresses(shouldResetAccountBalance);
        checkToArchiveAddresses();
        updateReceivingAddresses();
        updateChangeAddresses();
        if (recoverStealthPayments && this.stealthWallet != null) {
            this.fetchNewStealthPaymentsAsync(recoverStealthPayments);
        }
        updateAccountNeedsRecovering(false);
        return accountMainAddressMaxIdx + accountChangeAddressMaxIdx;
    }

    public void recoverAccount(boolean shouldResetAccountBalance, boolean recoverStealthPayments, TLCallback callback) {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Integer sum = (Integer) msg.obj;
                callback.onSuccess(sum);
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message message = Message.obtain();
                message.obj = recoverAccount(shouldResetAccountBalance, recoverStealthPayments);
                fetchedAccountData = true;
                handler.sendMessage(Message.obtain(message));
            }
        }).start();
    }

    public void updateAccountNeedsRecovering(boolean needsRecovering) {
        try {
            if (accountType == TLAccountType.HDWallet) {
                int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);
                this.appWallet.updateAccountNeedsRecoveringFromHDWallet(accountIdx, needsRecovering);
            } else if (accountType == TLAccountType.ColdWallet) {
                this.appWallet.updateAccountNeedsRecoveringFromColdWalletAccount(getPositionInWalletArray(), needsRecovering);
            } else if (accountType == TLAccountType.Imported) {
                this.appWallet.updateAccountNeedsRecoveringFromImportedAccount(getPositionInWalletArray(), needsRecovering);
            } else {
                this.appWallet.updateAccountNeedsRecoveringFromImportedWatchAccount(getPositionInWalletArray(), needsRecovering);
            }
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_NEEDS_RECOVERING, needsRecovering);
        } catch (JSONException e) {
        }
    }

    void clearAllAddresses() {

        mainActiveAddresses = new ArrayList<String>();
        mainArchivedAddresses = new ArrayList<String>();
        changeActiveAddresses = new ArrayList<String>();
        changeArchivedAddresses = new ArrayList<String>();

        txidToAccountAmountDict = new HashMap<String, TLCoin>();
        txidToAccountAmountTypeDict = new HashMap<String, TLAccountTxType>();
        address2HDIndexDict = new HashMap<String, Integer>();
        address2BalanceDict = new HashMap<String, TLCoin>();
        address2NumberOfTransactions = new HashMap<String, Integer>();
        activeAddressesDict = new HashSet<String>();

        try {
            if (accountType == TLAccountType.HDWallet) {
                int accountIdx = accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX);
                this.appWallet.clearAllAddressesFromHDWallet(accountIdx);
                this.appWallet.clearAllStealthPaymentsFromHDWallet(accountIdx);
            } else if (accountType == TLAccountType.ColdWallet) {
                this.appWallet.clearAllAddressesFromColdWalletAccount(getPositionInWalletArray());
            } else if (accountType == TLAccountType.Imported) {
                this.appWallet.clearAllAddressesFromImportedAccount(getPositionInWalletArray());
                this.appWallet.clearAllStealthPaymentsFromImportedAccount(getPositionInWalletArray());
            } else {
                this.appWallet.clearAllAddressesFromImportedWatchAccount(getPositionInWalletArray());
            }
        } catch (JSONException e) {
        }
    }

    boolean needsRecovering() {
        try {
            boolean needsRecovering = accountDict.getBoolean(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_NEEDS_RECOVERING);
            return needsRecovering;
        } catch (JSONException e) {
            return false;
        }
    }

    JSONArray getUnspentArray() {
        return unspentOutputs;
    }

    JSONArray getStealthPaymentUnspentOutputsArray() {
        return stealthPaymentUnspentOutputs;
    }

    TLCoin getTotalUnspentSum() {
        if (totalUnspentOutputsSum != null) {
            return totalUnspentOutputsSum;
        }

        if (unspentOutputs == null) {
            return TLCoin.zero();
        }

        long totalUnspentOutputsSumTemp = 0;

        for (int i = 0; i < stealthPaymentUnspentOutputs.length(); i++) {
            try {
                JSONObject unspentOutput = stealthPaymentUnspentOutputs.getJSONObject(i);
                long amount = unspentOutput.getLong("value");
                totalUnspentOutputsSumTemp += amount;
            } catch (JSONException e) {
            }
        }

        for (int i = 0; i < unspentOutputs.length(); i++) {
            try {
                JSONObject unspentOutput = unspentOutputs.getJSONObject(i);
                long amount = unspentOutput.getLong("value");
                totalUnspentOutputsSumTemp += amount;
            } catch (JSONException e) {
            }
        }

        totalUnspentOutputsSum = new TLCoin(totalUnspentOutputsSumTemp);
        return totalUnspentOutputsSum;
    }

    public int getInputsNeededToConsume(TLCoin amountNeeded) {
        long valueSelected = 0;
        int inputCount = 0;
        for (int i = 0; i < stealthPaymentUnspentOutputs.length(); i++) {
            try {
                JSONObject unspentOutput = unspentOutputs.getJSONObject(i);
                long amount = unspentOutput.getLong("value");
                valueSelected += amount;
                inputCount++;
                if (valueSelected >= amountNeeded.toNumber() && inputCount >= MAX_CONSOLIDATE_STEALTH_PAYMENT_UTXOS_COUNT) {
                    break;
                }
            } catch (JSONException e) {
            }
        }

        if (valueSelected >= amountNeeded.toNumber()) {
            return inputCount;
        }

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

    void getUnspentOutputs(TLCallback callback) {
        this.getUnspentOutputs(this, callback);
    }
    private void getUnspentOutputs(TLAccountObject self, TLCallback callback) {
        List<String> activeAddresses = new ArrayList<String>(getActiveMainAddresses());
        activeAddresses.addAll(getActiveChangeAddresses());

        if (this.stealthWallet != null) {
            activeAddresses.addAll(this.stealthWallet.getUnspentPaymentAddresses());
        }
        unspentOutputs = null;
        totalUnspentOutputsSum = null;
        stealthPaymentUnspentOutputs = null;
        unspentOutputsCount = 0;
        stealthPaymentUnspentOutputsCount = 0;
        haveUpDatedUTXOs = false;
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                JSONObject jsonData = (JSONObject) msg.obj;
                if (jsonData == null || jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                    try {
                        callback.onFail(jsonData.getInt(TLNetworking.HTTP_ERROR_CODE), jsonData.getString(TLNetworking.HTTP_ERROR_MSG));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                try {
                    JSONArray unspentOutputs = jsonData.getJSONArray("unspent_outputs");
                    List<JSONObject> sortedUnspentOutputs = new ArrayList<JSONObject>();
                    List<JSONObject> sortedStealthPaymentUnspentOutputs = new ArrayList<JSONObject>();

                    for (int i = 0; i < unspentOutputs.length(); i++) {
                        JSONObject unspentOutput = unspentOutputs.getJSONObject(i);
                        String outputScript = unspentOutput.getString("script");
                        String address = TLBitcoinjWrapper.getAddressFromOutputScript(outputScript, self.appWallet.walletConfig.isTestnet);
                        if (address == null) {
                            Log.d("TLAccountObject", "address cannot be decoded. not normal pubkeyhash outputScript: " + outputScript);
                            continue;
                        }
                        if (self.stealthWallet != null && self.stealthWallet.isPaymentAddress(address) == true) {
                            sortedStealthPaymentUnspentOutputs.add(unspentOutput);
                            stealthPaymentUnspentOutputsCount++;
                        } else {
                            sortedUnspentOutputs.add(unspentOutput);
                            unspentOutputsCount++;
                        }
                    }

                    Collections.sort(sortedUnspentOutputs, (a, b) -> {
                        long first;
                        try {
                            first = a.getLong("confirmations");
                        } catch (JSONException e) {
                            first = 0;
                        }
                        long second;
                        try {
                            second = b.getLong("confirmations");
                        } catch (JSONException e) {
                            second = 0;
                        }
                        if (first > second) {
                            return 1;
                        } else if (first < second) {
                            return -1;
                        } else {
                            return 0;
                        }
                    });
                    self.unspentOutputs = new JSONArray();
                    for (int i = 0; i < sortedUnspentOutputs.size(); i++) {
                        self.unspentOutputs.put(sortedUnspentOutputs.get(i));
                    }
                    Collections.sort(sortedStealthPaymentUnspentOutputs, (a, b) -> {
                        long first;
                        try {
                            first = a.getLong("confirmations");
                        } catch (JSONException e) {
                            first = 0;
                        }
                        long second;
                        try {
                            second = b.getLong("confirmations");
                        } catch (JSONException e) {
                            second = 0;
                        }
                        if (first > second) {
                            return 1;
                        } else if (first < second) {
                            return -1;
                        } else {
                            return 0;
                        }
                    });
                    self.stealthPaymentUnspentOutputs = new JSONArray();
                    for (int i = 0; i < sortedStealthPaymentUnspentOutputs.size(); i++) {
                        self.stealthPaymentUnspentOutputs.put(sortedStealthPaymentUnspentOutputs.get(i));
                    }
                    haveUpDatedUTXOs = true;
                    callback.onSuccess(jsonData);
                } catch (JSONException e) {
                    Log.d("TLAccountObject", "getUnspentOutputs JSONException " + e.getLocalizedMessage());
                }
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject object = appDelegate.blockExplorerAPI.getUnspentOutputs(activeAddresses);
                    Message message = Message.obtain();
                    message.obj = object;
                    handler.sendMessage(Message.obtain(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void fetchNewStealthPaymentsAsync(boolean isRestoringAccount) {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Boolean success = (Boolean) msg.obj;
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    fetchNewStealthPayments(isRestoringAccount);
                    Message message = Message.obtain();
                    message.obj = true;
                    handler.sendMessage(Message.obtain(message));
                } catch (Exception e) {
                    Log.d("TLAccountObject", e.getLocalizedMessage());
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.obj = false;
                    handler.sendMessage(Message.obtain(message));
                }
            }
        }).start();
    }

    void fetchNewStealthPayments(boolean isRestoringAccount) {
        this.stealthWallet.checkToWatchStealthAddress();
        int offset = 0;
        long currentLatestTxTime = 0;
        while (true) {
            GetAndStoreStealthPaymentsResults ret = this.stealthWallet.getAndStoreStealthPayments(offset);
            if (ret == null) {
                break;
            }
            long latestTxTime = ret.latestTxTime;
            if (latestTxTime > currentLatestTxTime) {
                currentLatestTxTime = latestTxTime;
            }
            boolean gotOldestPaymentAddresses = ret.gotOldestPaymentAddresses;
            List<String>  newStealthPaymentAddresses  = ret.stealthPayments;
            if (newStealthPaymentAddresses.size() > 0) {
                try {
                    JSONObject jsonData = this.appDelegate.blockExplorerAPI.getAddressesInfo(newStealthPaymentAddresses);
                    onAfterFetchAccountData(jsonData, false, null);
                } catch (Exception e) {
                    Log.d("TLAccountObject", e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
            if (gotOldestPaymentAddresses) {
                break;
            }
            offset += TLStealthExplorerAPI.STEALTH_PAYMENTS_FETCH_COUNT;
        }

        this.setStealthAddressLastTxTime(this.appDelegate.preferences.getStealthExplorerURL(), currentLatestTxTime);

        if (isRestoringAccount) {
            this.stealthWallet.setUpStealthPaymentAddresses(true, true, false);
        }
    }

    public boolean getAccountData(List<String> addresses, boolean shouldResetAccountBalance) {
        return this.getAccountData(addresses, shouldResetAccountBalance, this);
    }

    private boolean getAccountData(List<String> addresses, boolean shouldResetAccountBalance, TLAccountObject self) {
        try {
            JSONObject jsonData = this.appDelegate.blockExplorerAPI.getAddressesInfo(addresses);
            if (shouldResetAccountBalance) {
                self.resetAccountBalances();
            }

            try {
                JSONArray addressesArray = jsonData.getJSONArray("addresses");
                long balance = 0;
                for (int i = 0; i < addressesArray.length(); i++) {
                    JSONObject addressDict = addressesArray.getJSONObject(i);
                    int n_tx = addressDict.getInt("n_tx");
                    String address = addressDict.getString("address");
                    address2NumberOfTransactions.put(address, n_tx);
                    long addressBalance = addressDict.getLong("final_balance");
                    balance += addressBalance;
                    address2BalanceDict.put(address, new TLCoin(addressBalance));
                }
                self.accountBalance = new TLCoin(self.accountBalance.toNumber() + balance);
                self.processTxArray(jsonData.getJSONArray("txs"), true);

                self.fetchedAccountData = true;
                self.subscribeToWebsockets();
                self.downloadState = TLDownloadState.Downloaded;
                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA));
                return true;
            } catch (JSONException e) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void resetAccountBalances() {
        txObjectArray = new ArrayList<TLTxObject>();
        address2BalanceDict = new HashMap<String, TLCoin>();
        address2NumberOfTransactions = new HashMap<String, Integer>();

        accountBalance = TLCoin.zero();

        for (String address : mainActiveAddresses) {
            address2BalanceDict.put(address, TLCoin.zero());
            address2NumberOfTransactions.put(address, 0);
        }
        for (String address : changeActiveAddresses) {
            address2BalanceDict.put(address, TLCoin.zero());
            address2NumberOfTransactions.put(address, 0);
        }
        if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
            for (String address : mainArchivedAddresses) {
                address2BalanceDict.put(address, TLCoin.zero());
                address2NumberOfTransactions.put(address, 0);
            }
            for (String address : changeArchivedAddresses) {
                address2BalanceDict.put(address, TLCoin.zero());
                address2NumberOfTransactions.put(address, 0);
            }
        }
    }

    void setStealthAddressServerStatus(String serverURL, boolean isWatching) {
        if (this.accountType == TLAccountType.HDWallet) {
            int accountIdx = this.getAccountIdxNumber();
            this.appWallet.setStealthAddressServerStatusHDWallet(accountIdx, serverURL, isWatching);
        } else if (this.accountType == TLAccountType.ColdWallet) {
            this.appWallet.setStealthAddressServerStatusColdWalletAccount(this.getPositionInWalletArray(), serverURL, isWatching);
        } else if (this.accountType == TLAccountType.Imported) {
             this.appWallet.setStealthAddressServerStatusImportedAccount(this.getPositionInWalletArray(), serverURL, isWatching);
        } else {
             this.appWallet.setStealthAddressServerStatusImportedWatchAccount(this.getPositionInWalletArray(), serverURL, isWatching);
        }
    }

    void setStealthAddressLastTxTime(String serverURL, long lastTxTime) {
        if (this.accountType == TLAccountType.HDWallet) {
            int accountIdx = this.getAccountIdxNumber();
            this.appWallet.setStealthAddressLastTxTimeHDWallet(accountIdx, serverURL, lastTxTime);
        } else if (this.accountType == TLAccountType.ColdWallet) {
            this.appWallet.setStealthAddressLastTxTimeColdWalletAccount(this.getPositionInWalletArray(), serverURL, lastTxTime);
        } else if (this.accountType == TLAccountType.Imported) {
            this.appWallet.setStealthAddressLastTxTimeImportedAccount(this.getPositionInWalletArray(), serverURL, lastTxTime);
        } else {
            this.appWallet.setStealthAddressLastTxTimeImportedWatchAccount(this.getPositionInWalletArray(), serverURL, lastTxTime);
        }
    }

    void addStealthAddressPaymentKey(String privateKey, String address, String txid, long txTime, TLStealthPaymentStatus stealthPaymentStatus) {
        if (accountType == TLAccountType.HDWallet) {
            int accountIdx = this.getAccountIdxNumber();
            this.appWallet.addStealthAddressPaymentKeyHDWallet(accountIdx, privateKey,
                    address, txid, txTime, stealthPaymentStatus);
        } else if (this.accountType == TLAccountType.ColdWallet) {
            this.appWallet.addStealthAddressPaymentKeyColdWalletAccount(this.getPositionInWalletArray(),
                    privateKey, address, txid, txTime, stealthPaymentStatus);
        } else if (accountType == TLAccountType.Imported) {
            this.appWallet.addStealthAddressPaymentKeyImportedAccount(this.getPositionInWalletArray(),
                    privateKey, address, txid, txTime, stealthPaymentStatus);
        } else if (accountType == TLAccountType.ImportedWatch) {
            this.appWallet.addStealthAddressPaymentKeyImportedWatchAccount(this.getPositionInWalletArray(),
                    privateKey, address, txid, txTime, stealthPaymentStatus);
        }
    }

    void setStealthPaymentStatus(String txid, TLStealthPaymentStatus stealthPaymentStatus, long lastCheckTime) {
        if (accountType == TLAccountType.HDWallet) {
            int accountIdx = this.getAccountIdxNumber();
            this.appWallet.setStealthPaymentStatusHDWallet(accountIdx, txid,
                    stealthPaymentStatus, lastCheckTime);
        } else if (this.accountType == TLAccountType.ColdWallet) {
            this.appWallet.setStealthPaymentStatusColdWalletAccount(this.getPositionInWalletArray(),
                    txid, stealthPaymentStatus, lastCheckTime);
        } else if (accountType == TLAccountType.Imported) {
            this.appWallet.setStealthPaymentStatusImportedAccount(this.getPositionInWalletArray(),
                    txid, stealthPaymentStatus, lastCheckTime);
        } else if (accountType == TLAccountType.ImportedWatch) {
            this.appWallet.setStealthPaymentStatusImportedWatchAccount(this.getPositionInWalletArray(),
                    txid, stealthPaymentStatus, lastCheckTime);
        }
    }

    void removeOldStealthPayments() {
        if (accountType == TLAccountType.HDWallet) {
            int accountIdx = this.getAccountIdxNumber();
            this.appWallet.removeOldStealthPaymentsHDWallet(accountIdx);
        } else if (this.accountType == TLAccountType.ColdWallet) {
            this.appWallet.removeOldStealthPaymentsColdWalletAccount(this.getPositionInWalletArray());
        } else if (accountType == TLAccountType.Imported) {
            this.appWallet.removeOldStealthPaymentsImportedAccount(this.getPositionInWalletArray());
        } else if (accountType == TLAccountType.ImportedWatch) {
            this.appWallet.removeOldStealthPaymentsImportedWatchAccount(this.getPositionInWalletArray());
        }
    }

    void setStealthPaymentLastCheckTime(String txid, long lastCheckTime) {
        if (accountType == TLAccountType.HDWallet) {
            int accountIdx = this.getAccountIdxNumber();
             this.appWallet.setStealthPaymentLastCheckTimeHDWallet(accountIdx, txid, lastCheckTime);
        } else if (this.accountType == TLAccountType.ColdWallet) {
            this.appWallet.setStealthPaymentLastCheckTimeColdWalletAccount(this.getPositionInWalletArray(), txid, lastCheckTime);
        } else if (accountType == TLAccountType.Imported) {
             this.appWallet.setStealthPaymentLastCheckTimeImportedAccount(this.getPositionInWalletArray(), txid, lastCheckTime);
        } else if (accountType == TLAccountType.ImportedWatch) {
             this.appWallet.setStealthPaymentLastCheckTimeImportedWatchAccount(this.getPositionInWalletArray(), txid, lastCheckTime);
        }
    }

    public void getAccountDataO(boolean fetchDataAgain, TLCallback callback) {
        if (this.downloadState == TLDownloadState.QueuedForDownloading || this.downloadState == TLDownloadState.Downloading
                || (!fetchDataAgain && this.downloadState == TLDownloadState.Downloaded))  {
            if (callback != null) {
                callback.onFail(-1000, "Local failed");
            }
            return;
        }
        // if account needs recovering dont fetch account data
        if (needsRecovering()) {
            this.downloadState = TLDownloadState.Failed;
            if (callback != null) {
                callback.onFail(-1000, "Local failed");
            }
            return;
        }

        ArrayList<String> activeAddresses = new ArrayList<String>(getActiveMainAddresses());
        activeAddresses.addAll(getActiveChangeAddresses());

        if (this.stealthWallet != null) {
            activeAddresses.addAll(this.stealthWallet.getPaymentAddresses());
        }

        this.getAccountDataO(activeAddresses, true, callback);

        if (this.stealthWallet != null) {
            this.fetchNewStealthPaymentsAsync(false);
        }
    }

    public void getAccountDataO(TLCallback callback) {
        this.getAccountDataO(false, callback);
    }

    public void getAccountDataO() {
        this.getAccountDataO(null);

    }

    private void getAccountDataO(List<String> addresses, boolean shouldResetAccountBalance, TLCallback callback) {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                JSONObject jsonData = (JSONObject) msg.obj;
                onAfterFetchAccountData(jsonData, shouldResetAccountBalance, callback);
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonData = appDelegate.blockExplorerAPI.getAddressesInfo(addresses);
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

    private void onAfterFetchAccountData(JSONObject jsonData, boolean shouldResetAccountBalance, TLCallback callback) {
        if (jsonData == null || jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
            this.downloadState = TLDownloadState.Failed;
            return;
        }

        if (shouldResetAccountBalance) {
            this.resetAccountBalances();
        }

        try {
            JSONArray addressesArray = jsonData.getJSONArray("addresses");
            long balance = 0;
            for (int i = 0; i < addressesArray.length(); i++) {
                JSONObject addressDict = addressesArray.getJSONObject(i);
                int n_tx = addressDict.getInt("n_tx");
                String address = addressDict.getString("address");
                address2NumberOfTransactions.put(address, n_tx);
                long addressBalance = addressDict.getLong("final_balance");
                balance += addressBalance;
                address2BalanceDict.put(address, new TLCoin(addressBalance));
            }
            this.accountBalance = new TLCoin(this.accountBalance.toNumber() + balance);
            this.processTxArray(jsonData.getJSONArray("txs"), true);
            this.fetchedAccountData = true;
            this.subscribeToWebsockets();
            this.downloadState = TLDownloadState.Downloaded;
            Intent intent = new Intent(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA);
            LocalBroadcastManager.getInstance(this.appDelegate.context).sendBroadcast(intent);
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (callback != null) {
                callback.onFail(8000, "Impossible Failure");
            }
            Log.d("TLAccountObject", "onPostExecute accountObject" + e.getLocalizedMessage());
        }
    }

    private void subscribeToWebsockets() {
        if (this.listeningToIncomingTransactions == false) {
            this.listeningToIncomingTransactions = true;
            List<String> activeMainAddresses = this.getActiveMainAddresses();
            for (String address : activeMainAddresses) {
                    appDelegate.transactionListener.listenToIncomingTransactionForAddress(address);
            }
            List<String> activeChangeAddresses = this.getActiveChangeAddresses();
            for (String address : activeChangeAddresses) {
                appDelegate.transactionListener.listenToIncomingTransactionForAddress(address);
            }
        }
        if (this.stealthWallet != null) {
            List<String> stealthPaymentAddresses = this.stealthWallet.getUnspentPaymentAddresses();
            for (String address : stealthPaymentAddresses) {
                appDelegate.transactionListener.listenToIncomingTransactionForAddress(address);
            }

            if (this.stealthWallet.isListeningToStealthPayment == false) {
                String challenge = appDelegate.stealthWebSocket.challenge;
                Pair<String, String> addrAndSignature = this.stealthWallet.getStealthAddressAndSignatureFromChallenge(challenge);
                appDelegate.stealthWebSocket.sendMessageSubscribeToStealthAddress(addrAndSignature.first, addrAndSignature.second);
            }
        }
    }
}
