package com.arcbit.arcbit.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLHDWalletWrapper;

import com.arcbit.arcbit.R;

public class PassphraseActivity extends Activity {

    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    private ImageButton backImageButton;
    private TextView passphraseDescriptionTextView;
    private TextView passphraseTextView;
    private TextView masterSeedHexTitleTextView;
    private TextView masterSeedHexDescriptionTextView;
    private TextView masterSeedHexTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_passphrase);
        backImageButton = (ImageButton)findViewById(R.id.backImageButton);
        backImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        passphraseDescriptionTextView = (TextView)findViewById(R.id.passphrase_description);
        passphraseTextView = (TextView)findViewById(R.id.passphrase);

        masterSeedHexTitleTextView = (TextView)findViewById(R.id.master_seed_title);
        masterSeedHexDescriptionTextView = (TextView)findViewById(R.id.master_seed_description);
        masterSeedHexTextView = (TextView)findViewById(R.id.master_seed);


        String passphrase = appDelegate.encryptedPreferences.getWalletPassphrase();
        passphraseTextView.setText(passphrase);
        if (!appDelegate.preferences.enabledAdvancedMode()) {
            passphraseDescriptionTextView.setText(getString(R.string.passphrase_instructions));
            masterSeedHexTitleTextView.setVisibility(View.INVISIBLE);
            masterSeedHexDescriptionTextView.setVisibility(View.INVISIBLE);
            masterSeedHexTextView.setVisibility(View.INVISIBLE);
            masterSeedHexTextView.setText("");
        } else {
            passphraseDescriptionTextView.setText(getString(R.string.passphrase_instructions_advanced));
            masterSeedHexTitleTextView.setVisibility(View.VISIBLE);
            masterSeedHexDescriptionTextView.setVisibility(View.VISIBLE);
            masterSeedHexTextView.setVisibility(View.VISIBLE);
            masterSeedHexTextView.setText(TLHDWalletWrapper.getMasterHex(passphrase));
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return appDelegate.suggestions.checkToShowHiddenOverlayWarning(this, event) || super.dispatchTouchEvent(event);
    }
}
