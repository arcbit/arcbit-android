package com.arcbit.arcbit.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.UiThread;
import android.text.InputType;
import android.util.Log;
import android.webkit.URLUtil;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.APIs.TLBlockExplorerAPI;

import com.arcbit.arcbit.model.TLWalletUtils;
import com.arcbit.arcbit.ui.utils.TLPrompts;
import com.arcbit.arcbit.ui.utils.TLToast;
import com.arcbit.arcbit.R;

public class AdvancedSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    private static final String TAG = AdvancedSettingsFragment.class.getName();
    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    private SwitchPreference enableAdvancedModePref;
    private SwitchPreference enableColdWalletPref;
    private SwitchPreference defaultReusableAddressesPref;
    private Preference blockExplorerAPIPref;
    private Preference blockExplorerURLPref;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((SettingsActivity)getActivity()).toolbar.setTitle(getString(R.string.advanced_settings));
        refreshList();
    }

    @UiThread
    private void refreshList() {
        PreferenceScreen prefScreen = getPreferenceScreen();
        if (prefScreen != null) prefScreen.removeAll();
        addPreferencesFromResource(R.xml.advance_settings);

        enableColdWalletPref = (SwitchPreference) findPreference("enable_cold_wallet");
        enableColdWalletPref.setSummary(getString(R.string.enable_cold_wallet_description));
        enableColdWalletPref.setChecked(appDelegate.preferences.enabledColdWallet());

        enableAdvancedModePref = (SwitchPreference) findPreference("enable_advanced_mode");
        enableAdvancedModePref.setSummary(getString(R.string.enable_advanced_mode_description));
        enableAdvancedModePref.setChecked(appDelegate.preferences.enabledAdvancedMode());

        blockExplorerAPIPref = findPreference("block_explorer_api");
        TLBlockExplorerAPI.TLBlockExplorer blockExplorer = appDelegate.preferences.getBlockExplorerAPI();
        String blockExplorerAPIString = TLBlockExplorerAPI.TLBlockExplorer.getBlockExplorerAPIString(blockExplorer);
        blockExplorerAPIPref.setSummary(blockExplorerAPIString);
        blockExplorerAPIPref.setOnPreferenceClickListener(this);

        defaultReusableAddressesPref = (SwitchPreference) findPreference("default_reusable_addresses");
        defaultReusableAddressesPref.setSummary(getString(R.string.enable_default_reusable_address_description));
        defaultReusableAddressesPref.setChecked(appDelegate.preferences.enabledStealthAddressDefault());
        if (!TLWalletUtils.ENABLE_STEALTH_ADDRESS()) {
            PreferenceCategory category = (PreferenceCategory) findPreference("advanced_settings");
            category.removePreference(defaultReusableAddressesPref);
        }

        blockExplorerURLPref = findPreference("block_explorer_url");
        String url = appDelegate.preferences.getBlockExplorerURL(blockExplorer);
        blockExplorerURLPref.setSummary(url);
        blockExplorerURLPref.setOnPreferenceClickListener(this);
        if (blockExplorer == TLBlockExplorerAPI.TLBlockExplorer.Blockchain) {
            PreferenceCategory category = (PreferenceCategory) findPreference("block_explorer");
            category.removePreference(blockExplorerURLPref);
        }

        findPreference("enable_cold_wallet").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                appDelegate.preferences.setEnableColdWallet(!enableColdWalletPref.isChecked());
                return true;
            }
        });

        findPreference("enable_advanced_mode").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                appDelegate.preferences.setAdvancedMode(!enableAdvancedModePref.isChecked());
                return true;
            }
        });

        if (TLWalletUtils.ENABLE_STEALTH_ADDRESS()) {
            findPreference("default_reusable_addresses").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    appDelegate.preferences.setEnabledStealthAddressDefault(!defaultReusableAddressesPref.isChecked());
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "block_explorer_api":
                promptSelectBlockExplorerAPI();
                break;
            case "block_explorer_url":
                promptChangeBlockExplorerURL();
                break;
        }
        return true;
    }

    void promptSelectBlockExplorerAPI() {
        String blockExplorerAPIBlockchain = TLBlockExplorerAPI.TLBlockExplorer.getBlockExplorerAPIString(TLBlockExplorerAPI.TLBlockExplorer.Blockchain);
        String blockExplorerAPIInsight = TLBlockExplorerAPI.TLBlockExplorer.getBlockExplorerAPIString(TLBlockExplorerAPI.TLBlockExplorer.Insight);
        CharSequence[] otherButtons = {blockExplorerAPIBlockchain, blockExplorerAPIInsight};
        new AlertDialog.Builder(getActivity())
                .setItems(otherButtons, (dialog, which) -> {
                            if (which == 0) {
                                appDelegate.preferences.resetBlockExplorerAPIURL();
                                appDelegate.preferences.setBlockExplorerAPI(TLBlockExplorerAPI.TLBlockExplorer.Blockchain);
                                refreshList();
                            } else if (which == 1) {
                                appDelegate.preferences.resetBlockExplorerAPIURL();
                                appDelegate.preferences.setBlockExplorerAPI(TLBlockExplorerAPI.TLBlockExplorer.Insight);
                                refreshList();
                            }
                            TLPrompts.promptSuccessMessage(getActivity(), "", getString(R.string.exit_app_to_take_effect));
                        }
                ).show();
    }

    void promptChangeBlockExplorerURL() {
        if (appDelegate.preferences.getBlockExplorerAPI() != TLBlockExplorerAPI.TLBlockExplorer.Insight) {
            TLToast.makeText(getActivity(), getString(R.string.cannot_select_custom_url_for_this_block_explorer), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
            return;
        }
        TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.change_block_explorer_url), "", "", "https://", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
            @Override
            public void onSuccess(Object obj) {
                String url = (String) obj;
                if (URLUtil.isValidUrl(url)) {
                    appDelegate.preferences.setBlockExplorerURL(appDelegate.preferences.getBlockExplorerAPI(), url);
                    refreshList();
                    TLPrompts.promptSuccessMessage(getActivity(), "", getString(R.string.exit_app_to_take_effect));
                } else {
                    TLToast.makeText(getActivity(), getString(R.string.invalid_url), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
                }
            }

            @Override
            public void onCancel() {
            }
        });
    }
}