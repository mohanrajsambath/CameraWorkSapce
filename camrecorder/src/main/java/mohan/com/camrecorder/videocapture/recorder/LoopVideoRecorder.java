package mohan.com.camrecorder.videocapture.recorder;

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
 * Updated on : 4/10/18 5:58 PM.
 * File Name : LoopVideoRecorder.java.
 * ClassName : LoopVideoRecorder.
 * QualifiedClassName : com.videocapture.recorder.LoopVideoRecorder.
 * Module Name : app.
 * Workstation Username : innobot-linux-4.
 */

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.ibot.cyranoapp.utils.AppLog;
import com.ibot.cyranoapp.utils.CommonUtils;
import com.videocapture.camera.CameraWrapper;
import com.videocapture.camera.OpenCameraException;
import com.videocapture.camera.PrepareCameraException;
import com.videocapture.camera.RecordingSize;
import com.videocapture.configuration.CaptureConfiguration;
import com.videocapture.preview.CapturePreview;
import com.videocapture.preview.CapturePreviewInterface;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class LoopVideoRecorder implements MediaRecorder.OnInfoListener, CapturePreviewInterface {

    private  char screenOrientation;
    @Nullable
    private CameraWrapper mCameraWrapper;
    private Surface mPreviewSurface;
    @Nullable
    private CapturePreview mVideoCapturePreview = null;
    private String videoFilePath, videoFileName;
    private final CaptureConfiguration mCaptureConfiguration;
    Activity getActivityContext;
    @Nullable
    private MediaRecorder mRecorder;
    private boolean mRecording = false;
    private final VideoRecorderInterface mRecorderInterface;
    private int mCameraFacing = CameraCharacteristics.LENS_FACING_BACK;
    private boolean upsideDown = false;
    private boolean isQuick = false, isLong = false;

    @Nullable
    private LoopVideoRecorderListener mListener = null;
    private int cameraMode;


    public interface LoopVideoRecorderListener {
        void onLoopRecordingStopped(String videoFileList, String videoFileName);
    }

    public LoopVideoRecorder(Activity getActivity, VideoRecorderInterface recorderInterface, CaptureConfiguration captureConfiguration, @Nullable CameraWrapper cameraWrapper, SurfaceHolder previewHolder, int mCameraMode) {
        mCaptureConfiguration = captureConfiguration;
        mRecorderInterface = recorderInterface;
        this.getActivityContext = getActivity;
        mCameraWrapper = cameraWrapper;
        mPreviewSurface = previewHolder.getSurface();
        this.cameraMode = mCameraMode;
        initializeCamera();
        initializePreview(previewHolder);
    }

    public LoopVideoRecorder(Activity getActivity, VideoRecorderInterface recorderInterface, CaptureConfiguration captureConfiguration, @Nullable CameraWrapper cameraWrapper, SurfaceHolder previewHolder, int mCameraMode,char screenOrientation) {
        mCaptureConfiguration = captureConfiguration;
        mRecorderInterface = recorderInterface;
        this.getActivityContext = getActivity;
        mCameraWrapper = cameraWrapper;
        mPreviewSurface = previewHolder.getSurface();
        this.cameraMode = mCameraMode;
        this.screenOrientation= screenOrientation;
        initializeCamera();
        initializePreview(previewHolder);
    }

    public void setLoopVideoRecorderListener(LoopVideoRecorderListener listener) {
        this.mListener = listener;
    }

    public int getCameraFacing() {
        return mCameraFacing;
    }

    public void setCameraFacing(int mCameraFacing) {
        this.mCameraFacing = mCameraFacing;
    }

    //set up front or back camera based on user choice
    protected void initializeCamera() {
        try {
            mCameraWrapper.openCamera(cameraMode);
        } catch (@NonNull final OpenCameraException e) {
            e.printStackTrace();
            mRecorderInterface.onRecordingFailed(e.getMessage());
            return;
        }

    }


    //setup camera preview
    public void initializePreview(@NonNull SurfaceHolder previewHolder) {
        mVideoCapturePreview = new CapturePreview(this, mCameraWrapper, previewHolder,screenOrientation);

    }

    public void toggleRecording(boolean isLong) {
        //this.isQuick = isQuick;
        this.isLong = isLong;
        if (isRecording()) {
            //if stop recording is initiated from camera page by stop button then argument of null will be supplied
            stopRecording(null);
        } else {
            startRecording();
        }
    }

    public void startPreview(@NonNull SurfaceHolder previewHolder) {
        mPreviewSurface = previewHolder.getSurface();
        initializePreview(previewHolder);
        if (mRecorder != null) {
            mRecorder.setPreviewDisplay(mPreviewSurface);
        }
    }

    public void stopPreview() {
        if (!isRecording()) return;
        if (mRecorder != null) {
            mRecorder.setPreviewDisplay(null);
        }
        releasePreviewResources();
    }

    //start media recording
    protected void startRecording() {
        mRecording = false;
        setVideoFilePath();
        if (!initRecorder()) return;
        if (!prepareRecorder()) return;
        if (!startRecorder()) return;
        mRecording = true;
        mRecorderInterface.onRecordingStarted();
        //AppLog.d(AppLog.RECORDER, "Successfully started recording - outputfile: " + mVideoFile.getFullPath());
    }

    //generate file name and file path for the media to be recorded
    private void setVideoFilePath() {

        String fileFormat = ".mp4";
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        videoFileName = UUID.randomUUID().toString() + fileFormat;
        videoFilePath = CommonUtils.getVideoContentPath(getActivityContext).getAbsolutePath() + "/" + videoFileName;
    }

    //stop recording under triggered event
    public void stopRecording(@Nullable String message) {
        if (!isRecording()) return;
        if (mRecorder != null) {
            try {
                mRecorder.setOnErrorListener(null);
                mRecorder.setPreviewDisplay(null);
                mRecorder.stop();
//                mRecorder.release();
                if (message == null) {
                    mRecorderInterface.onRecordingSuccess();
                }
                AppLog.d(AppLog.RECORDER, "Successfully stopped recording - outputfile: " + videoFilePath);
            } catch (@NonNull final RuntimeException e) {
                AppLog.d(AppLog.RECORDER, "Failed to stop recording");
            }

            mRecording = false;
//            mRecorderInterface.onRecordingStopped(videoFilePath, videoFileName);

            if ((message == null) && (mListener != null)) {
                mListener.onLoopRecordingStopped(videoFilePath, videoFileName);
            } else {
                mRecorderInterface.onRecordingStopped(videoFilePath, videoFileName);

            }
            setVideoFilePath();
        }
    }

    private boolean initRecorder() {
        try {
            Objects.requireNonNull(mCameraWrapper).prepareCameraForRecording();
        } catch (@NonNull final PrepareCameraException e) {
            e.printStackTrace();
            mRecorderInterface.onRecordingFailed("Unable to record video");
            AppLog.e(AppLog.RECORDER, "Failed to initialize recorder - " + e.toString());
            return false;
        }

        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setOnErrorListener(null);
        } else {
            mRecorder.reset();
        }

        configureMediaRecorder(mRecorder, mCameraWrapper.getCamera());

        AppLog.d(AppLog.RECORDER, "MediaRecorder successfully initialized");
        return true;
    }

    //this sets up properties of camera for recording
    @SuppressWarnings("deprecation")
    protected void configureMediaRecorder(@NonNull final MediaRecorder recorder, Camera camera) throws IllegalStateException, IllegalArgumentException {
        recorder.setCamera(camera);
        recorder.setAudioSource(mCaptureConfiguration.getAudioSource());
        recorder.setVideoSource(mCaptureConfiguration.getVideoSource());

        CamcorderProfile baseProfile = mCameraWrapper.getBaseRecordingProfile();
        baseProfile.fileFormat = mCaptureConfiguration.getOutputFormat();
        baseProfile.duration = mCaptureConfiguration.getMaxCaptureDuration();

        RecordingSize size = mCameraWrapper.getSupportedRecordingSize(mCaptureConfiguration.getVideoWidth(), mCaptureConfiguration.getVideoHeight());

        baseProfile.videoFrameWidth = size.width;
        baseProfile.videoFrameHeight = size.height;
        baseProfile.videoBitRate = mCaptureConfiguration.getVideoBitrate();

        baseProfile.audioCodec = mCaptureConfiguration.getAudioEncoder();
        baseProfile.videoCodec = mCaptureConfiguration.getVideoEncoder();
        recorder.setProfile(baseProfile);
        recorder.setOutputFile(videoFilePath);
//        if(mCaptureConfiguration.getMaxCaptureFileSize() != CaptureConfiguration.NO_FILESIZE_LIMIT) {
        try {
//                recorder.setMaxFileSize(mCaptureConfiguration.getMaxCaptureFileSize());
//            recorder.setMaxFileSize(8388608);

            //  Quick Record flow change in sprint 1.9.1
           /* if (!isQuick) {
                recorder.setMaxFileSize(26214400); //25MB
            }*/

//  Quick Record flow change in sprint 1.9.1
            if (isLong) {
                recorder.setMaxFileSize(26214400); //25MB
            }


        } catch (IllegalArgumentException e) {
            AppLog.e(AppLog.RECORDER, "Failed to set max filesize - illegal argument: " + mCaptureConfiguration.getMaxCaptureFileSize());
        } catch (RuntimeException e2) {
            AppLog.e(AppLog.RECORDER, "Failed to set max filesize - runtime exception");
        }
//        }

        // added by xxm, for loop recording mode
//        if(mCaptureConfiguration.getMaxCaptureDuration() != CaptureConfiguration.NO_DURATION_LIMIT) {
//            try {
//                recorder.setMaxDuration(mCaptureConfiguration.getMaxCaptureDuration());
//            } catch (IllegalArgumentException e) {
//                AppLog.e(AppLog.RECORDER, "Failed to set max duration - illegal argument: " + mCaptureConfiguration.getMaxCaptureDuration());
//            } catch (RuntimeException e2) {
//                AppLog.e(AppLog.RECORDER, "Failed to set max duration - runtime exception");
//            }
//        }
        try {
            CameraManager cameraManager = (CameraManager) getActivityContext.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0]; // Default to back camera
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                int cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraFacing == mCameraFacing) {
                    cameraId = id;
                    break;
                }
            }


            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (sensorOrientation == 270) {
                // Camera is mounted the wrong way...
                upsideDown = true;
            }
            int rotation = getActivityContext.getWindowManager().getDefaultDisplay().getRotation();
            int orientation = getOrientation(rotation, upsideDown, cameraMode);
            recorder.setOrientationHint(orientation);
            recorder.setOnInfoListener(this);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


