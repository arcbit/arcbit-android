package com.arcbit.arcbit.model;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.MotionEvent;

import com.arcbit.arcbit.R;
import com.arcbit.arcbit.ui.utils.TLPrompts;
import com.arcbit.arcbit.utils.TLUtils;

import java.util.Date;

public class TLSuggestions {
    TLAppDelegate appDelegate;
    int VIEW_RECEIVE_SCREEN_GAP_COUNT_TO_SHOW_SUGGESTION_TO_ENABLE_PIN = 13;

    // use prime numbers to avoid multiple prompts to be displayed at once
    int VIEW_SEND_SCREEN_GAP_COUNT_TO_SHOW_SUGGESTION_TO_BACKUP_WALLET_PASSPHRASE = 3;
    int VIEW_SEND_SCREEN_GAP_COUNT_TO_SHOW_WEB_WALLET = 31;
    int VIEW_SEND_SCREEN_GAP_COUNT_TO_SHOW_TRY_COLD_WALLET = 37;
    int VIEW_SEND_SCREEN_GAP_COUNT_TO_SHOW_RATE_APP_ONCE = 47;
    int VIEW_SEND_SCREEN_GAP_COUNT_TO_SHOW_RATE_APP = 89;
    private AlertDialog alertDialog = null;

    TLSuggestions(TLAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
    }

