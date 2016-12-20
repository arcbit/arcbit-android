package com.arcbit.arcbit.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.arcbit.arcbit.model.TLWalletUtils.TLAccountType;
import com.arcbit.arcbit.model.TLWalletJSONKeys.TLAccount;

public class TLAccounts {

    TLAppDelegate appDelegate;
    private TLWallet appWallet;
    private Map<Integer, TLAccountObject> accountsDict;
    private List<TLAccountObject> accountsArray = new ArrayList<TLAccountObject>();
    private List<TLAccountObject> archivedAccountsArray = new ArrayList<TLAccountObject>();
    private TLAccountType accountType;

    public TLAccounts(TLAppDelegate appDelegate, TLWallet appWallet, List<TLAccountObject> accountsArray, TLAccountType accountType) {
        this.appDelegate = appDelegate;
        this.appWallet = appWallet;
        this.accountType = accountType;

        this.accountsDict = new HashMap<Integer, TLAccountObject>(accountsArray.size());

        for (int i = 0; i < accountsArray.size(); i++) {
            TLAccountObject accountObject = accountsArray.get(i);
            if (accountObject.isArchived()) {
                this.archivedAccountsArray.add(accountObject);
            } else {
                this.accountsArray.add(accountObject);
            }

            accountObject.setPositionInWalletArray(i);
            this.accountsDict.put(i, accountObject);
        }
    }

    public TLAccountObject addAccountWithExtendedKey(String extendedKey, String accountName) {
        assert(accountType != TLAccountType.HDWallet); // "accountType == TLAccountTypeHDWallet"
        TLAccountObject accountObject;

        if (accountType == TLAccountType.ColdWallet) {
            accountObject = this.appWallet.addColdWalletAccount(extendedKey);
        } else if (accountType == TLAccountType.Imported) {
            accountObject = this.appWallet.addImportedAccount(extendedKey);
        } else {
            accountObject = this.appWallet.addWatchOnlyAccount(extendedKey);
        }
        this.accountsArray.add(accountObject);
        int positionInWalletArray = this.getNumberOfAccounts()+getNumberOfArchivedAccounts()-1;
        accountObject.setPositionInWalletArray(positionInWalletArray);
        this.accountsDict.put(accountObject.getPositionInWalletArray(), accountObject);

        renameAccount(positionInWalletArray, accountName);

        return accountObject;
    }

    private boolean addAccount(TLAccountObject accountObject) {
        assert(accountType == TLAccountType.HDWallet);// "accountType != TLAccountTypeHDWallet"
        assert(this.accountsDict.get(accountObject.getAccountIdxNumber()) == null);
        this.accountsDict.put(accountObject.getAccountIdxNumber(), accountObject);
        this.accountsArray.add(accountObject);
        return true;
    }

    public boolean renameAccount(int accountIdxNumber, String accountName) {
        if (accountType == TLAccountType.HDWallet) {
            TLAccountObject accountObject = this.accountsDict.get(accountIdxNumber);
            accountObject.renameAccount(accountName);
            this.appWallet.renameAccount(accountObject.getAccountIdxNumber(), accountName);
        } else  {
            TLAccountObject accountObject = this.getAccountObjectForAccountIdxNumber(accountIdxNumber);
            accountObject.renameAccount(accountName);
            if (accountType == TLAccountType.ColdWallet) {
                this.appWallet.setColdWalletAccountName(accountName, accountIdxNumber);
            } else if (accountType == TLAccountType.Imported) {
                this.appWallet.setImportedAccountName(accountName, accountIdxNumber);
            } else if (accountType == TLAccountType.ImportedWatch) {
                this.appWallet.setWatchOnlyAccountName(accountName, accountIdxNumber);
            }
        }
        return true;
    }

    //in this context accountIdx is not the accountID, accountIdx is simply the order in which i want to display the accounts, neccessary cuz accounts can be deleted and such,
    public TLAccountObject getAccountObjectForIdx(int idx) {
        return this.accountsArray.get(idx);
    }

    public TLAccountObject getArchivedAccountObjectForIdx(int idx) {
        return this.archivedAccountsArray.get(idx);
    }

    public int getIdxForAccountObject(TLAccountObject accountObject) {
        return this.accountsArray.indexOf(accountObject);
    }

    public int getNumberOfAccounts() {
        return this.accountsArray.size();
    }

    public int getNumberOfArchivedAccounts() {
        return this.archivedAccountsArray.size();
    }

    public TLAccountObject getAccountObjectForAccountIdxNumber(int accountIdxNumber) {
        return this.accountsDict.get(accountIdxNumber);
    }

