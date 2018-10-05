package mohan.com.camrecorder.videocapture.camera;

/*
 * Copyright (c) 2018. Created for CYRANOAPP, PoweredBy INNOBOT SYSYTEMS Pvt.Ltd.,Coimbatore,TamilNadu,India.
 * All Rights Reserved,Company Confidential.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project Name : cyranoapp-android.
 * Created by : Mohanraj.S, Android Application Developer.
 * Created on : 10/4/2018
 * Updated on : 4/10/18 6:40 PM.
 * File Name : CameraWrapper.java.
 * ClassName : CameraWrapper.
 * QualifiedClassName : com.videocapture.camera.CameraWrapper.
 * Module Name : app.
 * Workstation Username : innobot-linux-4.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.ibot.cyranoapp.activity.CameraCaptureActivity;
import com.ibot.cyranoapp.activity.quickrecord.CameraCaptureActivityFA;
import com.ibot.cyranoapp.interfaces.TouchInterface;
import com.ibot.cyranoapp.utils.AppLog;
import com.ibot.cyranoapp.utils.SharedPrefsUtils;
import com.ibot.cyranoapp.utils.ShowToastUtils;
import com.videocapture.camera.OpenCameraException.OpenType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


@SuppressWarnings("deprecation")
public class CameraWrapper implements TouchInterface {

    @Nullable
    private Camera mCamera = null;
    @Nullable
    private Context mContext = null;
    @Nullable
    private Parameters mParameters = null;
    private float mDist = 0;
    private File mImageFolder;
    private String mImageFileName;
    private CameraInfo cameraInfo;
    private int cameraMode = 0;
    private CameraInfo info;
    private Camera tempCamera;
    private byte[] tempData;
    @NonNull
    String Tag = "CameraWrapper";
    Activity mActivity;
    private int rotation;

    public CameraWrapper(Context mContext) {
        this.mContext = mContext;
    }

    @Nullable
    public Camera getCamera() {
        return mCamera;
    }

    public void openCamera(int cameraMode) throws OpenCameraException {
        mCamera = null;
        this.cameraMode = cameraMode;
        try {
            mCamera = openCameraFromSystem(cameraMode);
            info = new CameraInfo();
            Camera.getCameraInfo(cameraMode, info);
            creatImageFolder();
            int rotation = getDeviceCurrentOrientation(mContext);
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }

            int result;
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            Parameters params = mCamera.getParameters();
//            mCamera.setDisplayOrientation(90);
            mCamera.setDisplayOrientation(result);
            //params.setVideoStabilization(true);
            //params.setExposureCompensation();
            String model = Build.MODEL;
            if (model.equalsIgnoreCase("Nexus 5X")) {
                // rotate camera 180°
                if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    String orientationID = SharedPrefsUtils.getParam(mContext, "orientationID", "").toString();
                    if (orientationID.equalsIgnoreCase("1")) {
                        //portrait
                        mCamera.setDisplayOrientation(90);
                    } else if (orientationID.equalsIgnoreCase("0")) {
                        //landscape
                        mCamera.setDisplayOrientation(0);
                    } else if (orientationID.equalsIgnoreCase("8")) {
                        //reverse landscape
                        mCamera.setDisplayOrientation(180);
                    }

                } else {
//                    Activity activity = (Activity) mContext;
//                    if (getScreenOrientation(activity)==0) {
//                        mCamera.setDisplayOrientation(180);
//                    }
                    String orientationID = SharedPrefsUtils.getParam(mContext, "orientationID", "").toString();
                    if (orientationID.equalsIgnoreCase("1")) {
                        //portrait
                        Log.i(Tag, "portrait");
                    } else if (orientationID.equalsIgnoreCase("0")) {
                        //landscape
                        mCamera.setDisplayOrientation(180);
                    } else if (orientationID.equalsIgnoreCase("8")) {
                        //reverse landscape
                        mCamera.setDisplayOrientation(0);
                    }
                }

            }
            mCamera.setParameters(params);
        } catch (@NonNull final RuntimeException e) {
            e.printStackTrace();
            throw new OpenCameraException(OpenType.INUSE);
        }
        if (mCamera == null) throw new
                OpenCameraException(OpenType.NOCAMERA);
    }

    public void prepareCameraForRecording() throws PrepareCameraException {
        try {
            storeCameraParametersBeforeUnlocking();
            unlockCameraFromSystem();
        } catch (@NonNull final RuntimeException e) {
            e.printStackTrace();
            throw new PrepareCameraException();
        }
    }

    public void releaseCamera() {
        if (getCamera() == null) return;
        releaseCameraFromSystem();
    }

    public void startPreview(final SurfaceHolder holder) throws IOException {
        mCamera.setPreviewDisplay(holder);
        mCamera.startPreview();
    }

    public void stopPreview() throws Exception {
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
    }

    @NonNull
    public RecordingSize getSupportedRecordingSize(int width, int height) {
        Size recordingSize = getOptimalSize(getSupportedVideoSizes(), width, height);
        if (recordingSize == null) {
            AppLog.e(AppLog.CAMERA, "Failed to find supported recording size - falling back to requested: " + width + "x" + height);
            return new RecordingSize(width, height);
        }
        AppLog.e(AppLog.CAMERA, "Recording size: " + recordingSize.width + "x" + recordingSize.height);
        return new RecordingSize(recordingSize.width, recordingSize.height);
    }

    public CamcorderProfile getBaseRecordingProfile() {
        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P))
            return CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P))
            return CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        else
            return CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
    }

    public void configureForPreview(int viewWidth, int viewHeight) {

        final Parameters params = getCameraParametersFromSystem();
        final Size previewSize = getOptimalSize(params.getSupportedPreviewSizes(), viewWidth, viewHeight);
        AppLog.e(AppLog.CAMERA, previewSize + "");
        params.setPreviewSize(previewSize.width, previewSize.height);
        params.setPreviewFormat(ImageFormat.NV21);
        // params.setZoom(0);
        //if you want the preview to be zoomed from start :
//        params.setZoom(params.getMaxZoom());
        mCamera.setParameters(params);
        AppLog.e(AppLog.CAMERA, "Preview size: " + previewSize.width + "x" + previewSize.height);
    }

    public void enableAutoFocus() {
        final Parameters params = getCameraParametersFromSystem();
        params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        mCamera.setParameters(params);
    }

    private Camera openCameraFromSystem(int cameraMode) {

        AppLog.e("***** cameraMode ***** ", " cameraMode ***** " + cameraMode);
        if (cameraMode == 0) {
            return Camera.open(CameraInfo.CAMERA_FACING_BACK);
        } else {
            if (isFrontCameraAvailable()) {
                return Camera.open(CameraInfo.CAMERA_FACING_FRONT);
            } else {
                return Camera.open(CameraInfo.CAMERA_FACING_BACK);
            }

        }
    }


    private void unlockCameraFromSystem() {
        mCamera.unlock();
    }

    private void releaseCameraFromSystem() {
        mCamera.release();
    }

    protected Parameters getCameraParametersFromSystem() {
        return mCamera.getParameters();
    }

    protected List<Size> getSupportedVideoSizes() {
        Parameters params = getCameraParametersAfterUnlocking();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return params.getSupportedVideoSizes();
        } else {
            AppLog.e(AppLog.CAMERA, "Using supportedPreviewSizes iso supportedVideoSizes due to API restriction");
            return params.getSupportedPreviewSizes();
        }
    }

    private void storeCameraParametersBeforeUnlocking() {
        mParameters = getCameraParametersFromSystem();
    }

    @Nullable
    private Parameters getCameraParametersAfterUnlocking() {
        return mParameters;
    }

    /**
     * Copyright (C) 2013 The Android Open Source Project
     * <p/>
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     * <p/>
     * http://www.apache.org/licenses/LICENSE-2.0
     * <p/>
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    @Nullable
    private Size getOptimalSize(@Nullable List<Size> sizes, int w, int h) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.05;
        final double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;

        // Start with max value and refine as we iterate over available preview sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        final int targetHeight = h;

        // Try to find a preview size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (final Size size : sizes) {
            final double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find preview size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (final Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * @return true : Front facing camera is available. false : Front facing
     * camera is not available.
     */
    private boolean isFrontCameraAvailable() {

        int cameraCount = 0;
        boolean isFrontCameraAvailable = false;
        cameraCount = Camera.getNumberOfCameras();

        while (cameraCount > 0) {
            cameraInfo = new CameraInfo();
            cameraCount--;
            Camera.getCameraInfo(cameraCount, cameraInfo);

            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                isFrontCameraAvailable = true;
                break;
            }

        }

        return isFrontCameraAvailable;
    }

    private static int getDeviceCurrentOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        AppLog.d("Utils", "Current orientation = " + rotation);
        return rotation;
    }

    public static int getScreenOrientation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = activity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            } else {
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            }
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            } else {
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            }
        }
        return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }

    @Override
    public void Zoom(@NonNull MotionEvent event) {
        Parameters params = mCamera.getParameters();
        int action = event.getAction();
        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                Log.v("Camera", "action == MotionEvent.ACTION_POINTER_DOWN");
                mDist = getFingerSpacing(event);
            } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                mCamera.cancelAutoFocus();
                handleZoom(event, params);
                Log.v("Camera", "action == MotionEvent.ACTION_MOVE && params.isZoomSupported()");
            }
        } else {
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                mCamera.cancelAutoFocus();
                handleFocus(event, params);
                Log.v("Camera", "action == MotionEvent.ACTION_UP");
            }
        }


    }

    private void handleZoom(@NonNull MotionEvent event, Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        mCamera.setParameters(params);

       /* if (zoomCallback != null) {
            zoomCallback.onZoomChanged((zoom * 100 / maxZoom));
        }*/

        Log.e("Zoom", "MAX :" + maxZoom + "Zoom %" + (zoom * 100 / maxZoom));
    }


    private void handleFocus(MotionEvent event, Parameters params) {

        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
            try {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {
                        // currently set to auto-focus on single touch
                    }
                });
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Determine the space between the first two fingers
     */
    private float getFingerSpacing(MotionEvent event) {
        // ...
        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @SuppressLint("StaticFieldLeak")
    public class savePictureAsync extends AsyncTask<Void, Void, Void> {
        @Nullable
        @Override
        protected Void doInBackground(Void... voids) {
            savePicture(tempData, tempCamera);
            return null;
        }
    }

    private void savePicture(@NonNull byte[] data, Camera camera) {
        if (tempCamera != null && tempData != null) {
            FileOutputStream fos;
            Bitmap picture = BitmapFactory.decodeByteArray(data, 0, data.length);
            //  Creating Image File Name
            try {
                createImageFileName();
            } catch (IOException e) {
                ShowToastUtils.showCustomToast(mContext, "IOException");
            }


            if (mContext.getApplicationContext().getApplicationInfo().className.equalsIgnoreCase("CameraCaptureActivity.this")) {
                CameraCaptureActivity activity = (CameraCaptureActivity) mContext;
            } else {
                CameraCaptureActivityFA activity_fa = (CameraCaptureActivityFA) mContext;
            }


            String model = Build.MODEL;
            if (model.equalsIgnoreCase("Nexus 5X")) {
                if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    String orientationID = SharedPrefsUtils.getParam(mContext, "orientationID", "").toString();
                    if (orientationID.equalsIgnoreCase("1")) {
                        //portrait
                        float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
                        Matrix matrixMirrorY = new Matrix();
                        matrixMirrorY.setValues(mirrorY);
                        matrix.postConcat(matrixMirrorY);
                        matrix.preRotate(180);
                        picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);
                    } else if (orientationID.equalsIgnoreCase("0")) {
                        //landscape
                        float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
                        Matrix matrixMirrorY = new Matrix();
                        matrixMirrorY.setValues(mirrorY);
                        matrix.postConcat(matrixMirrorY);
                        matrix.preRotate(270);
                        picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);
                    } else if (orientationID.equalsIgnoreCase("8")) {
                        //reverse landscape
                        float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
                        Matrix matrixMirrorY = new Matrix();
                        matrixMirrorY.setValues(mirrorY);
                        matrix.postConcat(matrixMirrorY);
                        matrix.preRotate(90);
                        picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);
                    }

                } else {

                    String orientationID = SharedPrefsUtils.getParam(mContext, "orientationID", "").toString();
                    if (orientationID.equalsIgnoreCase("1")) {
                        //portrait
                        Matrix matrix = new Matrix();
                        matrix.postRotate(270);
                        picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);
                    } else if (orientationID.equalsIgnoreCase("0")) {
                        //landscape
                        Matrix matrix = new Matrix();
                        matrix.postRotate(180);
                        picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);
                    } else if (orientationID.equalsIgnoreCase("8")) {
                        //reverse landscape
                        Matrix matrix = new Matrix();
                        matrix.postRotate(0);
                        picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);
                        // mCamera.setDisplayOrientation(0);
                    }
                }

            } else {

                if (mContext.getApplicationContext().getApplicationInfo().className.equalsIgnoreCase("CameraCaptureActivity.this")) {
                    CameraCaptureActivity activity = (CameraCaptureActivity) mContext;
                    rotation = ((CameraCaptureActivity) mContext).getWindowManager().getDefaultDisplay().getRotation();
                } else {
                    CameraCaptureActivityFA activity_fa = (CameraCaptureActivityFA) mContext;
                    rotation = ((CameraCaptureActivityFA) mContext).getWindowManager().getDefaultDisplay().getRotation();
                }


                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);

                    if (Build.VERSION.SDK_INT > 13 && cameraMode == 1) {
                        float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
                        matrix = new Matrix();
                        Matrix matrixMirrorY = new Matrix();
                        matrixMirrorY.setValues(mirrorY);
                        matrix.postConcat(matrixMirrorY);
                        matrix.preRotate(270);
                    }
