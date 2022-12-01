package com.mw2gd.NoiseDetection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.ResultReceiver;
import android.widget.Toast;

import java.io.IOException;
import java.security.Permission;

public class MainActivity extends AppCompatActivity {
    private MediaRecorder recorder;
    private int i = 0;
    private static int MICROPHONE_PERMISSION_CODE = 200;
    audioThread t1; // This thread will read from Mic

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    ProgressBar progressBar;
    TextView textLatLong, address, postcode, locaity, state, district, country;
    ResultReceiver resultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (isMicrophonePresent()) {
            getMicrophonePermission();
        }

        Button next_button= (Button)findViewById(R.id.next_button);
        next_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.layout);
            }
        });
    }

    public void listen(View view) {
        if (i == 0) {
            // Start audio Thread
            TextView audio_level = (TextView) findViewById(R.id.audio_level);
            t1 = new audioThread(getApplicationContext(), audio_level);
            t1.start();
            i=1;
        }
        else {
            i=0;
            t1.interrupt();
        }
    }

    private boolean isMicrophonePresent() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    private void getMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);
        }
    }

}