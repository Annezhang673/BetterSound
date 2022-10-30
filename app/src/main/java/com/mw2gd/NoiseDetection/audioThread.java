package com.mw2gd.NoiseDetection;

import android.media.MediaRecorder;
import android.widget.TextView;

import java.io.IOException;
import java.util.Locale;

public class audioThread implements Runnable {
    private TextView audio_level;   // View that displays current decibels
    private Thread worker;          // Runtime Thread
    private MediaRecorder recorder; // Used to record audio and video
    private int level = 0;          // Field to hold decibel value

    /*
     * Constructor is passed the relevant TextView
     */
    public audioThread(TextView view) {
        audio_level = view;
    }

    /*
     * Starts the Working Thread
     */
    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    /*
     * Flips the interrupt flag of thread
     */
    public void interrupt() {
        worker.interrupt();
    }

    /*
     * Defines the runtime behavior of thread
     */
    @Override
    public void run() {

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile("dev/null");

        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.start();   // Recording is now started

        /*
         * Runs until interrupt is encountered
         */
        while(!Thread.currentThread().isInterrupted()) {
            if (audio_level != null ) {
                audio_level.setText(String.format(Locale.getDefault(), "%d", level));
            }
        }

        recorder.stop();
        recorder.release();
    }
}
