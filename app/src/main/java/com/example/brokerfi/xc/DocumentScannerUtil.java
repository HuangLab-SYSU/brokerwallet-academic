package com.example.brokerfi.xc;

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
 * 简化版文档扫描工具类
 * 提供基础的图像增强功能（不使用OpenCV）
 */
public class DocumentScannerUtil {
    
    private static final String TAG = "DocumentScanner";
    
    /**
     * 初始化（简化版不需要OpenCV）
     */
    public static void initOpenCV(Context context) {
        Log.d(TAG, "Using simplified document scanner (no OpenCV)");
    }
    
    /**
     * 检查是否已初始化（简化版始终返回true）
     */
    public static boolean isOpenCVInitialized() {
        return true;
    }
    
    /**
     * 扫描文档 - 简化版（基础图像增强）
     * @param context 上下文
     * @param imageUri 图片URI
     * @return 增强后的Bitmap，失败返回null
     */
    public static Bitmap scanDocument(Context context, Uri imageUri) {
        try {
            // 加载图片
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to load image from URI");
                return null;
            }
            
            // 执行基础图像增强
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
     * 基础图像增强（不使用OpenCV）
     * @param originalBitmap 原始图片
     * @return 增强后的图片
     */
    private static Bitmap enhanceDocumentImage(Bitmap originalBitmap) {
        try {
            // 创建可变的Bitmap副本
            Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            
            // 应用图像增强
            Bitmap enhancedBitmap = applyContrastAndBrightness(mutableBitmap, 1.2f, 10);
            
            // 如果原图和增强图不是同一个对象，释放中间结果
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
     * 应用对比度和亮度调整
     * @param bitmap 原始图片
     * @param contrast 对比度 (1.0 = 原始, >1.0 = 增强对比度)
     * @param brightness 亮度 (0 = 原始, >0 = 增加亮度)
     * @return 调整后的图片
     */
    private static Bitmap applyContrastAndBrightness(Bitmap bitmap, float contrast, float brightness) {
        ColorMatrix colorMatrix = new ColorMatrix();
        
        // 设置对比度和亮度
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
