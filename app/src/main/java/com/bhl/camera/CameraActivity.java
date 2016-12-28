package com.bhl.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class CameraActivity extends AppCompatActivity implements View.OnClickListener, CameraView.CameraViewCallback {
    private CameraView cameraView;
    private LinearLayout mLinearLayout;
    private FrameLayout mPreviewLayout;
    private RelativeLayout activity_main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initView();
    }

    private void initView() {
        findViewById(R.id.btn_camera_shutter).setOnClickListener(this);
        findViewById(R.id.btn_camera_switch).setOnClickListener(this);
        findViewById(R.id.btn_camera_mode).setOnClickListener(this);
        activity_main = (RelativeLayout) findViewById(R.id.activity_main);
        mPreviewLayout = (FrameLayout) findViewById(R.id.framelayout_preview);
        cameraView = new CameraView(this);
        cameraView.setFocusView(new FocusView(this));
        cameraView.setCameraViewCallback(this);
        mPreviewLayout.addView(cameraView);
        mLinearLayout = new LinearLayout(this);
        activity_main.addView(mLinearLayout, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.resume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length != 1 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhoto();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    /*拍照*/
    private void takePhoto() {
        cameraView.takePicture();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_camera_mode:
                cameraView.toggleFlashMode();
                break;
            case R.id.btn_camera_switch:
                cameraView.switchCamera();
                break;
            case R.id.btn_camera_shutter:
                if (PermissionChecker.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            v.getId());
                } else {
                    takePhoto();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        cameraView.destroy();
        super.onDestroy();
    }

    @Override
    public void onFlashModeChange(String mode) {
        if (Camera.Parameters.FLASH_MODE_OFF.equals(mode)) {
            toast("关闭闪光灯");
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(mode)) {
            toast("打开闪光灯");
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(mode)) {
            toast("自动闪光灯");
        }
    }


    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        //TODO 获取到照片数据，可以将data转换成Bitmap 然后进行调整方向，调用cameraView.getScreenOrientation()获取当前设备拍照角度，保证照片方向正确
        //TODO 注意：调用cameraView.getScreenOrientation() 请先调用cameraView.resume();
        //Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        //bitmap = rotateBitmapByDegree(bitmap,cameraView.getScreenOrientation());

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), "BHLCamera");
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");
        byte2File(data, mediaStorageDir.getAbsolutePath(), mediaFile.getName());
    }

    private void toast(String content) {
        Toast.makeText(this, content, Toast.LENGTH_LONG).show();
    }

    public Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                    bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

    public void byte2File(byte[] buf, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {
                dir.mkdirs();
            }
            File file = new File(filePath + File.separator + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(buf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