    public boolean checkToShowHiddenOverlayWarning(Context context, MotionEvent event) {
        // https://blog.lookout.com/look-10-007-tapjacking/
        if (!appDelegate.preferences.disabledShowHiddenOverlayWarning() && (event.getFlags() & MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0) {

            if(alertDialog != null)
                alertDialog.dismiss();

            alertDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.screen_overlay_detected)
                    .setMessage(R.string.screen_overlay_description)
                    .setCancelable(false)
                    .setPositiveButton(R.string.continue_capitalize, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            appDelegate.preferences.setDisabledShowHiddenOverlayWarning(true);
                            dialog.dismiss();
                        }
                    }).setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            ((Activity) context).finish();
                        }
                    }).show();
            return true;
        }else {
            return false;
        }
    }

    public boolean disabledShowIsRootedWarning() {
        return this.appDelegate.preferences.disabledShowIsRootedWarning();
    }

    public boolean setDisabledShowIsRootedWarning(boolean disabled) {
        return this.appDelegate.preferences.setDisabledShowIsRootedWarning(disabled);
    }

    public boolean disabledShowStealthPaymentNote() {
        return this.appDelegate.preferences.disabledShowStealthPaymentNote();
    }

    public boolean setDisableShowStealthPaymentNote(boolean disabled) {
        return this.appDelegate.preferences.setDisableShowStealthPaymentNote(disabled);
    }

    public boolean disabledShowStealthPaymentDelayInfo() {
        return this.appDelegate.preferences.disabledShowStealthPaymentDelayInfo();
    }

    public boolean setDisableShowStealthPaymentDelayInfo(boolean disabled) {
        return this.appDelegate.preferences.setDisableShowStealthPaymentDelayInfo(disabled);
    }

    public boolean disabledShowManuallyScanTransactionForStealthTxInfo() {
        return this.appDelegate.preferences.disabledShowManuallyScanTransactionForStealthTxInfo();
    }

    public boolean setDisableShowManuallyScanTransactionForStealthTxInfo(boolean disabled) {
        return this.appDelegate.preferences.setDisableShowManuallyScanTransactionForStealthTxInfo(disabled);
    }

    public boolean conditionToPromptRateAppSatisfied() {
        int viewSendScreenCount = appDelegate.analytics.getEventCount(TLNotificationEvents.EVENT_VIEW_SEND_SCREEN);
        if (!appDelegate.preferences.disabledPromptRateApp() &&
                viewSendScreenCount > 0 &&
                ((!appDelegate.preferences.hasRatedOnce() && viewSendScreenCount % VIEW_SEND_SCREEN_GAP_COUNT_TO_SHOW_RATE_APP_ONCE == 0) ||
                        (appDelegate.preferences.hasRatedOnce() && viewSendScreenCount % VIEW_SEND_SCREEN_GAP_COUNT_TO_SHOW_RATE_APP == 0))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean conditionToPromptShowWebWallet() {
        int viewSendScreenCount = appDelegate.analytics.getEventCount(TLNotificationEvents.EVENT_VIEW_SEND_SCREEN);
        if (!appDelegate.preferences.disabledPromptShowWebWallet() &&
                viewSendScreenCount > 0 &&
                viewSendScreenCount % VIEW_SEND_SCREEN_GAP_COUNT_TO_SHOW_WEB_WALLET == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean conditionToPromptShowTryColdWallet() {
        int viewSendScreenCount = appDelegate.analytics.getEventCount(TLNotificationEvents.EVENT_VIEW_SEND_SCREEN);
        Date installDate = appDelegate.preferences.getInstallDate();

        //debug
//        SimpleDateFormat myFormat = new SimpleDateFormat("dd MM yyyy");
//        try {
//            installDate = myFormat.parse("10 9 2016");
//        } catch (ParseException e) {
//        }

        if (installDate == null) {
            return false;
        }
        if (!appDelegate.preferences.disabledPromptShowTryColdWallet() &&
                !appDelegate.preferences.enabledColdWallet() &&
                TLUtils.daysSinceDate(installDate) > 60 && //60 days
                viewSendScreenCount > 0 &&
                viewSendScreenCount % VIEW_SEND_SCREEN_GAP_COUNT_TO_SHOW_TRY_COLD_WALLET == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean disabledShowLowAndroidVersionWarning() {
        return this.appDelegate.preferences.disabledShowLowAndroidVersionWarning();
    }

    public boolean setDisabledShowLowAndroidVersionWarning(boolean disabled) {
        return this.appDelegate.preferences.setDisabledShowLowAndroidVersionWarning(disabled);
    }

    public void promptToShowLowAndroidVersionWarning(Activity activity)  {
        TLPrompts.promptForOK(activity, activity.getResources().getString(R.string.warning), activity.getResources().getString(R.string.old_android_warning), new TLPrompts.PromptOKCallback() {
            @Override
            public void onSuccess() {
                setDisabledShowLowAndroidVersionWarning(true);
            }
        });
    }

    public boolean setDisableSuggestedEnablePin(boolean disabled) {
        return this.appDelegate.preferences.setDisableSuggestedEnablePin(disabled);
    }

    public boolean disabledSuggestedEnablePin() {
        return this.appDelegate.preferences.disabledSuggestedEnablePin();
    }

    public boolean conditionToPromptToSuggestEnablePinSatisfied() {
        int viewReceiveScreenCount = appDelegate.analytics.getEventCount(TLNotificationEvents.EVENT_VIEW_RECEIVE_SCREEN);
        if (!disabledSuggestedEnablePin() &&
                viewReceiveScreenCount > 0 &&
                viewReceiveScreenCount % VIEW_RECEIVE_SCREEN_GAP_COUNT_TO_SHOW_SUGGESTION_TO_ENABLE_PIN == 0) {
            return true;
        } else {
            return false;
        }
    }

    public void promptToSuggestEnablePin(Activity activity)  {
        TLPrompts.promptYesNo(activity, activity.getResources().getString(R.string.enable_pin_code), activity.getResources().getString(R.string.enable_pin_code_to_better_secure),
                activity.getResources().getString(R.string.dont_remind_me), activity.getResources().getString(R.string.remind_me_later), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        setDisableSuggestedEnablePin(true);
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    public boolean setDisabledSuggestedBackUpWalletPassphrase(boolean disabled) {
        return this.appDelegate.preferences.setDisableSuggestedBackUpWalletPassphrase(disabled);
    }

    public boolean disabledSuggestedBackUpWalletPassphrase() {
        return this.appDelegate.preferences.disabledSuggestedBackUpWalletPassphrase();
    }

    public boolean conditionToPromptToSuggestedBackUpWalletPassphraseSatisfied() {
        int viewSendScreenCount = appDelegate.analytics.getEventCount(TLNotificationEvents.EVENT_VIEW_SEND_SCREEN);
        if (!disabledSuggestedBackUpWalletPassphrase() &&
                viewSendScreenCount > 0 &&
                viewSendScreenCount % VIEW_SEND_SCREEN_GAP_COUNT_TO_SHOW_SUGGESTION_TO_BACKUP_WALLET_PASSPHRASE == 0) {
            return true;
        } else {
            return false;
        }
    }

    public void promptToSuggestBackUpWalletPassphrase(Activity activity) {
        TLPrompts.promptYesNo(activity, activity.getResources().getString(R.string.back_up_wallet), activity.getResources().getString(R.string.write_down_backup_passphrase_suggestion),
                activity.getResources().getString(R.string.dont_remind_me), activity.getResources().getString(R.string.remind_me_later), new TLPrompts.PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        setDisabledSuggestedBackUpWalletPassphrase(true);
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    public boolean setDisableSuggestDontManageIndividualAccountAddress(boolean disabled)  {
        return this.appDelegate.preferences.setDisableSuggestDontManageIndividualAccountAddress(disabled);
    }

    public boolean disabledSuggestDontManageIndividualAccountAddress() {
        return this.appDelegate.preferences.disabledSuggestDontManageIndividualAccountAddress();
    }


    public boolean setDisableSuggestDontManageIndividualAccountPrivateKeys(boolean disabled)  {
        return this.appDelegate.preferences.setDisableSuggestDontManageIndividualAccountPrivateKeys(disabled);
    }

    public boolean disabledSuggestDontManageIndividualAccountPrivateKeys() {
        return this.appDelegate.preferences.disabledSuggestDontManageIndividualAccountPrivateKeys();
    }

    public boolean setDisableSuggestDontAddNormalAddressToAddressBook(boolean disabled)  {
        return this.appDelegate.preferences.setDisableSuggestDontAddNormalAddressToAddressBook(disabled);
    }

    public boolean disabledSuggestDontAddNormalAddressToAddressBook() {
        return this.appDelegate.preferences.disabledSuggestDontAddNormalAddressToAddressBook();
    }
}
