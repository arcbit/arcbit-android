package com.arcbit.arcbit;

import com.arcbit.arcbit.model.TLHDWalletWrapper;

import org.bitcoinj.utils.BriefLogFormatter;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TLHDWalletWrapperTest {
    private static final Logger log = LoggerFactory.getLogger(TLHDWalletWrapperTest.class);

    @Before
    public void setUp() throws Exception {
        BriefLogFormatter.init();
    }

    @Test
    public void testIsValidExtendedKeyMainNet() throws Exception {
        Boolean isTestNet = false;

        assertFalse(TLHDWalletWrapper.phraseIsValid("report age service frame aspect worry nature toward vendor jungle grit grit"));
        assertFalse(TLHDWalletWrapper.phraseIsValid("I'm sorry, Dave. I'm afraid I can't do that"));

        assertTrue(TLHDWalletWrapper.isValidExtendedPrivateKey("xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPrivateKey("xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB", isTestNet));

        assertTrue(TLHDWalletWrapper.isValidExtendedPublicKey("xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPublicKey("xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPrivateKey("I'm sorry, Dave. I'm afraid I can't do that", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPublicKey("I'm sorry, Dave. I'm afraid I can't do that", isTestNet));

        assertFalse(TLHDWalletWrapper.isValidExtendedPrivateKey("tprv8ghHTunGi9gpD75TviQkXEQGpj8geMySh6YUndnWspdBuUEk3KUENtftKJQuCpWyVNhzzL6zroqKWnrNWANmRkW9pfVg7vnX2U56nrK2gmU", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPublicKey("tpubDDPKcKpWrXNV6a7FpN5Lve4PPkecohAMGQ9G59ppJ6RajxVWfiHpZPHkVRvuLx7hDdQjUBYRUeNZRAN5dn9FnbBCE14f4QNaMyyoCqbdkeN", isTestNet));

        assertFalse(TLHDWalletWrapper.isValidExtendedPublicKey("tprv8ghHTunGi9gpD75TviQkXEQGpj8geMySh6YUndnWspdBuUEk3KUENtftKJQuCpWyVNhzzL6zroqKWnrNWANmRkW9pfVg7vnX2U56nrK2gmU", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPrivateKey("tpubDDPKcKpWrXNV6a7FpN5Lve4PPkecohAMGQ9G59ppJ6RajxVWfiHpZPHkVRvuLx7hDdQjUBYRUeNZRAN5dn9FnbBCE14f4QNaMyyoCqbdkeN", isTestNet));

        Boolean isValid = TLHDWalletWrapper.isValidExtendedPrivateKey("xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U", isTestNet);
        assertTrue(isValid);

        isValid = TLHDWalletWrapper.isValidExtendedPrivateKey("I'm sorry, Dave. I'm afraid I can't do that", isTestNet);
        assertTrue(!isValid);
    }

    @Test
    public void testIsValidExtendedKeyTestNet() throws Exception {
        Boolean isTestNet = true;

        assertTrue(TLHDWalletWrapper.isValidExtendedPrivateKey("tprv8ghHTunGi9gpD75TviQkXEQGpj8geMySh6YUndnWspdBuUEk3KUENtftKJQuCpWyVNhzzL6zroqKWnrNWANmRkW9pfVg7vnX2U56nrK2gmU", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPrivateKey("tpubDDPKcKpWrXNV6a7FpN5Lve4PPkecohAMGQ9G59ppJ6RajxVWfiHpZPHkVRvuLx7hDdQjUBYRUeNZRAN5dn9FnbBCE14f4QNaMyyoCqbdkeN", isTestNet));

        assertTrue(TLHDWalletWrapper.isValidExtendedPublicKey("tpubDDPKcKpWrXNV6a7FpN5Lve4PPkecohAMGQ9G59ppJ6RajxVWfiHpZPHkVRvuLx7hDdQjUBYRUeNZRAN5dn9FnbBCE14f4QNaMyyoCqbdkeN", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPublicKey("tprv8ghHTunGi9gpD75TviQkXEQGpj8geMySh6YUndnWspdBuUEk3KUENtftKJQuCpWyVNhzzL6zroqKWnrNWANmRkW9pfVg7vnX2U56nrK2gmU", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPrivateKey("I'm sorry, Dave. I'm afraid I can't do that", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPublicKey("I'm sorry, Dave. I'm afraid I can't do that", isTestNet));

        assertFalse(TLHDWalletWrapper.isValidExtendedPrivateKey("xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPublicKey("xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U", isTestNet));

        assertFalse(TLHDWalletWrapper.isValidExtendedPublicKey("xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB", isTestNet));
        assertFalse(TLHDWalletWrapper.isValidExtendedPrivateKey("xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB", isTestNet));
    }

    @Test
    public void testHDWalletMainNet() throws Exception {
        Boolean isTestNet = false;
        String backupPassphrase = "slogan lottery zone helmet fatigue rebuild solve best hint frown conduct ill";

        assertTrue(TLHDWalletWrapper.phraseIsValid(backupPassphrase));
        String masterHex = TLHDWalletWrapper.getMasterHex(backupPassphrase);
        assertTrue(masterHex.equals("ae3ff5936bf70293eda11b5ea5ee9585fe9b22c9a80b610ee37251a22120e970c75a18bbd95219a0348c7dee40eeb44a4d2480900be8f931d0cf85203f9d94ce"));

        String extendPrivKey = TLHDWalletWrapper.getExtendPrivKey(masterHex, 0, isTestNet);
        assertTrue(extendPrivKey.equals("xprv9z2LgaTwJsrjcHqwG9ZFManHWbiUQqwSMYdMvDN4Pr8i7sVf3x8Us9JSQ8FFCT8f7wBDzEVEhTFX3wJdNx2pchEZJ2HNTa4U7NKgM9uWoK6"));
        String extendPubKey = TLHDWalletWrapper.getExtendPubKey(extendPrivKey, isTestNet);
        assertTrue(extendPubKey.equals("xpub6D1h65zq9FR2pmvQNB6Fiij24dYxpJfHimYxibmfxBfgzfpobVSjQwcvFPr7pTATRisprc2YwYYWiysUEvJ1u9iuAQKMNsiLn2PPSrtVFt6"));
        HashMap<String, String> stealthAddressData = TLHDWalletWrapper.getStealthAddress(extendPrivKey, isTestNet);
        assertTrue(stealthAddressData.get("stealthAddress").equals("vJmuQKhaULxxLGKG5QiTkg2xhMibdauFTHnwMeNFgrVpnLr8ZcasPzt8QFcbhJDQJ2Wi2wExmEQ73xnzqR9kXLnaWVkBac6dj7iv9S"));
        assertTrue(stealthAddressData.get("scanPriv").equals("1e47179452fed4b73b2fd7c5ce3516aadf64c5217c67f5c556e5a1dc1908d047"));
        assertTrue(stealthAddressData.get("spendPriv").equals("365d66d8bd67fd40e98df439861922bf8ee507725d9dbcf479d4ad49b9177858"));

        ArrayList<Integer> mainAddressIndex0 = new ArrayList<Integer>(Arrays.asList(0,0));
        String mainAddress0 = TLHDWalletWrapper.getAddress(extendPubKey, mainAddressIndex0, isTestNet);
        assertTrue(mainAddress0.equals("1K7fXZeeQydcUvbsfvkMSQmiacV5sKRYQz"));
        String mainPrivKey0 = TLHDWalletWrapper.getPrivateKey(extendPrivKey, mainAddressIndex0, isTestNet);
        assertTrue(mainPrivKey0.equals("KwJhkmrjjg3AEX5gvccNAHCDcXnQLwzyZshnp5yK7vXz1mHKqDDq"));

        ArrayList<Integer> mainAddressIndex1 = new ArrayList<Integer>(Arrays.asList(0,1));
        String mainAddress1 = TLHDWalletWrapper.getAddress(extendPubKey, mainAddressIndex1, isTestNet);
        assertTrue(mainAddress1.equals("12eQLjACXw6XwfGF9kqBwy9U7Se8qGoBuq"));
        String mainPrivKey1 = TLHDWalletWrapper.getPrivateKey(extendPrivKey, mainAddressIndex1, isTestNet);
        assertTrue(mainPrivKey1.equals("KwpCsb3wBGk7E1M9EXcZWZhRoKBoZLNc63RsSP4YspUR53Ndefyr"));

        ArrayList<Integer> changeAddressIndex0 = new ArrayList<Integer>(Arrays.asList(1,0));
        String changeAddress0 = TLHDWalletWrapper.getAddress(extendPubKey, changeAddressIndex0, isTestNet);
        assertTrue(changeAddress0.equals("1CvpGn9VxVY1nsWWL3MSWRYaBHdNkCDbmv"));
        String changePrivKey0 = TLHDWalletWrapper.getPrivateKey(extendPrivKey, changeAddressIndex0, isTestNet);
        assertTrue(changePrivKey0.equals("L33guNrQHMXdpFd9jpjo2mQzddwLUgUrNzK3KqAM83D9ZU1H5NDN"));

        ArrayList<Integer> changeAddressIndex1 = new ArrayList<Integer>(Arrays.asList(1,1));
        String changeAddress1 = TLHDWalletWrapper.getAddress(extendPubKey, changeAddressIndex1, isTestNet);
        assertTrue(changeAddress1.equals("17vnH8d1fBbjX7GZx727X2Y6dheaid2NUR"));
        String changePrivKey1 = TLHDWalletWrapper.getPrivateKey(extendPrivKey, changeAddressIndex1, isTestNet);
        assertTrue(changePrivKey1.equals("KwiMiFtWv1PXNN3zV67TC59tWJxPbeagMJU1SSr3uLssAC82UKhf"));
    }

    @Test
    public void testHDWalletTestNet() throws Exception {
        Boolean isTestNet = true;
        String backupPassphrase = "slogan lottery zone helmet fatigue rebuild solve best hint frown conduct ill";

        assertTrue(TLHDWalletWrapper.phraseIsValid(backupPassphrase));
        String masterHex = TLHDWalletWrapper.getMasterHex(backupPassphrase);
        assertTrue(masterHex.equals("ae3ff5936bf70293eda11b5ea5ee9585fe9b22c9a80b610ee37251a22120e970c75a18bbd95219a0348c7dee40eeb44a4d2480900be8f931d0cf85203f9d94ce"));

        String extendPrivKey = TLHDWalletWrapper.getExtendPrivKey(masterHex, 0, isTestNet);
        assertTrue(extendPrivKey.equals("tprv8ghHTunGi9gpD75TviQkXEQGpj8geMySh6YUndnWspdBuUEk3KUENtftKJQuCpWyVNhzzL6zroqKWnrNWANmRkW9pfVg7vnX2U56nrK2gmU"));
        String extendPubKey = TLHDWalletWrapper.getExtendPubKey(extendPrivKey, isTestNet);
        assertTrue(extendPubKey.equals("tpubDDPKcKpWrXNV6a7FpN5Lve4PPkecohAMGQ9G59ppJ6RajxVWfiHpZPHkVRvuLx7hDdQjUBYRUeNZRAN5dn9FnbBCE14f4QNaMyyoCqbdkeN"));
        HashMap<String, String> stealthAddressData = TLHDWalletWrapper.getStealthAddress(extendPrivKey, isTestNet);
        assertTrue(stealthAddressData.get("stealthAddress").equals("waPVZhcLGRMy3vnt9b5JSextRPuoeaRMfVcCLJxLdyjkAHFQproqBuApbwY7FFo41bTqBNnbbzoELmjys2eTNLXPRieptnwGymfqv3"));
        assertTrue(stealthAddressData.get("scanPriv").equals("1e47179452fed4b73b2fd7c5ce3516aadf64c5217c67f5c556e5a1dc1908d047"));
        assertTrue(stealthAddressData.get("spendPriv").equals("365d66d8bd67fd40e98df439861922bf8ee507725d9dbcf479d4ad49b9177858"));

        ArrayList<Integer> mainAddressIndex0 = new ArrayList<Integer>(Arrays.asList(0,0));
        String mainAddress0 = TLHDWalletWrapper.getAddress(extendPubKey, mainAddressIndex0, isTestNet);
        assertTrue(mainAddress0.equals("mydcpcjdE14sG35VPVijGKz3Sc5nsbbeo7"));
        String mainPrivKey0 = TLHDWalletWrapper.getPrivateKey(extendPrivKey, mainAddressIndex0, isTestNet);
        assertTrue(mainPrivKey0.equals("cMfhDgrbAjjRPxYxK2RVXbhHEm5p1Q6fdurFvWRpd3BzGWQYiFw6"));

        ArrayList<Integer> mainAddressIndex1 = new ArrayList<Integer>(Arrays.asList(0,1));
        String mainAddress1 = TLHDWalletWrapper.getAddress(extendPubKey, mainAddressIndex1, isTestNet);
        assertTrue(mainAddress1.equals("mhAMdnFBLxXnimjrsKoZmtMnySEqo6Q7sk"));
        String mainPrivKey1 = TLHDWalletWrapper.getPrivateKey(extendPrivKey, mainAddressIndex1, isTestNet);
        assertTrue(mainPrivKey1.equals("cNBCLW3ncLSNPSpQcwRgstCVRYVDDnUJA5aLYoX4Nw8RKnSk1tYZ"));

        ArrayList<Integer> changeAddressIndex0 = new ArrayList<Integer>(Arrays.asList(1,0));
        String changeAddress0 = TLHDWalletWrapper.getAddress(extendPubKey, changeAddressIndex0, isTestNet);
        assertTrue(changeAddress0.equals("msSmZqEUmWyGZyz83cKpLLku3HE5eQSwyL"));
        String changePrivKey0 = TLHDWalletWrapper.getPrivateKey(extendPrivKey, changeAddressIndex0, isTestNet);
        assertTrue(changePrivKey0.equals("cTQgNHrFiRDtyh6R8EYvQ5v4FsEk98aYT2TWSFcrd9s9pD5Y6iTE"));

        ArrayList<Integer> changeAddressIndex1 = new ArrayList<Integer>(Arrays.asList(1,1));
        String changeAddress1 = TLHDWalletWrapper.getAddress(extendPubKey, changeAddressIndex1, isTestNet);
        assertTrue(changeAddress1.equals("mnSjaBhzUD2zJDkBffzVLwkRVhFHahwU7v"));
        String changePrivKey1 = TLHDWalletWrapper.getPrivateKey(extendPrivKey, changeAddressIndex1, isTestNet);
        assertTrue(changePrivKey1.equals("cN5MBAtNM55nXoXFsVvaZPex8YFoG6gNRLcUYsJZQTXsQwAQ86wM"));
    }
}
