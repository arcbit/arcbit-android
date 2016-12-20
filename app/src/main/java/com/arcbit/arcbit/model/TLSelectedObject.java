package com.arcbit.arcbit.model;

import com.arcbit.arcbit.model.TLOperationsManager.TLDownloadState;
import com.arcbit.arcbit.model.TLWalletUtils.TLAccountType;
import com.arcbit.arcbit.model.TLWalletUtils.TLAccountTxType;
import com.arcbit.arcbit.model.TLSendFormData.TLSelectObjectType;

public class TLSelectedObject {
    private TLAccountObject accountObject;
    private TLImportedAddress importedAddress;

    public void setSelectedAccount(TLAccountObject accountObject) {
        this.accountObject = accountObject;
        this.importedAddress = null;
    }

    public void setSelectedAddress(TLImportedAddress importedAddress) {
        this.importedAddress = importedAddress;
        this.accountObject = null;
    }

    public TLDownloadState getDownloadState() {
        if (this.accountObject != null) {
            return this.accountObject.downloadState;
        } else if (this.importedAddress != null) {
            return this.importedAddress.downloadState;
        }

        return TLDownloadState.Failed;
    }

    public TLCoin getBalanceForSelectedObject() {
        if (this.accountObject != null) {
            return this.accountObject.getBalance();
        } else if (this.importedAddress != null) {
            return this.importedAddress.getBalance();
        }
        return null;
    }

    public String getLabelForSelectedObject() {
        if (this.accountObject != null) {
            return this.accountObject.getAccountName();
        } else if (this.importedAddress != null) {
            return this.importedAddress.getLabel();
        }
        return null;
    }

    public int getReceivingAddressesCount() {
        if (this.accountObject != null) {
            return this.accountObject.getReceivingAddressesCount();
        } else if (this.importedAddress != null) {
            return 1;
        }
        return 0;
    }

    public String getReceivingAddressForSelectedObject(int idx) {
        if (this.accountObject != null) {
            return this.accountObject.getReceivingAddress(idx);
        } else if (this.importedAddress != null) {
            return this.importedAddress.getAddress();
        }

        return null;
    }

    public String getStealthAddress() {
        if (this.accountObject != null && this.accountObject.getAccountType() != TLAccountType.ImportedWatch) {
            if (this.accountObject.stealthWallet != null) {
                return this.accountObject.stealthWallet.getStealthAddress();
            }
        }
        return null;
    }

    public boolean hasFetchedCurrentFromData() {
        if (this.accountObject != null) {
            return this.accountObject.hasFetchedAccountData();
        } else if (this.importedAddress != null) {
            return this.importedAddress.hasFetchedAccountData();
        }

        return true;
    }


    public boolean isAddressPartOfAccount(String address) {
        if (this.accountObject != null) {
            return this.accountObject.isAddressPartOfAccount(address);
        } else if (this.importedAddress != null) {
            return this.importedAddress.getAddress() == address;
        }

        return true;
    }

    public int getTxObjectCount() {
        if (this.accountObject != null) {
            return this.accountObject.getTxObjectCount();
        } else if (this.importedAddress != null) {
            return this.importedAddress.getTxObjectCount();
        }

        return 1;
    }

    public TLTxObject getTxObject(int txIdx) {
        if (this.accountObject != null) {
            return this.accountObject.getTxObject(txIdx);
        } else if (this.importedAddress != null) {
            return this.importedAddress.getTxObject(txIdx);
        }

        return null;
    }

    public TLCoin getAccountAmountChangeForTx(String txHash) {
        if (this.accountObject != null) {
            return this.accountObject.getAccountAmountChangeForTx(txHash);
        } else if (this.importedAddress != null) {
            return this.importedAddress.getAccountAmountChangeForTx(txHash);
        }

        return null;
    }

    public TLAccountTxType getAccountAmountChangeTypeForTx(String txHash) {
        if (this.accountObject != null) {
            return this.accountObject.getAccountAmountChangeTypeForTx(txHash);
        } else if (this.importedAddress != null) {
            return this.importedAddress.getAccountAmountChangeTypeForTx(txHash);
        }

        return TLAccountTxType.Send;
    }

    public TLSelectObjectType getSelectedObjectType() {
        if (this.accountObject != null) {
            return TLSelectObjectType.Account;
        } else {
            return TLSelectObjectType.Address;
        }
    }

    public Object getSelectedObject() {
        if (this.accountObject != null) {
            return this.accountObject;
        } else {
            return this.importedAddress;
        }
    }

    public TLAccountType getAccountType() {
        if (this.accountObject != null) {
            return this.accountObject.getAccountType();
        } else {
            return TLAccountType.Unknown;
        }
    }
}
