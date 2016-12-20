package com.arcbit.arcbit.ui.utils;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.arcbit.arcbit.R;

public class TLToast {

    public static final String TYPE_ERROR = "TYPE_ERROR";
    public static final String TYPE_INFO = "TYPE_INFO";
    public static final String TYPE_SUCCESS = "TYPE_SUCCESS";

    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    private static Toast toast = null;

    public static void makeText(final Context context, final CharSequence text, final int duration, final String type) {
        toast = Toast.makeText(context, text, duration);
        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflate.inflate(R.layout.custom_toast, null);
        TextView textView = (TextView) view.findViewById(R.id.msg);
        textView.setText(text);
        switch (type) {
            case TYPE_ERROR:
                textView.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_view_toast_error));
                textView.setTextColor(ContextCompat.getColor(context, R.color.toast_error_text));
                break;
            case TYPE_INFO:
                textView.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_view_toast_info));
                textView.setTextColor(ContextCompat.getColor(context, R.color.toast_info_text));
                break;
            case TYPE_SUCCESS:
                textView.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_view_toast_success));
                textView.setTextColor(ContextCompat.getColor(context, R.color.toast_success_text));
                break;
            default:
                textView.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_view_toast_info));
                textView.setTextColor(ContextCompat.getColor(context, R.color.toast_info_text));
                break;
        }
        toast.setView(view);
        toast.show();
    }
}
