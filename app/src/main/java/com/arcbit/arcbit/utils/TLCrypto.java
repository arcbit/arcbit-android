package com.arcbit.arcbit.utils;

import org.bitcoinj.core.Sha256Hash;

import static org.bitcoinj.core.Utils.HEX;

public class TLCrypto {
    private static TLCrypto instance = null;

    private TLCrypto() {}

    public static TLCrypto getInstance() {
        if (instance == null) {
            instance = new TLCrypto();
        }
        return instance;
    }

    public String encrypt(String plainText, String password, byte[] iv) {
        try {
            TLAESCrypt crypt = new TLAESCrypt(password, iv);
            return crypt.encrypt(plainText);
        } catch (Exception e) {
            return null;
        }
    }

    public String decrypt(String cipherText, String password, byte[] iv) {
        try {
            TLAESCrypt crypt = new TLAESCrypt(password, iv);
            return crypt.decrypt(cipherText);
        } catch (Exception e) {
            return null;
        }
    }

    public String SHA256HashFor(String input) {
        return HEX.encode(Sha256Hash.hash(input.getBytes()));
    }

    public String doubleSHA256HashFor(String input) {
        return HEX.encode(Sha256Hash.hashTwice(input.getBytes()));
    }
}
