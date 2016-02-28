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
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import me.zhanghai.android.patternlock.PatternView;


public class LockScreenActivity extends Activity implements OnLockStatusChangedListener {

    private ArrayList<int[]> pattern = null;
    public static final String PATTERN_FILENAME = "pattern";
    public static final String PASSCODE_FILENAME = "passcode";
    private Button btnUnlock;
    private Button btnEdit;
    private LockScreenUtil lockscreenUtil;
    ArrayList<Double> timeAtClick = new ArrayList<>();
    private double[] delayTimes; // To store for startActivityForResult

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

        // Initialization
        setContentView(R.layout.activity_lockscreen);
        lockscreenUtil = new LockScreenUtil();

        // Sets up the machine learning Singleton
        if(!LockScreenML.isSetup() && !LockScreenML.setup(this)){ // First time
            Intent intent = new Intent(LockScreenActivity.this, SetPasswordActivity.class);
            startActivity(intent);
        }

        // Buttons
        btnEdit = (Button) findViewById(R.id.editPass);
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LockScreenActivity.this, SetPasswordActivity.class);
                startActivity(i);
            }
        });

        setupScreen();
        loadPattern();
        setupPatternListener();
    }

    private void setupPatternListener(){
        final PatternView patternView = (PatternView) findViewById(R.id.pattern);
        patternView.setOnPatternListener(new PatternView.OnPatternListener() {
            @Override
            public void onPatternStart() {
            }

            @Override
            public void onPatternCleared() {
            }

            @Override
            public void onPatternCellAdded(List<PatternView.Cell> pattern) {
                timeAtClick.add(((double) System.currentTimeMillis()));
            }

            @Override
            public void onPatternDetected(List<PatternView.Cell> p) {
                ArrayList<int[]> current = toList(p);
                patternView.clearPattern();
                if (equal(current, pattern)){
                    double[] elapsedTimes = calculateTimeElapsed(timeAtClick);
                    boolean verified = LockScreenML.getInstance().predict(elapsedTimes);
                    if (verified) lockscreenUtil.unlock();
                    else {
                        Toast.makeText(LockScreenActivity.this, "Suspicious Pattern, please enter your pin as well", Toast.LENGTH_SHORT).show();
                        handleSuspectEntry(elapsedTimes);
                    }
                }
                else Toast.makeText(LockScreenActivity.this, "Wrong Pattern", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sets up the screen to be locked.
     */
    private void setupScreen(){
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
     * Loads pattern from file.
     */
    private void loadPattern(){
        FileInputStream fis = null;
        ObjectInputStream is = null;
        try {
            fis = openFileInput(PATTERN_FILENAME);
            is = new ObjectInputStream(fis);
            pattern = (ArrayList<int[]>) is.readObject();
        } catch(IOException | ClassNotFoundException e){
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (fis != null) fis.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private double[] calculateTimeElapsed(ArrayList<Double> timeAtClick){
        double[] elapsedTimes = new double[timeAtClick.size()-1];
        for (int i = 0; i < timeAtClick.size() - 1; i++) {
            elapsedTimes[i] = timeAtClick.get(i + 1) - timeAtClick.get(i);
        }
        return elapsedTimes;
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

    private void handleSuspectEntry(double[] times){
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
                    LockScreenML.getInstance().addEntry(delayTimes, true, true);
                } else {
                    Toast.makeText(this, "Wrong PIN!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Converts the list of Cells representing patterns to a list of int[2] with the same row/col info
     * @param pattern The pattern to convert
     * @return The converted list of int[2]
     */
    private ArrayList<int[]> toList(List<PatternView.Cell> pattern){
        ArrayList<int[]> list = new ArrayList<>();
        for (PatternView.Cell cell : pattern){
            int[] a = new int[2];
            a[0] = cell.getRow();
            a[1] = cell.getColumn();
            list.add(a);
        }
        return list;
    }

    /**
     * Determines if two lists of int[2] are equal, because appearently Java can't check that for us.
     * @param l1 The first list
     * @param l2 The second list
     * @return If the lists are equal
     */
    private boolean equal(List<int[]> l1, List<int[]> l2){
        if (l1.size() != l2.size()) return false;
        for (int i = 0; i < l1.size(); i++){
            int[] e1 = l1.get(i);
            int[] e2 = l2.get(i);
            if (e1[0] != e2[0] || e1[1] != e2[1]) return false;
        }
        return true;
    }
}