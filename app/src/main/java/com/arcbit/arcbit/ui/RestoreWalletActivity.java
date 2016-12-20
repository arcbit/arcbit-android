package com.arcbit.arcbit.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLHDWalletWrapper;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.model.TLWalletUtils;

import com.arcbit.arcbit.R;
import com.arcbit.arcbit.ui.utils.TLHUDWrapper;
import com.arcbit.arcbit.ui.utils.TLPrompts;

public class RestoreWalletActivity extends Activity {
    private static final String TAG = RestoreWalletActivity.class.getName();

    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    private ImageButton backImageButton;
    private EditText passphraseEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_wallet);

        backImageButton = (ImageButton)findViewById(R.id.backImageButton);
        backImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        passphraseEditText = (EditText) findViewById(R.id.passphrase_edittext);
        passphraseEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        passphraseEditText.setImeActionLabel(getString(R.string.done), KeyEvent.KEYCODE_ENTER);

        passphraseEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "passphraseEditText setOnKeyListener: keyCode " + keyCode + " =? " + KeyEvent.KEYCODE_ENTER);
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    Log.d(TAG, "passphraseEditText setOnKeyListener: KEYCODE_ENTER ");
                    String passphrase = passphraseEditText.getText().toString();
                    passphrase = passphrase.trim();
                    checkPassphraseAndShowPromptToRestoreWallet(passphrase);
                    return true;
                }
                return false;
            }
        });
        passphraseEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "passphraseEditText onEditorAction: " + actionId + " =? " + EditorInfo.IME_ACTION_DONE);
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.d(TAG, "passphraseEditText onEditorAction: IME_ACTION_DONE ");
                    String passphrase = passphraseEditText.getText().toString();
                    passphrase = passphrase.trim();
                    checkPassphraseAndShowPromptToRestoreWallet(passphrase);
                    return true;
                }
                return false;
            }
        });
    }

    void checkPassphraseAndShowPromptToRestoreWallet(String passphrase) {
        if (passphrase == null) {
            TLPrompts.promptErrorMessage(RestoreWalletActivity.this, getString(R.string.error), getString(R.string.invalid_backup_passphrase));
            return;
        }
        passphrase = passphrase.trim();
        if (TLHDWalletWrapper.phraseIsValid(passphrase)) {
            showPromptToRestoreWallet(passphrase);
        } else {
            TLPrompts.promptErrorMessage(RestoreWalletActivity.this, getString(R.string.error), getString(R.string.invalid_backup_passphrase));
        }
    }

    void handleAfterRecoverWallet(String mnemonicPassphrase) {
        appDelegate.updateGodSend(TLWalletUtils.TLSendFromType.HDWallet, 0);
        appDelegate.updateReceiveSelectedObject(TLWalletUtils.TLSendFromType.HDWallet, 0);
        appDelegate.updateHistorySelectedObject(TLWalletUtils.TLSendFromType.HDWallet, 0);

        appDelegate.saveWalletJson();

        LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_RESTORE_WALLET));
    }

    void showPromptToRestoreWallet(String mnemonicPassphrase) {
        TLPrompts.promptYesNo(RestoreWalletActivity.this, getString(R.string.restore_wallet),
                getString(R.string.restor_wallet_explanation), getString(R.string.continue_capitalize),
                getString(R.string.cancel), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        recoverWallet(mnemonicPassphrase);
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    private void recoverWallet(String mnemonicPassphrase) {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                appDelegate.transactionListener.reconnect();
                appDelegate.stealthWebSocket.reconnect();
                TLHUDWrapper.hideHUD();
                TLPrompts.promptForOK(RestoreWalletActivity.this , getString(R.string.your_wallet_is_now_restored), "", new TLPrompts.PromptOKCallback() {
                    @Override
                    public void onSuccess() {
                        finish();
                    }
                });
            }
        };

        TLHUDWrapper.showHUD(RestoreWalletActivity.this, getString(R.string.restoring_wallet));
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message message = Message.obtain();
                appDelegate.saveWalletJSONEnabled = false;
                appDelegate.recoverHDWallet(mnemonicPassphrase, false);
                appDelegate.refreshHDWalletAccounts(true);
                appDelegate.refreshApp(mnemonicPassphrase, false);
                appDelegate.saveWalletJSONEnabled = true;
                handleAfterRecoverWallet(mnemonicPassphrase);
                message.obj = true;
                handler.sendMessage(Message.obtain(message));
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        TLHUDWrapper.hideHUD();
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return appDelegate.suggestions.checkToShowHiddenOverlayWarning(this, event) || super.dispatchTouchEvent(event);
    }
}
