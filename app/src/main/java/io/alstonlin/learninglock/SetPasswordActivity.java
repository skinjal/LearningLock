package io.alstonlin.learninglock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import me.zhanghai.android.patternlock.PatternUtils;
import me.zhanghai.android.patternlock.PatternView;
import me.zhanghai.android.patternlock.SetPatternActivity;

public class SetPasswordActivity extends SetPatternActivity {

    Context context = this;
    public List<PatternView.Cell> savedPattern;
    int nodesClicked = 0;
    double[] timeAtClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedPattern == null){
            //prompt user to create new one
        }
        else{
            //ask user if new pattern is desired
        }

        final PatternView patternView = (PatternView) findViewById(R.id.setPasswordPattern);
        patternView.setOnPatternListener(new PatternView.OnPatternListener() {
            @Override
            public void onPatternStart() {

            }

            @Override
            public void onPatternCleared() {
            }

            @Override
            public void onPatternCellAdded(List<PatternView.Cell> pattern) {
                //called each time a new node is touched; get value of times at each
                timeAtClick[nodesClicked] = ((double) System.currentTimeMillis());
                nodesClicked++;

            }

            @Override
            public void onPatternDetected(List<PatternView.Cell> pattern) {
                //returns size of pattern (nodes clicked)
                LockScreenML.setup(context);
                nodesClicked = 0;
                patternView.clearPattern();

                savedPattern = pattern;


            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    public double[] elapsedTimesArray (double[] timeAtClick){

        double[] elapsedTimes = new double[timeAtClick.length-1];
        for (int i = 0; i < timeAtClick.length - 1; i++) {
            elapsedTimes[i] = timeAtClick[i+1] - timeAtClick[i];
        }

        return elapsedTimes;
    }

    @Override
    protected void onSetPattern(List<PatternView.Cell> pattern) {
        //super.onSetPattern(pattern);
        Log.wtf("is this called", "yes");
        PatternUtils.patternToSha1(pattern);
        String patternSha1 = PatternUtils.patternToSha1String(pattern);

    }

    private void setPasscode(){
        // Starts Keypad Activity
        Intent intent = new Intent(this, KeypadActivity.class);
        startActivityForResult(intent, KeypadActivity.ACTIVITY_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == KeypadActivity.ACTIVITY_CODE) {
            if (resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra(KeypadActivity.PASSCODE_VALUE);
                // Saves passcode to file
                try {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(LockScreenActivity.PASSCODE_FILENAME, Context.MODE_PRIVATE));
                    outputStreamWriter.write(result);
                    outputStreamWriter.close();
                }
                catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }
            }
        }
    }
}
