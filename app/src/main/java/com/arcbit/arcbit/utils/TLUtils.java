package com.arcbit.arcbit.utils;

import android.content.Context;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateFormat;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TLUtils {
    public static JSONArray removeFromJSONArray(JSONArray jsonArray, int idx) {
        try {
            ArrayList<Object> list = new ArrayList<Object>();
            for (int i = 0; i < jsonArray.length() ;i++) {
                list.add(jsonArray.get(i));
            }
            list.remove(idx);
            JSONArray jsArray = new JSONArray(list);
            return jsArray;
        } catch (JSONException e) {
            return null;
        }
    }

    public static JSONArray concatArray(JSONArray... arrs)
            throws JSONException {
        JSONArray result = new JSONArray();
        for (JSONArray arr : arrs) {
            for (int i = 0; i < arr.length(); i++) {
                result.put(arr.get(i));
            }
        }
        return result;
    }

    public static Long daysSinceDate(Date date) {
        Date nowDate = new Date();
        long diff = nowDate.getTime() - date.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }

    public static String getFormattedDate(long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis);

        Calendar now = Calendar.getInstance();

        final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "EEEE, MMMM d, h:mm aa";
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE) ) {
            return DateFormat.format(timeFormatString, smsTime).toString();
//            return "Today " + DateFormat.format(timeFormatString, smsTime);
//        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
//            return "Yesterday " + DateFormat.format(timeFormatString, smsTime);
        } else if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        } else {
            return DateFormat.format("MMMM dd yyyy, h:mm aa", smsTime).toString();
        }
    }

    public static boolean isCameraOpen() {
        Camera camera = null;

        try {
            camera = Camera.open();
        } catch (RuntimeException e) {
            return true;
        } finally {
            if (camera != null) {
                camera.release();
            }
        }

        return false;
    }

    public static boolean haveInternetConnection(Context ctx) {
        boolean ret = false;

        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo neti = cm.getActiveNetworkInfo();
            if (neti != null && neti.isConnectedOrConnecting()) {
                ret = true;
            }
        }

        return ret;
    }
}
