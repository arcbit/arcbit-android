package com.arcbit.arcbit.ui;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.UiThread;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;

import com.arcbit.arcbit.model.TLCoin;
import com.arcbit.arcbit.model.TLCurrencyFormat;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.model.TLTransactionFee;
import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLCoin.TLBitcoinDenomination;

import com.arcbit.arcbit.R;
import com.arcbit.arcbit.APIs.TLTxFeeAPI;
import com.arcbit.arcbit.ui.utils.TLPrompts;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    private static final String TAG = SettingsFragment.class.getName();

    private TLAppDelegate appDelegate = TLAppDelegate.instance();

    private SwitchPreference enabledPinPref;
    private Preference changePinCodePref;
    private SwitchPreference displayLocalCurrencyPref;
    private Preference unitsPref;
    private Preference fiatPref;

    private SwitchPreference enabledDynamicFeePref;
    private Preference transactionFeeAmountPref;
    private Preference dynamicFeeOptionsPref;

    private Preference backupPassphrasePref;
    private Preference restoreWalletPref;

    private Preference advancedSettingsPref;
    private Preference aboutPref;

    static public boolean shouldHandlePin = true; // FIXME

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshList();
        shouldHandlePin = true;
    }

    @UiThread
    private void refreshList() {
        PreferenceScreen prefScreen = getPreferenceScreen();
        if(prefScreen != null)prefScreen.removeAll();
        addPreferencesFromResource(R.xml.settings);

        enabledPinPref = (SwitchPreference) findPreference("enable_pin_code");
        enabledPinPref.setChecked(appDelegate.preferences.isEnablePINCode());

        changePinCodePref = findPreference("change_pin_code");
        changePinCodePref.setOnPreferenceClickListener(SettingsFragment.this);
        if (!appDelegate.preferences.isEnablePINCode()) {
            PreferenceCategory preferencesCategory = (PreferenceCategory) findPreference("preferences");
            preferencesCategory.removePreference(changePinCodePref);
        }
        displayLocalCurrencyPref = (SwitchPreference) findPreference("display_local_currency");
        displayLocalCurrencyPref.setChecked(appDelegate.preferences.isDisplayLocalCurrency());
        unitsPref = findPreference("units");
        unitsPref.setSummary(getDisplayUnits());
        unitsPref.setOnPreferenceClickListener(SettingsFragment.this);
        fiatPref = findPreference("fiat");
        fiatPref.setSummary(appDelegate.currencyFormat.getCurrencyName());
        fiatPref.setOnPreferenceClickListener(SettingsFragment.this);

        enabledDynamicFeePref = (SwitchPreference) findPreference("enable_dynamic_fee");
        enabledDynamicFeePref.setChecked(appDelegate.preferences.enabledDynamicFee());
        transactionFeeAmountPref = findPreference("fixed_fee_amount");
        TLCoin amount =  new TLCoin(appDelegate.preferences.getTransactionFee());
        transactionFeeAmountPref.setSummary(appDelegate.currencyFormat.coinToProperBitcoinAmountString(amount) + " " + appDelegate.currencyFormat.getBitcoinDisplay());
        transactionFeeAmountPref.setOnPreferenceClickListener(SettingsFragment.this);
        dynamicFeeOptionsPref = findPreference("dynamic_fee_options");

        TLTxFeeAPI.TLDynamicFeeSetting dynamicFeeOption = appDelegate.preferences.getDynamicFeeOption();
        appDelegate.preferences.setDynamicFeeOption(dynamicFeeOption);
        if (dynamicFeeOption == TLTxFeeAPI.TLDynamicFeeSetting.FastestFee) {
            dynamicFeeOptionsPref.setSummary(getString(R.string.as_fast_as_possible));
        } else if (dynamicFeeOption == TLTxFeeAPI.TLDynamicFeeSetting.HalfHourFee) {
            dynamicFeeOptionsPref.setSummary(getString(R.string.within_half_hour_90_probability));
        } else if (dynamicFeeOption == TLTxFeeAPI.TLDynamicFeeSetting.HourFee) {
            dynamicFeeOptionsPref.setSummary(getString(R.string.within_an_hour_90_probability));
        }

        dynamicFeeOptionsPref.setOnPreferenceClickListener(this);
        PreferenceCategory transactionFeeCategory = (PreferenceCategory) findPreference("transaction_fee");
        if (appDelegate.preferences.enabledDynamicFee()) {
            transactionFeeCategory.removePreference(transactionFeeAmountPref);
        } else {
            transactionFeeCategory.removePreference(dynamicFeeOptionsPref);
        }

        backupPassphrasePref = findPreference("key_backup_passphrase");
        backupPassphrasePref.setOnPreferenceClickListener(this);
        restoreWalletPref = findPreference("key_restore_wallet");
        restoreWalletPref.setOnPreferenceClickListener(this);

        advancedSettingsPref = findPreference("key_advanced_settings");
        advancedSettingsPref.setOnPreferenceClickListener(this);

        aboutPref = findPreference("key_about");
        aboutPref.setOnPreferenceClickListener(this);

        findPreference("enable_pin_code").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT && !shouldHandlePin) {
                    shouldHandlePin = true;
                    if (appDelegate.preferences.isEnablePINCode()) {
                        appDelegate.encryptedPreferences.setPINValue(null);
                        appDelegate.preferences.setEnablePINCode(false);
                        PreferenceCategory preferencesCategory = (PreferenceCategory) findPreference("preferences");
                        preferencesCategory.removePreference(changePinCodePref);
                    }
                    return true;
                }
                Intent intent = new Intent(getActivity(), PinActivity.class);
                if (appDelegate.preferences.isEnablePINCode()) {
                    appDelegate.encryptedPreferences.setPINValue(null);
                    appDelegate.preferences.setEnablePINCode(false);
                    PreferenceCategory preferencesCategory = (PreferenceCategory) findPreference("preferences");
                    preferencesCategory.removePreference(changePinCodePref);
                } else {
                    intent.putExtra(PinFragment.PIN_OPTION, PinFragment.CREATE_NEW_PIN);
                    startActivityForResult(intent, 0);
                }
                return true;
            }
        });

        findPreference("display_local_currency").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                appDelegate.preferences.setDisplayLocalCurrency(!appDelegate.preferences.isDisplayLocalCurrency());
                displayLocalCurrencyPref.setChecked(appDelegate.preferences.isDisplayLocalCurrency());
                return true;
            }
        });

        findPreference("enable_dynamic_fee").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                appDelegate.preferences.setEnabledDynamicFee(!appDelegate.preferences.enabledDynamicFee());
                enabledDynamicFeePref.setChecked(appDelegate.preferences.enabledDynamicFee());
                refreshList();
                return true;
            }
        });
    }

    private String getDisplayUnits() {
        TLBitcoinDenomination bitcoinDenomination = appDelegate.preferences.getBitcoinDenomination();
        return TLBitcoinDenomination.getBTCUnitString(bitcoinDenomination);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        shouldHandlePin = false;
        refreshList();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {

            case "change_pin_code":
                Intent intent = new Intent(getActivity(), PinActivity.class);
                intent = new Intent(getActivity(), PinActivity.class);
                intent.putExtra(PinFragment.PIN_OPTION, PinFragment.CHANGE_PIN);
                startActivityForResult(intent, 0);
                break;

            case "units":
                showDialogBTCUnits();
                break;

            case "fiat":
                showDialogFiatUnits();
                break;

            case "dynamic_fee_options":
                showPromptDynamicFeeOptions();
                break;

            case "fixed_fee_amount":
                showPromptForSetTransactionFee();
                break;

            case "key_backup_passphrase":
                startActivity(new Intent(getActivity(), PassphraseActivity.class));
                break;

            case "key_restore_wallet":
                startActivity(new Intent(getActivity(), RestoreWalletActivity.class));
                break;

            case "key_advanced_settings":
                AdvancedSettingsFragment settingsFragment = new AdvancedSettingsFragment();
                FragmentTransaction transaction1 = getFragmentManager().beginTransaction();
                transaction1.setCustomAnimations(R.animator.right_slide_in, R.animator.left_slide_out, R.animator.left_slide_in, R.animator.right_slide_out);
                transaction1.replace(R.id.content_frame, settingsFragment);
                transaction1.addToBackStack(null);
                transaction1.commit();
                break;

            case "key_about":
                try {
                    PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                    String version = pInfo.versionName;
                    int verCode = pInfo.versionCode;
                    TLPrompts.promptSuccessMessage(getActivity(), "", "Version Name: " + version +"\nVersion Code: " + verCode);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.d(TAG, "NameNotFoundException: " + e.getLocalizedMessage());
                }
                break;
        }

        return true;
    }

    private void showDialogBTCUnits() {
        final CharSequence[] units = TLCoin.TLBitcoinDenomination.getBTCUnits();
        TLBitcoinDenomination bitcoinDenomination = appDelegate.preferences.getBitcoinDenomination();

        final int sel = TLBitcoinDenomination.getBTCUnitIdx(bitcoinDenomination);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_units)
                .setSingleChoiceItems(units, sel, (dialog, which) -> {
                            appDelegate.preferences.setBitcoinDisplay(TLBitcoinDenomination.getBitcoinDenomination(which));
                            unitsPref.setSummary(getDisplayUnits());
                            dialog.dismiss();
                            refreshList();
                        }
                ).show();
    }

    private void showDialogFiatUnits() {
        final String[] currencies = TLCurrencyFormat.getCurrencyNameArray().toArray(new String[0]);
        int selected = appDelegate.preferences.getCurrencyIdx();
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_currency)
                .setSingleChoiceItems(currencies, selected, (dialog, which) -> {
                            appDelegate.preferences.setCurrency(which);
                            fiatPref.setSummary(appDelegate.currencyFormat.getCurrencyName());
                            dialog.dismiss();
                            refreshList();
                        }
                ).show();
    }

    private void showPromptForSetTransactionFee() {
        TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.transaction_fee), getString(R.string.set_transaction_fee_in, appDelegate.currencyFormat.getBitcoinDisplay()),
                "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        String feeAmount = (String) obj;
                        if (feeAmount != null && !feeAmount.isEmpty()) {
                            TLCoin feeAmountCoin = appDelegate.currencyFormat.properBitcoinAmountStringToCoin(feeAmount);
                            appDelegate.preferences.setTransactionFee(feeAmountCoin.toNumber());
                            LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_CHANGE_AUTOMATIC_TX_FEE));
                            refreshList();
                        }
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    private void showPromptDynamicFeeOptions() {
        final CharSequence[] dynamicFeeOptions = {
                getString(R.string.as_fast_as_possible),
                getString(R.string.within_half_hour_90_probability),
                getString(R.string.within_an_hour_90_probability)
        };

        TLTxFeeAPI.TLDynamicFeeSetting dynamicFeeOption = appDelegate.preferences.getDynamicFeeOption();

        final int sel = TLTxFeeAPI.TLDynamicFeeSetting.getDynamicFeeOptionIdx(dynamicFeeOption);

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.fee_adjusted_to_confirm))
                .setSingleChoiceItems(dynamicFeeOptions, sel, (dialog, which) -> {
                            TLTxFeeAPI.TLDynamicFeeSetting newDynamicFeeOption = TLTxFeeAPI.TLDynamicFeeSetting.getDynamicFeeOption(which);
                            appDelegate.preferences.setDynamicFeeOption(newDynamicFeeOption);
                            if (newDynamicFeeOption == TLTxFeeAPI.TLDynamicFeeSetting.FastestFee) {
                                dynamicFeeOptionsPref.setSummary(getString(R.string.as_fast_as_possible));
                            } else if (newDynamicFeeOption == TLTxFeeAPI.TLDynamicFeeSetting.HalfHourFee) {
                                dynamicFeeOptionsPref.setSummary(getString(R.string.within_half_hour_90_probability));
                            } else if (newDynamicFeeOption == TLTxFeeAPI.TLDynamicFeeSetting.HourFee) {
                                dynamicFeeOptionsPref.setSummary(getString(R.string.within_an_hour_90_probability));
                            }

                            dialog.dismiss();
                            refreshList();
                        }
                ).show();
    }
}
