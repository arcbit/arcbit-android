package com.arcbit.arcbit.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arcbit.arcbit.model.TLKeyStore;
import com.arcbit.arcbit.ui.utils.TLPageQRCodeFragment;
import com.arcbit.arcbit.ui.utils.TLPrompts;
import com.arcbit.arcbit.utils.TLRootUtil;
import com.viewpagerindicator.CirclePageIndicator;

import com.arcbit.arcbit.model.TLAccountObject;
import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLCallback;
import com.arcbit.arcbit.model.TLImportedAddress;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.model.TLOperationsManager;
import com.arcbit.arcbit.model.TLSendFormData;
import com.arcbit.arcbit.model.TLWalletUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.arcbit.arcbit.R;

public class ReceiveFragment extends android.support.v4.app.Fragment implements View.OnClickListener {

    private static final String TAG = ReceiveFragment.class.getName();
    private int SELECT_ACCOUNT_REQUEST_CODE = 1334;

    private View rootView;
    private TextView accountNameTextView;
    private TextView accountBalanceTextView;
    private CirclePageIndicator mIndicator;
    private ImageButton imageButtonSelectAccount;
    private RelativeLayout layoutAccount;
    private ImageButton sendNavButton;
    private ProgressBar balanceSpinner;
    private ViewPager viewPager;
    private LinearLayout dotsLinearLayout;

    public static List<String> receiveAddresses = new ArrayList<>(); // is static as workaround to getItem not called when notifyDataSetChanged called , should fix this
    private ReceivingAddressesAdapter receivingAddressesAdapter;
    private int MAX_PAGE = 7;

