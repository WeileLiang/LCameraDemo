package com.sensetime.lcamerademo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private static final int REQUEST_CODE_FOR_CAMERA = 100;

    private String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    int SENSOR_UP = 0;
    int SENSOR_LEFT = 90;
    int SENSOR_DOWN = 180;
    int SENSOR_RIGHT = 270;

    private SensorManager sensorManager;

    private int mCameraId = 0;
    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private Camera mCamera;
    private Camera.Parameters mParameters;
    int mSensorDegrees = SENSOR_UP;

    private Camera.Size mPreviewSize;
    private Camera.Size mPictureSize;

    private AutoFitTextureView autoFitTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // 给 TextureView 配置 Matrix 转换。在相机预览尺寸和 TextureView 尺寸确定之后调用该函数。
            if (autoFitTextureView == null || mPreviewSize == null) {
                return;
            }
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, width, height);
            RectF bufferRect = new RectF(0, 0, mPreviewSize.height, mPreviewSize.width);
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                        (float) height / mPreviewSize.width,
                        (float) width / mPreviewSize.height);
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            } else if (Surface.ROTATION_180 == rotation) {
                matrix.postRotate(180, centerX, centerY);
            }
            autoFitTextureView.setTransform(matrix);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean res = checkPermissionsAndRequestIfNeed();
        setContentView(R.layout.activity_main);

        if (!res) return;

        sensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
        autoFitTextureView = findViewById(R.id.texture);
        findViewById(R.id.btn_capture).setOnClickListener(this);
    }

    private void openCamera(int width, int height) {
        Camera.getCameraInfo(mCameraId, mCameraInfo);
        Log.d("lwl_orientation", mCameraInfo.orientation + "");

        mCamera = Camera.open(mCameraId);
        mParameters = mCamera.getParameters();

        List<String> supportedFocusModes = mParameters.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        mCamera.cancelAutoFocus();

        List<String> supportedFlashModes = mParameters.getSupportedFlashModes();
        if (supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }

        mPreviewSize = getOptimalSize(mParameters.getSupportedPreviewSizes(), width, height);
        mPictureSize = Collections.max(mParameters.getSupportedPictureSizes(), new CompareByArea());

        mParameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        mParameters.setPictureSize(mPreviewSize.width, mPreviewSize.height);

        mCamera.setParameters(mParameters);
        mCamera.setDisplayOrientation(getOrientation());
        try {
            mCamera.setPreviewTexture(autoFitTextureView.getSurfaceTexture());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 开始预览。

    }

    @Override
    protected void onResume() {
        super.onResume();
        // 注册传感器监听器。
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        if (autoFitTextureView.isAvailable() && autoFitTextureView.getSurfaceTexture() != null) {
            openCamera(autoFitTextureView.getWidth(), autoFitTextureView.getHeight());
        } else {
            autoFitTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_capture: {
                takePicture();
                break;
            }

        }
    }

    private int getOrientation() {
//        if (isBackCamera()) {
        return (mCameraInfo.orientation - mSensorDegrees + 360) % 360;
//        } else {
//            return (cameraInfo.orientation + mSensorDegrees + 180) % 360;
//        }
    }

    private int getRotation() {
//        if (isBackCamera()) {
        return (mCameraInfo.orientation - mSensorDegrees + 360) % 360;
//        } else {
//            return (cameraInfo.orientation + mSensorDegrees + 180) % 360;
//        }
    }

    Camera.Size getOptimalSize(List<Camera.Size> sizeList, int viewWidth, int viewHeight) {


        final double aspectTolerance = 0.1;
        double targetRatio = (double) viewHeight / viewWidth;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizeList) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > aspectTolerance) {
                continue;
            }
            if (Math.abs(size.height - viewHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - viewHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizeList) {
                if (Math.abs(size.height - viewHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - viewHeight);
                }
            }
        }
        return optimalSize;
    }

    private void takePicture() {
        mCamera.cancelAutoFocus();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                // 自动对焦成功后拍照。
                captureStillPicture();
            }
        });
    }

    private void captureStillPicture() {
        mParameters.setRotation(getRotation());
        mCamera.setParameters(mParameters);
        mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        savePicture(data);
                    }
                });
                Toast.makeText(MainActivity.this, "Capture completed!", Toast.LENGTH_SHORT).show();
                camera.cancelAutoFocus();
                camera.startPreview();
            }
        });
    }

    private void savePicture(byte[] data) {
        FileOutputStream output = null;
        try {
            File parent=new File("/sdcard/DCIM/burstDualCam");
            if(!parent.exists()) parent.mkdirs();
            output = new FileOutputStream(new File(parent,generateTimestamp()+".jpg"));
            output.write(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String generateTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);
        return sdf.format(new Date());
    }

    private Camera.Size chooseOptimalSize(List<Camera.Size> sizes, int viewWidth, int viewHeight) {
        double tolerance = 0.01;
        double targetRatio = viewHeight / viewWidth;

        Camera.Size previewSize = null;
        int minDiff = Integer.MAX_VALUE;
        for (Camera.Size size : sizes) {
            if ((Math.abs(targetRatio - (double) size.width / size.height) > tolerance)) continue;

            if (Math.abs(size.width - viewHeight) < minDiff) {
                minDiff = Math.abs(size.width - viewHeight);
                previewSize = size;
            }
        }

        if (previewSize == null) {
            for (Camera.Size size : sizes) {
                if (Math.abs(size.width - viewHeight) < minDiff) {
                    minDiff = Math.abs(size.width - viewHeight);
                    previewSize = size;
                }
            }
        }

        return previewSize;

    }

    private boolean checkPermissionsAndRequestIfNeed() {
        boolean result = true;
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissionsToBeRequested = new ArrayList<>();
            for (String permission : permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToBeRequested.add(permission);
                    result = false;
                }
            }
            if (!permissionsToBeRequested.isEmpty()) {
                requestPermissions(permissionsToBeRequested.toArray(new String[0]), REQUEST_CODE_FOR_CAMERA);
            }
        }

        return result;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // 传感器方向发生改变。
        synchronized (this) {
            if (sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
                return;
            }
            final int oldDegrees = mSensorDegrees;
            float minXY = -1.5F, maxXY = 1.5F;
            float x = sensorEvent.values[0], y = sensorEvent.values[1];
            if (x < maxXY && x > minXY) {
                if (y > maxXY) {
                    mSensorDegrees = SENSOR_UP;
                } else if (y < minXY) {
                    mSensorDegrees = SENSOR_DOWN;
                }
            } else if (y < maxXY && y > minXY) {
                if (x > maxXY) {
                    mSensorDegrees = SENSOR_LEFT;
                } else if (x < minXY) {
                    mSensorDegrees = SENSOR_RIGHT;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    class CompareByArea implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            return Long.signum((long) lhs.width * lhs.height - (long) rhs.width * rhs.height);
        }
    }

}
