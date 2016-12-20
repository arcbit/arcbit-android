package com.arcbit.arcbit.ui.utils;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arcbit.arcbit.model.TLStealthAddress;
import com.arcbit.arcbit.ui.ReceiveFragment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import com.arcbit.arcbit.model.TLAppDelegate;
import com.arcbit.arcbit.model.TLWalletUtils;
import com.arcbit.arcbit.R;

public class TLPageQRCodeFragment extends android.support.v4.app.Fragment {
    private static final String TAG = TLPageQRCodeFragment.class.getName();

    static public int qrCodeDimension = 1000;
    private TLAppDelegate appDelegate;
    public String address;
    public int idx;
    private TextView txtQR;
    private RelativeLayout relativeLayout;
    private ImageView imageView;
    private ProgressBar qrcodeLoadingSpinner;
    private Handler qrCodeHandler;
    private TextView textView;
    private LinearLayout centerTextLayout;
    private RelativeLayout QRCodeLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        appDelegate = TLAppDelegate.instance();
        relativeLayout = (RelativeLayout)inflater.inflate(R.layout.page_qr, container, false);
        idx = getArguments().getInt("idx");
        centerTextLayout = (LinearLayout)relativeLayout.findViewById(R.id.center_text_layout);
        txtQR = (TextView)relativeLayout.findViewById(R.id.txtQR);
        imageView = (ImageView)relativeLayout.findViewById(R.id.imageViewQR);
        qrcodeLoadingSpinner = (ProgressBar)relativeLayout.findViewById(R.id.qr_code_progress_bar);
        QRCodeLayout = (RelativeLayout)relativeLayout.findViewById(R.id.qr_code_layout);
        textView = (TextView)relativeLayout.findViewById(R.id.textView);
        if (appDelegate != null && appDelegate.receiveSelectedObject != null) {
            updateFragment();
        }
        return relativeLayout;
    }

    private void updateFragment() {
        if (idx < ReceiveFragment.receiveAddresses.size()) {
            centerTextLayout.setVisibility(View.GONE);
            qrcodeLoadingSpinner.setVisibility(View.GONE);
            address = ReceiveFragment.receiveAddresses.get(idx);
            qrcodeLoadingSpinner.setVisibility(View.GONE);
            setQRImageView();
        } else {
            QRCodeLayout.setVisibility(View.GONE);
            String text;
            if (appDelegate.receiveSelectedObject.getAccountType() == TLWalletUtils.TLAccountType.ImportedWatch) {
                text = getString(R.string.reusable_address_watch_only_account_explanation);
            } else if (appDelegate.receiveSelectedObject.getAccountType() == TLWalletUtils.TLAccountType.ColdWallet) {
                text = getString(R.string.reusable_address_cold_wallet_account_explanation);
            } else {
                text = getString(R.string.new_address_generation_explanation);
            }
            textView.setText(text);
        }
    }

    private void setQRImageView() {
        if (TLStealthAddress.isStealthAddress(address, appDelegate.appWallet.walletConfig.isTestnet)) {
            txtQR.setText(getString(R.string.reusable_address_colon, address));
        } else {
            txtQR.setText(getString(R.string.address_colon, address));
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = null;
                clip = android.content.ClipData.newPlainText("Address text", address);
                TLToast.makeText(getActivity(), getActivity().getString(R.string.copied_to_clipboard), TLToast.LENGTH_LONG, TLToast.TYPE_INFO);
                clipboard.setPrimaryClip(clip);
            }
        });

        Bitmap bitmap = appDelegate.address2BitmapMap.get(address);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        qrCodeHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Bitmap bitmap = (Bitmap) msg.obj;
                qrcodeLoadingSpinner.setVisibility(View.GONE);
                imageView.setImageBitmap(bitmap);
                appDelegate.address2BitmapMap.put(address, bitmap);
                imageView.invalidate();
            }
        };

        qrcodeLoadingSpinner.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(address, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);
                try {
                    Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
                    Message message = Message.obtain();
                    message.obj = bitmap;
                    qrCodeHandler.sendMessage(Message.obtain(message));
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
