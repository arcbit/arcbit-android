package com.arcbit.arcbit.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import  com.arcbit.arcbit.model.TLCoin.TLBitcoinDenomination;

public class TLCurrencyFormat {
    static ArrayList<String> _currencyNames;
    static ArrayList<String> _currencies;
    static ArrayList<String> _currencySymbols;
    static ArrayList<String> _bitcoinDisplays;
    static ArrayList<String> _bitcoinDisplayWords;
    TLAppDelegate appDelegate;
    NumberFormat formatter;
    private DecimalFormat fiatFormat = null;
    private DecimalFormat btcFormat = null;
    private DecimalFormat mBtcFormat = null;
    private DecimalFormat uBtcFormat = null;

    public TLCurrencyFormat(TLAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
        // work around to ancient unresolved bug
        // http://stackoverflow.com/questions/3821539/decimal-separator-comma-with-numberdecimal-inputtype-in-edittext
        // http://stackoverflow.com/questions/20655130/inputtype-numberdecimal-doesnt-accept-comma-even-when-using-androiddigits
        // https://code.google.com/p/android/issues/detail?id=2626
        //Locale locale = Locale.getDefault();
        Locale locale = Locale.US;
        fiatFormat = (DecimalFormat)NumberFormat.getInstance(locale);
        fiatFormat.setMaximumFractionDigits(2);
        fiatFormat.setMinimumFractionDigits(2);
        fiatFormat.setGroupingUsed(false);

        btcFormat = (DecimalFormat)NumberFormat.getInstance(locale);
        btcFormat.setMaximumFractionDigits(8);
        btcFormat.setMinimumFractionDigits(1);
        btcFormat.setGroupingUsed(false);

        mBtcFormat = (DecimalFormat)NumberFormat.getInstance(locale);
        mBtcFormat.setMaximumFractionDigits(5);
        mBtcFormat.setMinimumFractionDigits(1);
        mBtcFormat.setGroupingUsed(false);

        uBtcFormat = (DecimalFormat)NumberFormat.getInstance(locale);
        uBtcFormat.setMaximumFractionDigits(2);
        uBtcFormat.setMinimumFractionDigits(2);
        uBtcFormat.setGroupingUsed(false);

        formatter = NumberFormat.getCurrencyInstance();
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);
        DecimalFormatSymbols decimalFormatSymbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol("");
        ((DecimalFormat) formatter).setDecimalFormatSymbols(decimalFormatSymbols);
    }

    public boolean isDecimalComma(Locale locale) {
        //ex: locale == Locale.FRANCE -> true     locale == Locale.US -> false
        DecimalFormat format = (DecimalFormat)NumberFormat.getInstance(locale);
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(2);
        DecimalFormatSymbols symbolsF = format.getDecimalFormatSymbols();
        symbolsF.setGroupingSeparator(' ');
        format.setDecimalFormatSymbols(symbolsF);
        String formatted = format.format(1.1);
        return formatted.contains(",");
    }

    public String bigIntegerToBitcoinAmountString(TLCoin coin, TLBitcoinDenomination bitcoinDenomination) {

        if (bitcoinDenomination == TLCoin.TLBitcoinDenomination.BTC) {
            return btcFormat.format(coin.bigIntegerToBitcoin());
        } else if (bitcoinDenomination == TLCoin.TLBitcoinDenomination.mBTC) {
            return mBtcFormat.format(coin.bigIntegerToMilliBit());
        } else {
            return uBtcFormat.format(coin.bigIntegerToBits());
        }
    }

    public TLCoin coinFromString(String bitcoinAmount, TLBitcoinDenomination bitcoinDenomination) {
        if (bitcoinDenomination == TLCoin.TLBitcoinDenomination.BTC) {
            return new TLCoin(new BigDecimal(bitcoinAmount).multiply(BigDecimal.valueOf(100000000)).longValue());
        } else if (bitcoinDenomination == TLCoin.TLBitcoinDenomination.mBTC) {
            return new TLCoin(new BigDecimal(bitcoinAmount).multiply(BigDecimal.valueOf(100000)).longValue());
        } else {
            return new TLCoin(new BigDecimal(bitcoinAmount).multiply(BigDecimal.valueOf(100)).longValue());
        }
    }

    public TLCoin coinAmountFromFiat(String currency, String fiatAmountString) {
        try {
            Double fiatAmount = fiatFormat.parse(fiatAmountString).doubleValue();
            return appDelegate.exchangeRate.bitcoinAmountFromFiat(currency, fiatAmount);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public TLCoin bitcoinAmountStringToCoin(String amount) {
        return amountStringToCoin(amount, TLBitcoinDenomination.BTC);
    }

    public TLCoin properBitcoinAmountStringToCoin(String amount) {
        return amountStringToCoin(amount, this.appDelegate.preferences.getBitcoinDenomination());
    }

    private TLCoin amountStringToCoin(String amount, TLBitcoinDenomination bitcoinDenomination) {
        if (amount.length() != 0) {
            if (!amount.matches("[0123456789.,]+")) {
                return TLCoin.zero();
            } else {
                //TODO maybe need better way to solve this issue of comma base decimals
                amount = amount.replaceAll(Pattern.quote(","), ".");
                return coinFromString(amount, bitcoinDenomination);
            }
        } else {
            return TLCoin.zero();
        }
    }

    public String coinToProperBitcoinAmountString(TLCoin amount) {
        return coinToProperBitcoinAmountString(amount, false);
    }

    public String coinToProperBitcoinAmountString(TLCoin amount, boolean withCode) {
        if (amount == null) {
            return null;
        }
        if (withCode) {
            if (!isDecimalComma(Locale.getDefault())) {
                return bigIntegerToBitcoinAmountString(amount, this.appDelegate.preferences.getBitcoinDenomination()) + " " + appDelegate.currencyFormat.getBitcoinDisplay();
            } else {
                return bigIntegerToBitcoinAmountString(amount, this.appDelegate.preferences.getBitcoinDenomination()).replace('.', ',') + " " + appDelegate.currencyFormat.getBitcoinDisplay();
            }
        } else {
            if (!isDecimalComma(Locale.getDefault())) {
                return bigIntegerToBitcoinAmountString(amount, this.appDelegate.preferences.getBitcoinDenomination());
            } else {
                return bigIntegerToBitcoinAmountString(amount, this.appDelegate.preferences.getBitcoinDenomination()).replace('.', ',');
            }
        }
    }

    public String coinToProperFiatAmountString(TLCoin amount, boolean withCode) {
        String currency = getFiatCurrency();
        String fiatAmount = appDelegate.exchangeRate.fiatAmountStringFromBTC(currency, amount);
        if (withCode) {
            if (!isDecimalComma(Locale.getDefault())) {
                return fiatAmount + " " + currency;
            } else {
                return fiatAmount.replace('.', ',') + " " + currency;
            }
        } else {
            if (!isDecimalComma(Locale.getDefault())) {
                return fiatAmount;
            } else {
                return fiatAmount.replace('.', ',');
            }
        }
    }

    public String fiatAmountStringFromBitcoin(String currency, TLCoin bitcoinAmount) {
        String fiatAmount = appDelegate.exchangeRate.fiatAmountStringFromBTC(currency, bitcoinAmount);
        if (!isDecimalComma(Locale.getDefault())) {
            return fiatAmount;
        } else {
            return fiatAmount.replace('.', ',');
        }
    }

    public String getProperAmount(TLCoin amount) {
        String balance;
        if (this.appDelegate.preferences.isDisplayLocalCurrency()) {
            String currency = this.getProperCurrency();
            balance = fiatAmountStringFromBitcoin(currency, amount);
        } else {
            balance = coinToProperBitcoinAmountString(amount);
        }

        balance = balance + " " + getProperCurrency();
        return balance;
    }

    public String getCurrencyName() {
        return getCurrencyNameArray().get(this.appDelegate.preferences.getCurrencyIdx());
    }

    public String getCurrencySymbol() {
        return getCurrencySymbolArray().get(this.appDelegate.preferences.getCurrencyIdx());
    }

    public String getFiatCurrency() {
        return getCurrencyArray().get(this.appDelegate.preferences.getCurrencyIdx());
    }

    public String getProperCurrency() {
        if (this.appDelegate.preferences.isDisplayLocalCurrency()) {
            return getCurrencyArray().get(this.appDelegate.preferences.getCurrencyIdx());
        } else {
            return getBitcoinDisplay();
        }
    }

    public String getBitcoinDisplay() {
        TLBitcoinDenomination bitcoinDenomination = this.appDelegate.preferences.getBitcoinDenomination();
        if (bitcoinDenomination == TLBitcoinDenomination.BTC) {
            return getBitcoinDisplayArray().get(0);
        } else if (bitcoinDenomination == TLBitcoinDenomination.mBTC) {
            return getBitcoinDisplayArray().get(1);
        } else {
            return getBitcoinDisplayArray().get(2);
        }
    }

    public String getBitcoinDisplayWord() {
        TLBitcoinDenomination bitcoinDenomination = this.appDelegate.preferences.getBitcoinDenomination();
        if (bitcoinDenomination == TLBitcoinDenomination.BTC) {
            return getBitcoinDisplayWordArray().get(0);
        } else if (bitcoinDenomination == TLBitcoinDenomination.mBTC) {
            return getBitcoinDisplayWordArray().get(1);
        } else {
            return getBitcoinDisplayWordArray().get(2);
        }
    }

    public static ArrayList<String> getBitcoinDisplayArray() {
        if (_bitcoinDisplays == null) {
            _bitcoinDisplays = new ArrayList<String>();
            _bitcoinDisplays.add("BTC");
            _bitcoinDisplays.add("mBTC");
            _bitcoinDisplays.add("uBTC");
        }
        return _bitcoinDisplays;
    }

    static ArrayList<String> getBitcoinDisplayWordArray() {
        if (_bitcoinDisplayWords == null) {
            _bitcoinDisplayWords = new ArrayList<String>();
            _bitcoinDisplayWords.add("Bitcoin");
            _bitcoinDisplayWords.add("MilliBit");
            _bitcoinDisplayWords.add("Bits");
        }
        return _bitcoinDisplayWords;
    }

    static ArrayList<String> getCurrencySymbolArray() {
        if (_currencySymbols == null) {
            _currencySymbols = new ArrayList<String>();
            _currencySymbols.add("$");
            _currencySymbols.add("R$");
            _currencySymbols.add("$");
            _currencySymbols.add("CHF");
            _currencySymbols.add("$");
            _currencySymbols.add("¥");
            _currencySymbols.add("kr");
            _currencySymbols.add("€");
            _currencySymbols.add("£");
            _currencySymbols.add("$");
            _currencySymbols.add("kr");
            _currencySymbols.add("¥");
            _currencySymbols.add("₩");
            _currencySymbols.add("$");
            _currencySymbols.add("zł");
            _currencySymbols.add("RUB");
            _currencySymbols.add("kr");
            _currencySymbols.add("$");
            _currencySymbols.add("฿");
            _currencySymbols.add("$");
            _currencySymbols.add("$");

            _currencySymbols.add("D");
            _currencySymbols.add("N");
            _currencySymbols.add("L");
            _currencySymbols.add("D");
            _currencySymbols.add("G");
            _currencySymbols.add("A");
            _currencySymbols.add("S");
            _currencySymbols.add("G");
            _currencySymbols.add("N");
            _currencySymbols.add("M");
            _currencySymbols.add("D");
            _currencySymbols.add("T");
            _currencySymbols.add("N");
            _currencySymbols.add("D");
            _currencySymbols.add("F");
            _currencySymbols.add("D");
            _currencySymbols.add("D");
            _currencySymbols.add("B");
            _currencySymbols.add("D");
            _currencySymbols.add("N");
            _currencySymbols.add("P");
            _currencySymbols.add("R");
            _currencySymbols.add("D");
            _currencySymbols.add("F");
            _currencySymbols.add("F");
            _currencySymbols.add("P");
            _currencySymbols.add("C");
            _currencySymbols.add("E");
            _currencySymbols.add("K");
            _currencySymbols.add("F");
            _currencySymbols.add("P");
            _currencySymbols.add("D");
            _currencySymbols.add("K");
            _currencySymbols.add("P");
            _currencySymbols.add("B");
            _currencySymbols.add("D");
            _currencySymbols.add("P");
            _currencySymbols.add("L");
            _currencySymbols.add("S");
            _currencySymbols.add("P");
            _currencySymbols.add("D");
            _currencySymbols.add("F");
            _currencySymbols.add("Q");
            _currencySymbols.add("D");
            _currencySymbols.add("L");
            _currencySymbols.add("K");
            _currencySymbols.add("G");
            _currencySymbols.add("F");
            _currencySymbols.add("R");
            _currencySymbols.add("S");
            _currencySymbols.add("R");
            _currencySymbols.add("D");
            _currencySymbols.add("P");
            _currencySymbols.add("D");
            _currencySymbols.add("D");
            _currencySymbols.add("S");
            _currencySymbols.add("S");
            _currencySymbols.add("R");
            _currencySymbols.add("F");
            _currencySymbols.add("D");
            _currencySymbols.add("D");
            _currencySymbols.add("T");
            _currencySymbols.add("K");
            _currencySymbols.add("P");
            _currencySymbols.add("R");
            _currencySymbols.add("D");
            _currencySymbols.add("L");
            _currencySymbols.add("L");
            _currencySymbols.add("L");
            _currencySymbols.add("D");
            _currencySymbols.add("D");
            _currencySymbols.add("L");
            _currencySymbols.add("A");
            _currencySymbols.add("D");
            _currencySymbols.add("K");
            _currencySymbols.add("T");
            _currencySymbols.add("P");
            _currencySymbols.add("O");
            _currencySymbols.add("R");
            _currencySymbols.add("R");
            _currencySymbols.add("K");
            _currencySymbols.add("N");
            _currencySymbols.add("R");
            _currencySymbols.add("N");
            _currencySymbols.add("D");
            _currencySymbols.add("N");
            _currencySymbols.add("O");
            _currencySymbols.add("K");
            _currencySymbols.add("R");
            _currencySymbols.add("R");
            _currencySymbols.add("B");
            _currencySymbols.add("N");
            _currencySymbols.add("K");
            _currencySymbols.add("P");
            _currencySymbols.add("R");
            _currencySymbols.add("G");
            _currencySymbols.add("R");
            _currencySymbols.add("N");
            _currencySymbols.add("D");
            _currencySymbols.add("F");
            _currencySymbols.add("R");
            _currencySymbols.add("D");
            _currencySymbols.add("R");
            _currencySymbols.add("G");
            _currencySymbols.add("P");
            _currencySymbols.add("L");
            _currencySymbols.add("S");
            _currencySymbols.add("D");
            _currencySymbols.add("D");
            _currencySymbols.add("C");
            _currencySymbols.add("P");
            _currencySymbols.add("L");
            _currencySymbols.add("S");
            _currencySymbols.add("T");
            _currencySymbols.add("D");
            _currencySymbols.add("P");
            _currencySymbols.add("Y");
            _currencySymbols.add("D");
            _currencySymbols.add("S");
            _currencySymbols.add("H");
            _currencySymbols.add("X");
            _currencySymbols.add("U");
            _currencySymbols.add("S");
            _currencySymbols.add("F");
            _currencySymbols.add("D");
            _currencySymbols.add("V");
            _currencySymbols.add("T");
            _currencySymbols.add("F");
            _currencySymbols.add("G");
            _currencySymbols.add("U");
            _currencySymbols.add("D");
            _currencySymbols.add("F");
            _currencySymbols.add("F");
            _currencySymbols.add("R");
            _currencySymbols.add("R");
            _currencySymbols.add("W");
            _currencySymbols.add("L");
        }
        return _currencySymbols;
    }

    static ArrayList<String> getCurrencyArray() {
        if (_currencies == null) {
            _currencies = new ArrayList<String>();
            _currencies.add("AUD");
            _currencies.add("BRL");
            _currencies.add("CAD");
            _currencies.add("CHF");
            _currencies.add("CLP");
            _currencies.add("CNY");
            _currencies.add("DKK");
            _currencies.add("EUR");
            _currencies.add("GBP");
            _currencies.add("HKD");
            _currencies.add("ISK");
            _currencies.add("JPY");
            _currencies.add("KRW");
            _currencies.add("NZD");
            _currencies.add("PLN");
            _currencies.add("RUB");
            _currencies.add("SEK");
            _currencies.add("SGD");
            _currencies.add("THB");
            _currencies.add("TWD");
            _currencies.add("USD");

            _currencies.add("AED");
            _currencies.add("AFN");
            _currencies.add("ALL");
            _currencies.add("AMD");
            _currencies.add("ANG");
            _currencies.add("AOA");
            _currencies.add("ARS");
            _currencies.add("AWG");
            _currencies.add("AZN");
            _currencies.add("BAM");
            _currencies.add("BBD");
            _currencies.add("BDT");
            _currencies.add("BGN");
            _currencies.add("BHD");
            _currencies.add("BIF");
            _currencies.add("BMD");
            _currencies.add("BND");
            _currencies.add("BOB");
            _currencies.add("BSD");
            _currencies.add("BTN");
            _currencies.add("BWP");
            _currencies.add("BYR");
            _currencies.add("BZD");
            _currencies.add("CDF");
            _currencies.add("CLF");
            _currencies.add("COP");
            _currencies.add("CRC");
            _currencies.add("CVE");
            _currencies.add("CZK");
            _currencies.add("DJF");
            _currencies.add("DOP");
            _currencies.add("DZD");
            _currencies.add("EEK");
            _currencies.add("EGP");
            _currencies.add("ETB");
            _currencies.add("FJD");
            _currencies.add("FKP");
            _currencies.add("GEL");
            _currencies.add("GHS");
            _currencies.add("GIP");
            _currencies.add("GMD");
            _currencies.add("GNF");
            _currencies.add("GTQ");
            _currencies.add("GYD");
            _currencies.add("HNL");
            _currencies.add("HRK");
            _currencies.add("HTG");
            _currencies.add("HUF");
            _currencies.add("IDR");
            _currencies.add("ILS");
            _currencies.add("INR");
            _currencies.add("IQD");
            _currencies.add("JEP");
            _currencies.add("JMD");
            _currencies.add("JOD");
            _currencies.add("KES");
            _currencies.add("KGS");
            _currencies.add("KHR");
            _currencies.add("KMF");
            _currencies.add("KWD");
            _currencies.add("KYD");
            _currencies.add("KZT");
            _currencies.add("LAK");
            _currencies.add("LBP");
            _currencies.add("LKR");
            _currencies.add("LRD");
            _currencies.add("LSL");
            _currencies.add("LTL");
            _currencies.add("LVL");
            _currencies.add("LYD");
            _currencies.add("MAD");
            _currencies.add("MDL");
            _currencies.add("MGA");
            _currencies.add("MKD");
            _currencies.add("MMK");
            _currencies.add("MNT");
            _currencies.add("MOP");
            _currencies.add("MRO");
            _currencies.add("MUR");
            _currencies.add("MVR");
            _currencies.add("MWK");
            _currencies.add("MXN");
            _currencies.add("MYR");
            _currencies.add("MZN");
            _currencies.add("NAD");
            _currencies.add("NGN");
            _currencies.add("NIO");
            _currencies.add("NOK");
            _currencies.add("NPR");
            _currencies.add("OMR");
            _currencies.add("PAB");
            _currencies.add("PEN");
            _currencies.add("PGK");
            _currencies.add("PHP");
            _currencies.add("PKR");
            _currencies.add("PYG");
            _currencies.add("QAR");
            _currencies.add("RON");
            _currencies.add("RSD");
            _currencies.add("RWF");
            _currencies.add("SAR");
            _currencies.add("SBD");
            _currencies.add("SCR");
            _currencies.add("SDG");
            _currencies.add("SHP");
            _currencies.add("SLL");
            _currencies.add("SOS");
            _currencies.add("SRD");
            _currencies.add("STD");
            _currencies.add("SVC");
            _currencies.add("SYP");
            _currencies.add("SZL");
            _currencies.add("TJS");
            _currencies.add("TMT");
            _currencies.add("TND");
            _currencies.add("TOP");
            _currencies.add("TRY");
            _currencies.add("TTD");
            _currencies.add("TZS");
            _currencies.add("UAH");
            _currencies.add("UGX");
            _currencies.add("UYU");
            _currencies.add("UZS");
            _currencies.add("VEF");
            _currencies.add("VND");
            _currencies.add("VUV");
            _currencies.add("WST");
            _currencies.add("XAF");
            _currencies.add("XAG");
            _currencies.add("XAU");
            _currencies.add("XCD");
            _currencies.add("XOF");
            _currencies.add("XPF");
            _currencies.add("YER");
            _currencies.add("ZAR");
            _currencies.add("ZMW");
            _currencies.add("ZWL");
        }
        return _currencies;
    }

    static public ArrayList<String> getCurrencyNameArray() {
        if (_currencyNames == null) {
            _currencyNames = new ArrayList<String>();
            _currencyNames.add("$ - AUD");
            _currencyNames.add("R$ - BRL");
            _currencyNames.add("$ - CAD");
            _currencyNames.add("CHF - CHF");
            _currencyNames.add("$ - CLP");
            _currencyNames.add("¥ - CNY");
            _currencyNames.add("kr - DKK");
            _currencyNames.add("€ - EUR");
            _currencyNames.add("£ - GBP");
            _currencyNames.add("$ - HKD");
            _currencyNames.add("kr - ISK");
            _currencyNames.add("¥ - JPY");
            _currencyNames.add("₩ - KRW");
            _currencyNames.add("$ - NZD");
            _currencyNames.add("zł - PLN");
            _currencyNames.add("RUB - RUB");
            _currencyNames.add("kr - SEK");
            _currencyNames.add("$ - SGD");
            _currencyNames.add("฿ - THB");
            _currencyNames.add("$ - TWD");
            _currencyNames.add("$ - USD");

            _currencyNames.add("UAE Dirham - AED");
            _currencyNames.add("Afghan Afghani - AFN");
            _currencyNames.add("Albanian Lek - ALL");
            _currencyNames.add("Armenian Dram - AMD");
            _currencyNames.add("Netherlands Antillean Guilder - ANG");
            _currencyNames.add("Angolan Kwanza - AOA");
            _currencyNames.add("Argentine Peso - ARS");
            _currencyNames.add("Aruban Florin - AWG");
            _currencyNames.add("Azerbaijani Manat - AZN");
            _currencyNames.add("Bosnia-Herzegovina Convertible Mark - BAM");
            _currencyNames.add("Barbadian Dollar - BBD");
            _currencyNames.add("Bangladeshi Taka - BDT");
            _currencyNames.add("Bulgarian Lev - BGN");
            _currencyNames.add("Bahraini Dinar - BHD");
            _currencyNames.add("Burundian Franc - BIF");
            _currencyNames.add("Bermudan Dollar - BMD");
            _currencyNames.add("Brunei Dollar - BND");
            _currencyNames.add("Bolivian Boliviano - BOB");
            _currencyNames.add("Bahamian Dollar - BSD");
            _currencyNames.add("Bhutanese Ngultrum - BTN");
            _currencyNames.add("Botswanan Pula - BWP");
            _currencyNames.add("Belarusian Ruble - BYR");
            _currencyNames.add("Belize Dollar - BZD");
            _currencyNames.add("Congolese Franc - CDF");
            _currencyNames.add("Chilean Unit of Account (UF) - CLF");
            _currencyNames.add("Colombian Peso - COP");
            _currencyNames.add("Costa Rican Colón - CRC");
            _currencyNames.add("Cape Verdean Escudo - CVE");
            _currencyNames.add("Czech Koruna - CZK");
            _currencyNames.add("Djiboutian Franc - DJF");
            _currencyNames.add("Dominican Peso - DOP");
            _currencyNames.add("Algerian Dinar - DZD");
            _currencyNames.add("Estonian Kroon - EEK");
            _currencyNames.add("Egyptian Pound - EGP");
            _currencyNames.add("Ethiopian Birr - ETB");
            _currencyNames.add("Fijian Dollar - FJD");
            _currencyNames.add("Falkland Islands Pound - FKP");
            _currencyNames.add("Georgian Lari - GEL");
            _currencyNames.add("Ghanaian Cedi - GHS");
            _currencyNames.add("Gibraltar Pound - GIP");
            _currencyNames.add("Gambian Dalasi - GMD");
            _currencyNames.add("Guinean Franc - GNF");
            _currencyNames.add("Guatemalan Quetzal - GTQ");
            _currencyNames.add("Guyanaese Dollar - GYD");
            _currencyNames.add("Honduran Lempira - HNL");
            _currencyNames.add("Croatian Kuna - HRK");
            _currencyNames.add("Haitian Gourde - HTG");
            _currencyNames.add("Hungarian Forint - HUF");
            _currencyNames.add("Indonesian Rupiah - IDR");
            _currencyNames.add("Israeli Shekel - ILS");
            _currencyNames.add("Indian Rupee - INR");
            _currencyNames.add("Iraqi Dinar - IQD");
            _currencyNames.add("Jersey Pound - JEP");
            _currencyNames.add("Jamaican Dollar - JMD");
            _currencyNames.add("Jordanian Dinar - JOD");
            _currencyNames.add("Kenyan Shilling - KES");
            _currencyNames.add("Kyrgystani Som - KGS");
            _currencyNames.add("Cambodian Riel - KHR");
            _currencyNames.add("Comorian Franc - KMF");
            _currencyNames.add("Kuwaiti Dinar - KWD");
            _currencyNames.add("Cayman Islands Dollar - KYD");
            _currencyNames.add("Kazakhstani Tenge - KZT");
            _currencyNames.add("Laotian Kip - LAK");
            _currencyNames.add("Lebanese Pound - LBP");
            _currencyNames.add("Sri Lankan Rupee - LKR");
            _currencyNames.add("Liberian Dollar - LRD");
            _currencyNames.add("Lesotho Loti - LSL");
            _currencyNames.add("Lithuanian Litas - LTL");
            _currencyNames.add("Latvian Lats - LVL");
            _currencyNames.add("Libyan Dinar - LYD");
            _currencyNames.add("Moroccan Dirham - MAD");
            _currencyNames.add("Moldovan Leu - MDL");
            _currencyNames.add("Malagasy Ariary - MGA");
            _currencyNames.add("Macedonian Denar - MKD");
            _currencyNames.add("Myanma Kyat - MMK");
            _currencyNames.add("Mongolian Tugrik - MNT");
            _currencyNames.add("Macanese Pataca - MOP");
            _currencyNames.add("Mauritanian Ouguiya - MRO");
            _currencyNames.add("Mauritian Rupee - MUR");
            _currencyNames.add("Maldivian Rufiyaa - MVR");
            _currencyNames.add("Malawian Kwacha - MWK");
            _currencyNames.add("Mexican Peso - MXN");
            _currencyNames.add("Malaysian Ringgit - MYR");
            _currencyNames.add("Mozambican Metical - MZN");
            _currencyNames.add("Namibian Dollar - NAD");
            _currencyNames.add("Nigerian Naira - NGN");
            _currencyNames.add("Nicaraguan Córdoba - NIO");
            _currencyNames.add("Norwegian Krone - NOK");
            _currencyNames.add("Nepalese Rupee - NPR");
            _currencyNames.add("Omani Rial - OMR");
            _currencyNames.add("Panamanian Balboa - PAB");
            _currencyNames.add("Peruvian Nuevo Sol - PEN");
            _currencyNames.add("Papua New Guinean Kina - PGK");
            _currencyNames.add("Philippine Peso - PHP");
            _currencyNames.add("Pakistani Rupee - PKR");
            _currencyNames.add("Paraguayan Guarani - PYG");
            _currencyNames.add("Qatari Rial - QAR");
            _currencyNames.add("Romanian Leu - RON");
            _currencyNames.add("Serbian Dinar - RSD");
            _currencyNames.add("Rwandan Franc - RWF");
            _currencyNames.add("Saudi Riyal - SAR");
            _currencyNames.add("Solomon Islands Dollar - SBD");
            _currencyNames.add("Seychellois Rupee - SCR");
            _currencyNames.add("Sudanese Pound - SDG");
            _currencyNames.add("Saint Helena Pound - SHP");
            _currencyNames.add("Sierra Leonean Leone - SLL");
            _currencyNames.add("Somali Shilling - SOS");
            _currencyNames.add("Surinamese Dollar - SRD");
            _currencyNames.add("São Tomé and Príncipe Dobra - STD");
            _currencyNames.add("Salvadoran Colón - SVC");
            _currencyNames.add("Syrian Pound - SYP");
            _currencyNames.add("Swazi Lilangeni - SZL");
            _currencyNames.add("Tajikistani Somoni - TJS");
            _currencyNames.add("Turkmenistani Manat - TMT");
            _currencyNames.add("Tunisian Dinar - TND");
            _currencyNames.add("Tongan Paʻanga - TOP");
            _currencyNames.add("Turkish Lira - TRY");
            _currencyNames.add("Trinidad and Tobago Dollar - TTD");
            _currencyNames.add("Tanzanian Shilling - TZS");
            _currencyNames.add("Ukrainian Hryvnia - UAH");
            _currencyNames.add("Ugandan Shilling - UGX");
            _currencyNames.add("Uruguayan Peso - UYU");
            _currencyNames.add("Uzbekistan Som - UZS");
            _currencyNames.add("Venezuelan Bolívar Fuerte - VEF");
            _currencyNames.add("Vietnamese Dong - VND");
            _currencyNames.add("Vanuatu Vatu - VUV");
            _currencyNames.add("Samoan Tala - WST");
            _currencyNames.add("CFA Franc BEAC - XAF");
            _currencyNames.add("Silver (troy ounce) - XAG");
            _currencyNames.add("Gold (troy ounce) - XAU");
            _currencyNames.add("East Caribbean Dollar - XCD");
            _currencyNames.add("CFA Franc BCEAO - XOF");
            _currencyNames.add("CFP Franc - XPF");
            _currencyNames.add("Yemeni Rial - YER");
            _currencyNames.add("South African Rand - ZAR");
            _currencyNames.add("Zambian Kwacha - ZMW");
            _currencyNames.add("Zimbabwean Dollar - ZWL");
        }
        return _currencyNames;
    }
}