//        if (camerama.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            rotation = (info.orientation - mOrientation + 360) % 360;
//        } else {  // back-facing camera
//            rotation = (info.orientation + mOrientation) % 360;
//        }


    }

    private boolean prepareRecorder() {
        try {
            Objects.requireNonNull(mRecorder).prepare();
            AppLog.d(AppLog.RECORDER, "MediaRecorder successfully prepared");
            return true;
        } catch (@NonNull final IllegalStateException e) {
            e.printStackTrace();
            AppLog.e(AppLog.RECORDER, "MediaRecorder preparation failed - " + e.toString());
            return false;
        } catch (@NonNull final IOException e) {
            e.printStackTrace();
            AppLog.e(AppLog.RECORDER, "MediaRecorder preparation failed - " + e.toString());
            return false;
        }
    }

    private boolean startRecorder() {
        try {
            Objects.requireNonNull(mRecorder).start();
            AppLog.d(AppLog.RECORDER, "MediaRecorder successfully started");
            return true;
        } catch (@NonNull final IllegalStateException e) {
            e.printStackTrace();
            AppLog.e(AppLog.RECORDER, "MediaRecorder start failed - " + e.toString());
            return false;
        } catch (@NonNull final RuntimeException e2) {
            e2.printStackTrace();
            AppLog.e(AppLog.RECORDER, "MediaRecorder start failed - " + e2.toString());
            mRecorderInterface.onRecordingFailed("Unable to record video with given settings");
            return false;
        }
    }

    protected boolean isRecording() {
        return mRecording;
    }

    @Nullable
    public MediaRecorder getMediaRecorder() {
        return mRecorder;
    }

    public static String getDeviceModel() {
        String model = Build.MODEL;
        return model;
    }

    public static int getOrientation(int rotation, boolean upsideDown, int cameraMode) {
        if (upsideDown) {
            if (cameraMode == 0) {
                switch (rotation) {
                    case Surface.ROTATION_0:
                        return 270;
                    case Surface.ROTATION_90:
                        return 180;
                    case Surface.ROTATION_180:
                        return 90;
                    case Surface.ROTATION_270:
                        return 0;
                }
            } else {
                switch (rotation) {
                    case Surface.ROTATION_0:
                        return 270;
                    case Surface.ROTATION_90:
                        return 0;
                    case Surface.ROTATION_180:
                        return 90;
                    case Surface.ROTATION_270:
                        return 180;
                }
            }

        } else {

            // This for Back camera
            if (cameraMode == 0) {
                switch (rotation) {
                    case Surface.ROTATION_0:
                        return 90;
                    case Surface.ROTATION_90:
                        return 0;
                    case Surface.ROTATION_180:
                        return 270;
                    case Surface.ROTATION_270:
                        return 180;
                }
            } else {
                // This for front camera
                switch (rotation) {
                    case Surface.ROTATION_0:
//                            return 270;
                        if (getDeviceModel().equalsIgnoreCase("Nexus 6P")) {
                            return 90;
                        } else {
                            return 270;
                        }
                    case Surface.ROTATION_90:
                        return 0;
                    case Surface.ROTATION_180:
                        return 90;
                    case Surface.ROTATION_270:
                        return 180;

                }
            }
        }


        return 0;
    }

