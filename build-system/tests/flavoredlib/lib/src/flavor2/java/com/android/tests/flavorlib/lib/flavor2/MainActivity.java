package com.android.tests.flavorlib.lib.flavor2;

import android.app.Activity;
import android.os.Bundle;

import com.android.tests.flavorlib.lib.R;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lib_main);

        Lib.handleTextView(this);
    }
}
