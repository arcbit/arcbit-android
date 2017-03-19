package com.arcbit.arcbit.model;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.UiThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.util.Log;

import com.arcbit.arcbit.APIs.TLBlockExplorerAPI;
import com.arcbit.arcbit.APIs.TLExchangeRate;
import com.arcbit.arcbit.APIs.TLNetworking;
import com.arcbit.arcbit.APIs.TLPushTxAPI;
import com.arcbit.arcbit.APIs.TLStealthExplorerAPI;
import com.arcbit.arcbit.APIs.TLStealthWebSocket;
import com.arcbit.arcbit.APIs.TLTransactionListener;
import com.arcbit.arcbit.APIs.TLTxFeeAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import com.arcbit.arcbit.model.TLWalletUtils.TLAccountAddressType;
import com.arcbit.arcbit.model.TLWalletUtils.TLAccountType;
import com.arcbit.arcbit.model.TLWalletUtils.TLSendFromType;
import com.arcbit.arcbit.model.TLWalletJSONKeys.TLAccount;
import com.arcbit.arcbit.model.TLOperationsManager.TLDownloadState;
import com.arcbit.arcbit.APIs.TLBlockExplorerAPI.TLBlockExplorer;
import com.arcbit.arcbit.model.TLWalletJSONKeys.TLStealthPaymentStatus;

public class TLAppDelegate {
    private static final String TAG = TLAppDelegate.class.getName();

    static final private int MAX_CONSECUTIVE_FAILED_STEALTH_CHALLENGE_COUNT = 8;
    static final private int SAVE_WALLET_PAYLOAD_DELAY = 2;
    static final private TLBlockExplorer DEFAULT_BLOCKEXPLORER_API = TLBlockExplorer.Blockchain;
    //static final private TLBlockExplorer DEFAULT_BLOCKEXPLORER_API = TLBlockExplorer.Insight;
    static final private int  RESPOND_TO_STEALTH_PAYMENT_GET_TX_TRIES_MAX_TRIES = 3;

    private static TLAppDelegate instance = null;
    public Context context = null;

    public TLPreferences preferences;
    public TLBlockExplorerAPI blockExplorerAPI;
    public TLCurrencyFormat currencyFormat;
    public TLStealthExplorerAPI stealthExplorerAPI;
    public TLBlockchainStatus blockchainStatus;
    public TLStealthServerConfig stealthServerConfig;
    public TLAnalytics analytics;
    public TLSuggestions suggestions;
    public TLPushTxAPI pushTxAPI;

    public TLWallet appWallet;
    public TLAccounts accounts;
    public TLAccounts importedAccounts;
    public TLAccounts coldWalletAccounts;
    public TLAccounts importedWatchAccounts;
    public TLImportedAddresses importedAddresses;
    public TLImportedAddresses importedWatchAddresses;
    public TLSpaghettiGodSend godSend;
    public TLSelectedObject receiveSelectedObject;
    public TLSelectedObject historySelectedObject;
    public TLSendFormData sendFormData;
    public TLExchangeRate exchangeRate;
    public TLTransactionListener transactionListener;
    public TLStealthWebSocket stealthWebSocket;
    public TLWalletJson walletJson;
    public TLTxFeeAPI txFeeAPI;
    public TLKeyStore keyStore;
    public TLEncryptedPreferences encryptedPreferences;

    public Set webSocketNotifiedTxHashSet = new HashSet();
    public String pendingSelfStealthPaymentTxid = null;
    public Map<String, Bitmap> address2BitmapMap = new HashMap<String, Bitmap>();
    public TLAccountObject viewAddressesAccountObject;
    public boolean justSetupHDWallet = false;
    public boolean saveWalletJSONEnabled = true;

    private boolean isAccountsAndImportsLoaded = false;
    private int consecutiveFailedStealthChallengeCount = 0;
    private int respondToStealthPaymentGetTxTries = 0;
    private Map<String, String> sentPaymentHashes = new HashMap<String, String>();
    private final Handler saveWalletHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveWalletRunnable = new Runnable() {
        @Override
        public void run() {
            saveWalletJson();
        }
    };

    private TLAppDelegate(Context ctx) {
        context = ctx;
    }

    public static TLAppDelegate instance() {
        return instance;
    }

    public static TLAppDelegate instance(Context ctx) {
        if (instance == null) {
            instance = new TLAppDelegate(ctx);
        }
        return instance;
    }

    public void initAppDelegate() {
        preferences = new TLPreferences(this);
    }

    public void updateGodSend() {
        TLSendFromType sendFromType = this.preferences.getSendFromType();
        int sendFromIndex = this.preferences.getSendFromIndex();

        if (sendFromType == TLSendFromType.HDWallet) {
            if (sendFromIndex > this.accounts.getNumberOfAccounts() - 1 ) {
                sendFromType = TLSendFromType.HDWallet;
                sendFromIndex = 0;
            }
        } else if (sendFromType == TLSendFromType.ColdWalletAccount) {
            if (sendFromIndex > this.coldWalletAccounts.getNumberOfAccounts() - 1) {
                sendFromType = TLSendFromType.HDWallet;
                sendFromIndex = 0;
            }
        } else if (sendFromType == TLSendFromType.ImportedAccount) {
            if (sendFromIndex > this.importedAccounts.getNumberOfAccounts() - 1) {
                sendFromType = TLSendFromType.HDWallet;
                sendFromIndex = 0;
            }
        } else if (sendFromType == TLSendFromType.ImportedWatchAccount) {
            if (sendFromIndex > this.importedWatchAccounts.getNumberOfAccounts() - 1) {
                sendFromType = TLSendFromType.HDWallet;
                sendFromIndex = 0;
            }
        } else if (sendFromType == TLSendFromType.ImportedAddress) {
            if (sendFromIndex > this.importedAddresses.getCount() - 1) {
                sendFromType = TLSendFromType.HDWallet;
                sendFromIndex = 0;
            }
        } else if (sendFromType == TLSendFromType.ImportedWatchAddress) {
            if (sendFromIndex > this.importedWatchAddresses.getCount() - 1) {
                sendFromType = TLSendFromType.HDWallet;
                sendFromIndex = 0;
            }
        }

        this.updateGodSend(sendFromType, sendFromIndex);
    }

