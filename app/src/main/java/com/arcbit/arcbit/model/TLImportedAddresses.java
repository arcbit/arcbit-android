package com.arcbit.arcbit.model;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arcbit.arcbit.APIs.TLNetworking;
import com.arcbit.arcbit.model.TLOperationsManager.TLDownloadState;
import com.arcbit.arcbit.model.TLWalletUtils.TLAccountAddressType;

public class TLImportedAddresses {
    TLAppDelegate appDelegate;
    private TLWallet appWallet;
    private List<TLImportedAddress> importedAddresses = new ArrayList<TLImportedAddress>();
    private List<TLImportedAddress> archivedImportedAddresses = new ArrayList<TLImportedAddress>();
    private Map<String, List<Integer>> addressToIdxDict = new HashMap<String, List<Integer>>();
    private Map<Integer, TLImportedAddress> addressToPositionInWalletArrayDict = new HashMap<Integer, TLImportedAddress>();
    private TLAccountAddressType accountAddressType;
    TLDownloadState downloadState = TLOperationsManager.TLDownloadState.NotDownloading;

    public TLImportedAddresses(TLAppDelegate appDelegate, TLWallet appWallet, List<TLImportedAddress> importedAddresses, TLAccountAddressType accountAddressType) {
        this.appDelegate = appDelegate;
        this.appWallet = appWallet;
        this.accountAddressType = accountAddressType;

        for (int i = 0; i < importedAddresses.size(); i++) {
            TLImportedAddress importedAddressObject = importedAddresses.get(i);
            if (importedAddressObject.isArchived()) {
                this.archivedImportedAddresses.add(importedAddressObject);
            } else {
                List<Integer> indexes = this.addressToIdxDict.get(importedAddressObject.getAddress());
                if (indexes == null) {
                    indexes = new ArrayList<Integer>();
                    this.addressToIdxDict.put(importedAddressObject.getAddress(), indexes);
                }

                indexes.add(this.importedAddresses.size());

                this.importedAddresses.add(importedAddressObject);
            }

            importedAddressObject.setPositionInWalletArray(i);
            this.addressToPositionInWalletArrayDict.put(importedAddressObject.getPositionInWalletArrayNumber(), importedAddressObject);
        }
    }

    public TLImportedAddress getAddressObjectAtIdx(int idx) {
        return this.importedAddresses.get(idx);
    }

    public TLImportedAddress getArchivedAddressObjectAtIdx(int idx) {
        return this.archivedImportedAddresses.get(idx);
    }

    public int getCount() {
        return this.importedAddresses.size();
    }

    public int getArchivedCount() {
        return this.archivedImportedAddresses.size();
    }

