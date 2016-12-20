package com.arcbit.arcbit.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by Administrator on 6/3/2016.
 */
public class TLAnalytics {
    TLAppDelegate appDelegate;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("TLAnalytics", "TLAnalyticsTLAnalytics " + action);
            updateUserAnalyticsWithEvent(action);
        }
    };

    TLAnalytics(TLAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
        observeUserInterfaceInteractions();
    }

    private void observeUserInterfaceInteractionsWithAchievements() {
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_SEND_PAYMENT));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_RECEIVE_PAYMENT));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_HISTORY));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_CREATE_NEW_ACCOUNT));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_EDIT_ACCOUNT_NAME));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_ARCHIVE_ACCOUNT));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_ENABLE_PIN_CODE));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_BACKUP_PASSPHRASE));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_RESTORE_WALLET));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_ADD_TO_ADDRESS_BOOK));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_EDIT_ENTRY_ADDRESS_BOOK));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_DELETE_ENTRY_ADDRESS_BOOK));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_SEND_TO_ADDRESS_IN_ADDRESS_BOOK));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_TAG_TRANSACTION));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_TOGGLE_AUTOMATIC_TX_FEE));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_CHANGE_AUTOMATIC_TX_FEE));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_ACCOUNT_ADDRESSES));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_ACCOUNT_ADDRESS));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_TRANSACTION_IN_WEB));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_ACCOUNT_ADDRESS_IN_WEB));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_IMPORT_ACCOUNT));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_IMPORT_WATCH_ONLY_ACCOUNT));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_IMPORT_PRIVATE_KEY));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_IMPORT_WATCH_ONLY_ADDRESS));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_CHANGE_BLOCKEXPLORER_TYPE));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_EXTENDED_PUBLIC_KEY));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_EXTENDED_PRIVATE_KEY));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_ACCOUNT_PRIVATE_KEY));
    }

    void  observeUserInterfaceInteractions() {
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_SEND_SCREEN));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_RECEIVE_SCREEN));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_ACCOUNTS_SCREEN));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_MANAGE_ACCOUNTS_SCREEN));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_HELP_SCREEN));
        LocalBroadcastManager.getInstance(this.appDelegate.context).registerReceiver(mMessageReceiver,
                new IntentFilter(TLNotificationEvents.EVENT_VIEW_SETTINGS_SCREEN));
        observeUserInterfaceInteractionsWithAchievements();
    }

    public int getEventCount(String event)  {
        return appDelegate.preferences.getKeyInt(appDelegate.preferences.context, event);
    }

    private void updateUserAnalyticsWithEvent(String event)  {
        int eventCount = appDelegate.preferences.getKeyInt(appDelegate.preferences.context, event);
        appDelegate.preferences.setKeyInt(appDelegate.preferences.context, event, eventCount+1);
    }
}
