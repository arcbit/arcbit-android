package com.arcbit.arcbit.model;

import android.content.Context;
import android.util.Log;

import com.arcbit.arcbit.utils.TLAESCrypt;
import com.arcbit.arcbit.utils.TLCrypto;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;

public class TLWalletJson {
    private static final String TAG = TLWalletJson.class.getName();

    private Context context = null;
    private TLAppDelegate appDelegate = null;

    public TLWalletJson(TLAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
        this.context = appDelegate.context;
    }

    String getWalletJsonFileName() {
        return "wallet.json.asc";
    }

    String generatePayloadChecksum(String payload) {
        return TLCrypto.getInstance().doubleSHA256HashFor(payload);
    }

    String getEncryptedWalletJsonContainer(JSONObject walletJson, String password) {
        assert(TLHDWalletWrapper.phraseIsValid(password)); // "phrase is invalid"
        String str = walletJson.toString();
        String encryptJSONPassword = TLCrypto.getInstance().doubleSHA256HashFor(password);
        byte[] iv = this.appDelegate.encryptedPreferences.getWalletJSONEncryptionIV();
        if (iv == null) {
            iv = TLAESCrypt.getRandomIV();
            this.appDelegate.encryptedPreferences.setWalletJSONEncryptionIV(iv);
        }
        str = TLCrypto.getInstance().encrypt(str, encryptJSONPassword, iv);
        JSONObject walletJsonEncryptedWrapperDict = new JSONObject();
        try {
            walletJsonEncryptedWrapperDict.put("version", 1);
            walletJsonEncryptedWrapperDict.put("payload", str);
            String walletJsonEncryptedWrapperString = walletJsonEncryptedWrapperDict.toString();
            return walletJsonEncryptedWrapperString;
        } catch (JSONException e) {
            return null;
        }
    }

    JSONObject getWalletJsonDict(String encryptedWalletJSONFileContent, String password) {
        if (encryptedWalletJSONFileContent == null) {
            return null;
        }
        try {
            JSONObject walletJsonEncryptedWrapperDict = new JSONObject(encryptedWalletJSONFileContent);
            int version = walletJsonEncryptedWrapperDict.getInt("version");
            assert(version == 1); //"Incorrect encryption version"

            String encryptedWalletJSONPayloadString = walletJsonEncryptedWrapperDict.getString("payload");

            String walletJsonString = decryptWalletJSONFile(encryptedWalletJSONPayloadString, password);
            if (walletJsonString == null) {
                return null;
            }

            JSONObject walletDict = new JSONObject(walletJsonString);
            return walletDict;
        } catch (JSONException e) {
            return null;
        }
    }

    String decryptWalletJSONFile(String encryptedWalletJSONFile, String password) {
        if (encryptedWalletJSONFile == null || password == null) {
            return null;
        }
        assert(TLHDWalletWrapper.phraseIsValid(password));// "phrase is invalid"
        String encryptJSONPassword = TLCrypto.getInstance().doubleSHA256HashFor(password);
        byte[] iv = this.appDelegate.encryptedPreferences.getWalletJSONEncryptionIV();
        if (iv == null) {
            return null;
        }
        String str = TLCrypto.getInstance().decrypt(encryptedWalletJSONFile, encryptJSONPassword, iv);
        return str;
    }

    boolean saveWalletJson(String walletFile, Date date) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(this.getWalletJsonFileName(), Context.MODE_PRIVATE));
            outputStreamWriter.write(walletFile);
            outputStreamWriter.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.getLocalizedMessage());
            return false;
        }
    }

    String getLocalWalletJSONFile() {
        String ret = null;
        try {
            InputStream inputStream = context.openFileInput(this.getWalletJsonFileName());

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.getLocalizedMessage());
        }

        return ret;
    }
}
