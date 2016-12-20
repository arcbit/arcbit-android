package com.arcbit.arcbit.model;

import android.util.Base64;

import com.arcbit.arcbit.utils.TLAESCrypt;
import com.arcbit.arcbit.utils.TLCrypto;

public class TLEncryptedPreferences {
    private static final String TAG = TLEncryptedPreferences.class.getName();

    TLAppDelegate appDelegate;
    private static final String PREFERENCE_PIN_VALUE = "pref-pin-value";
    private static final String PREFERENCE_WALLET_PASSPHRASE = "pref-wallet-passphrase";
    private static final String PREFERENCE_ENCRYPTED_WALLET_JSON_PASSPHRASE = "pref-encrypted-wallet-passphrase";

    private static final String PREFERENCE_ENCRYPTED_PIN = "pref-encrypted-pin";
    private static final String PREFERENCE_PIN_ENCRYPTION_IV = "pref-pin-encryption-iv";
    private static final String PREFERENCE_PIN_ENCRYPTION_KEY = "pref-pin-encryption-key";

    private static final String PREFERENCE_ENCRYPTED_WALLET_PASSPHRASE = "pref-encrypted-wallet-passphrase";
    private static final String PREFERENCE_WALLET_PASSPHRASE_ENCRYPTION_IV = "pref-wallet-passphrase-encryption-iv";
    private static final String PREFERENCE_WALLET_PASSPHRASE_ENCRYPTION_KEY = "pref-wallet-passphrase-encryption-key";

    private static final String PREFERENCE_ENCRYPTED_ENCRYPTED_WALLET_JSON_PASSPHRASE = "pref-encrypted-encrypted-wallet-json-passphrase";
    private static final String PREFERENCE_ENCRYPTED_WALLET_JSON_PASSPHRASE_ENCRYPTION_IV = "pref-encrypted-wallet-json-passphrase-encryption-iv";
    private static final String PREFERENCE_ENCRYPTED_WALLET_JSON_PASSPHRASE_ENCRYPTION_KEY = "pref-encrypted-wallet-json-passphrase-encryption-key";

    private static final String PREFERENCE_WALLET_JSON_ENCRYPTION_IV = "pref-encrypted-wallet-json-encryption-iv";


    public TLEncryptedPreferences(TLAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
    }

