package com.arcbit.arcbit.model;

import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import static org.bitcoinj.core.Utils.HEX;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Address;

public class TLHDWalletWrapper {
    private static final int entropyBitLength = 128; // 12 word mnemonic
    //private static final int entropyBitLength = 64; // 6 word mnemonic

    public static DeterministicKey getBIP44KeyChain(String masterHex, Integer accountIdx, NetworkParameters params) {
        try {
            DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(HEX.decode(masterHex));
            DeterministicKey purposeKey = HDKeyDerivation.deriveChildKey(masterPrivateKey, new ChildNumber(44, true));
            DeterministicKey coinTypeKey = HDKeyDerivation.deriveChildKey(purposeKey, new ChildNumber(0, true));
            DeterministicKey accountKey = HDKeyDerivation.deriveChildKey(coinTypeKey, new ChildNumber(accountIdx, true));
            return accountKey;
        } catch (IllegalArgumentException name) {
            return null;
        }
    }

    public static String generateMnemonicPassphrase() {
        try {
            SecureRandom secureRandom = new SecureRandom();
            String entropy = new BigInteger(entropyBitLength, secureRandom).toString(16);
            while (entropy.length() < 32) {
                entropy = '0' + entropy;
            }
            MnemonicCode mc = new MnemonicCode();
            List<String> code = mc.toMnemonic(HEX.decode(entropy));
            String mnemonic = StringUtils.join(code, " ");
            return mnemonic;
        } catch (IOException name) {
            return null;
        } catch (MnemonicException name) {
            return null;
        }
    }

    private static List<String> split(String words) {
        return new ArrayList<String>(Arrays.asList(words.split("\\s+")));
    }

    public static Boolean phraseIsValid(String phrase) {
        try {
            MnemonicCode mc = new MnemonicCode();
            byte[] entropy = mc.toEntropy(split(phrase));
            return true;
        } catch (IOException name) {
            return false;
        } catch (MnemonicException name) {
            return false;
        }
    }

    public static String getMasterHex(String mnemonic) {
        try {
            MnemonicCode mc = new MnemonicCode();
            byte[] entropy = mc.toSeed(split(mnemonic), "");
            return HEX.encode(entropy);
        } catch (IOException name) {
        } catch (Exception e) {
        }
        return null;
    }

    public static HashMap<String, String> getStealthAddress(String extendedKey, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);

        DeterministicKey scanKeyChain = DeterministicKey.deserializeB58(null, extendedKey, params);
        ArrayList<ChildNumber> scanPrivSequence =
                new ArrayList<ChildNumber>(Arrays.asList(new ChildNumber(100, true), new ChildNumber(0, false)));
        for (int i = 0; i < scanPrivSequence.size(); i++) {
            ChildNumber idxHardened = scanPrivSequence.get(i);
            scanKeyChain = HDKeyDerivation.deriveChildKey(scanKeyChain, idxHardened);
        }

        DeterministicKey spendKeyChain = DeterministicKey.deserializeB58(null, extendedKey, params);
        ArrayList<ChildNumber> spendPrivSequence =
                new ArrayList<ChildNumber>(Arrays.asList(new ChildNumber(100, true), new ChildNumber(1, false)));
        for (int i = 0; i < spendPrivSequence.size(); i++) {
            ChildNumber idxHardened = spendPrivSequence.get(i);
            spendKeyChain = HDKeyDerivation.deriveChildKey(spendKeyChain, idxHardened);
        }

        String scanPriv = scanKeyChain.getPrivateKeyAsHex();
        String scanPublicKey = scanKeyChain.getPublicKeyAsHex();

        String spendPriv = spendKeyChain.getPrivateKeyAsHex();
        String spendPublicKey = spendKeyChain.getPublicKeyAsHex();

        String stealthAddress = TLStealthAddress.createStealthAddress(scanPublicKey, spendPublicKey, isTestnet);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("stealthAddress", stealthAddress);
        map.put("scanPriv", scanPriv);
        map.put("spendPriv", spendPriv);
        return map;
    }

    public static Integer getAccountIdxForExtendedKey(String extendedKey, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        DeterministicKey key = DeterministicKey.deserializeB58(null, extendedKey, params);
        return key.getChildNumber().num();
    }

    public static Boolean isValidExtendedPublicKey(String extendedPublicKey, Boolean isTestnet) {
        try {
            NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
            DeterministicKey key = DeterministicKey.deserializeB58(null, extendedPublicKey, params);
            return key.isPubKeyOnly();
        } catch (IllegalArgumentException name) {
            return false;
        }
    }

    public static Boolean isValidExtendedPrivateKey(String extendedPrivateKey, Boolean isTestnet) {
        try {
            NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
            DeterministicKey key = DeterministicKey.deserializeB58(null, extendedPrivateKey, params);
            return !key.isPubKeyOnly();
        } catch (IllegalArgumentException name) {
            return false;
        }
    }

    public static String getExtendPubKey(String extendPrivKey, Boolean isTestnet) {
        try {
            NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
            DeterministicKey key = DeterministicKey.deserializeB58(null, extendPrivKey, params);
            return key.serializePubB58(params);
        } catch (IllegalArgumentException name) {
            return null;
        }
    }

    public static String getExtendPubKeyFromMasterHex(String masterHex, Integer accountIdx, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        return getBIP44KeyChain(masterHex, accountIdx, params).serializePubB58(params);
    }

    public static String getExtendPrivKey(String masterHex, Integer accountIdx, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        return getBIP44KeyChain(masterHex, accountIdx, params).serializePrivB58(params);
    }

    public static String getAddress(String extendPubKey, List<Integer> sequence, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        DeterministicKey key = DeterministicKey.deserializeB58(null, extendPubKey, params);
        for (int i = 0; i < sequence.size(); i++) {
            key = HDKeyDerivation.deriveChildKey(key, new ChildNumber(sequence.get(i), false));
        }
        ECKey addressKey = ECKey.fromPublicOnly(key.getPubKey());
        Address addr = addressKey.toAddress(params);
        return addr.toString();
    }

    public static String getPrivateKey(String extendPrivKey, List<Integer> sequence, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        DeterministicKey key = DeterministicKey.deserializeB58(null, extendPrivKey, params);
        for (int i = 0; i < sequence.size(); i++) {
            key = HDKeyDerivation.deriveChildKey(key, new ChildNumber(sequence.get(i), false));
        }
        BigInteger privKey = key.getPrivKey();
        ECKey privateKey = ECKey.fromPrivate(privKey);
        return privateKey.getPrivateKeyAsWiF(params);
    }
}
