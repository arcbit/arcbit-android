package com.arcbit.arcbit.model;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.Pair;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arcbit.arcbit.APIs.TLNetworking;
import com.arcbit.arcbit.model.TLSendFormData.TLSelectObjectType;

public class TLSpaghettiGodSend {
    private static final String TAG = TLSpaghettiGodSend.class.getName();

    public class CreateTransactionException extends Exception {
        public CreateTransactionException(String message) {
            super(message);
        }
    }

    public class DustException extends CreateTransactionException {
        public TLCoin spendableAmount;

        public DustException(String message, TLCoin spendableAmount) {
            super(message);
            this.spendableAmount = spendableAmount;
        }
    }

    public class InsufficientFundsException extends CreateTransactionException {
        public TLCoin valueSelected;
        public TLCoin valueNeeded;

        public InsufficientFundsException(String message, TLCoin valueSelected, TLCoin valueNeeded) {
            super(message);
            this.valueSelected = valueSelected;
            this.valueNeeded = valueNeeded;
        }
    }

    public class DustOutputException extends CreateTransactionException {
        public TLCoin dustAmount;

        public DustOutputException(String message, TLCoin dustAmount) {
            super(message);
            this.dustAmount = dustAmount;
        }
    }

    public class InsufficientUnspentOutputException extends CreateTransactionException {
        public InsufficientUnspentOutputException(String message) {
            super(message);
        }
    }

    private class InputDataObject {
        byte[] tx_hash;
        byte[] txid;
        int tx_output_n;
        byte[] script;
        String private_key;
        JSONObject hd_account_info; //FIXME, not a clean way to do this
    }

    private class OutputDataObject {
        String to_address;
        long amount;
    }

    long DUST_AMOUNT = 546;

    TLAppDelegate appDelegate;
    private TLWallet appWallet;
    private List<TLAccountObject> sendFromAccounts;
    private List<TLImportedAddress> sendFromAddresses;

    public TLSpaghettiGodSend(TLAppDelegate appDelegate, TLWallet appWallet) {
        this.appDelegate = appDelegate;
        this.appWallet = appWallet;
        sendFromAccounts = new ArrayList<TLAccountObject>();
        sendFromAddresses = new ArrayList<TLImportedAddress>();
    }

    public void clearFromAccountsAndAddresses() {
        sendFromAccounts = new ArrayList<TLAccountObject>();
        sendFromAddresses = new ArrayList<TLImportedAddress>();
    }

