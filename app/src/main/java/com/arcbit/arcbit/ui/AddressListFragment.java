package com.arcbit.arcbit.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.support.v4.util.Pair;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.view.View.OnClickListener;

import com.arcbit.arcbit.ui.utils.TLPrompts;
import com.arcbit.arcbit.ui.utils.TLPrompts.PromptCallback;
import com.arcbit.arcbit.model.TLAccountObject;
import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLHDWalletWrapper;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.model.TLWalletJSONKeys;
import com.arcbit.arcbit.model.TLWalletUtils;
import com.arcbit.arcbit.ui.utils.TLToast;
import com.arcbit.arcbit.ui.items.*;

import com.arcbit.arcbit.R;
import com.arcbit.arcbit.utils.TLPermissionUtil;
import com.arcbit.arcbit.utils.TLUtils;
import com.google.zxing.client.android.CaptureActivity;

public class AddressListFragment extends android.support.v4.app.Fragment {
    public static final int SCAN_EXTENDED_PRIVATE_KEY = 1329;
    private ListView addressListview;
    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    private View rootView;
    private TLAccountObject accountObject;
    private boolean showBalances;
    private int rowCount;
    private TLWalletJSONKeys.TLAddressType selectedAddressType;
    private String selectedAddress;
    private AddressListAdapter addressListAdapter;

