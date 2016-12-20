package com.arcbit.arcbit.ui;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.view.View.OnClickListener;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLBitcoinjWrapper;
import com.arcbit.arcbit.model.TLHDWalletWrapper;
import com.arcbit.arcbit.model.TLWalletJSONKeys;
import com.arcbit.arcbit.ui.utils.TLPrompts;

import com.arcbit.arcbit.R;

import java.util.ArrayList;
import java.util.Arrays;

public class BrainWalletFragment extends android.support.v4.app.Fragment {
    private enum TLColdWalletKeyType {
        mnemonic,
        accountPrivateKey,
        accountPublicKey,
    }

    private static final String TAG = BrainWalletFragment.class.getName();
    private TLAppDelegate appDelegate = TLAppDelegate.instance();

    private View rootView;
    private TLColdWalletKeyType coldWalletKeyType = TLColdWalletKeyType.mnemonic;
    private TLColdWalletKeyType previousColdWalletKeyType = TLColdWalletKeyType.mnemonic;
    private boolean isSettingEditFromCode = false;

    private RadioGroup keyMethodRadioGroup;
    private EditText mnemonicEditText;
    private Button newWalletButton;
    private EditText accountIDEditText;
    private EditText accountPublicKeyEditText;
    private EditText accountPrivateKeyEditText;
    private TextWatcher mnemonicTextWatcher;
    private TextWatcher accountIDTextWatcher;
    private TextWatcher accountPublicKeyTextWatcher;
    private TextWatcher accountPrivateKeyTextWatcher;
    private Button showAccountPublicKeyQRButton;
    private Button showAccountPrivateKeyQRButton;
    
    private EditText startingAddressIDTextField;
    private TextView addressLabel1;
    private EditText addressTextField1;
    private Button showAddressQRCodeButton1;
    private EditText privateKeyTextField1;
    private Button showPrivateKeyQRCodeButton1;
    private TextView addressLabel2;
    private EditText addressTextField2;
    private Button showAddressQRCodeButton2;
    private EditText privateKeyTextField2;
    private Button showPrivateKeyQRCodeButton2;
    private TextView addressLabel3;
    private EditText addressTextField3;
    private Button showAddressQRCodeButton3;
    private EditText privateKeyTextField3;
    private Button showPrivateKeyQRCodeButton3;
    private TextView addressLabel4;
    private EditText addressTextField4;
    private Button showAddressQRCodeButton4;
    private EditText privateKeyTextField4;
    private Button showPrivateKeyQRCodeButton4;
    private TextView addressLabel5;
    private EditText addressTextField5;
    private Button showAddressQRCodeButton5;
    private EditText privateKeyTextField5;
    private Button showPrivateKeyQRCodeButton5;

