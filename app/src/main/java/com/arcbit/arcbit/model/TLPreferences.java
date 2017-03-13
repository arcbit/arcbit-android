package com.arcbit.arcbit.model;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;

import com.arcbit.arcbit.APIs.TLBlockExplorerAPI;
import com.arcbit.arcbit.APIs.TLTxFeeAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class TLPreferences {

    private TLAppDelegate appDelegate;
    public Context context = null;

    private static final String kPreferenceKey = "com.arcbit.io.preferences";

    private static final String FIXED_TRANSACTION_FEE = "fixedtransactionfee";
    private static final String DYNAMIC_FEE_OPTION = "dynamicfeeoption";
    private static final String STEALTH_EXPLORER_URL = "stealthexplorerurl";
    private static final String STEALTH_SERVER_PORT = "stealthwebserverport";
    private static final String STEALTH_WEB_SOCKET_PORT = "stealthwebsocketport";

    private static final String PREFERENCE_INSTALL_DATE = "pref-install-date";
    private static final String PREFERENCE_INSTALLED_ANDROID_VERSION = "pref-installed-android-version";
    private static final String PREFERENCE_APP_VERSION = "pref-app-version";


    private static final String PREFERENCE_FIAT_DISPLAY = "pref-fiat-display";
    private static final String PREFERENCE_BITCOIN_DISPLAY = "pref-bitcoin-display";
    private static final String PREFERENCE_BLOCKEXPLORER_API = "pref-blockexplorer-api";
    private static final String PREFERENCE_BLOCKEXPLORER_API_URL_DICT = "pref-blockexplorer-api-url";
    private static final String PREFERENCE_ENCRYPTED_WALLET_JSON_CHECKSUM = "pref-encrypted-wallet-json-checksum";
    private static final String PREFERENCE_LAST_SAVED_ENCRYPTED_WALLET_JSON_DATE = "pref-last-saved-encrypted-wallet-json-date";
    private static final String PREFERENCE_ENABLE_PIN_CODE = "pref-enable-pin-code";
    private static final String PREFERENCE_WALLET_COLD_WALLET = "pref-cold-wallet";
    private static final String PREFERENCE_WALLET_ADVANCE_MODE = "pref-advance-mode";
    private static final String PREFERENCE_DISPLAY_LOCAL_CURRENCY = "pref-display-local-currency";
    private static final String PREFERENCE_AUTOMATIC_FEE = "pref-automatic-fee";
    private static final String PREFERENCE_SEND_FROM_TYPE = "pref-send-from-type";
    private static final String PREFERENCE_SEND_FROM_INDEX = "pref-send-from-index";
    private static final String PREFERENCE_HAS_SETUP_HDWALLET = "pref-has-setup-hdwallet";
    private static final String PREFERENCE_ENABLE_STEALTH_ADDRESS_DEFAULT = "pref-enable-stealth-address-default";
    private static final String PREFERENCE_ENCRYPTED_BACKUP_PASSPHRASE_KEY = "pref-encrypted-backup-passphrase-key";
    private static final String PREFERENCE_ENABLED_PROMPT_SHOW_WEB_WALLET = "pref-enabled-prompt-show-web-wallet";
    private static final String PREFERENCE_ENABLED_PROMPT_SHOW_TRY_COLD_WALLET = "pref-enabled-prompt-show-try-cold-wallet";
    private static final String PREFERENCE_ENABLED_PROMPT_RATE_APP = "pref-enabled-prompt-rate-app";
    private static final String PREFERENCE_RATED_ONCE = "pref-rated-once";


    private static final String DISABLE_SUGGEST_ENABLE_PIN  = "disableSuggestedEnablePin";
    private static final String DISABLE_SUGGEST_BACKUP_WALLET_PASSPHRASE = "disableSuggestBackUpWalletPassphrase";
    private static final String DISABLE_SUGGEST_DONT_MANAGE_INDIVIDUAL_ACCOUNT_ADDRESSES  = "disableSuggestDontManageIndividualAccountAddresses";
    private static final String DISABLE_SUGGEST_DONT_MANAGE_INDIVIDUAL_ACCOUNT_PRIVATE_KEYS  = "disableSuggestDontManageIndividualAccountPrivateKeys";
    private static final String DISABLE_SUGGEST_DONT_ADD_falseRMAL_ADDRESS_TO_ADDRESS_BOOK  = "disableSuggestDontAddNormalAddressToAddressBook";
    private static final String DISABLE_SUGGEST_MANUALLY_SCAN_TRANSACTION_FOR_STEALTH_TX_INFO  = "disableShowManuallyScanTransactionForStealthTxInfo";
    private static final String DISABLE_SUGGEST_STEALTH_PAYMENT_DELAY_INFO  = "disableShowStealthPaymentDelayInfo";
    private static final String DISABLE_SUGGEST_SHOW_FEE_EXPLANATION_INFO  = "disableShowFeeExplanationInfo";
    private static final String DISABLE_SUGGEST_STEALTH_PAYMENT_NOTE  = "disableShowStealthPaymentNote";
    private static final String DISABLE_SHOW_HIDDEN_OVERLAY_WARNING  = "disabledShowHiddenOverlayWarning";
    private static final String DISABLE_SHOW_IS_ROOTED_WARNING  = "disabledShowIsRootedWarning";
    private static final String DISABLE_SHOW_LOW_ANDROID_VERSION_WARNING  = "disabledShowLowAndroidVersionWarning";


    public TLPreferences(TLAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
        this.context = appDelegate.context;
    }

    public boolean setKeyBool(Context context, String keyName, boolean value) {
        SharedPreferences.Editor editor = this.getWritablePreferenceObject(context);
        editor.putBoolean(keyName, value);
        return editor.commit();
    }

    public boolean getKeyBool(Context context, String keyName) {
        SharedPreferences settings = this.getPreferenceObject(context);
        return settings.getBoolean(keyName, false);
    }

    public boolean setKeyString(Context context, String keyName, String value) {
        SharedPreferences.Editor editor = this.getWritablePreferenceObject(context);
        editor.putString(keyName, value);
        return editor.commit();
    }

    public boolean removeKeyString(Context context, String keyName) {
        SharedPreferences.Editor editor = this.getWritablePreferenceObject(context);
        editor.remove(keyName);
        return editor.commit();
    }

    public int getKeyInt(Context context, String keyName) {
        SharedPreferences settings = this.getPreferenceObject(context);
        return settings.getInt(keyName, 0);
    }

    public boolean setKeyInt(Context context, String keyName, int value) {
        SharedPreferences.Editor editor = this.getWritablePreferenceObject(context);
        editor.putInt(keyName, value);
        return editor.commit();
    }

    public long getKeyLong(Context context, String keyName) {
        SharedPreferences settings = this.getPreferenceObject(context);
        return settings.getLong(keyName, 0);
    }

    public boolean setKeyLong(Context context, String keyName, long value) {
        SharedPreferences.Editor editor = this.getWritablePreferenceObject(context);
        editor.putLong(keyName, value);
        return editor.commit();
    }

    public String getKeyString(Context context, String keyName) {
        SharedPreferences settings = this.getPreferenceObject(context);
        return settings.getString(keyName, null);
    }

    private SharedPreferences.Editor getWritablePreferenceObject(Context ctx) {
        Context sharedDelegate = ctx.getApplicationContext();
        return sharedDelegate.getSharedPreferences(kPreferenceKey, Context.MODE_PRIVATE).edit();
    }

    private SharedPreferences getPreferenceObject(Context ctx) {
        Context sharedDelegate = ctx.getApplicationContext();
        return sharedDelegate.getSharedPreferences(kPreferenceKey, Context.MODE_PRIVATE);
    }

    public boolean setHasSetupHDWallet(boolean value) {
        return setKeyBool(context, PREFERENCE_HAS_SETUP_HDWALLET, value);
    }

    public boolean hasSetupHDWallet() {
        return getKeyBool(context, PREFERENCE_HAS_SETUP_HDWALLET);
    }

    public boolean setInstalledAndroidVersion() {
        if(getInstalledAndroidVersion() == 0) {
            return setKeyInt(context, PREFERENCE_INSTALLED_ANDROID_VERSION, Build.VERSION.SDK_INT);
        }
        return false;
    }

    public int getInstalledAndroidVersion() {
        return getKeyInt(context, PREFERENCE_INSTALLED_ANDROID_VERSION);
    }

    public boolean setInstallDate() {
        if(getInstallDate() == null) {
            return setKeyLong(context, PREFERENCE_INSTALL_DATE, new Date().getTime());
        }
        return false;
    }

    public Date getInstallDate() {
        long time = getKeyLong(context, PREFERENCE_INSTALL_DATE);
        if (time > 0) {
            return new Date(time);
        }
        return null;
    }

    public String getAppVersion() {
        return getKeyString(context, PREFERENCE_APP_VERSION);
    }

    public boolean setAppVersion(String version) {
        return setKeyString(context, PREFERENCE_APP_VERSION, version);
    }

    public int getCurrencyIdx() {
        return getKeyInt(context, PREFERENCE_FIAT_DISPLAY);
    }

    public boolean setCurrency(int currencyIdx) {
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_PREFERENCES_FIAT_DISPLAY_CHANGED));
        return setKeyInt(context, PREFERENCE_FIAT_DISPLAY, currencyIdx);
    }

    public TLCoin.TLBitcoinDenomination getBitcoinDenomination() {
        String enumStr = getKeyString(context, PREFERENCE_BITCOIN_DISPLAY);
        if (enumStr != null) {
            return TLCoin.TLBitcoinDenomination.toMyEnum(enumStr);
        } else {
            return TLCoin.TLBitcoinDenomination.BTC;
        }
    }

    public boolean setBitcoinDisplay(TLCoin.TLBitcoinDenomination bitcoinDisplayIdx) {
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_PREFERENCES_BITCOIN_DISPLAY_CHANGED));
        return setKeyString(context, PREFERENCE_BITCOIN_DISPLAY, bitcoinDisplayIdx.toString());
    }

    public TLWalletUtils.TLSendFromType getSendFromType() {
        String enumStr = getKeyString(context, PREFERENCE_SEND_FROM_TYPE);
        if (enumStr != null) {
            return TLWalletUtils.TLSendFromType.toMyEnum(enumStr);
        } else {
            return TLWalletUtils.TLSendFromType.HDWallet;
        }
    }

    public boolean setSendFromType(TLWalletUtils.TLSendFromType sendFromType) {
        return setKeyString(context, PREFERENCE_SEND_FROM_TYPE, sendFromType.toString());
    }

    public int getSendFromIndex() {
        return getKeyInt(context, PREFERENCE_SEND_FROM_INDEX);
    }

    public boolean setSendFromIndex(int sendFromIndex) {
        return setKeyInt(context, PREFERENCE_SEND_FROM_INDEX, sendFromIndex);
    }

    public TLBlockExplorerAPI.TLBlockExplorer getBlockExplorerAPI() {
        String enumStr = getKeyString(context, PREFERENCE_BLOCKEXPLORER_API);
        if (enumStr != null) {
            return TLBlockExplorerAPI.TLBlockExplorer.toMyEnum(enumStr);
        } else {
            return TLBlockExplorerAPI.TLBlockExplorer.Blockchain;
        }
    }

    public boolean setBlockExplorerAPI(TLBlockExplorerAPI.TLBlockExplorer blockExplorerIdx) {
        return setKeyString(context, PREFERENCE_BLOCKEXPLORER_API, blockExplorerIdx.toString());
    }

    public String getBlockExplorerURL(TLBlockExplorerAPI.TLBlockExplorer blockExplorer) {
        try {
            JSONObject blockExplorer2blockExplorerURLDict = new JSONObject(getKeyString(context, PREFERENCE_BLOCKEXPLORER_API_URL_DICT));
            return blockExplorer2blockExplorerURLDict.getString(blockExplorer.toString());
        } catch (JSONException e) {
            return null;
        }
    }

    public boolean setBlockExplorerURL(TLBlockExplorerAPI.TLBlockExplorer blockExplorer, String value) {
        assert(blockExplorer != TLBlockExplorerAPI.TLBlockExplorer.Insight);
        try {
            JSONObject blockExplorer2blockExplorerURLDict = new JSONObject(getKeyString(context, PREFERENCE_BLOCKEXPLORER_API_URL_DICT));
            blockExplorer2blockExplorerURLDict.put(blockExplorer.toString(), value);
            return setKeyString(context, PREFERENCE_BLOCKEXPLORER_API_URL_DICT, blockExplorer2blockExplorerURLDict.toString());
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean resetBlockExplorerAPIURL() {
        JSONObject blockExplorer2blockExplorerURLDict = new JSONObject();
        try {
            blockExplorer2blockExplorerURLDict.put(TLBlockExplorerAPI.TLBlockExplorer.Blockchain.toString(), "https://blockchain.info/");
            blockExplorer2blockExplorerURLDict.put(TLBlockExplorerAPI.TLBlockExplorer.Insight.toString(), "https://insight.bitpay.com/");
            return setKeyString(context, PREFERENCE_BLOCKEXPLORER_API_URL_DICT, blockExplorer2blockExplorerURLDict.toString());
        } catch (JSONException e) {
            return false;
        }
    }

    public String getStealthExplorerURL() {
        return getKeyString(context, STEALTH_EXPLORER_URL);
    }

    public boolean setStealthExplorerURL(String value) {
        return setKeyString(context, STEALTH_EXPLORER_URL, value);
    }

    public int getStealthServerPort() {
        return getKeyInt(context, STEALTH_SERVER_PORT);
    }

    public boolean setStealthServerPort(int value) {
        return setKeyInt(context, STEALTH_SERVER_PORT, value);
    }

    public int getStealthWebSocketPort() {
        return getKeyInt(context, STEALTH_WEB_SOCKET_PORT);
    }

    public boolean setStealthWebSocketPort(int value) {
        return setKeyInt(context, STEALTH_WEB_SOCKET_PORT, value);
    }

    public boolean resetStealthExplorerAPIURL() {
        return setStealthExplorerURL(TLAppDelegate.instance().stealthServerConfig.getStealthServerUrl());
    }

    public boolean resetStealthServerPort() {
        return setStealthServerPort(TLAppDelegate.instance().stealthServerConfig.getStealthServerPort());
    }

    public boolean resetStealthWebSocketPort() {
        return setStealthWebSocketPort(TLAppDelegate.instance().stealthServerConfig.getWebSocketServerPort());
    }

    public long getTransactionFee() {
        return getKeyLong(context, FIXED_TRANSACTION_FEE);
    }

    public boolean setTransactionFee(long satoshis) {
        return setKeyLong(context, FIXED_TRANSACTION_FEE, satoshis);
    }

    public TLTxFeeAPI.TLDynamicFeeSetting getDynamicFeeOption() {
        String enumStr = getKeyString(context, DYNAMIC_FEE_OPTION);
        if (enumStr != null) {
            return TLTxFeeAPI.TLDynamicFeeSetting.toMyEnum(enumStr);
        } else {
            return TLTxFeeAPI.TLDynamicFeeSetting.FastestFee;
        }
    }

    public boolean setDynamicFeeOption(TLTxFeeAPI.TLDynamicFeeSetting dynamicFeeOptionIdx) {
        return setKeyString(context, DYNAMIC_FEE_OPTION, dynamicFeeOptionIdx.toString());
    }


    public String getEncryptedWalletPassphraseKey() {
        return getKeyString(context, PREFERENCE_ENCRYPTED_BACKUP_PASSPHRASE_KEY);
    }

    public boolean setEncryptedWalletPassphraseKey(String value)  {
        return setKeyString(context, PREFERENCE_ENCRYPTED_BACKUP_PASSPHRASE_KEY, value);
    }

    public boolean clearEncryptedWalletPassphraseKey() {
        return removeKeyString(context, PREFERENCE_ENCRYPTED_BACKUP_PASSPHRASE_KEY);
    }

    public String getEncryptedWalletJSONChecksum() {
        return getKeyString(context, PREFERENCE_ENCRYPTED_WALLET_JSON_CHECKSUM);
    }

    public boolean setEncryptedWalletJSONChecksum(String value) {
        return setKeyString(context, PREFERENCE_ENCRYPTED_BACKUP_PASSPHRASE_KEY, value);
    }

    public Date getLastSavedEncryptedWalletJSONDate() {
        long time = getKeyLong(context, PREFERENCE_LAST_SAVED_ENCRYPTED_WALLET_JSON_DATE);
        if (time > 0) {
            return new Date(time);
        }
        return null;
    }

    public boolean setLastSavedEncryptedWalletJSONDate(Date value) {
        return setKeyLong(context, PREFERENCE_LAST_SAVED_ENCRYPTED_WALLET_JSON_DATE, value.getTime());

    }

    public boolean isDisplayLocalCurrency() {
        return getKeyBool(context, PREFERENCE_DISPLAY_LOCAL_CURRENCY);
    }

    public boolean setDisplayLocalCurrency(boolean value) {
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED));
        return setKeyBool(context, PREFERENCE_DISPLAY_LOCAL_CURRENCY, value);
    }

    public boolean enabledColdWallet() {
        return getKeyBool(context, PREFERENCE_WALLET_COLD_WALLET);
    }

    public boolean setEnableColdWallet(boolean value) {
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_TOGGLED_COLD_WALLET));
        return setKeyBool(context, PREFERENCE_WALLET_COLD_WALLET, value);
    }

    public boolean enabledAdvancedMode() {
        return getKeyBool(context, PREFERENCE_WALLET_ADVANCE_MODE);
    }

    public boolean setAdvancedMode(boolean value) {
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_ADVANCE_MODE_TOGGLED));
        return setKeyBool(context, PREFERENCE_WALLET_ADVANCE_MODE, value);
    }

    public boolean isEnablePINCode() {
        return getKeyBool(context, PREFERENCE_ENABLE_PIN_CODE);
    }

    public boolean setEnablePINCode(boolean value) {
        return setKeyBool(context, PREFERENCE_ENABLE_PIN_CODE, value);
    }

    public boolean enabledDynamicFee() {
        return getKeyBool(context, PREFERENCE_AUTOMATIC_FEE);
    }

    public boolean setEnabledDynamicFee(boolean value) {
        return setKeyBool(context, PREFERENCE_AUTOMATIC_FEE, value);
    }

    public boolean enabledStealthAddressDefault() {
        return getKeyBool(context, PREFERENCE_ENABLE_STEALTH_ADDRESS_DEFAULT);
    }

    public boolean setEnabledStealthAddressDefault(boolean value) {
        return setKeyBool(context, PREFERENCE_ENABLE_STEALTH_ADDRESS_DEFAULT, value);
    }

    public boolean disabledPromptShowWebWallet() {
        return getKeyBool(context, PREFERENCE_ENABLED_PROMPT_SHOW_WEB_WALLET);
    }

    public boolean setDisabledPromptShowWebWallet(boolean disabled) {
        return setKeyBool(context, PREFERENCE_ENABLED_PROMPT_SHOW_WEB_WALLET, disabled);
    }

    public boolean disabledPromptShowTryColdWallet() {
        return getKeyBool(context, PREFERENCE_ENABLED_PROMPT_SHOW_TRY_COLD_WALLET);
    }

    public boolean setDisabledPromptShowTryColdWallet(boolean disabled) {
        return setKeyBool(context, PREFERENCE_ENABLED_PROMPT_SHOW_TRY_COLD_WALLET, disabled);
    }

    public boolean disabledPromptRateApp() {
        return getKeyBool(context, PREFERENCE_ENABLED_PROMPT_RATE_APP);
    }

    public boolean setDisabledPromptRateApp(boolean disabled) {
        return setKeyBool(context, PREFERENCE_ENABLED_PROMPT_RATE_APP, disabled);
    }

    public boolean hasRatedOnce() {
        return getKeyBool(context, PREFERENCE_RATED_ONCE);
    }

    public boolean setHasRatedOnce() {
        return setKeyBool(context, PREFERENCE_RATED_ONCE, true);
    }




    // suggestions
    public boolean disabledShowHiddenOverlayWarning() {
        return getKeyBool(context, DISABLE_SHOW_HIDDEN_OVERLAY_WARNING);
    }

    public boolean setDisabledShowHiddenOverlayWarning(boolean disabled) {
        return setKeyBool(context, DISABLE_SHOW_HIDDEN_OVERLAY_WARNING, disabled);
    }

    public boolean disabledShowIsRootedWarning() {
        return getKeyBool(context, DISABLE_SHOW_IS_ROOTED_WARNING);
    }

    public boolean setDisabledShowIsRootedWarning(boolean disabled) {
        return setKeyBool(context, DISABLE_SHOW_IS_ROOTED_WARNING, disabled);
    }

    public boolean disabledShowLowAndroidVersionWarning() {
        return getKeyBool(context, DISABLE_SHOW_LOW_ANDROID_VERSION_WARNING);
    }

    public boolean setDisabledShowLowAndroidVersionWarning(boolean disabled) {
        return setKeyBool(context, DISABLE_SHOW_LOW_ANDROID_VERSION_WARNING, disabled);
    }

    public boolean disabledShowStealthPaymentNote() {
        return getKeyBool(context, DISABLE_SUGGEST_STEALTH_PAYMENT_NOTE);
    }

    public boolean setDisableShowStealthPaymentNote(boolean disabled) {
        return setKeyBool(context, DISABLE_SUGGEST_STEALTH_PAYMENT_NOTE, disabled);
    }

    public boolean disabledShowStealthPaymentDelayInfo() {
        return getKeyBool(context, DISABLE_SUGGEST_STEALTH_PAYMENT_DELAY_INFO);
    }

    public boolean setDisableShowStealthPaymentDelayInfo(boolean disabled) {
        return setKeyBool(context, DISABLE_SUGGEST_STEALTH_PAYMENT_DELAY_INFO, disabled);
    }

    public boolean disabledShowFeeExplanationInfo() {
        return getKeyBool(context, DISABLE_SUGGEST_SHOW_FEE_EXPLANATION_INFO);
    }

    public boolean setDisableShowFeeExplanationInfo(boolean disabled) {
        return setKeyBool(context, DISABLE_SUGGEST_SHOW_FEE_EXPLANATION_INFO, disabled);
    }

    public boolean disabledShowManuallyScanTransactionForStealthTxInfo() {
        return getKeyBool(context, DISABLE_SUGGEST_MANUALLY_SCAN_TRANSACTION_FOR_STEALTH_TX_INFO);
    }

    public boolean setDisableShowManuallyScanTransactionForStealthTxInfo(boolean enabled) {
        return setKeyBool(context, DISABLE_SUGGEST_MANUALLY_SCAN_TRANSACTION_FOR_STEALTH_TX_INFO, enabled);
    }

    public boolean setDisableSuggestedEnablePin(boolean enabled) {
        return setKeyBool(context, DISABLE_SUGGEST_ENABLE_PIN, enabled);
    }

    public boolean disabledSuggestedEnablePin() {
        return getKeyBool(context, DISABLE_SUGGEST_ENABLE_PIN);
    }

    public boolean setDisableSuggestedBackUpWalletPassphrase(boolean enabled) {
        return setKeyBool(context, DISABLE_SUGGEST_BACKUP_WALLET_PASSPHRASE, enabled);
    }

    public boolean disabledSuggestedBackUpWalletPassphrase() {
        return getKeyBool(context, DISABLE_SUGGEST_BACKUP_WALLET_PASSPHRASE);
    }

    public boolean setDisableSuggestDontManageIndividualAccountAddress(boolean enabled)  {
        return setKeyBool(context, DISABLE_SUGGEST_DONT_MANAGE_INDIVIDUAL_ACCOUNT_ADDRESSES, enabled);
    }

    public boolean disabledSuggestDontManageIndividualAccountAddress() {
        return getKeyBool(context, DISABLE_SUGGEST_DONT_MANAGE_INDIVIDUAL_ACCOUNT_ADDRESSES);
    }


    public boolean setDisableSuggestDontManageIndividualAccountPrivateKeys(boolean enabled)  {
        return setKeyBool(context, DISABLE_SUGGEST_DONT_MANAGE_INDIVIDUAL_ACCOUNT_PRIVATE_KEYS, enabled);
    }

    public boolean disabledSuggestDontManageIndividualAccountPrivateKeys() {
        return getKeyBool(context, DISABLE_SUGGEST_DONT_MANAGE_INDIVIDUAL_ACCOUNT_PRIVATE_KEYS);
    }

    public boolean setDisableSuggestDontAddNormalAddressToAddressBook(boolean enabled)  {
        return setKeyBool(context, DISABLE_SUGGEST_DONT_ADD_falseRMAL_ADDRESS_TO_ADDRESS_BOOK, enabled);
    }

    public boolean disabledSuggestDontAddNormalAddressToAddressBook() {
        return getKeyBool(context, DISABLE_SUGGEST_DONT_ADD_falseRMAL_ADDRESS_TO_ADDRESS_BOOK);
    }






    public String getValue(String name, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(name, (value == null || value.length() < 1) ? "" : value);
    }

    public boolean setValue(String name, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(name, (value == null || value.length() < 1) ? "" : value);
        return editor.commit();
    }

    public int getValue(String name, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(name, 0);
    }

    public boolean setValue(String name, int value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(name, (value < 0) ? 0 : value);
        return editor.commit();
    }

    public boolean setValue(String name, long value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(name, (value < 0L) ? 0L : value);
        return editor.commit();
    }

    public long getValue(String name, long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long result = 0l;
        try {
            result = prefs.getLong(name, 0L);
        } catch (Exception e) {
            result = (long) prefs.getInt(name, 0);
        }

        return result;
    }

    public boolean getValue(String name, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(name, value);
    }

    public boolean setValue(String name, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(name, value);
        return editor.commit();
    }

    public boolean has(String name) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains(name);
    }

    public boolean removeValue(String name) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(name);
        return editor.commit();
    }

    public boolean clear() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.clear();
        return editor.commit();
    }
}