//    public void lockVideoFile() {
////        mVideoFile.setLocked(true);
////        mVideoFileList.set(mFileIndex, mVideoFile);
//    }

//    private void setNextVideoFile() {
//        String fileFormat = ".mp4";
//        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
//        mVideoFilePath = new File(extStorageDirectory, "/Contents/Videos/");
//        if (!mVideoFilePath.exists()) {
//            mVideoFilePath.mkdirs();
//        }
//        String videoFileName = UUID.randomUUID().toString() + fileFormat;
//        videoFilePath = mVideoFilePath.getAbsolutePath() + "/" + videoFileName;
////        int sz = mVideoFileList.size();
////        for (int n = 1; n <= sz; n++) {
////            int fileIndex = (mFileIndex + n) % sz;
////            if (!mVideoFileList.get(fileIndex).getLocked()) {
////                mFileIndex = fileIndex;
////                mVideoFile = mVideoFileList.get(fileIndex);
////                break;
////            }
////        }
//    }

    private void releaseRecorderResources() {
//        MediaRecorder recorder = mRecorder;
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void releasePreviewResources() {
        if (mVideoCapturePreview != null) {
            mVideoCapturePreview.releasePreviewResources();
        }
    }

    public void releaseAllResources() {
        if (mVideoCapturePreview != null) {
            mVideoCapturePreview.releasePreviewResources();
        }
        if (mCameraWrapper != null) {
            mCameraWrapper.releaseCamera();
            mCameraWrapper = null;
        }
        releaseRecorderResources();
        AppLog.d(AppLog.RECORDER, "Released all resources");
    }

    @Override
    public void onCapturePreviewFailed() {
        mRecorderInterface.onRecordingFailed("Unable to show camera preview");
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                // NOP
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                AppLog.d(AppLog.RECORDER, "MediaRecorder max duration reached");
                stopRecording("Capture looping - Max duration reached");
                // loop recording
                setVideoFilePath();
                startRecording();
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                AppLog.d(AppLog.RECORDER, "MediaRecorder max filesize reached");
                stopRecording("Capture looping - Max file size reached");
                // loop recording
                setVideoFilePath();
                startRecording();
                break;
            default:
                break;
        }
    }

    /**
     * This method will make camera null- if it already opened
     *
     * @return
     */
    public boolean isCameraUsebyApp() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (RuntimeException e) {
            return true;
        } finally {
            if (camera != null) camera.release();
        }
        return false;
    }

}

