package io.alstonlin.learninglock;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
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


public class LockScreenService extends Service {

    // Constants
    public static final String PATTERN_FILENAME = "pattern";
    public static final String PASSCODE_FILENAME = "passcode";
    public static final String LOCKSCREEN_SERVICE = "lockscreen_service";
    // Fields
    private String pin;
    private BroadcastReceiver mReceiver;
    private WindowManager windowManager;
    private View container;
    private ArrayList<int[]> pattern = null;
    private Button btnEdit;
    private ArrayList<Double> timeAtClick = new ArrayList<>();
    private double[] delayTimes; // To store for startActivityForResult

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Sets up the ML Singleton
        if(!LockScreenML.isSetup() && !LockScreenML.setup(this)){ // First time
            Intent intent = new Intent(this, SetPasswordActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            lock();
            loadPattern();
            setupPatternListener();
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

    private void setupPatternListener(){
        final PatternView patternView = (PatternView) container.findViewById(R.id.pattern);
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
                if (equal(current, pattern)) {
                    double[] elapsedTimes = calculateTimeElapsed(timeAtClick);
                    boolean verified = LockScreenML.getInstance().predict(elapsedTimes);
                    if (verified) unlock();
                    else {
                        Toast.makeText(LockScreenService.this, "Suspicious Pattern, please enter your pin as well", Toast.LENGTH_SHORT).show();
                        handleSuspectEntry(elapsedTimes);
                    }
                } else
                    Toast.makeText(LockScreenService.this, "Wrong Pattern", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSuspectEntry(double[] times){
        this.delayTimes = times;
        unlock();
        // Starts Keypad Activity
        Intent intent = new Intent(this, KeypadActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LOCKSCREEN_SERVICE, true);
        startActivity(intent);
    }

    private double[] calculateTimeElapsed(ArrayList<Double> timeAtClick){
        double[] elapsedTimes = new double[timeAtClick.size()-1];
        for (int i = 0; i < timeAtClick.size() - 1; i++) {
            elapsedTimes[i] = timeAtClick.get(i + 1) - timeAtClick.get(i);
        }
        return elapsedTimes;
    }

    /**
     * Registers the receiver
     * @param intent The intent this was called by
     * @param flags Flags passed
     * @param startId Service ID
     * @return The result
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new LockScreenReceiver();
        registerReceiver(mReceiver, filter);

        pin = intent.getStringExtra(KeypadActivity.PASSCODE_VALUE);
        if (pin != null){
            String actual = null;
            try {
                FileInputStream fileInputStream = openFileInput(PASSCODE_FILENAME);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                actual = (String) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Compares
            if (pin.equals(actual)){
                LockScreenML.getInstance().addEntry(delayTimes, true, true);
                stopSelf();
            } else {
                Toast.makeText(this, "Wrong PIN!", Toast.LENGTH_LONG).show();
            }
        } else {
            startForeground();
        }
        return START_STICKY;
    }

    /**
     * Runs in foreground so it won't be killed by system
     */
    private void startForeground() {
        setupNotification();
        // Buttons
        if (container != null) {
            btnEdit = (Button) container.findViewById(R.id.editPass);
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(LockScreenService.this, SetPasswordActivity.class);
                    startActivity(i);
                }
            });
        }
    }

    private void setupNotification(){
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setTicker(getResources().getString(R.string.app_name))
                .setContentText("Running")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(null)
                .setOngoing(true)
                .build();
        startForeground(9999, notification);
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

    /**
     * Unregisters receiver
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        unlock();
    }

    private void unlock(){
        try {
            if (container != null) windowManager.removeView(container);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    public void lock(){
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.OPAQUE);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;
        params.screenOrientation = Configuration.ORIENTATION_PORTRAIT;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        container = new FrameLayout(this);
        windowManager.addView(container, params);
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.activity_lockscreen, (ViewGroup) container);
    }
}