package com.example.brokerfi.token;

import com.example.brokerfi.xc.ChainAddressUtil;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.QRCode.Capture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.InputStream;

/** QR scanning helper for token send/receive flows and gallery fallback. */
public final class TokenQrScanHelper {

    public static final int REQUEST_GALLERY = 9201;

    public interface AddressCallback {
        void onAddress(String address);
    }

    private TokenQrScanHelper() {
    }

    public static void startCameraScan(AppCompatActivity activity) {
        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.setCaptureActivity(Capture.class);
        integrator.setOrientationLocked(true);
        integrator.setBeepEnabled(false);
        integrator.setPrompt(activity.getString(R.string.token_scan_prompt));
        integrator.initiateScan();
    }

    public static void startGalleryPicker(AppCompatActivity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, REQUEST_GALLERY);
    }

    /**
     * @return {@code true} when the request code was handled here.
     */
    public static boolean handleActivityResult(
            AppCompatActivity activity,
            int requestCode,
            int resultCode,
            @Nullable Intent data,
            AddressCallback callback) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            handleCameraResult(activity, resultCode, data, callback);
            return true;
        }
        if (requestCode == REQUEST_GALLERY) {
            handleGalleryResult(activity, resultCode, data, callback);
            return true;
        }
        return false;
    }

    private static void handleCameraResult(
            AppCompatActivity activity,
            int resultCode,
            @Nullable Intent data,
            AddressCallback callback) {
        IntentResult result = IntentIntegrator.parseActivityResult(
                IntentIntegrator.REQUEST_CODE, resultCode, data);
        if (result == null) {
            if (resultCode != Activity.RESULT_CANCELED) {
                toast(activity, R.string.token_scan_failed);
            }
            return;
        }
        if (TextUtils.isEmpty(result.getContents())) {
            if (resultCode != Activity.RESULT_CANCELED) {
                toast(activity, R.string.token_scan_failed);
            }
            return;
        }
        deliverAddress(activity, result.getContents(), callback);
    }

    private static void handleGalleryResult(
            AppCompatActivity activity,
            int resultCode,
            @Nullable Intent data,
            AddressCallback callback) {
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            if (resultCode != Activity.RESULT_CANCELED) {
                toast(activity, R.string.token_scan_failed);
            }
            return;
        }
        String raw = decodeQrFromImageUri(activity, data.getData());
        if (TextUtils.isEmpty(raw)) {
            toast(activity, R.string.token_scan_failed);
            return;
        }
        deliverAddress(activity, raw, callback);
    }

    private static void deliverAddress(Context context, String raw, AddressCallback callback) {
        String address = ChainAddressUtil.parseAddressFromQr(raw);
        if (address != null) {
            if (callback != null) {
                callback.onAddress(address);
            }
        } else {
            toast(context, R.string.token_scan_invalid_address);
        }
    }

    @Nullable
    public static String decodeQrFromImageUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            if (bitmap == null) {
                return null;
            }
            int max = 1024;
            if (bitmap.getWidth() > max || bitmap.getHeight() > max) {
                float scale = Math.min(
                        (float) max / bitmap.getWidth(),
                        (float) max / bitmap.getHeight());
                int w = Math.max(1, (int) (bitmap.getWidth() * scale));
                int h = Math.max(1, (int) (bitmap.getHeight() * scale));
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, w, h, true);
                if (scaled != bitmap) {
                    bitmap.recycle();
                }
                bitmap = scaled;
            }
            return decodeQrFromBitmap(bitmap);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static String decodeQrFromBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = new MultiFormatReader().decode(binaryBitmap);
            return result != null ? result.getText() : null;
        } catch (Exception e) {
            return null;
        } finally {
            bitmap.recycle();
        }
    }

    private static void toast(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }
}
