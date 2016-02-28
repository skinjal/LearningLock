package io.alstonlin.learninglock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LockScreenActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Starts the service
        try {
            startService(new Intent(this, LockScreenService.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Finishes
        finish();
    }

}