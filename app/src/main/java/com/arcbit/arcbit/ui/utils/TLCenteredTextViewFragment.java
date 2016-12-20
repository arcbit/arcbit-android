package com.arcbit.arcbit.ui.utils;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arcbit.arcbit.R;

public class TLCenteredTextViewFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RelativeLayout relativeLayout = (RelativeLayout)inflater.inflate(R.layout.fragment_centered_textview, container, false);
        TextView textView = (TextView)relativeLayout.findViewById(R.id.textView);
        textView.setText(getArguments().getString("text"));
        return relativeLayout;
    }
}
