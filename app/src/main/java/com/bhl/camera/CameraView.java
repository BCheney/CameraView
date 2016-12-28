package com.bhl.camera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 作者：边泓霖
 * 说明：相机View
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback,
        Camera.PictureCallback, Camera.AutoFocusCallback {
    private static final String TAG = "ARCameraView";
    private Activity mContext;
    /*Camera Holder*/
    private SurfaceHolder mHolder;
    /*Camera对象*/
    private Camera mCamera;
    /*Camera ID*/
    private int cameraId;
    /*Camera 后置id*/
    private int backFacing = 0;
    /*Camera 前置id*/
    private int frontFacing = 0;
    /*屏幕方向监听*/
    private OrientationEventListener mScreenOrientationEventListener;
    /*屏幕方向值*/
    private int mScreenExifOrientation;
    /*聚焦View*/
    private FocusView focusView;
    /*闪光灯模式：默认关闭*/
    private String flashMode = Camera.Parameters.FLASH_MODE_OFF;
    /*能否拍照，默认开启*/
    private boolean canTakePicture = true;
    /*拍照结果与闪光灯改变模式回调*/
    private CameraViewCallback cameraViewCallback;
    /*当前设备摄像头所支持预览大小*/
    private List<Camera.Size> mSupportedPreviewSizes;
    /*当前设备摄像头所支持最优预览大小*/
    private Camera.Size mPreviewSize;
    /*Camera 参数*/
    private Camera.Parameters parameters;
    /*缩放值*/
    private int zoom;
    /*触摸动作*/
    private int FOCUS = 1;            // 聚焦
    /*缩放动作*/
    private int ZOOM = 2;            // 缩放
    /*动作模式*/
    private int mode;
    /*当前ZOOMVALUE*/
    private int curZoomValue = 0;

    public CameraView(Activity context) {
        super(context);
        this.mContext = context;
        initCamera();
        initScreenOritation();
    }

    /* 初始化相机*/
    private void initCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int num = Camera.getNumberOfCameras();
        for (int i = 0; i < num; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backFacing = i;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
        }
        if (null != mCamera) {
            return;
        }
        //检查是否有摄像头，且是否被占用
        if (checkCameraHardware(mContext)) {
            mCamera = Camera.open(cameraId);
            if (null == mCamera) {
                return;
            }
            setCameraDisplayOrientation(cameraId);
            parameters = mCamera.getParameters();
            mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            mHolder = getHolder();
            mHolder.setFixedSize(PhoneUtils.getWidthPixels(), PhoneUtils.getHeightPixels());
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            try {
                initCameraParameters(mCamera);
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (IOException e) {
                //TODO 注意：因android设备碎片化太严重，各种厂商都喜欢定制自己的手机，所以为了保证不出现崩溃，许多调用Camera的地方都进行了try-catch
            }
        }
    }


    /*初始化方向传感器*/
    private void initScreenOritation() {
        //TODO 监听拍照的方向。由于摄像头会出现颠倒，翻转等现象，拍出的照片方向也会是错误的，
        mScreenOrientationEventListener = new OrientationEventListener(mContext) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (45 <= orientation && orientation < 135) {
                    mScreenExifOrientation = 90;
                } else if (135 <= orientation && orientation < 225) {
                    mScreenExifOrientation = 180;
                } else if (225 <= orientation && orientation < 315) {
                    mScreenExifOrientation = 270;
                } else {
                    mScreenExifOrientation = 0;
                }
            }
        };

    }

    /*获取显示方向*/
    public int getDisplayRotation() {
        int rotation = mContext.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    /*设置相机显示方向*/
    public void setCameraDisplayOrientation(int cameraId) {
        try {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            int degrees = getDisplayRotation();
            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360; // compensate the mirror
            } else { // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            mCamera.setDisplayOrientation(result);
        } catch (Exception e) {
        }
    }

    /**
     * 检查是否有摄像头
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    /*设置相机参数*/
    private void initCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setPictureFormat(PixelFormat.JPEG);//设置照片的输出格式
//            parameters.set("jpeg-quality", 100);//照片质量
        parameters.setFlashMode(flashMode);
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();// 获取支持预览照片的尺寸
        Camera.Size previewSize = getBestSupportedSize(supportedPreviewSizes);// 从List取出Size
        parameters.setPreviewSize(previewSize.width, previewSize.height);// 设置预览照片的大小
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();// 获取支持保存图片的尺寸
        Camera.Size pictureSize = getBestSupportedSize(supportedPictureSizes);// 从List取出Size
        parameters.setPictureSize(pictureSize.width, pictureSize.height);// 设置照片的大小
        try {
            camera.setParameters(parameters);
        } catch (Exception e) {
            setAlternativeParameters(camera);
        }
    }

    /*设置备选相机参数*/
    private void setAlternativeParameters(Camera camera) {
        int PreviewWidth = 0;
        int PreviewHeight = 0;
        Camera.Parameters parameters = camera.getParameters();
        // 选择合适的预览尺寸
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();

        // 如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择
        if (sizeList.size() > 1) {
            Iterator<Camera.Size> itor = sizeList.iterator();
            while (itor.hasNext()) {
                Camera.Size cur = itor.next();
                if (cur.width >= PreviewWidth
                        && cur.height >= PreviewHeight) {
                    PreviewWidth = cur.width;
                    PreviewHeight = cur.height;
                    break;
                }
            }
        }
        parameters.setPreviewSize(PreviewWidth, PreviewHeight); //获得摄像区域的大小
        parameters.setPictureFormat(PixelFormat.JPEG);//设置照片输出的格式
        parameters.setPictureSize(PreviewWidth, PreviewHeight);//设置拍出来的屏幕大小
        try {
            camera.setParameters(parameters);//把上面的设置 赋给摄像头
        } catch (Exception e) {
        }
    }

    private Camera.Size getBestSupportedSize(List<Camera.Size> sizes) {
        // 取能适用的最大的SIZE
        Camera.Size largestSize = sizes.get(0);
        int largestArea = sizes.get(0).height * sizes.get(0).width;
        for (Camera.Size s : sizes) {
            int area = s.width * s.height;
            if (area > largestArea) {
                largestArea = area;
                largestSize = s;
            }
        }
        return largestSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    /*获取预览大小等参数*/
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private float downX, downY;
    private float lastX;
    private float touchSlot = 10f;
    private double dist;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                lastX = downX;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float dx = x - lastX;
                lastX = x;
                break;
            case MotionEvent.ACTION_UP:
                float upX = event.getX();
                float upY = event.getY();
                if (Math.abs(upX - downX) < touchSlot && Math.abs(upY - downY) < touchSlot) {
                    onFocus(upX, upY);
                }
                break;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // 主点按下
            case MotionEvent.ACTION_DOWN:
                mode = FOCUS;
                break;
            // 副点按下
            case MotionEvent.ACTION_POINTER_DOWN:
                dist = spacing(event);
                // 如果连续两点距离大于10，则判定为多点模式
                if (spacing(event) > 10f) {
                    mode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = FOCUS;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == FOCUS) {
                } else if (mode == ZOOM) {
                    double newDist = spacing(event);
                    if (newDist > 10f) {
                        double tScale = (newDist - dist) / dist;
                        if (tScale < 0) {
                            tScale = tScale * 10;
                        }
                        addZoomIn((int) tScale);
                    }
                }
                break;
        }
        return true;
    }

    /* 两点的距离 */
    private double spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return Math.sqrt(x * x + y * y);
    }

    /*根据手势调整焦距*/
    private void addZoomIn(int delta) {
        try {
            Camera.Parameters params = mCamera.getParameters();
            if (!params.isZoomSupported()) {
                return;
            }
            curZoomValue += delta;
            if (curZoomValue < 0) {
                curZoomValue = 0;
            } else if (curZoomValue > params.getMaxZoom()) {
                curZoomValue = params.getMaxZoom();
            }

            if (!params.isSmoothZoomSupported()) {
                zoom(curZoomValue);
                return;
            } else {
                mCamera.startSmoothZoom(curZoomValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*聚焦*/
    public void onFocus(float x, float y) {
        Rect touchRect = new Rect(
                (int) (x - 100),
                (int) (y - 100),
                (int) (x + 100),
                (int) (y + 100));

        final Rect targetFocusRect = new Rect(
                touchRect.left * 2000 / this.getWidth() - 1000,
                touchRect.top * 2000 / this.getHeight() - 1000,
                touchRect.right * 2000 / this.getWidth() - 1000,
                touchRect.bottom * 2000 / this.getHeight() - 1000);

        doTouchFocus(targetFocusRect);
        if (null != focusView) {
            focusView.setHaveTouch(true, touchRect);
            focusView.invalidate();

            // Remove the square after some time
            focusView.postDelayed(new Runnable() {

                @Override
                public void run() {
                    focusView.setHaveTouch(false, new Rect(0, 0, 0, 0));
                    focusView.invalidate();
                }
            }, 1000);
        }
    }

    /*触摸聚焦*/
    private void doTouchFocus(final Rect tfocusRect) {
        try {
            List<Camera.Area> focusList = new ArrayList<Camera.Area>();
            Camera.Area focusArea = new Camera.Area(tfocusRect, 1000);
            focusList.add(focusArea);
            Camera.Parameters para = mCamera.getParameters();
            para.setFocusAreas(focusList);
            para.setMeteringAreas(focusList);
            mCamera.setParameters(para);
            mCamera.autoFocus(this);
        } catch (Exception e) {
        }
    }

    /*设置聚焦View*/
    public void setFocusView(FocusView focusView) {
        this.focusView = focusView;
    }

    /*设置CameraViewCallback回调，闪光灯改变与拍照结果*/
    public void setCameraViewCallback(CameraViewCallback cameraViewCallback) {
        this.cameraViewCallback = cameraViewCallback;
    }

    /*是否可以拍照*/
    public boolean canTakePicture() {
        return canTakePicture;
    }

    /*设置当前可以拍摄*/
    public void setCanTakePicture(boolean canTakePicture) {
        this.canTakePicture = canTakePicture;
    }

    /*切换前置后置摄像头*/
    public void switchCamera() {
        try {
            if (null != mCamera) {
                if (cameraId == backFacing) {
                    cameraId = frontFacing;
                } else {
                    cameraId = backFacing;
                }
            }
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        } catch (Exception e) {
        }

        mCamera.release();
        mCamera = null;
        initCamera();
        initCameraParameters(mCamera);
    }


    /*切换闪光等模式*/
    public void toggleFlashMode() {
        if (Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
            flashMode = Camera.Parameters.FLASH_MODE_ON;
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {
            flashMode = Camera.Parameters.FLASH_MODE_AUTO;
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(flashMode)) {
            flashMode = Camera.Parameters.FLASH_MODE_OFF;
        }
        if (null != cameraViewCallback) {
            cameraViewCallback.onFlashModeChange(flashMode);
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }
        initCameraParameters(mCamera);
        try {
            mCamera.startPreview();
        } catch (Exception e) {
        }
    }

    /*在Activity OnResume中调用*/
    public void resume() {
        mScreenOrientationEventListener.enable();
    }

    /*在Activity OnDestroy中调用*/
    public void destroy() {
        mScreenOrientationEventListener.disable();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        System.gc();
    }

    /*放大缩小*/
    public void zoom(int value) {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setZoom(value);
        mCamera.setParameters(parameters);
    }

    /*获取当前焦距*/
    public int getZoom() {
        Camera.Parameters parameters = mCamera.getParameters();
        return parameters.getZoom();
    }

    /*获取屏幕方向*/
    public int getScreenOrientation() {
        if (mScreenOrientationEventListener != null) {
            return mScreenExifOrientation;
        }
        return mScreenExifOrientation;
    }

    /*拍照*/
    public void takePicture() {
        if (null != mCamera && canTakePicture()) {
            mCamera.takePicture(null, null, this);
            setCanTakePicture(false);
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        zoom = getZoom();
        if (cameraViewCallback != null) {
            cameraViewCallback.onPictureTaken(data, camera);
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {

        }
        initCameraParameters(mCamera);
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            zoom(zoom);
        } catch (Exception e) {
        }
        canTakePicture = true;

    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            mCamera.cancelAutoFocus();
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            initCameraParameters(mCamera);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            return;
        }
        mCamera.stopPreview();
        try {
            initCameraParameters(mCamera);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    /*回调接口：闪光灯改变回调、拍摄结果回调*/
    public interface CameraViewCallback {
        void onFlashModeChange(String mode);
        void onPictureTaken(byte[] data, Camera camera);
    }

}
