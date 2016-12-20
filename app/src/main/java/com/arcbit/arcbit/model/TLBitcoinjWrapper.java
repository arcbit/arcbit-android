package com.arcbit.arcbit.model;

import android.support.v4.util.Pair;
//import com.sun.tools.javac.util.Pair;
//import android.util.Pair;
// must use below cuz of http://stackoverflow.com/questions/33502939/android-util-pair-holds-string-as-parameters-in-androidtest-but-null-in-unit

import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

import static android.R.id.message;
import static org.bitcoinj.core.Utils.HEX;
import static org.bitcoinj.core.Utils.reverseBytes;
import org.bitcoinj.core.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TLBitcoinjWrapper {
    public static NetworkParameters getNetwork(Boolean isTestnet) {
        if (isTestnet) {
            return TestNet3Params.get();
        } else {
            return MainNetParams.get();
        }
    }

    public static String reverseHexString(String hexString) {
        return HEX.encode(reverseBytes(HEX.decode(hexString)));
    }

    public static Pair<String, Long> getAddressAndAmountFromURI(String uri) {
        try {
            BitcoinURI bitcoinURI = new BitcoinURI(MainNetParams.get(), uri);
            Coin amount = bitcoinURI.getAmount();
            return new Pair<String, Long>(bitcoinURI.getAddress().toString(), amount != null ? amount.longValue() : null);
        } catch (BitcoinURIParseException e) {
            return null;
        }
    }

    public static String getNewPrivateKey(Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        ECKey key = new ECKey();
        return key.getPrivateKeyAsWiF(params);
    }

    public static String getAddressFromOutputScript(String scriptHex, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        Script script = new Script(HEX.decode(scriptHex));
        Address addr = new Address(params, script.getPubKeyHash());
        return addr.toString();
    }

    public static String getStandardPubKeyHashScriptFromAddress(String address, Boolean isTestnet) {
        try {
            NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
            Address addr = Address.fromBase58(params, address);
            Script script = ScriptBuilder.createOutputScript(addr);
            return HEX.encode(script.getProgram());
        } catch (AddressFormatException name) {
            return null;
        }
    }

    public static String getAddress(String privateKey, Boolean isTestnet) {
        try {
            NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
            ECKey key = DumpedPrivateKey.fromBase58(params, privateKey).getKey();
            Address addr = key.toAddress(params);
            return addr.toString();
        } catch (AddressFormatException name) {
            return null;
        }
    }

    public static String getAddressFromPublicKey(String publicKey, Boolean isTestnet) {
        ECKey addressKey = ECKey.fromPublicOnly(HEX.decode(publicKey));
        return addressKey.toAddress(TLBitcoinjWrapper.getNetwork(isTestnet)).toString();
    }

    public static String getAddressFromSecret(String secret, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        ECKey key = ECKey.fromPrivate(new BigInteger(secret, 16));
        Address addr = key.toAddress(params);
        return addr.toString();
    }

    public static Boolean isBIP38EncryptedKey(String privateKey, Boolean isTestnet) {
        try {
            BIP38PrivateKey.fromBase58(TLBitcoinjWrapper.getNetwork(isTestnet), privateKey);
            return true;
        } catch (AddressFormatException name) {
            return false;
        }
    }

    public static String privateKeyFromEncryptedPrivateKey(String encryptedPrivateKey, String password, Boolean isTestnet) {
        try {
            if (password == null || password.isEmpty()) {
                return null;
            }
            NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
            BIP38PrivateKey bip38 = BIP38PrivateKey.fromBase58(params, encryptedPrivateKey);
            final ECKey key = bip38.decrypt(password);
            return key.getPrivateKeyAsWiF(params);
        } catch (AddressFormatException e) {
            e.printStackTrace();
            return null;
        } catch (BIP38PrivateKey.BadPassphraseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String privateKeyFromSecret(String secret, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        ECKey key = ECKey.fromPrivate(new BigInteger(secret, 16));
        return key.getPrivateKeyAsWiF(params);
    }

    public static Boolean isAddressVersion0(String address, Boolean isTestnet) {
        try {
            NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
            Address addr = Address.fromBase58(params, address);
            return address.charAt(0) == '1';
        } catch (AddressFormatException name) {
            return false;
        }
    }

    public static Boolean isValidAddress(String address, Boolean isTestnet) {
        try {
            NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
            Address addr = Address.fromBase58(params, address);
            return true;
        } catch (AddressFormatException name) {
            if (TLStealthAddress.isStealthAddress(address, isTestnet)) {
                return true;
            }
            return false;
        }
    }

    public static Boolean isValidPrivateKey(String privateKey, Boolean isTestnet) {
        try {
            NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
            ECKey key = DumpedPrivateKey.fromBase58(params, privateKey).getKey();
            return true;
        } catch (AddressFormatException name) {
            return false;
        }
    }

    public static String getSignature(String privateKey, String message, Boolean isTestnet) {
        ECKey key = ECKey.fromPrivate(new BigInteger(privateKey, 16));
        return key.signMessage(message);
    }

    public static Boolean verifySignature(String address, String signature, String message, Boolean isTestnet) {
        try {
            Address expectedAddress = Address.fromBase58(MainNetParams.get(), address);
            ECKey key = ECKey.signedMessageToKey(message, signature);
            NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
            Address gotAddress = key.toAddress(params);
            return expectedAddress.toString().equals(gotAddress.toString());
        } catch (AddressFormatException name) {
            return false;
        } catch (SignatureException name) {
            return false;
        }

    }
    public static CreatedTransactionObject createSignedSerializedTransactionHex(List<String> hashes, List<Integer> inputIndexes,
                                                                           List<String> inputScripts, List<String> outputAddresses,
                                                                           List<Long> amounts, List<String> privateKeys,
                                                                           List<String> outputScripts, Boolean isTestnet) {
        return createSignedSerializedTransactionHex(hashes, inputIndexes, inputScripts, outputAddresses,
                amounts, privateKeys, outputScripts, true, isTestnet);
    }

    public static CreatedTransactionObject createSignedSerializedTransactionHex(List<String> hashes, List<Integer> inputIndexes,
                                                                           List<String> inputScripts, List<String> outputAddresses,
                                                                           List<Long> amounts, List<String> privateKeys,
                                                                           List<String> outputScripts, Boolean signTx, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        Transaction tx = new Transaction(params);

        if (outputScripts != null) {
            for (int i = 0; i < outputScripts.size(); i++) {
                String outputScript = outputScripts.get(i);
//                Script script = ScriptBuilder.createOpReturnScript(new byte[0]);
                tx.addOutput(Coin.ZERO, new Script(HEX.decode(outputScript)));
            }
        }
        try {
            for (int i = 0; i < outputAddresses.size(); i++) {
                String address = outputAddresses.get(i);
                long amount = amounts.get(i);
                Address addr = Address.fromBase58(params, address);
                tx.addOutput(Coin.valueOf(amount), addr);
//                tx.addOutput(new TransactionOutput(params, null, Coin.COIN, addr));
            }

        } catch (AddressFormatException name) {
            return null;
        }

        JSONArray inputHexScripts = new JSONArray();
        for (int i = 0; i < hashes.size(); i++) {
            String hash = hashes.get(i);
            hash = TLBitcoinjWrapper.reverseHexString(hash);
            Integer inputIndex = inputIndexes.get(i);
            String inputScript = inputScripts.get(i);
            Script script = new Script(HEX.decode(inputScript));
            tx.addInput(Sha256Hash.wrap(hash), inputIndex, script);
            if (!signTx) {
                inputHexScripts.put(inputScript);
            }
        }
        if (!signTx) {
            CreatedTransactionObject createdTransactionObject = new CreatedTransactionObject();
            createdTransactionObject.inputHexScripts = inputHexScripts;
            createdTransactionObject.txHex = HEX.encode(tx.bitcoinSerialize());
            return createdTransactionObject;
        }

        for (int i = 0; i < hashes.size(); i++) {
            String inputScript = inputScripts.get(i);
            Script scriptPubKey = new Script(HEX.decode(inputScript));
            String privateKey = privateKeys.get(i);
            TransactionInput input = tx.getInput(i);
            try {
                ECKey key = DumpedPrivateKey.fromBase58(params, privateKey).getKey();
                TransactionSignature txSig = tx.calculateSignature(i, key, scriptPubKey, Transaction.SigHash.ALL, false);
                input.setScriptSig(ScriptBuilder.createInputScript(txSig, key));
                if (scriptPubKey.isSentToRawPubKey())
                    input.setScriptSig(ScriptBuilder.createInputScript(txSig));
                else if (scriptPubKey.isSentToAddress())
                    input.setScriptSig(ScriptBuilder.createInputScript(txSig, key));
                else
                    throw new ScriptException("Don't know how to sign for this kind of scriptPubKey: " + scriptPubKey);
            } catch (AddressFormatException name) {
                return null;
            }
        }
        CreatedTransactionObject createdTransactionObject = new CreatedTransactionObject();
        createdTransactionObject.txHash = tx.getHashAsString();
        createdTransactionObject.txHex = HEX.encode(tx.bitcoinSerialize());
        //FIXME getMessageSize does not match iOS, its ok because only use it to calculate dynamic fees
        createdTransactionObject.txSize = tx.getMessageSize();
        return createdTransactionObject;
    }

    public static JSONObject createSignedSerializedTransactionHex(byte[] unsignedTx, List<String> inputScripts,
                                                                  List<String> privateKeys, Boolean isTestnet) {
        NetworkParameters params = TLBitcoinjWrapper.getNetwork(isTestnet);
        Transaction tx = new Transaction(params, unsignedTx);
        for (int i = 0; i < inputScripts.size(); i++) {
            String inputScript = inputScripts.get(i);
            Script scriptPubKey = new Script(HEX.decode(inputScript));
            String privateKey = privateKeys.get(i);
            TransactionInput input = tx.getInput(i);
            try {
                ECKey key = DumpedPrivateKey.fromBase58(params, privateKey).getKey();
                TransactionSignature txSig = tx.calculateSignature(i, key, scriptPubKey, Transaction.SigHash.ALL, false);
                input.setScriptSig(ScriptBuilder.createInputScript(txSig, key));
                if (scriptPubKey.isSentToRawPubKey())
                    input.setScriptSig(ScriptBuilder.createInputScript(txSig));
                else if (scriptPubKey.isSentToAddress())
                    input.setScriptSig(ScriptBuilder.createInputScript(txSig, key));
                else
                    throw new ScriptException("Don't know how to sign for this kind of scriptPubKey: " + scriptPubKey);
            } catch (AddressFormatException name) {
                return null;
            }
        }
        JSONObject obj = new JSONObject();
        try {
            obj.put("txHex", HEX.encode(tx.bitcoinSerialize()));
            obj.put("txHash", tx.getHashAsString());
            //FIXME getMessageSize does not match iOS, its ok because only use it to calculate dynamic fees
            obj.put("txSize", tx.getMessageSize());
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}

