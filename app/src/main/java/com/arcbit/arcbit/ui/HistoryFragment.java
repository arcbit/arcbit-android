package com.arcbit.arcbit.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.View.OnClickListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.arcbit.arcbit.model.TLAccountObject;
import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLCallback;
import com.arcbit.arcbit.model.TLOperationsManager;
import com.arcbit.arcbit.model.TLSendFormData;
import com.arcbit.arcbit.model.TLImportedAddress;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.model.TLTxObject;
import com.arcbit.arcbit.model.TLWalletUtils;
import com.arcbit.arcbit.ui.utils.TLPrompts;
import com.arcbit.arcbit.ui.utils.TLToast;
import com.arcbit.arcbit.ui.items.*;

import com.arcbit.arcbit.R;

public class HistoryFragment extends android.support.v4.app.Fragment implements View.OnClickListener{
    private int MAX_CONFIRMATIONS_TO_DISPLAY = 6;

    private ListView selectAccountListview;
    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    private View rootView;
    private TransactionAdapter transactionAdapter;

    private int SELECT_ACCOUNT_REQUEST_CODE = 1334;

    private ImageButton imageButtonSelectAccount;
    private RelativeLayout layoutAccount;
    private TextView accountNameTextView;
    private TextView accountBalanceTextView;
    private ProgressBar balanceSpinner;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED)) {
                updateViewToNewSelectedObject();
                updateTransactionsTableView();
            } else if (action.equals(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA)) {
                updateViewToNewSelectedObject();
            } else if (action.equals(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION)) {
                updateViewToNewSelectedObject();
            } else if (action.equals(TLNotificationEvents.EVENT_PREFERENCES_BITCOIN_DISPLAY_CHANGED)) {
                updateAccountBalance();
                updateTransactionsTableView();
            } else if (action.equals(TLNotificationEvents.EVENT_PREFERENCES_FIAT_DISPLAY_CHANGED)) {
                updateAccountBalance();
                updateTransactionsTableView();
            } else if (action.equals(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_BLOCK)) {
                updateTransactionsTableView();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_history, container, false);
        if (appDelegate == null) {
            return rootView;
        }
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getActivity().setTitle(getString(R.string.history));

        balanceSpinner = (ProgressBar)rootView.findViewById(R.id.balanceProgressBar);
        balanceSpinner.setVisibility(View.GONE);

        imageButtonSelectAccount = (ImageButton)rootView.findViewById(R.id.selectAccountArrow);
        imageButtonSelectAccount.setOnClickListener(this);

        layoutAccount = (RelativeLayout)rootView.findViewById(R.id.layoutAccount);
        layoutAccount.setOnClickListener(this);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_PREFERENCES_BITCOIN_DISPLAY_CHANGED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_PREFERENCES_FIAT_DISPLAY_CHANGED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_BLOCK));

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.arcbit_main, R.color.arcbit_main, R.color.arcbit_main);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(false);
                if (appDelegate.historySelectedObject.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Account) {
                    TLAccountObject accountObject = (TLAccountObject) appDelegate.historySelectedObject.getSelectedObject();
                    accountObject.setFetchedAccountData(false);
                } else if (appDelegate.historySelectedObject.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Address) {
                    TLImportedAddress importedAddress = (TLImportedAddress) appDelegate.historySelectedObject.getSelectedObject();
                    importedAddress.setFetchedAccountData(false);
                }

                updateTransactionsTableView();
                refreshSelectedAccount(true);
            }
        });

        accountNameTextView = (TextView)rootView.findViewById(R.id.account_name);
        accountNameTextView.setText(appDelegate.historySelectedObject.getLabelForSelectedObject());
        accountBalanceTextView = (TextView)rootView.findViewById(R.id.account_balance);
        updateAccountBalance();

        transactionAdapter = new TransactionAdapter(rootView.getContext());

        selectAccountListview = (ListView) rootView.findViewById(R.id.history_list_view);

        selectAccountListview.setAdapter(transactionAdapter);

        this.updateViewToNewSelectedObject();

        this.refreshSelectedAccount(false);

        updateViewToNewSelectedObject();
        LocalBroadcastManager.getInstance(this.appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_HISTORY));

        return rootView;
    }

    void refreshSelectedAccount(boolean fetchDataAgain) {
        if (!appDelegate.historySelectedObject.hasFetchedCurrentFromData() || fetchDataAgain) {
            if (appDelegate.historySelectedObject.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Account) {
                TLAccountObject accountObject = (TLAccountObject) appDelegate.historySelectedObject.getSelectedObject();
                balanceSpinner.setVisibility(View.VISIBLE);
                accountBalanceTextView.setVisibility(View.INVISIBLE);
                accountObject.getAccountDataO(fetchDataAgain, new TLCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        accountBalanceTextView.setVisibility(View.VISIBLE);
                        balanceSpinner.setVisibility(View.GONE);
                        if (accountObject.downloadState != TLOperationsManager.TLDownloadState.Downloaded) {
                            updateAccountBalance();
                        }
                    }

                    @Override
                    public void onFail(Integer status, String error) {
                    }
                });

            } else if (appDelegate.historySelectedObject.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Address) {
                TLImportedAddress importedAddress = (TLImportedAddress) appDelegate.historySelectedObject.getSelectedObject();
                balanceSpinner.setVisibility(View.VISIBLE);
                accountBalanceTextView.setVisibility(View.INVISIBLE);
                importedAddress.getSingleAddressDataO(fetchDataAgain, new TLCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        accountBalanceTextView.setVisibility(View.VISIBLE);
                        balanceSpinner.setVisibility(View.GONE);
                        if (importedAddress.downloadState != TLOperationsManager.TLDownloadState.Downloaded) {
                            updateAccountBalance();
                        }
                    }

                    @Override
                    public void onFail(Integer status, String error) {
                    }
                });
            }
        } else {
            String balance = appDelegate.currencyFormat.getProperAmount(appDelegate.historySelectedObject.getBalanceForSelectedObject());
            accountBalanceTextView.setText(balance);
            balanceSpinner.setVisibility(View.GONE);
        }
    }

    void updateViewToNewSelectedObject() {
        String label = appDelegate.historySelectedObject.getLabelForSelectedObject();
        accountNameTextView.setText(label);
        this.updateAccountBalance();
        this.updateTransactionsTableView();
    }

    void updateTransactionsTableView() {
        transactionAdapter.notifyDataSetChanged();
    }

    void updateAccountBalance() {
        String balance = appDelegate.currencyFormat.getProperAmount(appDelegate.historySelectedObject.getBalanceForSelectedObject());
        balanceSpinner.setVisibility(View.GONE);
        accountBalanceTextView.setText(balance);
        accountBalanceTextView.setVisibility(View.VISIBLE);
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
        setupToolbar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SELECT_ACCOUNT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                Integer selectAccountType = data.getIntExtra("SELECT_ACCOUNT_TYPE", 0);
                Integer selectAccountIdx = data.getIntExtra("SELECT_ACCOUNT_IDX", 0);
                appDelegate.updateHistorySelectedObject(TLWalletUtils.TLSendFromType.getSendFromType(selectAccountType), selectAccountIdx);
                updateViewToNewSelectedObject();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.selectAccountArrow:
            case R.id.layoutAccount:
                android.support.v4.app.Fragment fragmentChild = new SelectAccountFragment();
                fragmentChild.setTargetFragment(this, SELECT_ACCOUNT_REQUEST_CODE);
                android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.right_slide_in, R.anim.left_slide_out, R.anim.left_slide_in, R.anim.right_slide_out);
                transaction.replace(R.id.fragment_container, fragmentChild);
                transaction.addToBackStack(null);
                transaction.commit();
                break;
            default:
                break;
        }
    }

    private void promptTransactionActionSheet(String txHash) {
        CharSequence[] otherButtonTitles = {getString(R.string.view_in_web), getString(R.string.label_transaction), getString(R.string.copy_transaction_id_to_clipboard)};
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.id_colon, txHash))
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == 0) {
                                String url = appDelegate.blockExplorerAPI.getURLForWebViewTx(txHash);
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);
                                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_ACCOUNT_ADDRESS_IN_WEB));
                            } else if (which == 1) {
                                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.edit_transaction_label), "", getString(R.string.label), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        String tag = (String) obj;
                                        if (tag.length() == 0) {
                                            appDelegate.appWallet.deleteTransactionTag(txHash);
                                        } else {
                                            appDelegate.appWallet.setTransactionTag(txHash, tag);
                                            LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_TAG_TRANSACTION));
                                        }
                                        transactionAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancel() {
                                    }
                                });
                            } else if (which == 2) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("txid text", txHash);
                                TLToast.makeText(getActivity(), getActivity().getString(R.string.copied_to_clipboard), TLToast.LENGTH_LONG, TLToast.TYPE_INFO);
                                clipboard.setPrimaryClip(clip);
                            }
                            dialog.dismiss();


                        }
                ).show();
    }

    private class TransactionAdapter extends ArrayAdapter<Item> {
        private Context context;
        private LayoutInflater vi;

        public TransactionAdapter(Context context) {
            super(context, 0);
            this.context = context;
            vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            final Item item = getItem(position); //better??
            if (item != null) {
                if(item.isSection()){
                    SectionItem si = (SectionItem)item;
                    v = vi.inflate(R.layout.list_item_section, null);

                    v.setOnClickListener(null);
                    v.setOnLongClickListener(null);
                    v.setLongClickable(false);

                    final TextView sectionView = (TextView) v.findViewById(R.id.list_item_section_text);
                    sectionView.setText(si.getTitle());
                } else {
                    TransactionItem ei = (TransactionItem)item;
                    v = vi.inflate(R.layout.list_item_transaction, null);

                    final Button amountButton = (Button)v.findViewById(R.id.amount);
                    final TextView descriptionLabel = (TextView)v.findViewById(R.id.tag);
                    final TextView confirmationsLabel = (TextView)v.findViewById(R.id.confirmations);
                    final TextView dateLabel = (TextView)v.findViewById(R.id.date);
                    GradientDrawable amountButtonDrawable = (GradientDrawable) amountButton.getBackground();
                    GradientDrawable confirmationsLabelDrawable = (GradientDrawable) confirmationsLabel.getBackground();


                    assert amountButton != null;
                    amountButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            appDelegate.preferences.setDisplayLocalCurrency(!appDelegate.preferences.isDisplayLocalCurrency());
                            notifyDataSetChanged();
                            updateAccountBalance();
                        }
                    });

                    TLTxObject txObject = appDelegate.historySelectedObject.getTxObject(position);

                    Log.d("TransactionAdapter", "txObject hash " + txObject.getHash());
                    v.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            promptTransactionActionSheet(txObject.getHash());
                        }
                    });

                    dateLabel.setText(txObject.getTime());

                    String amount = appDelegate.currencyFormat.getProperAmount(appDelegate.historySelectedObject.getAccountAmountChangeForTx(txObject.getHash()));
                    TLWalletUtils.TLAccountTxType amountType = appDelegate.historySelectedObject.getAccountAmountChangeTypeForTx(txObject.getHash());
                    String txTag = appDelegate.appWallet.getTransactionTag(txObject.getHash());

                    String amountTypeString = "";
                    if (amountType == TLWalletUtils.TLAccountTxType.Send) {
                        amountTypeString = "-";
                        amountButtonDrawable.setColor(Color.RED);
                        if (txTag == null || txTag.length() == 0) {
                            JSONArray outputAddressToValueArray = txObject.getOutputAddressToValueArray();
                            for (int i = 0; i < outputAddressToValueArray.length(); i++) {
                                try {
                                    JSONObject dict = outputAddressToValueArray.getJSONObject(i);
                                    if (dict.has("addr")) {
                                        String address = dict.getString("addr");
                                        if (appDelegate.historySelectedObject.isAddressPartOfAccount(address)) {
                                            descriptionLabel.setText(address);
                                        } else {
                                            descriptionLabel.setText(address);
                                            break;
                                        }
                                    } else {
                                        descriptionLabel.setText("");
                                    }
                                } catch (JSONException e) {
                                }
                            }
                        } else {
                            descriptionLabel.setText(txTag);
                        }
                    } else if (amountType == TLWalletUtils.TLAccountTxType.Receive) {
                        amountTypeString = "+";
                        amountButtonDrawable.setColor(Color.GREEN);
                        if (txTag == null || txTag.length() == 0) {
                            descriptionLabel.setText("");
                        } else {
                            descriptionLabel.setText(txTag);
                        }

                    } else {
                        amountButtonDrawable.setColor(Color.GRAY);
                        if (txTag == null) {
                            descriptionLabel.setText(getString(R.string.internal_account_transfer));
                        } else {
                            descriptionLabel.setText(txTag);
                        }
                    }
                    amountButton.setText(amountTypeString+amount);

                    long confirmations = txObject.getConfirmations();

                    if (confirmations > MAX_CONFIRMATIONS_TO_DISPLAY) {
                        confirmationsLabel.setText(getString(R.string.amount_confirmations, confirmations)); // label is hidden
                        confirmationsLabelDrawable.setColor(Color.GREEN);
                        confirmationsLabel.setVisibility(View.INVISIBLE);
                    } else {
                        if (confirmations == 0) {
                            confirmationsLabelDrawable.setColor(Color.RED);
                        } else if (confirmations == 1) {
                            confirmationsLabelDrawable.setColor(0xFF8000); //orange
                        } else if (confirmations <= 2 && confirmations <= 5) {
                            confirmationsLabelDrawable.setColor(Color.YELLOW); //yellow color too light
//                            confirmationsLabelDrawable.setColor(Color.GREEN);
                        } else {
                            confirmationsLabelDrawable.setColor(Color.GREEN);
                        }

                        if (confirmations == 0) {
                            confirmationsLabel.setText(getString(R.string.unconfirmed));
                        } else if (confirmations == 1) {
                            confirmationsLabel.setText(getString(R.string.one_confirmation));
                        } else {
                            confirmationsLabel.setText(getString(R.string.amount_confirmations, confirmations));
                        }
                        confirmationsLabel.setVisibility(View.VISIBLE);
                    }

                }
            }

            return v;
        }

        @Override
        public int getCount() {
            return appDelegate.historySelectedObject.getTxObjectCount();
        }

        @Override
        public Item getItem(int position) {
            return new TransactionItem();
        }

        @Override
        public long getItemId(int position) {
            return position;
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
