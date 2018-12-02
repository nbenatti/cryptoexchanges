package com.benatti.nicolas.cryptoexchanges;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class Splash extends Activity {

    int startDelay = 2000;  // in microseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Thread.sleep(1000);
        }
        catch(InterruptedException e) {
            return;
        }

        startActivity(new Intent(this, MainActivity.class));

        finish();
    }
}
