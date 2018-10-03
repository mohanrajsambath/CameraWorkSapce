package mohan.com.hovertouchfocus.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;

import mohan.com.hovertouchfocus.R;
import mohan.com.hovertouchfocus.activity.MainActivity;
import mohan.com.hovertouchfocus.utils.SleepThread;



public class AnimationImageView extends android.support.v7.widget.AppCompatImageView {
    private Handler mMainHandler;
    private Animation mAnimation;
    private Context mContext;
    /*Prevented another text,
    but the last time that it has not disappeared,
     the new text will disappear after an hour.*/
    public int mTimes = 0;

    public AnimationImageView(Context context) {
        super(context);
        mContext = context;
    }

    public AnimationImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public AnimationImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    /*public AnimationImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }*/

    public void setmMainHandler(Handler mMainHandler) {
        this.mMainHandler = mMainHandler;
    }

    public void setmAnimation(Animation mAnimation) {
        this.mAnimation = mAnimation;
    }

    public void initFocus() {
        this.setVisibility(VISIBLE);
        new Thread(new SleepThread(mMainHandler, MainActivity.FOCUS_DISAPPEAR, 1000, null)).start();
    }

    public void startFocusing() {
        mTimes++;
        this.setVisibility(View.VISIBLE);
        try {
            this.startAnimation(mAnimation);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setBackground(mContext.getDrawable(R.drawable.focus));
        new Thread(new SleepThread(mMainHandler, MainActivity.FOCUS_DISAPPEAR, 1000, mTimes)).start();
    }

    public void focusFailed() {
        mTimes++;
        this.setBackground(mContext.getDrawable(R.drawable.focus_failed));
        new Thread(new SleepThread(mMainHandler, MainActivity.FOCUS_DISAPPEAR, 800, Integer.valueOf(mTimes))).start();
    }

    public void focusSuccess() {
        mTimes++;
        this.setVisibility(View.VISIBLE);
        this.setBackground(mContext.getDrawable(R.drawable.focus_succeed));
        new Thread(new SleepThread(mMainHandler, MainActivity.FOCUS_DISAPPEAR, 800, Integer.valueOf(mTimes))).start();
    }

    public void stopFocus() {
        this.setVisibility(INVISIBLE);
    }
}
