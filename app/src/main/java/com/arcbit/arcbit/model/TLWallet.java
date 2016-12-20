package com.arcbit.arcbit.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.arcbit.arcbit.APIs.TLStealthExplorerAPI;
import com.arcbit.arcbit.model.TLWalletUtils.TLAccountType;
import com.arcbit.arcbit.model.TLWalletJSONKeys.TLStealthPaymentStatus;
import com.arcbit.arcbit.utils.TLUtils;


public class TLWallet {

    TLAppDelegate appDelegate;
    private String walletName;
    public TLWalletConfig walletConfig;
    private JSONObject rootDict;
    private int currentHDWalletIdx;
    private String masterHex;

    public TLWallet(TLAppDelegate appDelegate, String walletName, TLWalletConfig walletConfig) {
        this.appDelegate = appDelegate;
        this.walletName = walletName;
        this.walletConfig = walletConfig;
        this.currentHDWalletIdx = 0;
    }

    //----------------------------------------------------------------------------------------------------------------
    
    private JSONObject createStealthAddressDict(String extendKey, boolean isPrivateExtendedKey) {
        assert(isPrivateExtendedKey == true); // "Cant generate stealth address scan key from xpub key"
        JSONObject stealthAddressDict = new JSONObject();
        HashMap<String, String> stealthAddressObject = TLHDWalletWrapper.getStealthAddress(extendKey, this.walletConfig.isTestnet);
        try {
            stealthAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESS, (stealthAddressObject.get("stealthAddress")));
            stealthAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESS_SCAN_KEY, (stealthAddressObject.get("scanPriv")));
            stealthAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESS_SPEND_KEY, (stealthAddressObject.get("spendPriv")));

            stealthAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_SERVERS, new JSONObject());
            stealthAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS, new JSONArray());
            stealthAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LAST_TX_TIME, 0);
            return stealthAddressDict;
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject createAccountDictWithPreload(String accountName, String extendedKey,
                                                    boolean isPrivateExtendedKey, int accountIdx,
                                                    boolean preloadStartingAddresses) {

        JSONObject account = new JSONObject();
        try {
            account.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_NAME, accountName);
            account.put(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_IDX, accountIdx);


            if (isPrivateExtendedKey) {
                String extendedPublickey = TLHDWalletWrapper.getExtendPubKey(extendedKey, this.walletConfig.isTestnet);
                account.put(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PUBLIC_KEY, extendedPublickey);
                account.put(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PRIVATE_KEY, extendedKey);


                JSONArray stealthAddressesArray = new JSONArray();

                JSONObject stealthAddressDict = createStealthAddressDict(extendedKey, isPrivateExtendedKey);
                stealthAddressesArray.put(stealthAddressDict);
                account.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESSES, stealthAddressesArray);
            } else {
                account.put(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PUBLIC_KEY, extendedKey);
            }

            account.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, TLWalletJSONKeys.TLAddressStatus.Active);
            account.put(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_NEEDS_RECOVERING, true);

            JSONArray mainAddressesArray = new JSONArray();
            account.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES, mainAddressesArray);

            JSONArray changeAddressesArray = new JSONArray();
            account.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES, changeAddressesArray);

            account.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX, 0);
            account.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX, 0);

            if (!preloadStartingAddresses) {
                return account;
            }

            //create initial receiving address
            String extendedPublicKey = account.getString(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PUBLIC_KEY);
            for (int i = 0; i < TLAccountObject.MAX_ACCOUNT_WAIT_TO_RECEIVE_ADDRESS(); i++) {
                JSONObject mainAddressDict = new JSONObject();
                int mainAddressIdx = i;
                ArrayList<Integer> mainAddressSequence = new ArrayList<Integer>(Arrays.asList((Integer)TLWalletJSONKeys.TLAddressType.Main.getValue(), mainAddressIdx));

                String address = TLHDWalletWrapper.getAddress(extendedPublicKey, mainAddressSequence, this.walletConfig.isTestnet);
                mainAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS, address);
                mainAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, TLWalletJSONKeys.TLAddressStatus.Active.getValue());
                mainAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_INDEX, i);
                mainAddressesArray.put(mainAddressDict);
            }

            JSONObject changeAddressDict = new JSONObject();
            int changeAddressIdx = 0;
            ArrayList<Integer> changeAddressSequence = new ArrayList<Integer>(Arrays.asList((Integer)TLWalletJSONKeys.TLAddressType.Change.getValue(), changeAddressIdx));

            String address = TLHDWalletWrapper.getAddress(extendedPublicKey, changeAddressSequence, this.walletConfig.isTestnet);
            changeAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS, address);
            changeAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, TLWalletJSONKeys.TLAddressStatus.Active.getValue());
            changeAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_INDEX, 0);
            changeAddressesArray.put(changeAddressDict);

            return account;
        } catch (JSONException e) {
            return null;
        }
    }


    private JSONObject getAccountDict(int accountIdx) {
        JSONArray accountsArray = getAccountsArray();
        try {
            JSONObject accountDict = accountsArray.getJSONObject(accountIdx);
        return accountDict;
        } catch (JSONException e) {
            return null;
        }
    }

    //----------------------------------------------------------------------------------------------------------------

    void clearAllAddressesFromHDWallet(int accountIdx) {
        JSONObject accountDict = getAccountDict(accountIdx);
        JSONArray mainAddressesArray = new JSONArray();
        try {
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES, mainAddressesArray);
            JSONArray changeAddressesArray = new JSONArray();
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES, changeAddressesArray);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX, 0);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX, 0);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void clearAllAddressesFromColdWalletAccount(int idx) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        JSONArray mainAddressesArray = new JSONArray();
        try {
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES, mainAddressesArray);
            JSONArray changeAddressesArray = new JSONArray();
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES, changeAddressesArray);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX, 0);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX, 0);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void clearAllAddressesFromImportedAccount(int idx) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        JSONArray mainAddressesArray = new JSONArray();
        try {
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES, mainAddressesArray);
            JSONArray changeAddressesArray = new JSONArray();
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES, changeAddressesArray);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX, 0);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX, 0);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void clearAllAddressesFromImportedWatchAccount(int idx) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        JSONArray mainAddressesArray = new JSONArray();
        try {
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES, mainAddressesArray);
            JSONArray changeAddressesArray = new JSONArray();
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES, changeAddressesArray);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX, 0);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX, 0);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    void updateAccountNeedsRecoveringFromHDWallet(int accountIdx, boolean accountNeedsRecovering) {
        JSONObject accountDict = getAccountDict(accountIdx);
        try {
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_NEEDS_RECOVERING, accountNeedsRecovering);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void updateAccountNeedsRecoveringFromColdWalletAccount(int idx, boolean accountNeedsRecovering) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        try {
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_NEEDS_RECOVERING, accountNeedsRecovering);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void updateAccountNeedsRecoveringFromImportedAccount(int idx, boolean accountNeedsRecovering) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        try {
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_NEEDS_RECOVERING, accountNeedsRecovering);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }

    }

    void updateAccountNeedsRecoveringFromImportedWatchAccount(int idx, boolean accountNeedsRecovering) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        try {
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_ACCOUNT_NEEDS_RECOVERING, accountNeedsRecovering);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }
    //----------------------------------------------------------------------------------------------------------------


    void updateMainAddressStatusFromHDWallet(int accountIdx, int addressIdx,
                                             TLWalletJSONKeys.TLAddressStatus addressStatus) {
        JSONObject accountDict = getAccountDict(accountIdx);
        this.updateMainAddressStatus(accountDict, addressIdx, addressStatus);
    }

    void updateMainAddressStatusFromColdWalletAccount(int idx, int addressIdx,
                                                      TLWalletJSONKeys.TLAddressStatus addressStatus) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        this.updateMainAddressStatus(accountDict, addressIdx, addressStatus);
    }

    void updateMainAddressStatusFromImportedAccount(int idx, int addressIdx,
                                                    TLWalletJSONKeys.TLAddressStatus addressStatus) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        this.updateMainAddressStatus(accountDict, addressIdx, addressStatus);
    }

    void updateMainAddressStatusFromImportedWatchAccount(int idx, int addressIdx,
                                                         TLWalletJSONKeys.TLAddressStatus addressStatus) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        this.updateMainAddressStatus(accountDict, addressIdx, addressStatus);
    }

    void updateMainAddressStatus(JSONObject accountDict, int addressIdx, TLWalletJSONKeys.TLAddressStatus addressStatus) {
        try {
            long minMainAddressIdx = accountDict.getLong(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX);
            assert(addressIdx == minMainAddressIdx); //"addressIdx != minMainAddressIdx"
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX, minMainAddressIdx+1);

            JSONArray mainAddressesArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES);

            if (addressStatus == TLWalletJSONKeys.TLAddressStatus.Archived && !TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
                mainAddressesArray = TLUtils.removeFromJSONArray(mainAddressesArray, 0);
                accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES, mainAddressesArray);
            } else {
                JSONObject mainAddressDict = mainAddressesArray.getJSONObject(addressIdx);
                mainAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, addressStatus.getValue());
            }
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void updateChangeAddressStatusFromHDWallet(int accountIdx, int addressIdx,
                                               TLWalletJSONKeys.TLAddressStatus addressStatus) {
        JSONObject accountDict = getAccountDict(accountIdx);
        this.updateChangeAddressStatus(accountDict, addressIdx, addressStatus);
    }

    void updateChangeAddressStatusFromColdWalletAccount(int idx, int addressIdx,
                                                        TLWalletJSONKeys.TLAddressStatus addressStatus) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        this.updateChangeAddressStatus(accountDict, addressIdx, addressStatus);
    }

    void updateChangeAddressStatusFromImportedAccount(int idx, int addressIdx,
                                                      TLWalletJSONKeys.TLAddressStatus addressStatus) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        this.updateChangeAddressStatus(accountDict, addressIdx, addressStatus);
    }

    void updateChangeAddressStatusFromImportedWatchAccount(int idx, int addressIdx,
                                                           TLWalletJSONKeys.TLAddressStatus addressStatus) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        this.updateChangeAddressStatus(accountDict, addressIdx, addressStatus);
    }

    void updateChangeAddressStatus(JSONObject accountDict, int addressIdx, TLWalletJSONKeys.TLAddressStatus addressStatus) {
        try {
            long minChangeAddressIdx = accountDict.getLong(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX);
            assert(addressIdx == minChangeAddressIdx); //"addressIdx != minMainAddressIdx"
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX, minChangeAddressIdx+1);

            JSONArray changeAddressesArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES);

            if (addressStatus == TLWalletJSONKeys.TLAddressStatus.Archived && !TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
                changeAddressesArray = TLUtils.removeFromJSONArray(changeAddressesArray, 0);
                accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES, changeAddressesArray);
            } else {
                JSONObject changeAddressDict = changeAddressesArray.getJSONObject(addressIdx);
                changeAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, addressStatus.getValue());
            }
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }
    //----------------------------------------------------------------------------------------------------------------
    int getMinMainAddressIdxFromHDWallet(int accountIdx) throws JSONException {
        JSONObject accountDict = getAccountDict(accountIdx);
        return this.getMinMainAddressIdx(accountDict);
    }

    int getMinMainAddressIdxFromColdWalletAccount(int idx) throws JSONException {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        return this.getMinMainAddressIdx(accountDict);
    }

    int getMinMainAddressIdxFromImportedAccount(int idx) throws JSONException {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        return this.getMinMainAddressIdx(accountDict);
    }

    int getMinMainAddressIdxFromImportedWatchAccount(int idx) throws JSONException {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        return this.getMinMainAddressIdx(accountDict);
    }

    int getMinMainAddressIdx(JSONObject accountDict) throws JSONException {
        return accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX);
    }

    int getMinChangeAddressIdxFromHDWallet(int accountIdx) throws JSONException {
        JSONObject accountDict = getAccountDict(accountIdx);
        return this.getMinChangeAddressIdx(accountDict);
    }

    int getMinChangeAddressIdxFromColdWalletAccount(int idx) throws JSONException {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        return this.getMinChangeAddressIdx(accountDict);
    }

    int getMinChangeAddressIdxFromImportedAccount(int idx) throws JSONException {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        return this.getMinChangeAddressIdx(accountDict);
    }

    int getMinChangeAddressIdxFromImportedWatchAccount(int idx) throws JSONException {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        return this.getMinChangeAddressIdx(accountDict);
    }

    int getMinChangeAddressIdx(JSONObject accountDict) throws JSONException {
        return accountDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX);
    }

    //----------------------------------------------------------------------------------------------------------------

    JSONObject getNewMainAddressFromHDWallet(int accountIdx, int expectedAddressIndex) throws Exception {
        JSONObject accountDict = getAccountDict(accountIdx);
        return getNewMainAddress(accountDict, expectedAddressIndex);
    }

    JSONObject getNewMainAddressFromColdWalletAccount(int idx, int expectedAddressIndex) throws Exception {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        return getNewMainAddress(accountDict, expectedAddressIndex);
    }

    JSONObject getNewMainAddressFromImportedAccount(int idx, int expectedAddressIndex) throws Exception {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        return getNewMainAddress(accountDict, expectedAddressIndex);
    }

    JSONObject getNewMainAddressFromImportedWatchAccount(int idx, int expectedAddressIndex) throws Exception {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        return getNewMainAddress(accountDict, expectedAddressIndex);
    }

    private JSONObject getNewMainAddress(JSONObject accountDict, int expectedAddressIndex) throws Exception {
        JSONArray mainAddressesArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES);
        int mainAddressIdx = 0;
        if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
            assert(expectedAddressIndex == mainAddressesArray.length()); // "expectedAddressIndex != mainAddressesArray.count"
            mainAddressIdx = expectedAddressIndex;
        } else {
            long minMainAddressIdx = accountDict.getLong(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX);
            assert(expectedAddressIndex == mainAddressesArray.length() + minMainAddressIdx); //"expectedAddressIndex != mainAddressesArray.count + minMainAddressIdx"
            mainAddressIdx = expectedAddressIndex;
        }

        if (mainAddressIdx >= Integer.MAX_VALUE) {
            throw new Exception("reached max hdwallet index");
        }

        ArrayList<Integer> mainAddressSequence = new ArrayList<Integer>(Arrays.asList((Integer)TLWalletJSONKeys.TLAddressType.Main.getValue(), mainAddressIdx));
        JSONObject mainAddressDict = new JSONObject();
        String extendedPublicKey = accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PUBLIC_KEY);
        String address = TLHDWalletWrapper.getAddress(extendedPublicKey, mainAddressSequence, this.walletConfig.isTestnet);
        mainAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS, address);
        mainAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, TLWalletJSONKeys.TLAddressStatus.Active.getValue());
        mainAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_INDEX, mainAddressIdx);
        mainAddressesArray.put(mainAddressDict);

        appDelegate.saveWalletPayloadDelay();

        return mainAddressDict;
    }
    //----------------------------------------------------------------------------------------------------------------
    JSONObject getNewChangeAddressFromHDWallet(int accountIdx, int expectedAddressIndex) throws Exception {
        JSONObject accountDict = getAccountDict(accountIdx);
        return getNewChangeAddress(accountDict, expectedAddressIndex);
    }

    JSONObject getNewChangeAddressFromColdWalletAccount(int idx, int expectedAddressIndex) throws Exception {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        return getNewChangeAddress(accountDict, expectedAddressIndex);
    }

    JSONObject getNewChangeAddressFromImportedAccount(int idx, int expectedAddressIndex) throws Exception {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        return getNewChangeAddress(accountDict, expectedAddressIndex);
    }

    JSONObject getNewChangeAddressFromImportedWatchAccount(int idx, int expectedAddressIndex) throws Exception {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        return getNewChangeAddress(accountDict, expectedAddressIndex);
    }

    private JSONObject getNewChangeAddress(JSONObject accountDict, int expectedAddressIndex) throws Exception {
        JSONArray changeAddressesArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES);

        int changeAddressIdx = 0;
        if (TLWalletUtils.SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON) {
            assert(expectedAddressIndex == changeAddressesArray.length()); // "expectedAddressIndex != changeAddressesArray.count"
            changeAddressIdx = expectedAddressIndex;
        } else {
            long minChangeAddressIdx = accountDict.getLong(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX);
            assert(expectedAddressIndex == changeAddressesArray.length() + minChangeAddressIdx); //"expectedAddressIndex != changeAddressesArray.count + minChangeAddressIdx"
            changeAddressIdx = expectedAddressIndex;
        }

        if (changeAddressIdx >= Integer.MAX_VALUE) {
            throw new Exception("reached max hdwallet index");
        }

        ArrayList<Integer> changeAddressSequence = new ArrayList<Integer>(Arrays.asList((Integer)TLWalletJSONKeys.TLAddressType.Change.getValue(), changeAddressIdx));
        JSONObject changeAddressDict = new JSONObject();
        String extendedPublicKey = accountDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_EXTENDED_PUBLIC_KEY);
        String address = TLHDWalletWrapper.getAddress(extendedPublicKey, changeAddressSequence, this.walletConfig.isTestnet);
        changeAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS, address);
        changeAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, TLWalletJSONKeys.TLAddressStatus.Active.getValue());
        changeAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_INDEX, changeAddressIdx);
        changeAddressesArray.put(changeAddressDict);

        appDelegate.saveWalletPayloadDelay();

        return changeAddressDict;
    }

    //----------------------------------------------------------------------------------------------------------------
    String removeTopMainAddressFromHDWallet(int accountIdx) {
        JSONObject accountDict = getAccountDict(accountIdx);
        return removeTopMainAddress(accountDict);
    }

    String removeTopMainAddressFromColdWalletAccount(int idx) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        return removeTopMainAddress(accountDict);
    }

    String removeTopMainAddressFromImportedAccount(int idx) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        return removeTopMainAddress(accountDict);
    }

    String removeTopMainAddressFromImportedWatchAccount(int idx) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        return removeTopMainAddress(accountDict);
    }

    private String removeTopMainAddress(JSONObject accountDict) {
        try {
            JSONArray mainAddressesArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES);
            if (mainAddressesArray.length() > 0) {
                JSONObject mainAddressDict = mainAddressesArray.getJSONObject(mainAddressesArray.length()-1);
                mainAddressesArray = TLUtils.removeFromJSONArray(mainAddressesArray, mainAddressesArray.length()-1);
                accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAIN_ADDRESSES, mainAddressesArray);
                appDelegate.saveWalletPayloadDelay();
                return mainAddressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
            }
            return null;
        } catch (JSONException e) {
            return null;
        }
    }
    //----------------------------------------------------------------------------------------------------------------
    String removeTopChangeAddressFromHDWallet(int accountIdx) {
        JSONObject accountDict = getAccountDict(accountIdx);
        return removeTopChangeAddress(accountDict);
    }

    String removeTopChangeAddressFromColdWalletAccount(int idx) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        return removeTopChangeAddress(accountDict);
    }

    String removeTopChangeAddressFromImportedAccount(int idx) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        return removeTopChangeAddress(accountDict);
    }

    String removeTopChangeAddressFromImportedWatchAccount(int idx) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        return removeTopChangeAddress(accountDict);
    }

    private String removeTopChangeAddress(JSONObject accountDict) {
        try {
            JSONArray changeAddressesArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES);
            if (changeAddressesArray.length() > 0) {
                JSONObject changeAddressDict = changeAddressesArray.getJSONObject(changeAddressesArray.length()-1);
                changeAddressesArray = TLUtils.removeFromJSONArray(changeAddressesArray, changeAddressesArray.length()-1);
                accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES, changeAddressesArray);
                appDelegate.saveWalletPayloadDelay();
                return changeAddressDict.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
            }
            return null;
        } catch (JSONException e) {
            return null;
        }
    }

    //----------------------------------------------------------------------------------------------------------------

    void archiveAccountHDWallet(int accountIdx, boolean enabled) {
        JSONArray accountsArray = getAccountsArray();
        assert(accountsArray.length() > 1);
        try {
            JSONObject accountDict = accountsArray.getJSONObject(accountIdx);
            TLWalletJSONKeys.TLAddressStatus status = enabled ? TLWalletJSONKeys.TLAddressStatus.Archived : TLWalletJSONKeys.TLAddressStatus.Active;
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, status.getValue());
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void archiveAccountColdWalletAccount(int idx, boolean enabled) {
        try {
            JSONObject accountDict = getColdWalletAccountAtIndex(idx);
            TLWalletJSONKeys.TLAddressStatus status = enabled ? TLWalletJSONKeys.TLAddressStatus.Archived : TLWalletJSONKeys.TLAddressStatus.Active;
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, status.getValue());
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void archiveAccountImportedAccount(int idx, boolean enabled) {
        try {
            JSONObject accountDict = getImportedAccountAtIndex(idx);
            TLWalletJSONKeys.TLAddressStatus status = enabled ? TLWalletJSONKeys.TLAddressStatus.Archived : TLWalletJSONKeys.TLAddressStatus.Active;
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, status.getValue());
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void archiveAccountImportedWatchAccount(int idx, boolean enabled) {
        try {
            JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
            TLWalletJSONKeys.TLAddressStatus status = enabled ? TLWalletJSONKeys.TLAddressStatus.Archived : TLWalletJSONKeys.TLAddressStatus.Active;
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, status.getValue());
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //----------------------------------------------------------------------------------------------------------------

    private JSONArray getAccountsArray() {
        try {
            JSONObject hdWalletDict = getHDWallet();
            JSONArray accountsArray = hdWalletDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ACCOUNTS);
            return accountsArray;
        } catch (JSONException e) {
            return null;
        }
    }

    boolean removeTopAccount() {
        JSONArray accountsArray = getAccountsArray();
        if (accountsArray.length() > 0) {
            accountsArray = TLUtils.removeFromJSONArray(accountsArray, accountsArray.length()-1);
            try {
                JSONObject hdWalletDict = getHDWallet();
                hdWalletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ACCOUNTS, accountsArray);
                int maxAccountIDCreated = hdWalletDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAX_ACCOUNTS_CREATED);
                hdWalletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAX_ACCOUNTS_CREATED, (maxAccountIDCreated - 1));
                appDelegate.saveWalletPayloadDelay();
                return true;
            } catch (JSONException e) {
            }
        }
        return false;
    }

    private TLAccountObject createNewAccount(String accountName, TLWalletJSONKeys.TLAccount accountType) {
        return createNewAccount(accountName, accountType, true);
    }

    TLAccountObject createNewAccount(String accountName, TLWalletJSONKeys.TLAccount accountType, boolean preloadStartingAddresses) {
        assert(this.masterHex != null);
        try {
            JSONObject hdWalletDict = getHDWallet();
            JSONArray accountsArray = getAccountsArray();
            int maxAccountIDCreated = hdWalletDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAX_ACCOUNTS_CREATED);
            String extendPrivKey = TLHDWalletWrapper.getExtendPrivKey(this.masterHex, maxAccountIDCreated, this.walletConfig.isTestnet);
            JSONObject accountDict = createAccountDictWithPreload(accountName, extendPrivKey,
                    true, maxAccountIDCreated, preloadStartingAddresses);
            accountsArray.put(accountDict);
            hdWalletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAX_ACCOUNTS_CREATED, (maxAccountIDCreated + 1));

            appDelegate.saveWalletPayloadDelay();

            if (getCurrentAccountID() == null) {
                setCurrentAccountID("0");
            }

            return new TLAccountObject(this.appDelegate, this, accountDict, TLAccountType.HDWallet);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject createWallet(String passphrase, String masterHex, String walletName) {
        try {
            JSONObject createdWalletDict = new JSONObject();

            JSONObject hdWalletDict = new JSONObject();
            hdWalletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_NAME, walletName);
            hdWalletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MASTER_HEX, masterHex);
            hdWalletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PASSPHRASE, passphrase);

            hdWalletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_MAX_ACCOUNTS_CREATED, 0);

            JSONArray accountsArray = new JSONArray();
            hdWalletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ACCOUNTS, accountsArray);
            JSONArray hdWalletsArray = new JSONArray();
            hdWalletsArray.put(hdWalletDict);

            createdWalletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_HDWALLETS, hdWalletsArray);

            JSONObject importedKeysDict = new JSONObject();

            JSONArray coldWalletAccountsArray = new JSONArray();
            JSONArray importedAccountsArray = new JSONArray();
            JSONArray watchOnlyAccountsArray = new JSONArray();
            JSONArray importedPrivateKeysArray = new JSONArray();
            JSONArray watchOnlyAddressesArray = new JSONArray();
            importedKeysDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_COLD_WALLET_ACCOUNTS, coldWalletAccountsArray);
            importedKeysDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_ACCOUNTS, importedAccountsArray);
            importedKeysDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ACCOUNTS, watchOnlyAccountsArray);
            importedKeysDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_PRIVATE_KEYS, importedPrivateKeysArray);
            importedKeysDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ADDRESSES, watchOnlyAddressesArray);

            createdWalletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTS, importedKeysDict);

            return createdWalletDict;
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject getImportedKeysDict() {
        try {
            JSONObject hdWallet = getCurrentWallet();
            return hdWallet.getJSONObject(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTS);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject getColdWalletAccountAtIndex(int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray coldWalletAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_COLD_WALLET_ACCOUNTS);
            JSONObject accountDict = coldWalletAccountsArray.getJSONObject(idx);
            return accountDict;
        } catch (JSONException e) {
            return null;
        }
    }
    private JSONObject getImportedAccountAtIndex(int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray importedAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_ACCOUNTS);
            JSONObject accountDict = importedAccountsArray.getJSONObject(idx);
            return accountDict;
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject getImportedWatchOnlyAccountAtIndex(int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray watchOnlyAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ACCOUNTS);
            JSONObject accountDict = watchOnlyAccountsArray.getJSONObject(idx);
            return accountDict;
        } catch (JSONException e) {
            return null;
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------------------------------------

    TLAccountObject addColdWalletAccount(String extendedPublicKey) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray coldWalletAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_COLD_WALLET_ACCOUNTS);

            int accountIdx = coldWalletAccountsArray.length(); // "accountIdx" key is different for ImportedAccount then hdwallet account
            JSONObject watchOnlyAccountDict = createAccountDictWithPreload("", extendedPublicKey, false, accountIdx, false);
            coldWalletAccountsArray.put(watchOnlyAccountDict);
            appDelegate.saveWalletPayloadDelay();
            return new TLAccountObject(this.appDelegate, this, watchOnlyAccountDict, TLAccountType.ColdWallet);
        } catch (JSONException e) {
            return null;
        }
    }

    void deleteColdWalletAccount(int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray coldWalletAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_COLD_WALLET_ACCOUNTS);
            coldWalletAccountsArray = TLUtils.removeFromJSONArray(coldWalletAccountsArray, idx);
            importedKeysDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_COLD_WALLET_ACCOUNTS, coldWalletAccountsArray);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void setColdWalletAccountName(String name, int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray coldWalletAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_COLD_WALLET_ACCOUNTS);

            JSONObject accountDict = coldWalletAccountsArray.getJSONObject(idx);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_NAME, name);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    ArrayList<TLAccountObject> getColdWalletAccountArray() {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray accountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_COLD_WALLET_ACCOUNTS);

            ArrayList<TLAccountObject> accountObjectArray = new ArrayList<TLAccountObject>();
            for (int i = 0; i < accountsArray.length(); i++) {
                JSONObject accountDict = accountsArray.getJSONObject(i);
                TLAccountObject accountObject = new TLAccountObject(this.appDelegate, this, accountDict, TLAccountType.ColdWallet);
                accountObjectArray.add(accountObject);
            }
            return accountObjectArray;
        } catch (JSONException e) {
            return null;
        }
    }

    TLAccountObject addImportedAccount(String extendedPrivateKey) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray importedAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_ACCOUNTS);

            int accountIdx = importedAccountsArray.length(); // "accountIdx" key is different for ImportedAccount then hdwallet account
            JSONObject accountDict = createAccountDictWithPreload("", extendedPrivateKey, true, accountIdx, false);
            importedAccountsArray.put(accountDict);
            appDelegate.saveWalletPayloadDelay();

            return new TLAccountObject(this.appDelegate, this, accountDict, TLAccountType.Imported);
        } catch (JSONException e) {
            return null;
        }
    }

    void deleteImportedAccount(int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray importedAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_ACCOUNTS);
            importedAccountsArray = TLUtils.removeFromJSONArray(importedAccountsArray, idx);
            importedKeysDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_ACCOUNTS, importedAccountsArray);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void setImportedAccountName(String name, int idx) {
        try {
            JSONObject accountDict = getImportedAccountAtIndex(idx);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_NAME, name);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    ArrayList<TLAccountObject> getImportedAccountArray() {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray accountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_ACCOUNTS);

            ArrayList<TLAccountObject> accountObjectArray = new ArrayList<TLAccountObject>();
            for (int i = 0; i < accountsArray.length(); i++) {
                JSONObject accountDict = accountsArray.getJSONObject(i);
                TLAccountObject accountObject = new TLAccountObject(this.appDelegate, this, accountDict, TLAccountType.Imported);
                accountObjectArray.add(accountObject);
            }
            return accountObjectArray;
        } catch (JSONException e) {
            return null;
        }
    }

    TLAccountObject addWatchOnlyAccount(String extendedPublicKey) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray watchOnlyAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ACCOUNTS);

            int accountIdx = watchOnlyAccountsArray.length();
            JSONObject watchOnlyAccountDict = createAccountDictWithPreload("", extendedPublicKey, false, accountIdx, false);
            watchOnlyAccountsArray.put(watchOnlyAccountDict);
            appDelegate.saveWalletPayloadDelay();
            return new TLAccountObject(this.appDelegate, this, watchOnlyAccountDict, TLAccountType.ImportedWatch);
        } catch (JSONException e) {
            return null;
        }
    }

    void deleteWatchOnlyAccount(int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray watchOnlyAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ACCOUNTS);
            watchOnlyAccountsArray = TLUtils.removeFromJSONArray(watchOnlyAccountsArray, idx);
            importedKeysDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ACCOUNTS, watchOnlyAccountsArray);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void setWatchOnlyAccountName(String name, int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray watchOnlyAccountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ACCOUNTS);

            JSONObject accountDict = watchOnlyAccountsArray.getJSONObject(idx);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_NAME, name);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    ArrayList<TLAccountObject> getWatchOnlyAccountArray() {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray accountsArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ACCOUNTS);

            ArrayList<TLAccountObject> accountObjectArray = new ArrayList<TLAccountObject>();
            for (int i = 0; i < accountsArray.length(); i++) {
                JSONObject accountDict = accountsArray.getJSONObject(i);
                TLAccountObject accountObject = new TLAccountObject(this.appDelegate, this, accountDict, TLAccountType.ImportedWatch);
                accountObjectArray.add(accountObject);
            }
            return accountObjectArray;
        } catch (JSONException e) {
            return null;
        }
    }

    JSONObject addImportedPrivateKey(String privateKey, String encryptedPrivateKey) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray importedPrivateKeyArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_PRIVATE_KEYS);

            JSONObject importedPrivateKey = new JSONObject();
            if (encryptedPrivateKey == null) {
                importedPrivateKey.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_KEY, privateKey);
                importedPrivateKey.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS, TLBitcoinjWrapper.getAddress(privateKey, this.walletConfig.isTestnet));
                importedPrivateKey.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL, "");
                importedPrivateKey.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, TLWalletJSONKeys.TLAddressStatus.Active.getValue());
            } else {
                importedPrivateKey.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_KEY, encryptedPrivateKey);
                importedPrivateKey.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS, TLBitcoinjWrapper.getAddress(privateKey, this.walletConfig.isTestnet));
                importedPrivateKey.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL, "");
                importedPrivateKey.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, TLWalletJSONKeys.TLAddressStatus.Active.getValue());
            }

            importedPrivateKeyArray.put(importedPrivateKey);
            appDelegate.saveWalletPayloadDelay();
            return importedPrivateKey;
        } catch (JSONException e) {
            return null;
        }
    }

    void deleteImportedPrivateKey(int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray importedPrivateKeyArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_PRIVATE_KEYS);
            importedPrivateKeyArray = TLUtils.removeFromJSONArray(importedPrivateKeyArray, idx);
            importedKeysDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_PRIVATE_KEYS, importedPrivateKeyArray);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void setImportedPrivateKeyLabel(String label, int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray importedPrivateKeyArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_PRIVATE_KEYS);

            JSONObject privateKeyDict = importedPrivateKeyArray.getJSONObject(idx);
            privateKeyDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL, label);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void setImportedPrivateKeyArchive(boolean archive, int idx) {
        JSONObject importedKeysDict = getImportedKeysDict();
        try {
            JSONArray importedPrivateKeyArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_PRIVATE_KEYS);
            JSONObject privateKeyDict = importedPrivateKeyArray.getJSONObject(idx);

            TLWalletJSONKeys.TLAddressStatus status = archive ? TLWalletJSONKeys.TLAddressStatus.Archived : TLWalletJSONKeys.TLAddressStatus.Active;
            privateKeyDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, status.getValue());
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    ArrayList<TLImportedAddress> getImportedPrivateKeyArray() {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();

            JSONArray importedAddresses = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_IMPORTED_PRIVATE_KEYS);
            ArrayList<TLImportedAddress> importedAddressesObjectArray = new ArrayList<TLImportedAddress>(importedAddresses.length());

            for (int i = 0; i < importedAddresses.length(); i++) {
                JSONObject importedAddressObject = importedAddresses.getJSONObject(i);
                TLImportedAddress accountObject = new TLImportedAddress(this.appDelegate, this, importedAddressObject);
                importedAddressesObjectArray.add(accountObject);
            }
            return importedAddressesObjectArray;
        } catch (JSONException e) {
            return null;
        }
    }

    JSONObject addWatchOnlyAddress(String address) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray watchOnlyAddressArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ADDRESSES);

            JSONObject watchOnlyAddress = new JSONObject();
            watchOnlyAddress.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS, address);
            watchOnlyAddress.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL, "");
            watchOnlyAddress.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, TLWalletJSONKeys.TLAddressStatus.Active.getValue());

            watchOnlyAddressArray.put(watchOnlyAddress);
            appDelegate.saveWalletPayloadDelay();
            return watchOnlyAddress;
        } catch (JSONException e) {
            return null;
        }
    }

    void deleteImportedWatchAddress(int idx) {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();
            JSONArray watchOnlyAddressArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ADDRESSES);
            watchOnlyAddressArray = TLUtils.removeFromJSONArray(watchOnlyAddressArray, idx);
            importedKeysDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ADDRESSES, watchOnlyAddressArray);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }


    void setWatchOnlyAddressLabel(String label, int idx) {
        JSONObject importedKeysDict = getImportedKeysDict();
        try {
            JSONArray watchOnlyAddressArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ADDRESSES);
            JSONObject addressDict = watchOnlyAddressArray.getJSONObject(idx);
            addressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL, label);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void setWatchOnlyAddressArchive(boolean archive, int idx) {
        JSONObject importedKeysDict = getImportedKeysDict();
        try {
            JSONArray watchOnlyAddressArray = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ADDRESSES);
            JSONObject addressDict = watchOnlyAddressArray.getJSONObject(idx);

            TLWalletJSONKeys.TLAddressStatus status = archive ? TLWalletJSONKeys.TLAddressStatus.Archived : TLWalletJSONKeys.TLAddressStatus.Active;
            addressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, status.getValue());
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }


    ArrayList<TLImportedAddress> getWatchOnlyAddressArray() {
        try {
            JSONObject importedKeysDict = getImportedKeysDict();

            JSONArray importedAddresses = importedKeysDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_WATCH_ONLY_ADDRESSES);
            ArrayList<TLImportedAddress> importedAddressesObjectArray = new ArrayList<TLImportedAddress>(importedAddresses.length());

            for (int i = 0; i < importedAddresses.length(); i++) {
                JSONObject importedAddressObject = importedAddresses.getJSONObject(i);
                TLImportedAddress accountObject = new TLImportedAddress(this.appDelegate, this, importedAddressObject);
                importedAddressesObjectArray.add(accountObject);
            }
            return importedAddressesObjectArray;
        } catch (JSONException e) {
            return null;
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------------------------------------

    public JSONArray getAddressBook() {
        try {
            return this.getCurrentWallet().getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS_BOOK);
        } catch (JSONException e) {
            return null;
        }
    }

    public void addAddressBookEntry(String address, String label) {
        try {
            JSONArray addressBookArray = getCurrentWallet().getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS_BOOK);
            JSONObject addr = new JSONObject();
            addr.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS, address);
            addr.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL, label);
            addressBookArray.put(addr);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    public String getLabelForAddress(String address) {
        try {
            JSONArray addressBookArray = getCurrentWallet().getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS_BOOK);
            for (int i = 0; i < addressBookArray.length(); i++) {
                JSONObject addressBook = addressBookArray.getJSONObject(i);
                if (address.equals(addressBook.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS))) {
                    return addressBook.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL);
                }
            }
        } catch (JSONException e) {
        }
        return null;
    }

    public void editAddressBookEntry(int index, String label) {
        try {
            JSONArray addressBookArray = getCurrentWallet().getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS_BOOK);
            JSONObject oldEntry = addressBookArray.getJSONObject(index);
            JSONObject newEntry = new JSONObject();
            newEntry.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS, oldEntry.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS));
            newEntry.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL, label);
            addressBookArray.put(index, newEntry);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    public void deleteAddressBookEntry(int idx) {
        try {
            JSONObject currentWallet = getCurrentWallet();
            JSONArray addressBookArray = currentWallet.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS_BOOK);
            addressBookArray = TLUtils.removeFromJSONArray(addressBookArray, idx);
            currentWallet.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS_BOOK, addressBookArray);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    public void setTransactionTag(String txid, String tag) {
        try {
            JSONObject transactionLabelDict = getCurrentWallet().getJSONObject(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_TRANSACTION_TAGS);
            transactionLabelDict.put(txid, tag);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    public void deleteTransactionTag(String txid) {
        try {
            JSONObject transactionLabelDict = getCurrentWallet().getJSONObject(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_TRANSACTION_TAGS);
            transactionLabelDict.remove(txid);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    public String getTransactionTag(String txid) {
        try {
        JSONObject transactionLabelDict = getCurrentWallet().getJSONObject(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_TRANSACTION_TAGS);
        return transactionLabelDict.getString(txid);
        } catch (JSONException e) {
            return null;
        }
    }


    private void createNewWallet(String passphrase, String masterHex, String walletName) {
        try {
            JSONArray walletsArray = getWallets();

            JSONObject walletDict = createWallet(passphrase, masterHex, walletName);
            walletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS_BOOK, new JSONArray());
            walletDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_TRANSACTION_TAGS, new JSONObject());

            walletsArray.put(walletDict);
        } catch (JSONException e) {
        }
    }

    public void createInitialWalletPayload(String passphrase, String masterHex) {
        try {
            this.masterHex = masterHex;

            rootDict = new JSONObject();
            JSONArray walletsArray = new JSONArray();
            rootDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_VERSION, TLWalletJSONKeys.WALLET_PAYLOAD_VERSION);

            JSONObject payload = new JSONObject();
            rootDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYLOAD, payload);

            payload.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_WALLETS, walletsArray);
            createNewWallet(passphrase, masterHex, "default");
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void loadWalletPayload(JSONObject walletPayload, String masterHex) {
        this.masterHex = masterHex;
        rootDict = walletPayload;
    }

    JSONObject getWalletsJson() {
        try {
            return new JSONObject(rootDict.toString());
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONArray getWallets() {
        try {
            return rootDict.getJSONObject(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYLOAD).getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_WALLETS);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject getFirstWallet() {
        try {
            return getWallets().getJSONObject(0);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject getCurrentWallet() {
        return getFirstWallet();
    }

    private JSONObject getHDWallet() {
        try {
            JSONArray a = getCurrentWallet().getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_HDWALLETS);
            return a.getJSONObject(0);
        } catch (JSONException e) {
            return null;
        }
    }

    private String getCurrentAccountID() {
        try {
            JSONObject hdWallet = getHDWallet();
            return hdWallet.getString(TLWalletJSONKeys.WALLET_PAYLOAD_CURRENT_ACCOUNT_ID);
        } catch (JSONException e) {
            return null;
        }
    }

    private void setCurrentAccountID(String accountID) {
        try {
            JSONObject hdWallet = getHDWallet();
            hdWallet.put(TLWalletJSONKeys.WALLET_PAYLOAD_CURRENT_ACCOUNT_ID, accountID);
        } catch (JSONException e) {
        }
    }

    boolean renameAccount(int accountIdxNumber, String accountName) {
        try {
            JSONArray accountsArray = getAccountsArray();
            JSONObject accountDict = accountsArray.getJSONObject(accountIdxNumber);
            accountDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_NAME, accountName);
            appDelegate.saveWalletPayloadDelay();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public ArrayList<TLAccountObject> getAccountObjectArray() {
        try {
            JSONArray accountsArray = getAccountsArray();
            ArrayList<TLAccountObject> accountObjectArray = new ArrayList<TLAccountObject>();
            for (int i = 0; i < accountsArray.length(); i++) {
                JSONObject accountDict = accountsArray.getJSONObject(i);
                TLAccountObject accountObject = new TLAccountObject(this.appDelegate, this, accountDict, TLAccountType.HDWallet);
                accountObjectArray.add(accountObject);
            }

            return accountObjectArray;
        } catch (JSONException e) {
            return null;
        }
    }

    private TLAccountObject getAccountObjectForIdx(int accountIdx) {
        try {
            JSONArray accountsArray = getAccountsArray();
            JSONObject accountDict = accountsArray.getJSONObject(accountIdx);
            return new TLAccountObject(this.appDelegate, this, accountDict, TLAccountType.HDWallet);
        } catch (JSONException e) {
            return null;
        }
    }

    // TLWallet+Stealth
    //----------------------------------------------------------------------------------------------------------------

    void setStealthAddressServerStatus(JSONObject accountDict, String serverURL, boolean isWatching) {
        try {
            JSONArray stealthAddressArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESSES);
            JSONObject stealthAddressServersDict = stealthAddressArray.getJSONObject(0).getJSONObject(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_SERVERS);
            try {
                JSONObject stealthServerDict = stealthAddressServersDict.getJSONObject(serverURL);
                stealthServerDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_WATCHING, isWatching);
            } catch (JSONException e) {
                JSONObject serverAttributesDict = new JSONObject();
                serverAttributesDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_WATCHING, isWatching);
                stealthAddressServersDict.put(serverURL, serverAttributesDict);
            }
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void setStealthAddressServerStatusHDWallet(int accountIdx, String serverURL, boolean isWatching) {
        JSONObject accountDict = getAccountDict(accountIdx);
        setStealthAddressServerStatus(accountDict, serverURL, isWatching);
    }

    void setStealthAddressServerStatusColdWalletAccount(int idx, String serverURL, boolean isWatching) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        setStealthAddressServerStatus(accountDict, serverURL, isWatching);
    }

    void setStealthAddressServerStatusImportedAccount(int idx, String serverURL, boolean isWatching) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        setStealthAddressServerStatus(accountDict, serverURL, isWatching);
    }

    void setStealthAddressServerStatusImportedWatchAccount(int idx, String serverURL, boolean isWatching) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        setStealthAddressServerStatus(accountDict, serverURL, isWatching);
    }


    void setStealthAddressLastTxTime(JSONObject accountDict, String serverURL, long lastTxTime) {
        try {
            JSONArray stealthAddressArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESSES);
            JSONObject stealthAddressDict = stealthAddressArray.getJSONObject(0);
            stealthAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LAST_TX_TIME, lastTxTime);
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void setStealthAddressLastTxTimeHDWallet(int accountIdx, String serverURL, long lastTxTime) {
        JSONObject accountDict = getAccountDict(accountIdx);
        setStealthAddressLastTxTime(accountDict, serverURL, lastTxTime);
    }

    void setStealthAddressLastTxTimeColdWalletAccount(int idx, String serverURL, long lastTxTime) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        if (accountDict == null) {
            return;
        }
        setStealthAddressLastTxTime(accountDict, serverURL, lastTxTime);
    }

    void setStealthAddressLastTxTimeImportedAccount(int idx, String serverURL, long lastTxTime) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        if (accountDict == null) {
            return;
        }
        setStealthAddressLastTxTime(accountDict, serverURL, lastTxTime);
    }

    void setStealthAddressLastTxTimeImportedWatchAccount(int idx, String serverURL, long lastTxTime) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        if (accountDict == null) {
            return;
        }
        setStealthAddressLastTxTime(accountDict, serverURL, lastTxTime);
    }


    private void addStealthAddressPaymentKey(JSONObject accountDict, String privateKey,
                                             String address, String txid, long txTime, TLStealthPaymentStatus stealthPaymentStatus) {
        try {
            JSONObject stealthAddressPaymentDict = new JSONObject();

            stealthAddressPaymentDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_KEY, privateKey);
            stealthAddressPaymentDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS, address);
            stealthAddressPaymentDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_TXID, txid);
            stealthAddressPaymentDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_TIME, txTime);
            stealthAddressPaymentDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHECK_TIME, 0);
            stealthAddressPaymentDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, stealthPaymentStatus.getValue());

            JSONArray newStealthAddressPaymentsArray = new JSONArray();

            JSONArray stealthAddressArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESSES);
            JSONObject stealthAddressDict = stealthAddressArray.getJSONObject(0);
            JSONArray stealthAddressPaymentsArray = stealthAddressDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS);
            int i;
            for (i = 0; i < stealthAddressPaymentsArray.length(); i++) {
                JSONObject currentStealthAddressPaymentDict = stealthAddressPaymentsArray.getJSONObject(i);
                if (txTime > currentStealthAddressPaymentDict.getLong(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_TIME)) {
                    newStealthAddressPaymentsArray.put(currentStealthAddressPaymentDict);
                } else {
                    break;
                }
            }
            newStealthAddressPaymentsArray.put(stealthAddressPaymentDict);
            for (; i < stealthAddressPaymentsArray.length(); i++) {
                JSONObject currentStealthAddressPaymentDict = stealthAddressPaymentsArray.getJSONObject(i);
                newStealthAddressPaymentsArray.put(currentStealthAddressPaymentDict);
            }
            stealthAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS, newStealthAddressPaymentsArray);

            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void addStealthAddressPaymentKeyHDWallet(int accountIdx, String privateKey, String address,
                                             String txid, long txTime, TLStealthPaymentStatus stealthPaymentStatus) {
        JSONObject accountDict = getAccountDict(accountIdx);
        addStealthAddressPaymentKey(accountDict, privateKey, address,
                 txid, txTime, stealthPaymentStatus);
    }

    void addStealthAddressPaymentKeyColdWalletAccount(int idx, String privateKey, String address,
                                                         String txid, long txTime, TLStealthPaymentStatus stealthPaymentStatus) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        addStealthAddressPaymentKey(accountDict, privateKey, address,
                txid, txTime, stealthPaymentStatus);
    }

    void addStealthAddressPaymentKeyImportedAccount(int idx, String privateKey, String address,
                                                    String txid, long txTime, TLStealthPaymentStatus stealthPaymentStatus) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        addStealthAddressPaymentKey(accountDict, privateKey, address,
                 txid, txTime, stealthPaymentStatus);
    }

    void addStealthAddressPaymentKeyImportedWatchAccount(int idx, String privateKey, String address,
                                                         String txid, long txTime, TLStealthPaymentStatus stealthPaymentStatus) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        addStealthAddressPaymentKey(accountDict, privateKey, address,
                 txid, txTime, stealthPaymentStatus);
    }


    void setStealthPaymentLastCheckTime(JSONObject accountDict, String txid, long lastCheckTime) {
        try {
            JSONArray stealthAddressArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESSES);
            JSONArray paymentsArray = stealthAddressArray.getJSONObject(0).getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS);
            for (int i = 0; i < paymentsArray.length(); i++) {
                JSONObject payment = paymentsArray.getJSONObject(i);
                if (payment.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_TXID) == txid) {
                    payment.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHECK_TIME, lastCheckTime);
                    break;
                }
            }
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void setStealthPaymentLastCheckTimeHDWallet(int accountIdx, String txid, long lastCheckTime) {
        JSONObject accountDict = getAccountDict(accountIdx);
        setStealthPaymentLastCheckTime(accountDict,  txid, lastCheckTime);
    }

    void setStealthPaymentLastCheckTimeColdWalletAccount(int idx, String txid, long lastCheckTime) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        setStealthPaymentLastCheckTime(accountDict,  txid, lastCheckTime);
    }

    void setStealthPaymentLastCheckTimeImportedAccount(int idx, String txid, long lastCheckTime) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        setStealthPaymentLastCheckTime(accountDict,  txid, lastCheckTime);
    }

    void setStealthPaymentLastCheckTimeImportedWatchAccount(int idx, String txid, long lastCheckTime) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        setStealthPaymentLastCheckTime(accountDict,  txid, lastCheckTime);
    }

    void setStealthPaymentStatus(JSONObject accountDict, String txid,
                                 TLStealthPaymentStatus stealthPaymentStatus, long lastCheckTime) {
        try {
            JSONArray stealthAddressArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESSES);
            JSONArray paymentsArray = stealthAddressArray.getJSONObject(0).getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS);
            for (int i = 0; i < paymentsArray.length(); i++) {
                JSONObject payment = paymentsArray.getJSONObject(i);
                if (payment.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_TXID) == txid) {
                    payment.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS, stealthPaymentStatus.getValue());
                    payment.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_CHECK_TIME, lastCheckTime);
                    break;
                }
            }
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void setStealthPaymentStatusHDWallet(int accountIdx, String txid, TLStealthPaymentStatus stealthPaymentStatus, long lastCheckTime) {
        JSONObject accountDict = getAccountDict(accountIdx);
        setStealthPaymentStatus(accountDict,  txid, stealthPaymentStatus, lastCheckTime);
    }

    void setStealthPaymentStatusColdWalletAccount(int idx, String txid, TLStealthPaymentStatus stealthPaymentStatus, long lastCheckTime) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        setStealthPaymentStatus(accountDict,  txid, stealthPaymentStatus, lastCheckTime);
    }

    void setStealthPaymentStatusImportedAccount(int idx, String txid, TLStealthPaymentStatus stealthPaymentStatus, long lastCheckTime) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        setStealthPaymentStatus(accountDict,  txid, stealthPaymentStatus, lastCheckTime);
    }

    void setStealthPaymentStatusImportedWatchAccount(int idx, String txid, TLStealthPaymentStatus stealthPaymentStatus, long lastCheckTime) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        setStealthPaymentStatus(accountDict,  txid, stealthPaymentStatus, lastCheckTime);
    }


    void removeOldStealthPayments(JSONObject accountDict) {
        try {
            JSONArray stealthAddressArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESSES);
            JSONArray stealthAddressPaymentsArray = stealthAddressArray.getJSONObject(0).getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS);

            int startCount = stealthAddressPaymentsArray.length();
            int stealthAddressPaymentsArrayCount = stealthAddressPaymentsArray.length();
            while (stealthAddressPaymentsArray.length() > TLStealthExplorerAPI.STEALTH_PAYMENTS_FETCH_COUNT) {
                JSONObject stealthAddressPaymentDict = stealthAddressPaymentsArray.getJSONObject(0);
                if (stealthAddressPaymentDict.getInt(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STATUS) == TLStealthPaymentStatus.Spent.getValue()) {
                    stealthAddressPaymentsArray = TLUtils.removeFromJSONArray(stealthAddressPaymentsArray, 0);
                    stealthAddressPaymentsArrayCount--;
                } else {
                    break;
                }
            }

            if (startCount != stealthAddressPaymentsArrayCount) {
                stealthAddressArray.getJSONObject(0).put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS, stealthAddressPaymentsArray);
                appDelegate.saveWalletPayloadDelay();
            }
        } catch (JSONException e) {
        }
    }

    void removeOldStealthPaymentsHDWallet(int accountIdx) {
        JSONObject accountDict = getAccountDict(accountIdx);
        removeOldStealthPayments(accountDict);
    }

    void removeOldStealthPaymentsColdWalletAccount(int idx) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        removeOldStealthPayments(accountDict);
    }

    void removeOldStealthPaymentsImportedAccount(int idx) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        removeOldStealthPayments(accountDict);
    }

    void removeOldStealthPaymentsImportedWatchAccount(int idx) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        removeOldStealthPayments(accountDict);
    }


    void clearAllStealthPayments(JSONObject accountDict) {
        try {
            JSONArray stealthAddressArray = accountDict.getJSONArray(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_STEALTH_ADDRESSES);
            JSONObject stealthAddressDict = stealthAddressArray.getJSONObject(0);
            stealthAddressDict.put(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_PAYMENTS, new JSONArray());
            appDelegate.saveWalletPayloadDelay();
        } catch (JSONException e) {
        }
    }

    void clearAllStealthPaymentsFromHDWallet(int accountIdx) {
        JSONObject accountDict = getAccountDict(accountIdx);
        clearAllStealthPayments(accountDict);
    }

    void clearAllStealthPaymentsFromColdWalletAccount(int idx) {
        JSONObject accountDict = getColdWalletAccountAtIndex(idx);
        clearAllStealthPayments(accountDict);
    }

    void clearAllStealthPaymentsFromImportedAccount(int idx) {
        JSONObject accountDict = getImportedAccountAtIndex(idx);
        clearAllStealthPayments(accountDict);
    }

    void clearAllStealthPaymentsFromImportedWatchAccount(int idx) {
        JSONObject accountDict = getImportedWatchOnlyAccountAtIndex(idx);
        clearAllStealthPayments(accountDict);
    }
}
