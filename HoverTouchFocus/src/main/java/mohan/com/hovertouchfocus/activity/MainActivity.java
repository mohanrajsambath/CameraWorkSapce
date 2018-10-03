package mohan.com.hovertouchfocus.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import mohan.com.hovertouchfocus.R;
import mohan.com.hovertouchfocus.callback.PreviewSessionCallback;
import mohan.com.hovertouchfocus.customviews.AnimationImageView;
import mohan.com.hovertouchfocus.customviews.AnimationTextView;
import mohan.com.hovertouchfocus.customviews.CustomTextureView;
import mohan.com.hovertouchfocus.customviews.PreferenceHelper;
import mohan.com.hovertouchfocus.listeners.JpegReaderListener;
import mohan.com.hovertouchfocus.listeners.TextureViewTouchEvent;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    CustomTextureView mTextureView;
    private int mToPreviewWidth;
    private int mToPreviewHeight;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private String mCameraId = "0";
    private Size mPreviewSize;
    private int mFormat;
    private SharedPreferences mSp;
    private SharedPreferences.Editor mEditor;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraCharacteristics mCameraCharacteristics;
    private Size mlargest;
    private CaptureRequest.Builder mPreviewBuilder;
    private Surface mSurface;
    private Activity getActivityContext;
    private ImageReader mImageReader;
    private List<Surface> mOutputSurfaces;

    private static final int STATE_PREVIEW = 1;
    private float valueAF;
    private int valueAE;
    private long valueAETime;
    private int valueISO;
    private int mState = 0;
    private PreviewSessionCallback mPreviewSessionCallback;
    public static final int FOCUS_DISAPPEAR = 100;
    public static final int WINDOW_TEXT_DISAPPEAR = 101;
    public static final int FOCUS_AGAIN = 102;

    private static final int SHOW_AF = 1;
    private static final int SHOW_AE = 2;
    private static final int SHOW_AWB = 3;
    private static final int SHOW_ISO = 4;
    private static final int SHOW_ZOOM = 5;
    private static final int SHOW_ZOOM_2 = 6;
    private AnimationImageView mFocusImage;
    private AnimationTextView mWindowTextView;
    private ImageView btnChangeCamera;
    private ScaleAnimation mScaleFocusAnimation;
    private AlphaAnimation mAlphaInAnimation;
    private AlphaAnimation mAlphaOutAnimation;
    private ScaleAnimation mScaleWindowAnimation;
    private TranslateAnimation mShowAction;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActivityContext = MainActivity.this;
        initUI();
        initFocusImage();
        initAnimation();
    }

    private void initUI() {
        mTextureView = (CustomTextureView) findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        mWindowTextView = (AnimationTextView) findViewById(R.id.txt_window_txt);
        mWindowTextView.setVisibility(View.INVISIBLE);
        mWindowTextView.setmAnimation(mScaleWindowAnimation);
        mWindowTextView.setmMainHandler(mMainHandler);

        mFocusImage = (AnimationImageView) findViewById(R.id.img_focus);
        mFocusImage.setVisibility(View.INVISIBLE);
        mFocusImage.setmMainHandler(mMainHandler);
        mFocusImage.setmAnimation(mScaleFocusAnimation);
        btnChangeCamera = (ImageView) findViewById(R.id.imgVw_cameraMode);
        btnChangeCamera.setOnClickListener(this);
    }

    private void initAnimation() {
        mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        mShowAction.setDuration(500);
        mScaleFocusAnimation = new ScaleAnimation(2.0f, 1.0f, 2.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mScaleFocusAnimation.setDuration(200);
        mScaleWindowAnimation = new ScaleAnimation(2.0f, 1.0f, 2.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mScaleWindowAnimation.setDuration(500);
        mAlphaInAnimation = new AlphaAnimation(0.0f, 1.0f);
        mAlphaInAnimation.setDuration(500);
        mAlphaOutAnimation = new AlphaAnimation(1.0f, 0.0f);
        mAlphaOutAnimation.setDuration(500);
    }

    /*Texture listener*/

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
            Log.i("SurfaceTextureListener", "onSurfaceTextureAvailable");
            mToPreviewWidth = i;
            mToPreviewHeight = i2;
            try {
                openCamera(i, i2);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
            Log.i("SurfaceTextureListener", "onSurfaceTextureSizeChanged");
            configureTransform(i, i2);
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


    /**
     * In the official sample
     *
     * @param viewWidth
     * @param viewHeight
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivityContext;
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void openCamera(int viewWidth, int viewHeight) throws CameraAccessException, InterruptedException {
        initHandler();//Initialize child thread and handler
        //Get the camera service
        mCameraManager = (CameraManager) getActivityContext.getSystemService(Context.CAMERA_SERVICE);
        setUpCameraOutputs(viewWidth, viewHeight);
        configureTransform(viewWidth, viewHeight);
        initOutputSurface();
        //Turn on the camera
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //Turn on the camera
        mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler);
        newPreviewSession();
    }

    /*Generate a PreviewSession object*/
    private void newPreviewSession() {
        //mPreviewSessionCallback = new PreviewSessionCallback(mMainHandler, mTextureView);
        mPreviewSessionCallback = new PreviewSessionCallback(mFocusImage, mMainHandler, mTextureView);
    }

    /*Initialize child thread and handler*/
    private void initHandler() {
        mHandlerThread = new HandlerThread("Android_L_Camera");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    /*This is done to get the height and width of the mFocusImage*/
    private void initFocusImage() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mFocusImage.setLayoutParams(layoutParams);
        mFocusImage.initFocus();
    }


    /**
     * Get information such as CameraCharacteristics,
     * set the display size
     *
     * @throws CameraAccessException
     */
    private void setUpCameraOutputs(int width, int height) throws CameraAccessException {
        //mFormat = mSp.getInt("format", 256);
        mFormat = 256;
        //Size mPreviewSize;
        CameraManager manager = (CameraManager) getActivityContext.getSystemService(Context.CAMERA_SERVICE);
        String cameraId = null;
        try {
            cameraId = Objects.requireNonNull(manager).getCameraIdList()[Integer.parseInt(mCameraId)];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        CameraCharacteristics characteristics = null;
        try {
            characteristics = Objects.requireNonNull(manager).getCameraCharacteristics(Objects.requireNonNull(cameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        //StreamConfigurationMap map = Objects.requireNonNull(characteristics).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        //CameraDevice
        mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mPreviewSize = Objects.requireNonNull(map).getOutputSizes(SurfaceTexture.class)[Integer.parseInt(mCameraId)];
        if (mCameraId.equals("0") && mFormat == ImageFormat.JPEG) {
            mlargest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mlargest);
        } else if (mCameraId.equals("0") && mFormat == ImageFormat.RAW_SENSOR) {
            mlargest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.RAW_SENSOR)), new CompareSizesByArea());
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mlargest);
        } else {
//            mPreviewSize = new Size(1280, 720);
            mlargest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mlargest);
        }
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTextureView.fitWindow(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else {
            mTextureView.fitWindow(mPreviewSize.getHeight(), mPreviewSize.getWidth());
        }
    }


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

    @Override
    public void onClick(View view) {
        mFocusImage.stopFocus();
        switch (view.getId()) {
            case R.id.imgVw_cameraMode:
                reOpenCamera(mToPreviewWidth, mToPreviewHeight);
                break;
        }

    }

    private class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private void initOutputSurface() {
        //Image Format
        // mFormat = mSp.getInt("format", 256);
        mFormat =256;
        //size of the picture
        /*int sizeWidth = mSp.getInt("format_" + mFormat + "_pictureSize_width", 1280);
        int sizeHeight = mSp.getInt("format_" + mFormat + "_pictureSize_height", 960);
*/
        int sizeWidth =1920;
        int sizeHeight = 720;

        mImageReader = ImageReader.newInstance(mlargest.getWidth(), mlargest.getHeight(), mFormat, 2);
        if (mFormat == ImageFormat.JPEG) {
            mImageReader.setOnImageAvailableListener(new JpegReaderListener(), mHandler);
        } else if (mFormat == ImageFormat.RAW_SENSOR) {

        }
        Size mPreviewSize;
        CameraManager manager = (CameraManager) getActivityContext.getSystemService(Context.CAMERA_SERVICE);
        String cameraId = null;
        try {
            cameraId = Objects.requireNonNull(manager).getCameraIdList()[Integer.parseInt(mCameraId)];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        CameraCharacteristics characteristics = null;
        try {
            characteristics = Objects.requireNonNull(manager).getCameraCharacteristics(Objects.requireNonNull(cameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        StreamConfigurationMap map = Objects.requireNonNull(characteristics).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mPreviewSize = Objects.requireNonNull(map).getOutputSizes(SurfaceTexture.class)[Integer.parseInt(mCameraId)];
//SurfaceTexture
        SurfaceTexture texture = mTextureView.getSurfaceTexture();

        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //Displayed Surface
        mSurface = new Surface(texture);
        //Get the surface
        mOutputSurfaces = new ArrayList<Surface>(2);
        mOutputSurfaces.add(mImageReader.getSurface());
        mOutputSurfaces.add(mSurface);
    }


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

    /*Initialize the preview of the builder,
     this is done in order to adjust the iso
      when the ae is not the lowest*/
    private void initPreviewBuilder() {
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
        mPreviewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, valueAF);
        mPreviewBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, valueAETime);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, valueAE);
        mPreviewBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, valueISO);
    }

    /*Session state callback
    Continue preview*/
    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            Log.i("Thread", "onConfigured---->" + Thread.currentThread().getName());
            Log.i("CaptureStateCallback", "mSessionStateCallback--->onConfigured");
            try {
                mCameraCaptureSession = cameraCaptureSession;
                setListener();
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


    /*UI thread handler*/
    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FOCUS_DISAPPEAR:
                    if (msg.obj == null) {
                        mFocusImage.stopFocus();
                        break;
                    }
                    Integer valueTimes = (Integer) msg.obj;
                    if (mFocusImage.mTimes == valueTimes.intValue()) {
                        mFocusImage.stopFocus();
                    }
                    break;
                case WINDOW_TEXT_DISAPPEAR:
                    if (msg.obj == null) {
                        break;
                    }
                    Integer valueTimes2 = (Integer) msg.obj;
                    if (mWindowTextView.mTimes == valueTimes2.intValue()) {
                        mWindowTextView.stop();
                    }
                    break;
                case FOCUS_AGAIN:
                    Log.i("FOCUS_AGAIN", "FOCUS_AGAINFOCUS_AGAINFOCUS_AGAIN");
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                    updatePreview();
                    break;
            }
        }
    };


    /*Set the focus listener
    TextureView of touch
    awbseekbar of change*/
    private void setListener() {
        /*The touch zoom monitor is set when the display starts.*/
        mTextureView.setmMyTextureViewTouchEvent(new TextureViewTouchEvent(mCameraCharacteristics, mTextureView, mPreviewBuilder, mCameraCaptureSession, mHandler, mPreviewSessionCallback));
        //mAwbSb.setmOnAwbSeekBarChangeListener(new AwbSeekBarChangeListener(getActivity(), mSeekBarTextView, mPreviewBuilder, mCameraCaptureSession, mHandler, mPreviewSessionCallback));
    }

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


    /*Change cameraID*/
    private void reOpenCamera(int viewWidth, int viewHeight) {
        mFocusImage.stopFocus();
        Size mPreviewSize;
        CameraManager manager = (CameraManager) getActivityContext.getSystemService(Context.CAMERA_SERVICE);
        String cameraId = null;
        try {
            cameraId = Objects.requireNonNull(manager).getCameraIdList()[Integer.parseInt(mCameraId)];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        CameraCharacteristics characteristics = null;
        try {
            characteristics = Objects.requireNonNull(manager).getCameraCharacteristics(Objects.requireNonNull(cameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        StreamConfigurationMap map = Objects.requireNonNull(characteristics).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mPreviewSize = Objects.requireNonNull(map).getOutputSizes(SurfaceTexture.class)[Integer.parseInt(mCameraId)];
        if (mCameraId.equals("1")) {
            mCameraId = "0";
            mPreviewSize = new Size(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else if (mCameraId.equals("0")) {
            mCameraId = "1";
            mPreviewSize = new Size(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            //mPreviewSize = new Size(1280, 720);
        } else {
            mCameraId = "0";
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
            if (ActivityCompat.checkSelfPermission(getActivityContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler);
            newPreviewSession();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("onDestroy", "onDestroy");
        closeCamera();
        saveCurrentPreference();
    }

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
    private void saveCurrentPreference() {
        PreferenceHelper.writeCurrentCameraid(getActivityContext, mCameraId);
    }
}