    public void archiveAccount(int positionInWalletArray) {
        setArchiveAccount(positionInWalletArray, true);

        TLAccountObject toMoveAccountObject = this.accountsDict.get(positionInWalletArray);

        this.accountsArray.remove(toMoveAccountObject);
        for (int i = 0; i < this.archivedAccountsArray.size(); i++) {
            TLAccountObject accountObject = this.archivedAccountsArray.get(i);

            if (accountObject.getPositionInWalletArray() > toMoveAccountObject.getPositionInWalletArray()) {
                this.archivedAccountsArray.add(i, toMoveAccountObject);
                return;
            }
        }
        this.archivedAccountsArray.add(toMoveAccountObject);
    }

    public void unarchiveAccount(int positionInWalletArray) {
        setArchiveAccount(positionInWalletArray, false);

        TLAccountObject toMoveAccountObject = this.accountsDict.get(positionInWalletArray);

        this.archivedAccountsArray.remove(toMoveAccountObject);
        for (int i = 0; i < this.accountsArray.size(); i++) {
            TLAccountObject accountObject = this.accountsArray.get(i);
            if (accountObject.getPositionInWalletArray() > toMoveAccountObject.getPositionInWalletArray()) {
                this.accountsArray.add(i, toMoveAccountObject);
                return;
            }
        }
        this.accountsArray.add(toMoveAccountObject);
    }

    private void setArchiveAccount(int accountIdxNumber, boolean enabled) {
        TLAccountObject accountObject = getAccountObjectForAccountIdxNumber(accountIdxNumber);
        accountObject.archiveAccount(enabled);

        if (accountType == TLAccountType.HDWallet) {
            this.appWallet.archiveAccountHDWallet(accountIdxNumber, enabled);
        } else if (accountType == TLAccountType.ColdWallet) {
            this.appWallet.archiveAccountColdWalletAccount(accountIdxNumber, enabled);
        } else if (accountType == TLAccountType.Imported) {
            this.appWallet.archiveAccountImportedAccount(accountIdxNumber, enabled);
        } else if (accountType == TLAccountType.ImportedWatch) {
            this.appWallet.archiveAccountImportedWatchAccount(accountIdxNumber, enabled);
        }
    }

    private TLAccountObject getAccountWithAccountName(String accountName) {
        for (Integer key : this.accountsDict.keySet()) {
            TLAccountObject accountObject = this.accountsDict.get(key);
            if (accountObject.getAccountName().equals(accountName)) {
                return accountObject;
            }
        }
        return null;
    }

    public boolean accountNameExist(String accountName) {
        return getAccountWithAccountName(accountName) == null ? false : true;
    }

    public TLAccountObject createNewAccount(String accountName, TLAccount accountType) {
        TLAccountObject accountObject = this.appWallet.createNewAccount(accountName, TLAccount.Normal, true);
        accountObject.updateAccountNeedsRecovering(false);
        addAccount(accountObject);
        return accountObject;
    }

    public TLAccountObject createNewAccount(String accountName, TLAccount accountType, boolean preloadStartingAddresses) {
        TLAccountObject accountObject = this.appWallet.createNewAccount(accountName, TLAccount.Normal, preloadStartingAddresses);
        addAccount(accountObject);
        return accountObject;
    }

    public boolean popTopAccount() {
        if (this.accountsArray.size() <= 0) {
            return false;
        }

        TLAccountObject accountObject = this.accountsArray.get(this.accountsArray.size()-1);
        this.accountsDict.remove(accountObject.getAccountIdxNumber());

        this.accountsArray.remove(this.accountsArray.size()-1);
        this.appWallet.removeTopAccount();
        return true;
    }

    public boolean deleteAccount(int idx) {
        assert(accountType != TLAccountType.HDWallet); //"accountType == TLAccountTypeHDWallet"

        TLAccountObject accountObject = this.archivedAccountsArray.get(idx);
        this.archivedAccountsArray.remove(idx);

        if (accountType == TLAccountType.ColdWallet) {
            this.appWallet.deleteColdWalletAccount(accountObject.getPositionInWalletArray());
        } else if (accountType == TLAccountType.Imported) {
            this.appWallet.deleteImportedAccount(accountObject.getPositionInWalletArray());
        } else if (accountType == TLAccountType.ImportedWatch) {
            this.appWallet.deleteWatchOnlyAccount(accountObject.getPositionInWalletArray());
        }

        this.accountsDict.remove(accountObject.getPositionInWalletArray());

        Map<Integer, TLAccountObject> tmpDict = new HashMap<Integer, TLAccountObject>(this.accountsDict);
        for (Integer key : tmpDict.keySet()) {
            TLAccountObject ao = this.accountsDict.get(key);
            if (ao.getPositionInWalletArray() > accountObject.getPositionInWalletArray()) {
                ao.setPositionInWalletArray(ao.getPositionInWalletArray()-1);
                this.accountsDict.put(ao.getPositionInWalletArray(), ao);
            }
        }

        if (accountObject.getPositionInWalletArray() < this.accountsDict.size() - 1) {
            this.accountsDict.remove(this.accountsDict.size()-1);
        }

        return true;
    }
}
