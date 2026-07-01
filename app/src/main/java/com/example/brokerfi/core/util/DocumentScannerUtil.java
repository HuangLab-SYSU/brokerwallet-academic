package com.example.brokerfi.core.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;

/**
 * Simplified version of document scanning tool / 简化版文档扫描工具类
 * Provides basic image enhancement functions (without using OpenCV) / 提供基础的图像增强功能（不使用OpenCV）
 */
public class DocumentScannerUtil {

    private static final String TAG = "DocumentScanner";

    /**
     * Initialization (the simplified version does not require OpenCV) / 初始化（简化版不需要OpenCV）
     */
    public static void initOpenCV(Context context) {
        Log.d(TAG, "Using simplified document scanner (no OpenCV)");
    }

    /**
     * Check if it has been initialized (simplified version always returns true) / 检查是否已初始化（简化版始终返回true）
     */
    public static boolean isOpenCVInitialized() {
        return true;
    }

    /**
     * Scan Documents - Lite (Basic Image Enhancement) / 扫描文档 - 简化版（基础图像增强）
     * @param context context / 上下文
     * @param imageUri Image URI / 图片URI
     * @return Enhanced Bitmap, returns null on failure / 增强后的Bitmap，失败返回null
     */
    public static Bitmap scanDocument(Context context, Uri imageUri) {
        try {
            // Load images
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to load image from URI");
                return null;
            }

            // Perform basic image enhancement
            Bitmap enhancedBitmap = enhanceDocumentImage(originalBitmap);

            return enhancedBitmap;

        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during document scanning", e);
            return null;
        }
    }

    /**
     * Basic image enhancement (without using OpenCV) / 基础图像增强（不使用OpenCV）
     * @param originalBitmap original picture / 原始图片
     * @return Enhanced image / 增强后的图片
     */
    private static Bitmap enhanceDocumentImage(Bitmap originalBitmap) {
        try {
            // Create a mutable copy of Bitmap
            Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

            // Apply image enhancement
            Bitmap enhancedBitmap = applyContrastAndBrightness(mutableBitmap, 1.2f, 10);

            // If the original image and the enhanced image are not the same object, release the intermediate result.
            if (mutableBitmap != enhancedBitmap) {
                mutableBitmap.recycle();
            }

            return enhancedBitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error enhancing image", e);
            return originalBitmap;
        }
    }

    /**
     * Apply contrast and brightness adjustments / 应用对比度和亮度调整
     * @param bitmap original picture / 原始图片
     * @param contrast Contrast (1.0 = original, >1.0 = enhanced contrast) / 对比度 (1.0 = 原始, >1.0 = 增强对比度)
     * @param brightness Brightness (0 = original, >0 = increased brightness) / 亮度 (0 = 原始, >0 = 增加亮度)
     * @return Adjusted picture / 调整后的图片
     */
    private static Bitmap applyContrastAndBrightness(Bitmap bitmap, float contrast, float brightness) {
        ColorMatrix colorMatrix = new ColorMatrix();

        // Set contrast and brightness
        colorMatrix.set(new float[] {
            contrast, 0, 0, 0, brightness,
            0, contrast, 0, 0, brightness,
            0, 0, contrast, 0, brightness,
            0, 0, 0, 1, 0
        });

        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        Paint paint = new Paint();
        paint.setColorFilter(colorFilter);

        Bitmap resultBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return resultBitmap;
    }
}
