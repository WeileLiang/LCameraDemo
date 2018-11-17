package com.sensetime.lcamerademo;

import java.io.IOException;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * ����һ��Ԥ����
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "main";
	private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // ͨ��SurfaceView���SurfaceHolder
        mHolder = getHolder();
        // ΪSurfaceHolderָ���ص�
        mHolder.addCallback(this);
        // ����Surface��ά���Լ��Ļ����������ǵȴ���Ļ����Ⱦ���潫�������͵����� ��Android3.0֮������
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // ��Surface������֮�󣬿�ʼCamera��Ԥ��
        try {
        	mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Ԥ��ʧ��");
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    	
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Surface�����ı��ʱ�򽫱����ã���һ����ʾ�������ʱ��Ҳ�ᱻ����
        if (mHolder.getSurface() == null){
          // ���SurfaceΪ�գ�����������
          return;
        }

        // ֹͣCamera��Ԥ��
        try {
            mCamera.stopPreview();
        } catch (Exception e){
        	Log.d(TAG, "��Surface�ı��ֹͣԤ������");
        }

        // ��Ԥ��ǰ����ָ��Camera�ĸ������

        // ���¿�ʼԤ��
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Ԥ��Camera����");
        }
    }
}
