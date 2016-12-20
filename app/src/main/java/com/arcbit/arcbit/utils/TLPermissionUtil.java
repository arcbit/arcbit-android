package com.arcbit.arcbit.utils;

import android.Manifest;
import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.arcbit.arcbit.R;

public class TLPermissionUtil {

    public static final int PERMISSION_REQUEST_CAMERA = 161;

    public static void requestCameraPermissionFromActivity(View parentView, final Activity activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {

            Snackbar.make(parentView, activity.getString(R.string.request_camera_permission),
                    Snackbar.LENGTH_INDEFINITE).setAction(activity.getString(R.string.ok_capitalize), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.CAMERA},
                            PERMISSION_REQUEST_CAMERA);
                }
            }).show();

        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        }
    }
}