// create a rotated version and replace the original bitmap
                    picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);

                }

            }
            try {

                // Writing byte data in outputstream to save image
                fos = new FileOutputStream(mImageFileName);
                picture.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                //  fos.write(data);
                fos.flush();
                fos.close();

                if (mContext.getApplicationContext().getApplicationInfo().className.equalsIgnoreCase("CameraCaptureActivity.this")) {
                    CameraCaptureActivity activity = (CameraCaptureActivity) mContext;
                    activity.onPictureCaptured("", "Success");
                } else {
                    CameraCaptureActivityFA activity_fa = (CameraCaptureActivityFA) mContext;
                    activity_fa.onPictureCaptured("", "Success");
                }
            } catch (FileNotFoundException e) {

                if (mContext.getApplicationContext().getApplicationInfo().className.equalsIgnoreCase("CameraCaptureActivity.this")) {
                    CameraCaptureActivity activity = (CameraCaptureActivity) mContext;
                    activity.onPictureCaptured("", "Failed");
                    ShowToastUtils.showCustomToast(mContext, "FileNotFoundException");
                } else {
                    CameraCaptureActivityFA activity_fa = (CameraCaptureActivityFA) mContext;
                    activity_fa.onPictureCaptured("", "Failed");
                    ShowToastUtils.showCustomToast(mContext, "FileNotFoundException");
                }

            } catch (NullPointerException ex) {

                if (mContext.getApplicationContext().getApplicationInfo().className.equalsIgnoreCase("CameraCaptureActivity.this")) {
                    CameraCaptureActivity activity = (CameraCaptureActivity) mContext;
                    activity.onPictureCaptured("", "Failed");
                    ShowToastUtils.showCustomToast(mContext, "NullPointerException");
                } else {
                    CameraCaptureActivityFA activity_fa = (CameraCaptureActivityFA) mContext;
                    activity_fa.onPictureCaptured("", "Failed");
                    ShowToastUtils.showCustomToast(mContext, "NullPointerException");
                }
            } catch (IOException e) {

                if (mContext.getApplicationContext().getApplicationInfo().className.equalsIgnoreCase("CameraCaptureActivity.this")) {
                    CameraCaptureActivity activity = (CameraCaptureActivity) mContext;
                    activity.onPictureCaptured("", "Failed");
                    ShowToastUtils.showCustomToast(mContext, "IOException");
                } else {
                    CameraCaptureActivityFA activity_fa = (CameraCaptureActivityFA) mContext;
                    activity_fa.onPictureCaptured("", "Failed");
                    ShowToastUtils.showCustomToast(mContext, "IOException");
                }
            }

            //   MediaStore.Images.Media.insertImage(mContext.getContentResolver(), mImageFolder.getAbsolutePath(), mImageFolder.getName(), mImageFolder.getName());
            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(mContext, new String[]{mImageFileName.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });

        } else {
            if (mContext.getApplicationContext().getApplicationInfo().className.equalsIgnoreCase("CameraCaptureActivity.this")) {
                CameraCaptureActivity activity = (CameraCaptureActivity) mContext;
                activity.onPictureCaptured("", "Failed");
            } else {
                CameraCaptureActivityFA activity_fa = (CameraCaptureActivityFA) mContext;
                activity_fa.onPictureCaptured("", "Failed");
            }
        }

    }


    //code for taking photo
    @NonNull
    private Camera.PictureCallback mpicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            tempCamera = camera;
            tempData = data;
            new savePictureAsync().execute();
        }
    };

    private void creatImageFolder() {

        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mImageFolder = new File(imageFile, "Cyrano");
        if (!mImageFolder.exists()) {
            mImageFolder.mkdirs();
        }
    }

    @NonNull
    private File createImageFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "Cyrano" + timestamp + "_";
        File imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
        mImageFileName = imageFile.getAbsolutePath();
        return imageFile;
    }


    public void takeShot() throws OpenCameraException {
        Camera camera = getCamera();
        camera.takePicture(null, null, mpicture);

    }


}
