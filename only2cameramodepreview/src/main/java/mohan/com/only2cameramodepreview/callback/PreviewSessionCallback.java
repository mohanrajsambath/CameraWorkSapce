package mohan.com.only2cameramodepreview.callback;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;

import mohan.com.only2cameramodepreview.views.CustomTextureView;


public class PreviewSessionCallback extends CameraCaptureSession.CaptureCallback implements CustomTextureView.FocusPositionTouchEvent  {
    private int mAfState = CameraMetadata.CONTROL_AF_STATE_INACTIVE;
    private Handler mMainHandler;
    private int mRawX;
    private int mRawY;

    public PreviewSessionCallback(Handler mMainHandler) {
        this.mMainHandler = mMainHandler;
    }

    public PreviewSessionCallback( Handler mMainHandler, CustomTextureView mMyTextureView) {
        this.mMainHandler = mMainHandler;
        mMyTextureView.setmFocusPositionTouchEvent(this);
    }


    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, final TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);
        Log.i("Thread", "onCaptureCompleted---->" + Thread.currentThread().getName());
        Log.i("PreviewSessionCallback", "onCaptureCompleted");
        Integer nowAfState = result.get(CaptureResult.CONTROL_AF_STATE);
        //Acquisition failed
        if (nowAfState == null) {
            return;
        }
        //This time the value is the same as before, ignore it.
        if (nowAfState.intValue() == mAfState) {
            return;
        }
        mAfState = nowAfState.intValue();
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                judgeFocus();
            }
        });
    }

    private void judgeFocus() {
        switch (mAfState) {
            case CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN:
            case CameraMetadata.CONTROL_AF_STATE_PASSIVE_SCAN:
                //focusFocusing();
                break;
            case CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED:
            case CameraMetadata.CONTROL_AF_STATE_PASSIVE_FOCUSED:
                //focusSucceed();
                break;
            case CameraMetadata.CONTROL_AF_STATE_INACTIVE:
                //focusInactive();
                break;
            case CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
            case CameraMetadata.CONTROL_AF_STATE_PASSIVE_UNFOCUSED:
                //focusFailed();
                break;
        }
    }


    @Override
    public void getPosition(MotionEvent event) {
        mRawX = (int) event.getRawX();
        mRawY = (int) event.getRawY();
    }
}
