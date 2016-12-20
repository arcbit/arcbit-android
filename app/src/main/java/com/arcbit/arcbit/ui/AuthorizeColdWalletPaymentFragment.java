package com.arcbit.arcbit.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLColdWallet;
import com.arcbit.arcbit.model.TLHDWalletWrapper;
import com.arcbit.arcbit.ui.utils.TLPrompts;

import com.arcbit.arcbit.R;
import com.arcbit.arcbit.ui.utils.TLToast;
import com.arcbit.arcbit.utils.TLPermissionUtil;
import com.arcbit.arcbit.utils.TLUtils;
import com.google.zxing.client.android.CaptureActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthorizeColdWalletPaymentFragment extends android.support.v4.app.Fragment {
    private static final String TAG = AuthorizeColdWalletPaymentFragment.class.getName();
    private static final int SCAN_UNSIGNED_TX = 1300;
    private TLAppDelegate appDelegate = TLAppDelegate.instance();

    private View rootView;
    private TextView scanStatusTextView;
    private EditText mnemonicEditText;
    private TextView mnemonicStatusTextView;
    private Button passButton;

    private Map<Integer, String> scannedUnsignedTxAirGapDataPartsDict = new HashMap<>(); 
    private int totalExpectedParts = 0;
    private String scannedUnsignedTxAirGapData;
    private List<String> airGapDataBase64PartsArray;
    private List<String> savedAirGapDataBase64PartsArray;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_authorize_cold_wallet_payment, container, false);
        if (appDelegate == null) {
            return rootView;
        }

        getActivity().setTitle(getString(R.string.authorize_payment));
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setupUIToHideKeyBoardOnTouch(rootView);

        ImageView scanInfoImageView = (ImageView)rootView.findViewById(R.id.scan_info_image_view);
        Button scanButton = (Button)rootView.findViewById(R.id.scan_button);
        this.scanStatusTextView = (TextView)rootView.findViewById(R.id.scan_status_text_view);
        ImageView mnemonicInfoImageView = (ImageView)rootView.findViewById(R.id.mnemonic_info_image_view);
        this.mnemonicEditText = (EditText)rootView.findViewById(R.id.mnemonic_edit_text);
        this.mnemonicStatusTextView = (TextView)rootView.findViewById(R.id.mnemonic_status_text_view);
        ImageView passTxInfoImageView = (ImageView)rootView.findViewById(R.id.pass_tx_info_image_view);
        this.passButton = (Button)rootView.findViewById(R.id.pass_button);

        this.passButton.setEnabled(false);
        this.passButton.setAlpha(0.5f);
        this.setTextViewTextColorEnabled(mnemonicStatusTextView, false);
        this.setTextViewTextColorEnabled(scanStatusTextView, false);

        scanInfoImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TLPrompts.promptForOK(getActivity(), "", getString(R.string.scan_transaction_to_authorize_info), new TLPrompts.PromptOKCallback() {
                    @Override
                    public void onSuccess() {
                    }
                });
            }
        });
        mnemonicInfoImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TLPrompts.promptForOK(getActivity(), "", getString(R.string.input_12_word_backup_passphrase_info), new TLPrompts.PromptOKCallback() {
                    @Override
                    public void onSuccess() {
                    }
                });
            }
        });
        passTxInfoImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TLPrompts.promptForOK(getActivity(), "", getString(R.string.pass_authorized_transaction_data_info), new TLPrompts.PromptOKCallback() {
                    @Override
                    public void onSuccess() {
                    }
                });
            }
        });

        scanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                scanQRCode(SCAN_UNSIGNED_TX);
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
                checkToCreateSignedTx();
            }
        });
        passButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (airGapDataBase64PartsArray == null) {
                    return;
                }
                TLPrompts.promptForOK(getActivity(), getString(R.string.transaction_authorized), getString(R.string.transaction_authorized_next_step_description), new TLPrompts.PromptOKCallback() {

                    @Override
                    public void onSuccess() {
                        showNextSignedTxPartQRCode();
                    }
                });
            }
        });

        return rootView;
    }

    void setTextViewTextColorEnabled(TextView textView, boolean enabled) {
        if (enabled) {
            textView.setTextColor(Color.GREEN);
        } else {
            textView.setTextColor(Color.RED);
        }
    }

    void showNextSignedTxPartQRCode() {
        if (this.airGapDataBase64PartsArray == null) {
            return;
        }
        String nextAipGapDataPart = this.airGapDataBase64PartsArray.get(0);
        this.airGapDataBase64PartsArray.remove(0);
        TLPrompts.promptQRCodeDialog(getActivity(), nextAipGapDataPart, getString(R.string.next), new TLPrompts.PromptCallback() {
            @Override
            public void onSuccess(Object obj) {
                if (airGapDataBase64PartsArray.size() > 0) {
                    showNextSignedTxPartQRCode();
                } else {
                    TLPrompts.promptYesNo(getActivity(), getString(R.string.finished_passing_transaction_data), getString(R.string.finished_passing_transaction_data_description), getString(R.string.continue_capitalize), getString(R.string.cancel), new TLPrompts.PromptCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            airGapDataBase64PartsArray = new ArrayList<String>();
                            for (String part : savedAirGapDataBase64PartsArray) {
                                airGapDataBase64PartsArray.add(part);
                            }
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
                }
            }

            @Override
            public void onCancel() {
                airGapDataBase64PartsArray = new ArrayList<String>();
                for (String part : savedAirGapDataBase64PartsArray) {
                    airGapDataBase64PartsArray.add(part);
                }
            }
        });
    }

    void checkToCreateSignedTx() {
        String keyText = this.mnemonicEditText.getText().toString();
        if (keyText.isEmpty()) {
            this.mnemonicStatusTextView.setText(getString(R.string.incomplete));
            this.setTextViewTextColorEnabled(this.mnemonicStatusTextView, false);
            this.passButton.setEnabled(false);
            this.passButton.setAlpha(0.5f);
            return;
        }
        if (!TLHDWalletWrapper.phraseIsValid(keyText) && !TLHDWalletWrapper.isValidExtendedPrivateKey(keyText, appDelegate.appWallet.walletConfig.isTestnet)) {
            this.mnemonicStatusTextView.setText(getString(R.string.invalid_passphrase));
            this.setTextViewTextColorEnabled(this.mnemonicStatusTextView, false);
            this.passButton.setAlpha(0.5f);
            this.passButton.setEnabled(false);
            return;
        }
        if (this.scannedUnsignedTxAirGapData == null) {
            this.mnemonicStatusTextView.setText(getString(R.string.complete_step_1));
            this.setTextViewTextColorEnabled(this.mnemonicStatusTextView, false);
            this.passButton.setEnabled(false);
            this.passButton.setAlpha(0.5f);
            return;
        }

        try {
            String serializedSignedAipGapData = TLColdWallet.createSerializedSignedTxAipGapData(this.scannedUnsignedTxAirGapData,
                    keyText, appDelegate.appWallet.walletConfig.isTestnet);
            this.airGapDataBase64PartsArray = TLColdWallet.splitStringToArray(serializedSignedAipGapData);
            this.savedAirGapDataBase64PartsArray = TLColdWallet.splitStringToArray(serializedSignedAipGapData);
            this.mnemonicStatusTextView.setText(getString(R.string.complete));
            this.setTextViewTextColorEnabled(this.mnemonicStatusTextView, true);
            this.passButton.setEnabled(true);
            this.passButton.setAlpha(1.0f);
        } catch (TLColdWallet.TLInvalidScannedData e) { //shouldn't happen, if user scanned correct QR codes
            Log.d(TAG, "TLColdWallet InvalidScannedData " + e);
            this.airGapDataBase64PartsArray = null;
            this.savedAirGapDataBase64PartsArray = null;
            this.mnemonicStatusTextView.setText(getString(R.string.invalid_scanned_data));
            this.setTextViewTextColorEnabled(this.mnemonicStatusTextView, false);
            this.passButton.setEnabled(false);
            this.passButton.setAlpha(0.5f);
        } catch (TLColdWallet.TLColdWalletInvalidKey e) {
            Log.d(TAG, "TLColdWallet InvalidKey " + e);
            this.airGapDataBase64PartsArray = null;
            this.savedAirGapDataBase64PartsArray = null;
            this.mnemonicStatusTextView.setText(getString(R.string.invalid_passphrase));
            this.setTextViewTextColorEnabled(this.mnemonicStatusTextView, false);
            this.passButton.setEnabled(false);
            this.passButton.setAlpha(0.5f);
        } catch (TLColdWallet.TLColdWalletMisMatchExtendedPublicKey e) {
            Log.d(TAG, "TLColdWallet MisMatchExtendedPublicKey " + e);
            this.airGapDataBase64PartsArray = null;
            this.savedAirGapDataBase64PartsArray = null;
            this.mnemonicStatusTextView.setText(getString(R.string.passphrase_does_not_match_the_transaction));
            this.mnemonicStatusTextView.setText(getString(R.string.invalid_passphrase));
            this.setTextViewTextColorEnabled(this.mnemonicStatusTextView, false);
            this.passButton.setEnabled(false);
            this.passButton.setAlpha(0.5f);
        } catch (Exception e) {
            Log.d(TAG, "TLColdWallet error " + e);
        }
        
    }
    public void didScanUnsignedTxPart() {
        if (this.totalExpectedParts != 0 && this.scannedUnsignedTxAirGapDataPartsDict.size() == this.totalExpectedParts) {
            this.scannedUnsignedTxAirGapData = "";
            for (int i = 1; i <= this.totalExpectedParts; i++) {
                String dataPart = this.scannedUnsignedTxAirGapDataPartsDict.get(Integer.valueOf(i));
                this.scannedUnsignedTxAirGapData = this.scannedUnsignedTxAirGapData + dataPart;
            }
            this.scannedUnsignedTxAirGapDataPartsDict = new HashMap<>();
            this.checkToCreateSignedTx();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_UNSIGNED_TX
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {

            String value = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            List<Object> ret = TLColdWallet.parseScannedPart(value);
            String dataPart = (String) ret.get(0);
            Integer partNumber = (Integer) ret.get(1);
            Integer totalParts = (Integer) ret.get(2);

            this.totalExpectedParts = totalParts;
            this.scannedUnsignedTxAirGapDataPartsDict.put(partNumber, dataPart);

            Integer partsScanned = this.scannedUnsignedTxAirGapDataPartsDict.size();
            if (partsScanned == 0 && totalParts == 0) {
                this.setTextViewTextColorEnabled(scanStatusTextView, false);
                this.scanStatusTextView.setText(getString(R.string.incomplete));
            } else if (partsScanned < totalParts) {
                this.setTextViewTextColorEnabled(scanStatusTextView, false);
                this.scanStatusTextView.setText(getString(R.string.x_over_y_complete, partsScanned, totalParts));
            } else {
                this.setTextViewTextColorEnabled(scanStatusTextView, true);
                this.scanStatusTextView.setText(getString(R.string.x_over_y_complete, partsScanned, totalParts));
            }
            didScanUnsignedTxPart();
        }
    }

    private void scanQRCode(int requestCode) {
        if (ContextCompat.checkSelfPermission(appDelegate.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            TLPermissionUtil.requestCameraPermissionFromActivity(rootView, getActivity());
        } else {
            startScanActivity(requestCode);
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