    public void updateGodSend(TLSendFromType sendFromType, int sendFromIndex) {
        this.preferences.setSendFromType(sendFromType);
        this.preferences.setSendFromIndex(sendFromIndex);
        if (this.godSend == null) {
            return;
        }
        if (sendFromType == TLSendFromType.HDWallet) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(sendFromIndex);
            this.godSend.setOnlyFromAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ColdWalletAccount) {
            TLAccountObject accountObject = this.coldWalletAccounts.getAccountObjectForIdx(sendFromIndex);
            this.godSend.setOnlyFromAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ImportedAccount) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(sendFromIndex);
            this.godSend.setOnlyFromAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ImportedWatchAccount) {
            TLAccountObject accountObject = this.importedWatchAccounts.getAccountObjectForIdx(sendFromIndex);
            this.godSend.setOnlyFromAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ImportedAddress) {
            TLImportedAddress importedAddress = this.importedAddresses.getAddressObjectAtIdx(sendFromIndex);
            this.godSend.setOnlyFromAddress(importedAddress);
        } else if (sendFromType == TLSendFromType.ImportedWatchAddress) {
            TLImportedAddress importedAddress = this.importedWatchAddresses.getAddressObjectAtIdx(sendFromIndex);
            this.godSend.setOnlyFromAddress(importedAddress);
        }
    }

    public void updateReceiveSelectedObject(TLSendFromType sendFromType, int sendFromIndex) {
        if (sendFromType == TLSendFromType.HDWallet) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(sendFromIndex);
            this.receiveSelectedObject.setSelectedAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ColdWalletAccount) {
            TLAccountObject accountObject = this.coldWalletAccounts.getAccountObjectForIdx(sendFromIndex);
            this.receiveSelectedObject.setSelectedAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ImportedAccount) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(sendFromIndex);
            this.receiveSelectedObject.setSelectedAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ImportedWatchAccount) {
            TLAccountObject accountObject = this.importedWatchAccounts.getAccountObjectForIdx(sendFromIndex);
            this.receiveSelectedObject.setSelectedAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ImportedAddress) {
            TLImportedAddress importedAddress = this.importedAddresses.getAddressObjectAtIdx(sendFromIndex);
            this.receiveSelectedObject.setSelectedAddress(importedAddress);
        } else if (sendFromType == TLSendFromType.ImportedWatchAddress) {
            TLImportedAddress importedAddress = this.importedWatchAddresses.getAddressObjectAtIdx(sendFromIndex);
            this.receiveSelectedObject.setSelectedAddress(importedAddress);
        }
    }

    public void updateHistorySelectedObject(TLSendFromType sendFromType, int sendFromIndex) {
        if (sendFromType == TLSendFromType.HDWallet) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(sendFromIndex);
            this.historySelectedObject.setSelectedAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ColdWalletAccount) {
            TLAccountObject accountObject = this.coldWalletAccounts.getAccountObjectForIdx(sendFromIndex);
            this.historySelectedObject.setSelectedAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ImportedAccount) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(sendFromIndex);
            this.historySelectedObject.setSelectedAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ImportedWatchAccount) {
            TLAccountObject accountObject = this.importedWatchAccounts.getAccountObjectForIdx(sendFromIndex);
            this.historySelectedObject.setSelectedAccount(accountObject);
        } else if (sendFromType == TLSendFromType.ImportedAddress) {
            TLImportedAddress importedAddress = this.importedAddresses.getAddressObjectAtIdx(sendFromIndex);
            this.historySelectedObject.setSelectedAddress(importedAddress);
        } else if (sendFromType == TLSendFromType.ImportedWatchAddress) {
            TLImportedAddress importedAddress = this.importedWatchAddresses.getAddressObjectAtIdx(sendFromIndex);
            this.historySelectedObject.setSelectedAddress(importedAddress);
        }
    }

    public void recoverHDWallet(String mnemonic) {
        this.recoverHDWallet(mnemonic, true);
    }
    public void recoverHDWallet(String mnemonic, boolean shouldRefreshApp) {
        if (shouldRefreshApp) {
            this.refreshApp(mnemonic);
        } else {
            String masterHex = TLHDWalletWrapper.getMasterHex(mnemonic);
            this.appWallet.createInitialWalletPayload(mnemonic, masterHex);

            this.accounts = new TLAccounts(this, this.appWallet, this.appWallet.getAccountObjectArray(), TLAccountType.HDWallet);
            this.coldWalletAccounts = new TLAccounts(this, this.appWallet, this.appWallet.getColdWalletAccountArray(), TLAccountType.ColdWallet);
            this.importedWatchAccounts = new TLAccounts(this, this.appWallet, this.appWallet.getWatchOnlyAccountArray(), TLAccountType.ImportedWatch);
            this.importedAccounts = new TLAccounts(this, this.appWallet, this.appWallet.getImportedAccountArray(), TLAccountType.Imported);
            this.importedAddresses = new TLImportedAddresses(this, this.appWallet, this.appWallet.getImportedPrivateKeyArray(), TLAccountAddressType.Imported);
            this.importedWatchAddresses = new TLImportedAddresses(this, this.appWallet, this.appWallet.getWatchOnlyAddressArray(), TLAccountAddressType.ImportedWatch);
        }

        int accountIdx = 0;
        int consecutiveUnusedAccountCount = 0;
        final int MAX_CONSECUTIVE_UNUSED_ACCOUNT_LOOK_AHEAD_COUNT = 4;

        while (true) {
            String accountName = "Account " + (accountIdx + 1);
            TLAccountObject accountObject = this.accounts.createNewAccount(accountName, TLAccount.Normal, false);

            Log.d(TAG, "recoverHDWalletaccountName " + accountName);

            int sumMainAndChangeAddressMaxIdx = accountObject.recoverAccount(false);
            Log.d(TAG, String.format("accountName %s sumMainAndChangeAddressMaxIdx: %d", accountName, sumMainAndChangeAddressMaxIdx));
            if (sumMainAndChangeAddressMaxIdx > -2 || accountObject.stealthWallet.checkIfHaveStealthPayments()) {
                consecutiveUnusedAccountCount = 0;
            } else {
                consecutiveUnusedAccountCount++;
                if (consecutiveUnusedAccountCount == MAX_CONSECUTIVE_UNUSED_ACCOUNT_LOOK_AHEAD_COUNT) {
                    break;
                }
            }

            accountIdx += 1;
        }

        Log.d(TAG, "recoverHDWallet getNumberOfAccounts: " + this.accounts.getNumberOfAccounts());
        if (this.accounts.getNumberOfAccounts() == 0) {
            this.accounts.createNewAccount("Account 1", TLAccount.Normal);
        } else if (this.accounts.getNumberOfAccounts() > 1) {
            while (this.accounts.getNumberOfAccounts() > 1 && consecutiveUnusedAccountCount > 0) {
                this.accounts.popTopAccount();
                consecutiveUnusedAccountCount--;
            }
        }
    }

    public void refreshApp(String passphrase) {
        this.refreshApp(passphrase, true);
    }

    public void refreshApp(String passphrase, boolean clearWalletInMemory) {
        this.encryptedPreferences.setWalletPassphrase(null);
        this.encryptedPreferences.setWalletJSONPassphrase(null);

        this.encryptedPreferences.setWalletPassphrase(passphrase);
        this.encryptedPreferences.setWalletJSONPassphrase(passphrase);
        this.preferences.clearEncryptedWalletPassphraseKey();

        this.preferences.setEnabledDynamicFee(false);
        this.preferences.setTransactionFee(TLTransactionFee.DEFAULT_FIXED_FEE_AMOUNT);
        this.preferences.setEnablePINCode(false);
        this.encryptedPreferences.setPINValue(null);
        this.preferences.resetBlockExplorerAPIURL();
        this.preferences.setEnableColdWallet(false);

        this.preferences.setBlockExplorerAPI(DEFAULT_BLOCKEXPLORER_API);

        this.preferences.resetStealthExplorerAPIURL();
        this.preferences.resetStealthServerPort();
        this.preferences.resetStealthWebSocketPort();

        int DEFAULT_CURRENCY_IDX = 20;
        this.preferences.setCurrency(DEFAULT_CURRENCY_IDX);

        this.preferences.setSendFromType(TLSendFromType.HDWallet);
        this.preferences.setSendFromIndex(0);
        //this.preferences.setAdvancedMode(true);

        this.preferences.setHasReceivePaymentForFirstTime(false);
        this.preferences.sethasShownBackupPassphrase(false);

        if (clearWalletInMemory) {
            String masterHex = TLHDWalletWrapper.getMasterHex(passphrase);
            this.appWallet.createInitialWalletPayload(passphrase, masterHex);

            this.accounts = new TLAccounts(this, this.appWallet, this.appWallet.getAccountObjectArray(), TLAccountType.HDWallet);
            this.coldWalletAccounts = new TLAccounts(this, this.appWallet, this.appWallet.getColdWalletAccountArray(), TLAccountType.ColdWallet);
            this.importedWatchAccounts = new TLAccounts(this, this.appWallet, this.appWallet.getWatchOnlyAccountArray(), TLAccountType.ImportedWatch);
            this.importedAccounts = new TLAccounts(this, this.appWallet, this.appWallet.getImportedAccountArray(), TLAccountType.Imported);
            this.importedAddresses = new TLImportedAddresses(this, this.appWallet, this.appWallet.getImportedPrivateKeyArray(), TLAccountAddressType.Imported);
            this.importedWatchAddresses = new TLImportedAddresses(this, this.appWallet, this.appWallet.getWatchOnlyAddressArray(), TLAccountAddressType.ImportedWatch);
        }

        this.receiveSelectedObject = new TLSelectedObject();
        this.historySelectedObject = new TLSelectedObject();
    }

    public void setAccountsListeningToStealthPaymentsToFalse() {
        for (int i = 0; i < this.accounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(i);
            if (accountObject.stealthWallet != null) {
                accountObject.stealthWallet.isListeningToStealthPayment = false;
            }
        }

        for (int i = 0; i < this.importedAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(i);
            if (accountObject.stealthWallet != null) {
                accountObject.stealthWallet.isListeningToStealthPayment = false;
            }
        }
    }

    public void respondToStealthChallegeNotification(JSONObject jsonObject) {
        try {
            String challenge = jsonObject.getString("challenge");
            this.stealthWebSocket.challenge = challenge;
            this.respondToStealthChallege(challenge);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void respondToStealthChallege(String challenge) {
        this.isAccountsAndImportsLoaded = true; //FIXME dont need this var??
        if (!this.isAccountsAndImportsLoaded || !this.stealthWebSocket.isWebSocketOpen()) {
            return;
        }

        for (int i = 0; i < this.accounts.getNumberOfAccounts(); i++) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(i);
            if (accountObject.hasFetchedAccountData() &&
                    accountObject.stealthWallet != null && accountObject.stealthWallet.isListeningToStealthPayment == false) {
                Pair<String, String> addrAndSignature = accountObject.stealthWallet.getStealthAddressAndSignatureFromChallenge(challenge);
                this.stealthWebSocket.sendMessageSubscribeToStealthAddress(addrAndSignature.first, addrAndSignature.second);
            }
        }

        for (int i = 0; i < this.importedAccounts.getNumberOfAccounts(); i++) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(i);
            if (accountObject.hasFetchedAccountData() &&
                    accountObject.stealthWallet != null && accountObject.stealthWallet.isListeningToStealthPayment == false) {
                Pair<String, String> addrAndSignature = accountObject.stealthWallet.getStealthAddressAndSignatureFromChallenge(challenge);
                this.stealthWebSocket.sendMessageSubscribeToStealthAddress(addrAndSignature.first, addrAndSignature.second);
            }
        }
    }

    public void respondToStealthAddressSubscription(JSONObject jsonObject) {
        String stealthAddress = null;
        String subscriptionSuccess = null;
        try {
            stealthAddress = jsonObject.getString("addr");
            subscriptionSuccess = jsonObject.getString("success");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (subscriptionSuccess.equals("False") && consecutiveFailedStealthChallengeCount < MAX_CONSECUTIVE_FAILED_STEALTH_CHALLENGE_COUNT) {
            consecutiveFailedStealthChallengeCount++;
            this.stealthWebSocket.sendMessageGetChallenge();
            return;
        }
        consecutiveFailedStealthChallengeCount = 0;

        for (int i = 0; i < this.accounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(i);
            if (accountObject.stealthWallet.getStealthAddress().equals(stealthAddress)) {
                accountObject.stealthWallet.isListeningToStealthPayment = true;
            }
        }

        for (int i = 0; i < this.importedAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(i);
            if (accountObject.stealthWallet.getStealthAddress().equals(stealthAddress)) {
                accountObject.stealthWallet.isListeningToStealthPayment = true;
            }
        }
    }

    private void processStealthPayment(TLAccountObject accountObject, String stealthAddress, String paymentAddress,
                                       String txid, long txTime, TLTxObject txObject, List<String> possibleStealthDataScripts,
                                       List<String> inputAddresses) {
        if (accountObject.stealthWallet.getStealthAddress().equals(stealthAddress)) {
            if (accountObject.hasFetchedAccountData()) {
                for (String stealthDataScript : possibleStealthDataScripts) {
                    String privateKey = accountObject.stealthWallet.generateAndAddStealthAddressPaymentKey(stealthDataScript, paymentAddress,
                            txid, txTime, TLStealthPaymentStatus.Unspent);
                    if (privateKey != null) {
                        this.handleNewTxForAccount(accountObject, txObject);
                        break;
                    }
                }
            }
        } else {
            // must refresh account balance if a input address belongs to account
            // this is needed because websocket api does not notify of addresses being used as inputs
            for (String address : inputAddresses) {
                if (accountObject.hasFetchedAccountData() && accountObject.isAddressPartOfAccount(address)) {
                    this.handleNewTxForAccount(accountObject, txObject);
                }
            }
        }
    }

    void handleGetTxSuccessForRespondToStealthPayment(String stealthAddress, String paymentAddress,
                                                      String txid, long txTime, TLTxObject txObject) {
        List<String> inputAddresses = txObject.getInputAddressArray();
        List<String> outputAddresses = txObject.getOutputAddressArray();

        if (outputAddresses.indexOf(paymentAddress) == -1) {
            return;
        }

        List<String> possibleStealthDataScripts = txObject.getPossibleStealthDataScripts();

        for (int i = 0; i < this.accounts.getNumberOfAccounts(); i++) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(i);
            processStealthPayment(accountObject, stealthAddress, paymentAddress,
                    txid, txTime, txObject, possibleStealthDataScripts, inputAddresses);
        }

        for (int i = 0; i < this.importedAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(i);
            processStealthPayment(accountObject, stealthAddress, paymentAddress,
                    txid, txTime, txObject, possibleStealthDataScripts, inputAddresses);
        }
        for (int i = 0; i < this.coldWalletAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.coldWalletAccounts.getAccountObjectForIdx(i);
            for (String address : inputAddresses) {
                if (accountObject.isAddressPartOfAccount(address)) {
                    this.handleNewTxForAccount(accountObject, txObject);
                }
            }
        }
        for (int i = 0; i < this.importedWatchAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedWatchAccounts.getAccountObjectForIdx(i);
            for (String address : inputAddresses) {
                if (accountObject.isAddressPartOfAccount(address)) {
                    this.handleNewTxForAccount(accountObject, txObject);
                }
            }
        }
        for (int i = 0; i < this.importedAddresses.getCount();  i++) {
            TLImportedAddress importedAddress = this.importedAddresses.getAddressObjectAtIdx(i);
            for (String addr : inputAddresses) {
                if (addr.equals(importedAddress.getAddress())) {
                    this.handleNewTxForImportedAddress(importedAddress, txObject);
                }
            }
        }
        for (int i = 0; i < this.importedWatchAddresses.getCount();  i++) {
            TLImportedAddress importedAddress = this.importedWatchAddresses.getAddressObjectAtIdx(i);
            for (String addr : inputAddresses) {
                if (addr.equals(importedAddress.getAddress())) {
                    this.handleNewTxForImportedAddress(importedAddress, txObject);
                }
            }
        }
    }

    public void respondToStealthPayment(JSONObject jsonObject) {
        try {
            String stealthAddress = jsonObject.getString("stealth_addr");
            String txid = jsonObject.getString("txid");
            String paymentAddress = jsonObject.getString("addr");
            long txTime = jsonObject.getLong("time");
            respondToStealthPayment(stealthAddress, txid, paymentAddress, txTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void respondToStealthPayment(String stealthAddress, String txid, String paymentAddress, long txTime) {
        if (this.respondToStealthPaymentGetTxTries < this.RESPOND_TO_STEALTH_PAYMENT_GET_TX_TRIES_MAX_TRIES) {
            TLAppDelegate self = this;
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    JSONObject jsonObject = (JSONObject) msg.obj;
                    if (jsonObject == null) {
                        return;
                    }
                    if (jsonObject.has(TLNetworking.HTTP_ERROR_CODE)) {
                        respondToStealthPayment(stealthAddress, txid, paymentAddress, txTime);
                        respondToStealthPaymentGetTxTries++;
                    } else {
                        TLTxObject txObject = new TLTxObject(self, jsonObject);
                        handleGetTxSuccessForRespondToStealthPayment(stealthAddress,
                                paymentAddress, txid, txTime, txObject);
                        respondToStealthPaymentGetTxTries = 0;
                    }
                }
            };

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject object = blockExplorerAPI.getTx(txid);
                        Message message = Message.obtain();
                        message.obj = object;
                        handler.sendMessage(Message.obtain(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public void setWalletDataNotFetched() {
        for (int i = 0; i < this.accounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(i);
            accountObject.setFetchedAccountData(false);
        }
        for (int i = 0; i < this.coldWalletAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.coldWalletAccounts.getAccountObjectForIdx(i);
            accountObject.setFetchedAccountData(false);
        }
        for (int i = 0; i < this.importedAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(i);
            accountObject.setFetchedAccountData(false);
        }
        for (int i = 0; i < this.importedWatchAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedWatchAccounts.getAccountObjectForIdx(i);
            accountObject.setFetchedAccountData(false);
        }
        for (int i = 0; i < this.importedAddresses.getCount();  i++) {
            TLImportedAddress importedAddress = this.importedAddresses.getAddressObjectAtIdx(i);
            importedAddress.setFetchedAccountData(false);
        }
        for (int i = 0; i < this.importedWatchAddresses.getCount();  i++) {
            TLImportedAddress importedAddress = this.importedWatchAddresses.getAddressObjectAtIdx(i);
            importedAddress.setFetchedAccountData(false);
        }
    }

    public void setWalletTransactionListenerClosed() {
        for (int i = 0; i < this.accounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(i);
            accountObject.listeningToIncomingTransactions = false;
        }
        for (int i = 0; i < this.coldWalletAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.coldWalletAccounts.getAccountObjectForIdx(i);
            accountObject.listeningToIncomingTransactions = false;
        }
        for (int i = 0; i < this.importedAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(i);
            accountObject.listeningToIncomingTransactions = false;
        }
        for (int i = 0; i < this.importedWatchAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedWatchAccounts.getAccountObjectForIdx(i);
            accountObject.listeningToIncomingTransactions = false;
        }
        for (int i = 0; i < this.importedAddresses.getCount();  i++) {
            TLImportedAddress importedAddress = this.importedAddresses.getAddressObjectAtIdx(i);
            importedAddress.listeningToIncomingTransactions = false;
        }
        for (int i = 0; i < this.importedWatchAddresses.getCount();  i++) {
            TLImportedAddress importedAddress = this.importedWatchAddresses.getAddressObjectAtIdx(i);
            importedAddress.listeningToIncomingTransactions = false;
        }
    }

    public void updateModelWithNewTransaction(JSONObject txDict) {
        TLTxObject txObject = new TLTxObject(this, txDict);
        if (this.pendingSelfStealthPaymentTxid != null) {
            // Special case where receiving stealth payment from same sending account.
            // Let stealth websocket handle it
            // Need this cause, must generate private key and add address to account so that the bitcoins can be accounted for.
            if (txObject.getHash().equals(this.pendingSelfStealthPaymentTxid)) {
                //this.pendingSelfStealthPaymentTxid = null
                return;
            }
        }

        List<String> addressesInTx = txObject.getAddresses();

        for (int i = 0; i < this.accounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData()) {
                continue;
            }
            for (String address : addressesInTx) {
                if (accountObject.isAddressPartOfAccount(address )) {
                    this.handleNewTxForAccount(accountObject, txObject);
                }
            }
        }

        for (int i = 0; i < this.coldWalletAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.coldWalletAccounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData()) {
                continue;
            }
            for (String address : addressesInTx) {
                if (accountObject.isAddressPartOfAccount(address)) {
                    this.handleNewTxForAccount(accountObject, txObject);
                }
            }
        }

        for (int i = 0; i < this.importedAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData()) {
                continue;
            }
            for (String address : addressesInTx) {
                if (accountObject.isAddressPartOfAccount(address)) {
                    this.handleNewTxForAccount(accountObject, txObject);
                }
            }
        }

        for (int i = 0; i < this.importedWatchAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedWatchAccounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData()) {
                continue;
            }
            for (String address : addressesInTx) {
                if (accountObject.isAddressPartOfAccount(address)) {
                    this.handleNewTxForAccount(accountObject, txObject);
                }
            }
        }

        for (int i = 0; i < this.importedAddresses.getCount();  i++) {
            TLImportedAddress importedAddress = this.importedAddresses.getAddressObjectAtIdx(i);
            if (!importedAddress.hasFetchedAccountData()) {
                continue;
            }
            String address = importedAddress.getAddress();
            for (String addr : addressesInTx) {
                if (addr.equals(address)) {
                    this.handleNewTxForImportedAddress(importedAddress, txObject);
                }
            }
        }

        for (int i = 0; i < this.importedWatchAddresses.getCount();  i++) {
            TLImportedAddress importedAddress = this.importedWatchAddresses.getAddressObjectAtIdx(i);
            if (!importedAddress.hasFetchedAccountData()) {
                continue;
            }
            String address = importedAddress.getAddress();
            for (String addr : addressesInTx) {
                if (addr.equals(address)) {
                    this.handleNewTxForImportedAddress(importedAddress, txObject);
                }
            }
        }
    }

    void handleNewTxForAccount(TLAccountObject accountObject, TLTxObject txObject) {
        TLCoin receivedAmount = accountObject.processNewTx(txObject);
        String receivedTo = accountObject.getAccountNameOrAccountPublicKey();
        accountObject.getAccountDataO(true, new TLCallback() {
            @Override
            public void onSuccess(Object obj) {
                updateUIForNewTx(txObject.getHash(), receivedAmount, receivedTo);
            }

            @Override
            public void onFail(Integer status, String error) {

            }
        });
    }

    void handleNewTxForImportedAddress(TLImportedAddress importedAddress, TLTxObject txObject) {
        TLCoin receivedAmount = importedAddress.processNewTx(txObject);
        String receivedTo = importedAddress.getLabel();
        importedAddress.getSingleAddressDataO(true, new TLCallback() {
            @Override
            public void onSuccess(Object obj) {
                updateUIForNewTx(txObject.getHash(), receivedAmount, receivedTo);
            }

            @Override
            public void onFail(Integer status, String error) {

            }
        });
    }

    @UiThread
    void updateUIForNewTx(String txHash, TLCoin receivedAmount, String receivedTo) {
        webSocketNotifiedTxHashSet.add(txHash);
        Intent intent = new Intent(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION);
        intent.putExtra("txHash", txHash);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
        if (receivedAmount != null) {
            Intent intent1 = new Intent(TLNotificationEvents.EVENT_RECEIVE_PAYMENT);
            intent1.putExtra("receivedTo", receivedTo);
            intent1.putExtra("receivedAmount", this.currencyFormat.getProperAmount(receivedAmount));
            LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent1);
        }
    }

    public void updateModelWithNewBlock(JSONObject jsonObject) {
        long blockHeight = 0;
        try {
            blockHeight = jsonObject.getLong("height");
            this.blockchainStatus.blockHeight = blockHeight;
            Intent intent = new Intent(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_BLOCK);
            LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void initializeWalletApp(String appVersion, boolean recoverHDWalletIfNewlyInstalledApp) {
//        StringBuilder stringBuilder = new StringBuilder();
//        for (StackTraceElement element : Thread.currentThread().getStackTrace()) stringBuilder.append(element.toString()).append("\n");
//        Log.d(TAG, "initializeWalletApp getStackTracetoString " + stringBuilder.toString());


        this.blockchainStatus = new TLBlockchainStatus();
        this.stealthServerConfig = new TLStealthServerConfig();
        this.walletJson = new TLWalletJson(this);

        appWallet = new TLWallet(this, "App Wallet", new TLWalletConfig(false));

        if (this.preferences.getInstallDate() == null) {
            this.preferences.setHasSetupHDWallet(false);
            this.preferences.setInstallDate();
            this.preferences.setInstalledAndroidVersion();
            this.preferences.setAppVersion(appVersion);
            Log.d(TAG, "setInstallDate " + this.preferences.getInstallDate());
        } else if (!appVersion.equals(this.preferences.getAppVersion())) {
            TLUpdateAppData.instance().beforeUpdatedAppVersion = this.preferences.getAppVersion();
            this.preferences.setAppVersion(appVersion);
            Log.d(TAG, "setAppVersion " + appVersion);
            this.preferences.setDisabledPromptRateApp(false);
        }
        this.isAccountsAndImportsLoaded = false;



        this.keyStore = new TLKeyStore(this);
        this.encryptedPreferences = new TLEncryptedPreferences(this);
        JSONObject walletPayload = null;
        if (this.encryptedPreferences.getWalletPassphrase() != null) {
            walletPayload = this.getLocalWalletJsonDict();
        }



        String passphrase = this.encryptedPreferences.getWalletPassphrase();

        if (!this.preferences.hasSetupHDWallet()) {
            if (recoverHDWalletIfNewlyInstalledApp) {
                this.recoverHDWallet(passphrase);
            } else {
                passphrase = TLHDWalletWrapper.generateMnemonicPassphrase();
                this.refreshApp(passphrase);
                TLAccountObject accountObject = this.accounts.createNewAccount("Account 1", TLAccount.Normal, true);
                accountObject.updateAccountNeedsRecovering(false);
                this.updateGodSend(TLSendFromType.HDWallet, 0);
                this.updateReceiveSelectedObject(TLSendFromType.HDWallet, 0);
                this.updateHistorySelectedObject(TLSendFromType.HDWallet, 0);
            }
            this.justSetupHDWallet = true;
            String encryptedWalletJson = this.walletJson.getEncryptedWalletJsonContainer(this.appWallet.getWalletsJson(),
                    this.encryptedPreferences.getWalletJSONPassphrase());
            boolean success = this.saveWalletJson(encryptedWalletJson, new Date());
            if (success) {
                this.preferences.setHasSetupHDWallet(true);
            } else {
            }
        } else {
            String masterHex = TLHDWalletWrapper.getMasterHex(passphrase);
            if (walletPayload != null) {
                this.appWallet.loadWalletPayload(walletPayload, masterHex);
            } else {
            }
        }

        //this.appWallet.addAddressBookEntry("vJmwhHhMNevDQh188gSeHd2xxxYGBQmnVuMY2yG2MmVTC31UWN5s3vaM3xsM2Q1bUremdK1W7eNVgPg1BnvbTyQuDtMKAYJanahvse", "ArcBit Donation");

        currencyFormat = new TLCurrencyFormat(this);
        blockExplorerAPI = new TLBlockExplorerAPI(this);
        stealthExplorerAPI = new TLStealthExplorerAPI(this);
        this.analytics = new TLAnalytics(this);
        this.suggestions = new TLSuggestions(this);
        this.transactionListener = new TLTransactionListener(this);
        this.stealthWebSocket = new TLStealthWebSocket(this);
        this.pushTxAPI = new TLPushTxAPI(this);
        this.sendFormData = new TLSendFormData();
        this.txFeeAPI = new TLTxFeeAPI(this);

        this.accounts = new TLAccounts(this, this.appWallet, this.appWallet.getAccountObjectArray(), TLAccountType.HDWallet);
        this.coldWalletAccounts = new TLAccounts(this, this.appWallet, this.appWallet.getColdWalletAccountArray(), TLAccountType.ColdWallet);
        this.importedWatchAccounts = new TLAccounts(this, this.appWallet, this.appWallet.getWatchOnlyAccountArray(), TLAccountType.ImportedWatch);
        this.importedAccounts = new TLAccounts(this, this.appWallet, this.appWallet.getImportedAccountArray(), TLAccountType.Imported);
        this.importedAddresses = new TLImportedAddresses(this, this.appWallet, this.appWallet.getImportedPrivateKeyArray(), TLAccountAddressType.Imported);
        this.importedWatchAddresses = new TLImportedAddresses(this, this.appWallet, this.appWallet.getWatchOnlyAddressArray(), TLAccountAddressType.ImportedWatch);


        this.isAccountsAndImportsLoaded = true;

        this.godSend = new TLSpaghettiGodSend(this, this.appWallet);
        this.receiveSelectedObject = new TLSelectedObject();
        this.historySelectedObject = new TLSelectedObject();
        this.updateGodSend();
        Object selectObjected = this.godSend.getSelectedSendObject();
        if (selectObjected instanceof TLAccountObject) {
            this.receiveSelectedObject.setSelectedAccount((TLAccountObject)selectObjected);
            this.historySelectedObject.setSelectedAccount((TLAccountObject)selectObjected);
        } else if (selectObjected instanceof TLImportedAddress) {
            this.receiveSelectedObject.setSelectedAddress((TLImportedAddress)selectObjected);
            this.historySelectedObject.setSelectedAddress((TLImportedAddress)selectObjected);
        }

        assert(this.accounts.getNumberOfAccounts() > 0);

        exchangeRate = new TLExchangeRate();
        exchangeRate.getExchangeRates();
        TLAchievements.instance();

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                JSONObject jsonData = (JSONObject) msg.obj;
                if (jsonData == null || jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                    Log.d(TAG, "getBlockHeight Error getting block height.");
                    return;
                }
                try {
                    String blockHeight = jsonData.getString("height");
                    Log.d(TAG, "getBlockHeight setBlockHeight: " + blockHeight);
                    blockchainStatus.blockHeight = Long.valueOf(blockHeight);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonData = blockExplorerAPI.getBlockHeight();
                    Message message = Message.obtain();
                    message.obj = jsonData;
                    handler.sendMessage(Message.obtain(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void refreshHDWalletAccounts(boolean isRestoringWallet) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < this.accounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(i);

            if (accountObject.needsRecovering()) {
                return;
            }

            List<String> activeAddresses = new ArrayList<String>(accountObject.getActiveMainAddresses());
            activeAddresses.addAll(accountObject.getActiveChangeAddresses());

            if (accountObject.stealthWallet != null) {
                activeAddresses.addAll(accountObject.stealthWallet.getPaymentAddresses());
            }

            if (accountObject.stealthWallet != null) {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        accountObject.fetchNewStealthPayments(isRestoringWallet);
                    }
                });
            }

            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    accountObject.getAccountData(activeAddresses, true);
                }
            });
        }
        executorService.shutdown();
        try {
            boolean finished = executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void showLocalNotificationForCoinsSent(String txHash, String address, TLCoin amount) {
        if (sentPaymentHashes.get(txHash) == null) {
            sentPaymentHashes.put(txHash, "");
            String msg = String.format("Sent %s to %s", this.currencyFormat.getProperAmount(amount), address);
            //TLPrompts.promptSuccessMessage(msg, "");
            //this.showLocalNotification(msg)
        }
    }

    public void listenToIncomingTransactionForWallet() {
        if (!this.isAccountsAndImportsLoaded || !this.transactionListener.isWebSocketOpen()) {
            return;
        }

        for (int i = 0; i < this.accounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.accounts.getAccountObjectForIdx(i);
            if (accountObject.downloadState != TLDownloadState.Downloaded) {
                continue;
            }
            List<String> activeMainAddresses = accountObject.getActiveMainAddresses();
            for (String address : activeMainAddresses) {
                    this.transactionListener.listenToIncomingTransactionForAddress(address);
            }
            List<String> activeChangeAddresses = accountObject.getActiveChangeAddresses();
            for (String address : activeChangeAddresses) {
                    this.transactionListener.listenToIncomingTransactionForAddress(address);
            }

            if (accountObject.stealthWallet != null) {
                List<String> stealthPaymentAddresses = accountObject.stealthWallet.getUnspentPaymentAddresses();
                for (String address : stealthPaymentAddresses) {
                    this.transactionListener.listenToIncomingTransactionForAddress(address);
                }
            }
            accountObject.listeningToIncomingTransactions = true;
        }

        for (int i = 0; i < this.importedAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedAccounts.getAccountObjectForIdx(i);
            if (accountObject.downloadState != TLDownloadState.Downloaded) {
                continue;
            }
            List<String> activeMainAddresses = accountObject.getActiveMainAddresses();
            for (String address : activeMainAddresses) {
                    this.transactionListener.listenToIncomingTransactionForAddress(address);
            }
            List<String> activeChangeAddresses = accountObject.getActiveChangeAddresses();
            for (String address : activeChangeAddresses) {
                    this.transactionListener.listenToIncomingTransactionForAddress(address);
            }

            if (accountObject.stealthWallet != null) {
                List<String> stealthPaymentAddresses = accountObject.stealthWallet.getUnspentPaymentAddresses();
                for (String address : stealthPaymentAddresses) {
                    this.transactionListener.listenToIncomingTransactionForAddress(address);
                }
            }
            accountObject.listeningToIncomingTransactions = true;
        }

        for (int i = 0; i < this.coldWalletAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.coldWalletAccounts.getAccountObjectForIdx(i);
            if (accountObject.downloadState != TLDownloadState.Downloaded) {
                continue;
            }
            List<String> activeMainAddresses = accountObject.getActiveMainAddresses();
            for (String address : activeMainAddresses) {
                this.transactionListener.listenToIncomingTransactionForAddress(address);
            }
            List<String> activeChangeAddresses = accountObject.getActiveChangeAddresses();
            for (String address : activeChangeAddresses) {
                this.transactionListener.listenToIncomingTransactionForAddress(address);
            }
            accountObject.listeningToIncomingTransactions = true;
        }

        for (int i = 0; i < this.importedWatchAccounts.getNumberOfAccounts();  i++) {
            TLAccountObject accountObject = this.importedWatchAccounts.getAccountObjectForIdx(i);
            if (accountObject.downloadState != TLDownloadState.Downloaded) {
                continue;
            }
            List<String> activeMainAddresses = accountObject.getActiveMainAddresses();
            for (String address : activeMainAddresses) {
                    this.transactionListener.listenToIncomingTransactionForAddress(address);
            }
            List<String> activeChangeAddresses = accountObject.getActiveChangeAddresses();
            for (String address : activeChangeAddresses) {
                    this.transactionListener.listenToIncomingTransactionForAddress(address);
            }
            accountObject.listeningToIncomingTransactions = true;
        }


        for (int i = 0; i < this.importedAddresses.getCount();  i++) {
            TLImportedAddress importedAddress = this.importedAddresses.getAddressObjectAtIdx(i);
            if (importedAddress.downloadState != TLDownloadState.Downloaded) {
                continue;
            }
            String address = importedAddress.getAddress();
            this.transactionListener.listenToIncomingTransactionForAddress(address);
            importedAddress.listeningToIncomingTransactions = true;
        }

        for (int i = 0; i < this.importedWatchAddresses.getCount();  i++) {
            TLImportedAddress importedAddress = this.importedWatchAddresses.getAddressObjectAtIdx(i);
            if (importedAddress.downloadState != TLDownloadState.Downloaded) {
                continue;
            }
            String address = importedAddress.getAddress();
            this.transactionListener.listenToIncomingTransactionForAddress(address);
            importedAddress.listeningToIncomingTransactions = true;
        }

    }

    void saveWalletPayloadDelay() {
        if (this.saveWalletJSONEnabled == false) {
            return;
        }
        Log.d(TAG, "saveWalletPayloadDelay starting...");
        saveWalletHandler.removeCallbacks(saveWalletRunnable);
        saveWalletHandler.postDelayed(saveWalletRunnable, this.SAVE_WALLET_PAYLOAD_DELAY*1000);
    }

    public boolean saveWalletJson() {
        if (saveWalletJSONEnabled == false) {
            Log.d(TAG, "saveWalletJSONEnabled disabled");
            return false;
        }
        Log.d(TAG, "saveWalletJson starting...");

        String encryptedWalletJson = this.walletJson.getEncryptedWalletJsonContainer(this.appWallet.getWalletsJson(),
                this.encryptedPreferences.getWalletJSONPassphrase());
        this.saveWalletJson(encryptedWalletJson, new Date());

        return true;
    }

    private boolean saveWalletJson(String encryptedWalletJson, Date date) {
        boolean success = this.walletJson.saveWalletJson(encryptedWalletJson, date);
        Log.d(TAG, "saveWalletJson success: " + success);
        if (!success) {
            LocalBroadcastManager.getInstance(this.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_SAVE_WALLET_ERROR));
        }
        return success;
    }
    public JSONObject getLocalWalletJsonDict() {
        return this.walletJson.getWalletJsonDict(this.walletJson.getLocalWalletJSONFile(),
                this.encryptedPreferences.getWalletJSONPassphrase());
    }
}
