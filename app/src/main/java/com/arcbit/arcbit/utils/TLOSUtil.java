package com.arcbit.arcbit.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

public class TLOSUtil {

    private static TLOSUtil instance = null;
    private static Context context = null;

    private TLOSUtil() {
    }

    public static TLOSUtil getInstance(Context ctx) {
        context = ctx;
        if (instance == null) {
            instance = new TLOSUtil();
        }
        return instance;
    }

    public boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo s : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(s.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}
