package io.alstonlin.learninglock;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import me.zhanghai.android.patternlock.PatternView;

/**
 * The Service that runs in the background to "lock" and "unlock" the screen by attaching a View
 * over the WindowManager whenever the power button is pressed.
 */
public class LockScreenService extends Service {

    // Constants
    public static final String PATTERN_FILENAME = "pattern";
    public static final String PASSCODE_FILENAME = "passcode";
    public static final String LOCKSCREEN_SERVICE = "lockscreen_service";
    // Fields
    private String pin;
    private BroadcastReceiver mReceiver;
    private FrameLayout lockView;
    private ArrayList<int[]> pattern = null;
    private ArrayList<Double> timeAtClick = new ArrayList<>();
    private String[] weather;
    // Location Listener for Weather
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            GetWeatherTask task = new GetWeatherTask();
            task.execute(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    /**
     * Called when the Service is started. Either runs the initial setup, or simply locks the screen.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // TODO: Move the setup to the Activity.
        if (!LockScreenML.isSetup() && !LockScreenML.setup(this)) { // First time
            Intent intent = new Intent(this, SetPasswordActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            lockView = new FrameLayout(this);
            LockScreenUtil.lock(this, lockView);
            loadPattern();
            setupPatternListener();
            setupWeather();
        }
    }

    /**
     * Setups the function to show the weather.
     */
    private void setupWeather() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // No Permission; returns
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        // Uses last known location
        if (location != null){
            GetWeatherTask task = new GetWeatherTask();
            task.execute();
        } else {
            // Requests for location
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
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

    /**
     * Setups the Listener to when the user enters a pattern.
     */
    private void setupPatternListener(){
        final PatternView patternView = (PatternView) lockView.findViewById(R.id.pattern);
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
                ArrayList<int[]> current = LockScreenUtil.toList(p);
                patternView.clearPattern();
                if (LockScreenUtil.equals(current, pattern)) {
                    double[] elapsedTimes = LockScreenUtil.calculateTimeElapsed(timeAtClick);
                    boolean verified = LockScreenML.getInstance().predict(elapsedTimes);
                    if (verified) LockScreenUtil.unlock(LockScreenService.this, lockView);
                    else {
                        Toast.makeText(LockScreenService.this, "Suspicious Pattern, please enter your pin as well", Toast.LENGTH_SHORT).show();
                        handleSuspectEntry(elapsedTimes);
                    }
                } else
                    Toast.makeText(LockScreenService.this, "Wrong Pattern", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Handle what happens when ML says it's not the owner who entered the password.
     * @param times The data that the user entered
     */
    private void handleSuspectEntry(double[] times){
        // TODO: Finish this by changing the View
        LockScreenUtil.unlock(this, lockView);
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
        // Registers the receiver to detect when screen is turned off and on
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new LockScreenReceiver();
        registerReceiver(mReceiver, filter);
        // If this service was called by an intent (i.e. from SetPasswordActivity)
        // TODO: Clean this up
        if (intent != null) {
            pin = intent.getStringExtra(KeypadActivity.PASSCODE_VALUE);
            if (pin != null) {
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
                if (pin.equals(actual)) {
                    //LockScreenML.getInstance().addEntry(delayTimes, true, true);
                    LockScreenUtil.unlock(this, lockView);
                } else {
                    Toast.makeText(this, "Wrong PIN!", Toast.LENGTH_LONG).show();
                }
            } else {
                startForeground();
            }
        }
        return START_STICKY;
    }

    /**
     * Runs in foreground so it won't be killed by system.
     */
    private void startForeground() {
        setupNotification();
    }

    /**
     * Sets up the notification showing the app is running.
     */
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


    /**
     * Unregisters receiver.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        LockScreenUtil.unlock(this, lockView);
    }

    /**
     * Not Used.
     * @param intent The intent this was started from
     * @return Not Used.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * AsyncTask for downloading images for weather.
     */
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    /**
     * AsyncTask for getting the weather information.
     */
    private class GetWeatherTask extends AsyncTask<Location, Void, String[]> {
        @Override
        protected String[] doInBackground(Location... location) {
            return DAO.getWeather(getString(R.string.weather_api_key), location[0].getLongitude(), location[0].getLatitude());
        }

        @Override
        protected void onPostExecute(String[] val){
            super.onPostExecute(val);
            try {
                weather = val;
                ImageView imageView = (ImageView) lockView.findViewById(R.id.weather_img);
                TextView tempView = (TextView) lockView.findViewById(R.id.temp);
                new DownloadImageTask(imageView).execute(weather[0]);
                tempView.setText(weather[1] + "C");
            } catch (Throwable t){
                t.printStackTrace();
            }
        }
    };
}