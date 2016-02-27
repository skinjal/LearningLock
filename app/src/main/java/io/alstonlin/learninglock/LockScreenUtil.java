package io.alstonlin.learninglock;


import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;


public class LockScreenUtil {


    private OverlayDialog mOverlayDialog;
    private OnLockStatusChangedListener mLockStatusChangedListener;

    public LockScreenUtil() {
        reset();
    }

    /**
     * Locks the screen.
     * @param activity Activity to lock with
     */
    public void lock(Activity activity) {
        if (mOverlayDialog == null) {
            mOverlayDialog = new OverlayDialog(activity);
            mOverlayDialog.show();
            mLockStatusChangedListener = (OnLockStatusChangedListener) activity;
        }
    }

    /**
     * Resets the variables.
     */
    public void reset() {
        if (mOverlayDialog != null) {
            mOverlayDialog.dismiss();
            mOverlayDialog = null;
        }
    }

    /**
     * Unlocks the device.
     */
    public void unlock() {
        if (mOverlayDialog != null) {
            mOverlayDialog.dismiss();
            mOverlayDialog = null;
            if(mLockStatusChangedListener!=null)
            {
                mLockStatusChangedListener.onLockStatusChanged(false);
            }
        }
    }

    /**
     * Overlay to disable hardware buttons.
     */
    private static class OverlayDialog extends AlertDialog {
        public OverlayDialog(Activity activity) {
            super(activity, R.style.OverlayDialog);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.type = LayoutParams.TYPE_SYSTEM_ERROR;
            params.dimAmount = 0.0F;
            params.width = 0;
            params.height = 0;
            params.gravity = Gravity.BOTTOM;
            getWindow().setAttributes(params);
            getWindow().setFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED | LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    0xffffff);
            setOwnerActivity(activity);
            setCancelable(false);
        }

        // consume touch events
        public final boolean dispatchTouchEvent(MotionEvent motionevent) {
            return true;
        }

    }
}