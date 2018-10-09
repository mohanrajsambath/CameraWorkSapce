package mohan.com.only2cameramodepreview.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TextureView;

/*
 * Copyright (c) 2018. Created by Mohanraj.S,Innobot Systems on 1/10/18 for CameraWorkSapce
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
 */
public class CustomTextureView extends TextureView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    //private MyTextureViewTouchEvent mMyTextureViewTouchEvent;
   // private FocusPositionTouchEvent mFocusPositionTouchEvent;



    /*Constructors*/
    public CustomTextureView(Context context) {
        super(context);
    }

    public CustomTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    public void fitWindow(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @SuppressLint("ClickableViewAccessibility")
    /*@Override
    public boolean onTouchEvent(MotionEvent event) {
       // mFocusPositionTouchEvent.getPosition(event);
        return mMyTextureViewTouchEvent.onAreaTouchEvent(event);
    }*/


    /*public void setmMyTextureViewTouchEvent(MyTextureViewTouchEvent myTextureViewTouchEvent) {
        this.mMyTextureViewTouchEvent = myTextureViewTouchEvent;
    }*/

    /*public void setmFocusPositionTouchEvent(FocusPositionTouchEvent mFocusPositionTouchEvent) {
        this.mFocusPositionTouchEvent = mFocusPositionTouchEvent;
    }*/

    public interface MyTextureViewTouchEvent {
        public boolean onAreaTouchEvent(MotionEvent event);
    }

    public interface FocusPositionTouchEvent {
        public void getPosition(MotionEvent event);
    }
}
