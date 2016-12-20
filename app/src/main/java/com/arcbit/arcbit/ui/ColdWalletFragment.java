package com.arcbit.arcbit.ui;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View.OnClickListener;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.ui.utils.TLPrompts;
import com.arcbit.arcbit.ui.items.*;

import com.arcbit.arcbit.R;

public class ColdWalletFragment extends android.support.v4.app.Fragment {
    private ListView coldWalletListview;
    private View rootView;
    private TLAppDelegate appDelegate = TLAppDelegate.instance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_cold_wallet, container, false);
        if (appDelegate == null) {
            return rootView;
        }

        getActivity().setTitle(getString(R.string.cold_wallet));

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ColdWalletListAdapter adapter = new ColdWalletListAdapter(rootView.getContext());
        coldWalletListview = (ListView) rootView.findViewById(R.id.cold_wallet_list_view);
        coldWalletListview.setAdapter(adapter);

        return rootView;
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arcbit_menu_white));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerLayout drawer = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        setupToolbar();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupToolbar();
    }

    private class ColdWalletListAdapter extends ArrayAdapter<Item> {

        private Context context;
        private LayoutInflater vi;

        public ColdWalletListAdapter(Context context) {
            super(context, 0);
            this.context = context;
            vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            if (appDelegate.preferences.enabledAdvancedMode()) {
                return 6;
            } else {
                return 4;
            }
        }

        @Override
        public Item getItem(int position) {
            if (position == 0) {
                return new SectionItem(getString(R.string.cold_wallet));
            } else if (position == 1) {
                return new LeftTitleItem(getString(R.string.cold_wallet_overview));
            } else if (position == 2) {
                return new LeftTitleItem(getString(R.string.create_cold_wallet));
            } else if (position == 3) {
                return new LeftTitleItem(getString(R.string.authorize_cold_wallet_account_payment));
            } else if (position == 4) {
                return new SectionItem(getString(R.string.internal_wallet_data));
            } else if (position == 5) {
                return new LeftTitleItem(getString(R.string.internal_wallet_data));
            }
            return new SectionItem("");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            final Item i = getItem(position);
            if (i != null) {
                if(i.isSection()){
                    SectionItem si = (SectionItem)i;
                    v = vi.inflate(R.layout.list_item_section, null);

                    v.setOnClickListener(null);
                    v.setOnLongClickListener(null);
                    v.setLongClickable(false);

                    final TextView sectionView = (TextView) v.findViewById(R.id.list_item_section_text);
                    sectionView.setText(si.getTitle());
                } else {
                    if (i instanceof LeftTitleItem) {
                        LeftTitleItem ei = (LeftTitleItem)i;
                        v = vi.inflate(R.layout.left_title_entry, null);
                        final TextView title = (TextView)v.findViewById(R.id.title);
                        if (title != null) {
                            title.setText(ei.title);
                        }
                        v.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (position == 1) {
                                    TLPrompts.promptForOK(getActivity(), getString(R.string.cold_wallet_overview), getString(R.string.cold_wallet_overview_description), new TLPrompts.PromptOKCallback() {

                                        @Override
                                        public void onSuccess() {
                                        }
                                    });
                                } else if (position == 2) {
                                    android.support.v4.app.Fragment fragmentChild = new CreateColdWalletFragment();
                                    android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
                                    transaction.setCustomAnimations(R.anim.right_slide_in, R.anim.left_slide_out, R.anim.left_slide_in, R.anim.right_slide_out);
                                    transaction.replace(R.id.fragment_container, fragmentChild);
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                } else if (position == 3) {
                                    android.support.v4.app.Fragment fragmentChild = new AuthorizeColdWalletPaymentFragment();
                                    android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
                                    transaction.setCustomAnimations(R.anim.right_slide_in, R.anim.left_slide_out, R.anim.left_slide_in, R.anim.right_slide_out);
                                    transaction.replace(R.id.fragment_container, fragmentChild);
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                } else if (position == 5) {
                                    android.support.v4.app.Fragment fragmentChild = new BrainWalletFragment();
                                    android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
                                    transaction.setCustomAnimations(R.anim.right_slide_in, R.anim.left_slide_out, R.anim.left_slide_in, R.anim.right_slide_out);
                                    transaction.replace(R.id.fragment_container, fragmentChild);
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            }
                        });
                    }
                }
            }

            return v;
        }
    }
}
