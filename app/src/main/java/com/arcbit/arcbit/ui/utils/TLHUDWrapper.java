package com.arcbit.arcbit.ui.utils;

import android.app.Activity;
import android.app.ProgressDialog;

import com.arcbit.arcbit.R;

public class TLHUDWrapper {
    private static ProgressDialog progress = null;

    public static void showHUD(Activity activity, String message) {
        progress = new ProgressDialog(activity);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(message);
        if(!activity.isFinishing()) {
            progress.show();
        }
    }

    public static void hideHUD() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
    }
}
