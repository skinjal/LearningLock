package io.alstonlin.learninglock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LockScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)
                || intent.getAction().equals(Intent.ACTION_SCREEN_ON)
                || intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            startLockscreen(context);
        }
    }

    private void startLockscreen(Context context) {
        context.stopService(new Intent(context, LockScreenService.class));
        context.startService(new Intent(context, LockScreenService.class));
    }

}