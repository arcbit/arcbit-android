package com.arcbit.arcbit.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.arcbit.arcbit.R;
import com.arcbit.arcbit.model.TLAppDelegate;

public class PinActivity extends Activity {

    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    public Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pin_activity);

        String pinOption = getIntent().getExtras().getString(PinFragment.PIN_OPTION);
        setupToolbar(!pinOption.equals(PinFragment.VERIFY_PIN));

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Fragment fragment = new PinFragment();
        Bundle data = new Bundle();
        data.putString(PinFragment.PIN_OPTION, pinOption);
        fragment.setArguments(data);
        getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    private void setupToolbar(boolean haveBackNavigationButton) {
        toolbar = (Toolbar) this.findViewById(R.id.toolbar_general);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        if (haveBackNavigationButton) {
            toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arcbit_arrow_left));
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            toolbar.setNavigationIcon(null);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (appDelegate != null && appDelegate.suggestions != null) {
            return appDelegate.suggestions.checkToShowHiddenOverlayWarning(this, event) || super.dispatchTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
    }
}
