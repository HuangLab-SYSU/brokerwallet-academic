package com.example.brokerfi.core.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;

// syh_work_02 new: random avatar (identicon) generation + local avatar storage
public class AvatarUtil {

    private static final String AVATAR_DIR = "account_avatars";

    // 取头像统一入口：有自定义头像文件→读文件；否则→实时生成默认 identicon（统一转圆形）
    public static Bitmap loadAvatar(Context context, String address, int sizePx) {
        Bitmap bmp = null;
        if (address != null) {
            File f = getAvatarFile(context, address);
            if (f.exists()) {
                Bitmap decoded = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (decoded != null) {
                    bmp = Bitmap.createScaledBitmap(decoded, sizePx, sizePx, true);
                }
            }
        }
        if (bmp == null) {
            bmp = generateIdenticon(address, sizePx);
        }
        return toCircle(bmp);
    }

    // 将方形 bitmap 裁成圆形（透明四角）
    private static Bitmap toCircle(Bitmap src) {
        int size = Math.min(src.getWidth(), src.getHeight());
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }

    // 保存用户上传的头像（已裁剪/压缩）
    public static void saveAvatar(Context context, String address, Bitmap bitmap) {
        File f = getAvatarFile(context, address);
        try (FileOutputStream out = new FileOutputStream(f)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 删除自定义头像 → 回退到默认 identicon
    public static void removeAvatar(Context context, String address) {
        File f = getAvatarFile(context, address);
        if (f.exists()) {
            f.delete();
        }
    }

    public static boolean hasCustomAvatar(Context context, String address) {
        return getAvatarFile(context, address).exists();
    }

    private static File getAvatarFile(Context context, String address) {
        File dir = new File(context.getFilesDir(), AVATAR_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, address.toLowerCase() + ".jpg");
    }

    // 确定性 identicon：5×5 水平对称网格，颜色由 seed 哈希决定（同一地址永远同一头像）
    public static Bitmap generateIdenticon(String seed, int sizePx) {
        byte[] hash = sha256(seed == null ? "" : seed);

        int hue = (hash[0] & 0xFF) % 360;
        float sat = 0.45f + ((hash[1] & 0xFF) % 40) / 100f;   // 0.45 ~ 0.84
        float val = 0.45f + ((hash[2] & 0xFF) % 30) / 100f;   // 0.45 ~ 0.74
        int fg = Color.HSVToColor(new float[]{hue, sat, val});
        int bg = Color.rgb(238, 238, 238);

        int cols = 5, rows = 5;
        boolean[][] grid = new boolean[rows][cols];
        int idx = 4;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 3; c++) {          // 只驱动左 3 列，右侧镜像
                int b = hash[idx % hash.length] & 0xFF;
                idx++;
                boolean on = (b & 0x01) != 0;
                grid[r][c] = on;
                grid[r][cols - 1 - c] = on;
            }
        }

        int cell = sizePx / cols;
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bg);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(fg);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c]) {
                    float left = c * cell;
                    float top = r * cell;
                    canvas.drawRect(left, top, left + cell, top + cell, paint);
                }
            }
        }
        return bitmap;
    }

    private static byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes("UTF-8"));
        } catch (Exception e) {
            return new byte[32];
        }
    }
}
