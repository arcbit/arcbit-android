package com.arcbit.arcbit;

import com.arcbit.arcbit.model.CreatedTransactionObject;
import com.arcbit.arcbit.model.TLBitcoinjWrapper;
import android.support.v4.util.Pair;
//import com.sun.tools.javac.util.Pair;
//import android.util.Pair;
// must use below cuz of http://stackoverflow.com/questions/33502939/android-util-pair-holds-string-as-parameters-in-androidtest-but-null-in-unit
import org.bitcoinj.utils.BriefLogFormatter;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TLBitcoinjWrapperTest {
    private static final Logger log = LoggerFactory.getLogger(TLBitcoinjWrapperTest.class);
    static final Boolean networkTestnet = true;
    static final Boolean networkBitcoin = false;

    @Before
    public void setUp() throws Exception {
        BriefLogFormatter.init();
    }

    @Test
    public void testKeyAndAddresses() throws Exception {
        String address = TLBitcoinjWrapper.getAddressFromOutputScript("76a9147ab89f9fae3f8043dcee5f7b5467a0f0a6e2f7e188ac", networkBitcoin);
        assertTrue(address.equals("1CBtcGivXmHQ8ZqdPgeMfcpQNJrqTrSAcG"));

        address = TLBitcoinjWrapper.getAddressFromOutputScript("76a914988cb8253f4e28be6e8bfded1b4aa11c646e1a8588ac", networkTestnet);
        assertTrue(address.equals("muRZaZYTZCDrb68nKQ7Kjk595aj8mmjaKP"));

        String pubKeyHash = TLBitcoinjWrapper.getStandardPubKeyHashScriptFromAddress("1DvKTsUfVaExCD7rBK4nyHt88tYpxU93eD", networkBitcoin);
        assertTrue(pubKeyHash.equals("76a9148db6f7be85dffed461440320f4f779735dacdfdc88ac"));

        String address1 = TLBitcoinjWrapper.getAddress("L4rK1yDtCWekvXuE6oXD9jCYfFNV2cWRpVuPLBcCU2z8TrisoyY1", networkBitcoin);
        assertTrue(address1.equals("1F3sAm6ZtwLAUnj7d38pGFxtP3RVEvtsbV"));
        address1 = TLBitcoinjWrapper.getAddress("5KYZdUEo39z3FPrtuX2QbbwGnNP5zTd7yyr2SC1j299sBCnWjss", networkBitcoin);
        assertTrue(address1.equals("1HZwkjkeaoZfTSaJxDw6aKkxp45agDiEzN"));
        address1 = TLBitcoinjWrapper.getAddress("cVDJUtDjdaM25yNVVDLLX3hcHUfth4c7tY3rSc4hy9e8ibtCuj6G", networkTestnet);
        assertTrue(address1.equals("muZpTpBYhxmRFuCjLc7C6BBDF32C8XVJUi"));
        address1 = TLBitcoinjWrapper.getAddress("93KCDD4LdP4BDTNBXrvKUCVES2jo9dAKKvhyWpNEMstuxDauHty", networkTestnet);
        assertTrue(address1.equals("mx5u3nqdPpzvEZ3vfnuUQEyHg3gHd8zrrH"));

        String address2 = TLBitcoinjWrapper.getAddressFromPublicKey("03a34b99f22c790c4e36b2b3c2c35a36db06226e41c692fc82b8b56ac1c540c5bd", networkBitcoin);
        assertTrue(address2.equals("1F3sAm6ZtwLAUnj7d38pGFxtP3RVEvtsbV"));
        address2 = TLBitcoinjWrapper.getAddressFromPublicKey("03a34b99f22c790c4e36b2b3c2c35a36db06226e41c692fc82b8b56ac1c540c5bd", networkTestnet);
        assertTrue(address2.equals("muZpTpBYhxmRFuCjLc7C6BBDF32C8XVJUi"));

        String address3 = TLBitcoinjWrapper.getAddressFromSecret("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", networkBitcoin);
        assertTrue(address3.equals("1F3sAm6ZtwLAUnj7d38pGFxtP3RVEvtsbV"));
        address3 = TLBitcoinjWrapper.getAddressFromSecret("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", networkTestnet);
        assertTrue(address3.equals("muZpTpBYhxmRFuCjLc7C6BBDF32C8XVJUi"));

        String key = TLBitcoinjWrapper.privateKeyFromSecret("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", networkBitcoin);
        assertTrue(key.equals("L4rK1yDtCWekvXuE6oXD9jCYfFNV2cWRpVuPLBcCU2z8TrisoyY1"));

        key = TLBitcoinjWrapper.privateKeyFromSecret("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", networkTestnet);
        assertTrue(key.equals("cVDJUtDjdaM25yNVVDLLX3hcHUfth4c7tY3rSc4hy9e8ibtCuj6G"));


        Boolean valid = TLBitcoinjWrapper.isValidPrivateKey("L4rK1yDtCWekvXuE6oXD9jCYfFNV2cWRpVuPLBcCU2z8TrisoyY1", networkBitcoin);
        assertTrue(valid);

        Boolean valid1 = TLBitcoinjWrapper.isValidPrivateKey("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", networkBitcoin);
        assertFalse(valid1);


        Boolean valid2 = TLBitcoinjWrapper.isAddressVersion0("1DvKTsUfVaExCD7rBK4nyHt88tYpxU93eD", networkBitcoin);
        assertTrue(valid2);

        Boolean valid3 = TLBitcoinjWrapper.isAddressVersion0("3EktnHQD7RiAE6uzMj2ZifT9YgRrkSgzQX", networkBitcoin);
        assertFalse(valid3);

        valid = TLBitcoinjWrapper.isAddressVersion0("1", networkBitcoin);
        assertFalse(valid);

        valid = TLBitcoinjWrapper.isValidAddress("1DvKTsUfVaExCD7rBK4nyHt88tYpxU93eD", false);
        assertTrue(valid);
        valid = TLBitcoinjWrapper.isValidAddress("3EktnHQD7RiAE6uzMj2ZifT9YgRrkSgzQX", false);
        assertTrue(valid);
        valid = TLBitcoinjWrapper.isValidAddress("1", networkBitcoin);
        assertFalse(valid);

        valid = TLBitcoinjWrapper.isValidAddress("mipcBbFg9gMiCh81Kj8tqqdgoZub1ZJRfn", false);
        assertFalse(valid);
        valid = TLBitcoinjWrapper.isValidAddress("2MzQwSSnBHWHqSAqtTVQ6v47XtaisrJa1Vc", false);
        assertFalse(valid);
        valid = TLBitcoinjWrapper.isValidAddress("2MzQwSSnBHWHqSAqtTVQ6v47XtaisrJa1Ve", false);
        assertFalse(valid);

        valid = TLBitcoinjWrapper.isValidAddress("1DvKTsUfVaExCD7rBK4nyHt88tYpxU93eD", true);
        assertFalse(valid);
        valid = TLBitcoinjWrapper.isValidAddress("3EktnHQD7RiAE6uzMj2ZifT9YgRrkSgzQX", true);
        assertFalse(valid);
        valid = TLBitcoinjWrapper.isValidAddress("1", networkBitcoin);
        assertFalse(valid);

        valid = TLBitcoinjWrapper.isValidAddress("mipcBbFg9gMiCh81Kj8tqqdgoZub1ZJRfn", true);
        assertTrue(valid);
        valid = TLBitcoinjWrapper.isValidAddress("2MzQwSSnBHWHqSAqtTVQ6v47XtaisrJa1Vc", true);
        assertTrue(valid);
        valid = TLBitcoinjWrapper.isValidAddress("2MzQwSSnBHWHqSAqtTVQ6v47XtaisrJa1Ve", true);
        assertFalse(valid);
    }

    @Test
    public void testCreateSignedSerializedTransactionHex() {
        String hash = TLBitcoinjWrapper.reverseHexString("935c6975aa65f95cb55616ace8c8bede83b010f7191c0a6d385be1c95992870d");
        String script = "76a9149a1c78a507689f6f54b847ad1cef1e614ee23f1e88ac";
        String address = "1F3sAm6ZtwLAUnj7d38pGFxtP3RVEvtsbV";
        String privateKey = "L4rK1yDtCWekvXuE6oXD9jCYfFNV2cWRpVuPLBcCU2z8TrisoyY1";
        List<String> hashes = new ArrayList<String>(Arrays.asList(hash));
        List<Integer> inputIndexes = new ArrayList<Integer>(Arrays.asList(0));
        List<String> inputScripts = new ArrayList<String>(Arrays.asList(script));
        List<String> outputAddresses = new ArrayList<String>(Arrays.asList(address));
        List<Long> amounts = new ArrayList<Long>();
        amounts.add((long)2500000);
        List<String> privateKeys = new ArrayList<String>(Arrays.asList(privateKey));
        CreatedTransactionObject createdTransactionObject = TLBitcoinjWrapper.createSignedSerializedTransactionHex(hashes, inputIndexes,
                inputScripts, outputAddresses, amounts, privateKeys, null, false);
        log.info("txid: " + createdTransactionObject.txHash);
        log.info("txhex: " + createdTransactionObject.txHex);
        assertTrue(createdTransactionObject.txHash.equals("121d274734c83488e2bd6a2a3a136823d6099bf5a3517f78931c3ed0b9a2c619"));
        assertTrue(createdTransactionObject.txHex.equals("01000000010d879259c9e15b386d0a1c19f710b083debec8e8ac1656b55cf965aa75695c93000000006b4830450221009ceebee12f7a6321e39e83a0d0f8ba3db33271439e98addbc2c8518e9dd4d4ab022061965b500a9b1dd154545df086c3cc44661265841c82a4db20c44304711f1a0a012103a34b99f22c790c4e36b2b3c2c35a36db06226e41c692fc82b8b56ac1c540c5bdffffffff01a0252600000000001976a9149a1c78a507689f6f54b847ad1cef1e614ee23f1e88ac00000000"));
    }

    @Test
    public void testSignature() {
        String signature0 = TLBitcoinjWrapper.getSignature("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "message", networkBitcoin);
        assertTrue(signature0.equals("IIKZsJmBeK/xz3fgdBWTZdMlAkKJPbppe7Z706uYpcDCDxqm3iOJE5nv8736HXb6XvLIbNz1hIhJJTfJMOCiiw8="));

        String privKey = "4e422fb1e5e1db6c1f6ab32a7706d368ceb385e7fab098e633c5c5949c3b97cd";
        String msg = "I'm sorry, Dave. I'm afraid I can't do that";
        String signature = TLBitcoinjWrapper.getSignature(privKey, msg, networkBitcoin);
        assertTrue(signature.equals("IPD9ItlEbXBhPOg1ENibzP3ab1npAPjzFAt3krUrnlfDJvosnoNHi8CfUZcxrrcYFmHKu7dm1V9XVrkIONKLxrg="));
        String address = TLBitcoinjWrapper.getAddressFromSecret(privKey, networkBitcoin);
        assertTrue(TLBitcoinjWrapper.verifySignature(address, signature, msg, networkBitcoin));
        assertFalse(TLBitcoinjWrapper.verifySignature("1DvKTsUfVaExCD7rBK4nyHt88tYpxU93eD", signature, msg, networkBitcoin));
    }

    @Test
    public void testReverseHexString() {
        String txid = "2c441ba4920f03f37866edb5647f2626b64f57ad98b0a8e011af07da0aefcec3";
        String txHash = TLBitcoinjWrapper.reverseHexString(txid);
        assertTrue(txHash.equals("c3ceef0ada07af11e0a8b098ad574fb626267f64b5ed6678f3030f92a41b442c"));
    }

    @Test
    public void testBip38() {
        Boolean valid = TLBitcoinjWrapper.isValidPrivateKey("93KCDD4LdP4BDTNBXrvKUCVES2jo9dAKKvhyWpNEMstuxDauHty", networkTestnet);
        assertTrue(valid);

        valid = TLBitcoinjWrapper.isBIP38EncryptedKey("6PfNtAd2tHsBBEeaHuRLAbRc7dXbx1VL3DJEQkjgnNMGcKdr1TVeCxvwd8", networkBitcoin);
        assertTrue(valid);
        valid = TLBitcoinjWrapper.isBIP38EncryptedKey("6P", networkBitcoin);
        assertFalse(valid);

        valid = TLBitcoinjWrapper.isBIP38EncryptedKey("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", networkBitcoin);
        assertFalse(valid);
        valid = TLBitcoinjWrapper.isBIP38EncryptedKey("6p", networkBitcoin);
        assertFalse(valid);
        valid = TLBitcoinjWrapper.isBIP38EncryptedKey("6", networkBitcoin);
        assertFalse(valid);
        valid = TLBitcoinjWrapper.isBIP38EncryptedKey("", networkBitcoin);
        assertFalse(valid);

        String encryptedPrivateKey = "6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg";
        String password = "TestingOneTwoThree";
        String privateKey = TLBitcoinjWrapper.privateKeyFromEncryptedPrivateKey(encryptedPrivateKey, password, networkBitcoin);
        assertTrue(privateKey.equals("5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR"));
        password = "wrongPassword";
        privateKey = TLBitcoinjWrapper.privateKeyFromEncryptedPrivateKey(encryptedPrivateKey, password, networkBitcoin);
        assertTrue(privateKey == null);
    }
}