    public String getPINValue() {
        if (TLKeyStore.canUseKeyStore()) {
            String encryptedValue = appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_PIN_VALUE);
            if (encryptedValue == null || encryptedValue.isEmpty()) {
                return null;
            }
            String value = this.appDelegate.keyStore.decryptString(TLKeyStore.KEY_STORE_WALLET_PASSPHRASE_ALIAS, encryptedValue);
            return value;
        } else {
            String key = this.getPINEncryptionKey();
            byte[] iv = this.getPINEncryptionIV();
            assert (key != null);
            assert (iv != null);
            String encryptedValue = this.getEncryptedPIN();
            return TLCrypto.getInstance().decrypt(encryptedValue, key, iv);
        }
    }
    public boolean setPINValue(String value) {
        if (TLKeyStore.canUseKeyStore()) {
            String encryptedValue = null;
            if (value != null && !value.isEmpty()) {
                encryptedValue = this.appDelegate.keyStore.encryptString(TLKeyStore.KEY_STORE_WALLET_PASSPHRASE_ALIAS, value);
            }
            return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_PIN_VALUE, encryptedValue);
        } else {
            String key = this.getPINEncryptionKey();
            if (key == null) {
                key = generateEncryptionKey();
                this.setPINEncryptionKey(key);
            }
            byte[] iv = this.getPINEncryptionIV();
            if (iv == null) {
                iv = TLAESCrypt.getRandomIV();
                this.setPINEncryptionIV(iv);
            }
            String encryptedValue = TLCrypto.getInstance().encrypt(value, key, iv);
            return this.setEncryptedPIN(encryptedValue);
        }
    }

    public String getWalletPassphrase() {
        if (TLKeyStore.canUseKeyStore()) {
            String encryptedValue = appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_WALLET_PASSPHRASE);
            if (encryptedValue == null || encryptedValue.isEmpty()) {
                return null;
            }
            String value = this.appDelegate.keyStore.decryptString(TLKeyStore.KEY_STORE_WALLET_PASSPHRASE_ALIAS, encryptedValue);
            return value;
        } else {
            String key = this.getWalletPassphraseEncryptionKey();
            byte[] iv = this.getWalletPassphraseEncryptionIV();
            assert (key != null);
            assert (iv != null);
            String encryptedValue = this.getEncryptedWalletPassphrase();
            return TLCrypto.getInstance().decrypt(encryptedValue, key, iv);
        }
    }
    public boolean setWalletPassphrase(String value) {
        if (TLKeyStore.canUseKeyStore()) {
            String encryptedValue = null;
            if (value != null && !value.isEmpty()) {
                encryptedValue = this.appDelegate.keyStore.encryptString(TLKeyStore.KEY_STORE_WALLET_PASSPHRASE_ALIAS, value);
            }
            return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_WALLET_PASSPHRASE, encryptedValue);
        } else {
            String key = this.getWalletPassphraseEncryptionKey();
            if (key == null) {
                key = generateEncryptionKey();
                this.setWalletPassphraseEncryptionKey(key);
            }
            byte[] iv = this.getWalletPassphraseEncryptionIV();
            if (iv == null) {
                iv = TLAESCrypt.getRandomIV();
                this.setWalletPassphraseEncryptionIV(iv);
            }
            String encryptedValue = TLCrypto.getInstance().encrypt(value, key, iv);
            return this.setEncryptedWalletPassphrase(encryptedValue);
        }
    }

    public String getWalletJSONPassphrase() {
        if (TLKeyStore.canUseKeyStore()) {
            String encryptedValue = appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_WALLET_JSON_PASSPHRASE);
            if (encryptedValue == null || encryptedValue.isEmpty()) {
                return null;
            }
            String value = this.appDelegate.keyStore.decryptString(TLKeyStore.KEY_STORE_WALLET_PASSPHRASE_ALIAS, encryptedValue);
            return value;
        } else {
            String key = this.getEncryptedWalletJSONPassphraseEncryptionKey();
            byte[] iv = this.getEncryptedWalletJSONPassphraseEncryptionIV();
            assert (key != null);
            assert (iv != null);
            String encryptedValue = this.getEncryptedWalletJSONPassphrase();
            return TLCrypto.getInstance().decrypt(encryptedValue, key, iv);
        }
    }
    public boolean setWalletJSONPassphrase(String value) {
        if (TLKeyStore.canUseKeyStore()) {
            String encryptedValue = null;
            if (value != null && !value.isEmpty()) {
                encryptedValue = this.appDelegate.keyStore.encryptString(TLKeyStore.KEY_STORE_WALLET_PASSPHRASE_ALIAS, value);
            }
            return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_WALLET_JSON_PASSPHRASE, encryptedValue);
        } else {
            String key = this.getEncryptedWalletJSONPassphraseEncryptionKey();
            if (key == null) {
                key = generateEncryptionKey();
                this.setEncryptedWalletJSONPassphraseEncryptionKey(key);
            }
            byte[] iv = this.getEncryptedWalletJSONPassphraseEncryptionIV();
            if (iv == null) {
                iv = TLAESCrypt.getRandomIV();
                this.setEncryptedWalletJSONPassphraseEncryptionIV(iv);
            }
            String encryptedValue = TLCrypto.getInstance().encrypt(value, key, iv);
            return this.setEncryptedWalletJSONPassphrase(encryptedValue);
        }
    }


    public String generateEncryptionKey() {
        return TLBitcoinjWrapper.getNewPrivateKey(this.appDelegate.appWallet.walletConfig.isTestnet);
    }


    private String getEncryptedPIN() {
        return appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_PIN);
    }
    private boolean setEncryptedPIN(String value) {
        return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_PIN, value);
    }
    private String getPINEncryptionKey() {
        return appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_PIN_ENCRYPTION_KEY);
    }
    private boolean setPINEncryptionKey(String value) {
        return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_PIN_ENCRYPTION_KEY, value);
    }
    private byte[] getPINEncryptionIV() {
        String ivBase64 = appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_PIN_ENCRYPTION_IV);
        if (ivBase64 != null) {
            return Base64.decode(ivBase64, Base64.DEFAULT);
        }
        return null;
    }
    private boolean setPINEncryptionIV(byte[] value) {
        return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_PIN_ENCRYPTION_IV, Base64.encodeToString(value, Base64.DEFAULT));
    }

    private String getEncryptedWalletPassphrase() {
        return appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_WALLET_PASSPHRASE);
    }
    private boolean setEncryptedWalletPassphrase(String value) {
        return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_WALLET_PASSPHRASE, value);
    }
    private String getWalletPassphraseEncryptionKey() {
        return appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_WALLET_PASSPHRASE_ENCRYPTION_KEY);
    }
    private boolean setWalletPassphraseEncryptionKey(String value) {
        return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_WALLET_PASSPHRASE_ENCRYPTION_KEY, value);
    }
    private byte[] getWalletPassphraseEncryptionIV() {
        String ivBase64 = appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_WALLET_PASSPHRASE_ENCRYPTION_IV);
        if (ivBase64 != null) {
            return Base64.decode(ivBase64, Base64.DEFAULT);
        }
        return null;
    }
    private boolean setWalletPassphraseEncryptionIV(byte[] value) {
        return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_WALLET_PASSPHRASE_ENCRYPTION_IV, Base64.encodeToString(value, Base64.DEFAULT));
    }

    private String getEncryptedWalletJSONPassphrase() {
        return appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_ENCRYPTED_WALLET_JSON_PASSPHRASE);
    }
    private boolean setEncryptedWalletJSONPassphrase(String value) {
        return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_ENCRYPTED_WALLET_JSON_PASSPHRASE, value);
    }
    private String getEncryptedWalletJSONPassphraseEncryptionKey() {
        return appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_WALLET_JSON_PASSPHRASE_ENCRYPTION_KEY);
    }
    private boolean setEncryptedWalletJSONPassphraseEncryptionKey(String value) {
        return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_WALLET_JSON_PASSPHRASE_ENCRYPTION_KEY, value);
    }
    private byte[] getEncryptedWalletJSONPassphraseEncryptionIV() {
        String ivBase64 = appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_WALLET_JSON_PASSPHRASE_ENCRYPTION_IV);
        if (ivBase64 != null) {
            return Base64.decode(ivBase64, Base64.DEFAULT);
        }
        return null;
    }
    private boolean setEncryptedWalletJSONPassphraseEncryptionIV(byte[] value) {
        return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_ENCRYPTED_WALLET_JSON_PASSPHRASE_ENCRYPTION_IV, Base64.encodeToString(value, Base64.DEFAULT));
    }

    public byte[] getWalletJSONEncryptionIV() {
        String ivBase64 = appDelegate.preferences.getKeyString(appDelegate.context, PREFERENCE_WALLET_JSON_ENCRYPTION_IV);
        if (ivBase64 != null) {
            return Base64.decode(ivBase64, Base64.DEFAULT);
        }
        return null;
    }
    public boolean setWalletJSONEncryptionIV(byte[] value) {
        return appDelegate.preferences.setKeyString(appDelegate.context, PREFERENCE_WALLET_JSON_ENCRYPTION_IV, Base64.encodeToString(value, Base64.DEFAULT));
    }
}
