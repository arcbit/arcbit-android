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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.View.OnClickListener;

import com.arcbit.arcbit.ui.utils.TLHUDWrapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.arcbit.arcbit.model.TLAccountObject;
import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLBitcoinjWrapper;
import com.arcbit.arcbit.model.TLCallback;
import com.arcbit.arcbit.model.TLStealthAddress;
import com.arcbit.arcbit.model.TLHDWalletWrapper;
import com.arcbit.arcbit.model.TLImportedAddress;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.model.TLStealthWallet;
import com.arcbit.arcbit.model.TLTxObject;
import com.arcbit.arcbit.model.TLWalletJSONKeys;
import com.arcbit.arcbit.model.TLWalletUtils;
import com.arcbit.arcbit.ui.utils.TLPrompts;
import com.arcbit.arcbit.ui.utils.TLToast;
import com.arcbit.arcbit.ui.items.*;

import java.util.Arrays;
import java.util.List;

import com.arcbit.arcbit.R;
import com.arcbit.arcbit.utils.TLPermissionUtil;
import com.arcbit.arcbit.utils.TLUtils;
import com.google.zxing.client.android.CaptureActivity;

public class AccountsFragment extends android.support.v4.app.Fragment {
    private static final String TAG = AccountsFragment.class.getName();

    public static final int SCAN_URI_COLD_WALLET_ACCOUNT = 2712;
    public static final int SCAN_URI_IMPORT_ACCOUNT = 2713;
    public static final int SCAN_URI_IMPORT_WATCH_ACCOUNT = 2714;
    public static final int SCAN_URI_PRIVATE_KEY = 2715;
    public static final int SCAN_URI_IMPORT_WATCH_ADDRESS = 2716;

    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    private View rootView;
    private ListView manageAccountListview;
    private ManageAccountAdapter manageAccountAdapter;
    private int rowCount;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private enum CellType {
        CellTypeNone, CellTypeRegularAccount, CellTypeColdWalletAccount, CellTypeImportedAccount, CellTypeImportedWatchAccount, CellTypeImportedAddress, CellTypeImportedWatchAddress, CellTypeAction;
    }
    private enum AccountAction {
        AccountActionNone, AccountActionCreateNewAccount, AccountActionColdWalletAccount, AccountActionImportAccount, AccountActionImportWatchAccount, AccountActionImportAddress, AccountActionImportWatchAddress;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED)) {
                if (manageAccountAdapter != null) {
                    manageAccountAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(TLNotificationEvents.EVENT_PREFERENCES_BITCOIN_DISPLAY_CHANGED)) {
                if (manageAccountAdapter != null) {
                    manageAccountAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(TLNotificationEvents.EVENT_PREFERENCES_FIAT_DISPLAY_CHANGED)) {
                if (manageAccountAdapter != null) {
                    manageAccountAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA)) {
                if (manageAccountAdapter != null) {
                    manageAccountAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(TLNotificationEvents.EVENT_ADVANCE_MODE_TOGGLED)) {
                if (manageAccountAdapter != null) {
                    updateListView();
                    manageAccountAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION)) {
                if (manageAccountAdapter != null) {
                    manageAccountAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(TLNotificationEvents.EVENT_EXCHANGE_RATE_UPDATED)) {
                if (manageAccountAdapter != null) {
                    manageAccountAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_manage_accounts, container, false);
        if (appDelegate == null) {
            return rootView;
        }

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getActivity().setTitle(R.string.accounts);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_ADVANCE_MODE_TOGGLED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_PREFERENCES_BITCOIN_DISPLAY_CHANGED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_PREFERENCES_FIAT_DISPLAY_CHANGED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_EXCHANGE_RATE_UPDATED));

        manageAccountListview = (ListView) rootView.findViewById(R.id.account_list_view);
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
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arcbit_menu_white));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerLayout drawer = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
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
        updateListView();
        manageAccountAdapter.notifyDataSetChanged();
        setupToolbar();
    }

    void showAddressListView(TLAccountObject accountObject, boolean showAddressListShowBalances) {
        appDelegate.viewAddressesAccountObject = accountObject; //probably find better way
        android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();


        android.support.v4.app.Fragment fragmentChild = new AddressListFragment();
        Bundle args = new Bundle();
        args.putBoolean("showAddressListShowBalances", showAddressListShowBalances);
        fragmentChild.setArguments(args);
        android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.right_slide_in, R.anim.left_slide_out, R.anim.left_slide_in, R.anim.right_slide_out);
        transaction.replace(R.id.fragment_container, fragmentChild);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    void updateListView() {
        rowCount = 0;
        if (appDelegate.accounts.getNumberOfAccounts() > 0) {
            rowCount += appDelegate.accounts.getNumberOfAccounts() + 1;
        }
        if (appDelegate.accounts.getNumberOfArchivedAccounts() > 0) {
            rowCount += appDelegate.accounts.getNumberOfArchivedAccounts() + 1;
        }
        if (appDelegate.preferences.enabledColdWallet()) {
            if (appDelegate.coldWalletAccounts.getNumberOfAccounts() > 0) {
                rowCount += appDelegate.coldWalletAccounts.getNumberOfAccounts() + 1;
            }
            if (appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() > 0) {
                rowCount += appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() + 1;
            }
            rowCount += 1;
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

            if (appDelegate.importedAccounts.getNumberOfArchivedAccounts() > 0) {
                rowCount += appDelegate.importedAccounts.getNumberOfArchivedAccounts() + 1;
            }
            if (appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() > 0) {
                rowCount += appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() + 1;
            }
            if (appDelegate.importedAddresses.getArchivedCount() > 0) {
                rowCount += appDelegate.importedAddresses.getArchivedCount() + 1;
            }
            if (appDelegate.importedWatchAddresses.getArchivedCount() > 0) {
                rowCount += appDelegate.importedWatchAddresses.getArchivedCount() + 1;
            }
            rowCount += 6;
        } else {
            rowCount += 2;
        }
        manageAccountAdapter = new ManageAccountAdapter(rootView.getContext());
        manageAccountListview.setAdapter(manageAccountAdapter);
    }

    private class ManageAccountAdapter extends ArrayAdapter<Item> {

        private Context context;
        private LayoutInflater vi;

        public ManageAccountAdapter(Context context) {
            super(context,0 );
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
                        String balance;
                        if (accountObject.getBalance() != null) {
                            balance = appDelegate.currencyFormat.getProperAmount(accountObject.getBalance());
                        } else {
                            balance = "";
                        }
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
                        String balance;
                        if (accountObject.getBalance() != null) {
                            balance = appDelegate.currencyFormat.getProperAmount(accountObject.getBalance());
                        } else {
                            balance = "";
                        }
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
                            String balance;
                            if (accountObject.getBalance() != null) {
                                balance = appDelegate.currencyFormat.getProperAmount(accountObject.getBalance());
                            } else {
                                balance = "";
                            }
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
                            String balance;
                            if (accountObject.getBalance() != null) {
                                balance = appDelegate.currencyFormat.getProperAmount(accountObject.getBalance());
                            } else {
                                balance = "";
                            }
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
                            String balance;
                            if (importedAddressObject.getBalance() != null) {
                                balance = appDelegate.currencyFormat.getProperAmount(importedAddressObject.getBalance());
                            } else {
                                balance = "";
                            }
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
                            String balance;
                            if (importedAddressObject.getBalance() != null) {
                                balance = appDelegate.currencyFormat.getProperAmount(importedAddressObject.getBalance());
                            } else {
                                balance = "";
                            }
                            return new EntryItem(balance, importedAddressObject.getLabel(), importedAddressObject);
                        }
                    }
                    offset = maxRange;
                }
            }

            if (appDelegate.accounts.getNumberOfArchivedAccounts() > 0) {
                maxRange += appDelegate.accounts.getNumberOfArchivedAccounts() + 1;

                if (position < maxRange) {
                    if (position == offset) {
                        return new SectionItem(getString(R.string.archived_accounts));
                    } else {
                        TLAccountObject accountObject = appDelegate.accounts.getArchivedAccountObjectForIdx(position - offset - 1);
                        return new LeftTitleItem(accountObject.getAccountName());
                    }
                }
                offset = maxRange;
            }
            if (appDelegate.preferences.enabledColdWallet() && appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() > 0) {
                maxRange += appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() + 1;
                if (position < maxRange) {
                    if (position == offset) {
                        return new SectionItem(getString(R.string.archived_cold_wallet_accounts));
                    } else {
                        TLAccountObject accountObject = appDelegate.coldWalletAccounts.getArchivedAccountObjectForIdx(position - offset - 1);
                        return new LeftTitleItem(accountObject.getAccountName());
                    }
                }
                offset = maxRange;
            }
            if (appDelegate.preferences.enabledAdvancedMode()) {
                if (appDelegate.importedAccounts.getNumberOfArchivedAccounts() > 0) {
                    maxRange += appDelegate.importedAccounts.getNumberOfArchivedAccounts() + 1;
                    if (position < maxRange) {
                        if (position == offset) {
                            return new SectionItem(getString(R.string.archived_imported_accounts));
                        } else {
                            TLAccountObject accountObject = appDelegate.importedAccounts.getArchivedAccountObjectForIdx(position - offset - 1);
                            return new LeftTitleItem(accountObject.getAccountName());
                        }
                    }
                    offset = maxRange;
                }
                if (appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() > 0) {
                    maxRange += appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() + 1;
                    if (position < maxRange) {
                        if (position == offset) {
                            return new SectionItem(getString(R.string.archived_imported_watch_accounts));
                        } else {
                            TLAccountObject accountObject = appDelegate.importedWatchAccounts.getArchivedAccountObjectForIdx(position - offset - 1);
                            return new LeftTitleItem(accountObject.getAccountName());
                        }
                    }
                    offset = maxRange;
                }
                if (appDelegate.importedAddresses.getArchivedCount() > 0) {
                    maxRange += appDelegate.importedAddresses.getArchivedCount() + 1;
                    if (position < maxRange) {
                        if (position == offset) {
                            return new SectionItem(getString(R.string.archived_imported_addresses));
                        } else {
                            TLImportedAddress importedAddressObject = appDelegate.importedAddresses.getArchivedAddressObjectAtIdx(position - offset - 1);
                            return new LeftTitleItem(importedAddressObject.getLabel());
                        }
                    }
                    offset = maxRange;
                }
                if (appDelegate.importedWatchAddresses.getArchivedCount() > 0) {
                    maxRange += appDelegate.importedWatchAddresses.getArchivedCount() + 1;
                    if (position < maxRange) {
                        if (position == offset) {
                            return new SectionItem(getString(R.string.archived_imported_watch_addresses));
                        } else {
                            TLImportedAddress importedAddressObject = appDelegate.importedWatchAddresses.getArchivedAddressObjectAtIdx(position - offset - 1);
                            return new LeftTitleItem(importedAddressObject.getLabel());
                        }
                    }
                    offset = maxRange;
                }
            }
            if (position == offset) {
                return new SectionItem(getString(R.string.actions));
            } else if (position == offset + 1) {
                return new ActionItem(getString(R.string.create_new_account));
            }
            if (appDelegate.preferences.enabledColdWallet()) {
                if (position == offset + 2) {
                    return new ActionItem(getString(R.string.import_cold_wallet_account));
                }
                offset += 1;
            }
            if (appDelegate.preferences.enabledAdvancedMode()) {
                if (position == offset + 2) {
                    return new ActionItem(getString(R.string.import_account));
                } else if (position == offset + 3) {
                    return new ActionItem(getString(R.string.import_watch_account));
                } else if (position == offset + 4) {
                    return new ActionItem(getString(R.string.import_private_key));
                } else if (position == offset + 5) {
                    return new ActionItem(getString(R.string.import_watch_address));
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

                        if (button != null)
                            button.setText(ei.title);
                        if(subtitle != null)
                            subtitle.setText(ei.subtitle);

                        v.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CellType section = getSection(position);

                                if (section == CellType.CellTypeRegularAccount) {
                                    promptAccountsActionSheet(getIdx(position));
                                } else if (section == CellType.CellTypeColdWalletAccount) {
                                    promptColdWalletAccountsActionSheet(getIdx(position));
                                } else if (section == CellType.CellTypeImportedAccount) {
                                    promptImportedAccountsActionSheet(getIdx(position));
                                } else if (section == CellType.CellTypeImportedWatchAccount) {
                                    promptImportedWatchAccountsActionSheet(getIdx(position));
                                } else if (section == CellType.CellTypeImportedAddress) {
                                    promptImportedAddressActionSheet(getIdx(position));
                                } else if (section == CellType.CellTypeImportedWatchAddress) {
                                    promptImportedWatchAddressActionSheet(getIdx(position));
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
                    } else if (i instanceof ActionItem) {
                        ActionItem ei = (ActionItem) i;
                        v = vi.inflate(R.layout.list_item_action, null);
                        final TextView title = (TextView) v.findViewById(R.id.action_item_action_name);
                        if (title != null) {
                            title.setText(ei.title);
                        }
                        v.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (getAccountAction(position) == AccountAction.AccountActionNone) {
                                    return;
                                }
                                if (getAccountAction(position) == AccountAction.AccountActionCreateNewAccount) {
                                    promptCreateNewAccount();
                                } else if (getAccountAction(position) == AccountAction.AccountActionColdWalletAccount) {
                                    promptColdWalletActionSheet();
                                } else if (getAccountAction(position) == AccountAction.AccountActionImportAccount) {
                                    promptImportAccountActionSheet();
                                } else if (getAccountAction(position) == AccountAction.AccountActionImportWatchAccount) {
                                    promptImportWatchAccountActionSheet();
                                } else if (getAccountAction(position) == AccountAction.AccountActionImportAddress) {
                                    promptImportPrivateKeyActionSheet();
                                } else if (getAccountAction(position) == AccountAction.AccountActionImportWatchAddress) {
                                    promptImportWatchAddressActionSheet();
                                }
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
                                int offset = position-getArchivedOffset();
                                CellType section = getArchivedSection(offset);
                                if (section == CellType.CellTypeRegularAccount) {
                                    promptArchivedAccountsActionSheet(getArchivedIdx(offset));
                                } else if (section == CellType.CellTypeColdWalletAccount) {
                                    promptArchivedImportedAccountsActionSheet(getArchivedIdx(offset), TLWalletUtils.TLAccountType.ColdWallet);
                                } else if (section == CellType.CellTypeImportedAccount) {
                                    promptArchivedImportedAccountsActionSheet(getArchivedIdx(offset), TLWalletUtils.TLAccountType.Imported);
                                } else if (section == CellType.CellTypeImportedWatchAccount) {
                                    promptArchivedImportedAccountsActionSheet(getArchivedIdx(offset), TLWalletUtils.TLAccountType.ImportedWatch);
                                } else if (section == CellType.CellTypeImportedAddress) {
                                    promptArchivedImportedAddressActionSheet(getArchivedIdx(offset));
                                } else if (section == CellType.CellTypeImportedWatchAddress) {
                                    promptArchivedImportedWatchAddressActionSheet(getArchivedIdx(offset));
                                }
                            }
                        });
                    }
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

        CellType getArchivedSection(int position) {
            int archivedSectionCount = 0;
            if (appDelegate.accounts.getNumberOfArchivedAccounts() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts() + archivedSectionCount) {
                return CellType.CellTypeRegularAccount;
            }
            if (appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() + archivedSectionCount) {
                return CellType.CellTypeColdWalletAccount;
            }
            if (appDelegate.importedAccounts.getNumberOfArchivedAccounts() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAccounts.getNumberOfArchivedAccounts() + archivedSectionCount) {
                return CellType.CellTypeImportedAccount;
            }
            if (appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() + archivedSectionCount) {
                return CellType.CellTypeImportedWatchAccount;
            }
            if (appDelegate.importedAddresses.getArchivedCount() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAddresses.getArchivedCount() + archivedSectionCount) {
                return CellType.CellTypeImportedAddress;
            }
            if (appDelegate.importedWatchAddresses.getArchivedCount() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAddresses.getArchivedCount()
                    + appDelegate.importedWatchAddresses.getArchivedCount() + archivedSectionCount) {
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

        int getArchivedIdx(int position) {
            int archivedSectionCount = 0;
            if (appDelegate.accounts.getNumberOfArchivedAccounts() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts() + archivedSectionCount) {
                return position-archivedSectionCount;
            }
            if (appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() + archivedSectionCount) {
                return position - (appDelegate.accounts.getNumberOfArchivedAccounts() + archivedSectionCount);
            }
            if (appDelegate.importedAccounts.getNumberOfArchivedAccounts() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAccounts.getNumberOfArchivedAccounts() + archivedSectionCount) {
                return position - (appDelegate.accounts.getNumberOfArchivedAccounts() + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() + archivedSectionCount);
            }
            if (appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() + archivedSectionCount) {
                return position - (appDelegate.accounts.getNumberOfArchivedAccounts() + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() + appDelegate.importedAccounts.getNumberOfArchivedAccounts() + archivedSectionCount);
            }
            if (appDelegate.importedAddresses.getArchivedCount() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAddresses.getArchivedCount() + archivedSectionCount) {
                return position - (appDelegate.accounts.getNumberOfArchivedAccounts() + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() + appDelegate.importedAccounts.getNumberOfArchivedAccounts() + appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() + archivedSectionCount);
            }
            if (appDelegate.importedWatchAddresses.getArchivedCount() > 0) {
                archivedSectionCount++;
            }
            if (position < appDelegate.accounts.getNumberOfArchivedAccounts()
                    + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts()
                    + appDelegate.importedAddresses.getArchivedCount()
                    + appDelegate.importedWatchAddresses.getArchivedCount() + archivedSectionCount) {
                return position - (appDelegate.accounts.getNumberOfArchivedAccounts() + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() + appDelegate.importedAccounts.getNumberOfArchivedAccounts() + appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() + appDelegate.importedAddresses.getArchivedCount() + archivedSectionCount);
            }
            return -1;
        }

        AccountAction getAccountAction(int position) {
            int sum = appDelegate.accounts.getNumberOfAccounts() + 1;
            if (appDelegate.accounts.getNumberOfArchivedAccounts() > 0) {
                sum += appDelegate.accounts.getNumberOfArchivedAccounts() + 1;
            }
            if (appDelegate.preferences.enabledColdWallet() && appDelegate.coldWalletAccounts.getNumberOfAccounts() > 0) {
                sum += appDelegate.coldWalletAccounts.getNumberOfAccounts() + 1;
            }
            if (appDelegate.preferences.enabledAdvancedMode()) {
                if (appDelegate.importedAccounts.getNumberOfAccounts() > 0) {
                    sum += appDelegate.importedAccounts.getNumberOfAccounts() + 1;
                }
                if (appDelegate.importedWatchAccounts.getNumberOfAccounts() > 0) {
                    sum += appDelegate.importedWatchAccounts.getNumberOfAccounts() + 1;
                }
                if (appDelegate.importedAddresses.getCount() > 0) {
                    sum += appDelegate.importedAddresses.getCount() + 1;
                }
                if (appDelegate.importedWatchAddresses.getCount() > 0) {
                    sum += appDelegate.importedWatchAddresses.getCount() + 1;
                }

                if (appDelegate.importedAccounts.getNumberOfArchivedAccounts() > 0) {
                    sum += appDelegate.importedAccounts.getNumberOfArchivedAccounts() + 1;
                }
                if (appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() > 0) {
                    sum += appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() + 1;
                }
                if (appDelegate.importedAddresses.getArchivedCount() > 0) {
                    sum += appDelegate.importedAddresses.getArchivedCount() + 1;
                }
                if (appDelegate.importedWatchAddresses.getArchivedCount() > 0) {
                    sum += appDelegate.importedWatchAddresses.getArchivedCount() + 1;
                }
            }
            if (appDelegate.preferences.enabledColdWallet() && appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() > 0) {
                sum += appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() + 1;
            }
            if (position > sum) {
                if (position - sum == 1) {
                    return AccountAction.AccountActionCreateNewAccount;
                }
                if (appDelegate.preferences.enabledColdWallet()) {
                    if (position - sum == 2) {
                        return AccountAction.AccountActionColdWalletAccount;
                    }
                    if (position - sum == 3) {
                        return AccountAction.AccountActionImportAccount;
                    }
                    if (position - sum == 4) {
                        return AccountAction.AccountActionImportWatchAccount;
                    }
                    if (position - sum == 5) {
                        return AccountAction.AccountActionImportAddress;
                    }
                    if (position - sum == 6) {
                        return AccountAction.AccountActionImportWatchAddress;
                    }
                } else {
                    if (position - sum == 2) {
                        return AccountAction.AccountActionImportAccount;
                    }
                    if (position - sum == 3) {
                        return AccountAction.AccountActionImportWatchAccount;
                    }
                    if (position - sum == 4) {
                        return AccountAction.AccountActionImportAddress;
                    }
                    if (position - sum == 5) {
                        return AccountAction.AccountActionImportWatchAddress;
                    }
                }
            }
            return AccountAction.AccountActionNone;
        }

        public int getArchivedOffset() {
            int offset = 0;
            if (appDelegate.accounts.getNumberOfAccounts() > 0) {
                offset += appDelegate.accounts.getNumberOfAccounts() + 1;
            }
            if (appDelegate.preferences.enabledColdWallet() && appDelegate.coldWalletAccounts.getNumberOfAccounts() > 0) {
                offset += appDelegate.coldWalletAccounts.getNumberOfAccounts() + 1;
            }
            if (!appDelegate.preferences.enabledAdvancedMode()) {
                return offset;
            }
            if (appDelegate.importedAccounts.getNumberOfAccounts() > 0) {
                offset += appDelegate.importedAccounts.getNumberOfAccounts() + 1;
            }
            if (appDelegate.importedWatchAccounts.getNumberOfAccounts() > 0) {
                offset += appDelegate.importedWatchAccounts.getNumberOfAccounts() + 1;
            }
            if (appDelegate.importedAddresses.getCount() > 0) {
                offset += appDelegate.importedAddresses.getCount() + 1;
            }
            if (appDelegate.importedWatchAddresses.getCount() > 0) {
                offset += appDelegate.importedWatchAddresses.getCount() + 1;
            }
            return offset;
        }
    }

    private void promptCreateNewAccount() {
        TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.create_new_account), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
            @Override
            public void onSuccess(Object obj) {
                String accountName = (String) obj;
                TLAccountObject accountObject = appDelegate.accounts.createNewAccount(accountName, TLWalletJSONKeys.TLAccount.Normal);
                if (accountName.length() == 0) {
                    appDelegate.accounts.renameAccount(accountObject.getAccountIdxNumber(), getString(R.string.account_number, (accountObject.getAccountIdxNumber()+1)));
                }
                //if account has been used then balance will be wrong, np because this only can happen in testing when starting a new wallet with used passphraser
                accountObject.setFetchedAccountData(true);
                updateListView();
                manageAccountAdapter.notifyDataSetChanged();
                TLToast.makeText(getActivity(), getString(R.string.created_new_account), TLToast.LENGTH_SHORT, TLToast.TYPE_INFO);

            }

            @Override
            public void onCancel() {

            }
        });
    }

    private void refreshColdWalletAccounts(boolean fetchDataAgain) {
        for (int i = 0; i < appDelegate.coldWalletAccounts.getNumberOfAccounts(); i++) {
            TLAccountObject accountObject = appDelegate.coldWalletAccounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData() || fetchDataAgain) {
                accountObject.getAccountDataO(fetchDataAgain, null);
            }
        }
    }

    private void refreshImportedAccounts(boolean fetchDataAgain) {
        for (int i = 0; i < appDelegate.importedAccounts.getNumberOfAccounts(); i++) {
            TLAccountObject accountObject = appDelegate.importedAccounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData() || fetchDataAgain) {
                accountObject.getAccountDataO(fetchDataAgain, null);
            }
        }
    }

    private void refreshImportedWatchAccounts(boolean fetchDataAgain) {
        for (int i = 0; i < appDelegate.importedWatchAccounts.getNumberOfAccounts(); i++) {
            TLAccountObject accountObject = appDelegate.importedWatchAccounts.getAccountObjectForIdx(i);
            if (!accountObject.hasFetchedAccountData() || fetchDataAgain) {
                accountObject.getAccountDataO(fetchDataAgain, null);
            }
        }
    }

    private void refreshImportedAddressBalances(boolean fetchDataAgain) {
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

    private void promptAccountsActionSheet(int idx) {
        CharSequence[] otherButtonTitles;
        final int VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX;
        final int VIEW_EXTENDED_PRIVATE_KEY_BUTTON_IDX;
        final int VIEW_ADDRESSES_BUTTON_IDX;
        final int MANUALLY_SCAN_TX_FOR_STEALTH_TRANSACTION_BUTTON_IDX;
        final int RENAME_ACCOUNT_BUTTON_IDX;
        final int ARCHIVE_ACCOUNT_BUTTON_IDX;
        if (appDelegate.preferences.enabledAdvancedMode()) {
            otherButtonTitles = new String[]{getString(R.string.view_account_public_key_qr_code), getString(R.string.view_account_private_key_qr_code), getString(R.string.view_addresses), getString(R.string.scan_for_reusable_address_payment), getString(R.string.edit_account_name), getString(R.string.archive_account)};
            VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX = 0;
            VIEW_EXTENDED_PRIVATE_KEY_BUTTON_IDX = 1;
            VIEW_ADDRESSES_BUTTON_IDX = 2;
            MANUALLY_SCAN_TX_FOR_STEALTH_TRANSACTION_BUTTON_IDX = 3;
            RENAME_ACCOUNT_BUTTON_IDX = 4;
            ARCHIVE_ACCOUNT_BUTTON_IDX = 5;
        } else {
            otherButtonTitles = new String[]{getString(R.string.view_addresses), getString(R.string.edit_account_name), getString(R.string.archive_account)};
            VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX = -1;
            VIEW_EXTENDED_PRIVATE_KEY_BUTTON_IDX = -1;
            MANUALLY_SCAN_TX_FOR_STEALTH_TRANSACTION_BUTTON_IDX = -1;
            VIEW_ADDRESSES_BUTTON_IDX = 0;
            RENAME_ACCOUNT_BUTTON_IDX = 1;
            ARCHIVE_ACCOUNT_BUTTON_IDX = 2;
        }

        TLAccountObject accountObject = appDelegate.accounts.getAccountObjectForIdx(idx);
        int accountHDIndex = accountObject.getAccountHDIndex();
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.account_id_number, accountHDIndex))
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountObject.getExtendedPubKey());
                                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_EXTENDED_PUBLIC_KEY));
                            } else if (which == VIEW_EXTENDED_PRIVATE_KEY_BUTTON_IDX) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountObject.getExtendedPrivKey());
                                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_EXTENDED_PRIVATE_KEY));
                            } else if (which == VIEW_ADDRESSES_BUTTON_IDX) {
                                showAddressListView(accountObject, true);
                            } else if (which == MANUALLY_SCAN_TX_FOR_STEALTH_TRANSACTION_BUTTON_IDX) {
                                promptInfoAndToManuallyScanForStealthTransactionAccount(accountObject);
                            } else if (which == RENAME_ACCOUNT_BUTTON_IDX) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.rename_account), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        appDelegate.accounts.renameAccount(accountObject.getAccountIdxNumber(), (String)obj);
                                        manageAccountAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancel() {

                                    }
                                });
                            } else if (which == ARCHIVE_ACCOUNT_BUTTON_IDX) {
                                promptToArchiveAccountHDWalletAccount(accountObject);
                            }

                            dialog.dismiss();


                        }
                ).show();

    }

    private void promptColdWalletAccountsActionSheet(int idx) {
        TLAccountObject accountObject = appDelegate.coldWalletAccounts.getAccountObjectForIdx(idx);

        CharSequence[] otherButtonTitles;
        final int VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX;
        final int VIEW_ADDRESSES_BUTTON_IDX;
        final int RENAME_ACCOUNT_BUTTON_IDX;
        final int ARCHIVE_ACCOUNT_BUTTON_IDX;
        if (appDelegate.preferences.enabledAdvancedMode()) {
            otherButtonTitles = new String[]{getString(R.string.view_account_public_key_qr_code), getString(R.string.view_addresses), getString(R.string.edit_account_name), getString(R.string.archive_account)};
            VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX = 0;
            VIEW_ADDRESSES_BUTTON_IDX = 1;
            RENAME_ACCOUNT_BUTTON_IDX = 2;
            ARCHIVE_ACCOUNT_BUTTON_IDX = 3;
        } else {
            otherButtonTitles = new String[]{getString(R.string.view_addresses), getString(R.string.edit_account_name), getString(R.string.archive_account)};
            VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX = -1;
            VIEW_ADDRESSES_BUTTON_IDX = 0;
            RENAME_ACCOUNT_BUTTON_IDX = 1;
            ARCHIVE_ACCOUNT_BUTTON_IDX = 2;
        }

        int accountHDIndex = accountObject.getAccountHDIndex();
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.account_id_number, accountHDIndex))
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountObject.getExtendedPubKey());
                                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_EXTENDED_PUBLIC_KEY));
                            } else if (which == VIEW_ADDRESSES_BUTTON_IDX) {
                                showAddressListView(accountObject, true);
                            } else if (which == RENAME_ACCOUNT_BUTTON_IDX) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.rename_account), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        appDelegate.coldWalletAccounts.renameAccount(accountObject.getAccountIdxNumber(), (String) obj);
                                        manageAccountAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancel() {

                                    }
                                });
                            } else if (which == ARCHIVE_ACCOUNT_BUTTON_IDX) {
                                this.promptToArchiveAccount(accountObject);
                            }
                        }
                ).show();
    }

    private void promptImportedAccountsActionSheet(int idx) {
        TLAccountObject accountObject = appDelegate.importedAccounts.getAccountObjectForIdx(idx);
        CharSequence[] otherButtonTitles = {getString(R.string.view_account_public_key_qr_code), getString(R.string.view_account_private_key_qr_code), getString(R.string.view_addresses), getString(R.string.scan_for_reusable_address_payment), getString(R.string.edit_account_name), getString(R.string.archive_account)};
        int accountHDIndex = accountObject.getAccountHDIndex();
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.account_id_number, accountHDIndex))
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == 0) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountObject.getExtendedPubKey());
                                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_EXTENDED_PUBLIC_KEY));
                            } else if (which == 1) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountObject.getExtendedPrivKey());
                                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_EXTENDED_PRIVATE_KEY));
                            } else if (which == 2) {
                                showAddressListView(accountObject, true);
                            } else if (which == 3) {
                                promptInfoAndToManuallyScanForStealthTransactionAccount(accountObject);
                            } else if (which == 4) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.rename_account), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        appDelegate.importedAccounts.renameAccount(accountObject.getAccountIdxNumber(), (String)obj);
                                        manageAccountAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancel() {
                                    }
                                });
                            } else if (which == 5) {
                                promptToArchiveAccount(accountObject);
                            }

                            dialog.dismiss();
                        }
                ).show();
    }

    private void promptImportedWatchAccountsActionSheet(int idx) {
        TLAccountObject accountObject = appDelegate.importedWatchAccounts.getAccountObjectForIdx(idx);
        final boolean addClearPrivateKeyButton;
        CharSequence[] otherButtons;
        if (accountObject.hasSetExtendedPrivateKeyInMemory()) {
            addClearPrivateKeyButton = true;
            otherButtons = new String[]{getString(R.string.clear_account_private_key_from_memory), getString(R.string.view_account_public_key_qr_code), getString(R.string.view_addresses),  getString(R.string.edit_account_name), getString(R.string.archive_account)};
        } else {
            addClearPrivateKeyButton = false;
            otherButtons = new String[]{getString(R.string.view_account_public_key_qr_code), getString(R.string.view_addresses), getString(R.string.edit_account_name), getString(R.string.archive_account)};
        }

        int accountHDIndex = accountObject.getAccountHDIndex();
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.account_id_number, accountHDIndex))
                .setItems(otherButtons, (dialog, which) -> {
                            int CLEAR_ACCOUNT_PRIVATE_KEY_BUTTON_IDX = -1;
                            int VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX = 0;
                            int VIEW_ADDRESSES_BUTTON_IDX = 1;
                            int RENAME_ACCOUNT_BUTTON_IDX = 2;
                            int ARCHIVE_ACCOUNT_BUTTON_IDX = 3;

                            if (accountObject.hasSetExtendedPrivateKeyInMemory()) {
                                CLEAR_ACCOUNT_PRIVATE_KEY_BUTTON_IDX = 0;
                                VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX = 1;
                                VIEW_ADDRESSES_BUTTON_IDX = 2;
                                RENAME_ACCOUNT_BUTTON_IDX = 3;
                                ARCHIVE_ACCOUNT_BUTTON_IDX = 4;
                            }

                            if (addClearPrivateKeyButton && which == CLEAR_ACCOUNT_PRIVATE_KEY_BUTTON_IDX) {
                                assert (accountObject.hasSetExtendedPrivateKeyInMemory());
                                accountObject.clearExtendedPrivateKeyFromMemory();
                                TLToast.makeText(getActivity(), getString(R.string.cleared_from_memory), TLToast.LENGTH_SHORT, TLToast.TYPE_INFO);
                            } else if (which == VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountObject.getExtendedPubKey());
                                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_EXTENDED_PUBLIC_KEY));
                            } else if (which == VIEW_ADDRESSES_BUTTON_IDX) {
                                showAddressListView(accountObject, true);
                            } else if (which == RENAME_ACCOUNT_BUTTON_IDX) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.rename_account), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        appDelegate.importedWatchAccounts.renameAccount(accountObject.getAccountIdxNumber(), (String) obj);
                                        manageAccountAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancel() {

                                    }
                                });
                            } else if (which == ARCHIVE_ACCOUNT_BUTTON_IDX) {
                                this.promptToArchiveAccount(accountObject);
                            }
                        }
                    ).show();
    }

    private void promptImportedAddressActionSheet(int importedAddressIdx) {
        TLImportedAddress importAddressObject = appDelegate.importedAddresses.getAddressObjectAtIdx(importedAddressIdx);
        CharSequence[] otherButtons = {getString(R.string.view_address_qr_code), getString(R.string.view_private_key_qr_code), getString(R.string.view_address_in_web), getString(R.string.edit_label), getString(R.string.archive_address)};
        new AlertDialog.Builder(getActivity())
                .setItems(otherButtons, (dialog, which) -> {
                            if (which == 0) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), importAddressObject.getAddress());
                            } else if (which == 1) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), importAddressObject.getEitherPrivateKeyOrEncryptedPrivateKey());
                            } else if (which == 2) {
                                String url = appDelegate.blockExplorerAPI.getURLForWebViewAddress(importAddressObject.getAddress());
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);
                            } else if (which == 3) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.enter_label), "", "", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        appDelegate.importedAddresses.setLabel((String) obj, importAddressObject.getPositionInWalletArrayNumber());
                                        manageAccountAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancel() {

                                    }
                                });
                            } else if (which == 4) {
                                this.promptToArchiveAddress(importAddressObject);

                            }
                        }
                ).show();
    }

    private void promptArchivedImportedAddressActionSheet(int importedAddressIdx) {
        TLImportedAddress importAddressObject = appDelegate.importedAddresses.getArchivedAddressObjectAtIdx(importedAddressIdx);
        CharSequence[] otherButtons = {getString(R.string.view_address_qr_code), getString(R.string.view_private_key_qr_code), getString(R.string.view_address_in_web), getString(R.string.edit_label), getString(R.string.unarchived_address), getString(R.string.delete_address)};
        new AlertDialog.Builder(getActivity())
                .setItems(otherButtons, (dialog, which) -> {
                            if (which == 0) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), importAddressObject.getAddress());
                            } else if (which == 1) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), importAddressObject.getEitherPrivateKeyOrEncryptedPrivateKey());
                            } else if (which == 2) {
                                String url = appDelegate.blockExplorerAPI.getURLForWebViewAddress(importAddressObject.getAddress());
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);
                            } else if (which == 3) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.enter_label), "", "", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        appDelegate.importedAddresses.setLabel((String) obj, importAddressObject.getPositionInWalletArrayNumber());
                                        manageAccountAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancel() {

                                    }
                                });
                            } else if (which == 4) {
                                this.promptToUnarchiveAddress(importAddressObject);
                            } else if (which == 5) {
                                this.promptToDeleteImportedAddress(importedAddressIdx);
                            }
                        }
                ).show();
    }

    private void promptImportedWatchAddressActionSheet(int importedAddressIdx) {
        TLImportedAddress importAddressObject = appDelegate.importedWatchAddresses.getAddressObjectAtIdx(importedAddressIdx);
        final boolean addClearPrivateKeyButton;

        CharSequence[] otherButtonTitles;
        if (importAddressObject.hasSetPrivateKeyInMemory()) {
            addClearPrivateKeyButton = true;
            otherButtonTitles = new String[]{getString(R.string.clear_private_key_from_memory), getString(R.string.view_address_qr_code), getString(R.string.view_address_in_web), getString(R.string.edit_label), getString(R.string.archive_address)};
        } else {
            addClearPrivateKeyButton = false;
            otherButtonTitles = new String[]{getString(R.string.view_address_qr_code), getString(R.string.view_address_in_web), getString(R.string.edit_label), getString(R.string.archive_address)};
        }

        new AlertDialog.Builder(getActivity())
                .setItems(otherButtonTitles, (dialog, which) -> {
                            int CLEAR_PRIVATE_KEY_BUTTON_IDX = -1;
                            int VIEW_ADDRESS_BUTTON_IDX = 0;
                            int VIEW_ADDRESS_IN_WEB_BUTTON_IDX = 1;
                            int RENAME_ADDRESS_BUTTON_IDX = 2;
                            int ARCHIVE_ADDRESS_BUTTON_IDX = 3;
                            if (importAddressObject.hasSetPrivateKeyInMemory()) {
                                CLEAR_PRIVATE_KEY_BUTTON_IDX = 0;
                                VIEW_ADDRESS_BUTTON_IDX = 1;
                                VIEW_ADDRESS_IN_WEB_BUTTON_IDX = 2;
                                RENAME_ADDRESS_BUTTON_IDX = 3;
                                ARCHIVE_ADDRESS_BUTTON_IDX = 4;
                            }

                            if (addClearPrivateKeyButton && which == CLEAR_PRIVATE_KEY_BUTTON_IDX) {
                                assert(importAddressObject.hasSetPrivateKeyInMemory());
                                importAddressObject.clearPrivateKeyFromMemory();
                                TLToast.makeText(getActivity(), getString(R.string.cleared_from_memory), TLToast.LENGTH_SHORT, TLToast.TYPE_INFO);
                            }
                            if (which == VIEW_ADDRESS_BUTTON_IDX) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), importAddressObject.getAddress());
                            } else if (which == VIEW_ADDRESS_IN_WEB_BUTTON_IDX) {
                                String url = appDelegate.blockExplorerAPI.getURLForWebViewAddress(importAddressObject.getAddress());
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);
                            } else if (which == RENAME_ADDRESS_BUTTON_IDX) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.enter_label), "", "", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        appDelegate.importedWatchAddresses.setLabel((String) obj, importAddressObject.getPositionInWalletArrayNumber());
                                        manageAccountAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancel() {

                                    }
                                });
                            } else if (which == ARCHIVE_ADDRESS_BUTTON_IDX) {
                                this.promptToArchiveAddress(importAddressObject);
                            }
                        }
                ).show();
        }

    private void promptArchivedImportedWatchAddressActionSheet(int importedAddressIdx) {
        TLImportedAddress importAddressObject = appDelegate.importedWatchAddresses.getArchivedAddressObjectAtIdx(importedAddressIdx);
        final boolean addClearPrivateKeyButton;
        CharSequence[] otherButtonTitles;
        if (importAddressObject.hasSetPrivateKeyInMemory()) {
            addClearPrivateKeyButton = true;
            otherButtonTitles = new String[]{getString(R.string.clear_private_key_from_memory), getString(R.string.view_address_qr_code), getString(R.string.view_address_in_web), getString(R.string.edit_label), getString(R.string.unarchived_address), getString(R.string.delete_address)};
        } else {
            addClearPrivateKeyButton = false;
            otherButtonTitles = new String[]{getString(R.string.view_address_qr_code), getString(R.string.view_address_in_web), getString(R.string.edit_label), getString(R.string.unarchived_address), getString(R.string.delete_address)};
        }
        new AlertDialog.Builder(getActivity())
                .setItems(otherButtonTitles, (dialog, which) -> {
                            int CLEAR_PRIVATE_KEY_BUTTON_IDX = -1;
                            int VIEW_ADDRESS_BUTTON_IDX = 0;
                            int VIEW_ADDRESS_IN_WEB_BUTTON_IDX = 1;
                            int RENAME_ADDRESS_BUTTON_IDX = 2;
                            int UNARCHIVE_ADDRESS_BUTTON_IDX = 3;
                            int DELETE_ADDRESS_BUTTON_IDX = 4;
                            if (importAddressObject.hasSetPrivateKeyInMemory()) {
                                CLEAR_PRIVATE_KEY_BUTTON_IDX = 0;
                                VIEW_ADDRESS_BUTTON_IDX = 1;
                                VIEW_ADDRESS_IN_WEB_BUTTON_IDX = 2;
                                RENAME_ADDRESS_BUTTON_IDX = 3;
                                UNARCHIVE_ADDRESS_BUTTON_IDX = 4;
                                DELETE_ADDRESS_BUTTON_IDX = 5;
                            }
                            if (addClearPrivateKeyButton && which == CLEAR_PRIVATE_KEY_BUTTON_IDX) {
                                assert(importAddressObject.hasSetPrivateKeyInMemory());
                                importAddressObject.clearPrivateKeyFromMemory();
                                TLToast.makeText(getActivity(), getString(R.string.cleared_from_memory), TLToast.LENGTH_SHORT, TLToast.TYPE_INFO);
                            }
                            if (which == VIEW_ADDRESS_BUTTON_IDX) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), importAddressObject.getAddress());
                            } else if (which == VIEW_ADDRESS_IN_WEB_BUTTON_IDX) {
                                String url = appDelegate.blockExplorerAPI.getURLForWebViewAddress(importAddressObject.getAddress());
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);
                            } else if (which == RENAME_ADDRESS_BUTTON_IDX) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.enter_label), "", "", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        appDelegate.importedWatchAddresses.setLabel((String) obj, importAddressObject.getPositionInWalletArrayNumber());
                                        manageAccountAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancel() {

                                    }
                                });
                            } else if (which == UNARCHIVE_ADDRESS_BUTTON_IDX) {
                                this.promptToUnarchiveAddress(importAddressObject);
                            } else if (which == DELETE_ADDRESS_BUTTON_IDX) {
                                this.promptToDeleteImportedWatchAddress(importedAddressIdx);
                            }

                        }
                ).show();
    }

    private void promptArchivedImportedAccountsActionSheet(int indexPath, TLWalletUtils.TLAccountType accountType) {
        assert(accountType == TLWalletUtils.TLAccountType.Imported || accountType == TLWalletUtils.TLAccountType.ImportedWatch || accountType == TLWalletUtils.TLAccountType.ColdWallet);
        final TLAccountObject accountObject;
        if (accountType == TLWalletUtils.TLAccountType.ColdWallet) {
            accountObject = appDelegate.coldWalletAccounts.getArchivedAccountObjectForIdx(indexPath);
        } else if (accountType == TLWalletUtils.TLAccountType.Imported) {
            accountObject = appDelegate.importedAccounts.getArchivedAccountObjectForIdx(indexPath);
        } else {
//        } else if (accountType == TLWalletUtils.TLAccountType.ImportedWatch) {
            accountObject = appDelegate.importedWatchAccounts.getArchivedAccountObjectForIdx(indexPath);
        }

        int accountHDIndex = accountObject.getAccountHDIndex();
        String title = getString(R.string.account_id_number, accountHDIndex);
        CharSequence[] otherButtonTitles;
        if (accountObject.getAccountType() == TLWalletUtils.TLAccountType.Imported) {
            otherButtonTitles = new String[]{getString(R.string.view_account_public_key_qr_code), getString(R.string.view_account_private_key_qr_code), getString(R.string.view_addresses), getString(R.string.edit_account_name), getString(R.string.unarchive_account), getString(R.string.delete_account)};
        } else {
            otherButtonTitles = new String[]{getString(R.string.view_account_public_key_qr_code), getString(R.string.view_addresses), getString(R.string.edit_account_name), getString(R.string.unarchive_account), getString(R.string.delete_account)};
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setItems(otherButtonTitles, (dialog, which) -> {
                    int VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX = 0;
                    int VIEW_EXTENDED_PRIVATE_KEY_BUTTON_IDX = 1;
                    int VIEW_ADDRESSES_BUTTON_IDX = 2;
                    int RENAME_ACCOUNT_BUTTON_IDX = 3;
                    int UNARCHIVE_ACCOUNT_BUTTON_IDX = 4;
                    int DELETE_ACCOUNT_BUTTON_IDX = 5;
                    if (accountObject.getAccountType() == TLWalletUtils.TLAccountType.Imported) {
                    } else {
                        VIEW_EXTENDED_PRIVATE_KEY_BUTTON_IDX = -1;
                        VIEW_ADDRESSES_BUTTON_IDX = 1;
                        RENAME_ACCOUNT_BUTTON_IDX = 2;
                        UNARCHIVE_ACCOUNT_BUTTON_IDX = 3;
                        DELETE_ACCOUNT_BUTTON_IDX = 4;
                    }
                    if (which == VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX) {
                        TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountObject.getExtendedPubKey());
                        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_EXTENDED_PUBLIC_KEY));
                    } else if (which == VIEW_EXTENDED_PRIVATE_KEY_BUTTON_IDX) {
                        TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountObject.getExtendedPrivKey());
                        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_EXTENDED_PRIVATE_KEY));
                    } else if (which == VIEW_ADDRESSES_BUTTON_IDX) {
                        showAddressListView(accountObject, false);
                    } else if (which == RENAME_ACCOUNT_BUTTON_IDX) {
                        TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.rename_account), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                            @Override
                            public void onSuccess(Object obj) {
                                if (accountType == TLWalletUtils.TLAccountType.ColdWallet) {
                                    appDelegate.coldWalletAccounts.renameAccount(accountObject.getAccountIdxNumber(), (String) obj);
                                } else if (accountType == TLWalletUtils.TLAccountType.Imported) {
                                    appDelegate.importedAccounts.renameAccount(accountObject.getAccountIdxNumber(), (String) obj);
                                } else if (accountType == TLWalletUtils.TLAccountType.ImportedWatch) {
                                    appDelegate.importedWatchAccounts.renameAccount(accountObject.getAccountIdxNumber(), (String) obj);
                                }
                                manageAccountAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancel() {

                            }
                        });
                    } else if (which == UNARCHIVE_ACCOUNT_BUTTON_IDX) {
                        this.promptToUnarchiveAccount(accountObject);
                    } else if (which == DELETE_ACCOUNT_BUTTON_IDX) {
                        if (accountType == TLWalletUtils.TLAccountType.ColdWallet) {
                            this.promptToDeleteColdWalletAccount(indexPath);
                        } else if (accountType == TLWalletUtils.TLAccountType.Imported) {
                            this.promptToDeleteImportedAccount(indexPath);
                        } else if (accountType == TLWalletUtils.TLAccountType.ImportedWatch) {
                            this.promptToDeleteImportedWatchAccount(indexPath);
                        }
                    }
                        }
                ).show();
    }

    private void promptArchivedAccountsActionSheet(int idx) {
        TLAccountObject accountObject = appDelegate.accounts.getArchivedAccountObjectForIdx(idx);
        int accountHDIndex = accountObject.getAccountHDIndex();
        String title = getString(R.string.account_id_number, accountHDIndex);
        CharSequence[] otherButtonTitles;
        if (appDelegate.preferences.enabledAdvancedMode()) {
            otherButtonTitles = new String[]{getString(R.string.view_account_public_key_qr_code), getString(R.string.view_account_private_key_qr_code), getString(R.string.view_addresses), getString(R.string.edit_account_name), getString(R.string.unarchive_account)};
        } else {
            otherButtonTitles = new String[]{getString(R.string.view_addresses), getString(R.string.edit_account_name), getString(R.string.unarchive_account)};
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setItems(otherButtonTitles, (dialog, which) -> {
                            int VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX = 0;
                            int VIEW_EXTENDED_PRIVATE_KEY_BUTTON_IDX = 1;
                            int VIEW_ADDRESSES_BUTTON_IDX = 2;
                            int RENAME_ACCOUNT_BUTTON_IDX = 3;
                            int UNARCHIVE_ACCOUNT_BUTTON_IDX = 4;
                            if (!appDelegate.preferences.enabledAdvancedMode()) {
                                VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX = -1;
                                VIEW_EXTENDED_PRIVATE_KEY_BUTTON_IDX = -1;
                                VIEW_ADDRESSES_BUTTON_IDX = 0;
                                RENAME_ACCOUNT_BUTTON_IDX = 1;
                                UNARCHIVE_ACCOUNT_BUTTON_IDX = 2;
                            }

                            if (which == VIEW_EXTENDED_PUBLIC_KEY_BUTTON_IDX) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountObject.getExtendedPubKey());
                                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_EXTENDED_PUBLIC_KEY));
                            } else if (which == VIEW_EXTENDED_PRIVATE_KEY_BUTTON_IDX) {
                                TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountObject.getExtendedPrivKey());
                                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_EXTENDED_PRIVATE_KEY));
                            } else if (which == VIEW_ADDRESSES_BUTTON_IDX) {
                                showAddressListView(accountObject, false);
                            } else if (which == RENAME_ACCOUNT_BUTTON_IDX) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.rename_account), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        appDelegate.accounts.renameAccount(accountObject.getAccountIdxNumber(), (String) obj);
                                        manageAccountAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancel() {

                                    }
                                });
                            } else if (which == UNARCHIVE_ACCOUNT_BUTTON_IDX) {
                                this.promptToUnarchiveAccount(accountObject);
                            }
                        }
                ).show();
    }

    private void promptToManuallyScanForStealthTransactionAccount(TLAccountObject accountObject) {
        TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.scan_for_reusable_address_transaction),
                "", getString(R.string.transaction_id), getString(R.string.scan), getString(R.string.cancel), "", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
            @Override
            public void onSuccess(Object obj) {
                manuallyScanForStealthTransactionAccount(accountObject, (String) obj);
            }

            @Override
            public void onCancel() {
            }
        });
    }

    private void lookupStealthTx(String stealthAddress, String txid) {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                JSONObject jsonData = (JSONObject) msg.obj;
                if (jsonData != null) {
                    Log.d(TAG, "lookupStealthTx success " + jsonData.toString());
                }
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonData = appDelegate.stealthExplorerAPI.lookupTx(stealthAddress, txid);
                    Message message = Message.obtain();
                    message.obj = jsonData;
                    handler.sendMessage(Message.obtain(message));
                } catch (Exception e) {
                }
            }
        }).start();
    }

    private void onGetTxSuccessOfManuallyScanForStealthTransactionAccount(TLAccountObject accountObject, String txid, JSONObject jsonData) {
        Pair<String, List<String>> stealthDataScriptAndOutputAddresses = TLStealthWallet.getStealthDataScriptAndOutputAddresses(jsonData);
        if (stealthDataScriptAndOutputAddresses == null || stealthDataScriptAndOutputAddresses.first == null) {
            TLHUDWrapper.hideHUD();
            TLPrompts.promptSuccessMessage(getActivity(), "", getString(R.string.transaction_is_not_a_reusable_address_transaction));
            return;
        }

        String scanPriv = accountObject.stealthWallet.getStealthAddressScanKey();
        String spendPriv = accountObject.stealthWallet.getStealthAddressSpendKey();
        String stealthDataScript = stealthDataScriptAndOutputAddresses.first;
        String secret = TLStealthAddress.getPaymentAddressPrivateKeySecretFromScript(stealthDataScript, scanPriv, spendPriv);
        if (secret != null) {
            String paymentAddress = TLBitcoinjWrapper.getAddressFromSecret(secret, appDelegate.appWallet.walletConfig.isTestnet);
            if (stealthDataScriptAndOutputAddresses.second.indexOf(paymentAddress) != -1) {
                TLTxObject txObject = new TLTxObject(appDelegate, jsonData);

                Handler handler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        JSONObject jsonData = (JSONObject) msg.obj;
                        if (jsonData != null) {
                            try {
                                if (!jsonData.has("unspent_outputs")) {
                                    TLHUDWrapper.hideHUD();
                                    TLPrompts.promptSuccessMessage(getActivity(), "",  getString(R.string.funds_have_been_claimed_already));
                                    return;
                                }
                                JSONArray unspentOutputs = jsonData.getJSONArray("unspent_outputs");
                                if (unspentOutputs.length() > 0) {
                                    String privateKey = TLBitcoinjWrapper.privateKeyFromSecret(secret, appDelegate.appWallet.walletConfig.isTestnet);
                                    long txTime = txObject.getTxUnixTime();
                                    accountObject.stealthWallet.addStealthAddressPaymentKey(privateKey, paymentAddress,
                                            txid, txTime, TLWalletJSONKeys.TLStealthPaymentStatus.Unspent);

                                    TLHUDWrapper.hideHUD();
                                    TLPrompts.promptSuccessMessage(getActivity(), getString(R.string.success), getString(R.string.funds_imported));
                                    accountObject.getAccountDataO(true, new TLCallback() {
                                        @Override
                                        public void onSuccess(Object obj) {
                                            refreshWalletAccounts(false);
                                            lookupStealthTx(accountObject.stealthWallet.getStealthAddress(), txid);
                                        }

                                        @Override
                                        public void onFail(Integer status, String error) {
                                        }
                                    });
                                } else {
                                    TLHUDWrapper.hideHUD();
                                    TLPrompts.promptSuccessMessage(getActivity(), "",  getString(R.string.funds_have_been_claimed_already));
                                }
                            } catch (JSONException e) {
                                TLHUDWrapper.hideHUD();
                                TLPrompts.promptSuccessMessage(getActivity(), "",  getString(R.string.funds_have_been_claimed_already));
                            }
                        } else {
                            TLHUDWrapper.hideHUD();
                            TLPrompts.promptSuccessMessage(getActivity(), "", getString(R.string.funds_have_been_claimed_already));
                        }
                    }
                };
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonData = appDelegate.blockExplorerAPI.getUnspentOutputs(Arrays.asList(paymentAddress));
                            Message message = Message.obtain();
                            message.obj = jsonData;
                            handler.sendMessage(Message.obtain(message));
                        } catch (Exception e) {
                        }
                    }
                }).start();
            } else {
                TLHUDWrapper.hideHUD();
                TLPrompts.promptSuccessMessage(getActivity(), "", getString(R.string.transaction_does_not_belong_to_account, txid));
            }
        } else {
            TLHUDWrapper.hideHUD();
            TLPrompts.promptSuccessMessage(getActivity(), "", getString(R.string.transaction_does_not_belong_to_account, txid));
        }

    }

    private void manuallyScanForStealthTransactionAccount(TLAccountObject accountObject, String txid) {
        if (accountObject.stealthWallet.paymentTxidExist(txid)) {
            TLPrompts.promptSuccessMessage(getActivity(), "", getString(R.string.transaction_already_accounted_for, txid));
            return;
        }

        if (txid.length() != 64 || TLWalletUtils.hexStringToData(txid) == null) {
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.invalid_transaction_id), "");
            return;
        }

        TLHUDWrapper.showHUD(getActivity(), getString(R.string.checking_transaction));


        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                JSONObject jsonData = (JSONObject) msg.obj;
                if (jsonData != null) {
                    onGetTxSuccessOfManuallyScanForStealthTransactionAccount(accountObject, txid, jsonData);
                } else {
                    TLHUDWrapper.hideHUD();
                    TLPrompts.promptSuccessMessage(getActivity(), getString(R.string.error), getString(R.string.error_fetching_transaction));
                }
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonData = appDelegate.blockExplorerAPI.getTx(txid);
                    Message message = Message.obtain();
                    message.obj = jsonData;
                    handler.sendMessage(Message.obtain(message));
                } catch (Exception e) {
                }
            }
        }).start();
    }

    private void promptInfoAndToManuallyScanForStealthTransactionAccount(TLAccountObject accountObject) {
        if (!appDelegate.suggestions.disabledShowManuallyScanTransactionForStealthTxInfo()) {
            TLPrompts.promptForOK(getActivity(),"", getString(R.string.scan_payment_txid_description), new TLPrompts.PromptOKCallback() {
                @Override
                public void onSuccess() {
                    promptToManuallyScanForStealthTransactionAccount(accountObject);
                    appDelegate.suggestions.setDisableShowManuallyScanTransactionForStealthTxInfo(true);
                }
            });

        } else {
            this.promptToManuallyScanForStealthTransactionAccount(accountObject);
        }
    }

    private void promptToUnarchiveAccount(TLAccountObject accountObject) {
        TLPrompts.promptForOKCancel(getActivity(), getString(R.string.unarchive_account),
                getString(R.string.are_you_sure_you_want_to_unarchive_account, accountObject.getAccountName()), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        if (accountObject.getAccountType() == TLWalletUtils.TLAccountType.HDWallet) {
                            appDelegate.accounts.unarchiveAccount(accountObject.getAccountIdxNumber());
                        } else if (accountObject.getAccountType() == TLWalletUtils.TLAccountType.ColdWallet) {
                            appDelegate.coldWalletAccounts.unarchiveAccount(accountObject.getPositionInWalletArray());
                        } else if (accountObject.getAccountType() == TLWalletUtils.TLAccountType.Imported) {
                            appDelegate.importedAccounts.unarchiveAccount(accountObject.getPositionInWalletArray());
                        } else if (accountObject.getAccountType() == TLWalletUtils.TLAccountType.ImportedWatch) {
                            appDelegate.importedWatchAccounts.unarchiveAccount(accountObject.getPositionInWalletArray());
                        }
                        if (!accountObject.isWatchOnly() && !accountObject.isColdWalletAccount() && !accountObject.stealthWallet.hasUpdateStealthPaymentStatuses) {
                            accountObject.stealthWallet.updateStealthPaymentStatusesAsync();
                        }
                        manageAccountAdapter.notifyDataSetChanged();
                        accountObject.getAccountDataO();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void promptToArchiveAccount(TLAccountObject accountObject) {
        TLPrompts.promptForOKCancel(getActivity(), getString(R.string.archive_account),
                getString(R.string.are_you_sure_you_want_to_archive_account, accountObject.getAccountName()), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        if (accountObject.getAccountType() == TLWalletUtils.TLAccountType.HDWallet) {
                            appDelegate.accounts.archiveAccount(accountObject.getAccountIdxNumber());
                        } else if (accountObject.getAccountType() == TLWalletUtils.TLAccountType.ColdWallet) {
                            appDelegate.coldWalletAccounts.archiveAccount(accountObject.getPositionInWalletArray());
                        } else if (accountObject.getAccountType() == TLWalletUtils.TLAccountType.Imported) {
                            appDelegate.importedAccounts.archiveAccount(accountObject.getPositionInWalletArray());
                        } else if (accountObject.getAccountType() == TLWalletUtils.TLAccountType.ImportedWatch) {
                            appDelegate.importedWatchAccounts.archiveAccount(accountObject.getPositionInWalletArray());
                        }
                        manageAccountAdapter.notifyDataSetChanged();
                        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_ARCHIVE_ACCOUNT));
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void promptToArchiveAccountHDWalletAccount(TLAccountObject accountObject) {
        if (accountObject.getAccountIdxNumber() == 0) {
            TLToast.makeText(getActivity(), getString(R.string.cannot_archive_your_default_account), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
            return;
        } else if (appDelegate.accounts.getNumberOfAccounts() <= 1) {
            TLToast.makeText(getActivity(), getString(R.string.cannot_archive_your_one_and_only_account), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
            return;
        } else {
            promptToArchiveAccount(accountObject);
        }
    }

    private void promptToArchiveAddress(TLImportedAddress importedAddressObject) {
        TLPrompts.promptForOKCancel(getActivity(), getString(R.string.archive_address),
                getString(R.string.are_you_sure_you_want_to_archive_address, importedAddressObject.getLabel()), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        if (importedAddressObject.isWatchOnly()) {
                            appDelegate.importedWatchAddresses.archiveAddress(importedAddressObject.getPositionInWalletArrayNumber());
                        } else {
                            appDelegate.importedAddresses.archiveAddress(importedAddressObject.getPositionInWalletArrayNumber());
                        }

                        manageAccountAdapter.notifyDataSetChanged();
                        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_ARCHIVE_ACCOUNT));
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void promptToUnarchiveAddress(TLImportedAddress importedAddressObject) {
        TLPrompts.promptForOKCancel(getActivity(), getString(R.string.unarchive_address),
                getString(R.string.are_you_sure_you_want_to_unarchive_address, importedAddressObject.getLabel()), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        if (importedAddressObject.isWatchOnly()) {
                            appDelegate.importedWatchAddresses.unarchiveAddress(importedAddressObject.getPositionInWalletArrayNumber());
                        } else {
                            appDelegate.importedAddresses.unarchiveAddress(importedAddressObject.getPositionInWalletArrayNumber());
                        }

                        manageAccountAdapter.notifyDataSetChanged();
                        importedAddressObject.getSingleAddressDataO(true);
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void promptToDeleteColdWalletAccount(int indexPath) {
        TLAccountObject accountObject = appDelegate.coldWalletAccounts.getArchivedAccountObjectForIdx(indexPath);
        TLPrompts.promptForOKCancel(getActivity(), getString(R.string.delete_account_name, accountObject.getAccountName()),
                getString(R.string.are_you_sure_you_want_to_delete_this_account), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        appDelegate.coldWalletAccounts.deleteAccount(indexPath);
                        updateListView();
                        manageAccountAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void promptToDeleteImportedAccount(int indexPath) {
        TLAccountObject accountObject = appDelegate.importedAccounts.getArchivedAccountObjectForIdx(indexPath);
        TLPrompts.promptForOKCancel(getActivity(), getString(R.string.delete_account_name, accountObject.getAccountName()),
                getString(R.string.are_you_sure_you_want_to_delete_this_account), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        appDelegate.importedAccounts.deleteAccount(indexPath);
                        updateListView();
                        manageAccountAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void promptToDeleteImportedWatchAccount(int indexPath) {
        TLAccountObject accountObject = appDelegate.importedWatchAccounts.getArchivedAccountObjectForIdx(indexPath);
        TLPrompts.promptForOKCancel(getActivity(), getString(R.string.delete_account_name, accountObject.getAccountName()),
                getString(R.string.are_you_sure_you_want_to_delete_this_account), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        appDelegate.importedWatchAccounts.deleteAccount(indexPath);
                        updateListView();
                        manageAccountAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void promptToDeleteImportedAddress(int importedAddressIdx) {
        TLImportedAddress importedAddressObject = appDelegate.importedAddresses.getArchivedAddressObjectAtIdx(importedAddressIdx);
        TLPrompts.promptForOKCancel(getActivity(), getString(R.string.delete_account_name, importedAddressObject.getLabel()),
                getString(R.string.are_you_sure_you_want_to_delete_this_account), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        appDelegate.importedAddresses.deleteAddress(importedAddressIdx);
                        updateListView();
                        manageAccountAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private void promptToDeleteImportedWatchAddress(int importedAddressIdx) {
        TLImportedAddress importedAddressObject = appDelegate.importedWatchAddresses.getArchivedAddressObjectAtIdx(importedAddressIdx);
        TLPrompts.promptForOKCancel(getActivity(), getString(R.string.delete_account_name, importedAddressObject.getLabel()),
                getString(R.string.are_you_sure_you_want_to_delete_this_account), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        appDelegate.importedWatchAddresses.deleteAddress(importedAddressIdx);
                        updateListView();
                        manageAccountAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private boolean importColdWalletAccount(String extendedPublicKey) {
        if (TLHDWalletWrapper.isValidExtendedPublicKey(extendedPublicKey, appDelegate.appWallet.walletConfig.isTestnet)) {
            appDelegate.saveWalletJson();
            appDelegate.saveWalletJSONEnabled = false;
            String accountName = getString(R.string.importing_cold_wallet_account_account_name, (appDelegate.coldWalletAccounts.getNumberOfAccounts() + appDelegate.coldWalletAccounts.getNumberOfArchivedAccounts() + 1));
            TLAccountObject accountObject = appDelegate.coldWalletAccounts.addAccountWithExtendedKey(extendedPublicKey, accountName);
            TLHUDWrapper.showHUD(getActivity(), getString(R.string.importing_cold_wallet_account));
            accountObject.recoverAccount(false, true, new TLCallback() {
                @Override
                public void onSuccess(Object obj) {
                    appDelegate.saveWalletJSONEnabled = true;
                    appDelegate.saveWalletJson();

                    LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_IMPORT_COLD_WALLET_ACCOUNT));
                    // don't need to call do accountObject.getAccountData like in importAccount() cause watch account does not see stealth payments. yet
                    TLHUDWrapper.hideHUD();
                    TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.edit_account_name), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            String accountName = (String) obj;
                            if (accountName != null && accountName.length() != 0) {
                                appDelegate.coldWalletAccounts.renameAccount(accountObject.getAccountIdxNumber(), accountName);
                            }

                            TLToast.makeText(getActivity().getApplicationContext(), getString(R.string.account_account_name_imported, accountName), TLToast.LENGTH_SHORT, TLToast.TYPE_SUCCESS);
                            updateListView();
                            manageAccountAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancel() {
                            updateListView();
                            manageAccountAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onFail(Integer status, String error) {
                    appDelegate.importedWatchAccounts.deleteAccount(appDelegate.importedWatchAccounts.getNumberOfAccounts() - 1);
                    TLHUDWrapper.hideHUD();
                    TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error_importing_account), getString(R.string.try_again));
                    updateListView();
                    manageAccountAdapter.notifyDataSetChanged();
                }
            });
            return true;
        } else {
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.invalid_account_public_key), "");
            return false;
        }
    }

    private boolean importAccount(String extendedPrivateKey) {
        if (TLHDWalletWrapper.isValidExtendedPrivateKey(extendedPrivateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
            appDelegate.saveWalletJson();
            appDelegate.saveWalletJSONEnabled = false;
            String accountName = getString(R.string.imported_account_account_name, (appDelegate.importedAccounts.getNumberOfAccounts() + appDelegate.importedAccounts.getNumberOfArchivedAccounts() + 1));
            TLAccountObject accountObject = appDelegate.importedAccounts.addAccountWithExtendedKey(extendedPrivateKey, accountName);
            TLHUDWrapper.showHUD(getActivity(), getString(R.string.importing_account));
            accountObject.recoverAccount(false, true, new TLCallback() {
                @Override
                public void onSuccess(Object obj) {
                    appDelegate.saveWalletJSONEnabled = true;
                    appDelegate.saveWalletJson();
                    LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_IMPORT_ACCOUNT));
                    appDelegate.stealthWebSocket.sendMessageGetChallenge();
                    TLHUDWrapper.hideHUD();
                    TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.edit_account_name), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            String accountName = (String) obj;
                            if (accountName != null && accountName.length() != 0) {
                                appDelegate.importedAccounts.renameAccount(accountObject.getAccountIdxNumber(), accountName);
                            }
                            TLToast.makeText(getActivity().getApplicationContext(), getString(R.string.account_account_name_imported, accountName), TLToast.LENGTH_SHORT, TLToast.TYPE_SUCCESS);
                            updateListView();
                            manageAccountAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancel() {
                            updateListView();
                            manageAccountAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onFail(Integer status, String error) {
                    appDelegate.importedAccounts.deleteAccount(appDelegate.importedAccounts.getNumberOfAccounts() - 1);
                    TLHUDWrapper.hideHUD();
                    TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error_importing_account), getString(R.string.try_again));
                    updateListView();
                    manageAccountAdapter.notifyDataSetChanged();
                }
            });
            return true;
        } else {
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.invalid_account_private_key), "");
            return false;
        }
    }

    private boolean importWatchOnlyAccount(String extendedPublicKey) {
        if (TLHDWalletWrapper.isValidExtendedPublicKey(extendedPublicKey, appDelegate.appWallet.walletConfig.isTestnet)) {
            appDelegate.saveWalletJson();
            appDelegate.saveWalletJSONEnabled = false;
            String accountName = getString(R.string.importing_watch_account_account_name, (appDelegate.importedWatchAccounts.getNumberOfAccounts() + appDelegate.importedWatchAccounts.getNumberOfArchivedAccounts() + 1));
            TLAccountObject accountObject = appDelegate.importedWatchAccounts.addAccountWithExtendedKey(extendedPublicKey, accountName);
            TLHUDWrapper.showHUD(getActivity(), getString(R.string.importing_watch_account));
            accountObject.recoverAccount(false, true, new TLCallback() {
                @Override
                public void onSuccess(Object obj) {
                    appDelegate.saveWalletJSONEnabled = true;
                    appDelegate.saveWalletJson();

                    LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_IMPORT_WATCH_ONLY_ACCOUNT));
                    // don't need to call do accountObject.getAccountData like in importAccount() cause watch account does not see stealth payments. yet
                    TLHUDWrapper.hideHUD();
                    TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.edit_account_name), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            String accountName = (String) obj;
                            if (accountName != null && accountName.length() != 0) {
                                appDelegate.importedWatchAccounts.renameAccount(accountObject.getAccountIdxNumber(), accountName);
                            }

                            TLToast.makeText(getActivity().getApplicationContext(), getString(R.string.account_account_name_imported, accountName), TLToast.LENGTH_SHORT, TLToast.TYPE_SUCCESS);
                            updateListView();
                            manageAccountAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancel() {
                            updateListView();
                            manageAccountAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onFail(Integer status, String error) {
                    appDelegate.importedWatchAccounts.deleteAccount(appDelegate.importedWatchAccounts.getNumberOfAccounts() - 1);
                    TLHUDWrapper.hideHUD();
                    TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error_importing_account), getString(R.string.try_again));
                    updateListView();
                    manageAccountAdapter.notifyDataSetChanged();
                }
            });
            return true;
        } else {
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.invalid_account_public_key), "");
            return false;
        }
    }

    private boolean checkAndImportAddress(String privateKey, String encryptedPrivateKey) {
        if (TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
            if (encryptedPrivateKey != null) {
                TLPrompts.promptYesNo(getActivity(), getString(R.string.import_private_key_encrypted_or_unencrypted), getString(R.string.import_private_key_encrypted_or_unencrypted_description),
                        getString(R.string.unencrypted), getString(R.string.encrypted), new TLPrompts.PromptCallback() {
                            @Override
                            public void onSuccess(Object obj) {
                                TLImportedAddress importedAddressObject = appDelegate.importedAddresses.addImportedPrivateKey(privateKey, null);
                                refreshAfterImportAddress(importedAddressObject);
                            }

                            @Override
                            public void onCancel() {
                                TLImportedAddress importedAddressObject = appDelegate.importedAddresses.addImportedPrivateKey(privateKey, encryptedPrivateKey);
                                refreshAfterImportAddress(importedAddressObject);
                            }
                        });
            } else {
                TLImportedAddress importedAddressObject = appDelegate.importedAddresses.addImportedPrivateKey(privateKey, null);
                this.refreshAfterImportAddress(importedAddressObject);
            }

            return true;
        } else {
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.invalid_private_key));
            return false;
        }
    }

    private void refreshAfterImportAddress(TLImportedAddress importedAddressObject) {

        importedAddressObject.getSingleAddressDataO(true, new TLCallback() {
            @Override
            public void onSuccess(Object obj) {
                updateListView();
                manageAccountAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFail(Integer status, String error) {
                updateListView();
                manageAccountAdapter.notifyDataSetChanged();
            }
        });

        TLToast.makeText(getActivity().getApplicationContext(), getString(R.string.imported_address), TLToast.LENGTH_SHORT, TLToast.TYPE_SUCCESS);
        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_IMPORT_PRIVATE_KEY));
    }

    private boolean checkAndImportWatchAddress(String address) {
        if (TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
            if (TLStealthAddress.isStealthAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.cannot_import_reusable_address));
                return false;
            }

            TLImportedAddress importedAddressObject = appDelegate.importedWatchAddresses.addImportedWatchAddress(address);
            importedAddressObject.getSingleAddressDataO(true, new TLCallback() {
                @Override
                public void onSuccess(Object obj) {
                    updateListView();
                    manageAccountAdapter.notifyDataSetChanged();
                }

                @Override
                public void onFail(Integer status, String error) {
                    updateListView();
                    manageAccountAdapter.notifyDataSetChanged();
                }
            });
            TLToast.makeText(getActivity().getApplicationContext(), getString(R.string.imported_address), TLToast.LENGTH_SHORT, TLToast.TYPE_SUCCESS);
            LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_IMPORT_WATCH_ONLY_ADDRESS));
            return true;
        } else {
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.invalid_bitcoin_address), "");
            return false;
        }
    }

    private void promptColdWalletActionSheet() {
        CharSequence[] otherButtonTitles = {getString(R.string.import_with_qr_code), getString(R.string.import_with_text_input)};
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.import_cold_wallet_account))
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == 0) {
                                scanQRCode(SCAN_URI_COLD_WALLET_ACCOUNT);
                            } else if (which == 1) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.import_cold_wallet_account),
                                        getString(R.string.input_account_public_key), "", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                            @Override
                                            public void onSuccess(Object obj) {
                                                importColdWalletAccount((String) obj);
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

    private void promptImportAccountActionSheet() {
        CharSequence[] otherButtonTitles = {getString(R.string.import_with_qr_code), getString(R.string.import_with_text_input)};
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.import_account))
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == 0) {
                                scanQRCode(SCAN_URI_IMPORT_ACCOUNT);
                            } else if (which == 1) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.import_account),
                                        getString(R.string.input_account_private_key), "", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                            @Override
                                            public void onSuccess(Object obj) {
                                                importAccount((String) obj);
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

    private void promptImportWatchAccountActionSheet() {
        CharSequence[] otherButtonTitles = {getString(R.string.import_with_qr_code), getString(R.string.import_with_text_input)};
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.import_watch_account))
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == 0) {
                                scanQRCode(SCAN_URI_IMPORT_WATCH_ACCOUNT);
                            } else if (which == 1) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.import_watch_account),
                                        getString(R.string.input_account_public_key), "", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                            @Override
                                            public void onSuccess(Object obj) {
                                                importWatchOnlyAccount((String) obj);
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

    private void promptImportPrivateKeyActionSheet() {
        CharSequence[] otherButtonTitles = {getString(R.string.import_with_qr_code), getString(R.string.import_with_text_input)};
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.import_private_key))
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == 0) {
                                scanQRCode(SCAN_URI_PRIVATE_KEY);
                            } else if (which == 1) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.import_private_key), "", "", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        String inputText = (String) obj;
                                        if (TLBitcoinjWrapper.isBIP38EncryptedKey(inputText, appDelegate.appWallet.walletConfig.isTestnet)) {
                                            TLPrompts.promptForEncryptedPrivKeyPassword(getActivity(), inputText, appDelegate.appWallet.walletConfig.isTestnet, new TLPrompts.PromptCallback() {
                                                @Override
                                                public void onSuccess(Object obj) {
                                                    checkAndImportAddress((String) obj, inputText);
                                                }

                                                @Override
                                                public void onCancel() {
                                                }
                                            });

                                        } else {
                                            checkAndImportAddress(inputText, null);
                                        }
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

    private void promptImportWatchAddressActionSheet() {
        CharSequence[] otherButtonTitles = {getString(R.string.import_with_qr_code), getString(R.string.import_with_text_input)};
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.import_watch_address))
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == 0) {
                                scanQRCode(SCAN_URI_IMPORT_WATCH_ADDRESS);
                            } else if (which == 1) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.import_watch_address),
                                        "", getString(R.string.address), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                            @Override
                                            public void onSuccess(Object obj) {
                                                checkAndImportWatchAddress((String) obj);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String value = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            if (requestCode == SCAN_URI_IMPORT_ACCOUNT) {
                this.importAccount(value);
            } else if (requestCode == SCAN_URI_COLD_WALLET_ACCOUNT) {
                this.importColdWalletAccount(value);
            } else if (requestCode == SCAN_URI_IMPORT_WATCH_ACCOUNT) {
                this.importWatchOnlyAccount(value);
            } else if (requestCode == SCAN_URI_PRIVATE_KEY) {
                if (TLBitcoinjWrapper.isBIP38EncryptedKey(value, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptForEncryptedPrivKeyPassword(getActivity(), value, appDelegate.appWallet.walletConfig.isTestnet, new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            checkAndImportAddress((String) obj, value);
                        }

                        @Override
                        public void onCancel() {
                        }
                    });

                } else {
                    checkAndImportAddress(value, null);
                }
            } else if (requestCode == SCAN_URI_IMPORT_WATCH_ADDRESS) {
                this.checkAndImportWatchAddress(value);
            }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (appDelegate != null) {
            LocalBroadcastManager.getInstance(appDelegate.context).unregisterReceiver(receiver);
        }
    }
}
