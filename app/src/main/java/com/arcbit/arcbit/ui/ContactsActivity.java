package com.arcbit.arcbit.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLBitcoinjWrapper;
import com.arcbit.arcbit.model.TLNotificationEvents;
import com.arcbit.arcbit.model.TLWalletJSONKeys;
import com.arcbit.arcbit.model.TLWalletUtils;
import com.arcbit.arcbit.ui.utils.SwipeDismissListViewTouchListener;
import com.arcbit.arcbit.ui.utils.TLPrompts;
import com.arcbit.arcbit.ui.utils.TLToast;
import com.arcbit.arcbit.ui.items.*;

import com.arcbit.arcbit.R;
import com.arcbit.arcbit.utils.TLPermissionUtil;
import com.arcbit.arcbit.utils.TLUtils;
import com.google.zxing.client.android.CaptureActivity;

public class ContactsActivity extends ListActivity {

    private TLAppDelegate appDelegate = TLAppDelegate.instance();
    private ListView contactsListView;
    private ContactsListAdapter adapter;
    public static final int SCAN_ADDRESS = 3123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        setTitle(getString(R.string.contacts));
        setupToolbar();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptAddContact();
            }
        });

        adapter = new ContactsListAdapter(ContactsActivity.this);
        contactsListView = getListView();
        contactsListView.setAdapter(adapter);

        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        contactsListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    appDelegate.appWallet.deleteAddressBookEntry(position-1);
                                    adapter.remove(adapter.getItem(position));
                                    //promptDeleteContact(position);
                                    //break;
                                }
                                adapter.notifyDataSetChanged();
                            }
                        });
        contactsListView.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        contactsListView.setOnScrollListener(touchListener.makeScrollListener());
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar_general);
        toolbar.setTitle(getString(R.string.contacts));
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arcbit_arrow_left));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_ADDRESS
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String value = data.getStringExtra(CaptureActivity.SCAN_RESULT);
            processAddressBookAddress(value);
        }
    }

    private void startScanActivity() {
        if (!TLUtils.isCameraOpen()) {
            Intent intent = new Intent(ContactsActivity.this, CaptureActivity.class);
            startActivityForResult(intent, SCAN_ADDRESS);
        } else {
            TLToast.makeText(ContactsActivity.this, getString(R.string.camera_unavailable), TLToast.LENGTH_SHORT, TLToast.TYPE_ERROR);
        }
    }

    private void scanQRCode() {
        if (ContextCompat.checkSelfPermission(appDelegate.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            TLPermissionUtil.requestCameraPermissionFromActivity(findViewById(android.R.id.content), ContactsActivity.this);
        }else{
            startScanActivity();
        }
    }

    void promptAddContact() {
        CharSequence[] otherButtonTitles = {getString(R.string.import_with_qr_code), getString(R.string.import_with_text_input)};
        new AlertDialog.Builder(ContactsActivity.this)
                .setTitle(getString(R.string.create_new_contact))
                .setItems(otherButtonTitles, (dialog, which) -> {
                            if (which == 0) {
                                scanQRCode();
                            } else if (which == 1) {
                                TLPrompts.promptForInputSaveCancel(ContactsActivity.this, getString(R.string.input_address), "", getString(R.string.address), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
                                    @Override
                                    public void onSuccess(Object obj) {
                                        String address = (String) obj;
                                        processAddressBookAddress(address);
                                    }

                                    @Override
                                    public void onCancel() {
                                    }
                                });
                            }
                            dialog.dismiss();
                        }
                ).show();
    }

    void promptEditContact(int index) {
        TLPrompts.promptForInputSaveCancel(ContactsActivity.this, getString(R.string.edit_contact_name), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
            @Override
            public void onSuccess(Object obj) {
                String name = (String) obj;
                appDelegate.appWallet.editAddressBookEntry(index-1, name);
            }

            @Override
            public void onCancel() {
            }
        });
    }

    void promptDeleteContact(int index) {
        TLPrompts.promptForOKCancel(ContactsActivity.this, getString(R.string.delete_contact), "", new TLPrompts.PromptCallback() {
            @Override
            public void onSuccess(Object obj) {
                appDelegate.appWallet.deleteAddressBookEntry(index-1);
            }

            @Override
            public void onCancel() {

            }
        });
    }

    void processAddressBookAddress(String address) {
        if (TLBitcoinjWrapper.isValidAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
            if (TLBitcoinjWrapper.isAddressVersion0(address, appDelegate.appWallet.walletConfig.isTestnet)) {
                if (TLWalletUtils.ENABLE_STEALTH_ADDRESS() && !appDelegate.suggestions.disabledSuggestDontAddNormalAddressToAddressBook()) {
                    TLPrompts.promptForOKCancel(ContactsActivity.this, getString(R.string.warning),
                            getString(R.string.add_non_reusable_address_to_contacts_warning), new TLPrompts.PromptCallback() {
                                @Override
                                public void onSuccess(Object obj) {
                                    promptForLabel(address);
                                    appDelegate.suggestions.setDisableSuggestDontAddNormalAddressToAddressBook(true);
                                }

                                @Override
                                public void onCancel() {

                                }
                            });
                } else {
                    promptForLabel(address);
                }
            } else {
                promptForLabel(address);
            }
        }
        else {
            TLPrompts.promptErrorMessage(ContactsActivity.this, getString(R.string.invalid_bitcoin_address), "");
        }
    }

    void promptForLabel(String address) {
        TLPrompts.promptForInputSaveCancel(ContactsActivity.this, getString(R.string.input_contact_name_for_address), "", getString(R.string.name), InputType.TYPE_CLASS_TEXT, new TLPrompts.PromptCallback() {
            @Override
            public void onSuccess(Object obj) {
                appDelegate.appWallet.addAddressBookEntry(address, (String) obj);
                LocalBroadcastManager.getInstance(appDelegate.context).sendBroadcast(new Intent(TLNotificationEvents.EVENT_ADD_TO_ADDRESS_BOOK));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancel() {
            }
        });
    }

    private class ContactsListAdapter extends ArrayAdapter<Item> {
        private LayoutInflater vi;
        public ContactsListAdapter(Context context) {
            super(context, 0);
            vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return appDelegate.appWallet.getAddressBook().length() + 1;
        }

        @Override
        public Item getItem(int position) {
            if (position == 0) {
                return new SectionItem("");
            }
            return new ContactItem();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            final Item i = getItem(position);
            if (i != null) {
                if (i instanceof SectionItem) {
                    SectionItem si = (SectionItem)i;
                    v = vi.inflate(R.layout.list_item_section, null);
                    final TextView sectionView = (TextView) v.findViewById(R.id.list_item_section_text);
                    sectionView.setText(si.getTitle());
                } else if (i instanceof ContactItem) {
                    v = vi.inflate(R.layout.list_item_contact, null);
                    final TextView labelTextView = (TextView)v.findViewById(R.id.list_item_label);
                    final TextView addressTextView = (TextView)v.findViewById(R.id.list_item_address);
                    try {
                        JSONObject addr = appDelegate.appWallet.getAddressBook().getJSONObject(position-1);
                        String address = addr.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
                        String label = addr.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_LABEL);
                        if (labelTextView != null)
                            labelTextView.setText(label);
                        if(addressTextView != null)
                            addressTextView.setText(address);
                    } catch (JSONException e) {
                    }
                }
            }
            return v;
        }
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Log.d("ssss", "onListItemClick: " + position);
        if (position == 0) {
            return;
        }
        Log.d("ssss", "onListItemClick 1: " + position);
        try {
            JSONObject addr = appDelegate.appWallet.getAddressBook().getJSONObject(position-1);
            String address = addr.getString(TLWalletJSONKeys.WALLET_PAYLOAD_KEY_ADDRESS);
            Intent intent = new Intent();
            intent.putExtra("ADDRESS", address);
            setResult(Activity.RESULT_OK, intent);
            finish();
        } catch (JSONException e) {
        }
    }
}
