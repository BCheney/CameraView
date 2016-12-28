package com.bhl.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Field;

/**
 * Created by hackooo on 16/4/23.
 */
public class PhoneUtils {
    public static float DENSITY;
    public static int DENSITY_DPI;
    public static int WIDTH_PIXELS;
    public static float WIDTH_DP;
    public static int HEIGHT_PIXELS;
    public static float HEIGHT_DP;
    public static int PRODUCT_STATUS_BAR_MARGIN;
    public static int STATUS_BAR_HEIGHT;


    public static int dpToPx(float dp) {
        return (int) (DENSITY * dp + 0.5);
    }

    public static float pxToDp(int px) {
        return px / DENSITY;
    }

    /**
     * do it when app start up
     * @param context
     */
    public static void init(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        DENSITY = dm.density;
        DENSITY_DPI = dm.densityDpi;
        WIDTH_PIXELS = dm.widthPixels;
        HEIGHT_PIXELS = dm.heightPixels;
        WIDTH_DP = pxToDp(WIDTH_PIXELS);
        HEIGHT_DP = pxToDp(HEIGHT_PIXELS);
        STATUS_BAR_HEIGHT = getStatusBarHeight(context);
        PRODUCT_STATUS_BAR_MARGIN = 0;
    }

    public static int getWidthPixels() {
        return WIDTH_PIXELS;
    }

    public static int getHeightPixels() {
        return HEIGHT_PIXELS;
    }

    // 获取手机状态栏高度
    public static int getStatusBarHeight(Context context) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
        }
        return statusBarHeight;
    }
    /**
     * 方法说明：获得屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    /**
     * 方法说明：获得屏幕高度
     *
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    /**
     * 方法说明：获得状态栏的高度
     *
     * @param context
     * @return
     */
    public static int getStatusHeight(Context context) {

        int statusHeight = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }

    /**
     * 方法说明：获取当前屏幕截图，包含状态栏
     *
     * @param activity
     * @return
     */
    public static Bitmap snapShotWithStatusBar(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bmp = view.getDrawingCache();
        int width = getScreenWidth(activity);
        int height = getScreenHeight(activity);
        Bitmap bp = null;
        bp = Bitmap.createBitmap(bmp, 0, 0, width, height);
        view.destroyDrawingCache();
        return bp;

    }

    /**
     * 方法说明：获取当前屏幕截图，不包含状态栏
     *
     * @param activity
     * @return
     */
    public static Bitmap snapShotWithoutStatusBar(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bmp = view.getDrawingCache();
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;

        int width = getScreenWidth(activity);
        int height = getScreenHeight(activity);
        Bitmap bp = null;
        bp = Bitmap.createBitmap(bmp,   0, statusBarHeight, width, height
                - statusBarHeight);
        view.destroyDrawingCache();
        return bp;

    }

}
