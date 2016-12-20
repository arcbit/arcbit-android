package com.arcbit.arcbit.utils;

import android.content.Context;
import android.content.Intent;

import com.arcbit.arcbit.ui.MainActivity;
import com.arcbit.arcbit.ui.utils.TLToast;

import java.security.Security;

import com.arcbit.arcbit.R;

public class TLAppUtil {

    private static TLAppUtil instance = null;
    private static Context context = null;

    public static final String LOGOUT_ACTION = "com.arcbit.arcbit.LOGOUT";

    private TLAppUtil() {
    }

    public static TLAppUtil getInstance(Context ctx) {
        context = ctx;
        if (instance == null) {
            instance = new TLAppUtil();
        }
        return instance;
    }

    public static void restartApp() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(LOGOUT_ACTION);
        context.startActivity(intent);
    }

    public void applyPRNGFixes() {
        try {
            PRNGFixes.apply();
        } catch (Exception e0) {
            //
            // some Android 4.0 devices throw an exception when PRNGFixes is re-applied
            // removing provider before apply() is a workaround
            //
            Security.removeProvider("LinuxPRNG");
            try {
                PRNGFixes.apply();
            } catch (Exception e1) {
                TLToast.makeText(context, context.getString(R.string.cannot_use_app_on_this_device), TLToast.LENGTH_LONG, TLToast.TYPE_ERROR);
                TLAppUtil.restartApp();
            }
        }
    }
}
