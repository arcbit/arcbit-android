package com.arcbit.arcbit.ui.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import com.arcbit.arcbit.model.TLBitcoinjWrapper;
import com.arcbit.arcbit.R;

public class TLPrompts {
    public interface PromptOKCallback {
        public void onSuccess();
    }

    public interface PromptCallback {
        public void onSuccess(Object obj);
        public void onCancel();
    }

    static public void promptQRCodeDialog(Activity activity, String text, String positiveButtonText,
                                          PromptCallback promptCallback) {
        promptQRCodeDialog(activity, text, positiveButtonText, false, promptCallback);
    }
    
    static public void promptQRCodeDialog(Activity activity, String text, String positiveButtonText,
                                          boolean copyToClipBoard, PromptCallback promptCallback) {
        final ImageView imageView = new ImageView(activity);
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size);

        Handler qrCodeHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Bitmap bitmap = (Bitmap) msg.obj;
                imageView.setImageBitmap(bitmap);
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                int qrCodeDimension = size.x;
                QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(text, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

                try {
                    bitmap = qrCodeEncoder.encodeAsBitmap();
                    Message message = Message.obtain();
                    message.obj = bitmap;
                    qrCodeHandler.sendMessage(Message.obtain(message));
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setMessage(text)
                .setView(imageView)
                .setCancelable(false)
                .setPositiveButton(positiveButtonText, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        alertDialog.setOnShowListener(dialog -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                alertDialog.dismiss();

                if (copyToClipBoard) {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = null;
                    clip = android.content.ClipData.newPlainText("QR text", text);
                    TLToast.makeText(activity, activity.getString(R.string.copied_to_clipboard), TLToast.LENGTH_LONG, TLToast.TYPE_INFO);
                    clipboard.setPrimaryClip(clip);
                    alertDialog.dismiss();   
                }
                
                if (promptCallback != null) {
                    promptCallback.onSuccess(null);
                }
            });

            Button buttonNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(view -> {
                alertDialog.dismiss();
                if (promptCallback != null) {
                    promptCallback.onCancel();
                }
            });
        });

        alertDialog.show();
    }

    static public void promptQRCodeDialogCopyToClipboard(Activity activity, String text) {
        promptQRCodeDialog(activity, text, activity.getString(R.string.copy), true, null);
    }

    public static void promptForInputSaveCancel(Activity activity, String title, String message, String hint,
                                                String yesText, String noText, String preInputtedText, int inputType, PromptCallback promptCallback) {
        final EditText editText = new EditText(activity);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_NORMAL);
        editText.setPadding(46, 16, 46, 16);
        editText.setHint(hint);
        editText.setText(preInputtedText);
        editText.setSelection(editText.getText().length());
        editText.setInputType(inputType);

        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setView(editText)
                .setCancelable(false)
                .setPositiveButton(yesText, null)
                .setNegativeButton(noText, null)
                .create();
        alertDialog.setOnShowListener(dialog -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                promptCallback.onSuccess(editText.getText().toString());
                alertDialog.dismiss();
            });

            Button buttonNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(view -> {
                promptCallback.onCancel();
                alertDialog.dismiss();
            });
        });
        alertDialog.show();
    }

    public static void promptForInput(Activity activity, String title, String message, String hint, String yesText, String noText, int inputType, PromptCallback promptCallback) {
        promptForInputSaveCancel(activity, title, message, hint, yesText, noText, "", inputType, promptCallback);
    }

    public static void promptForInputSaveCancel(Activity activity, String title, String message, String hint, int inputType, PromptCallback promptCallback) {
        promptForInputSaveCancel(activity, title, message, hint, activity.getString(R.string.save), activity.getString(R.string.cancel), "", inputType, promptCallback);
    }

    public static void promptForInputSaveCancel(Activity activity, String title, String message, String hint, String preInputtedText, int inputType, PromptCallback promptCallback) {
        promptForInputSaveCancel(activity, title, message, hint, activity.getString(R.string.save), activity.getString(R.string.cancel), preInputtedText, inputType, promptCallback);
    }

    public static void promptSuccessMessage(Activity activity, String title, String message) {
        promptForOK(activity, title, message, null);
    }

    public static void promptErrorMessage(Activity activity, String title, String message) {
        promptForOK(activity, title, message, null);
    }

    public static void promptForOK(Activity activity, String title, String message, PromptOKCallback promptOKCallback) {
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_capitalize, null)
                .create();
        alertDialog.setOnShowListener(dialog -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                alertDialog.dismiss();
                if (promptOKCallback != null) {
                    promptOKCallback.onSuccess();
                }
            });
        });
        alertDialog.show();
    }

    public static void promptForOKCancel(Activity activity, String title, String message, PromptCallback promptCallback) {
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_capitalize, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        alertDialog.setOnShowListener(dialog -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                alertDialog.dismiss();
                promptCallback.onSuccess(null);
            });


            Button buttonNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(view -> {
                alertDialog.dismiss();
                promptCallback.onCancel();
            });
        });
        alertDialog.show();
    }

    public static void promptYesNo(Activity activity, String title, String message, String yesText, String noText, PromptCallback promptCallback) {
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(yesText, null)
                .setNegativeButton(noText, null)
                .create();
        alertDialog.setOnShowListener(dialog -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                alertDialog.dismiss();
                promptCallback.onSuccess(null);
            });

            Button buttonNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(view -> {
                alertDialog.dismiss();
                promptCallback.onCancel();
            });
        });
        alertDialog.show();
    }

    public static void promptWithOneButton(Activity activity, String title, String message, String buttonText, PromptOKCallback promptOKCallback) {
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(buttonText, null)
                .create();
        alertDialog.setOnShowListener(dialog -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                alertDialog.dismiss();
                promptOKCallback.onSuccess();
            });
        });
        alertDialog.show();
    }

    public static void promptForEncryptedPrivKeyPassword(Activity activity, String encryptedPrivKey, boolean isTestnet, PromptCallback promptCallback) {
        promptForInputSaveCancel(activity, activity.getString(R.string.enter_password_for_encrypted_private_key), "", activity.getString(R.string.password),
                activity.getString(R.string.ok_capitalize), activity.getString(R.string.cancel), "", InputType.TYPE_CLASS_TEXT, new PromptCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        String password = (String) obj;
                        TLHUDWrapper.showHUD(activity, activity.getString(R.string.decrypting));


                        Handler handler = new Handler(Looper.getMainLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                String privKey = (String) msg.obj;
                                TLHUDWrapper.hideHUD();
                                if (privKey != null) {
                                    promptCallback.onSuccess(privKey);
                                } else {
                                    promptYesNo(activity, activity.getString(R.string.invalid_passphrase), "", activity.getString(R.string.retry), activity.getString(R.string.cancel), new PromptCallback() {
                                        @Override
                                        public void onSuccess(Object obj) {
                                            promptForEncryptedPrivKeyPassword(activity, encryptedPrivKey, isTestnet, promptCallback);
                                        }

                                        @Override
                                        public void onCancel() {
                                            promptCallback.onCancel();
                                        }
                                    });
                                }
                            }
                        };

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String privKey = TLBitcoinjWrapper.privateKeyFromEncryptedPrivateKey(encryptedPrivKey, password, isTestnet);
                                Message message = Message.obtain();
                                message.obj = privKey;
                                handler.sendMessage(Message.obtain(message));
                            }
                        });
                        thread.setPriority(Thread.MAX_PRIORITY);
                        thread.start();
                    }

                    @Override
                    public void onCancel() {
                        promptCallback.onCancel();
                    }
                });
    }
}