    public void checkToGetAndSetAddressesDataO(boolean fetchDataAgain) {
        List<String> addresses = new ArrayList<String>();

        for (TLImportedAddress importedAddressObject : this.importedAddresses) {
            if (!importedAddressObject.hasFetchedAccountData() || fetchDataAgain) {
                String address = importedAddressObject.getAddress();
                addresses.add(address);
            }
        }
        
        if (addresses.size() == 0) {
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                JSONObject jsonData = (JSONObject) msg.obj;
                if (jsonData == null || jsonData.has(TLNetworking.HTTP_ERROR_CODE)) {
                    downloadState = TLDownloadState.Failed;
                    return;
                }

                try {
                    JSONArray addressesArray = jsonData.getJSONArray("addresses");
                    JSONArray txArray = jsonData.getJSONArray("txs");
                    for (int i = 0; i < addressesArray.length(); i++) {
                        JSONObject addressDict = addressesArray.getJSONObject(i);
                        String address = addressDict.getString("address");

                        List<Integer> indexes = addressToIdxDict.get(address);
                        for (Integer idx : indexes) {
                            TLImportedAddress importedAddressObject = importedAddresses.get(idx);
                            long addressBalance = addressDict.getLong("final_balance");
                            importedAddressObject.balance = new TLCoin(addressBalance);
                            importedAddressObject.processTxArray(txArray, false);
                            importedAddressObject.setHasFetchedAccountData(true);
                        }
                    }

                    downloadState = TLDownloadState.Downloaded;
                    Intent intent = new Intent(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA);
                    LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("TLImportedAddresses", "onPostExecute " + e.getLocalizedMessage());
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonData = appDelegate.blockExplorerAPI.getAddressesInfo(addresses);
                    Message message = Message.obtain();
                    message.obj = jsonData;
                    handler.sendMessage(Message.obtain(message));
                } catch (Exception e) {
                    Log.d("TLImportedAddress", e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public TLImportedAddress addImportedPrivateKey(String privateKey, String encryptedPrivateKey) {
        JSONObject importedPrivateKeyDict = this.appWallet.addImportedPrivateKey(privateKey, encryptedPrivateKey);

        TLImportedAddress importedAddressObject = new TLImportedAddress(this.appDelegate, appWallet, importedPrivateKeyDict);
        this.importedAddresses.add(importedAddressObject);

        importedAddressObject.setPositionInWalletArray(this.importedAddresses.size() + this.archivedImportedAddresses.size() - 1);
        this.addressToPositionInWalletArrayDict.put(importedAddressObject.getPositionInWalletArrayNumber(), importedAddressObject);

        String address = TLBitcoinjWrapper.getAddress(privateKey, this.appWallet.walletConfig.isTestnet);

        List<Integer> indexes = this.addressToIdxDict.get(address);
        if (indexes == null) {
            indexes = new ArrayList<Integer>();
            this.addressToIdxDict.put(importedAddressObject.getAddress(), indexes);
        }

        indexes.remove(new Integer(this.importedAddresses.size()-1));

        setLabel(importedAddressObject.getDefaultAddressLabel(), importedAddressObject.getPositionInWalletArray());

        return importedAddressObject;
    }

    public TLImportedAddress addImportedWatchAddress(String address) {
        JSONObject importedDict = this.appWallet.addWatchOnlyAddress(address);
        TLImportedAddress importedAddressObject = new TLImportedAddress(this.appDelegate, this.appWallet, importedDict);
        this.importedAddresses.add(importedAddressObject);

        importedAddressObject.setPositionInWalletArray(this.importedAddresses.size() + this.archivedImportedAddresses.size() - 1);
        this.addressToPositionInWalletArrayDict.put(importedAddressObject.getPositionInWalletArrayNumber(), importedAddressObject);

        List<Integer> indexes = this.addressToIdxDict.get(address);
        if (indexes == null) {
            indexes = new ArrayList<Integer>();
            this.addressToIdxDict.put(address, indexes);
        }

        indexes.add(this.importedAddresses.size()-1);

        setLabel(importedAddressObject.getDefaultAddressLabel(), importedAddressObject.getPositionInWalletArray());

        return importedAddressObject;
    }

    public boolean setLabel(String label, int positionInWalletArray) {
        TLImportedAddress importedAddressObject = this.addressToPositionInWalletArrayDict.get(positionInWalletArray);
        StringBuilder stringBuilder = new StringBuilder();
                for (StackTraceElement element : Thread.currentThread().getStackTrace()) stringBuilder.append(element.toString()).append("\n");

        importedAddressObject.setLabel(label);
        if (this.accountAddressType == TLAccountAddressType.Imported) {

            this.appWallet.setImportedPrivateKeyLabel(label, positionInWalletArray);
        } else if (this.accountAddressType == TLAccountAddressType.ImportedWatch) {
            this.appWallet.setWatchOnlyAddressLabel(label, positionInWalletArray);
        }

        return true;
    }

    public void archiveAddress(int positionInWalletArray) {
        this.setArchived(positionInWalletArray, true);

        TLImportedAddress toMoveAddressObject = this.addressToPositionInWalletArrayDict.get(positionInWalletArray);
        List<Integer> indexes = this.addressToIdxDict.get(toMoveAddressObject.getAddress());
        if (indexes == null) {
            indexes = new ArrayList<Integer>();
            this.addressToIdxDict.put(toMoveAddressObject.getAddress(), indexes);
        }
        int toMoveIndex = this.importedAddresses.indexOf(toMoveAddressObject);

        for (String address : this.addressToIdxDict.keySet()) {
            List<Integer> indexes2 = new ArrayList<Integer>(this.addressToIdxDict.get(address));
            for(Integer idx : indexes2) {
                List<Integer> indexes3 = this.addressToIdxDict.get(address);
                indexes3.remove(idx);
                indexes3.add(new Integer(idx - new Integer(1)));
            }
        }

        indexes.remove(new Integer(toMoveIndex));

        this.importedAddresses.remove(toMoveAddressObject);
        for (int i = 0; i < this.archivedImportedAddresses.size(); i++) {
            TLImportedAddress importedAddressObject = this.archivedImportedAddresses.get(i);

            if (importedAddressObject.getPositionInWalletArray() > toMoveAddressObject.getPositionInWalletArray()) {
                this.archivedImportedAddresses.add(i, toMoveAddressObject);
                return;
            }
        }
        this.archivedImportedAddresses.add(toMoveAddressObject);
    }

    public void unarchiveAddress(int positionInWalletArray) {
        setArchived(positionInWalletArray, false);

        TLImportedAddress toMoveAddressObject = this.addressToPositionInWalletArrayDict.get(positionInWalletArray);

        this.archivedImportedAddresses.remove(toMoveAddressObject);
        for (int i = 0; i < this.importedAddresses.size(); i++) {
            TLImportedAddress importedAddressObject = this.importedAddresses.get(i);
            if (importedAddressObject.getPositionInWalletArray() > toMoveAddressObject.getPositionInWalletArray()) {
                this.importedAddresses.add(i, toMoveAddressObject);
                List<Integer> indexes = this.addressToIdxDict.get(toMoveAddressObject.getAddress());
                if (indexes == null) {
                    indexes = new ArrayList<Integer>();
                    indexes.add(i);
                    this.addressToIdxDict.put(toMoveAddressObject.getAddress(), indexes);
                }

                for (String address : this.addressToIdxDict.keySet()) {
                    List<Integer> indexes2 = new ArrayList<Integer>(this.addressToIdxDict.get(address));
                    for(Integer idx : indexes2) {
                        List<Integer> indexes3 = this.addressToIdxDict.get(address);
                        indexes3.remove(idx);
                        indexes3.add(new Integer(idx + new Integer(1)));
                    }
                }
                return;
            }
        }
        this.importedAddresses.add(toMoveAddressObject);
    }

    private boolean setArchived(int positionInWalletArray, boolean archive) {
        TLImportedAddress importedAddressObject = this.addressToPositionInWalletArrayDict.get(positionInWalletArray);

        importedAddressObject.setArchived(archive);
        if (this.accountAddressType == TLAccountAddressType.Imported) {
            this.appWallet.setImportedPrivateKeyArchive(archive, positionInWalletArray);
        } else if (this.accountAddressType == TLAccountAddressType.ImportedWatch) {
            this.appWallet.setWatchOnlyAddressArchive(archive, positionInWalletArray);
        }

        return true;
    }

    public boolean deleteAddress(int idx) {
        TLImportedAddress importedAddressObject = this.archivedImportedAddresses.get(idx);

        this.archivedImportedAddresses.remove(idx);
        if (this.accountAddressType == TLAccountAddressType.Imported) {
            this.appWallet.deleteImportedPrivateKey(importedAddressObject.getPositionInWalletArray());
        } else if (this.accountAddressType == TLAccountAddressType.ImportedWatch) {
            this.appWallet.deleteImportedWatchAddress(importedAddressObject.getPositionInWalletArray());
        }

        this.addressToPositionInWalletArrayDict.remove(importedAddressObject.getPositionInWalletArrayNumber());

        Map<Integer, TLImportedAddress> tmpDict = new HashMap<Integer, TLImportedAddress>(this.addressToPositionInWalletArrayDict);
        for (Integer key : tmpDict.keySet()) {
            TLImportedAddress ia = this.addressToPositionInWalletArrayDict.get(key);
            if (ia.getPositionInWalletArray() > importedAddressObject.getPositionInWalletArray()) {
                ia.setPositionInWalletArray(ia.getPositionInWalletArray()-1);
                this.addressToPositionInWalletArrayDict.put(ia.getPositionInWalletArrayNumber(), ia);
            }
        }

        if (importedAddressObject.getPositionInWalletArray() < this.addressToPositionInWalletArrayDict.size() - 1) {
            this.addressToPositionInWalletArrayDict.remove(this.addressToPositionInWalletArrayDict.size()-1);
        }

        return true;
    }

    public boolean hasFetchedAddressesData() {
        for (TLImportedAddress importedAddressObject : this.importedAddresses) {
            if (!importedAddressObject.hasFetchedAccountData()) {
                return false;
            }
        }
        return true;
    }
}
