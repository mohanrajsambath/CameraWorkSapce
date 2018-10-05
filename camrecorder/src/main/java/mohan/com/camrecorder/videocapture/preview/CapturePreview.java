package mohan.com.camrecorder.videocapture.preview;

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
 * Updated on : 4/10/18 6:06 PM.
 * File Name : CapturePreview.java.
 * ClassName : CapturePreview.
 * QualifiedClassName : com.videocapture.preview.CapturePreview.
 * Module Name : app.
 * Workstation Username : innobot-linux-4.
 */

import android.support.annotation.NonNull;
import android.view.SurfaceHolder;

import com.ibot.cyranoapp.utils.AppLog;
import com.videocapture.camera.CameraWrapper;

import java.io.IOException;


public class CapturePreview  implements SurfaceHolder.Callback {

    private  char screenOrientation;
    private boolean mPreviewRunning = false;
    private final CapturePreviewInterface mInterface;
    public final CameraWrapper mCameraWrapper;
    private int cyrWidth=0;
    private int cyrHeight=0;


    public CapturePreview(CapturePreviewInterface capturePreviewInterface, CameraWrapper cameraWrapper,
                          @NonNull SurfaceHolder holder) {
        mInterface = capturePreviewInterface;
        mCameraWrapper = cameraWrapper;

        initalizeSurfaceHolder(holder);
    }

    public CapturePreview(CapturePreviewInterface capturePreviewInterface, CameraWrapper cameraWrapper,
                          @NonNull SurfaceHolder holder,char screenOrientation) {
        mInterface = capturePreviewInterface;
        mCameraWrapper = cameraWrapper;
        this.screenOrientation= screenOrientation;
        initalizeSurfaceHolder(holder);
    }


    @SuppressWarnings("deprecation")
    private void initalizeSurfaceHolder(final SurfaceHolder surfaceHolder) {
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // Necessary for older API's
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        // NOP
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
        if (mPreviewRunning) {
            try {
                mCameraWrapper.stopPreview();
            } catch (@NonNull final Exception e) {
                e.printStackTrace();
            }
        }

        try {
//            mCameraWrapper.configureForPreview(1280, 720);
//            if (SharedPrefsUtils.getParam(getActivityContext, "isScriptedUpload", "").toString().equalsIgnoreCase("true")) {
//                mCameraWrapper.configureForPreview(1280, 720);
//            } else {
            //mCameraWrapper.configureForPreview(1280, 720);
//            }

            //CYR-12338 Quick Record. The camera position does not stay still while recording.
            //mCameraWrapper.configureForPreview(1280, 720);
            if(screenOrientation=='P'){
                if(width>=1920){
                    cyrWidth=1920;
                }else{
                    cyrWidth=1920;
                }//2560x1440
                if(height>=1080){
                    cyrHeight=1080;
                }else{
                    cyrHeight=1080;
                }
            }else if(screenOrientation=='L'){
                if(width>=1080){
                    cyrWidth=1080;
                }else{
                    cyrWidth=1080;
                }//2560x1440
                if(height>=1920){
                    cyrHeight=1920;
                }else{
                    cyrHeight=1920;
                }
            }

            mCameraWrapper.configureForPreview(cyrWidth, cyrHeight);
            //mCameraWrapper.configureForPreview(width, height);

            AppLog.d(AppLog.PREVIEW, "Configured camera for preview in surface of " + width + " by " + height);
        } catch (@NonNull final RuntimeException e) {
            e.printStackTrace();
            AppLog.d(AppLog.PREVIEW, "Failed to show preview - invalid parameters set to camera preview");
            mInterface.onCapturePreviewFailed();
            return;
        }

        try {
            mCameraWrapper.enableAutoFocus();
        } catch (@NonNull final RuntimeException e) {
            e.printStackTrace();
            AppLog.d(AppLog.PREVIEW, "AutoFocus not available for preview");
        }

        try {
            mCameraWrapper.startPreview(holder);
            setPreviewRunning(true);
        } catch (@NonNull final IOException e) {
            e.printStackTrace();
            AppLog.d(AppLog.PREVIEW, "Failed to show preview - unable to connect camera to preview (IOException)");
            mInterface.onCapturePreviewFailed();
        } catch (@NonNull final RuntimeException e) {
            e.printStackTrace();
            AppLog.d(AppLog.PREVIEW, "Failed to show preview - unable to start camera preview (RuntimeException)");
            mInterface.onCapturePreviewFailed();
        }
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        //NOP
    }

    public void releasePreviewResources() {
        if (mPreviewRunning) {
            try {
                mCameraWrapper.stopPreview();
                setPreviewRunning(false);
            } catch (@NonNull final Exception e) {
                e.printStackTrace();
                AppLog.e(AppLog.PREVIEW, "Failed to clean up preview resources");
            }
        }
    }

    protected void setPreviewRunning(boolean running) {
        mPreviewRunning = running;
    }




}