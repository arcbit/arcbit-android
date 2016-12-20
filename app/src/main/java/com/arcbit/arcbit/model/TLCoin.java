package com.arcbit.arcbit.model;

import org.bitcoinj.core.Coin;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class TLCoin {

    private Coin coin;

    public enum TLBitcoinDenomination {
        BTC, mBTC, bits;
        private static CharSequence[] btcUnits = {"BTC", "mBTC", "bits"};

        public static CharSequence[] getBTCUnits() {
            return btcUnits;
        }

        public static String getBTCUnitString(TLBitcoinDenomination bitcoinDenomination) {
            if (bitcoinDenomination == BTC) {
                return (String) btcUnits[0];
            }
            if (bitcoinDenomination == mBTC) {
                return (String) btcUnits[1];
            }
            return (String) btcUnits[2];
        }

        public static TLBitcoinDenomination getBitcoinDenomination(int idx) {
            if (idx == 0) {
                return BTC;
            }
            if (idx == 1) {
                return mBTC;
            }
            return bits;
        }

        public static int getBTCUnitIdx(TLBitcoinDenomination bitcoinDenomination) {
            if (bitcoinDenomination == BTC) {
                return 0;
            }
            if (bitcoinDenomination == mBTC) {
                return 1;
            }
            return 2;
        }

        public static TLBitcoinDenomination toMyEnum(String myEnumString) {
            try {
                return valueOf(myEnumString);
            } catch (Exception ex) {
                return BTC;
            }
        }
    }

    public TLCoin(long satoshis) {
        this.coin = Coin.valueOf(satoshis);
    }

    private TLCoin(Coin coin) {
        this.coin = coin;
    }

    public static TLCoin fromString(String bitcoinAmount, TLBitcoinDenomination bitcoinDenomination) {
        if (bitcoinDenomination == TLCoin.TLBitcoinDenomination.BTC) {
            return new TLCoin(new BigDecimal(bitcoinAmount).multiply(BigDecimal.valueOf(100000000)).longValue());
        } else if (bitcoinDenomination == TLCoin.TLBitcoinDenomination.mBTC) {
            return new TLCoin(new BigDecimal(bitcoinAmount).multiply(BigDecimal.valueOf(100000)).longValue());
        } else {
            return new TLCoin(new BigDecimal(bitcoinAmount).multiply(BigDecimal.valueOf(100)).longValue());
        }
    }

    public static TLCoin zero() {
        return new TLCoin(0);
    }

    public static TLCoin one() {
        return new TLCoin(1);
    }

    public static TLCoin negativeOne() {
        return new TLCoin(-1);
    }

    public Coin getBTCNumber() {
        return this.coin;
    }

    public TLCoin add(TLCoin coin) {
        return new TLCoin(this.coin.add(coin.coin));
    }

    public TLCoin subtract(TLCoin coin) {
        return new TLCoin(this.coin.subtract(coin.coin));
    }

    public TLCoin multiply(TLCoin coin) {
        return new TLCoin(this.coin.multiply(coin.coin.value));
    }

    public TLCoin divide(TLCoin coin) {
        return new TLCoin(this.coin.divide(coin.coin));
    }

    public long toNumber() {
        return this.coin.value;
    }

    public String bigIntegerToBitcoinAmountString(TLBitcoinDenomination bitcoinDenomination) {

        if (bitcoinDenomination == TLCoin.TLBitcoinDenomination.BTC) {
            return this.coin.toPlainString();
            //return new DecimalFormat("#.########").format(this.bigIntegerToBitcoin());
        } else if (bitcoinDenomination == TLCoin.TLBitcoinDenomination.mBTC) {
            return new DecimalFormat("#.#####").format(this.bigIntegerToMilliBit());
            //return String.valueOf(this.bigIntegerToMilliBit().toString());
        } else {
            return new DecimalFormat("#.00").format(this.bigIntegerToBits());
            // have 0 in most single digit place means always get a 0.XX
            //return new DecimalFormat("0.00").format(this.bigIntegerToBits());
            //return String.valueOf(this.bigIntegerToBits().toString());
        }
    }

    public String toPlainString() {
        return this.coin.toPlainString();
    }

    public Double bigIntegerToBits() {
        return this.coin.value*0.01;
    }

    public Double bigIntegerToMilliBit() {
        return this.coin.value*0.00001;
    }

    public Double bigIntegerToBitcoin() {
        return this.coin.value*0.00000001;
    }

    public Boolean less(TLCoin coin) {
        return this.coin.isLessThan(coin.coin);
    }

    public Boolean lessOrEqual(TLCoin coin) {
        return this.coin.isLessThan(coin.coin) || this.coin.compareTo(coin.coin) == 0;
    }

    public Boolean greater(TLCoin coin) {
        return this.coin.isGreaterThan(coin.coin);
    }

    public Boolean greaterOrEqual(TLCoin coin) {
        return this.coin.isGreaterThan(coin.coin) || this.coin.compareTo(coin.coin) == 0;
    }

    public Boolean equalTo(TLCoin coin) {
        return this.coin.compareTo(coin.coin) == 0;
    }
}

