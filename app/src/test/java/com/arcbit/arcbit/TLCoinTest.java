package com.arcbit.arcbit;

import com.arcbit.arcbit.model.TLCoin;

import org.bitcoinj.utils.BriefLogFormatter;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TLCoinTest {
    private static final Logger log = LoggerFactory.getLogger(TLCoinTest.class);

    @Before
    public void setUp() throws Exception {
        BriefLogFormatter.init();
    }

    @Test
    public void testArithmetic() {
        TLCoin coin = new TLCoin(1000);
        assertTrue(coin.toNumber() == 1000);
        TLCoin c1 = coin.add(new TLCoin(1000));
        assertTrue(c1.toNumber() == 2000);
        TLCoin c2 = coin.subtract(new TLCoin(1000));
        assertTrue(c2.toNumber() == 0);
        TLCoin c3 = coin.multiply(new TLCoin(1000));
        assertTrue(c3.toNumber() == 1000000);
        TLCoin c4 = coin.divide(new TLCoin(1000));
        assertTrue(c4.toNumber() == 1);


        TLCoin sum = new TLCoin(5500821).add(new TLCoin(500000000));
        assertTrue(sum.toNumber() == 505500821);

        TLCoin a = new TLCoin(5500821);
        TLCoin b = new TLCoin(500000000);
        a.add(b);
        assertTrue(a.toNumber() == 5500821);
        a = a.add(b);
        assertTrue(a.toNumber() == 505500821);
    }

    @Test
    public void testInequality() {
        assertTrue(new TLCoin(100).less(new TLCoin(1000)));
        assertFalse(new TLCoin(1000).less(new TLCoin(1000)));
        assertFalse(new TLCoin(10000).less(new TLCoin(1000)));

        assertTrue(new TLCoin(100).lessOrEqual(new TLCoin(1000)));
        assertTrue(new TLCoin(1000).lessOrEqual(new TLCoin(1000)));
        assertFalse(new TLCoin(10000).lessOrEqual(new TLCoin(1000)));

        assertFalse(new TLCoin(100).greater(new TLCoin(1000)));
        assertFalse(new TLCoin(1000).greater(new TLCoin(1000)));
        assertTrue(new TLCoin(10000).greater(new TLCoin(1000)));

        assertFalse(new TLCoin(100).greaterOrEqual(new TLCoin(1000)));
        assertTrue(new TLCoin(1000).greaterOrEqual(new TLCoin(1000)));
        assertTrue(new TLCoin(10000).greaterOrEqual(new TLCoin(1000)));

        assertFalse(new TLCoin(100).equalTo(new TLCoin(1000)));
        assertTrue(new TLCoin(1000).equalTo(new TLCoin(1000)));
        assertFalse(new TLCoin(10000).equalTo(new TLCoin(1000)));
    }

    @Test
    public void testConversion() {
        assertTrue(new TLCoin(10000).bigIntegerToBitcoin() == 0.0001);
        assertTrue(new TLCoin(10000).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.BTC).equals("0.0001"));

        assertTrue(new TLCoin(1).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.BTC).equals("0.00000001"));
        assertTrue(new TLCoin(9).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.BTC).equals("0.00000009"));
        assertTrue(new TLCoin(10).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.BTC).equals("0.0000001"));
        assertTrue(new TLCoin(11).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.BTC).equals("0.00000011"));

        assertTrue(new TLCoin(99999999).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.BTC).equals("0.99999999"));
        assertTrue(new TLCoin(100000000).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.BTC).equals("1"));
        assertTrue(new TLCoin(100000001).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.BTC).equals("1.00000001"));


        assertTrue(new TLCoin(10000).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.mBTC).equals("0.1"));
        assertTrue(new TLCoin(1).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.mBTC).equals("0.00001"));
        assertTrue(new TLCoin(9999).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.mBTC).equals("0.09999"));
        assertTrue(new TLCoin(99999).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.mBTC).equals("0.99999"));
        assertTrue(new TLCoin(100000).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.mBTC).equals("1"));
        assertTrue(new TLCoin(100001).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.mBTC).equals("1.00001"));


        assertTrue(new TLCoin(10000).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.bits).equals("100.00"));
        assertTrue(new TLCoin(10010).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.bits).equals("100.10"));
        assertTrue(new TLCoin(10001).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.bits).equals("100.01"));

        assertTrue(new TLCoin(100).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.bits).equals("1.00"));
        assertTrue(new TLCoin(10).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.bits).equals(".10"));
        assertTrue(new TLCoin(1).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.bits).equals(".01"));

        assertTrue(new TLCoin(999).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.bits).equals("9.99"));
        assertTrue(new TLCoin(99).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.bits).equals(".99"));
        assertTrue(new TLCoin(9).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.bits).equals(".09"));


        assertTrue(TLCoin.fromString("0.010000", TLCoin.TLBitcoinDenomination.BTC).bigIntegerToBitcoinAmountString(TLCoin.TLBitcoinDenomination.BTC).equals("0.01"));

        assertTrue(TLCoin.fromString("0.010000", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 1000000);
        assertTrue(TLCoin.fromString("0.010000", TLCoin.TLBitcoinDenomination.mBTC).toNumber() == 1000);
        assertTrue(TLCoin.fromString("0.010000", TLCoin.TLBitcoinDenomination.bits).toNumber() == 1);

        assertTrue(TLCoin.fromString("1", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 100000000);
        assertTrue(TLCoin.fromString("1.0", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 100000000);
        assertTrue(TLCoin.fromString("1.00", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 100000000);
        assertTrue(TLCoin.fromString("1.000000009", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 100000000);
        assertTrue(TLCoin.fromString("1.000000019", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 100000001);
        assertTrue(TLCoin.fromString("1.00000001", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 100000001);
        assertTrue(TLCoin.fromString("0.99999999", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 99999999);
        assertTrue(TLCoin.fromString("1.99999999", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 199999999);

        // test values outside decimal precision bounds
        assertTrue(TLCoin.fromString("0.000000009", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 0);
        assertTrue(TLCoin.fromString("0.00000001", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 1);
        assertTrue(TLCoin.fromString("0.000000019", TLCoin.TLBitcoinDenomination.BTC).toNumber() == 1);

        assertTrue(TLCoin.fromString("0.000009", TLCoin.TLBitcoinDenomination.mBTC).toNumber() == 0);
        assertTrue(TLCoin.fromString("0.00001", TLCoin.TLBitcoinDenomination.mBTC).toNumber() == 1);
        assertTrue(TLCoin.fromString("0.000019", TLCoin.TLBitcoinDenomination.mBTC).toNumber() == 1);

        assertTrue(TLCoin.fromString("0.019", TLCoin.TLBitcoinDenomination.bits).toNumber() == 1);
        assertTrue(TLCoin.fromString("0.009", TLCoin.TLBitcoinDenomination.bits).toNumber() == 0);
        assertTrue(TLCoin.fromString("0.001", TLCoin.TLBitcoinDenomination.bits).toNumber() == 0);
    }

    @Test
    public void testBitcoinAmountStringToCoin() {

    }
}
