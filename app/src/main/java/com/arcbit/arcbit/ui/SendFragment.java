package com.arcbit.arcbit.ui;

import com.arcbit.arcbit.model.CreatedTransactionObject;
import com.arcbit.arcbit.model.TLColdWallet;
import com.arcbit.arcbit.model.TLSpaghettiGodSend;
import com.arcbit.arcbit.ui.utils.TLToast;
import com.arcbit.arcbit.utils.TLUtils;
import com.arcbit.arcbit.ui.utils.TLHUDWrapper;
import com.arcbit.arcbit.ui.utils.TLPrompts;
import com.google.zxing.client.android.CaptureActivity;

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
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arcbit.arcbit.model.TLAccountObject;
import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLBitcoinjWrapper;
import com.arcbit.arcbit.APIs.TLBlockExplorerAPI;
import com.arcbit.arcbit.model.TLCallback;
import com.arcbit.arcbit.model.TLCoin;
import com.arcbit.arcbit.model.TLHDWalletWrapper;
import com.arcbit.arcbit.model.TLImportedAddress;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.model.TLOperationsManager;
import com.arcbit.arcbit.model.TLSendFormData;
import com.arcbit.arcbit.model.TLStealthAddress;
import com.arcbit.arcbit.model.TLTransactionFee;
import com.arcbit.arcbit.model.TLWalletUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.arcbit.arcbit.utils.TLPermissionUtil;
import com.arcbit.arcbit.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SendFragment extends android.support.v4.app.Fragment implements View.OnClickListener {
    private static final String TAG = SendFragment.class.getName();

    private View rootView;
    private int SELECT_ACCOUNT_REQUEST_CODE = 1334;
    private int SELECT_TO_ADDRESS_REQUEST_CODE = 1333;
    private static final int SCAN_URI = 1332;
    private static final int SCAN_PRIVATE_KEY = 1331;
    private static final int SCAN_EXTENDED_PRIVATE_KEY = 1330;
    private static final int SCAN_SIGNED_TX = 1329;

    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    private ImageButton imageButtonSelectAccount;
    private RelativeLayout layoutAccount;
    private Button scanQRButton;
    private Button contactsButton;
    private Button reviewPaymentButton;
    private TextView accountNameTextView;
    private TextView accountBalanceTextView;
    private EditText amountEditText;
    private EditText fiatAmountEditText;
    private EditText toAddressEditText;
    private TextView fiatCurrencyDisplayTextView;
    private TextView bitcoinDisplayTextView;
    private TextWatcher amountEditTextTextWatcher;
    private TextWatcher fiatAmountEditTextTextWatcher;
    private ImageButton receiveNavButton;
    static boolean allowUpdateToAmount;

    private View reviewPaymentDialogView;
    private AlertDialog reviewPaymentAlertDialog;
    private Set showedPromptedForSentPaymentTxHashSet = new HashSet();
    private String sendTxHash;
    private String inputtedToAddress;
    private TLCoin inputtedToAmount;
    private TLCoin amountMovedFromAccount;


    private List<String> airGapDataBase64PartsArray;
    private Map<Integer, String> scannedSignedTxAirGapDataPartsDict = new HashMap<>();
    private Integer totalExpectedParts = 0;
    private String scannedSignedTxAirGapData = null;

    private String signedAirGapTxHex = null;
    private String signedAirGapTxHash = null;

    private List<String> realToAddresses = null;

    private ProgressBar balanceSpinner;
    private final Handler sendHandler = new Handler();
    private final Runnable sendRunnable = new Runnable() {
        @Override
        public void run() {
            retryFinishSend();
        }
    };

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (appDelegate == null || appDelegate.godSend == null) {
                return;
            }
            String action = intent.getAction();
            if (action.equals(TLNotificationEvents.EVENT_PREFERENCES_BITCOIN_DISPLAY_CHANGED)) {
                clearSendForm();
                updateBitcoinDisplayView();
                updateAccountBalanceView();
            } else if (action.equals(TLNotificationEvents.EVENT_PREFERENCES_FIAT_DISPLAY_CHANGED)) {
                clearSendForm();
                updateCurrencyView();
                updateAccountBalanceView();
            } else if (action.equals(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED)) {
                updateAccountBalanceView();
            } else if (action.equals(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA)) {
                updateAccountBalanceView();
            } else if (action.equals(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION)) {
                hideHUDAndUpdateBalanceView();
                String txHash = intent.getStringExtra("txHash");
                finishSend(txHash);
            } else if (action.equals(TLNotificationEvents.EVENT_CLICKED_USE_ALL_FUNDS)) {
                checkToFetchUTXOsAndDynamicFeesAndFillAmountFieldWithWholeBalance();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_send, container, false);
        if (appDelegate == null) {
            return rootView;
        }

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getActivity().setTitle(getString(R.string.send));

        balanceSpinner = (ProgressBar) rootView.findViewById(R.id.balanceProgressBar);
        balanceSpinner.setVisibility(View.GONE);

        receiveNavButton = (ImageButton) rootView.findViewById(R.id.but_receive);
        receiveNavButton.setOnClickListener(this);
        receiveNavButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.support.v4.app.Fragment fragment = new ReceiveFragment();
                android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
            }
        });


        accountNameTextView = (TextView) rootView.findViewById(R.id.account_name);
        accountBalanceTextView = (TextView) rootView.findViewById(R.id.account_balance);
        amountEditText = (EditText) rootView.findViewById(R.id.txt_amountBTC);
        fiatAmountEditText = (EditText) rootView.findViewById(R.id.txt_amountUSD);
        toAddressEditText = (EditText) rootView.findViewById(R.id.txt_address);
        fiatCurrencyDisplayTextView = (TextView) rootView.findViewById(R.id.fiat_display_unit);
        bitcoinDisplayTextView = (TextView) rootView.findViewById(R.id.display_unit);

        toAddressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (appDelegate == null || appDelegate.sendFormData == null) {
                    return;
                }
                appDelegate.sendFormData.setAddress(s.toString());
            }
        });

        amountEditTextTextWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                str = str.replace(',', '.');
                if (str.length() == 1 && str.equals(".")) {
                    str = "0.";
                }
                updateFiatAmountTextFieldExchangeRate(str);
                appDelegate.sendFormData.setAmount(str);
                appDelegate.sendFormData.setFiatAmount(null);
                appDelegate.sendFormData.useAllFunds = false;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                preFetchUTXOsAndDynamicFees();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };

        fiatAmountEditTextTextWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                str = str.replace(',', '.');
                if (str.length() == 1 && str.equals(".")) {
                    str = "0.";
                }

                if (allowUpdateToAmount) {
                    updateAmountTextFieldExchangeRate(str);
                    appDelegate.sendFormData.setFiatAmount(str);
                    appDelegate.sendFormData.setAmount(null);
                    appDelegate.sendFormData.useAllFunds = false;
                } else {
                    allowUpdateToAmount = true;
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                preFetchUTXOsAndDynamicFees();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        fiatAmountEditText.addTextChangedListener(fiatAmountEditTextTextWatcher);
        amountEditText.addTextChangedListener(amountEditTextTextWatcher);

        amountEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (((MainActivity) getActivity()).useAllFundsMenuItem == null) {
                    return;
                }
                if (hasFocus) {
                    ((MainActivity) getActivity()).useAllFundsMenuItem.setVisible(true);
                } else {
                    ((MainActivity) getActivity()).useAllFundsMenuItem.setVisible(false);
                }
            }
        });

        fiatAmountEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (((MainActivity) getActivity()).useAllFundsMenuItem == null) {
                    return;
                }
                if (hasFocus) {
                    ((MainActivity) getActivity()).useAllFundsMenuItem.setVisible(true);
                } else {
                    ((MainActivity) getActivity()).useAllFundsMenuItem.setVisible(false);
                }
            }
        });

        this.setUpReviewPaymentView();

        imageButtonSelectAccount = (ImageButton) rootView.findViewById(R.id.selectAccountArrow);
        imageButtonSelectAccount.setOnClickListener(this);

        layoutAccount = (RelativeLayout) rootView.findViewById(R.id.layoutAccount);
        layoutAccount.setOnClickListener(this);

        contactsButton = (Button) rootView.findViewById(R.id.butContacts);
        contactsButton.setOnClickListener(this);
        scanQRButton = (Button) rootView.findViewById(R.id.butScanQR);
        scanQRButton.setOnClickListener(this);
        reviewPaymentButton = (Button) rootView.findViewById(R.id.butReviewPayment);
        reviewPaymentButton.setOnClickListener(this);


        accountNameTextView = (TextView) rootView.findViewById(R.id.account_name);
        accountBalanceTextView = (TextView) rootView.findViewById(R.id.account_balance);

        setupUIToHideKeyBoardOnTouch(rootView);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_PREFERENCES_BITCOIN_DISPLAY_CHANGED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_PREFERENCES_FIAT_DISPLAY_CHANGED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_DISPLAY_LOCAL_CURRENCY_TOGGLED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_FETCHED_ADDRESSES_DATA));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_MODEL_UPDATED_NEW_UNCONFIRMED_TRANSACTION));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_CLICKED_USE_ALL_FUNDS));


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
        accountNameTextView.setText(appDelegate.godSend.getCurrentFromLabel());
        String balance = appDelegate.currencyFormat.getProperAmount(appDelegate.godSend.getCurrentFromBalance());
        accountBalanceTextView.setText(balance);

        this.updateSendForm();
        this.sendViewSetup();

        this.updateViewToNewSelectedObject();

        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_VIEW_SEND_SCREEN));

        if (!appDelegate.preferences.isEnablePINCode() && appDelegate.suggestions.conditionToPromptToSuggestEnablePinSatisfied()) {
            appDelegate.suggestions.promptToSuggestEnablePin(getActivity());
        } else if (appDelegate.suggestions.conditionToPromptRateAppSatisfied()) {
            TLPrompts.promptYesNo(getActivity(), getString(R.string.like_using_arcbit),
                    getString(R.string.rate_app), getString(R.string.rate), getString(R.string.remind_me_later), new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            String url = "market://details?id=" + getActivity().getPackageName(); // getPackageName = com.arcbit.android
                            url = "https://itunes.apple.com/app/id999487888"; // TODO remove dummy url
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                            appDelegate.preferences.setDisabledPromptRateApp(true);
                            if (!appDelegate.preferences.hasRatedOnce()) {
                                appDelegate.preferences.setHasRatedOnce();
                            }
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
        } else if (appDelegate.suggestions.conditionToPromptShowWebWallet()) {
            TLPrompts.promptYesNo(getActivity(), getString(R.string.checkout_the_arcbit_web_wallet),
                    getString(R.string.checkout_the_arcbit_web_wallet_description), getString(R.string.ok_capitalize),
                    getString(R.string.not_now), new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            String url = "https://chrome.google.com/webstore/detail/arcbit-bitcoin-wallet/dkceiphcnbfahjbomhpdgjmphnpgogfk";
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                            appDelegate.preferences.setDisabledPromptShowWebWallet(true);
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
        } else if (appDelegate.suggestions.conditionToPromptShowTryColdWallet()) {
            appDelegate.preferences.setEnableColdWallet(true);
            TLPrompts.promptForOK(getActivity(), getString(R.string.try_our_new_cold_wallet_feature),
                    getString(R.string.try_our_new_cold_wallet_feature_description), new TLPrompts.PromptOKCallback() {
                        @Override
                        public void onSuccess() {
                            appDelegate.preferences.setDisabledPromptShowTryColdWallet(true);
                        }
                    });
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arcbit_menu_white));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
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

    public void setupUIToHideKeyBoardOnTouch(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideKeyBoard();
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUIToHideKeyBoardOnTouch(innerView);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String value = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            handleScannedAddress(value);
        } else if (resultCode == Activity.RESULT_OK && requestCode == SCAN_PRIVATE_KEY
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String value = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            checkKeyAndHandleTemporaryImportPrivateKey(value);
        } else if (resultCode == Activity.RESULT_OK && requestCode == SCAN_EXTENDED_PRIVATE_KEY
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String value = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            temporaryImportExtendedPrivateKey(value);
        } else if (resultCode == Activity.RESULT_OK && requestCode == SCAN_SIGNED_TX
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {

            String value = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            List<Object> ret = TLColdWallet.parseScannedPart(value);
            String dataPart = (String) ret.get(0);
            Integer partNumber = (Integer) ret.get(1);
            Integer totalParts = (Integer) ret.get(2);

            this.totalExpectedParts = totalParts;
            this.scannedSignedTxAirGapDataPartsDict.put(partNumber, dataPart);
            didScanSignedTxButton();
        } else if (requestCode == SELECT_TO_ADDRESS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String value = data.getStringExtra("ADDRESS");
                if (value != null) {
                    appDelegate.sendFormData.setAddress(value);
                    toAddressEditText.setText(value);
                }
            }
        } else if (requestCode == SELECT_ACCOUNT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Integer selectAccountType = data.getIntExtra("SELECT_ACCOUNT_TYPE", 0);
                Integer selectAccountIdx = data.getIntExtra("SELECT_ACCOUNT_IDX", 0);
                appDelegate.updateGodSend(TLWalletUtils.TLSendFromType.getSendFromType(selectAccountType), selectAccountIdx);
                updateViewToNewSelectedObject();
            }
        }
    }

    void hideKeyBoard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.selectAccountArrow:
            case R.id.layoutAccount:
                hideKeyBoard();
                allowUpdateToAmount = false;
                android.support.v4.app.Fragment fragmentChild = new SelectAccountFragment();
                fragmentChild.setTargetFragment(this, SELECT_ACCOUNT_REQUEST_CODE);
                android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.right_slide_in, R.anim.left_slide_out, R.anim.left_slide_in, R.anim.right_slide_out);
                transaction.replace(R.id.fragment_container, fragmentChild);
                transaction.addToBackStack(null);
                transaction.commit();
                break;

            case R.id.butReviewPayment:
                reviewPaymentClicked();
                break;

            case R.id.butScanQR:
                scanQRCode(SCAN_URI);
                break;

            case R.id.butContacts:
                startActivityForResult(new Intent(getActivity(), ContactsActivity.class), SELECT_TO_ADDRESS_REQUEST_CODE);
                break;

            default:
                break;
        }
    }

    private void setAmountFromUrlHandler(TLCoin amount, String address) {
        this.toAddressEditText.setText(address);
        String amountString = appDelegate.currencyFormat.coinToProperBitcoinAmountString(amount);
        this.setAmountEditTextWrapper(amountString);

        appDelegate.sendFormData.setAddress(address);
        appDelegate.sendFormData.setAmount(amountString);

        this.updateFiatAmountTextFieldExchangeRate();
    }

    void checkToFetchUTXOsAndDynamicFeesAndFillAmountFieldWithWholeBalance() {
        TLCoin accountBalance = appDelegate.godSend.getCurrentFromBalance();
        if (accountBalance.lessOrEqual(TLCoin.zero())) {
            return;
        }
        if (appDelegate.preferences.enabledDynamicFee()) {
            if (!appDelegate.godSend.haveUpDatedUTXOs()) {
                appDelegate.godSend.getAndSetUnspentOutputs(new TLCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        checkToFetchDynamicFeesAndFillAmountFieldWithWholeBalance();
                    }

                    @Override
                    public void onFail(Integer status, String error) {
                        TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.error_fetching_unspent_outputs_try_again_later));
                    }
                });
            } else {
                this.checkToFetchDynamicFeesAndFillAmountFieldWithWholeBalance();
            }
        } else {
            this.fillAmountFieldWithWholeBalance(false);
        }
    }

    void checkToFetchDynamicFeesAndFillAmountFieldWithWholeBalance() {
        if (!appDelegate.txFeeAPI.haveUpdatedCachedDynamicFees()) {
            appDelegate.txFeeAPI.getDynamicTxFee(new TLCallback() {
                @Override
                public void onSuccess(Object obj) {
                    fillAmountFieldWithWholeBalance(true);
                }

                @Override
                public void onFail(Integer status, String error) {
                    TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.unable_to_get_dynamic_fees));
                    fillAmountFieldWithWholeBalance(false);
                }
            });
        } else {
            this.fillAmountFieldWithWholeBalance(true);
        }
    }

    void fillAmountFieldWithWholeBalance(boolean useDynamicFees) {
        TLCoin fee;
        int txSizeBytes = 0;
        if (useDynamicFees) {
            if (appDelegate.godSend.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Account) {
                TLAccountObject accountObject = (TLAccountObject) appDelegate.godSend.getSelectedSendObject();
                int inputCount = accountObject.stealthPaymentUnspentOutputsCount + accountObject.unspentOutputsCount;
                txSizeBytes = appDelegate.godSend.getEstimatedTxSize(inputCount, 1);
            } else if (appDelegate.godSend.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Address) {
                TLImportedAddress importedAddress = (TLImportedAddress) appDelegate.godSend.getSelectedSendObject();
                txSizeBytes = appDelegate.godSend.getEstimatedTxSize(importedAddress.unspentOutputsCount, 1);
            }
            Long dynamicFeeSatoshis = appDelegate.txFeeAPI.getCachedDynamicFee();
            if (dynamicFeeSatoshis != null) {
                fee = new TLCoin(txSizeBytes * dynamicFeeSatoshis);
            } else {
                fee = new TLCoin(appDelegate.preferences.getTransactionFee());
            }
        } else {
            fee = new TLCoin(appDelegate.preferences.getTransactionFee());
        }


        TLCoin accountBalance = appDelegate.godSend.getCurrentFromBalance();
        TLCoin sendAmount = accountBalance.subtract(fee);
        appDelegate.sendFormData.feeAmount = fee;
        appDelegate.sendFormData.useAllFunds = true;
        if (sendAmount.greater(TLCoin.zero())) {
            appDelegate.sendFormData.setAmount(appDelegate.currencyFormat.coinToProperBitcoinAmountString(sendAmount));
        } else {
            appDelegate.sendFormData.setAmount(null);
        }
        appDelegate.sendFormData.setFiatAmount(null);
        this.updateSendForm();
    }

    private void sendViewSetup() {
        this.updateCurrencyView();
        this.updateBitcoinDisplayView();

        this.updateViewToNewSelectedObject();

        if (appDelegate.godSend.hasFetchedCurrentFromData()) {
            TLCoin balance = appDelegate.godSend.getCurrentFromBalance();
            String balanceString = appDelegate.currencyFormat.getProperAmount(balance);
            accountBalanceTextView.setText(balanceString);
            accountBalanceTextView.setVisibility(View.VISIBLE);
            balanceSpinner.setVisibility(View.GONE);
        } else {
            this.refreshAccountDataAndSetBalanceView();
        }
    }

    void refreshAccountDataAndSetBalanceView() {
        refreshAccountDataAndSetBalanceView(false);
    }

    void refreshAccountDataAndSetBalanceView(boolean fetchDataAgain) {

        if (appDelegate.godSend.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Account) {
            TLAccountObject accountObject = (TLAccountObject) appDelegate.godSend.getSelectedSendObject();
            this.accountBalanceTextView.setVisibility(View.INVISIBLE);
            balanceSpinner.setVisibility(View.VISIBLE);
            accountObject.getAccountDataO(fetchDataAgain, new TLCallback() {
                @Override
                public void onSuccess(Object obj) {
                    if (accountObject.downloadState == TLOperationsManager.TLDownloadState.Downloaded) {
                        accountBalanceTextView.setVisibility(View.VISIBLE);
                        balanceSpinner.setVisibility(View.GONE);
                        updateAccountBalanceView();
                    }
                }

                @Override
                public void onFail(Integer status, String error) {
                }
            });
        } else if (appDelegate.godSend.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Address) {
            TLImportedAddress importedAddress = (TLImportedAddress) appDelegate.godSend.getSelectedSendObject();
            this.accountBalanceTextView.setVisibility(View.INVISIBLE);
            balanceSpinner.setVisibility(View.VISIBLE);
            importedAddress.getSingleAddressDataO(fetchDataAgain, new TLCallback() {
                @Override
                public void onSuccess(Object obj) {
                    if (importedAddress.downloadState == TLOperationsManager.TLDownloadState.Downloaded) {
                        accountBalanceTextView.setVisibility(View.VISIBLE);
                        balanceSpinner.setVisibility(View.GONE);
                        updateAccountBalanceView();
                    }
                }

                @Override
                public void onFail(Integer status, String error) {
                }
            });
        }
    }

    private void updateViewToNewSelectedObject() {
        accountNameTextView.setText(appDelegate.godSend.getCurrentFromLabel());
        this.updateAccountBalanceView();
    }

    void clearSendForm() {
        appDelegate.sendFormData.setAddress(null);
        appDelegate.sendFormData.setAmount(null);
        this.updateSendForm();
    }

    private void updateSendForm() {
        this.toAddressEditText.setText(appDelegate.sendFormData.getAddress());
        if (appDelegate.sendFormData.getAmount() != null) {
            this.setAmountEditTextWrapper(appDelegate.sendFormData.getAmount());
            this.updateFiatAmountTextFieldExchangeRate();
        } else if (appDelegate.sendFormData.getFiatAmount() != null) {
            this.setFiatAmountEditTextWrapper(appDelegate.sendFormData.getFiatAmount());
            this.updateAmountTextFieldExchangeRate();
        }
    }

    void updateCurrencyView() {
        String currency = appDelegate.currencyFormat.getFiatCurrency();
        this.fiatCurrencyDisplayTextView.setText(currency);
        this.updateSendForm();
    }

    void updateBitcoinDisplayView() {
        String bitcoinDisplay = appDelegate.currencyFormat.getBitcoinDisplay();
        bitcoinDisplayTextView.setText(bitcoinDisplay);
        this.updateSendForm();
        this.updateFiatAmountTextFieldExchangeRate();
    }

    void hideHUDAndUpdateBalanceView() {
        this.accountBalanceTextView.setVisibility(View.VISIBLE);
        balanceSpinner.setVisibility(View.GONE);
        this.updateAccountBalanceView();
    }

    void updateAccountBalanceView() {
        TLCoin balance = appDelegate.godSend.getCurrentFromBalance();
        String balanceString = appDelegate.currencyFormat.getProperAmount(balance);
        accountBalanceTextView.setText(balanceString);
    }

    private boolean fillToAddressTextField(String address) {
        if (TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
            this.toAddressEditText.setText(address);
            appDelegate.sendFormData.setAddress(address);
            return true;
        } else {
            TLToast.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
            return false;
        }
    }

    private void checkTofetchFeeThenFinalPromptReviewTx() {
        if (appDelegate.preferences.enabledDynamicFee() && !appDelegate.txFeeAPI.haveUpdatedCachedDynamicFees()) {
            appDelegate.txFeeAPI.getDynamicTxFee(new TLCallback() {
                @Override
                public void onSuccess(Object obj) {
                    showFinalPromptReviewTx();
                }

                @Override
                public void onFail(Integer status, String error) {
                    showFinalPromptReviewTx();
                }
            });
        } else {
            this.showFinalPromptReviewTx();
        }
    }

    void showReviewPaymentView(TLCoin inputtedAmount, boolean useDynamicFees) {
        TLCoin fee;
        int txSizeBytes;
        if (useDynamicFees) {
            if (appDelegate.sendFormData.useAllFunds) {
                fee = appDelegate.sendFormData.feeAmount;
            } else {
                if (appDelegate.godSend.getSelectedObjectType() == TLSendFormData.TLSelectObjectType.Account) {
                    TLAccountObject accountObject = (TLAccountObject) appDelegate.godSend.getSelectedSendObject();
                    int inputCount = accountObject.getInputsNeededToConsume(inputtedAmount);
                    //FIXME account for change output, output count likely 2 (3 if have stealth payment) cause if user dont do click use all funds because will likely have change
                    // but for now dont need to be fully accurate with tx fee, for now we will underestimate tx fee, wont underestimate much because outputs contributes little to tx size
                    txSizeBytes = appDelegate.godSend.getEstimatedTxSize(inputCount, 1);
                    Log.d(TAG, "showPromptReviewTx TLAccountObject useDynamicFees inputCount txSizeBytes: " + inputCount + " " + txSizeBytes);
                } else {
                    TLImportedAddress importedAddress = (TLImportedAddress) appDelegate.godSend.getSelectedSendObject();
                    //FIXME account for change output, output count likely 2 (3 if have stealth payment) cause if user dont do click use all funds because will likely have change
                    int inputCount = importedAddress.getInputsNeededToConsume(inputtedAmount);
                    txSizeBytes = appDelegate.godSend.getEstimatedTxSize(inputCount, 1);
                    Log.d(TAG, "showPromptReviewTx importedAddress useDynamicFees inputCount txSizeBytes: " + importedAddress.unspentOutputsCount + " " + txSizeBytes);
                }

                Long dynamicFeeSatoshis = appDelegate.txFeeAPI.getCachedDynamicFee();
                if (dynamicFeeSatoshis != null) {
                    fee = new TLCoin(txSizeBytes * dynamicFeeSatoshis);
                    Log.d(TAG, "showPromptReviewTx coinFeeAmount dynamicFeeSatoshis: " + txSizeBytes * dynamicFeeSatoshis);

                } else {
                    fee = new TLCoin(appDelegate.preferences.getTransactionFee());
                }
                appDelegate.sendFormData.feeAmount = fee;
            }
        } else {
            fee = new TLCoin(appDelegate.preferences.getTransactionFee());
            appDelegate.sendFormData.feeAmount = fee;
        }

        TLCoin amountNeeded = inputtedAmount.add(fee);
        TLCoin sendFromBalance = appDelegate.godSend.getCurrentFromBalance();
        if (amountNeeded.greater(sendFromBalance)) {
            String msg = getString(R.string.more_funds_needed, appDelegate.currencyFormat.coinToProperBitcoinAmountString(sendFromBalance, true), appDelegate.currencyFormat.coinToProperBitcoinAmountString(amountNeeded, true));
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.insufficient_funds), msg);
            return;
        }

        Log.d(TAG, "showPromptReviewTx accountBalance: " + sendFromBalance.toNumber());
        Log.d(TAG, "showPromptReviewTx inputtedAmount: " + inputtedAmount.toNumber());
        Log.d(TAG, "showPromptReviewTx fee: " + fee.toNumber());
        appDelegate.sendFormData.fromLabel = appDelegate.godSend.getCurrentFromLabel();
        confirmPayment();
    }

    void checkToFetchDynamicFees(TLCoin inputtedAmount) {
        if (!appDelegate.txFeeAPI.haveUpdatedCachedDynamicFees()) {
            appDelegate.txFeeAPI.getDynamicTxFee(new TLCallback() {
                @Override
                public void onSuccess(Object obj) {
                    showReviewPaymentView(inputtedAmount, true);
                }

                @Override
                public void onFail(Integer status, String error) {
                    TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.unable_to_get_dynamic_fees));
                    showReviewPaymentView(inputtedAmount, false);
                }
            });
        } else {
            showReviewPaymentView(inputtedAmount, true);
        }
    }

    private void showFinalPromptReviewTx() {
        String bitcoinAmount = this.amountEditText.getText().toString();
        String toAddress = this.toAddressEditText.getText().toString();

        if (!TLBitcoinjWrapper.isValidAddress(toAddress, appDelegate.appWallet.walletConfig.isTestnet)) {
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.invalid_bitcoin_address));
            return;
        }

        TLCoin inputtedAmount = appDelegate.currencyFormat.properBitcoinAmountStringToCoin(bitcoinAmount);

        if (inputtedAmount.equalTo(TLCoin.zero())) {
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.invalid_amount));
            return;
        }

        if (appDelegate.preferences.enabledDynamicFee()) {
            if (!appDelegate.godSend.haveUpDatedUTXOs()) {
                appDelegate.godSend.getAndSetUnspentOutputs(new TLCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        checkToFetchDynamicFees(inputtedAmount);
                    }

                    @Override
                    public void onFail(Integer status, String error) {
                        TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.error_fetching_unspent_outputs_try_again_later));
                    }
                });
            } else {
                this.checkToFetchDynamicFees(inputtedAmount);
            }
        } else {
            showReviewPaymentView(inputtedAmount, false);
        }
    }

    private void checkKeyAndHandleTemporaryImportPrivateKey(String privateKey) {
        if (TLBitcoinjWrapper.isBIP38EncryptedKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
            TLPrompts.promptForEncryptedPrivKeyPassword(getActivity(), privateKey, appDelegate.appWallet.walletConfig.isTestnet, new TLPrompts.PromptCallback() {
                @Override
                public void onSuccess(Object obj) {
                    handletemporaryImportPrivateKey((String) obj);
                }

                @Override
                public void onCancel() {
                }
            });
        } else {
            handletemporaryImportPrivateKey(privateKey);
        }
    }

    private void handletemporaryImportPrivateKey(String privateKey) {
        if (!TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.invalid_private_key));
        } else {
            TLImportedAddress importedAddress = (TLImportedAddress) appDelegate.godSend.getSelectedSendObject();
            boolean success = importedAddress.setPrivateKeyInMemory(privateKey);
            if (!success) {
                TLPrompts.promptSuccessMessage(getActivity(), getString(R.string.error), getString(R.string.private_key_does_not_match_address));
            } else {
                this.checkTofetchFeeThenFinalPromptReviewTx();
            }
        }
    }

    private void showPromptReviewTx() {
        if (appDelegate.godSend.needWatchOnlyAccountPrivateKey()) {
            TLPrompts.promptForOKCancel(getActivity(), getString(R.string.account_private_key_missing),
                    getString(R.string.ask_import_your_account_private_key), new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            CharSequence[] otherButtonTitles = {getString(R.string.import_with_qr_code), getString(R.string.import_with_text_input)};
                            new AlertDialog.Builder(getActivity())
                                    .setItems(otherButtonTitles, (dialog, which) -> {
                                                if (which == 0) {
                                                    scanQRCode(SCAN_EXTENDED_PRIVATE_KEY);
                                                } else if (which == 1) {
                                                    TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.temporary_import_account_private_key), "", "", InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                                        @Override
                                                        public void onSuccess(Object obj) {
                                                            temporaryImportExtendedPrivateKey((String) obj);
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
        } else if (appDelegate.godSend.needWatchOnlyAddressPrivateKey()) {
            TLPrompts.promptForOKCancel(getActivity(), getString(R.string.private_key_missing), getString(R.string.ask_import_your_private_key),
                    new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            CharSequence[] otherButtonTitles = {getString(R.string.import_with_qr_code), getString(R.string.import_with_text_input)};
                            new AlertDialog.Builder(getActivity())
                                    .setItems(otherButtonTitles, (dialog, which) -> {
                                                if (which == 0) {
                                                    scanQRCode(SCAN_PRIVATE_KEY);
                                                } else if (which == 1) {
                                                    TLPrompts.promptForInput(getActivity(),
                                                            getString(R.string.temporary_import_private_key),
                                                            "", "", getString(R.string.ok_capitalize),
                                                            getString(R.string.cancel), InputType.TYPE_CLASS_TEXT,
                                                            new TLPrompts.PromptCallback() {
                                                                @Override
                                                                public void onSuccess(Object obj) {
                                                                    checkKeyAndHandleTemporaryImportPrivateKey((String) obj);
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
        } else if (appDelegate.godSend.needEncryptedPrivateKeyPassword()) {
            String encryptedPrivateKey = appDelegate.godSend.getEncryptedPrivateKey();
            TLPrompts.promptForEncryptedPrivKeyPassword(getActivity(), encryptedPrivateKey, appDelegate.appWallet.walletConfig.isTestnet, new TLPrompts.PromptCallback() {
                @Override
                public void onSuccess(Object obj) {
                    TLImportedAddress importedAddress = (TLImportedAddress) appDelegate.godSend.getSelectedSendObject();
                    boolean success = importedAddress.setPrivateKeyInMemory((String) obj);
                    if (!success) {
                        TLPrompts.promptSuccessMessage(getActivity(), getString(R.string.error), getString(R.string.private_key_does_not_match_address));
                    } else {
                        checkTofetchFeeThenFinalPromptReviewTx();
                    }
                }

                @Override
                public void onCancel() {
                }
            });
        } else {
            this.checkTofetchFeeThenFinalPromptReviewTx();
        }
    }

    private void temporaryImportExtendedPrivateKey(String privateKey) {
        if (!TLHDWalletWrapper.isValidExtendedPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.invalid_account_private_key));
        } else {
            TLAccountObject accountObject = (TLAccountObject) appDelegate.godSend.getSelectedSendObject();
            boolean success = accountObject.setExtendedPrivateKeyInMemory(privateKey);
            if (!success) {
                TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.account_private_key_not_match_imported_account_public_key));
            } else {
                checkTofetchFeeThenFinalPromptReviewTx();
            }
        }
    }

    private void handleScannedAddress(String data) {
        if (data.startsWith("bitcoin:")) {
            Pair<String, Long> parsedBitcoinURI = TLBitcoinjWrapper.getAddressAndAmountFromURI(data);
            if (parsedBitcoinURI == null) {
                TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.invalid_uri_does_not_contain__address));
                return;
            }
            String address = parsedBitcoinURI.first;
            if (address == null) {
                TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.invalid_uri_does_not_contain__address));
                return;
            }

            boolean success = this.fillToAddressTextField(address);
            if (success) {
                Long parsedBitcoinURIAmount = parsedBitcoinURI.second;
                if (parsedBitcoinURIAmount != null) {
                    TLCoin coinAmount = new TLCoin(parsedBitcoinURIAmount);
                    String amountString = appDelegate.currencyFormat.coinToProperBitcoinAmountString(coinAmount);
                    appDelegate.sendFormData.setAmount(amountString);
                    this.setAmountEditTextWrapper(amountString);
                    this.updateFiatAmountTextFieldExchangeRate(amountString);
                }
            }
        } else {
            this.fillToAddressTextField(data);
        }
    }

    private void setFiatAmountEditTextWrapper(String text) {
        fiatAmountEditText.removeTextChangedListener(fiatAmountEditTextTextWatcher);
        amountEditText.removeTextChangedListener(amountEditTextTextWatcher);
        this.fiatAmountEditText.setText(text);
        fiatAmountEditText.addTextChangedListener(fiatAmountEditTextTextWatcher);
        amountEditText.addTextChangedListener(amountEditTextTextWatcher);
    }

    private void setAmountEditTextWrapper(String text) {
        fiatAmountEditText.removeTextChangedListener(fiatAmountEditTextTextWatcher);
        amountEditText.removeTextChangedListener(amountEditTextTextWatcher);
        this.amountEditText.setText(text);
        fiatAmountEditText.addTextChangedListener(fiatAmountEditTextTextWatcher);
        amountEditText.addTextChangedListener(amountEditTextTextWatcher);
    }

    void preFetchUTXOsAndDynamicFees() {
        if (appDelegate.preferences.enabledDynamicFee()) {
            appDelegate.txFeeAPI.getDynamicTxFee(new TLCallback() {
                @Override
                public void onSuccess(Object obj) {
                }

                @Override
                public void onFail(Integer status, String error) {
                }
            });
            if (!appDelegate.godSend.haveUpDatedUTXOs()) {
                appDelegate.godSend.getAndSetUnspentOutputs(new TLCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                    }

                    @Override
                    public void onFail(Integer status, String error) {
                    }
                });
            }
        }
    }

    private void updateFiatAmountTextFieldExchangeRate(String btcAmountText) {
        String currency = appDelegate.currencyFormat.getFiatCurrency();
        TLCoin amount = appDelegate.currencyFormat.properBitcoinAmountStringToCoin(btcAmountText);
        if (amount != null && amount.greater(TLCoin.zero())) {
            this.setFiatAmountEditTextWrapper(appDelegate.currencyFormat.fiatAmountStringFromBitcoin(currency, amount));
            appDelegate.sendFormData.toAmount = amount;
        } else {
            this.setFiatAmountEditTextWrapper(null);
            appDelegate.sendFormData.toAmount = null;
        }
    }

    private void updateFiatAmountTextFieldExchangeRate() {
        updateFiatAmountTextFieldExchangeRate(this.amountEditText.getText().toString());
    }

    private void updateAmountTextFieldExchangeRate(String fiatAmountText) {
        String currency = appDelegate.currencyFormat.getFiatCurrency();
        if (fiatAmountText != null && fiatAmountText.length() > 0) {
            TLCoin bitcoinAmount = appDelegate.currencyFormat.coinAmountFromFiat(currency, fiatAmountText);
            this.setAmountEditTextWrapper(appDelegate.currencyFormat.coinToProperBitcoinAmountString(bitcoinAmount));
            appDelegate.sendFormData.toAmount = bitcoinAmount;
        } else {
            this.setAmountEditTextWrapper(null);
            appDelegate.sendFormData.toAmount = null;
        }
    }

    private void updateAmountTextFieldExchangeRate() {
        updateAmountTextFieldExchangeRate(this.fiatAmountEditText.getText().toString());
    }

    void checkToShowStealthPaymentDelayInfo() {
        if (!appDelegate.suggestions.disabledShowStealthPaymentDelayInfo() && appDelegate.blockExplorerAPI.blockExplorerAPI == TLBlockExplorerAPI.TLBlockExplorer.Blockchain) {
            TLPrompts.promptForOK(getActivity(), getString(R.string.warning), getString(R.string.send_to_reusable_address_not_insight), new TLPrompts.PromptOKCallback() {
                @Override
                public void onSuccess() {
                    appDelegate.suggestions.setDisableShowStealthPaymentDelayInfo(true);
                }
            });
        } else {
            this._reviewPaymentClicked();
        }
    }

    private void _reviewPaymentClicked() {
        showPromptReviewTx();
    }

    private void reviewPaymentClicked() {
        String toAddress = this.toAddressEditText.getText().toString();

        if (toAddress != null && TLStealthAddress.isStealthAddress(toAddress, appDelegate.appWallet.walletConfig.isTestnet)) {

            if (!appDelegate.suggestions.disabledShowStealthPaymentNote()) {

                TLPrompts.promptForOK(getActivity(), getString(R.string.warning), getString(R.string.send_to_reusable_address_warning), new TLPrompts.PromptOKCallback() {
                    @Override
                    public void onSuccess() {
                        _reviewPaymentClicked();
                        appDelegate.suggestions.setDisableShowStealthPaymentNote(true);
                        checkToShowStealthPaymentDelayInfo();
                    }
                });
            } else {
                checkToShowStealthPaymentDelayInfo();
            }

        } else {
            _reviewPaymentClicked();
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
        } else {
            startScanActivity(requestCode);
        }
    }

    private void setUpReviewPaymentView() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        reviewPaymentDialogView = inflater.inflate(R.layout.modal_review_payment, null);
        dialogBuilder.setView(reviewPaymentDialogView);
        reviewPaymentAlertDialog = dialogBuilder.create();
        reviewPaymentAlertDialog.setCanceledOnTouchOutside(false);
        setUpReviewPaymentViewButtons();
    }

    private void updateReviewPaymentView() {
        TLCoin sendAmount = appDelegate.sendFormData.toAmount;
        String sendAmountString = appDelegate.currencyFormat.coinToProperBitcoinAmountString(sendAmount);
        TLCoin feeAmount = appDelegate.sendFormData.feeAmount;
        TLCoin totalAmount = sendAmount.add(feeAmount);

        String fiatAmount = this.fiatAmountEditText.getText().toString();
        String feeString = appDelegate.currencyFormat.coinToProperBitcoinAmountString(feeAmount);

        String feeFiatString = appDelegate.currencyFormat.coinToProperBitcoinAmountString(feeAmount);
        String totalAmountBitcoin = appDelegate.currencyFormat.coinToProperBitcoinAmountString(totalAmount);
        String totalAmountFiat = appDelegate.currencyFormat.coinToProperBitcoinAmountString(totalAmount);


        TextView confirmFrom = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_from_label);

        confirmFrom.setText(appDelegate.sendFormData.fromLabel);

        TextView confirmDestination = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_to_label);
        confirmDestination.setText(appDelegate.sendFormData.getAddress());

        TextView tvAmountBtcUnit = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_amount_btc_unit);
        tvAmountBtcUnit.setText(appDelegate.currencyFormat.getBitcoinDisplay());
        TextView tvAmountFiatUnit = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_amount_fiat_unit);
        tvAmountFiatUnit.setText(appDelegate.currencyFormat.getFiatCurrency());

        //BTC Amount
        TextView tvAmountBtc = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_amount_btc);
        tvAmountBtc.setText(sendAmountString);

        //BTC Fee
        final TextView tvFeeBtc = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_fee_btc);
        tvFeeBtc.setText(feeString);

        TextView tvTotlaBtc = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_total_btc);
        tvTotlaBtc.setText(totalAmountBitcoin);

        //Fiat Amount
        TextView tvAmountFiat = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_amount_fiat);
        tvAmountFiat.setText(fiatAmount);

        //Fiat Fee
        TextView tvFeeFiat = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_fee_fiat);
        tvFeeFiat.setText(feeFiatString);

        TextView tvTotalFiat = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_total_fiat);
        tvTotalFiat.setText(totalAmountFiat);
    }

    private void setUpReviewPaymentViewButtons() {
        ImageView ivFeeInfo = (ImageView) reviewPaymentDialogView.findViewById(R.id.iv_fee_info);
        ivFeeInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TLPrompts.promptSuccessMessage(getActivity(), getString(R.string.transaction_fee), getString(R.string.transaction_fee_how));
            }
        });


        TextView tvCustomizeFee = (TextView) reviewPaymentDialogView.findViewById(R.id.tv_customize_fee);
        tvCustomizeFee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TLPrompts.promptForInputSaveCancel(getActivity(), getString(R.string.transaction_fee), getString(R.string.set_transaction_fee_in, appDelegate.currencyFormat.getBitcoinDisplay()),
                        "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, new TLPrompts.PromptCallback() {
                            @Override
                            public void onSuccess(Object obj) {
                                String feeAmountString = (String) obj;
                                TLCoin feeAmount = appDelegate.currencyFormat.properBitcoinAmountStringToCoin(feeAmountString);
                                TLCoin amountNeeded = appDelegate.sendFormData.toAmount.add(feeAmount);
                                TLCoin sendFromBalance = appDelegate.godSend.getCurrentFromBalance();
                                if (amountNeeded.greater(sendFromBalance)) {
                                    TLPrompts.promptErrorMessage(getActivity(), getString(R.string.insufficient_funds), getString(R.string.too_high_fee));
                                    return;
                                }

                                if (feeAmountString != null && !feeAmountString.isEmpty()) {
                                    if (TLTransactionFee.isTransactionFeeTooLow(feeAmount)) {
                                        TLPrompts.promptYesNo(getActivity(), getString(R.string.low_fee_not_recommended), "", getString(R.string.continue_capitalize), getString(R.string.cancel), new TLPrompts.PromptCallback() {
                                            @Override
                                            public void onSuccess(Object obj) {
                                                appDelegate.sendFormData.feeAmount = feeAmount;
                                                updateReviewPaymentView();
                                            }

                                            @Override
                                            public void onCancel() {
                                            }
                                        });
                                    } else if (TLTransactionFee.isTransactionFeeTooHigh(feeAmount)) {
                                        TLPrompts.promptYesNo(getActivity(), getString(R.string.high_fee_not_necessary), "", getString(R.string.continue_capitalize), getString(R.string.cancel), new TLPrompts.PromptCallback() {
                                            @Override
                                            public void onSuccess(Object obj) {
                                                appDelegate.sendFormData.feeAmount = feeAmount;
                                                updateReviewPaymentView();
                                            }

                                            @Override
                                            public void onCancel() {
                                            }
                                        });
                                    } else {
                                        appDelegate.sendFormData.feeAmount = feeAmount;
                                        updateReviewPaymentView();
                                    }
                                }
                            }

                            @Override
                            public void onCancel() {
                            }
                        });
            }
        });

        ImageView confirmCancel = (ImageView) reviewPaymentDialogView.findViewById(R.id.confirm_cancel);
        confirmCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (reviewPaymentAlertDialog != null && reviewPaymentAlertDialog.isShowing()) {
                    reviewPaymentAlertDialog.cancel();
                }
            }
        });

        TextView confirmSend = (TextView) reviewPaymentDialogView.findViewById(R.id.confirm_send);
        confirmSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSendTimer();
                if (!appDelegate.godSend.haveUpDatedUTXOs()) {
                    appDelegate.godSend.getAndSetUnspentOutputs(new TLCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            initiateSend();
                        }

                        @Override
                        public void onFail(Integer status, String error) {
                            cancelSend();
                            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.error_fetching_unspent_outputs_try_again));
                        }
                    });
                } else {
                    initiateSend();
                }
            }
        });
    }

    private void confirmPayment() {
        updateReviewPaymentView();
        reviewPaymentAlertDialog.show();
        if (!appDelegate.suggestions.disabledShowFeeExplanationInfo()) {
            TLPrompts.promptSuccessMessage(getActivity(), getString(R.string.transaction_fee), getString(R.string.transaction_fee_how));
            appDelegate.suggestions.setDisableShowFeeExplanationInfo(true);
        }
    }

    void showPromptPaymentSent(String txHash, String address, TLCoin amount) {
        TLHUDWrapper.hideHUD();
        Log.d(TAG, "showPromptPaymentSent " + txHash);
        String msg = getString(R.string.sent_amount_to_address, appDelegate.currencyFormat.getProperAmount(amount), address);
        TLPrompts.promptForOK(getActivity(), getString(R.string.success), msg, new TLPrompts.PromptOKCallback() {
            @Override
            public void onSuccess() {
                reviewPaymentAlertDialog.cancel();
            }
        });
    }

    void cancelSend() {
        sendHandler.removeCallbacks(sendRunnable);
        TLHUDWrapper.hideHUD();
    }

    @UiThread
    void retryFinishSend() {
        Log.d(TAG, "retryFinishSend " + sendTxHash);
        if (sendTxHash == null) {
            return;
        }
        if (!appDelegate.webSocketNotifiedTxHashSet.contains(sendTxHash)) {
            TLCoin nonUpdatedBalance = appDelegate.godSend.getCurrentFromBalance();
            TLCoin accountNewBalance = nonUpdatedBalance.subtract(amountMovedFromAccount);
            Log.d(TAG, "retryFinishSend 2 " + sendTxHash);
            appDelegate.godSend.setCurrentFromBalance(accountNewBalance);
        }

        if (!showedPromptedForSentPaymentTxHashSet.contains(sendTxHash)) {
            showedPromptedForSentPaymentTxHashSet.add(sendTxHash);
            sendHandler.removeCallbacks(sendRunnable);
            showPromptPaymentSent(sendTxHash, inputtedToAddress, inputtedToAmount);
        }
    }

    void finishSend(String webSocketNotifiedTxHash) {
        Log.d(TAG, "finishSend " + webSocketNotifiedTxHash);
        if (webSocketNotifiedTxHash.equals(sendTxHash) && !showedPromptedForSentPaymentTxHashSet.contains(webSocketNotifiedTxHash)) {
            Log.d(TAG, "finishSend 2 " + webSocketNotifiedTxHash);
            showedPromptedForSentPaymentTxHashSet.add(webSocketNotifiedTxHash);
            sendHandler.removeCallbacks(sendRunnable);
            showPromptPaymentSent(webSocketNotifiedTxHash, inputtedToAddress, inputtedToAmount);
        }
    }

    void promptToScanSignedTxAirGapData() {
        String msg = getString(R.string.parts_scanned, this.scannedSignedTxAirGapDataPartsDict.size(), this.totalExpectedParts);
        TLPrompts.promptYesNo(getActivity(), getString(R.string.scan_next_part),
                msg, getString(R.string.scan), getString(R.string.cancel), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        scanQRCode(SCAN_SIGNED_TX);
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    void startSendTimer() {
        TLHUDWrapper.showHUD(getActivity(), getString(R.string.sending));
        // relying on websocket to know when a payment has been sent can be unreliable, so cancel after a certain time
        final int TIME_TO_WAIT_TO_HIDE_HUD_AND_REFRESH_ACCOUNT = 13;
        sendHandler.postDelayed(sendRunnable, TIME_TO_WAIT_TO_HIDE_HUD_AND_REFRESH_ACCOUNT * 1000);
    }

    void promptToBroadcastColdWalletAccountSignedTx(String txHex, String txHash) {
        TLPrompts.promptYesNo(getActivity(), getString(R.string.send_authorized_payment),
                "", getString(R.string.send), getString(R.string.cancel), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        startSendTimer();
                        prepAndBroadcastTx(txHex, txHash);
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    void didScanSignedTxButton() {
        if (this.totalExpectedParts != 0 && this.scannedSignedTxAirGapDataPartsDict.size() == this.totalExpectedParts) {

            this.scannedSignedTxAirGapData = "";
            for (int i = 1; i <= this.totalExpectedParts; i++) {
                String dataPart = this.scannedSignedTxAirGapDataPartsDict.get(new Integer(i));
                this.scannedSignedTxAirGapData = this.scannedSignedTxAirGapData + dataPart;
            }
            this.scannedSignedTxAirGapDataPartsDict = new HashMap<>();
            JSONObject signedTxData = TLColdWallet.getSignedTxData(this.scannedSignedTxAirGapData);
            String txHex = null;
            try {
                txHex = signedTxData.getString("txHex");
                String txHash = signedTxData.getString("txHash");
                this.signedAirGapTxHex = txHex;
                this.signedAirGapTxHash = txHash;
                this.promptToBroadcastColdWalletAccountSignedTx(this.signedAirGapTxHex, this.signedAirGapTxHash);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            this.promptToScanSignedTxAirGapData();
        }
    }

    void showNextUnsignedTxPartQRCode() {
        if (this.airGapDataBase64PartsArray == null) {
            return;
        }
        String nextAipGapDataPart = this.airGapDataBase64PartsArray.get(0);
        this.airGapDataBase64PartsArray.remove(0);
        TLPrompts.promptQRCodeDialog(getActivity(), nextAipGapDataPart, getString(R.string.next), new TLPrompts.PromptCallback() {
            @Override
            public void onSuccess(Object obj) {
                if (airGapDataBase64PartsArray.size() > 0) {
                    showNextUnsignedTxPartQRCode();
                } else {
                    TLPrompts.promptYesNo(getActivity(), getString(R.string.finished_passing_transaction_data), getString(R.string.finished_passing_transaction_data_description), getString(R.string.continue_capitalize), getString(R.string.cancel), new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            scanQRCode(SCAN_SIGNED_TX);
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
                }
            }

            @Override
            public void onCancel() {
            }
        });
    }

    void promptToSignTransaction(String unSignedTx, JSONArray inputScripts, JSONArray txInputsAccountHDIdxes) {
        String extendedPublicKey = appDelegate.godSend.getExtendedPubKey();
        String airGapDataBase64 = TLColdWallet.createSerializedUnsignedTxAipGapData(unSignedTx, extendedPublicKey, inputScripts, txInputsAccountHDIdxes);
        if (airGapDataBase64 != null) {
            airGapDataBase64PartsArray = TLColdWallet.splitStringToArray(airGapDataBase64);
            TLPrompts.promptForOKCancel(getActivity(), getString(R.string.spending_from_a_cold_wallet_account),
                    getString(R.string.spending_from_a_cold_wallet_account_description), new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            showNextUnsignedTxPartQRCode();
                        }

                        @Override
                        public void onCancel() {

                        }
                    });
        }
    }

    void initiateSend() {
        TLCoin unspentOutputsSum = appDelegate.godSend.getCurrentFromUnspentOutputsSum();
        if (unspentOutputsSum.less(appDelegate.sendFormData.toAmount)) {
            // can only happen if unspentOutputsSum is for some reason less then the balance computed from the transactions, which it shouldn't
            cancelSend();
            String unspentOutputsSumString = appDelegate.currencyFormat.coinToProperBitcoinAmountString(unspentOutputsSum, true);
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.insufficient_funds), getString(R.string.funds_may_be_pending_confirmation, unspentOutputsSumString));
            return;
        }

        android.support.v4.util.Pair<String, TLCoin> toAddressesAndAmount = new android.support.v4.util.Pair<String, TLCoin>(appDelegate.sendFormData.getAddress(), appDelegate.sendFormData.toAmount);
        List<android.support.v4.util.Pair<String, TLCoin>> toAddressesAndAmounts = new ArrayList<android.support.v4.util.Pair<String, TLCoin>>();
        toAddressesAndAmounts.add(toAddressesAndAmount);
        CreatedTransactionObject createdTransactionObject = null;
        try {
            boolean signTx = !appDelegate.godSend.isColdWalletAccount();
            createdTransactionObject = appDelegate.godSend.createSignedSerializedTransactionHex(toAddressesAndAmounts, appDelegate.sendFormData.feeAmount, signTx);
        } catch (TLSpaghettiGodSend.DustException e) {
            String amountCanSendString = appDelegate.currencyFormat.coinToProperBitcoinAmountString(e.spendableAmount, true);
            cancelSend();
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.insufficient_funds_dust, amountCanSendString));
        } catch (TLSpaghettiGodSend.InsufficientFundsException e) {
            String valueSelectedString = this.appDelegate.currencyFormat.coinToProperBitcoinAmountString(e.valueSelected, true);
            String valueNeededString = this.appDelegate.currencyFormat.coinToProperBitcoinAmountString(e.valueNeeded, true);
            cancelSend();
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.insufficient_funds_account_balance, valueSelectedString, valueNeededString));
        } catch (TLSpaghettiGodSend.DustOutputException e) {
            String dustAmountBitcoins = appDelegate.currencyFormat.coinToProperBitcoinAmountString(e.dustAmount, true);
            cancelSend();
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), getString(R.string.cannot_create_transactions_with_outputs_less_then, dustAmountBitcoins));
        } catch (TLSpaghettiGodSend.InsufficientUnspentOutputException e) {
            Log.d(TAG, "SendFragment createSignedSerializedTransactionHex InsufficientUnspentOutputException " + e.getLocalizedMessage());
        } catch (TLSpaghettiGodSend.CreateTransactionException e) {
            cancelSend();
            TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), e.getLocalizedMessage());
        } finally {
            if (createdTransactionObject == null) {
                return;
            }
        }


        String txHex = createdTransactionObject.txHex;
        this.realToAddresses = createdTransactionObject.realToAddresses;

        if (txHex == null) {
            //should not reach here, because I check sum of unspent outputs already,
            // unless unspent outputs contains dust and are require to filled the amount I want to send
            cancelSend();
            return;
        }

        if (appDelegate.godSend.isColdWalletAccount()) {
            cancelSend();
            JSONArray txInputsAccountHDIdxes = createdTransactionObject.txInputsAccountHDIdxes;
            JSONArray inputScripts = createdTransactionObject.inputHexScripts;
            promptToSignTransaction(txHex, inputScripts, txInputsAccountHDIdxes);
            return;
        }

        String txHash = createdTransactionObject.txHash;
        prepAndBroadcastTx(txHex, txHash);
    }

    void prepAndBroadcastTx(String txHex, String txHash) {
        if (appDelegate.sendFormData.getAddress() == appDelegate.godSend.getStealthAddress()) {
            appDelegate.pendingSelfStealthPaymentTxid = txHash;
        }

        if (appDelegate.godSend.isPaymentToOwnAccount(appDelegate.sendFormData.getAddress())) {
            amountMovedFromAccount = appDelegate.sendFormData.feeAmount;
        } else {
            amountMovedFromAccount = appDelegate.sendFormData.toAmount.add(appDelegate.sendFormData.feeAmount);
        }

        for (String address : this.realToAddresses) {
            appDelegate.transactionListener.listenToIncomingTransactionForAddress(address);
        }

        inputtedToAddress = appDelegate.sendFormData.getAddress();
        inputtedToAmount = appDelegate.sendFormData.toAmount;
        sendTxHash = txHash;
        Log.d(TAG, "showPromptReviewTx txHex: " + txHex);
        Log.d(TAG, "showPromptReviewTx txHash: " + txHash);
        broadcastTx(txHex, txHash, inputtedToAddress);
    }

    void broadcastTx(String txHex, String txHash, String toAddress) {

        appDelegate.pushTxAPI.sendTx(txHex, txHash, toAddress, new TLCallback() {
            @Override
            public void onSuccess(Object obj) {
                String txid = (String) obj;
                Log.d(TAG, "showPromptReviewTx pushTx: success " + txid);

                if (TLStealthAddress.isStealthAddress(toAddress, appDelegate.appWallet.walletConfig.isTestnet) == true) {
                    // doing stealth payment with push tx insight get wrong hash back??
                    Log.d(TAG, "showPromptReviewTx pushTx: success txid" + txid);
                    Log.d(TAG, "showPromptReviewTx pushTx: success txHash " + txHash);
                    if (!txid.equals(txHash)) {
                        Log.d(TAG, "API Error: txid return does not match txid in app " + txHash);
                    }
                }

                String label = appDelegate.appWallet.getLabelForAddress(toAddress);
                if (label != null) {
                    appDelegate.appWallet.setTransactionTag(txHash, label);
                }
                clearSendForm();
                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_SEND_PAYMENT));
            }

            @Override
            public void onFail(Integer status, String error) {
                Log.d(TAG, "showPromptReviewTx pushTx: failure " + status + " " + error);
                if (status == 200) {
                    clearSendForm();
                    LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_SEND_PAYMENT));
                } else {
                    TLPrompts.promptErrorMessage(getActivity(), getString(R.string.error), error);
                    cancelSend();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (appDelegate != null) {
            LocalBroadcastManager.getInstance(appDelegate.context).unregisterReceiver(receiver);
        }
    }
}