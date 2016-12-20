package com.arcbit.arcbit.model;

import org.spongycastle.util.encoders.Hex;

public class TLWalletUtils {
    public enum TLSendFromType {
        HDWallet,
        ColdWalletAccount,
        ImportedAccount,
        ImportedWatchAccount,
        ImportedAddress,
        ImportedWatchAddress;
        public static TLSendFromType toMyEnum (String myEnumString) {
            try {
                return valueOf(myEnumString);
            } catch (Exception ex) {
                return HDWallet;
            }
        }

        public static TLSendFromType getSendFromType(int idx) {
            if (idx == 0) {
                return HDWallet;
            }
            if (idx == 1) {
                return ColdWalletAccount;
            }
            if (idx == 2) {
                return ImportedAccount;
            }
            if (idx == 3) {
                return ImportedWatchAccount;
            }
            if (idx == 4) {
                return ImportedAddress;
            }
            if (idx == 5) {
                return ImportedWatchAddress;
            }
            return HDWallet;
        }

        public static int getSendFromTypeIdx(TLSendFromType type) {
            if (type == HDWallet) {
                return 0;
            }
            if (type == ColdWalletAccount) {
                return 1;
            }
            if (type == ImportedAccount) {
                return 2;
            }
            if (type == ImportedWatchAccount) {
                return 3;
            }
            if (type == ImportedAddress) {
                return 4;
            }
            if (type == ImportedWatchAddress) {
                return 5;
            }
            return 0;
        }
    }

    public enum TLAccountTxType {
        Send,
        Receive,
        MoveBetweenAccount
    }

    public enum TLAccountType {
        Unknown,
        ColdWallet,
        HDWallet,
        Imported,
        ImportedWatch;
    }

    public enum TLAccountAddressType {
        Imported,
        ImportedWatch
    }

    static boolean SHOULD_SAVE_ARCHIVED_ADDRESSES_IN_JSON = false;
    static boolean ENABLE_STEALTH_ADDRESS = true;

    static String dataToHexString(byte[] data) {
        return Hex.toHexString(data);
    }

    public static byte[] hexStringToData(String hexString){
        return Hex.decode(hexString);
    }

    public static boolean ENABLE_STEALTH_ADDRESS(){
        return ENABLE_STEALTH_ADDRESS;
    }
}

