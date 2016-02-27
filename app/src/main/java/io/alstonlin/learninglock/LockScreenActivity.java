package io.alstonlin.learninglock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;


public class LockScreenActivity extends Activity implements OnLockStatusChangedListener {

    public static String PASSCODE_FILENAME = "passcode.txt";
    private Button btnUnlock;
    private Button btnEdit;
    private LockScreenUtil lockscreenUtil;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Makes the app fullscreen
        getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD

        );

        setContentView(R.layout.activity_lockscreen);
        lockscreenUtil = new LockScreenUtil();
        // Sets up the machine learning Singleton
        if(!LockScreenML.setup(this)){ // First time
            Intent intent = new Intent(LockScreenActivity.this, SetPasswordActivity.class);
            startActivity(intent);
        }

        // TODO: Remove temporary buttons
        btnEdit = (Button) findViewById(R.id.editPass);
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LockScreenActivity.this, SetPasswordActivity.class);
                startActivity(i);
            }
        });
        btnUnlock = (Button) findViewById(R.id.btnUnlock);
        btnUnlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockscreenUtil.unlock();
            }
        });

        // Disables anything that would make this exit
        if (getIntent() != null && getIntent().hasExtra("kill") && getIntent().getExtras().getInt("kill") == 1) {
            //enableKeyguard();
            lockscreenUtil.unlock();
        } else {
            try {
                //disableKeyguard();
                lockscreenUtil.lock(LockScreenActivity.this);
                startService(new Intent(this, LockScreenService.class));
                StateListener phoneStateListener = new StateListener();
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Unlocks if there's a call
     */
    private class StateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            if (state == TelephonyManager.CALL_STATE_RINGING){
                lockscreenUtil.unlock();
            }
        }
    };

    /**
     * Disables back button
     */
    @Override
    public void onBackPressed() {
        return;
    }

    /**
     * Key Listener
     * @param keyCode The code for the key pressed
     * @param event The Key press event
     * @return If the event was handled
     */
    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_POWER
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_CAMERA
                || keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void handleSuspectEntry(double[][] times){
        // Starts Keypad Activity
        Intent intent = new Intent(this, KeypadActivity.class);
        startActivityForResult(intent, KeypadActivity.ACTIVITY_CODE);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN
                || event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
            return false;
        }
        return event.getKeyCode() == KeyEvent.KEYCODE_HOME;
    }

    @Override
    public void onLockStatusChanged(boolean isLocked) {
        if (!isLocked) {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        lockscreenUtil.unlock();
    }

//    @SuppressWarnings("deprecation")
//    private void disableKeyguard() {
//        KeyguardManager mKM = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
//        KeyguardManager.KeyguardLock mKL = mKM.newKeyguardLock("IN");
//        mKL.disableKeyguard();
//    }
//
//    @SuppressWarnings("deprecation")
//    private void enableKeyguard() {
//        KeyguardManager mKM = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
//        KeyguardManager.KeyguardLock mKL = mKM.newKeyguardLock("IN");
//        mKL.reenableKeyguard();
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == KeypadActivity.ACTIVITY_CODE) {
            // Compares to actual pin value
            if (resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra(KeypadActivity.PASSCODE_VALUE);
                // Opens file and compares
                String actual = null;
                File fl = new File(PASSCODE_FILENAME);
                try {
                    FileInputStream fin = new FileInputStream(fl);
                    actual = convertStreamToString(fin);
                    fin.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Compares
                if (result.equals(actual)){

                }
            }
        }
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
}