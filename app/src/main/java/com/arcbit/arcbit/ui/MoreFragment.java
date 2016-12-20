package com.arcbit.arcbit.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
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

public class MoreFragment extends android.support.v4.app.Fragment {
    private ListView moreListview;
    private View rootView;
    private TLAppDelegate appDelegate = TLAppDelegate.instance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_more, container, false);
        if (appDelegate == null) {
            return rootView;
        }
        getActivity().setTitle(getString(R.string.more));

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        MoreListAdapter adapter = new MoreListAdapter(rootView.getContext());
        moreListview = (ListView) rootView.findViewById(R.id.more_list_view);
        moreListview.setAdapter(adapter);

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

    public void openInWeb(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private class MoreListAdapter extends ArrayAdapter<Item> {

        private Context context;
        private LayoutInflater vi;

        public MoreListAdapter(Context context) {
            super(context, 0);
            this.context = context;
            vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return 13;
        }

        @Override
        public Item getItem(int position) {
            if (position == 0) {
                return new SectionItem(getString(R.string.check_out_the_arcbit_web_wallet));
            } else if (position == 1) {
                return new LeftTitleItem(getString(R.string.check_out_the_arcbit_web_wallet));
            } else if (position == 2) {
                return new LeftTitleItem(getString(R.string.view_arcbit_web_wallet_details));
            } else if (position == 3) {
                return new SectionItem(getString(R.string.arcbit_brain_wallet));
            } else if (position == 4) {
                return new LeftTitleItem(getString(R.string.check_out_the_arcbit_brain_wallet));
            } else if (position == 5) {
                return new LeftTitleItem(getString(R.string.view_arcbit_brain_wallet_details));
            } else if (position == 6) {
                return new SectionItem(getString(R.string.arcbit_ios_wallet));
            } else if (position == 7) {
                return new LeftTitleItem(getString(R.string.check_out_the_arcbit_ios_wallet));
            } else if (position == 8) {
                return new SectionItem(getString(R.string.other_links));
            } else if (position == 9) {
                return new LeftTitleItem(getString(R.string.about_us));
            } else if (position == 10) {
                return new LeftTitleItem(getString(R.string.follow_us_on_twitter));
            } else if (position == 11) {
                return new SectionItem(getString(R.string.email_support));
            } else if (position == 12) {
                return new LeftTitleItem(getString(R.string.email_support));
            }
            return new SectionItem(getString(R.string.arcbit_web_wallet));
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
                                String url;
                                if (position == 1) {
                                    openInWeb("https://chrome.google.com/webstore/detail/arcbit-bitcoin-wallet/dkceiphcnbfahjbomhpdgjmphnpgogfk");
                                } else if (position == 2) {
                                    String detail1 = getString(R.string.arcbit_web_wallet_detail);
                                    String detail2 = getString(R.string.arcbit_web_wallet_detail_2);
                                    TLPrompts.promptSuccessMessage(getActivity(), getString(R.string.arcbit_web_wallet), detail1+detail2);
                                } else if (position == 4) {
                                    openInWeb("https://www.arcbitbrainwallet.com");
                                } else if (position == 5) {
                                    String detail1 = getString(R.string.arcbit_brain_wallet_detail);
                                    TLPrompts.promptSuccessMessage(getActivity(), getString(R.string.arcbit_brain_wallet), detail1);
                                } else if (position == 7) {
                                    openInWeb("https://itunes.apple.com/app/id999487888");
                                } else if (position == 9) {
                                    openInWeb("http://arcbit.io/");
                                } else if (position == 10) {
                                    openInWeb("https://twitter.com/arc_bit");
                                } else if (position == 12) {
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    String subject = getString(R.string.android_support);
                                    String androidOS = Build.VERSION.RELEASE;
                                    String message = String.format(getString(R.string.dear_arcbit_support)+"\n\n\n\n--\nApp Version: %s\nSystem: %s\n", appDelegate.preferences.getAppVersion(), androidOS);
                                    intent.setType("text/plain");
                                    intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                                    intent.putExtra(Intent.EXTRA_TEXT, message);
                                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@arcbit.zendesk.com"});
                                    Intent mailer = Intent.createChooser(intent, "Send email...");
                                    startActivity(mailer);
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
