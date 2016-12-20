package com.arcbit.arcbit.model;

import android.content.DialogInterface;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

public class TLKeyStore {
    private static final String TAG = TLKeyStore.class.getName();
    public static final String KEY_STORE_WALLET_PASSPHRASE_ALIAS = "walletPassphrase";

    TLAppDelegate appDelegate;
    KeyStore keyStore;
    List<String> keyAliases;

    public static boolean canUseKeyStore() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return true;
        } else {
            return false;
        }
    }

    public TLKeyStore(TLAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            createNewKeys(KEY_STORE_WALLET_PASSPHRASE_ALIAS);
        } catch(Exception e) {
            Log.d(TAG, "TLKeyStore Exception " + e.getMessage() + " occured");
            Log.e(TAG, Log.getStackTraceString(e));
        }
        refreshKeys();
    }

    private void refreshKeys() {
        keyAliases = new ArrayList<>();
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                keyAliases.add(aliases.nextElement());
            }
        } catch(Exception e) {
            Log.d(TAG, "refreshKeys Exception " + e.getMessage() + " occured");
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void createNewKeys(String alias) {
        try {
            //assert (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2);
            // Create new key if needed
            if (!keyStore.containsAlias(alias)) {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 1);
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(appDelegate.context)
                        .setAlias(alias)
                        .setSubject(new X500Principal("CN=Sample Name, O=Android Authority"))
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                generator.initialize(spec);

                KeyPair keyPair = generator.generateKeyPair();
            } else {
                Log.d(TAG, "createNewKeys alias " + alias + " exist");
            }
        } catch (Exception e) {
            Log.d(TAG, "createNewKeys Exception " + e.getMessage() + " occured");
            Log.e(TAG, Log.getStackTraceString(e));
        }
        refreshKeys();
    }

    public void deleteKey(final String alias) {
        try {
            keyStore.deleteEntry(alias);
            refreshKeys();
        } catch (KeyStoreException e) {
            Log.d(TAG, "deleteKey Exception " + e.getMessage() + " occured");
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public String encryptString(String alias, String plainText) {
        if(plainText.isEmpty()) {
            Log.d(TAG, "encryptString plainText isEmpty");
            return null;
        }
        try {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
            RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            Cipher inCipher;
            // ignore android lint warning, only applies to symmetric ciphers, http://stackoverflow.com/questions/36016288/cipher-with-ecb-mode-should-not-be-used
            inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            inCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    outputStream, inCipher);
            cipherOutputStream.write(plainText.getBytes("UTF-8"));
            cipherOutputStream.close();

            byte [] vals = outputStream.toByteArray();
            return Base64.encodeToString(vals, Base64.DEFAULT);
        } catch (Exception e) {
            Log.d(TAG, "encryptString Exception " + e.getMessage() + " occured");
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public String decryptString(String alias, String cipherText) {
        try {
            Cipher output;
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);

            // ignore android lint warning, only applies to symmetric ciphers, http://stackoverflow.com/questions/36016288/cipher-with-ecb-mode-should-not-be-used
            output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(Base64.decode(cipherText, Base64.DEFAULT)), output);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte)nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for(int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }

            String finalText = new String(bytes, 0, bytes.length, "UTF-8");
            return finalText;
        } catch (Exception e) {
            Log.d(TAG, "decryptString Exception " + e.getMessage() + " occured");
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }
}