    private EditText startingChangeAddressIDTextField;
    private TextView changeAddressLabel1;
    private EditText changeAddressTextField1;
    private Button showChangeAddressQRCodeButton1;
    private EditText changePrivateKeyTextField1;
    private Button showChangePrivateKeyQRCodeButton1;
    private TextView changeAddressLabel2;
    private EditText changeAddressTextField2;
    private Button showChangeAddressQRCodeButton2;
    private EditText changePrivateKeyTextField2;
    private Button showChangePrivateKeyQRCodeButton2;
    private TextView changeAddressLabel3;
    private EditText changeAddressTextField3;
    private Button showChangeAddressQRCodeButton3;
    private EditText changePrivateKeyTextField3;
    private Button showChangePrivateKeyQRCodeButton3;
    private TextView changeAddressLabel4;
    private EditText changeAddressTextField4;
    private Button showChangeAddressQRCodeButton4;
    private EditText changePrivateKeyTextField4;
    private Button showChangePrivateKeyQRCodeButton4;
    private TextView changeAddressLabel5;
    private EditText changeAddressTextField5;
    private Button showChangeAddressQRCodeButton5;
    private EditText changePrivateKeyTextField5;
    private Button showChangePrivateKeyQRCodeButton5;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_brain_wallet, container, false);;
        if (appDelegate == null) {
            return rootView;
        }

        getActivity().setTitle(getString(R.string.internal_wallet_data));
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setupUIToHideKeyBoardOnTouch(rootView);


        keyMethodRadioGroup = (RadioGroup) rootView.findViewById(R.id.key_method_radio_group);
        mnemonicEditText = (EditText)rootView.findViewById(R.id.mnemonic_edit_text);
        newWalletButton = (Button)rootView.findViewById(R.id.new_wallet_button);
        accountIDEditText = (EditText)rootView.findViewById(R.id.account_id_edit_text);
        accountPublicKeyEditText = (EditText)rootView.findViewById(R.id.account_public_key_edit_text);
        accountPrivateKeyEditText = (EditText)rootView.findViewById(R.id.account_private_key_edit_text);
        showAccountPublicKeyQRButton = (Button)rootView.findViewById(R.id.account_public_key_qr_code_button);
        showAccountPrivateKeyQRButton = (Button)rootView.findViewById(R.id.account_private_key_qr_code_button);

        this.startingAddressIDTextField = (EditText)rootView.findViewById(R.id.address_id_edit_text);
        this.addressLabel1 = (TextView)rootView.findViewById(R.id.receiving_address_0_text_view);
        this.addressTextField1 = (EditText)rootView.findViewById(R.id.receiving_address_0_edit_text);
        this.showAddressQRCodeButton1 = (Button) rootView.findViewById(R.id.receiving_address_0_qr_code_button);
        this.privateKeyTextField1 = (EditText)rootView.findViewById(R.id.receiving_private_key_0_edit_text);
        this.showPrivateKeyQRCodeButton1 = (Button) rootView.findViewById(R.id.receiving_private_key_0_qr_code_button);
        this.addressLabel2 = (TextView)rootView.findViewById(R.id.receiving_address_1_text_view);
        this.addressTextField2 = (EditText)rootView.findViewById(R.id.receiving_address_1_edit_text);
        this.showAddressQRCodeButton2 = (Button) rootView.findViewById(R.id.receiving_address_1_qr_code_button);
        this.privateKeyTextField2 = (EditText)rootView.findViewById(R.id.receiving_private_key_1_edit_text);
        this.showPrivateKeyQRCodeButton2 = (Button) rootView.findViewById(R.id.receiving_private_key_1_qr_code_button);
        this.addressLabel3 = (TextView)rootView.findViewById(R.id.receiving_address_2_text_view);
        this.addressTextField3 = (EditText)rootView.findViewById(R.id.receiving_address_2_edit_text);
        this.showAddressQRCodeButton3 = (Button) rootView.findViewById(R.id.receiving_address_2_qr_code_button);
        this.privateKeyTextField3 = (EditText)rootView.findViewById(R.id.receiving_private_key_2_edit_text);
        this.showPrivateKeyQRCodeButton3 = (Button) rootView.findViewById(R.id.receiving_private_key_2_qr_code_button);
        this.addressLabel4 = (TextView)rootView.findViewById(R.id.receiving_address_3_text_view);
        this.addressTextField4 = (EditText)rootView.findViewById(R.id.receiving_address_3_edit_text);
        this.showAddressQRCodeButton4 = (Button) rootView.findViewById(R.id.receiving_address_3_qr_code_button);
        this.privateKeyTextField4 = (EditText) rootView.findViewById(R.id.receiving_private_key_3_edit_text);
        this.showPrivateKeyQRCodeButton4 = (Button) rootView.findViewById(R.id.receiving_private_key_3_qr_code_button);

        this.addressLabel5 = (TextView)rootView.findViewById(R.id.receiving_address_4_text_view);
        this.addressTextField5 = (EditText)rootView.findViewById(R.id.receiving_address_4_edit_text);
        this.showAddressQRCodeButton5 = (Button) rootView.findViewById(R.id.receiving_address_4_qr_code_button);
        this.privateKeyTextField5 = (EditText)rootView.findViewById(R.id.receiving_private_key_4_edit_text);
        this.showPrivateKeyQRCodeButton5 = (Button) rootView.findViewById(R.id.receiving_private_key_4_qr_code_button);


        this.startingChangeAddressIDTextField = (EditText)rootView.findViewById(R.id.change_address_id_edit_text);
        this.changeAddressLabel1 = (TextView)rootView.findViewById(R.id.change_address_0_text_view);
        this.changeAddressTextField1 = (EditText)rootView.findViewById(R.id.change_address_0_edit_text);
        this.showChangeAddressQRCodeButton1 = (Button) rootView.findViewById(R.id.change_address_0_qr_code_button);
        this.changePrivateKeyTextField1 = (EditText)rootView.findViewById(R.id.change_private_key_0_edit_text);
        this.showChangePrivateKeyQRCodeButton1 = (Button) rootView.findViewById(R.id.change_private_key_0_qr_code_button);
        this.changeAddressLabel2 = (TextView)rootView.findViewById(R.id.change_address_1_text_view);
        this.changeAddressTextField2 = (EditText)rootView.findViewById(R.id.change_address_1_edit_text);
        this.showChangeAddressQRCodeButton2 = (Button) rootView.findViewById(R.id.change_address_1_qr_code_button);
        this.changePrivateKeyTextField2 = (EditText)rootView.findViewById(R.id.change_private_key_1_edit_text);
        this.showChangePrivateKeyQRCodeButton2 = (Button) rootView.findViewById(R.id.change_private_key_1_qr_code_button);
        this.changeAddressLabel3 = (TextView)rootView.findViewById(R.id.change_address_2_text_view);
        this.changeAddressTextField3 = (EditText)rootView.findViewById(R.id.change_address_2_edit_text);
        this.showChangeAddressQRCodeButton3 = (Button) rootView.findViewById(R.id.change_address_2_qr_code_button);
        this.changePrivateKeyTextField3 = (EditText)rootView.findViewById(R.id.change_private_key_2_edit_text);
        this.showChangePrivateKeyQRCodeButton3 = (Button) rootView.findViewById(R.id.change_private_key_2_qr_code_button);
        this.changeAddressLabel4 = (TextView)rootView.findViewById(R.id.change_address_3_text_view);
        this.changeAddressTextField4 = (EditText)rootView.findViewById(R.id.change_address_3_edit_text);
        this.showChangeAddressQRCodeButton4 = (Button) rootView.findViewById(R.id.change_address_3_qr_code_button);
        this.changePrivateKeyTextField4 = (EditText)rootView.findViewById(R.id.change_private_key_3_edit_text);
        this.showChangePrivateKeyQRCodeButton4 = (Button) rootView.findViewById(R.id.change_private_key_3_qr_code_button);
        this.changeAddressLabel5 = (TextView)rootView.findViewById(R.id.change_address_4_text_view);
        this.changeAddressTextField5 = (EditText)rootView.findViewById(R.id.change_address_4_edit_text);
        this.showChangeAddressQRCodeButton5 = (Button) rootView.findViewById(R.id.change_address_4_qr_code_button);
        this.changePrivateKeyTextField5 = (EditText)rootView.findViewById(R.id.change_private_key_4_edit_text);
        this.showChangePrivateKeyQRCodeButton5 = (Button) rootView.findViewById(R.id.change_private_key_4_qr_code_button);

        keyMethodRadioGroup.check(R.id.mnemonic_radio_button);
        keyMethodRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId) {
                    case R.id.mnemonic_radio_button:
                        updateCellWithColdWalletKeyType(TLColdWalletKeyType.mnemonic);
                        break;
                    case R.id.account_private_key_radio_button:
                        updateCellWithColdWalletKeyType(TLColdWalletKeyType.accountPrivateKey);
                        break;
                    case R.id.account_public_key_radio_button:
                        updateCellWithColdWalletKeyType(TLColdWalletKeyType.accountPublicKey);
                        break;
                }
            }
        });
        mnemonicTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isSettingEditFromCode) {
                    return;
                }
                String mnemonicString = s.toString();
                isSettingEditFromCode = true;
                didUpdateMnemonic(mnemonicString, null);
                isSettingEditFromCode = false;
            }
        };
        mnemonicEditText.addTextChangedListener(mnemonicTextWatcher);
        newWalletButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSettingEditFromCode) {
                    return;
                }
                String mnemonicString = TLHDWalletWrapper.generateMnemonicPassphrase();
                isSettingEditFromCode = true;
                setMnemonicEditTextWrapper(mnemonicString);
                didUpdateMnemonic(mnemonicString, null);
                isSettingEditFromCode = false;
            }
        });
        accountIDTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isSettingEditFromCode) {
                    return;
                }
                String accountIDString = s.toString();
                Long HDAccountID = 0L;
                try {
                    HDAccountID = Long.valueOf(accountIDString);
                } catch (NumberFormatException name) {
                }
                String mnemonicString = mnemonicEditText.getText().toString();
                isSettingEditFromCode = true;
                didUpdateMnemonic(mnemonicString, HDAccountID);
                isSettingEditFromCode = false;
            }
        };
        accountIDEditText.addTextChangedListener(accountIDTextWatcher);
        accountPublicKeyTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isSettingEditFromCode) {
                    return;
                }
                String accountPublicKeyString = s.toString();
                isSettingEditFromCode = true;
                didUpdateAccountPublicKey(accountPublicKeyString);
                isSettingEditFromCode = false;
            }
        };
        accountPublicKeyEditText.addTextChangedListener(accountPublicKeyTextWatcher);
        showAccountPublicKeyQRButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String accountPublicKey = accountPublicKeyEditText.getText().toString();
                if (accountPublicKey != null && TLHDWalletWrapper.isValidExtendedPublicKey(accountPublicKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountPublicKey);
                }
            }
        });
        accountPrivateKeyTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isSettingEditFromCode) {
                    return;
                }
                String accountPrivateKeyString = s.toString();
                isSettingEditFromCode = true;
                didUpdateAccountPrivateKey(accountPrivateKeyString);
                isSettingEditFromCode = false;
            }
        };
        accountPrivateKeyEditText.addTextChangedListener(accountPrivateKeyTextWatcher);
        showAccountPrivateKeyQRButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String accountPrivateKey = accountPrivateKeyEditText.getText().toString();
                if (accountPrivateKey != null && TLHDWalletWrapper.isValidExtendedPrivateKey(accountPrivateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountPrivateKey);
                }
            }
        });


        startingAddressIDTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String addressIDString = s.toString();
                Long addressID = 0L;
                try {
                    addressID = Long.valueOf(addressIDString);
                } catch (NumberFormatException name) {
                }
                updateAddressFieldsWithStartingAddressID(addressID);
            }
        });
        showAddressQRCodeButton1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = addressTextField1.getText().toString();
                if (address != null && TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
                }
            }
        });
        showPrivateKeyQRCodeButton1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String privateKey = privateKeyTextField1.getText().toString();
                if (privateKey != null && TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
                }
            }
        });
        showAddressQRCodeButton2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = addressTextField2.getText().toString();
                if (address != null && TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
                }
            }
        });
        showPrivateKeyQRCodeButton2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String privateKey = privateKeyTextField2.getText().toString();
                if (privateKey != null && TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
                }
            }
        });
        showAddressQRCodeButton3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = addressTextField3.getText().toString();
                if (address != null && TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
                }
            }
        });
        showPrivateKeyQRCodeButton3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String privateKey = privateKeyTextField3.getText().toString();
                if (privateKey != null && TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
                }
            }
        });
        showAddressQRCodeButton4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = addressTextField4.getText().toString();
                if (address != null && TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
                }
            }
        });
        showPrivateKeyQRCodeButton4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String privateKey = privateKeyTextField4.getText().toString();
                if (privateKey != null && TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
                }
            }
        });
        showAddressQRCodeButton1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = addressTextField1.getText().toString();
                if (address != null && TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
                }
            }
        });
        showPrivateKeyQRCodeButton5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String privateKey = privateKeyTextField5.getText().toString();
                if (privateKey != null && TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
                }
            }
        });


        startingChangeAddressIDTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String addressIDString = s.toString();
                Long addressID = 0L;
                try {
                    addressID = Long.valueOf(addressIDString);
                } catch (NumberFormatException name) {
                }
                updateChangeAddressFieldsWithStartingAddressID(addressID);
            }
        });
        showChangeAddressQRCodeButton1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = changeAddressTextField1.getText().toString();
                if (address != null && TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
                }
            }
        });
        showChangePrivateKeyQRCodeButton1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String privateKey = changePrivateKeyTextField1.getText().toString();
                if (privateKey != null && TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
                }
            }
        });
        showChangeAddressQRCodeButton2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = changeAddressTextField2.getText().toString();
                if (address != null && TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
                }
            }
        });
        showChangePrivateKeyQRCodeButton2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String privateKey = changePrivateKeyTextField2.getText().toString();
                if (privateKey != null && TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
                }
            }
        });
        showChangeAddressQRCodeButton3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = changeAddressTextField3.getText().toString();
                if (address != null && TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
                }
            }
        });
        showChangePrivateKeyQRCodeButton3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String privateKey = changePrivateKeyTextField3.getText().toString();
                if (privateKey != null && TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
                }
            }
        });
        showChangeAddressQRCodeButton4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = changeAddressTextField4.getText().toString();
                if (address != null && TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
                }
            }
        });
        showChangePrivateKeyQRCodeButton4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String privateKey = changePrivateKeyTextField4.getText().toString();
                if (privateKey != null && TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
                }
            }
        });
        showChangeAddressQRCodeButton5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = changeAddressTextField5.getText().toString();
                if (address != null && TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), address);
                }
            }
        });
        showChangePrivateKeyQRCodeButton5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String privateKey = changePrivateKeyTextField5.getText().toString();
                if (privateKey != null && TLBitcoinjWrapper.isValidPrivateKey(privateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), privateKey);
                }
            }
        });

        setMnemonicEditTextWrapper(null);
        setAccountPublicKeyEditTextWrapper(null);
        setAccountPrivateKeyEditTextWrapper(null);

        this.updateWalletKeys();
        this.updateAccountIDTextField(false);
        this.updateAccountPublicKeyEditText(null);
        this.updateAccountPrivateKeyEditText(null);
        this.clearAddressFields();
        return rootView;
    }

    void enableTextView(EditText textField, boolean enable) {
        textField.setEnabled(enable);
        if (enable) {
            textField.setAlpha(1.0f);
        } else {
            textField.setAlpha(0.5f);
        }
    }

    void enableTextField(EditText textField, boolean enable) {
        textField.setEnabled(enable);
        if (enable) {
            textField.setAlpha(1.0f);
        } else {
            textField.setAlpha(0.5f);
        }
    }

    void enableButton(Button button, boolean enable) {
        button.setEnabled(enable);
        if (enable) {
            button.setAlpha(1.0f);
        } else {
            button.setAlpha(0.5f);
        }
    }

    void setMnemonicEditTextWrapper(String str) {
        this.mnemonicEditText.setText(str);
    }

    void setAccountPrivateKeyEditTextWrapper(String str) {
        this.accountPrivateKeyEditText.setText(str);
    }

    void setAccountPublicKeyEditTextWrapper(String str) {
        this.accountPublicKeyEditText.setText(str);
    }

    void setAccountIdEditTextWrapper(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) stringBuilder.append(element.toString()).append("\n");
        this.accountIDEditText.setText(str);
    }

    void updateWalletKeys() {

        if (this.coldWalletKeyType == TLColdWalletKeyType.mnemonic) {
            this.enableTextView(this.mnemonicEditText, true);
            this.enableButton(this.newWalletButton, true);
            this.enableTextView(this.accountPrivateKeyEditText, false);
            this.enableTextView(this.accountPublicKeyEditText, false);

            this.isSettingEditFromCode = true;
            this.updateAccountIDTextField(false);
            didUpdateMnemonic(mnemonicEditText.getText().toString(), null);
            this.isSettingEditFromCode = false;

        } else if (this.coldWalletKeyType == TLColdWalletKeyType.accountPublicKey) {
            this.enableTextView(this.mnemonicEditText, false);
            this.enableButton(this.newWalletButton, false);
            this.enableTextView(this.accountPrivateKeyEditText, false);
            this.enableTextView(this.accountPublicKeyEditText, true);
            this.enableButton(this.showAccountPrivateKeyQRButton, false);

            this.isSettingEditFromCode = true;
            this.setMnemonicEditTextWrapper(null);
            this.updateAccountIDTextField(false);
            this.setAccountPrivateKeyEditTextWrapper(null);
            this.didUpdateAccountPublicKey(accountPublicKeyEditText.getText().toString());
            this.isSettingEditFromCode = false;

        } else if (this.coldWalletKeyType == TLColdWalletKeyType.accountPrivateKey) {
            this.enableTextView(this.mnemonicEditText, false);
            this.enableButton(this.newWalletButton, false);
            this.enableTextView(this.accountPrivateKeyEditText, true);
            this.enableTextView(this.accountPublicKeyEditText, false);

            this.isSettingEditFromCode = true;
            this.updateAccountIDTextField(false);
            this.setMnemonicEditTextWrapper(null);
            this.didUpdateAccountPrivateKey(accountPrivateKeyEditText.getText().toString());
            this.isSettingEditFromCode = false;
        }
    }

    void updateCellWithColdWalletKeyType(TLColdWalletKeyType coldWalletKeyType) {
        if (this.coldWalletKeyType == coldWalletKeyType) {
            return;
        }
        this.previousColdWalletKeyType = this.coldWalletKeyType;
        this.coldWalletKeyType = coldWalletKeyType;
        this.updateWalletKeys();
    }

    private void didUpdateMnemonic(String mnemonicPassphrase, Long accountID) {
        if (TLHDWalletWrapper.phraseIsValid(mnemonicPassphrase)) {
            String masterHex = TLHDWalletWrapper.getMasterHex(mnemonicPassphrase);
            Long HDAccountID = 0L;
            if (accountID == null) {
                try {
                    HDAccountID = Long.valueOf(this.accountIDEditText.getText().toString());
                } catch (NumberFormatException name) {
                }
            } else {
                HDAccountID = accountID;
            }

            this.updateAccountIDTextField(true);
            String extendedPublicKey = TLHDWalletWrapper.getExtendPubKeyFromMasterHex(masterHex, HDAccountID.intValue(), appDelegate.appWallet.walletConfig.isTestnet);
            this.setAccountPublicKeyEditTextWrapper(extendedPublicKey);
            this.updateAccountPublicKeyEditText(extendedPublicKey);
            String extendedPrivateKey = TLHDWalletWrapper.getExtendPrivKey(masterHex, HDAccountID.intValue(), appDelegate.appWallet.walletConfig.isTestnet);
            this.setAccountPrivateKeyEditTextWrapper(extendedPrivateKey);
            this.updateAccountPrivateKeyEditText(extendedPrivateKey);

            this.updateAddressFieldsWithStartingAddressID(null);
            this.updateChangeAddressFieldsWithStartingAddressID(null);
        } else {
            this.updateAccountIDTextField(false);
            this.setAccountPublicKeyEditTextWrapper(null);
            this.setAccountPrivateKeyEditTextWrapper(null);
            this.updateAccountPublicKeyEditText(null);
            this.updateAccountPrivateKeyEditText(null);
            this.clearAddressFields();
        }
    }

    void didUpdateAccountPublicKey(String accountPublicKey) {
        if (!accountPublicKey.isEmpty() && TLHDWalletWrapper.isValidExtendedPublicKey(accountPublicKey, appDelegate.appWallet.walletConfig.isTestnet)) {
            Integer accoundIdx = TLHDWalletWrapper.getAccountIdxForExtendedKey(accountPublicKey, appDelegate.appWallet.walletConfig.isTestnet);
            this.setAccountIdEditTextWrapper(Integer.toString(accoundIdx));
            this.enableButton(this.showAccountPublicKeyQRButton, true);
            this.updateAddressFieldsWithStartingAddressID(null);
            this.updateChangeAddressFieldsWithStartingAddressID(null);
        } else {
            this.setAccountIdEditTextWrapper(null);
            this.enableButton(this.showAccountPublicKeyQRButton, false);
            this.clearAddressFields();
        }
    }

    void didUpdateAccountPrivateKey(String accountPrivateKey) {
        if (!accountPrivateKey.isEmpty() && TLHDWalletWrapper.isValidExtendedPrivateKey(accountPrivateKey, appDelegate.appWallet.walletConfig.isTestnet)) {
            Integer accoundIdx = TLHDWalletWrapper.getAccountIdxForExtendedKey(accountPrivateKey, appDelegate.appWallet.walletConfig.isTestnet);
            this.setAccountIdEditTextWrapper(Integer.toString(accoundIdx));
            String accountPublicKey = TLHDWalletWrapper.getExtendPubKey(accountPrivateKey, appDelegate.appWallet.walletConfig.isTestnet);
            this.setAccountPublicKeyEditTextWrapper(accountPublicKey);
            this.enableButton(this.showAccountPrivateKeyQRButton, true);
            this.enableButton(this.showAccountPublicKeyQRButton, true);
            this.updateAccountPrivateKeyEditText(accountPrivateKey);
            this.updateAddressFieldsWithStartingAddressID(null);
            this.updateChangeAddressFieldsWithStartingAddressID(null);
        } else {
            this.setAccountIdEditTextWrapper(null);
            this.setAccountPublicKeyEditTextWrapper(null);
            this.updateAccountPublicKeyEditText(null);
            this.enableButton(this.showAccountPrivateKeyQRButton, false);
            this.enableButton(this.showAccountPublicKeyQRButton, false);
            this.clearAddressFields();
        }
    }

    void updateAccountIDTextField(boolean enable) {
        if (enable) {
            this.enableTextField(this.accountIDEditText, true);
        } else {
            this.enableTextField(this.accountIDEditText, false);
            this.setAccountIdEditTextWrapper(null);
        }
    }

    void updateAccountPublicKeyEditText(String extendedPublicKey) {
        if (extendedPublicKey == null) {
            this.enableButton(this.showAccountPublicKeyQRButton, false);
            return;
        }
        this.enableButton(this.showAccountPublicKeyQRButton, true);
    }

    void updateAccountPrivateKeyEditText(String extendedPrivateKey) {
        if (extendedPrivateKey == null) {
            this.enableButton(this.showAccountPrivateKeyQRButton, false);
            return;
        }
        this.enableButton(this.showAccountPrivateKeyQRButton, true);
    }

    void clearAddressFields() {
        this.clearReceivingAddressFields();
        this.clearChangeAddressFields();
    }

    void clearReceivingAddressFields() {
        this.enableTextField(this.startingAddressIDTextField, false);

        this.enableButton(this.showAddressQRCodeButton1, false);
        this.enableButton(this.showAddressQRCodeButton2, false);
        this.enableButton(this.showAddressQRCodeButton3, false);
        this.enableButton(this.showAddressQRCodeButton4, false);
        this.enableButton(this.showAddressQRCodeButton5, false);

        this.enableButton(this.showPrivateKeyQRCodeButton1, false);
        this.enableButton(this.showPrivateKeyQRCodeButton2, false);
        this.enableButton(this.showPrivateKeyQRCodeButton3, false);
        this.enableButton(this.showPrivateKeyQRCodeButton4, false);
        this.enableButton(this.showPrivateKeyQRCodeButton5, false);

        this.enableTextField(this.addressTextField1, false);
        this.enableTextField(this.addressTextField2, false);
        this.enableTextField(this.addressTextField3, false);
        this.enableTextField(this.addressTextField4, false);
        this.enableTextField(this.addressTextField5, false);

        this.enableTextField(this.privateKeyTextField1, false);
        this.enableTextField(this.privateKeyTextField2, false);
        this.enableTextField(this.privateKeyTextField3, false);
        this.enableTextField(this.privateKeyTextField4, false);
        this.enableTextField(this.privateKeyTextField5, false);

        this.startingAddressIDTextField.setText(null);

        this.privateKeyTextField1.setText(null);
        this.addressTextField1.setText(null);
        this.privateKeyTextField2.setText(null);
        this.addressTextField2.setText(null);
        this.privateKeyTextField3.setText(null);
        this.addressTextField3.setText(null);
        this.privateKeyTextField4.setText(null);
        this.addressTextField4.setText(null);
        this.privateKeyTextField5.setText(null);
        this.addressTextField5.setText(null);
    }

    void clearChangeAddressFields() {
        this.enableTextField(this.startingChangeAddressIDTextField, false);

        this.enableButton(this.showChangeAddressQRCodeButton1, false);
        this.enableButton(this.showChangeAddressQRCodeButton2, false);
        this.enableButton(this.showChangeAddressQRCodeButton3, false);
        this.enableButton(this.showChangeAddressQRCodeButton4, false);
        this.enableButton(this.showChangeAddressQRCodeButton5, false);

        this.enableButton(this.showChangePrivateKeyQRCodeButton1, false);
        this.enableButton(this.showChangePrivateKeyQRCodeButton2, false);
        this.enableButton(this.showChangePrivateKeyQRCodeButton3, false);
        this.enableButton(this.showChangePrivateKeyQRCodeButton4, false);
        this.enableButton(this.showChangePrivateKeyQRCodeButton5, false);

        this.enableTextField(this.changeAddressTextField1, false);
        this.enableTextField(this.changeAddressTextField2, false);
        this.enableTextField(this.changeAddressTextField3, false);
        this.enableTextField(this.changeAddressTextField4, false);
        this.enableTextField(this.changeAddressTextField5, false);

        this.enableTextField(this.changePrivateKeyTextField1, false);
        this.enableTextField(this.changePrivateKeyTextField2, false);
        this.enableTextField(this.changePrivateKeyTextField3, false);
        this.enableTextField(this.changePrivateKeyTextField4, false);
        this.enableTextField(this.changePrivateKeyTextField5, false);

        this.startingChangeAddressIDTextField.setText(null);

        this.changePrivateKeyTextField1.setText(null);
        this.changeAddressTextField1.setText(null);
        this.changePrivateKeyTextField2.setText(null);
        this.changeAddressTextField2.setText(null);
        this.changePrivateKeyTextField3.setText(null);
        this.changeAddressTextField3.setText(null);
        this.changePrivateKeyTextField4.setText(null);
        this.changeAddressTextField4.setText(null);
        this.changePrivateKeyTextField5.setText(null);
        this.changeAddressTextField5.setText(null);
    }

    void updateAddressFieldsWithStartingAddressID(Long startingAddressID) {
        Long addressID = 0L;
        if (startingAddressID == null) {
            try {
                addressID = Long.valueOf(this.startingAddressIDTextField.getText().toString());
            } catch (NumberFormatException name) {
            }
        } else {
            addressID = startingAddressID;
        }
        updateAddressFields(addressID);
    }

    void updateChangeAddressFieldsWithStartingAddressID(Long startingAddressID) {
        Long addressID = 0L;
        if (startingAddressID == null) {
            try {
                addressID = Long.valueOf(this.startingChangeAddressIDTextField.getText().toString());
            } catch (NumberFormatException name) {
            }
        } else {
            addressID = startingAddressID;
        }
        updateChangeAddressFields(addressID);
    }

    void updateAddressFields(Long startingAddressID) {
        String extendedPrivateKey = null;
        String extendedPublicKey = null;
        if (this.coldWalletKeyType == TLColdWalletKeyType.mnemonic) {
            extendedPublicKey = this.accountPublicKeyEditText.getText().toString();
            extendedPrivateKey = this.accountPrivateKeyEditText.getText().toString();
        } else if (this.coldWalletKeyType == TLColdWalletKeyType.accountPublicKey) {
            extendedPublicKey = this.accountPublicKeyEditText.getText().toString();
        } else if (this.coldWalletKeyType == TLColdWalletKeyType.accountPrivateKey) {
            extendedPublicKey = this.accountPublicKeyEditText.getText().toString();
            extendedPrivateKey = this.accountPrivateKeyEditText.getText().toString();
        }
        if (extendedPublicKey != null && TLHDWalletWrapper.isValidExtendedPublicKey(extendedPublicKey, appDelegate.appWallet.walletConfig.isTestnet)
        && (extendedPrivateKey == null || extendedPrivateKey != null && TLHDWalletWrapper.isValidExtendedPrivateKey(extendedPrivateKey, appDelegate.appWallet.walletConfig.isTestnet))) {
            this.enableTextField(this.startingAddressIDTextField, true);

            Integer HDAddressIdx = startingAddressID.intValue();
            ArrayList<Integer> addressSequence1 = new ArrayList<Integer>(Arrays.asList((Integer) TLWalletJSONKeys.TLAddressType.Main.getValue(), HDAddressIdx));
            this.addressLabel1.setText(getString(R.string.address_id_colon_number, HDAddressIdx));
            this.addressTextField1.setText(TLHDWalletWrapper.getAddress(extendedPublicKey, addressSequence1, appDelegate.appWallet.walletConfig.isTestnet));
            HDAddressIdx += 1;
            ArrayList<Integer> addressSequence2 = new ArrayList<Integer>(Arrays.asList((Integer) TLWalletJSONKeys.TLAddressType.Main.getValue(), HDAddressIdx));
            this.addressLabel2.setText(getString(R.string.address_id_colon_number, HDAddressIdx));
            this.addressTextField2.setText(TLHDWalletWrapper.getAddress(extendedPublicKey, addressSequence2, appDelegate.appWallet.walletConfig.isTestnet));
            HDAddressIdx += 1;
            ArrayList<Integer> addressSequence3 = new ArrayList<Integer>(Arrays.asList((Integer) TLWalletJSONKeys.TLAddressType.Main.getValue(), HDAddressIdx));
            this.addressLabel3.setText(getString(R.string.address_id_colon_number, HDAddressIdx));
            this.addressTextField3.setText(TLHDWalletWrapper.getAddress(extendedPublicKey, addressSequence3, appDelegate.appWallet.walletConfig.isTestnet));
            HDAddressIdx += 1;
            ArrayList<Integer> addressSequence4 = new ArrayList<Integer>(Arrays.asList((Integer) TLWalletJSONKeys.TLAddressType.Main.getValue(), HDAddressIdx));
            this.addressLabel4.setText(getString(R.string.address_id_colon_number, HDAddressIdx));
            this.addressTextField4.setText(TLHDWalletWrapper.getAddress(extendedPublicKey, addressSequence4, appDelegate.appWallet.walletConfig.isTestnet));
            HDAddressIdx += 1;
            ArrayList<Integer> addressSequence5 = new ArrayList<Integer>(Arrays.asList((Integer) TLWalletJSONKeys.TLAddressType.Main.getValue(), HDAddressIdx));
            this.addressLabel5.setText(getString(R.string.address_id_colon_number, HDAddressIdx));
            this.addressTextField5.setText(TLHDWalletWrapper.getAddress(extendedPublicKey, addressSequence5, appDelegate.appWallet.walletConfig.isTestnet));

            this.enableButton(this.showAddressQRCodeButton1, true);
            this.enableButton(this.showAddressQRCodeButton2, true);
            this.enableButton(this.showAddressQRCodeButton3, true);
            this.enableButton(this.showAddressQRCodeButton4, true);
            this.enableButton(this.showAddressQRCodeButton5, true);

            if (extendedPrivateKey != null) {
                this.privateKeyTextField1.setText(TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence1, appDelegate.appWallet.walletConfig.isTestnet));
                this.privateKeyTextField2.setText(TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence2, appDelegate.appWallet.walletConfig.isTestnet));
                this.privateKeyTextField3.setText(TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence3, appDelegate.appWallet.walletConfig.isTestnet));
                this.privateKeyTextField4.setText(TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence4, appDelegate.appWallet.walletConfig.isTestnet));
                this.privateKeyTextField5.setText(TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence5, appDelegate.appWallet.walletConfig.isTestnet));
                this.enableButton(this.showPrivateKeyQRCodeButton1, true);
                this.enableButton(this.showPrivateKeyQRCodeButton2, true);
                this.enableButton(this.showPrivateKeyQRCodeButton3, true);
                this.enableButton(this.showPrivateKeyQRCodeButton4, true);
                this.enableButton(this.showPrivateKeyQRCodeButton5, true);
            } else {
                this.privateKeyTextField1.setText(null);
                this.privateKeyTextField2.setText(null);
                this.privateKeyTextField3.setText(null);
                this.privateKeyTextField4.setText(null);
                this.privateKeyTextField5.setText(null);
                this.enableButton(this.showPrivateKeyQRCodeButton1, false);
                this.enableButton(this.showPrivateKeyQRCodeButton2, false);
                this.enableButton(this.showPrivateKeyQRCodeButton3, false);
                this.enableButton(this.showPrivateKeyQRCodeButton4, false);
                this.enableButton(this.showPrivateKeyQRCodeButton5, false);
            }
        }
    }

    void updateChangeAddressFields(Long startingAddressID) {
        String extendedPrivateKey = null;
        String extendedPublicKey = null;
        if (this.coldWalletKeyType == TLColdWalletKeyType.mnemonic) {
            extendedPublicKey = this.accountPublicKeyEditText.getText().toString();
            extendedPrivateKey = this.accountPrivateKeyEditText.getText().toString();
        } else if (this.coldWalletKeyType == TLColdWalletKeyType.accountPublicKey) {
            extendedPublicKey = this.accountPublicKeyEditText.getText().toString();
        } else if (this.coldWalletKeyType == TLColdWalletKeyType.accountPrivateKey) {
            extendedPublicKey = this.accountPublicKeyEditText.getText().toString();
            extendedPrivateKey = this.accountPrivateKeyEditText.getText().toString();
        }
        if (extendedPublicKey != null && TLHDWalletWrapper.isValidExtendedPublicKey(extendedPublicKey, appDelegate.appWallet.walletConfig.isTestnet)
        && (extendedPrivateKey == null || extendedPrivateKey != null && TLHDWalletWrapper.isValidExtendedPrivateKey(extendedPrivateKey, appDelegate.appWallet.walletConfig.isTestnet))) {
            this.enableTextField(this.startingChangeAddressIDTextField, true);

            Integer HDAddressIdx = startingAddressID.intValue();
            ArrayList<Integer> addressSequence1 = new ArrayList<Integer>(Arrays.asList((Integer) TLWalletJSONKeys.TLAddressType.Change.getValue(), HDAddressIdx));
            this.changeAddressLabel1.setText(getString(R.string.change_address_id_colon_number, HDAddressIdx));
            this.changeAddressTextField1.setText(TLHDWalletWrapper.getAddress(extendedPublicKey, addressSequence1, appDelegate.appWallet.walletConfig.isTestnet));
            HDAddressIdx += 1;
            ArrayList<Integer> addressSequence2 = new ArrayList<Integer>(Arrays.asList((Integer) TLWalletJSONKeys.TLAddressType.Change.getValue(), HDAddressIdx));
            this.changeAddressLabel2.setText(getString(R.string.change_address_id_colon_number, HDAddressIdx));
            this.changeAddressTextField2.setText(TLHDWalletWrapper.getAddress(extendedPublicKey, addressSequence2, appDelegate.appWallet.walletConfig.isTestnet));
            HDAddressIdx += 1;
            ArrayList<Integer> addressSequence3 = new ArrayList<Integer>(Arrays.asList((Integer) TLWalletJSONKeys.TLAddressType.Change.getValue(), HDAddressIdx));
            this.changeAddressLabel3.setText(getString(R.string.change_address_id_colon_number, HDAddressIdx));
            this.changeAddressTextField3.setText(TLHDWalletWrapper.getAddress(extendedPublicKey, addressSequence3, appDelegate.appWallet.walletConfig.isTestnet));
            HDAddressIdx += 1;
            ArrayList<Integer> addressSequence4 = new ArrayList<Integer>(Arrays.asList((Integer) TLWalletJSONKeys.TLAddressType.Change.getValue(), HDAddressIdx));
            this.changeAddressLabel4.setText(getString(R.string.change_address_id_colon_number, HDAddressIdx));
            this.changeAddressTextField4.setText(TLHDWalletWrapper.getAddress(extendedPublicKey, addressSequence4, appDelegate.appWallet.walletConfig.isTestnet));
            HDAddressIdx += 1;
            ArrayList<Integer> addressSequence5 = new ArrayList<Integer>(Arrays.asList((Integer) TLWalletJSONKeys.TLAddressType.Change.getValue(), HDAddressIdx));
            this.changeAddressLabel5.setText(getString(R.string.change_address_id_colon_number, HDAddressIdx));
            this.changeAddressTextField5.setText(TLHDWalletWrapper.getAddress(extendedPublicKey, addressSequence5, appDelegate.appWallet.walletConfig.isTestnet));

            this.enableButton(this.showChangeAddressQRCodeButton1, true);
            this.enableButton(this.showChangeAddressQRCodeButton2, true);
            this.enableButton(this.showChangeAddressQRCodeButton3, true);
            this.enableButton(this.showChangeAddressQRCodeButton4, true);
            this.enableButton(this.showChangeAddressQRCodeButton5, true);

            if (extendedPrivateKey != null) {
                this.changePrivateKeyTextField1.setText(TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence1, appDelegate.appWallet.walletConfig.isTestnet));
                this.changePrivateKeyTextField2.setText(TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence2, appDelegate.appWallet.walletConfig.isTestnet));
                this.changePrivateKeyTextField3.setText(TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence3, appDelegate.appWallet.walletConfig.isTestnet));
                this.changePrivateKeyTextField4.setText(TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence4, appDelegate.appWallet.walletConfig.isTestnet));
                this.changePrivateKeyTextField5.setText(TLHDWalletWrapper.getPrivateKey(extendedPrivateKey, addressSequence5, appDelegate.appWallet.walletConfig.isTestnet));
                this.enableButton(this.showChangePrivateKeyQRCodeButton1, true);
                this.enableButton(this.showChangePrivateKeyQRCodeButton2, true);
                this.enableButton(this.showChangePrivateKeyQRCodeButton3, true);
                this.enableButton(this.showChangePrivateKeyQRCodeButton4, true);
                this.enableButton(this.showChangePrivateKeyQRCodeButton5, true);
            } else {
                this.changePrivateKeyTextField1.setText(null);
                this.changePrivateKeyTextField2.setText(null);
                this.changePrivateKeyTextField3.setText(null);
                this.changePrivateKeyTextField4.setText(null);
                this.changePrivateKeyTextField5.setText(null);
                this.enableButton(this.showChangePrivateKeyQRCodeButton1, false);
                this.enableButton(this.showChangePrivateKeyQRCodeButton2, false);
                this.enableButton(this.showChangePrivateKeyQRCodeButton3, false);
                this.enableButton(this.showChangePrivateKeyQRCodeButton4, false);
                this.enableButton(this.showChangePrivateKeyQRCodeButton5, false);
            }
        }
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

    void hideKeyBoard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
