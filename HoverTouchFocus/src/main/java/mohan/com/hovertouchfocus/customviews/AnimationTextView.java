package mohan.com.hovertouchfocus.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.TextView;

import mohan.com.hovertouchfocus.utils.SleepThread;


@SuppressLint("AppCompatCustomView")
public class AnimationTextView extends TextView {
    private Handler mMainHandler;
    private Animation mAnimation;
    public int mTimes = 0;

    public AnimationTextView(Context context) {
        super(context);
    }

    public AnimationTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimationTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AnimationTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setmMainHandler(Handler mMainHandler) {
        this.mMainHandler = mMainHandler;
    }

    public void setmAnimation(Animation mAnimation) {
        this.mAnimation = mAnimation;
    }

    public void start(String text, int message) {
        if (mAnimation == null || mMainHandler == null) {
            return;
        }
        this.setVisibility(VISIBLE);
        mTimes++;
        this.setText(text);
        this.startAnimation(mAnimation);
        new Thread(new SleepThread(mMainHandler, message, 1000, Integer.valueOf(mTimes))).start();
    }

    public void stop() {
        this.setVisibility(GONE);
    }
}
