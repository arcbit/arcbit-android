package com.arcbit.arcbit.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.multidex.MultiDex;
import android.support.v4.content.LocalBroadcastManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.arcbit.arcbit.model.TLCallback;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.ui.utils.TLPrompts;
import com.arcbit.arcbit.ui.utils.TLToast;
import com.arcbit.arcbit.utils.TLAppUtil;
import com.arcbit.arcbit.R;
import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.utils.TLUtils;
import com.crashlytics.android.Crashlytics;

import org.json.JSONObject;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = MainActivity.class.getName();

    public MenuItem useAllFundsMenuItem;
    public MenuItem refreshBalanceMenuItem;
    public boolean initializedAppAndLoadedWallet = false;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            String action = intent.getAction();
            if (action.equals(TLNotificationEvents.EVENT_RECEIVE_PAYMENT)) {
                String receivedTo = intent.getStringExtra("receivedTo");
                String receivedAmount = intent.getStringExtra("receivedAmount");
                String msg = getString(R.string.account_received_amount, receivedTo, receivedAmount);
                promptReceivePaymentMessage(msg);
            } else if (action.equals(TLNotificationEvents.EVENT_SAVE_WALLET_ERROR)) {
                TLPrompts.promptErrorMessage(MainActivity.this, getString(R.string.error), getString(R.string.backup_wallet));
            } else if (action.equals(TLNotificationEvents.EVENT_TOGGLED_COLD_WALLET)) {
            }
        }
    };

    void promptReceivePaymentMessage(String msg) {
        if (!this.isFinishing()) {
            TLPrompts.promptSuccessMessage(MainActivity.this, "", msg);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.d(TAG, "MainActivity onCreate");

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView,slideOffset);
                Menu menu = navigationView.getMenu();
                MenuItem coldWalletMenuItem = menu.findItem(R.id.nav_cold_wallet);
                if (!coldWalletMenuItem.isVisible() && TLAppDelegate.instance().preferences.enabledColdWallet() ||
                        coldWalletMenuItem.isVisible() && !TLAppDelegate.instance().preferences.enabledColdWallet()) {
                    coldWalletMenuItem.setVisible(TLAppDelegate.instance().preferences.enabledColdWallet());
                }
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_RECEIVE_PAYMENT));
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_SAVE_WALLET_ERROR));
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(receiver,
                new IntentFilter(TLNotificationEvents.EVENT_TOGGLED_COLD_WALLET));

        TLAppUtil.getInstance(MainActivity.this).applyPRNGFixes();

        this.initModel();
        if (!TLAppDelegate.instance().preferences.hasSetupHDWallet()) {
            final android.support.v4.app.Fragment fragment = new ReceiveFragment();
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
        } else {
            final android.support.v4.app.Fragment fragment = new SendFragment();
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
        }

        if (TLAppDelegate.instance().preferences.isEnablePINCode()) {
            Intent intent = new Intent(MainActivity.this, PinActivity.class);
            intent.putExtra(PinFragment.PIN_OPTION, PinFragment.VERIFY_PIN);
            startActivity(intent);
        }

        if (!TLUtils.haveInternetConnection(MainActivity.this)) {
            TLToast.makeText(MainActivity.this, getString(R.string.no_internet_connection_description), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
        }
    }

    void initModel() {
        TLAppDelegate.instance(MainActivity.this).initAppDelegate();
    }

    public void initAppAndLoadWallet(TLCallback callback) {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                initializedAppAndLoadedWallet = true;
                callback.onSuccess(null);
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                loadWallet();
                Message message = Message.obtain();
                handler.sendMessage(Message.obtain(message));
            }
        }).start();
    }

    void loadWallet() {
        String version = null;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            //int verCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "NameNotFoundException: " + e.getLocalizedMessage());
        }
        TLAppDelegate.instance().initializeWalletApp(version, false);
        TLAppDelegate.instance().transactionListener.reconnect();
        TLAppDelegate.instance().stealthWebSocket.reconnect();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        useAllFundsMenuItem = menu.findItem(R.id.use_all_funds);
        useAllFundsMenuItem.setVisible(false);
        refreshBalanceMenuItem = menu.findItem(R.id.refresh_balance);
        refreshBalanceMenuItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.use_all_funds) {
            LocalBroadcastManager.getInstance(TLAppDelegate.instance().context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_CLICKED_USE_ALL_FUNDS));
            return true;
        }
        if (id == R.id.refresh_balance) {
            LocalBroadcastManager.getInstance(TLAppDelegate.instance().context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_CLICKED_REFRESH_BALANCE));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_send) {
            android.support.v4.app.Fragment fragment = new SendFragment();
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
        } else if (id == R.id.nav_receive) {
            android.support.v4.app.Fragment fragment = new ReceiveFragment();
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
        } else if (id == R.id.nav_history) {
            android.support.v4.app.Fragment fragment = new HistoryFragment();
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
        } else if (id == R.id.nav_accounts) {
            android.support.v4.app.Fragment fragment = new AccountsFragment();
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
        } else if (id == R.id.nav_cold_wallet) {
            android.support.v4.app.Fragment fragment = new ColdWalletFragment();
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
        } else if (id == R.id.nav_more) {
            android.support.v4.app.Fragment fragment = new MoreFragment();
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(TLAppDelegate.instance().context).unregisterReceiver(receiver);
    }
}