    private enum CellType {
        CellTypeNone, CellTypePaymentAddress, CellTypeMainActiveAddress, CellTypeMainArchivedAddress, CellTypeChangeActiveAddress, CellTypeChangeArchivedAddress;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(TLNotificationEvents.EVENT_EXCHANGE_RATE_UPDATED)) {
                if (addressListAdapter != null) {
                    addressListAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_address_list, container, false);
        if (appDelegate == null) {
            return rootView;
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_EXCHANGE_RATE_UPDATED));
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_ACCOUNT_ADDRESSES));

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getActivity().setTitle(R.string.addresses);
        showBalances = getArguments().getBoolean("showAddressListShowBalances");
        accountObject = appDelegate.viewAddressesAccountObject;

        updateListView();
        return rootView;
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arcbit_arrow_left));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        setupToolbar();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupToolbar();
    }


    public void openInWeb(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    Pair<CellType, Integer> getItemData(int position) {
        int offset = 1;
        int maxRange = 1;
        if (position == 0) {
            return new Pair<>(CellType.CellTypeNone, -1);
        }

        if (TLWalletUtils.ENABLE_STEALTH_ADDRESS()) {
            if (accountObject.getAccountType() != TLWalletUtils.TLAccountType.ImportedWatch && accountObject.getAccountType() != TLWalletUtils.TLAccountType.ColdWallet) {
                maxRange += Math.max(1, accountObject.stealthWallet.getStealthAddressPaymentsCount());
            } else {
                maxRange++;
            }

            if (position < maxRange) {
                if (accountObject.getAccountType() != TLWalletUtils.TLAccountType.ImportedWatch && accountObject.getAccountType() != TLWalletUtils.TLAccountType.ColdWallet) {
                    if (accountObject.stealthWallet.getStealthAddressPaymentsCount() == 0) {
                        return new Pair<>(CellType.CellTypeNone, -1);
                    } else {
                        return new Pair<>(CellType.CellTypePaymentAddress, accountObject.stealthWallet.getStealthAddressPaymentsCount()-(position-offset)-1);
                    }
                } else {
                    return new Pair<>(CellType.CellTypeNone, -1);
                }
            }
            if (accountObject.getAccountType() != TLWalletUtils.TLAccountType.ImportedWatch && accountObject.getAccountType() != TLWalletUtils.TLAccountType.ColdWallet) {
                if (accountObject.stealthWallet.getStealthAddressPaymentsCount() == 0) {
                    offset++;
                } else {
                    offset += accountObject.stealthWallet.getStealthAddressPaymentsCount();
                }
            } else {
                offset++;
            }
        } else {
            offset--;
            maxRange--;
        }

        if (accountObject.getMainActiveAddressesCount() != 0) {
            maxRange += accountObject.getMainActiveAddressesCount() + 1;
        } else {
            maxRange += 2;
        }
        if (position < maxRange) {
            if (position == offset) {
                return new Pair<>(CellType.CellTypeNone, -1);
            } else {
                if (accountObject.getMainActiveAddressesCount() != 0) {
                    return new Pair<>(CellType.CellTypeMainActiveAddress, accountObject.getMainActiveAddressesCount()-(position-offset));
                } else {
                    return new Pair<>(CellType.CellTypeNone, -1);
                }
            }
        }
        offset = maxRange;

        if (accountObject.getMainArchivedAddressesCount() != 0) {
            maxRange += accountObject.getMainArchivedAddressesCount() + 1;
        } else {
            maxRange += 2;
        }
        if (position < maxRange) {
            if (position == offset) {
                return new Pair<>(CellType.CellTypeNone, -1);
            } else {
                if (accountObject.getMainArchivedAddressesCount() != 0) {
                    return new Pair<>(CellType.CellTypeMainArchivedAddress, accountObject.getMainArchivedAddressesCount()-(position-offset));
                } else {
                    return new Pair<>(CellType.CellTypeNone, -1);
                }
            }
        }
        offset = maxRange;


        if (accountObject.getChangeActiveAddressesCount() != 0) {
            maxRange += accountObject.getChangeActiveAddressesCount() + 1;
        } else {
            maxRange += 2;
        }

        if (position < maxRange) {
            if (position == offset) {
                return new Pair<>(CellType.CellTypeNone, -1);
            } else {
                if (accountObject.getChangeActiveAddressesCount() != 0) {
                    return new Pair<>(CellType.CellTypeChangeActiveAddress, accountObject.getChangeActiveAddressesCount()-(position-offset));
                } else {
                    return new Pair<>(CellType.CellTypeNone, -1);
                }
            }
        }
        offset = maxRange;

        if (accountObject.getChangeArchivedAddressesCount() != 0) {
            maxRange += accountObject.getChangeArchivedAddressesCount() + 1;
        } else {
            maxRange += 2;
        }
        if (position < maxRange) {
            if (position == offset) {
                return new Pair<>(CellType.CellTypeNone, -1);
            } else {
                if (accountObject.getChangeArchivedAddressesCount() != 0) {
                    return new Pair<>(CellType.CellTypeChangeArchivedAddress, accountObject.getChangeArchivedAddressesCount()-(position-offset));
                } else {
                    return new Pair<>(CellType.CellTypeNone, -1);
                }
            }
        }

        return new Pair<>(CellType.CellTypeNone, -1);
    }

    void updateListView() {
        if (TLWalletUtils.ENABLE_STEALTH_ADDRESS()) {
            rowCount = 5; //num headers
            if (accountObject.getAccountType() != TLWalletUtils.TLAccountType.ImportedWatch && accountObject.getAccountType() != TLWalletUtils.TLAccountType.ColdWallet) {
                if (accountObject.stealthWallet.getStealthAddressPaymentsCount() == 0) {
                    rowCount++;
                } else {
                    rowCount += accountObject.stealthWallet.getStealthAddressPaymentsCount();
                }
            }
        } else {
            rowCount = 4; //num headers
        }
        if (accountObject.getMainActiveAddressesCount() == 0) {
            rowCount++;
        } else {
            rowCount += accountObject.getMainActiveAddressesCount();
        }
        if (accountObject.getChangeActiveAddressesCount() == 0) {
            rowCount++;
        } else {
            rowCount += accountObject.getChangeActiveAddressesCount();
        }
        if (accountObject.getMainArchivedAddressesCount() == 0) {
            rowCount++;
        } else {
            rowCount += accountObject.getMainArchivedAddressesCount();
        }
        if (accountObject.getChangeArchivedAddressesCount() == 0) {
            rowCount++;
        } else {
            rowCount += accountObject.getChangeArchivedAddressesCount();
        }
        addressListAdapter = new AddressListAdapter(rootView.getContext());

        addressListview = (ListView) rootView.findViewById(R.id.address_list_view);

        addressListview.setAdapter(addressListAdapter);
    }

    private class AddressListAdapter extends ArrayAdapter<Item> {

        private Context context;
        private LayoutInflater vi;

        public AddressListAdapter(Context context) {
            super(context, 0);
            this.context = context;
            vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return rowCount;
        }

        @Override
        public Item getItem(int position) {
            int offset = 1;
            int maxRange = 1;
            if (TLWalletUtils.ENABLE_STEALTH_ADDRESS()) {
                if (position == 0) {
                    return new SectionItem(getString(R.string.reusable_address_payment_addresses));
                }
                if (accountObject.getAccountType() != TLWalletUtils.TLAccountType.ImportedWatch && accountObject.getAccountType() != TLWalletUtils.TLAccountType.ColdWallet) {
                    maxRange += Math.max(1, accountObject.stealthWallet.getStealthAddressPaymentsCount());
                } else {
                    maxRange++;
                }

                if (position < maxRange) {
                    if (accountObject.getAccountType() != TLWalletUtils.TLAccountType.ImportedWatch && accountObject.getAccountType() != TLWalletUtils.TLAccountType.ColdWallet) {
                        if (accountObject.stealthWallet.getStealthAddressPaymentsCount() == 0) {
                            return new LeftTitleItem(getString(R.string.none_currently));
                        } else {
                            String address = accountObject.stealthWallet.getPaymentAddressForIndex(accountObject.stealthWallet.getStealthAddressPaymentsCount() - (position - offset) - 1);
                            if (!showBalances) {
                                return new LeftTitleItem(address);
                            }
                            String balance = appDelegate.currencyFormat.getProperAmount(accountObject.getAddressBalance(address));
                            return new EntryItem(balance, address);
                        }
                    } else {
                        return new LeftTitleItem(getString(R.string.this_account_type_cant_see_reusable_address_payments));
                    }
                }
                if (accountObject.getAccountType() != TLWalletUtils.TLAccountType.ImportedWatch && accountObject.getAccountType() != TLWalletUtils.TLAccountType.ColdWallet) {
                    if (accountObject.stealthWallet.getStealthAddressPaymentsCount() == 0) {
                        offset++;
                    } else {
                        offset += accountObject.stealthWallet.getStealthAddressPaymentsCount();
                    }
                } else {
                    offset++;
                }
            } else {
                offset--;
                maxRange--;
            }

            if (accountObject.getMainActiveAddressesCount() != 0) {
                maxRange += accountObject.getMainActiveAddressesCount() + 1;
            } else {
                maxRange += 2;
            }
            if (position < maxRange) {
                if (position == offset) {
                    return new SectionItem(getString(R.string.active_main_addresses));
                } else {
                    if (accountObject.getMainActiveAddressesCount() != 0) {
                        String address = accountObject.getMainActiveAddress(accountObject.getMainActiveAddressesCount()-(position-offset));
                        if (!showBalances) {
                            return new LeftTitleItem(address);
                        }
                        String balance = appDelegate.currencyFormat.getProperAmount(accountObject.getAddressBalance(address));
                        return new EntryItem(balance, address);
                    } else {
                        return new LeftTitleItem(getString(R.string.none_currently));
                    }
                }
            }
            offset = maxRange;

            if (accountObject.getMainArchivedAddressesCount() != 0) {
                maxRange += accountObject.getMainArchivedAddressesCount() + 1;
            } else {
                maxRange += 2;
            }
            if (position < maxRange) {
                if (position == offset) {
                    return new SectionItem(getString(R.string.archived_main_addresses));
                } else {
                    if (accountObject.getMainArchivedAddressesCount() != 0) {
                        String address = accountObject.getMainArchivedAddress(accountObject.getMainArchivedAddressesCount() - (position - offset));
                        return new LeftTitleItem(address);
                    } else {
                        return new LeftTitleItem(getString(R.string.none_currently));
                    }
                }
            }
            offset = maxRange;


            if (accountObject.getChangeActiveAddressesCount() != 0) {
                maxRange += accountObject.getChangeActiveAddressesCount() + 1;
            } else {
                maxRange += 2;
            }

            if (position < maxRange) {
                if (position == offset) {
                    return new SectionItem(getString(R.string.active_change_addresses));
                } else {
                    if (accountObject.getChangeActiveAddressesCount() != 0) {
                        String address = accountObject.getChangeActiveAddress(accountObject.getChangeActiveAddressesCount() - (position - offset));
                        if (!showBalances) {
                            return new LeftTitleItem(address);
                        }
                        String balance = appDelegate.currencyFormat.getProperAmount(accountObject.getAddressBalance(address));
                        return new EntryItem(balance, address);
                    } else {
                        return new LeftTitleItem(getString(R.string.none_currently));
                    }
                }
            }
            offset = maxRange;

            if (accountObject.getChangeArchivedAddressesCount() != 0) {
                maxRange += accountObject.getChangeArchivedAddressesCount() + 1;
            } else {
                maxRange += 2;
            }
            if (position < maxRange) {
                if (position == offset) {
                    return new SectionItem(getString(R.string.archived_change_addresses));
                } else {
                    if (accountObject.getChangeArchivedAddressesCount() != 0) {
                        String address = accountObject.getChangeArchivedAddress(accountObject.getChangeArchivedAddressesCount() - (position - offset));
                        return new LeftTitleItem(address);
                    } else {
                        return new LeftTitleItem(getString(R.string.none_currently));
                    }
                }
            }

            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            final Item i = getItem(position);
            if (i != null) {
                if(i.isSection()){
                    SectionItem si = (SectionItem)i;
                    v = vi.inflate(R.layout.list_item_section, null);

                    v.setOnClickListener(null);
                    v.setOnLongClickListener(null);
                    v.setLongClickable(false);

                    final TextView sectionView = (TextView) v.findViewById(R.id.list_item_section_text);
                    sectionView.setText(si.getTitle());
                }else{
                    if (i instanceof EntryItem) {
                        EntryItem ei = (EntryItem)i;
                        v = vi.inflate(R.layout.list_item_entry, null);
                        final Button button = (Button)v.findViewById(R.id.list_item_entry_title);
                        final TextView subtitle = (TextView)v.findViewById(R.id.list_item_entry_summary);
                        final ProgressBar progressBar = (ProgressBar)v.findViewById(R.id.balanceProgressBar);

                        if (button != null)
                            button.setText(ei.title);
                        if(subtitle != null)
                            subtitle.setText(ei.subtitle);
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        v.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Pair<CellType, Integer> itemData = getItemData(position);
                                if (itemData.first == CellType.CellTypePaymentAddress) {
                                    String address = accountObject.stealthWallet.getPaymentAddressForIndex(itemData.second);
                                    promptAddressActionSheet(getString(R.string.payment_index_colon) + " " + itemData.second, TLWalletJSONKeys.TLAddressType.Stealth, address);
                                } else if (itemData.first == CellType.CellTypeMainActiveAddress) {
                                    String address = accountObject.getMainActiveAddress(itemData.second);
                                    promptAddressActionSheet(getString(R.string.address_id_colon) + " " + itemData.second, TLWalletJSONKeys.TLAddressType.Main, address);
                                } else if (itemData.first == CellType.CellTypeChangeActiveAddress) {
                                    String address = accountObject.getChangeActiveAddress(itemData.second);
                                    promptAddressActionSheet(getString(R.string.address_id_colon) + " " + itemData.second, TLWalletJSONKeys.TLAddressType.Change, address);
                                }
                            }
                        });
                        assert button != null;
                        button.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                appDelegate.preferences.setDisplayLocalCurrency(!appDelegate.preferences.isDisplayLocalCurrency());
                                notifyDataSetChanged();

                            }
                        });
                    } else if (i instanceof LeftTitleItem) {
                        LeftTitleItem ei = (LeftTitleItem)i;
                        v = vi.inflate(R.layout.left_title_entry, null);
                        final TextView title = (TextView)v.findViewById(R.id.title);
                        if (title != null) {
                            title.setText(ei.title);
                        }
                        v.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Pair<CellType, Integer> itemData = getItemData(position);
                                if (itemData.first == CellType.CellTypePaymentAddress) {
                                    String address = accountObject.stealthWallet.getPaymentAddressForIndex(itemData.second);
                                    promptAddressActionSheet(getString(R.string.payment_index_colon) + " " + itemData.second, TLWalletJSONKeys.TLAddressType.Stealth, address);
                                } else if (itemData.first == CellType.CellTypeMainActiveAddress) {
                                    String address = accountObject.getMainActiveAddress(itemData.second);
                                    promptAddressActionSheet(getString(R.string.address_id_colon) + " " + itemData.second, TLWalletJSONKeys.TLAddressType.Main, address);
                                } else if (itemData.first == CellType.CellTypeMainArchivedAddress) {
                                    String address = accountObject.getMainArchivedAddress(itemData.second);
                                    promptAddressActionSheet(getString(R.string.address_id_colon) + " " + itemData.second, TLWalletJSONKeys.TLAddressType.Main, address);
                                } else if (itemData.first == CellType.CellTypeChangeActiveAddress) {
                                    String address = accountObject.getChangeActiveAddress(itemData.second);
                                    promptAddressActionSheet(getString(R.string.address_id_colon) + " " + itemData.second, TLWalletJSONKeys.TLAddressType.Change, address);
                                } else if (itemData.first == CellType.CellTypeChangeArchivedAddress) {
                                    String address = accountObject.getChangeArchivedAddress(itemData.second);
                                    promptAddressActionSheet(getString(R.string.address_id_colon) + " " + itemData.second, TLWalletJSONKeys.TLAddressType.Change, address);
                                }
                            }
                        });
                    }
                }
            }

            return v;
        }

    }

    private void promptAddressActionSheet(String title, TLWalletJSONKeys.TLAddressType addressType, String address) {
        CharSequence[] otherButtonTitles;
        if (appDelegate.preferences.enabledAdvancedMode()) {
            otherButtonTitles = new String[]{getString(R.string.view_in_web), getString(R.string.view_address_qr_code), getString(R.string.view_private_key_qr_code)};
        } else {
            otherButtonTitles = new String[]{getString(R.string.view_in_web), getString(R.string.view_address_qr_code)};
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == 0) {
                                openInWeb(appDelegate.blockExplorerAPI.getURLForWebViewAddress(address));
                                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_ACCOUNT_ADDRESS_IN_WEB));
                            } else if (which == 1) {
                                if (!appDelegate.suggestions.disabledSuggestDontManageIndividualAccountAddress()) {
                                    TLPrompts.promptForOK(getActivity(), getString(R.string.warning),
                                            getString(R.string.qr_code_address_warning), new TLPrompts.PromptOKCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    showAddressQRCode(address);
                                                    appDelegate.suggestions.setDisableSuggestDontManageIndividualAccountAddress(true);
                                                }

                                            });
                                } else {
                                    showAddressQRCode(address);
                                }
                            } else if (which == 2) {
                                if (!appDelegate.suggestions.disabledSuggestDontManageIndividualAccountPrivateKeys()) {
                                    TLPrompts.promptForOK(getActivity(), getString(R.string.warning),
                                            getString(R.string.qr_code_key_warning), new TLPrompts.PromptOKCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    showPrivateKeyQRCode(addressType, address);
                                                    appDelegate.suggestions.setDisableSuggestDontManageIndividualAccountPrivateKeys(true);
                                                }
                                            });
                                } else {
                                    showPrivateKeyQRCode(addressType, address);
                                }
                            }
                            dialog.dismiss();


                        }
                ).show();
    }

    private void showAddressQRCode(String address) {
        TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_ACCOUNT_ADDRESS));
    }

    private void showPrivateKeyQRCode(TLWalletJSONKeys.TLAddressType addressType, String address) {
        if (accountObject.isWatchOnly() && !accountObject.hasSetExtendedPrivateKeyInMemory() &&
                (addressType == TLWalletJSONKeys.TLAddressType.Main || addressType == TLWalletJSONKeys.TLAddressType.Change)) {

            TLPrompts.promptForOKCancel(getActivity(), getString(R.string.account_private_key_missing),
                    getString(R.string.ask_import_your_account_private_key), new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            CharSequence[] otherButtonTitles = {getString(R.string.import_with_qr_code), getString(R.string.import_with_text_input)};
                            new AlertDialog.Builder(getActivity())
                                    .setItems(otherButtonTitles, (dialog, which) -> {
                                                if (which == 0) {
                                                    selectedAddressType = addressType;
                                                    selectedAddress = address;
                                                    scanQRCode(SCAN_EXTENDED_PRIVATE_KEY);
                                                } else if (which == 1) {
                                                    TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.temporary_import_account_private_key), "", "", InputType.TYPE_CLASS_TEXT, new PromptCallback() {
                                                        @Override
                                                        public void onSuccess(Object obj) {
                                                            temporaryImportExtendedPrivateKey((String) obj, addressType, address);
                                                        }

                                                        @Override
                                                        public void onCancel() {
                                                        }
                                                    });

                                                }
                                                dialog.dismiss();
                                            }
                                    ).show();
                        }

                        @Override
                        public void onCancel() {

                        }
                    });
        } else if (accountObject.isColdWalletAccount()) {
            TLPrompts.promptErrorMessage(getActivity(), "", getString(R.string.cold_wallet_private_keys_not_store_here));
        } else {
            showPrivateKeyQRCodeFinal(address, addressType);
        }
    }

    private void temporaryImportExtendedPrivateKey(String extendedPrivateKey, TLWalletJSONKeys.TLAddressType addressType, String address) {
        if (!TLHDWalletWrapper.isValidExtendedPrivateKey(extendedPrivateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
            TLToast.makeText(getActivity(), getString(R.string.invalid_account_private_key), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
        } else {
            boolean success = accountObject.setExtendedPrivateKeyInMemory(extendedPrivateKey);
            if (!success) {
                TLToast.makeText(getActivity(), getString(R.string.account_private_key_not_match_imported_account_public_key), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
            } else {
                showPrivateKeyQRCodeFinal(address, addressType);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_EXTENDED_PRIVATE_KEY
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String value = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            temporaryImportExtendedPrivateKey(value, selectedAddressType, selectedAddress);
        }
    }

    private void startScanActivity(int requestCode) {
        if (!TLUtils.isCameraOpen()) {
            Intent intent = new Intent(getActivity(), CaptureActivity.class);
            startActivityForResult(intent, requestCode);
        } else {
            TLToast.makeText(getActivity(), getString(R.string.camera_unavailable), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
        }
    }

    private void scanQRCode(int requestCode) {
        if (ContextCompat.checkSelfPermission(appDelegate.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            TLPermissionUtil.requestCameraPermissionFromActivity(rootView, getActivity());
        }else{
            startScanActivity(requestCode);
        }
    }

    private void showPrivateKeyQRCodeFinal(String address, TLWalletJSONKeys.TLAddressType addressType) {
        String privateKey;
        if (addressType == TLWalletJSONKeys.TLAddressType.Stealth) {
            privateKey = accountObject.stealthWallet.getPaymentAddressPrivateKey(address);
        } else if (addressType == TLWalletJSONKeys.TLAddressType.Main) {
            privateKey = accountObject.getMainPrivateKey(address);
        } else {
            privateKey = accountObject.getChangePrivateKey(address);
        }
        TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_ACCOUNT_PRIVATE_KEY));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (appDelegate != null) {
            LocalBroadcastManager.getInstance(appDelegate.context).unregisterReceiver(receiver);
        }
    }
}
