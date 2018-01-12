package com.arcbit.arcbit.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.support.v7.widget.Toolbar;
import android.view.View.OnClickListener;

import com.arcbit.arcbit.model.TLAccountObject;
import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLImportedAddress;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.model.TLWalletUtils;
import com.arcbit.arcbit.ui.items.*;

import com.arcbit.arcbit.R;

public class SelectAccountFragment extends android.support.v4.app.Fragment {
    private final String TAG = getClass().getSimpleName();
    private ListView selectAccountListview;
    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    private int rowCount;
    private View rootView;
    private SelectAccountAdapter selectAccountAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private enum CellType {
        CellTypeNone, CellTypeColdWalletAccount, CellTypeRegularAccount, CellTypeImportedAccount, CellTypeImportedWatchAccount, CellTypeImportedAddress, CellTypeImportedWatchAddress, CellTypeAction;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED)) {
                refreshWalletAccounts(false);
            } else if (action.equals(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA)) {
                refreshWalletAccounts(false);
                if (selectAccountAdapter != null) {
                    selectAccountAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(TLNotificationEvents.EVENT_ADVANCE_MODE_TOGGLED)) {
                if (selectAccountAdapter != null) {
                    selectAccountAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION)) {
                if (selectAccountAdapter != null) {
                    selectAccountAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(TLNotificationEvents.EVENT_EXCHANGE_RATE_UPDATED)) {
                if (selectAccountAdapter != null) {
                    selectAccountAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_select_account, container, false);
        if (appDelegate == null) {
            return rootView;
        }
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getActivity().setTitle(getString(R.string.select_account));

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_ADVANCE_MODE_TOGGLED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_EXCHANGE_RATE_UPDATED));

        selectAccountListview = (ListView) rootView.findViewById(R.id.select_account_list_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.arcbit_main, R.color.arcbit_main, R.color.arcbit_main);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(false);
                appDelegate.setWalletDataNotFetched();
                updateListView();
                refreshWalletAccounts(true);
            }
        });

        this.refreshWalletAccounts(false);
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

    void updateListView() {
        if (appDelegate.accounts == null) {
            return;
        }
        rowCount = 0;

        if (appDelegate.accounts.getNumberOfAccounts() > 0) {
            rowCount += appDelegate.accounts.getNumberOfAccounts() + 1;
        }

        if (appDelegate.preferences.enabledColdWallet() && appDelegate.coldWalletAccounts.getNumberOfAccounts() > 0) {
            rowCount += appDelegate.coldWalletAccounts.getNumberOfAccounts() + 1;
        }

        if (appDelegate.preferences.enabledAdvancedMode()) {
            if (appDelegate.importedAccounts.getNumberOfAccounts() > 0) {
                rowCount += appDelegate.importedAccounts.getNumberOfAccounts() + 1;
            }
            if (appDelegate.importedWatchAccounts.getNumberOfAccounts() > 0) {
                rowCount += appDelegate.importedWatchAccounts.getNumberOfAccounts() + 1;
            }
            if (appDelegate.importedAddresses.getCount() > 0) {
                rowCount += appDelegate.importedAddresses.getCount() + 1;
            }
            if (appDelegate.importedWatchAddresses.getCount() > 0) {
                rowCount += appDelegate.importedWatchAddresses.getCount() + 1;
            }
        }
        selectAccountAdapter = new SelectAccountAdapter(rootView.getContext());
        selectAccountListview.setAdapter(selectAccountAdapter);
    }

    private void refreshWalletAccounts(boolean fetchDataAgain) {
        this.refreshAccountBalances(fetchDataAgain);
        if (appDelegate.preferences.enabledColdWallet()) {
            this.refreshColdWalletAccounts(fetchDataAgain);
        }
        if (appDelegate.preferences.enabledAdvancedMode()) {
            this.refreshImportedAccounts(fetchDataAgain);
            this.refreshImportedWatchAccounts(fetchDataAgain);
            this.refreshImportedAddressBalances(fetchDataAgain);
            this.refreshImportedWatchAddressBalances(fetchDataAgain);
        }
    }

    private void refreshAccountBalances(boolean fetchDataAgain) {
        if (appDelegate == null || appDelegate.accounts == null) {
            return;
        }
        for (int i = 0; i < appDelegate.accounts.getNumberOfAccounts(); i++) {
            TLAccountObject accountObject = appDelegate.accounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData() || fetchDataAgain) {
                accountObject.getAccountDataO(fetchDataAgain, null);
            }
        }
    }

    private void refreshColdWalletAccounts(boolean fetchDataAgain) {
        if (appDelegate == null || appDelegate.coldWalletAccounts == null) {
            return;
        }
        for (int i = 0; i < appDelegate.coldWalletAccounts.getNumberOfAccounts(); i++) {
            TLAccountObject accountObject = appDelegate.coldWalletAccounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData() || fetchDataAgain) {
                accountObject.getAccountDataO(fetchDataAgain, null);
            }
        }
    }

    private void refreshImportedAccounts(boolean fetchDataAgain) {
        if (appDelegate == null || appDelegate.importedAccounts == null) {
            return;
        }
        for (int i = 0; i < appDelegate.importedAccounts.getNumberOfAccounts(); i++) {
            TLAccountObject accountObject = appDelegate.importedAccounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData() || fetchDataAgain) {
                accountObject.getAccountDataO(fetchDataAgain, null);
            }
        }
    }

    private void refreshImportedWatchAccounts(boolean fetchDataAgain) {
        if (appDelegate == null || appDelegate.importedWatchAccounts == null) {
            return;
        }
        for (int i = 0; i < appDelegate.importedWatchAccounts.getNumberOfAccounts(); i++) {
            TLAccountObject accountObject = appDelegate.importedWatchAccounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData() || fetchDataAgain) {
                accountObject.getAccountDataO(fetchDataAgain, null);
            }
        }
    }

    private void refreshImportedAddressBalances(boolean fetchDataAgain) {
        if (appDelegate == null || appDelegate.importedAddresses == null) {
            return;
        }
        if (appDelegate.importedAddresses.getCount() > 0 &&
                (!appDelegate.importedAddresses.hasFetchedAddressesData() || fetchDataAgain)) {
            appDelegate.importedAddresses.checkToGetAndSetAddressesDataO(fetchDataAgain);
        }
    }

    private void refreshImportedWatchAddressBalances(boolean fetchDataAgain) {
        if (appDelegate.importedWatchAddresses.getCount() > 0 && (!appDelegate.importedWatchAddresses.hasFetchedAddressesData() || fetchDataAgain)) {
            appDelegate.importedWatchAddresses.checkToGetAndSetAddressesDataO(fetchDataAgain);
        }
    }

    private class SelectAccountAdapter extends ArrayAdapter<Item> {

        private Context context;
        private LayoutInflater vi;

        public SelectAccountAdapter(Context context) {
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
            int offset = 0;
            int maxRange = 0;
            if (appDelegate.accounts.getNumberOfAccounts() > 0) {
                maxRange += appDelegate.accounts.getNumberOfAccounts()+1;
                if (position < maxRange) {
                    if (position == offset) {
                        return new SectionItem(getString(R.string.accounts));
                    } else {
                        TLAccountObject accountObject = appDelegate.accounts.getAccountObjectForIdx(position - 1);
                        String balance = appDelegate.currencyFormat.getProperAmount(accountObject.getBalance());
                        return new EntryItem(balance, accountObject.getAccountName(), accountObject);
                    }
                }
                offset = maxRange;
            }
            if (appDelegate.preferences.enabledColdWallet() && appDelegate.coldWalletAccounts.getNumberOfAccounts() > 0) {
                maxRange += appDelegate.coldWalletAccounts.getNumberOfAccounts() + 1;
                if (position < maxRange) {
                    if (position == offset) {
                        return new SectionItem(getString(R.string.cold_wallet_accounts));
                    } else {
                        TLAccountObject accountObject = appDelegate.coldWalletAccounts.getAccountObjectForIdx(position - offset - 1);
                        String balance = appDelegate.currencyFormat.getProperAmount(accountObject.getBalance());
                        return new EntryItem(balance, accountObject.getAccountName(), accountObject);
                    }
                }
                offset = maxRange;
            }

            if (appDelegate.preferences.enabledAdvancedMode()) {

                if (appDelegate.importedAccounts.getNumberOfAccounts() > 0) {
                    maxRange += appDelegate.importedAccounts.getNumberOfAccounts() + 1;

                    if (position < maxRange) {
                        if (position == offset) {
                            return new SectionItem(getString(R.string.imported_accounts));
                        } else {
                            TLAccountObject accountObject = appDelegate.importedAccounts.getAccountObjectForIdx(position - offset - 1);
                            String balance = appDelegate.currencyFormat.getProperAmount(accountObject.getBalance());
                            return new EntryItem(balance, accountObject.getAccountName(), accountObject);
                        }
                    }
                    offset = maxRange;
                }
                if (appDelegate.importedWatchAccounts.getNumberOfAccounts() > 0) {
                    maxRange += appDelegate.importedWatchAccounts.getNumberOfAccounts() + 1;
                    if (position < maxRange) {
                        if (position == offset) {
                            return new SectionItem(getString(R.string.imported_watch_accounts));
                        } else {
                            TLAccountObject accountObject = appDelegate.importedWatchAccounts.getAccountObjectForIdx(position - offset - 1);
                            String balance = appDelegate.currencyFormat.getProperAmount(accountObject.getBalance());
                            return new EntryItem(balance, accountObject.getAccountName(), accountObject);
                        }
                    }
                    offset = maxRange;
                }
                if (appDelegate.importedAddresses.getCount() > 0) {
                    maxRange += appDelegate.importedAddresses.getCount() + 1;
                    if (position < maxRange) {
                        if (position == offset) {
                            return new SectionItem(getString(R.string.imported_addresses));
                        } else {
                            TLImportedAddress importedAddressObject = appDelegate.importedAddresses.getAddressObjectAtIdx(position - offset - 1);
                            String balance = appDelegate.currencyFormat.getProperAmount(importedAddressObject.getBalance());
                            return new EntryItem(balance, importedAddressObject.getLabel(), importedAddressObject);
                        }
                    }
                    offset = maxRange;
                }
                if (appDelegate.importedWatchAddresses.getCount() > 0) {
                    maxRange += appDelegate.importedWatchAddresses.getCount() + 1;
                    if (position < maxRange) {
                        if (position == offset) {
                            return new SectionItem(getString(R.string.imported_watch_addresses));
                        } else {
                            TLImportedAddress importedAddressObject = appDelegate.importedWatchAddresses.getAddressObjectAtIdx(position - offset - 1);
                            String balance = appDelegate.currencyFormat.getProperAmount(importedAddressObject.getBalance());
                            return new EntryItem(balance, importedAddressObject.getLabel(), importedAddressObject);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            final Item i = getItem(position);
            if (i != null) {
                if(i.isSection()) {
                    SectionItem si = (SectionItem)i;
                    v = vi.inflate(R.layout.list_item_section, null);

                    v.setOnClickListener(null);
                    v.setOnLongClickListener(null);
                    v.setLongClickable(false);

                    final TextView sectionView = (TextView) v.findViewById(R.id.list_item_section_text);
                    sectionView.setText(si.getTitle());
                } else {
                    EntryItem ei = (EntryItem)i;
                    v = vi.inflate(R.layout.list_item_entry, null);
                    final Button button = (Button)v.findViewById(R.id.list_item_entry_title);
                    final TextView subtitle = (TextView)v.findViewById(R.id.list_item_entry_summary);
                    final ProgressBar progressBar = (ProgressBar)v.findViewById(R.id.balanceProgressBar);

                    if (ei.accountObject != null && ei.accountObject instanceof TLAccountObject) {
                        if (((TLAccountObject)ei.accountObject).hasFetchedAccountData()) {
                            if (button != null) {
                                button.setVisibility(View.VISIBLE);
                                button.setText(ei.title);
                            }
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                        } else {
                            if (button != null) {
                                button.setVisibility(View.GONE);
                                button.setText(ei.title);
                            }
                            if (progressBar != null) {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        }
                    } else if (ei.accountObject != null && ei.accountObject instanceof TLImportedAddress) {
                        if (((TLImportedAddress)ei.accountObject).hasFetchedAccountData()) {
                            if (button != null) {
                                button.setVisibility(View.VISIBLE);
                                button.setText(ei.title);
                            }
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                        } else {
                            if (button != null) {
                                button.setVisibility(View.GONE);
                                button.setText(ei.title);
                            }
                            if (progressBar != null) {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        }
                    }


                    if(subtitle != null)
                        subtitle.setText(ei.subtitle);

                    v.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            int selectedAccountIdx = 0;
                            TLWalletUtils.TLSendFromType sendFromType = TLWalletUtils.TLSendFromType.HDWallet;
                            CellType section = getSection(position);
                            if (section == CellType.CellTypeRegularAccount) {
                                sendFromType = TLWalletUtils.TLSendFromType.HDWallet;
                                selectedAccountIdx = getIdx(position);
                            } else if (section == CellType.CellTypeColdWalletAccount) {
                                sendFromType = TLWalletUtils.TLSendFromType.ColdWalletAccount;
                                selectedAccountIdx = getIdx(position);
                            } else if (section == CellType.CellTypeImportedAccount) {
                                sendFromType = TLWalletUtils.TLSendFromType.ImportedAccount;
                                selectedAccountIdx = getIdx(position);
                            } else if (section == CellType.CellTypeImportedWatchAccount) {
                                sendFromType = TLWalletUtils.TLSendFromType.ImportedWatchAccount;
                                selectedAccountIdx = getIdx(position);
                            } else if (section == CellType.CellTypeImportedAddress) {
                                sendFromType = TLWalletUtils.TLSendFromType.ImportedAddress;
                                selectedAccountIdx = getIdx(position);
                            } else if (section == CellType.CellTypeImportedWatchAddress) {
                                sendFromType = TLWalletUtils.TLSendFromType.ImportedWatchAddress;
                                selectedAccountIdx = getIdx(position);
                            }

                            Intent intent = new Intent();
                            intent.putExtra("SELECT_ACCOUNT_TYPE", TLWalletUtils.TLSendFromType.getSendFromTypeIdx(sendFromType));
                            intent.putExtra("SELECT_ACCOUNT_IDX", selectedAccountIdx);
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                            getFragmentManager().popBackStack();
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
                }
            }

            return v;
        }

        CellType getSection(int position) {
            int offset = 0;
            if (appDelegate.accounts.getNumberOfAccounts() > 0) {
                offset++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts() + offset) {
                return CellType.CellTypeRegularAccount;
            }
            if (appDelegate.coldWalletAccounts.getNumberOfAccounts() > 0) {
                offset++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfAccounts() + offset) {
                return CellType.CellTypeColdWalletAccount;
            }
            if (appDelegate.importedAccounts.getNumberOfAccounts() > 0) {
                offset++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfAccounts()
                    + appDelegate.importedAccounts.getNumberOfAccounts() + offset) {
                return CellType.CellTypeImportedAccount;
            }
            if (appDelegate.importedWatchAccounts.getNumberOfAccounts() > 0) {
                offset++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfAccounts()
                    + appDelegate.importedAccounts.getNumberOfAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfAccounts() + offset) {
                return CellType.CellTypeImportedWatchAccount;
            }
            if (appDelegate.importedAddresses.getCount() > 0) {
                offset++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfAccounts()
                    + appDelegate.importedAccounts.getNumberOfAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfAccounts()
                    + appDelegate.importedAddresses.getCount() + offset) {
                return CellType.CellTypeImportedAddress;
            }
            if (appDelegate.importedWatchAddresses.getCount() > 0) {
                offset++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfAccounts()
                    + appDelegate.importedAccounts.getNumberOfAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfAccounts()
                    + appDelegate.importedAddresses.getCount()
                    + appDelegate.importedWatchAddresses.getCount() + offset) {
                return CellType.CellTypeImportedWatchAddress;
            }

            return CellType.CellTypeNone;
        }

        int getIdx(int position) {
            int sectionCount = 0;
            if (appDelegate.accounts.getNumberOfAccounts() > 0) {
                sectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts() + sectionCount) {
                return position-sectionCount;
            }
            if (appDelegate.coldWalletAccounts.getNumberOfAccounts() > 0) {
                sectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfAccounts() + sectionCount) {
                return position - (appDelegate.accounts.getNumberOfAccounts() + sectionCount);
            }
            if (appDelegate.importedAccounts.getNumberOfAccounts() > 0) {
                sectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfAccounts()
                    + appDelegate.importedAccounts.getNumberOfAccounts() + sectionCount) {
                return position - (appDelegate.accounts.getNumberOfAccounts() + appDelegate.coldWalletAccounts.getNumberOfAccounts() + sectionCount);
            }
            if (appDelegate.importedWatchAccounts.getNumberOfAccounts() > 0) {
                sectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfAccounts()
                    + appDelegate.importedAccounts.getNumberOfAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfAccounts() + sectionCount) {
                return position - (appDelegate.accounts.getNumberOfAccounts() + appDelegate.coldWalletAccounts.getNumberOfAccounts() + appDelegate.importedAccounts.getNumberOfAccounts() + sectionCount);
            }
            if (appDelegate.importedAddresses.getCount() > 0) {
                sectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfAccounts()
                    + appDelegate.importedAccounts.getNumberOfAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfAccounts()
                    + appDelegate.importedAddresses.getCount() + sectionCount) {
                return position - (appDelegate.accounts.getNumberOfAccounts() + appDelegate.coldWalletAccounts.getNumberOfAccounts() + appDelegate.importedAccounts.getNumberOfAccounts() + appDelegate.importedWatchAccounts.getNumberOfAccounts() + sectionCount);
            }
            if (appDelegate.importedWatchAddresses.getCount() > 0) {
                sectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfAccounts()
                    + appDelegate.importedAccounts.getNumberOfAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfAccounts()
                    + appDelegate.importedAddresses.getCount()
                    + appDelegate.importedWatchAddresses.getCount() + sectionCount) {
                return position - (appDelegate.accounts.getNumberOfAccounts() + appDelegate.coldWalletAccounts.getNumberOfAccounts() + appDelegate.importedAccounts.getNumberOfAccounts() + appDelegate.importedWatchAccounts.getNumberOfAccounts() + appDelegate.importedAddresses.getCount() + sectionCount);
            }
            return -1;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (appDelegate != null) {
            LocalBroadcastManager.getInstance(appDelegate.context).unregisterReceiver(receiver);
        }
    }
}
