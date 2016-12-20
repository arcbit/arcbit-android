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
import android.widget.ImageView;
import android.view.View.OnClickListener;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLHDWalletWrapper;
import com.arcbit.arcbit.ui.utils.TLPrompts;

import com.arcbit.arcbit.R;

public class CreateColdWalletFragment extends android.support.v4.app.Fragment {
    private static final String TAG = CreateColdWalletFragment.class.getName();
    private TLAppDelegate appDelegate = TLAppDelegate.instance();

    private View rootView;
    private String mnemonicString;
    private String accountIDString;
    private String accountPublicKeyString;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_create_cold_wallet, container, false);
        if (appDelegate == null) {
            return rootView;
        }
        getActivity().setTitle(getString(R.string.create_cold_wallet));
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setupUIToHideKeyBoardOnTouch(rootView);

        final ImageView mnemonicInfoIconImageView = (ImageView)rootView.findViewById(R.id.mnemonic_info_image_view);
        final EditText mnemonicEditText = (EditText)rootView.findViewById(R.id.mnemonic_edit_text);
        final Button newWalletButton = (Button)rootView.findViewById(R.id.new_wallet_button);
        final EditText accountIDEditText = (EditText)rootView.findViewById(R.id.account_id_edit_text);
        final ImageView accountInfoIconImageView = (ImageView)rootView.findViewById(R.id.account_info_image_view);
        final EditText accountPublicKeyEditText = (EditText)rootView.findViewById(R.id.account_public_key_edit_text);
        final Button accountPublicKeyQRCodeButton = (Button)rootView.findViewById(R.id.account_public_key_qr_code_button);
        accountPublicKeyEditText.setEnabled(false);
        accountPublicKeyQRCodeButton.setEnabled(false);
        accountPublicKeyQRCodeButton.setAlpha(.5f);
        mnemonicEditText.setText(mnemonicString);
        accountIDEditText.setText(accountIDString);
        accountPublicKeyEditText.setText(accountPublicKeyString);

        mnemonicInfoIconImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TLPrompts.promptForOK(getActivity(), "", getString(R.string.create_cold_wallet_mnemonic_description), new TLPrompts.PromptOKCallback() {

                    @Override
                    public void onSuccess() {
                    }
                });
            }
        });
        mnemonicEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mnemonicString = s.toString();
                if (TLHDWalletWrapper.phraseIsValid(mnemonicString)) {
                    String masterHex = TLHDWalletWrapper.getMasterHex(mnemonicString);

                    String accountID = accountIDEditText.getText().toString();
                    Integer accountIdx;
                    try {
                        accountIdx = Integer.parseInt(accountID);
                    } catch (NumberFormatException exception) {
                        accountIdx = 0;
                        accountIDEditText.setText("0");
                    }
                    accountPublicKeyString = TLHDWalletWrapper.getExtendPubKeyFromMasterHex(masterHex, accountIdx, appDelegate.appWallet.walletConfig.isTestnet);
                    accountPublicKeyEditText.setText(accountPublicKeyString);
                    accountPublicKeyQRCodeButton.setEnabled(true);
                    accountPublicKeyQRCodeButton.setAlpha(1.0f);
                } else {
                    accountPublicKeyString = null;
                    accountPublicKeyEditText.setText(null);
                    accountPublicKeyQRCodeButton.setEnabled(false);
                    accountPublicKeyQRCodeButton.setAlpha(.5f);
                }
            }
        });
        newWalletButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mnemonicString = TLHDWalletWrapper.generateMnemonicPassphrase();
                String masterHex = TLHDWalletWrapper.getMasterHex(mnemonicString);

                mnemonicEditText.setText(mnemonicString);
                String accountID = accountIDEditText.getText().toString();
                Integer accountIdx;
                try {
                    accountIdx = Integer.parseInt(accountID);
                } catch (NumberFormatException exception) {
                    accountIdx = 0;
                    accountIDEditText.setText("0");
                }
                accountPublicKeyString = TLHDWalletWrapper.getExtendPubKeyFromMasterHex(masterHex, accountIdx, appDelegate.appWallet.walletConfig.isTestnet);
                accountPublicKeyEditText.setText(accountPublicKeyString);
                accountPublicKeyQRCodeButton.setEnabled(true);
                accountPublicKeyQRCodeButton.setAlpha(1.0f);
            }
        });
        accountIDEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                accountIDString = s.toString();
                if (mnemonicString != null && TLHDWalletWrapper.phraseIsValid(mnemonicString)) {
                    Integer accountIdx;
                    try {
                        accountIdx = Integer.parseInt(accountIDString);
                    } catch (NumberFormatException exception) {
                        accountIdx = 0;
                        accountIDEditText.setText("0");
                    }
                    String masterHex = TLHDWalletWrapper.getMasterHex(mnemonicString);
                    accountPublicKeyString = TLHDWalletWrapper.getExtendPubKeyFromMasterHex(masterHex, accountIdx, appDelegate.appWallet.walletConfig.isTestnet);
                    accountPublicKeyEditText.setText(accountPublicKeyString);
                    accountPublicKeyQRCodeButton.setEnabled(true);
                    accountPublicKeyQRCodeButton.setAlpha(1.0f);
                } else {
                    accountPublicKeyQRCodeButton.setEnabled(false);
                    accountPublicKeyQRCodeButton.setAlpha(.5f);
                }
            }
        });
        accountInfoIconImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TLPrompts.promptForOK(getActivity(), "", getString(R.string.create_cold_wallet_account_description), new TLPrompts.PromptOKCallback() {

                    @Override
                    public void onSuccess() {
                    }
                });
            }
        });
        accountPublicKeyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                accountPublicKeyString = s.toString();
            }
        });
        accountPublicKeyQRCodeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String accountPublicKey = accountPublicKeyEditText.getText().toString();
                if (accountPublicKey != null) {
                    TLPrompts.promptQRCodeDialogCopyToClipboard(getActivity(), accountPublicKey);
                }
            }
        });

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
