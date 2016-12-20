package com.arcbit.arcbit.ui;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.ui.utils.TLToast;

import com.arcbit.arcbit.R;

public class PinFragment extends Fragment {

    public static String PIN_OPTION = "PIN_OPTION";
    public static String PREVIOUS_PIN_VALUE = "PREVIOUS_PIN_VALUE";

    public static String CREATE_NEW_PIN = "CREATE_NEW_PIN";
    public static String CREATE_NEW_PIN_REENTER_PIN = "CREATE_NEW_PIN_REENTER_PIN";

    public static String CHANGE_PIN = "CHANGE_PIN";
    public static String CHANGE_PIN_ENTER_NEW_PIN = "CHANGE_PIN_ENTER_NEW_PIN";
    public static String CHANGE_PIN_REENTER_NEW_PIN = "CHANGE_PIN_REENTER_NEW_PIN";

    public static String DISABLE_PIN = "DISABLE_PIN";
    public static String VERIFY_PIN = "VERIFY_PIN";

    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    private EditText pinEditText1;
    private EditText pinEditText2;
    private EditText pinEditText3;
    private EditText pinEditText4;
    private TextWatcher pinTextWatcher1;
    private TextWatcher pinTextWatcher2;
    private TextWatcher pinTextWatcher3;
    private TextWatcher pinTextWatcher4;
    private TextView pinInstructionTextView;
    private String pinOption;
    private String previousPinValue;
    boolean allowChangeEditTextFocus2 = true;
    boolean allowChangeEditTextFocus3 = true;
    boolean allowChangeEditTextFocus4 = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pin, container, false);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        pinInstructionTextView = (TextView) rootView.findViewById(R.id.pin_instruction);
        pinEditText1 = (EditText) rootView.findViewById(R.id.pin_entry_1);
        pinEditText2 = (EditText) rootView.findViewById(R.id.pin_entry_2);
        pinEditText3 = (EditText) rootView.findViewById(R.id.pin_entry_3);
        pinEditText4 = (EditText) rootView.findViewById(R.id.pin_entry_4);

        Bundle data = getArguments();
        if (data != null) {
            pinOption = data.getString(PIN_OPTION);
            previousPinValue = data.getString(PREVIOUS_PIN_VALUE);
        }

        String toolbarTitleText = null;
        String pinInstructionText = null;
        if (pinOption.equals(CREATE_NEW_PIN)) {
            toolbarTitleText = getString(R.string.create_pin);
            pinInstructionText = getString(R.string.pin_entry);
        } else if (pinOption.equals(CREATE_NEW_PIN_REENTER_PIN)) {
            toolbarTitleText = getString(R.string.create_pin);
            pinInstructionText = getString(R.string.reenter_new_pin);
        } else if (pinOption.equals(CHANGE_PIN)) {
            toolbarTitleText = getString(R.string.change_pin_code);
            pinInstructionText = getString(R.string.enter_current_pin);
        } else if (pinOption.equals(CHANGE_PIN_ENTER_NEW_PIN)) {
            toolbarTitleText = getString(R.string.change_pin_code);
            pinInstructionText = getString(R.string.enter_new_pin);
        } else if (pinOption.equals(CHANGE_PIN_REENTER_NEW_PIN)) {
            toolbarTitleText = getString(R.string.change_pin_code);
            pinInstructionText = getString(R.string.reenter_new_pin);
        } else if (pinOption.equals(DISABLE_PIN)) {
            toolbarTitleText = getString(R.string.disable_pin);
            pinInstructionText = getString(R.string.enter_current_pin);
        } else if (pinOption.equals(VERIFY_PIN)) {
            toolbarTitleText = "";
            pinInstructionText = getString(R.string.pin_entry);
        }
        ((PinActivity)getActivity()).toolbar.setTitle(toolbarTitleText);
        pinInstructionTextView.setText(pinInstructionText);

        pinTextWatcher1 = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    pinEditText2.requestFocus();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        pinTextWatcher2 = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    pinEditText3.requestFocus();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        pinTextWatcher3 = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    pinEditText4.requestFocus();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        pinTextWatcher4 = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                handleEnteredPINValue(getEnteredPINValue());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!s.toString().isEmpty()) {
                    allowChangeEditTextFocus4 = false;
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };

        pinEditText1.addTextChangedListener(pinTextWatcher1);
        pinEditText2.addTextChangedListener(pinTextWatcher2);
        pinEditText3.addTextChangedListener(pinTextWatcher3);
        pinEditText4.addTextChangedListener(pinTextWatcher4);


        pinEditText2.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(allowChangeEditTextFocus2 && keyCode == KeyEvent.KEYCODE_DEL && pinEditText2.getText().toString().isEmpty()) {
                    pinEditText1.setText("");
                    pinEditText1.requestFocus();
                }
                allowChangeEditTextFocus2 = true;
                return false;
            }
        });


        pinEditText3.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(allowChangeEditTextFocus3 && keyCode == KeyEvent.KEYCODE_DEL && pinEditText3.getText().toString().isEmpty()) {
                    allowChangeEditTextFocus2 = false;
                    pinEditText2.setText("");
                    pinEditText2.requestFocus();
                }
                allowChangeEditTextFocus3 = true;
                return false;
            }
        });


        pinEditText4.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(allowChangeEditTextFocus4 && keyCode == KeyEvent.KEYCODE_DEL && pinEditText4.getText().toString().isEmpty()) {
                    allowChangeEditTextFocus3 = false;
                    pinEditText3.setText("");
                    pinEditText3.requestFocus();
                }
                allowChangeEditTextFocus4 = true;
                return false;
            }
        });

        pinEditText1.requestFocus();
        return rootView;
    }

    String getEnteredPINValue() {
        if (!pinEditText1.getText().toString().isEmpty() &&
                !pinEditText2.getText().toString().isEmpty() &&
                !pinEditText3.getText().toString().isEmpty() &&
                !pinEditText4.getText().toString().isEmpty()) {
            return pinEditText1.getText().toString() + pinEditText2.getText().toString() +
                    pinEditText3.getText().toString() + pinEditText4.getText().toString();
        }

        return null;
    }

    void handleEnteredPINValue(String pinValue) {
        if (pinValue != null && pinValue.length() != 4) {
            return;
        }
        Bundle data = null;
        if (pinOption.equals(CREATE_NEW_PIN)) {
            data = new Bundle();
            data.putString(PIN_OPTION, CREATE_NEW_PIN_REENTER_PIN);
            data.putString(PREVIOUS_PIN_VALUE, pinValue);
        } else if (pinOption.equals(CREATE_NEW_PIN_REENTER_PIN)) {
            if (pinValue != null && previousPinValue != null && pinValue.equals(previousPinValue)) {
                appDelegate.encryptedPreferences.setPINValue(pinValue);
                appDelegate.preferences.setEnablePINCode(true);
                getActivity().finish();
            } else {
                TLToast.makeText(getActivity(), getString(R.string.pin_mismatch_error), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
                resetPinTextField();
            }
        } else if (pinOption.equals(CHANGE_PIN)) {
            if (pinValue != null && pinValue.equals(appDelegate.encryptedPreferences.getPINValue())) {
                data = new Bundle();
                data.putString(PIN_OPTION, CHANGE_PIN_ENTER_NEW_PIN);
            } else {
                TLToast.makeText(getActivity(), getString(R.string.invalid_pin), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
                resetPinTextField();
            }
        } else if (pinOption.equals(CHANGE_PIN_ENTER_NEW_PIN)) {
            data = new Bundle();
            data.putString(PIN_OPTION, CHANGE_PIN_REENTER_NEW_PIN);
            data.putString(PREVIOUS_PIN_VALUE, pinValue);
        } else if (pinOption.equals(CHANGE_PIN_REENTER_NEW_PIN)) {
            if (pinValue != null && previousPinValue != null && pinValue.equals(previousPinValue)) {
                appDelegate.encryptedPreferences.setPINValue(pinValue);
                getActivity().finish();
            } else {
                TLToast.makeText(getActivity(), getString(R.string.pin_mismatch_error), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
                resetPinTextField();
            }
        } else if (pinOption.equals(DISABLE_PIN)) {
            if (pinValue != null && pinValue.equals(appDelegate.encryptedPreferences.getPINValue())) {
                appDelegate.encryptedPreferences.setPINValue(null);
                appDelegate.preferences.setEnablePINCode(false);
                getActivity().finish();
            } else {
                TLToast.makeText(getActivity(), getString(R.string.invalid_pin), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
                resetPinTextField();
            }
        } else if (pinOption.equals(VERIFY_PIN)) {
            if (pinValue != null && pinValue.equals(appDelegate.encryptedPreferences.getPINValue())) {
                getActivity().finish();
            } else {
                TLToast.makeText(getActivity(), getString(R.string.invalid_pin), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
                resetPinTextField();
            }
        }

        if (data != null) {
            Fragment fragmentChild = new PinFragment();
            fragmentChild.setArguments(data);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.animator.right_slide_in, R.animator.left_slide_out, R.animator.left_slide_in, R.animator.right_slide_out);
            transaction.replace(R.id.content_frame, fragmentChild);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    void resetPinTextField() {
        pinEditText1.removeTextChangedListener(pinTextWatcher1);
        pinEditText2.removeTextChangedListener(pinTextWatcher2);
        pinEditText3.removeTextChangedListener(pinTextWatcher3);
        pinEditText4.removeTextChangedListener(pinTextWatcher4);
        pinEditText1.setText(null);
        pinEditText2.setText(null);
        pinEditText3.setText(null);
        pinEditText4.setText(null);
        pinEditText1.requestFocus();
        pinEditText1.addTextChangedListener(pinTextWatcher1);
        pinEditText2.addTextChangedListener(pinTextWatcher2);
        pinEditText3.addTextChangedListener(pinTextWatcher3);
        pinEditText4.addTextChangedListener(pinTextWatcher4);
    }
}
