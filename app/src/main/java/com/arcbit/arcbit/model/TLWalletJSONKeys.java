package com.arcbit.arcbit.model;


import java.util.HashMap;
import java.util.Map;

public class TLWalletJSONKeys {
        public enum TLAccount { Normal, Multisig;}

        public enum TLAddressStatus {
                Archived(0), Active(1);
                private final int value;
                private TLAddressStatus(int value) {
                        this.value = value;
                }
                public int getValue() {
                        return value;
                }
        }

        public enum TLAddressType {
                Main(0), Change(1), Stealth(2);
                private final int value;
                private TLAddressType(int value) {
                        this.value = value;
                }
                public int getValue() {
                        return value;
                }
        }

        public enum TLStealthPaymentStatus {
                Unspent(0), Claimed(1), Spent(2);
                private final int value;
                private TLStealthPaymentStatus(int value) {
                        this.value = value;
                }
                public int getValue() {
                        return value;
                }
        }

        static public final String WALLET_PAYLOAD_VERSION = "1";
        static public final String WALLET_PAYLOAD_KEY_VERSION = "version";
        static public final String WALLET_PAYLOAD_KEY_PAYLOAD = "payload";
        static public final String WALLET_PAYLOAD_KEY_WALLETS = "wallets";
        static public final String WALLET_PAYLOAD_KEY_HDWALLETS = "hd_wallets";
        static public final String  WALLET_PAYLOAD_KEY_ACCOUNTS = "accounts";
        static public final String WALLET_PAYLOAD_CURRENT_ACCOUNT_ID = "current_account_id";
        static public final String WALLET_PAYLOAD_IMPORTS = "imports";
        static public final String WALLET_PAYLOAD_COLD_WALLET_ACCOUNTS = "cold_wallet_accounts";
        static public final String WALLET_PAYLOAD_IMPORTED_ACCOUNTS = "imported_accounts";
        static public final String WALLET_PAYLOAD_WATCH_ONLY_ACCOUNTS = "watch_only_accounts";
        static public final String WALLET_PAYLOAD_IMPORTED_PRIVATE_KEYS = "imported_private_keys";
        static public final String WALLET_PAYLOAD_WATCH_ONLY_ADDRESSES = "watch_only_addresses";
        static public final String WALLET_PAYLOAD_ACCOUNT_IDX = "account_idx";
        static public final String WALLET_PAYLOAD_EXTENDED_PRIVATE_KEY = "xprv";
        static public final String WALLET_PAYLOAD_EXTENDED_PUBLIC_KEY = "xpub";
        static public final String WALLET_PAYLOAD_ACCOUNT_NEEDS_RECOVERING = "needs_recovering";
        static public final String WALLET_PAYLOAD_KEY_MAIN_ADDRESSES = "main_addresses";
        static public final String WALLET_PAYLOAD_KEY_CHANGE_ADDRESSES = "change_addresses";
        static public final String WALLET_PAYLOAD_KEY_STEALTH_ADDRESSES = "stealth_addresses";
        static public final String WALLET_PAYLOAD_KEY_STEALTH_ADDRESS = "stealth_address";
        static public final String WALLET_PAYLOAD_KEY_STEALTH_ADDRESS_SCAN_KEY = "scan_key";
        static public final String WALLET_PAYLOAD_KEY_STEALTH_ADDRESS_SPEND_KEY = "spend_key";
        static public final String WALLET_PAYLOAD_KEY_PAYMENTS = "payments";
        static public final String WALLET_PAYLOAD_KEY_SERVERS = "servers";
        static public final String WALLET_PAYLOAD_KEY_WATCHING = "watching";
        static public final String WALLET_PAYLOAD_KEY_TXID = "txid";
        static public final String WALLET_PAYLOAD_KEY_MIN_MAIN_ADDRESS_IDX = "min_main_address_idx";
        static public final String WALLET_PAYLOAD_KEY_MIN_CHANGE_ADDRESS_IDX = "min_change_address_idx";
        static public final String WALLET_PAYLOAD_KEY_TIME = "time";
        static public final String WALLET_PAYLOAD_KEY_CHECK_TIME = "check_time";
        static public final String WALLET_PAYLOAD_KEY_LAST_TX_TIME = "last_tx_time";
        static public final String WALLET_PAYLOAD_KEY_KEY = "key";
        static public final String WALLET_PAYLOAD_KEY_ADDRESS = "address";
        static public final String WALLET_PAYLOAD_KEY_STATUS = "status";
        static public final String WALLET_PAYLOAD_KEY_INDEX = "index";
        static public final String WALLET_PAYLOAD_KEY_LABEL = "label";
        static public final String WALLET_PAYLOAD_KEY_NAME = "name";
        static public final String WALLET_PAYLOAD_KEY_MAX_ACCOUNTS_CREATED = "max_account_id_created";
        static public final String WALLET_PAYLOAD_KEY_MASTER_HEX = "master_hex";
        static public final String WALLET_PAYLOAD_KEY_PASSPHRASE = "passphrase";
        static public final String WALLET_PAYLOAD_KEY_ADDRESS_BOOK = "address_book";
        static public final String WALLET_PAYLOAD_KEY_TRANSACTION_TAGS = "tx_tags";
}