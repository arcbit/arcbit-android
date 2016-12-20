package com.arcbit.arcbit;

import android.support.v4.util.Pair;
//import com.sun.tools.javac.util.Pair;
//import android.util.Pair;
// must use below cuz of http://stackoverflow.com/questions/33502939/android-util-pair-holds-string-as-parameters-in-androidtest-but-null-in-unit

import com.arcbit.arcbit.model.TLBitcoinjWrapper;
import com.arcbit.arcbit.model.TLStealthAddress;

import org.bitcoinj.utils.BriefLogFormatter;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TLStealthAddressTest {
    private static final Logger log = LoggerFactory.getLogger(TLStealthAddressTest.class);

    private static String expectedStealthAddress = "vJmujDzf2PyDEcLQEQWyzVNthLpRAXqTi3ZencThu2WCzrRNi64eFYJP6ZyPWj53hSZBKTcUAk8J5Mb8rZC4wvGn77Sj4Z3yP7zE69";
    private static String expectedScanPublicKey = "02a13daf6cc5ad7a1adcae59ff348a005247aa9e84453770d0e0ee96b894f8bbb1";
    private static String scanPrivateKey = "d63e1ca7e79bafd8fdc7e568c6b3fcf8a287ad328e80376e6582af2e69943eca";
    private static String expectedSpendPublicKey = "02c55695f16cd320fef70ff6f46601cdeed655d9198d555a533382fb81a8f6eab5";
    private static String spendPrivateKey = "c4054001795dd20c740d5d1389e080b424a9ff2ec9503aa3182369f4b71f00ac";
    private static String ephemeralPublicKey = "02d53b53c3cb7d6e8f4925e404ce40ec9edd81b0b03d49da950deb3c2240ca519a";
    private static String ephemeralPrivateKey = "dc406d598685e3400a7eff2d952d47f999de9f69d5ff1295302ad7314a2cf979";
    private static String paymentAddressPublicKey = "02da20a21ac1332edd5352306104f7a751b45e52bf4a41d4c350ccb890301d80e6";
    private static String paymentAddressPrivateKey = "775c912899b27ee8a1f944c0e2ac90e095f63893d39c3d66d0dd0a854b799eb5";
    private static Boolean isTestNet = false;
    private static Integer nonce = 0xdeadbeef;

    @Before
    public void setUp() throws Exception {
        BriefLogFormatter.init();
    }

    @Test
    public void testStealthAddress() throws Exception {
        String stealthAddress = TLStealthAddress.createStealthAddress(expectedScanPublicKey, expectedSpendPublicKey, isTestNet);
        assertTrue(stealthAddress.equals(expectedStealthAddress));

        stealthAddress = expectedStealthAddress;
        Pair<String, String> publicKeys = TLStealthAddress.getScanPublicKeyAndSpendPublicKey(stealthAddress, isTestNet);
        String scanPublicKey = publicKeys.first;
        String spendPublicKey = publicKeys.second;
        assertTrue(scanPublicKey.equals(expectedScanPublicKey));
        assertTrue(spendPublicKey.equals(expectedSpendPublicKey));

        Pair<String, String> stealthDataScriptAndPaymentAddress = TLStealthAddress.createDataScriptAndPaymentAddress(stealthAddress,
                ephemeralPrivateKey, nonce, isTestNet);
        String expectedPaymentAddress = TLBitcoinjWrapper.getAddressFromPublicKey(paymentAddressPublicKey, isTestNet);

        assertTrue(stealthDataScriptAndPaymentAddress.first.equals("6a2606deadbeef02d53b53c3cb7d6e8f4925e404ce40ec9edd81b0b03d49da950deb3c2240ca519a"));
        assertTrue(stealthDataScriptAndPaymentAddress.second.equals(expectedPaymentAddress));

        String publicKey = TLStealthAddress.getPaymentAddressPublicKeyFromScript(stealthDataScriptAndPaymentAddress.first, scanPrivateKey, spendPublicKey);
        assertTrue(publicKey.equals(paymentAddressPublicKey));
        assertTrue(TLBitcoinjWrapper.getAddressFromPublicKey(publicKey, isTestNet).equals("1C6gQ79qKKG21AGCA9USKYWPvu6LzoPH5h"));
    }

    @Test
    public void testAddition() throws Exception {
        String lhsPublicKey = "02a3fe61cf993845ec7c0c0833884ae2f2fdd1cc8d1c134f12836b4a4584178ab3";
        String rhsPublicKey = "028007c01dd3a4f074bc5552dd73bbe8f530fc0da5a438af04ab87feaf85a0136a";
        String addPublicKeysResult = TLStealthAddress.addPublicKeys(lhsPublicKey, rhsPublicKey);

        assertTrue(addPublicKeysResult.equals("0360882edc74ef593142ef477cb62e08eb0af14351d31e6dcf38c9ee8af726d3cb"));

        String lhsPrivateKey = "c4054001795dd20c740d5d1389e080b424a9ff2ec9503aa3182369f4b71f00ac";
        String rhsPrivateKey = "b35751272054acdc2debe7ad58cc102b2bfb164bb994a2ff788bff1d6490df4a";
        String addPrivateKeysResult = TLStealthAddress.addPrivateKeys(lhsPrivateKey, rhsPrivateKey);

        assertTrue(addPrivateKeysResult.equals(paymentAddressPrivateKey));
    }

    @Test
    public void testSharedSecret() throws Exception {
        String sharedSecret = TLStealthAddress.getSharedSecretForReceiver(ephemeralPublicKey, scanPrivateKey);
        assertTrue(sharedSecret.equals("b35751272054acdc2debe7ad58cc102b2bfb164bb994a2ff788bff1d6490df4a"));
    }

    @Test
    public void testStealthDataScript() throws Exception {
        String stealthDataScript = "6a2606deadbeef02d53b53c3cb7d6e8f4925e404ce40ec9edd81b0b03d49da950deb3c2240ca519a";

        String publicKey = TLStealthAddress.getPaymentAddressPublicKeyFromScript(stealthDataScript, scanPrivateKey, expectedSpendPublicKey);

        assertTrue(publicKey.equals(paymentAddressPublicKey));
        assertTrue(TLBitcoinjWrapper.getAddressFromPublicKey(publicKey, isTestNet).equals("1C6gQ79qKKG21AGCA9USKYWPvu6LzoPH5h"));

        String secret = TLStealthAddress.getPaymentAddressPrivateKeySecretFromScript(stealthDataScript, scanPrivateKey, spendPrivateKey);
        assertTrue(secret.equals(paymentAddressPrivateKey));
        assertTrue(TLBitcoinjWrapper.getAddressFromSecret(secret, isTestNet).equals("1C6gQ79qKKG21AGCA9USKYWPvu6LzoPH5h"));

        assertTrue(TLStealthAddress.isStealthAddress("waPaGzJ3AUzpu3tBBcu7vrsyGsBj29MNFvANP9G2RxokXncKZ3KJkEcnLHwgZvp6HhXabbQNkJhQbar6vXMFMmG9nJDz5rufxA7ZBp", true));
        assertFalse(TLStealthAddress.isStealthAddress("waPaGzJ3AUzpu3tBBcu7vrsyGsBj29MNFvANP9G2RxokXncKZ3KJkEcnLHwgZvp6HhXabbQNkJhQbar6vXMFMmG9nJDz5rufxA7ZBp", false));

        assertTrue(TLStealthAddress.isStealthAddress(expectedStealthAddress, isTestNet));
        assertFalse(TLStealthAddress.isStealthAddress(expectedStealthAddress, true));
        assertFalse(TLStealthAddress.isStealthAddress("1DvKTsUfVaExCD7rBK4nyHt88tYpxU93eD", isTestNet));
        assertFalse(TLStealthAddress.isStealthAddress("Open the pod bay doors Hal", isTestNet));

        String scanPublicKey = "03267a6dc59b3dfeae10efdca889245379ca0fa733dc5fa9c9b573d8896f01577e";
        String ephemeralPrivateKey = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String sharedSecret = TLStealthAddress.getSharedSecretForSender(scanPublicKey, ephemeralPrivateKey);
        assertTrue(sharedSecret.equals("b6e408dfe08aabde07d459cdcb9f6fb160b95063286161cd51261c448e5bc09c"));

        Pair<String, String> stealthDataScriptAndPaymentAddress = TLStealthAddress.createDataScriptAndPaymentAddress(expectedStealthAddress,
                ephemeralPrivateKey, nonce, isTestNet);
        assertTrue(stealthDataScriptAndPaymentAddress.first.equals("6a2606deadbeef03a34b99f22c790c4e36b2b3c2c35a36db06226e41c692fc82b8b56ac1c540c5bd"));
        assertTrue(stealthDataScriptAndPaymentAddress.second.equals("1HCvVzoWN9SpYEmaMuJGGHNDVXxtDDjDEh"));
    }

    @Test
    public void testNonce() throws Exception {
        Integer nonce = TLStealthAddress.generateNonce();
        assertTrue(nonce instanceof Integer);
    }
}
