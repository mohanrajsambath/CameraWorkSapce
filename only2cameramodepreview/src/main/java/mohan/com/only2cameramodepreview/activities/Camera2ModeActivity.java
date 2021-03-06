package mohan.com.only2cameramodepreview.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import mohan.com.only2cameramodepreview.R;
import mohan.com.only2cameramodepreview.callback.PreviewSessionCallback;
import mohan.com.only2cameramodepreview.listeners.JpegReaderListener;
import mohan.com.only2cameramodepreview.listeners.TextureViewTouchEvent;
import mohan.com.only2cameramodepreview.views.CustomTextureView;

public class Camera2ModeActivity extends AppCompatActivity implements View.OnClickListener{
    public Activity getActivityContext;
    public CustomTextureView mTextureView;
    public ImageView btnChangeCamera;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int STATE_PREVIEW = 1;
    private int mState = 0;
    public static final int FOCUS_AGAIN = 102;
    public Size mPreviewSize;
    public Size mlargest;
    private CameraDevice mCameraDevice;
    public CameraManager mCameraManager;
    public CameraCharacteristics mCameraCharacteristics = null;
    public StreamConfigurationMap mStreamConfigurationMap;
    public String mCameraId="0";//0-back,1-front camera.
    public HandlerThread mHandlerThread;
    public Handler mHandler;
    public int mFormat;
    public ImageReader mImageReader;
    public Surface mSurface;
    public List<Surface> mOutputSurfaces;
    private int mToPreviewWidth,mToPreviewHeight;
    //reopen
    public CameraCaptureSession mCameraCaptureSession;

    private CaptureRequest.Builder mPreviewBuilder;
    private PreviewSessionCallback mPreviewSessionCallback;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActivityContext = Camera2ModeActivity.this;
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }
//1
    private void initUI() {
        mTextureView = (CustomTextureView) findViewById(R.id.textureview);
        btnChangeCamera = (ImageView) findViewById(R.id.imgVw_cameraMode);
        btnChangeCamera.setOnClickListener(this);
        // Invoke Camera Access by SurfaceTextureListener
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

    }
//2
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.imgVw_cameraMode:
                reOpenCamera(mToPreviewWidth, mToPreviewHeight);
                break;
            default:
                break;
        }
    }
//3
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(getActivityContext, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

//4
    /*Texture listener*/

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int widthPrev, int heightPrev) {
            Log.i("SurfaceTextureListener", "onSurfaceTextureAvailable");
            mToPreviewWidth = widthPrev;
            mToPreviewHeight = heightPrev;
            try {

                openCamera(widthPrev, heightPrev);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int widthPrev, int heightPrev) {
            Log.i("SurfaceTextureListener", "onSurfaceTextureSizeChanged");
            configureTransform(widthPrev, heightPrev);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            Log.i("SurfaceTextureListener", "onSurfaceTextureDestroyed");
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            //Log.i("SurfaceTextureListener", "onSurfaceTextureUpdated");
        }
    };



//5
    private void openCamera(int viewWidth, int viewHeight) throws CameraAccessException {
        /*Initialize child thread and handler to handle Camera*/
        initHandler();
        //Get the camera service
        mCameraManager = (CameraManager) getActivityContext.getSystemService(Context.CAMERA_SERVICE);
        //Get the Camera Characteristics
        getCameraCharacterisitcs();
        //set the display size
        setUpCameraOutputs(viewWidth, viewHeight);
        //Configure Screen Orientation
        configureTransform(viewWidth, viewHeight);
        //Initialize Output of SurfaceTexturePreview
        initOutputSurface();
        if (ActivityCompat.checkSelfPermission(getActivityContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivityContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivityContext, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        //Turn on the camera
        mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler);
        //Invoke  newPreviewSession CallBack
        //newPreviewSession();
    }

//6
    /*Initialize child thread and handler*/
    private void initHandler() {
        mHandlerThread = new HandlerThread("Android_L_Camera");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }



//7
    /*Configure Preview based on the ScreenOrientation*/
    private void configureTransform(int widthPrev, int heightPrev) {
        if (null == mTextureView || null == mPreviewSize || null == getActivityContext) {
            return;
        }
        int rotation = getActivityContext.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, widthPrev, heightPrev);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) heightPrev / mPreviewSize.getHeight(),
                    (float) widthPrev / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }
//8
    private void getCameraCharacterisitcs(){
        mCameraManager= (CameraManager) getActivityContext.getSystemService(Context.CAMERA_SERVICE);
        String cameraId = null;
        try {
            cameraId = Objects.requireNonNull(mCameraManager).getCameraIdList()[Integer.parseInt(mCameraId)];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        try {
            mCameraCharacteristics = Objects.requireNonNull(mCameraManager).getCameraCharacteristics(Objects.requireNonNull(cameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mStreamConfigurationMap= Objects.requireNonNull(mCameraCharacteristics).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mPreviewSize = Objects.requireNonNull(mStreamConfigurationMap).getOutputSizes(SurfaceTexture.class)[Integer.parseInt(mCameraId)];
    }

    /**
     * set the display size
     * @throws CameraAccessException
     */
    private void setUpCameraOutputs(int width, int height) throws CameraAccessException {

        //mFormat = mSp.getInt("format", 256);
        mFormat = 256;
        if (mCameraId.equals("0") && mFormat == ImageFormat.JPEG) {
            mlargest = Collections.max(Arrays.asList(mStreamConfigurationMap.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            mPreviewSize = chooseOptimalSize(mStreamConfigurationMap.getOutputSizes(SurfaceTexture.class), width, height, mlargest);
        } else if (mCameraId.equals("0") && mFormat == ImageFormat.RAW_SENSOR) {
            mlargest = Collections.max(Arrays.asList(mStreamConfigurationMap.getOutputSizes(ImageFormat.RAW_SENSOR)), new CompareSizesByArea());
            mPreviewSize = chooseOptimalSize(mStreamConfigurationMap.getOutputSizes(SurfaceTexture.class), width, height, mlargest);
        } else {
            mlargest = Collections.max(Arrays.asList(mStreamConfigurationMap.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            mPreviewSize = chooseOptimalSize(mStreamConfigurationMap.getOutputSizes(SurfaceTexture.class), width, height, mlargest);
        }
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTextureView.fitWindow(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else {
            mTextureView.fitWindow(mPreviewSize.getHeight(), mPreviewSize.getWidth());
        }
    }
//9
    private class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
//10
    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e("nonono", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

//11
    private void initOutputSurface() {
        //Image Format
        mFormat =256;
        //size of the picture
        mImageReader = ImageReader.newInstance(mlargest.getWidth(), mlargest.getHeight(), mFormat, 2);
        if (mFormat == ImageFormat.JPEG) {
            mImageReader.setOnImageAvailableListener(new JpegReaderListener(), mHandler);
        } else if (mFormat == ImageFormat.RAW_SENSOR) { }

        //SurfaceTexture
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //Displayed Surface
        mSurface = new Surface(texture);
        //Get the surface
        mOutputSurfaces = new ArrayList<Surface>(1);
        //mOutputSurfaces.add(mImageReader.getSurface());
        mOutputSurfaces.add(mSurface);
    }
//12
    /*Generate a PreviewSession object*/
    private void newPreviewSession() {
        //mPreviewSessionCallback = new PreviewSessionCallback(mMainHandler);
        mPreviewSessionCallback = new PreviewSessionCallback(mMainHandler, mTextureView);
    }

//13
    /*Change cameraID*/
    private void reOpenCamera(int viewWidth, int viewHeight) {
        getCameraCharacterisitcs();
        if (mCameraId.equals("1")) {
            System.out.println("QT-Front Camera");
            System.out.println("QT-CameraID:"+mCameraId);
            btnChangeCamera.setImageResource(R.drawable.ic_camera_front);
            mCameraId = "0";
            mPreviewSize = new Size(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else if (mCameraId.equals("0")) {
            System.out.println("QT-Back Camera");
            System.out.println("QT-CameraID:"+mCameraId);
            btnChangeCamera.setImageResource(R.drawable.ic_camera_rear);
            mCameraId = "1";
            mPreviewSize = new Size(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else {
            mCameraId = "0";
            System.out.println("QT- Last State Back Camera");
            System.out.println("QT-Last State CameraID:"+mCameraId);
        }
        //Turn off the camera and turn on another camera
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        try {
            setUpCameraOutputs(viewWidth, viewHeight);
            configureTransform(viewWidth, viewHeight);
            if (ActivityCompat.checkSelfPermission(getActivityContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivityContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivityContext, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback,mHandler);
            //Invoke  newPreviewSession CallBack
            //newPreviewSession();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

//14
    /*Camera state callback
    and then turn on the camera in onOpened*/
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onClosed(CameraDevice camera) {
            Log.i("CameraDevice.SCB", "onClosed");
        }

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.i("Thread", "onOpened---->" + Thread.currentThread().getName());
            Log.i("CameraDevice.SCB", "onOpened");
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.i("CameraDevice.SCB", "onDisconnected");
            Toast.makeText(getActivityContext, "onDisconnected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            Log.i("CameraDevice.SCB", "onError--->" + i + ",,,null == cameraDevice--->" + (null == cameraDevice));
            Toast.makeText(getActivityContext, "onError", Toast.LENGTH_SHORT).show();
        }
    };
//15
    /*Session state callback
    Continue preview*/
    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            Log.i("Thread", "onConfigured---->" + Thread.currentThread().getName());
            Log.i("CaptureStateCallback", "mSessionStateCallback--->onConfigured");
            try {
                mCameraCaptureSession = cameraCaptureSession;
               // setListener();
                cameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mPreviewSessionCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            Log.i("CaptureStateCallback", "mSessionStateCallback--->onConfigureFailed");
            Toast.makeText(getActivityContext, "onConfigureFailed---Preview", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            super.onReady(session);
            Log.i("CaptureStateCallback", "mSessionStateCallback--->onReady");
        }

        @Override
        public void onActive(CameraCaptureSession session) {
            super.onActive(session);
            Log.i("CaptureStateCallback", "mSessionStateCallback--->onActive");
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            super.onClosed(session);
            Log.i("CaptureStateCallback", "mSessionStateCallback--->onClosed");
        }
    };
//16
    /*Start Preview*/
    private void startPreview() {
        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(mSurface);
            initPreviewBuilder();
            //3A--->auto
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            //3A
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
            mState = STATE_PREVIEW;
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface), mSessionPreviewStateCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
//17
    /*Initialize the preview of the builder,
        this is done in order to adjust the iso
         when the ae is not the lowest*/
    private void initPreviewBuilder() {
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
    }
//18
    /*Update preview*/
    private void updatePreview() {
        try {
            mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mPreviewSessionCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("updatePreview", "ExceptionExceptionException");
        }
    }
//19
    /*UI thread handler*/
    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FOCUS_AGAIN:
                    Log.i("FOCUS_AGAIN", "FOCUS_AGAINFOCUS_AGAINFOCUS_AGAIN");
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                    updatePreview();
                    break;
            }
        }
    };
//20
    @Override
    public void onStop() {
        super.onStop();
        Log.i("onDestroy", "onDestroy");
        closeCamera();
    }
//21
    /**
     * Turn off the camera
     */
    private void closeCamera() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mHandler != null) {
            try {
                //Close thread
                mHandlerThread.quitSafely();
                mHandlerThread.join();
                mHandlerThread = null;
                mHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
//22
    /*Set the focus listener
   TextureView of touch*/
    private void setListener() {
        /*The touch zoom monitor is set when the display starts.*/
        //mTextureView.setmMyTextureViewTouchEvent(new TextureViewTouchEvent(mCameraCharacteristics, mTextureView, mPreviewBuilder, mCameraCaptureSession, mHandler, mPreviewSessionCallback));
    }

}
