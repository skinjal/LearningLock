package io.alstonlin.learninglock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

public class KeypadActivity extends Activity{

    public static final String PASSCODE_VALUE = "PasscodeValue";
    public static final int ACTIVITY_CODE = 9;
    //Fields
    private ArrayList<Character> pin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pin = new ArrayList<>();
        setContentView(R.layout.passcode);
    }

    private void updateDisplay(){
        TextView tv = (TextView) findViewById(R.id.display);
        String text = new String(new char[pin.size()]).replace("\0", "*");
        tv.setText(text);
    }

    public void clickOne(View v){
        pin.add('1');
        updateDisplay();
    }

    public void clickTwo(View v){
        pin.add('2');
        updateDisplay();
    }

    public void clickThree(View v){
        pin.add('3');
        updateDisplay();
    }

    public void clickFour(View v){
        pin.add('4');
        updateDisplay();
    }

    public void clickFive(View v){
        pin.add('5');
        updateDisplay();
    }

    public void clickSix(View v){
        pin.add('6');
        updateDisplay();
    }

    public void clickSeven(View v){
        pin.add('7');
        updateDisplay();
    }

    public void clickEight(View v){
        pin.add('8');
        updateDisplay();
    }

    public void clickNine(View v){
        pin.add('9');
        updateDisplay();
    }

    public void clickZero(View v){
        pin.add('0');
        updateDisplay();
    }

    public void clickDone(View v){
        Intent result = new Intent();
        // Creates and puts String as result
        StringBuilder builder = new StringBuilder();
        for (char c : pin){
            builder.append(c);
        }
        result.putExtra(PASSCODE_VALUE, builder.toString());
        setResult(Activity.RESULT_OK, result);
        finish();
    }
}
