package com.arcbit.arcbit.model;

import android.support.v4.util.Pair;
//import com.sun.tools.javac.util.Pair;
//import android.util.Pair;
// must use below cuz of http://stackoverflow.com/questions/33502939/android-util-pair-holds-string-as-parameters-in-androidtest-but-null-in-unit

import org.bitcoinj.crypto.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.crypto.params.*;

import java.math.BigInteger;

import static org.bitcoinj.core.Utils.HEX;
import static org.bitcoinj.script.ScriptOpCodes.OP_RETURN;
import org.bitcoinj.core.*;


public class TLStealthAddress {
    private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
    public static final ECDomainParameters CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(),
            CURVE_PARAMS.getH());

    public static final byte STEALTH_ADDRESS_MSG_SIZE = 0x26;
    public static final byte STEALTH_ADDRESS_TRANSACTION_VERSION = 0x06;
    public static final byte BTC_MAGIC_BYTE = 0x2a;
    public static final byte BTC_TESTNET_MAGIC_BYTE = 0x2b;

    public static byte getStealthAddressTransacionVersion() {
        return STEALTH_ADDRESS_TRANSACTION_VERSION;
    }

    public static byte getMagicByte(Boolean isTestnet) {
        if (isTestnet) {
            return BTC_TESTNET_MAGIC_BYTE;
        } else {
            return BTC_MAGIC_BYTE;
        }
    }

    public static byte getStealthAddressMsgSize() {
        return STEALTH_ADDRESS_MSG_SIZE;
    }

    public static Boolean isStealthAddress(String stealthAddress, Boolean isTestnet) {
        try {
            byte[] data = Base58.decodeChecked(stealthAddress);
            if(data == null) {
                return false;
            }
            String stealthAddressHex = HEX.encode(data);
            if (stealthAddressHex != null && stealthAddressHex.length() != 142) {
                return false;
            }
            if (data[0] != getMagicByte(isTestnet)) {
                return false;
            }
            String scanPublicKey = stealthAddressHex.substring(4, 4+66);
            String spendPublicKey = stealthAddressHex.substring(72, 72+66);
            String stealthAddr = createStealthAddress(scanPublicKey, spendPublicKey, isTestnet);
            return stealthAddress.equals(stealthAddr);
        } catch (AddressFormatException name) {
            return false;
        }
    }

    public static String createStealthAddress(String scanPublicKey, String spendPublicKey, Boolean isTestnet) {
        String hexString = String.format("%02x00%s01%s0100", getMagicByte(isTestnet), scanPublicKey, spendPublicKey);
        byte[] hashed = Sha256Hash.hashTwice(HEX.decode(hexString));
        return Base58.encode(HEX.decode(hexString+HEX.encode(hashed).substring(0, 8)));
    }

    public static String generateEphemeralPrivkey() {
        ECKey key = new ECKey();
        return key.getPrivKey().toString(16);
    }


    public static Integer generateNonce() {
        ECKey key = new ECKey();
        return java.nio.ByteBuffer.wrap(key.getPrivKey().toByteArray()).getInt();
    }

    public static Pair<String, String> createDataScriptAndPaymentAddress(String stealthAddress, Boolean isTestnet) {
        String ephemeralPrivateKey = generateEphemeralPrivkey();
        Integer nonce = generateNonce();
        return createDataScriptAndPaymentAddress(stealthAddress, ephemeralPrivateKey, nonce, isTestnet);
    }

    public static Pair<String, String> createDataScriptAndPaymentAddress(String stealthAddress, String ephemeralPrivateKey, Integer nonce, Boolean isTestnet) {
        Pair<String, String> publicKeys = getScanPublicKeyAndSpendPublicKey(stealthAddress, isTestnet);
//        String scanPublicKey = publicKeys.fst;
//        String spendPublicKey = publicKeys.snd;
        String scanPublicKey = publicKeys.first;
        String spendPublicKey = publicKeys.second;
        assert(createStealthAddress(scanPublicKey, spendPublicKey, isTestnet).equals(stealthAddress));
        ECKey key = ECKey.fromPrivate(new BigInteger(ephemeralPrivateKey, 16));
        String ephemeralPublicKey = key.getPublicKeyAsHex();
        String stealthDataScript = String.format("%02x%02x%02x%08x%s",
                OP_RETURN,
                getStealthAddressMsgSize(),
                getStealthAddressTransacionVersion(),
                nonce,
                ephemeralPublicKey);

        String paymentPublicKey = getPaymentPublicKeySender(scanPublicKey, spendPublicKey, ephemeralPrivateKey);
        String paymentAddress;
        ECKey addressKey = ECKey.fromPublicOnly(HEX.decode(paymentPublicKey));
        paymentAddress = addressKey.toAddress(isTestnet ? TestNet3Params.get() : MainNetParams.get()).toString();
        return new Pair<String, String>(stealthDataScript, paymentAddress);
    }

    public static String getEphemeralPublicKeyFromStealthDataScript(String scriptHex) {
        if (scriptHex.length() != 80) {
            return null;
        }
        return scriptHex.substring(14);
    }

    public static String getPaymentAddressPrivateKeySecretFromScript(String stealthDataScript, String scanPrivateKey, String spendPrivateKey) {
        String ephemeralPublicKey = getEphemeralPublicKeyFromStealthDataScript(stealthDataScript);
        if (ephemeralPublicKey == null) {
            return null;
        }
        return getPaymentPrivateKey(scanPrivateKey, spendPrivateKey, ephemeralPublicKey);
    }

    public static String getPaymentAddressPublicKeyFromScript(String stealthDataScript, String scanPrivateKey, String spendPrivateKey) {
        String ephemeralPublicKey = getEphemeralPublicKeyFromStealthDataScript(stealthDataScript);
        if (ephemeralPublicKey == null) {
            return null;
        }
        return getPaymentPublicKeyForReceiver(scanPrivateKey, spendPrivateKey, ephemeralPublicKey);
    }

    public static Pair<String, String> getScanPublicKeyAndSpendPublicKey(String stealthAddress, Boolean isTestnet) {
        try {
            byte[] data = Base58.decodeChecked(stealthAddress);
            String stealthAddressHex = HEX.encode(data);
            assert(stealthAddressHex.length() == 142);
            assert(data[0] == getMagicByte(isTestnet));
            String scanPublicKey = stealthAddressHex.substring(4, 4+66);
            String spendPublicKey = stealthAddressHex.substring(72, 72+66);
            return new Pair<String, String>(scanPublicKey, spendPublicKey);
        } catch (AddressFormatException name) {
            return null;
        }
    }

    public static String getSharedSecretForSender(String scanPublicKey, String ephemeralPrivateKey) {
        LazyECPoint scanPublicKeyPoint = new LazyECPoint(CURVE.getCurve(), HEX.decode(scanPublicKey));
        BigInteger ephemeralPrivateKeySecret = new BigInteger(ephemeralPrivateKey, 16);
        ECKey key = ECKey.fromPrivateAndPrecalculatedPublic(null, scanPublicKeyPoint.multiply(ephemeralPrivateKeySecret));
        byte[] hashed = Sha256Hash.hash(key.getPubKey());
        return HEX.encode(hashed);
    }

    public static String getSharedSecretForReceiver(String ephemeralPublicKey, String scanPrivateKey) {
        LazyECPoint ephemeralPublicKeyPoint = new LazyECPoint(CURVE.getCurve(), HEX.decode(ephemeralPublicKey));
        BigInteger scanPrivateKeySecret = new BigInteger(scanPrivateKey, 16);
        ECKey key = ECKey.fromPrivateAndPrecalculatedPublic(null, ephemeralPublicKeyPoint.multiply(scanPrivateKeySecret));
        byte[] hashed = Sha256Hash.hash(key.getPubKey());
        return HEX.encode(hashed);
    }

    public static String getPaymentPublicKeyForReceiver(String scanPrivateKey, String spendPublicKey, String ephemeralPublicKey) {
        String sharedSecret = getSharedSecretForReceiver(ephemeralPublicKey, scanPrivateKey);
        if (sharedSecret == null) {
            return null;
        } else {
            ECKey key = ECKey.fromPrivate(new BigInteger(sharedSecret, 16));
            return addPublicKeys(spendPublicKey, HEX.encode(key.getPubKey()));
        }
    }

    public static String getPaymentPublicKeySender(String scanPublicKey, String spendPublicKey, String ephemeralPrivateKey) {
        String sharedSecret = getSharedSecretForSender(scanPublicKey, ephemeralPrivateKey);
        if (sharedSecret == null) {
            return null;
        } else {
            ECKey key = ECKey.fromPrivate(new BigInteger(sharedSecret, 16));
            return addPublicKeys(spendPublicKey, HEX.encode(key.getPubKey()));
        }
    }

    public static String getPaymentPrivateKey(String scanPrivateKey, String spendPrivateKey, String ephemeralPublicKey) {
        String sharedSecret = getSharedSecretForReceiver(ephemeralPublicKey, scanPrivateKey);
        if (sharedSecret == null) {
            return null;
        } else {
            return addPrivateKeys(spendPrivateKey, sharedSecret);
        }
    }

    public static String addPublicKeys(String lhsPublicKey, String rhsPublicKey) {
        LazyECPoint lhsLazyECPoint = new LazyECPoint(CURVE.getCurve(), HEX.decode(lhsPublicKey));
        LazyECPoint rhsLazyECPoint = new LazyECPoint(CURVE.getCurve(), HEX.decode(rhsPublicKey));
        byte[] encoded = lhsLazyECPoint.add(rhsLazyECPoint.get()).getEncoded();
        return HEX.encode(encoded);
    }

    public static String addPrivateKeys(String lhsPrivateKey, String rhsPrivateKey) {
        BigInteger lhsBigNumber = new BigInteger(lhsPrivateKey, 16);
        BigInteger rhsBigNumber = new BigInteger(rhsPrivateKey, 16);
        BigInteger result = lhsBigNumber.add(rhsBigNumber).mod(CURVE.getN());
        return result.toString(16);
    }
}