    private TLAppDelegate appDelegate = TLAppDelegate.instance();

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (appDelegate == null || appDelegate.godSend == null) {
                return;
            }
            String action = intent.getAction();
            if (action.equals(TLNotificationEvents.EVENT_PREFERENCES_BITCOIN_DISPLAY_CHANGED)) {
                updateAccountBalance();
            } else if (action.equals(TLNotificationEvents.EVENT_PREFERENCES_FIAT_DISPLAY_CHANGED)) {
                updateAccountBalance();
            } else if (action.equals(TLNotificationEvents.EVENT_UPDATED_RECEIVING_ADDRESSES)) {
                updateViewToNewSelectedObject();
            } else if (action.equals(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION)) {
                updateViewToNewSelectedObject();
            } else if (action.equals(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED)) {
                updateViewToNewSelectedObject();
            } else if (action.equals(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA)) {
                updateViewToNewSelectedObject();
            } else if (action.equals(TLNotificationEvents.EVENT_CLICKED_REFRESH_BALANCE)) {
                refreshSelectedAccount(true);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_receive, container, false);
        if (appDelegate == null) {
            return rootView;
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_UPDATED_RECEIVING_ADDRESSES));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_PREFERENCES_BITCOIN_DISPLAY_CHANGED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_PREFERENCES_FIAT_DISPLAY_CHANGED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_CLICKED_REFRESH_BALANCE));

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getActivity().setTitle(getString(R.string.receive));

        dotsLinearLayout = (LinearLayout)rootView.findViewById(R.id.ll_indicator);
        balanceSpinner = (ProgressBar)rootView.findViewById(R.id.balanceProgressBar);
        balanceSpinner.setVisibility(View.GONE);

        sendNavButton = (ImageButton)rootView.findViewById(R.id.but_send);
        sendNavButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                android.support.v4.app.Fragment fragment= new SendFragment();
                android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
            }
        });

        receivingAddressesAdapter = new ReceivingAddressesAdapter(getChildFragmentManager());
        viewPager = (ViewPager) rootView.findViewById(R.id.qr_pager);

        mIndicator = (CirclePageIndicator) rootView.findViewById(R.id.indicator);

        imageButtonSelectAccount = (ImageButton)rootView.findViewById(R.id.selectAccountArrow);
        imageButtonSelectAccount.setOnClickListener(this);

        layoutAccount = (RelativeLayout)rootView.findViewById(R.id.layoutAccount);
        layoutAccount.setOnClickListener(this);


        accountNameTextView = (TextView)rootView.findViewById(R.id.account_name);
        accountBalanceTextView = (TextView)rootView.findViewById(R.id.account_balance);

        if (!((MainActivity) getActivity()).initializedAppAndLoadedWallet) {
            ((MainActivity) getActivity()).initAppAndLoadWallet(new TLCallback() {

                @Override
                public void onSuccess(Object obj) {
                    appDelegate = TLAppDelegate.instance();
                    onFirstLoad();
                }

                @Override
                public void onFail(Integer status, String error) {
                }
            });
        } else {
            onFirstLoad();
        }

        return rootView;
    }

    public void onFirstLoad() {
        viewPager.setAdapter(receivingAddressesAdapter);
        mIndicator.setViewPager(viewPager);
        accountNameTextView.setText(appDelegate.receiveSelectedObject.getLabelForSelectedObject());
        String balance = appDelegate.currencyFormat.getProperAmount(appDelegate.receiveSelectedObject.getBalanceForSelectedObject());
        accountBalanceTextView.setText(balance);

        this.refreshSelectedAccount(false);
        LocalBroadcastManager.getInstance(this.appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_RECEIVE_SCREEN));

        if (TLRootUtil.getInstance().isDeviceRooted() && !appDelegate.suggestions.disabledShowIsRootedWarning())  {
            AlertDialog rootedAlertDialog;
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.device_rooted_description))
                    .setCancelable(false)
                    .setPositiveButton(R.string.continue_capitalize,
                            (d, id) -> {
                                d.dismiss();
                            });
            rootedAlertDialog = builder.create();
            rootedAlertDialog.show();
            appDelegate.suggestions.setDisabledShowIsRootedWarning(true);
        } else if (appDelegate.suggestions.conditionToPromptToSuggestedBackUpWalletPassphraseSatisfied()) {
            appDelegate.suggestions.promptToSuggestBackUpWalletPassphrase(getActivity());
        } else if (!appDelegate.suggestions.disabledShowLowAndroidVersionWarning() && !TLKeyStore.canUseKeyStore()) {
            appDelegate.suggestions.promptToShowLowAndroidVersionWarning(getActivity());
        }

        this.updateViewToNewSelectedObject();
        if (appDelegate.justSetupHDWallet) {
            appDelegate.justSetupHDWallet = false;
            TLPrompts.promptSuccessMessage(getActivity(), getString(R.string.welcome), getString(R.string.start_using_the_app_now));
        }
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
        if (((MainActivity)getActivity()).refreshBalanceMenuItem != null) {
            ((MainActivity)getActivity()).refreshBalanceMenuItem.setVisible(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupToolbar();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (((MainActivity)getActivity()).refreshBalanceMenuItem != null) {
            ((MainActivity)getActivity()).refreshBalanceMenuItem.setVisible(false);
        }
        if (appDelegate != null) {
            LocalBroadcastManager.getInstance(appDelegate.context).unregisterReceiver(receiver);
        }
    }

    void refreshSelectedAccount(boolean fetchDataAgain) {
        if (!appDelegate.receiveSelectedObject.hasFetchedCurrentFromData() || fetchDataAgain) {
            if (appDelegate.receiveSelectedObject.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Account) {
                TLAccountObject accountObject = (TLAccountObject) appDelegate.receiveSelectedObject.getSelectedObject();
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

            } else if (appDelegate.receiveSelectedObject.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Address) {
                TLImportedAddress importedAddress = (TLImportedAddress) appDelegate.receiveSelectedObject.getSelectedObject();
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
            String balance = appDelegate.currencyFormat.getProperAmount(appDelegate.receiveSelectedObject.getBalanceForSelectedObject());
            accountBalanceTextView.setText(balance);
            balanceSpinner.setVisibility(View.GONE);
        }
    }

    void updateReceiveAddressArray() {
        int receivingAddressesCount = appDelegate.receiveSelectedObject.getReceivingAddressesCount();
        receiveAddresses = new ArrayList<>(receivingAddressesCount);
        for (int i = 0; i < receivingAddressesCount; i++) {
            String address = appDelegate.receiveSelectedObject.getReceivingAddressForSelectedObject(i);
            receiveAddresses.add(address);
        }
        if (TLWalletUtils.ENABLE_STEALTH_ADDRESS()) {
            if (appDelegate.receiveSelectedObject.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Account) {
                String stealthAddress = appDelegate.receiveSelectedObject.getStealthAddress();
                if (stealthAddress != null) {
                    if (appDelegate.preferences.enabledStealthAddressDefault()) {
                        receiveAddresses.add(0, stealthAddress);
                    } else {
                        receiveAddresses.add(stealthAddress);
                    }
                }
            }
        }
    }

    void updateViewToNewSelectedObject() {
        this.updateAccountBalance();
        int receivingAddressesCount = appDelegate.receiveSelectedObject.getReceivingAddressesCount();
        if (receivingAddressesCount == 0) {
            return;
        }
        String label = appDelegate.receiveSelectedObject.getLabelForSelectedObject();
        accountNameTextView.setText(label);

        this.updateReceiveAddressArray();
        if (receivingAddressesAdapter != null) {
            receivingAddressesAdapter.notifyDataSetChanged();
        }
        if (appDelegate.receiveSelectedObject.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Account) {
            dotsLinearLayout.setVisibility(View.VISIBLE);
        } else {
            dotsLinearLayout.setVisibility(View.INVISIBLE);
        }
        //viewPager.setCurrentItem(0); //should I enabled this?
    }

    void updateAccountBalance() {
        String balance = appDelegate.currencyFormat.getProperAmount(appDelegate.receiveSelectedObject.getBalanceForSelectedObject());
        if (appDelegate.receiveSelectedObject.getDownloadState() == TLOperationsManager.TLDownloadState.Downloaded) {
            balanceSpinner.setVisibility(View.GONE);
            accountBalanceTextView.setText(balance);
            accountBalanceTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SELECT_ACCOUNT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                Integer selectAccountType = data.getIntExtra("SELECT_ACCOUNT_TYPE", 0);
                Integer selectAccountIdx = data.getIntExtra("SELECT_ACCOUNT_IDX", 0);
                // clear cache bitmap on account change to avoid low memory device from crashing
                // FIXME downside is on selection of new account, there is some ui lag since qr code bitmaps needs to be loaded
                Object oldSelectedObject = appDelegate.receiveSelectedObject.getSelectedObject();
                appDelegate.updateReceiveSelectedObject(TLWalletUtils.TLSendFromType.getSendFromType(selectAccountType), selectAccountIdx);
                if (appDelegate.receiveSelectedObject.getSelectedObject() != oldSelectedObject) {
                    appDelegate.address2BitmapMap = new HashMap<String, Bitmap>();
                }
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
                ((MainActivity)getActivity()).refreshBalanceMenuItem.setVisible(false);
                break;

            default:
                break;
        }
    }

    private class ReceivingAddressesAdapter extends android.support.v4.app.FragmentPagerAdapter {
        public ReceivingAddressesAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        //FIXME: getItemPosition is inefficient and can cause out of memory errors on low end devices,
        // FIXME: but needed it to fix issue where on first launch qr code does not show, can try below link out
        // FIXME: http://stackoverflow.com/questions/7263291/viewpager-pageradapter-not-updating-the-view/7287121#7287121
        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if(position < 0 || MAX_PAGE <= position) {
                return null;
            }
            TLPageQRCodeFragment qrCodePageFragment = new TLPageQRCodeFragment();
            Bundle args = new Bundle();
            args.putInt("idx", position);
            qrCodePageFragment.setArguments(args);
            return qrCodePageFragment;
        }

        @Override
        public int getCount() {
            if (appDelegate.receiveSelectedObject.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Account) {
                return MAX_PAGE;
            } else {
                return 1;
            }
        }
    }
}