    public Object getSelectedSendObject() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            return sendFromAccounts.get(0);
        } else if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            return sendFromAddresses.get(0);
        }

        return null;
    }

    public TLSelectObjectType getSelectedObjectType() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            return TLSelectObjectType.Account;
        } else if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            return TLSelectObjectType.Address;
        }

        return TLSelectObjectType.Unknown;
    }

    public boolean isPaymentToOwnAccount(String address) {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            if (accountObject.stealthWallet != null && address.equals(accountObject.stealthWallet.getStealthAddress())) {
                return true;
            }
            if (accountObject.isAddressPartOfAccount(address)) {
                return true;
            }
            return false;
        } else if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            if (address.equals(importedAddress.getAddress())) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean haveUpDatedUTXOs() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            return accountObject.haveUpDatedUTXOs;
        } else if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            return importedAddress.haveUpDatedUTXOs;
        }
        return false;
    }

    public String getLabelForSelectedSendObject() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            return accountObject.getAccountName();
        } else if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            return importedAddress.getLabel();
        }
        return null;
    }

    public String getCurrentFromLabel() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            return accountObject.getAccountName();
        } else if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            return importedAddress.getLabel();
        }

        return null;
    }

    public String getStealthAddress() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            if (accountObject.stealthWallet != null) {
                return accountObject.stealthWallet.getStealthAddress();
            }
        }
        return null;
    }

    public String getExtendedPubKey() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            return accountObject.getExtendedPubKey();
        }
        return null;
    }

    public void setOnlyFromAccount(TLAccountObject accountObject) {
        sendFromAddresses = null;
        sendFromAccounts = new ArrayList<TLAccountObject>(Arrays.asList(accountObject));
    }

    public void setOnlyFromAddress(TLImportedAddress importedAddress) {
        sendFromAccounts = null;
        sendFromAddresses = new ArrayList<TLImportedAddress>(Arrays.asList(importedAddress));
    }

    private void addSendAccount(TLAccountObject accountObject) {
        sendFromAccounts.add(accountObject);
    }

    private void addImportedAddress(TLImportedAddress importedAddress) {
        sendFromAddresses.add(importedAddress);
    }

    public boolean isColdWalletAccount() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            return accountObject.isColdWalletAccount();
        }
        return false;
    }

    public boolean needWatchOnlyAccountPrivateKey() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            return accountObject.isWatchOnly() && !accountObject.hasSetExtendedPrivateKeyInMemory();
        }
        return false;
    }

    public boolean needWatchOnlyAddressPrivateKey() {
        if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            return importedAddress.isWatchOnly() && !importedAddress.hasSetPrivateKeyInMemory();
        }
        return false;
    }

    public boolean needEncryptedPrivateKeyPassword() {
        if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            if (importedAddress.isWatchOnly()) {
                return false;
            } else {
                return importedAddress.isPrivateKeyEncrypted() && !importedAddress.hasSetPrivateKeyInMemory();
            }
        }
        return false;
    }

    public String getEncryptedPrivateKey() {
        assert (sendFromAddresses.size() != 0); //"sendFromAddresses.size() == 0"
        TLImportedAddress importedAddress = sendFromAddresses.get(0);
        assert (importedAddress.isPrivateKeyEncrypted() != false);// "! importedAddress isPrivateKeyEncrypted]"
        return importedAddress.getEncryptedPrivateKey();
    }

    public boolean hasFetchedCurrentFromData() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            return accountObject.hasFetchedAccountData();
        } else if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            return importedAddress.hasFetchedAccountData();
        }
        return true;
    }

    public void setCurrentFromBalance(TLCoin balance) {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            accountObject.accountBalance = balance;
        } else if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            importedAddress.balance = balance;
        }
    }

    public TLCoin getCurrentFromBalance() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            return accountObject.getBalance();
        } else if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            return importedAddress.getBalance();
        }
        return TLCoin.zero();
    }

    public TLCoin getCurrentFromUnspentOutputsSum() {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            return accountObject.getTotalUnspentSum();
        } else if (sendFromAddresses != null && sendFromAddresses.size() != 0) {
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            return importedAddress.getUnspentSum();
        }
        return TLCoin.zero();
    }

    public void getAndSetUnspentOutputs(TLCallback callback) {
        this.getAndSetUnspentOutputs(this, callback);
    }

    private void getAndSetUnspentOutputs(TLSpaghettiGodSend self, TLCallback callback) {
        if (sendFromAccounts != null && sendFromAccounts.size() != 0) {
            TLAccountObject accountObject = sendFromAccounts.get(0);
            TLCoin amount = accountObject.getBalance();
            if (amount.greater(TLCoin.zero())) {
                accountObject.getUnspentOutputs(callback);
            } else {
                callback.onFail(999, "Account Balance is zero");
            }
        } else {
            List<String> addresses = new ArrayList<>(sendFromAddresses.size());
            TLImportedAddress importedAddress = sendFromAddresses.get(0);
            TLCoin amount = importedAddress.getBalance();

            if (amount.greater(TLCoin.zero())) {
                addresses.add(importedAddress.getAddress());
            }

            if (addresses.size() > 0) {
                importedAddress.haveUpDatedUTXOs = false;
                Handler handler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        JSONObject jsonData = (JSONObject) msg.obj;
                        if (jsonData == null) {
                            callback.onFail(TLNetworking.HTTP_LOCAL_ERROR_CODE, TLNetworking.HTTP_LOCAL_ERROR_MSG);
                            return;
                        }
                        if (jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                            try {
                                callback.onFail(jsonData.getInt(TLNetworking.HTTP_ERROR_CODE), jsonData.getString(TLNetworking.HTTP_ERROR_MSG));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                        try {
                            JSONArray unspentOutputs = jsonData.getJSONArray("unspent_outputs");

                            Map<String, JSONArray> address2UnspentOutputs = new HashMap<String, JSONArray>(addresses.size());
                            for (int i = 0; i < unspentOutputs.length(); i++) {
                                try {
                                    JSONObject unspentOutput = unspentOutputs.getJSONObject(i);

                                    String outputScript = unspentOutput.getString("script");

                                    String address = TLBitcoinjWrapper.getAddressFromOutputScript(outputScript, self.appWallet.walletConfig.isTestnet);
                                    if (address == null) {
                                        Log.d(TAG, "address cannot be decoded. not normal pubkeyhash outputScript: " + outputScript);
                                        continue;
                                    }

                                    JSONArray cachedUnspentOutputs = address2UnspentOutputs.get(address);
                                    if (cachedUnspentOutputs == null) {
                                        cachedUnspentOutputs = new JSONArray();
                                        address2UnspentOutputs.put(address, cachedUnspentOutputs);
                                    }
                                    cachedUnspentOutputs.put(unspentOutput);
                                } catch (JSONException e) {
                                }
                            }

                            for (String address : address2UnspentOutputs.keySet()) {
                                int idx = addresses.indexOf(address);
                                TLImportedAddress importedAddress = self.sendFromAddresses.get(idx);
                                importedAddress.setUnspentOutputs(address2UnspentOutputs.get(address));
                                importedAddress.unspentOutputsCount = address2UnspentOutputs.get(address).length();
                                importedAddress.haveUpDatedUTXOs = true;
                            }
                            callback.onSuccess(jsonData);
                        } catch (JSONException e) {
                        }
                    }
                };
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject object = appDelegate.blockExplorerAPI.getUnspentOutputs(addresses);
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
    }

    public CreatedTransactionObject createSignedSerializedTransactionHex(List<Pair<String, TLCoin>> toAddressesAndAmounts,
                                                                         TLCoin feeAmount, Boolean signTx) throws CreateTransactionException {
        return createSignedSerializedTransactionHex(toAddressesAndAmounts, feeAmount, signTx, null, null);
    }

    public CreatedTransactionObject createSignedSerializedTransactionHex(List<Pair<String, TLCoin>> toAddressesAndAmounts,
                                                                                         TLCoin feeAmount) throws CreateTransactionException {
        return createSignedSerializedTransactionHex(toAddressesAndAmounts, feeAmount, true, null, null);
    }

    public int getEstimatedTxSize(int inputCount, int outputCount) {
        return 10 + 159 * inputCount + 34 * outputCount;
    }

    public CreatedTransactionObject createSignedSerializedTransactionHex(List<Pair<String, TLCoin>> toAddressesAndAmounts,
                                                                                       TLCoin feeAmount, Integer nonce,
                                                                                       String ephemeralPrivateKeyHex) throws CreateTransactionException {
        return createSignedSerializedTransactionHex(toAddressesAndAmounts, feeAmount, true, nonce, ephemeralPrivateKeyHex);
    }

    public CreatedTransactionObject createSignedSerializedTransactionHex(List<Pair<String, TLCoin>> toAddressesAndAmounts,
                                                                                TLCoin feeAmount, Boolean signTx, Integer nonce,
                                                                                String ephemeralPrivateKeyHex) throws CreateTransactionException {

        List<InputDataObject> inputsData = new ArrayList<InputDataObject>();
        List<OutputDataObject> outputsData = new ArrayList<OutputDataObject>();
        TLCoin outputValueSum = TLCoin.zero();

        for (Pair<String, TLCoin> toAddressAndAmount : toAddressesAndAmounts) {
            outputValueSum = outputValueSum.add(toAddressAndAmount.second);
        }
        TLCoin valueNeeded = outputValueSum.add(feeAmount);

        TLCoin valueSelected = TLCoin.zero();

        String changeAddress = null;
        long dustAmount = 0;

        if (sendFromAddresses != null) {
            for (TLImportedAddress importedAddress : sendFromAddresses) {
            if (changeAddress == null) {
                changeAddress = importedAddress.getAddress();
            }

                JSONArray unspentOutputs = importedAddress.getUnspentArray();
                for (int i = 0; i < unspentOutputs.length(); i++) {
                    try {
                        JSONObject unspentOutput = unspentOutputs.getJSONObject(i);
                        long amount = unspentOutput.getLong("value");
                        if (amount < DUST_AMOUNT) {
                            dustAmount += amount;
                            continue;
                        }

                        valueSelected = valueSelected.add(new TLCoin(amount));

                        String outputScript = unspentOutput.getString("script");

                        String address = TLBitcoinjWrapper.getAddressFromOutputScript(outputScript, this.appWallet.walletConfig.isTestnet);
                        if (address == null) {
                            Log.d(TAG, "address cannot be decoded. not normal pubkeyhash outputScript: " + outputScript);
                            continue;
                        }
                        assert(address == changeAddress); //"! address == changeAddress"

                        InputDataObject inputObject = new InputDataObject();
                        inputObject.tx_hash = TLWalletUtils.hexStringToData(unspentOutput.getString("tx_hash"));
                        inputObject.txid = TLWalletUtils.hexStringToData(unspentOutput.getString("tx_hash_big_endian"));
                        inputObject.tx_output_n = unspentOutput.getInt("tx_output_n");
                        inputObject.script = TLWalletUtils.hexStringToData(outputScript);
                        if (signTx) {
                            inputObject.private_key = importedAddress.getPrivateKey();
                        }
                        inputsData.add(inputObject);

                        if (valueSelected.greaterOrEqual(valueNeeded)) {
                            break;
                        }
                    } catch (JSONException e) {
                    }
                }
            }
        }

        if (valueSelected.less(valueNeeded)) {
            changeAddress = null;
            if (sendFromAccounts != null) {

                for (TLAccountObject accountObject :sendFromAccounts) {
                    if (changeAddress == null) {
                        changeAddress = accountObject.getCurrentChangeAddress();
                    }

                    // move some stealth payments to HD wallet as soon as possible
                    JSONArray stealthPaymentUnspentOutputs = new JSONArray();
                    if (accountObject.stealthWallet != null && accountObject.getStealthPaymentUnspentOutputsArray() != null) {
                        stealthPaymentUnspentOutputs = accountObject.getStealthPaymentUnspentOutputsArray();
                    }

                    int unspentOutputsUsingCount = 0;
                    for (int i = 0; i < stealthPaymentUnspentOutputs.length(); i++) {
                        try {
                            JSONObject unspentOutput = stealthPaymentUnspentOutputs.getJSONObject(i);
                            long amount = unspentOutput.getLong("value");
                            if (amount < DUST_AMOUNT) {
                                // if commented out, app will try to spend dust inputs
                                dustAmount += amount;
                                continue;
                            }

                            valueSelected = valueSelected.add(new TLCoin(amount));
                            String outputScript = unspentOutput.getString("script");

                            String address = TLBitcoinjWrapper.getAddressFromOutputScript(outputScript, this.appWallet.walletConfig.isTestnet);
                            if (address == null) {
                                Log.d(TAG, "address cannot be decoded. not normal pubkeyhash outputScript: " + outputScript);
                                continue;
                            }

                            InputDataObject inputObject = new InputDataObject();
                            inputObject.tx_hash = TLWalletUtils.hexStringToData(unspentOutput.getString("tx_hash"));
                            inputObject.txid = TLWalletUtils.hexStringToData(unspentOutput.getString("tx_hash_big_endian"));
                            inputObject.tx_output_n = unspentOutput.getInt("tx_output_n");
                            inputObject.script = TLWalletUtils.hexStringToData(outputScript);
                            if (signTx) {
                                inputObject.private_key = accountObject.stealthWallet.getPaymentAddressPrivateKey(address);
                            }
                            inputsData.add(inputObject);

                            unspentOutputsUsingCount++;
                            if (valueSelected.greaterOrEqual(valueNeeded) && unspentOutputsUsingCount > 12) {
                                // limit amount of stealth payment unspent outputs to use
                                break;
                            }
                        } catch (JSONException e) {
                        }
                    }

                    if (valueSelected.greaterOrEqual(valueNeeded)) {
                        break;
                    }
                    JSONArray unspentOutputs = accountObject.getUnspentArray();
                    for (int i = 0; i < unspentOutputs.length(); i++) {
                        try {
                            JSONObject unspentOutput = unspentOutputs.getJSONObject(i);
                            long amount = unspentOutput.getLong("value");
                            if (amount < DUST_AMOUNT) {
                                dustAmount += amount;
                                continue;
                            }

                            valueSelected = valueSelected.add(new TLCoin(amount));
                            String outputScript = unspentOutput.getString("script");

                            String address = TLBitcoinjWrapper.getAddressFromOutputScript(outputScript, this.appWallet.walletConfig.isTestnet);
                            if (address == null) {
                                Log.d(TAG, "address cannot be decoded. not normal pubkeyhash outputScript: " + outputScript);
                                continue;
                            }

                            InputDataObject inputObject = new InputDataObject();
                            inputObject.tx_hash = TLWalletUtils.hexStringToData(unspentOutput.getString("tx_hash"));
                            inputObject.txid = TLWalletUtils.hexStringToData(unspentOutput.getString("tx_hash_big_endian"));
                            inputObject.tx_output_n = unspentOutput.getInt("tx_output_n");
                            inputObject.script = TLWalletUtils.hexStringToData(outputScript);
                            if (signTx) {
                                inputObject.private_key = accountObject.getAccountPrivateKey(address);
                            } else {
                                inputObject.hd_account_info =  new JSONObject();
                                inputObject.hd_account_info.put("idx", accountObject.getAddressHDIndex(address));
                                inputObject.hd_account_info.put("is_change", !accountObject.isMainAddress(address));
                            }
                            inputsData.add(inputObject);
                            if (valueSelected.greaterOrEqual(valueNeeded)) {
                                break;
                            }
                        } catch (JSONException e) {
                        }
                    }
                }
            }
        }
        List<String> realToAddresses = new ArrayList<String>();
        if (valueSelected.less(valueNeeded)) {
            if (dustAmount > 0) {
                TLCoin dustCoinAmount = new TLCoin(dustAmount);
                throw new DustException("Insufficient Funds. Account contains bitcoin dust.", valueNeeded.subtract(dustCoinAmount));
            }
            throw new InsufficientFundsException("Insufficient Funds.", valueSelected, valueNeeded);
        }

        List<String> stealthOutputScripts = null;
        for (int i = 0; i < toAddressesAndAmounts.size(); i++) {
            String toAddress = toAddressesAndAmounts.get(i).first;
            TLCoin amount = toAddressesAndAmounts.get(i).second;

            if (!TLStealthAddress.isStealthAddress(toAddress, this.appWallet.walletConfig.isTestnet)) {
                realToAddresses.add(toAddress);

                OutputDataObject outputDataObject = new OutputDataObject();
                outputDataObject.to_address = toAddress;
                outputDataObject.amount = amount.toNumber();
                outputsData.add(outputDataObject);
            } else {
                if (stealthOutputScripts == null) {
                    stealthOutputScripts = new ArrayList<String>(1);
                }

                String ephemeralPrivateKey = ephemeralPrivateKeyHex != null ? ephemeralPrivateKeyHex : TLStealthAddress.generateEphemeralPrivkey();
                Integer stealthDataScriptNonce = nonce != null ? nonce : TLStealthAddress.generateNonce();
                Pair<String, String> stealthDataScriptAndPaymentAddress = TLStealthAddress.createDataScriptAndPaymentAddress(toAddress,
                        ephemeralPrivateKey, stealthDataScriptNonce, this.appWallet.walletConfig.isTestnet);

                stealthOutputScripts.add(stealthDataScriptAndPaymentAddress.first);
                String paymentAddress = stealthDataScriptAndPaymentAddress.second;
                realToAddresses.add(paymentAddress);

                OutputDataObject outputDataObject = new OutputDataObject();
                outputDataObject.to_address = paymentAddress;
                outputDataObject.amount = amount.toNumber();
                outputsData.add(outputDataObject);
            }
        }

        TLCoin changeAmount = TLCoin.zero();
        if (valueSelected.greater(valueNeeded)) {
            if (changeAddress != null) {
                changeAmount = valueSelected.subtract(valueNeeded);

                OutputDataObject outputDataObject = new OutputDataObject();
                outputDataObject.to_address = changeAddress;
                outputDataObject.amount = changeAmount.toNumber();
                outputsData.add(outputDataObject);
            }
        }

        if (valueNeeded.greater(valueSelected)) {
            throw new InsufficientUnspentOutputException("Send Error: not enough unspent outputs");
        }

        for (OutputDataObject outputDataObject : outputsData) {
            long outputAmount = outputDataObject.amount;
            if (outputAmount <= DUST_AMOUNT) {
                throw new DustOutputException("", new TLCoin(DUST_AMOUNT));
            }
        }

        Collections.sort(inputsData, (a, b) -> {
            for (int i = 0; i < a.txid.length; i++) {
                int aInt = a.txid[i];
                if (aInt < 0) aInt += 256; // account for fact that java byte are signed
                int bInt = b.txid[i];
                if (bInt < 0) bInt += 256; // account for fact that java byte are signed
                if (aInt < bInt) {
                    return -1;
                } else if (aInt > bInt) {
                    return 1;
                }
            }
            if (a.tx_output_n < b.tx_output_n) {
                return -1;
            } else if (a.tx_output_n > b.tx_output_n) {
                return 1;
            }
            return 0;
        });

        List<String> hashes = new ArrayList<String>();
        List<Integer> inputIndexes = new ArrayList<Integer>();
        List<String> inputScripts = new ArrayList<String>();
        List<String> privateKeys = new ArrayList<String>();
        JSONArray txInputsAccountHDIdxes = new JSONArray();
        boolean isInputsAllFromHDAccountAddresses = true; //only used for cold wallet accounts, and cant have addresses from other then hd account addresses
        for (InputDataObject sortedInput : inputsData) {
            hashes.add(TLWalletUtils.dataToHexString(sortedInput.tx_hash));
            inputIndexes.add(sortedInput.tx_output_n);
            if (signTx) {
                privateKeys.add(sortedInput.private_key);
            } else {
                if (sortedInput.hd_account_info != null) {
                    txInputsAccountHDIdxes.put(sortedInput.hd_account_info);
                } else {
                    isInputsAllFromHDAccountAddresses = false;
                }
            }
            inputScripts.add(TLWalletUtils.dataToHexString(sortedInput.script));
        }

        Collections.sort(outputsData, (a, b) -> {
            if (a.amount < b.amount) {
                return -1;
            } else if (a.amount > b.amount) {
                return 1;
            } else {
                String firstScript = TLBitcoinjWrapper.getStandardPubKeyHashScriptFromAddress(a.to_address, this.appWallet.walletConfig.isTestnet);
                String secondScript = TLBitcoinjWrapper.getStandardPubKeyHashScriptFromAddress(b.to_address, this.appWallet.walletConfig.isTestnet);
                byte[] firstScriptData = TLWalletUtils.hexStringToData(firstScript);
                byte[] secondScriptData = TLWalletUtils.hexStringToData(secondScript);

                for (int i = 0; i < firstScriptData.length; i++) {
                    int aInt = firstScriptData[i];
                    if (aInt < 0) aInt += 256; // account for fact that java byte are signed
                    int bInt = secondScriptData[i];
                    if (bInt < 0) bInt += 256; // account for fact that java byte are signed
                    if (aInt < bInt) {
                        return -1;
                    } else if (aInt > bInt) {
                        return 1;
                    }
                }
            }
            return 0;
        });

        List<Long> outputAmounts = new ArrayList<Long>();
        List<String> outputAddresses = new ArrayList<String>();
        for (OutputDataObject sortedOutput : outputsData) {
            outputAddresses.add(sortedOutput.to_address);
            outputAmounts.add(sortedOutput.amount);
        }

        Log.d(TAG, "createSignedSerializedTransactionHex hashes: " + hashes);
        Log.d(TAG, "createSignedSerializedTransactionHex inputIndexes: " + inputIndexes);
        Log.d(TAG, "createSignedSerializedTransactionHex inputScripts: " + inputScripts);
        Log.d(TAG, "createSignedSerializedTransactionHex outputAddresses: " + outputAddresses);
        Log.d(TAG, "createSignedSerializedTransactionHex outputAmounts: " + outputAmounts);
        //Log.d(TAG, "createSignedSerializedTransactionHex privateKeys: " + privateKeys);
        CreatedTransactionObject createdTransactionObject = TLBitcoinjWrapper.createSignedSerializedTransactionHex(
                hashes, inputIndexes, inputScripts, outputAddresses, outputAmounts, privateKeys,
                stealthOutputScripts, signTx, this.appWallet.walletConfig.isTestnet);
        if (createdTransactionObject != null) {
            if (isInputsAllFromHDAccountAddresses) {
                createdTransactionObject.txInputsAccountHDIdxes = txInputsAccountHDIdxes;
                createdTransactionObject.realToAddresses = realToAddresses;
            }
            return createdTransactionObject;
        }

        throw new CreateTransactionException("Encountered error creating transaction. Please try again.");
    }
}
