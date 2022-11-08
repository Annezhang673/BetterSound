package com.mw2gd.NoiseDetection;

import android.content.Context;
import android.media.MediaRecorder;
import android.widget.TextView;
import java.lang.Math;

import java.io.IOException;
import java.util.Locale;

public class audioThread implements Runnable {
    private TextView audio_level;   // View that displays current decibels
    private Thread worker;          // Runtime Thread
    private MediaRecorder recorder; // Used to record audio and video
    private double level = 0;          // Field to hold decibel value
    private Context context;

    /*
     * Constructor is passed the relevant TextView
     */
    public audioThread(Context context, TextView view) {
        this.context = context;
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
        recorder.setOutputFile(context.getExternalCacheDir().getAbsolutePath() + "/tmp.3gp");

        //System.out.println("+++++++" + context.toString());

        try {
            recorder.prepare();
            recorder.start();   // Recording is now started
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * Runs until interrupt is encountered
         */
        while(!Thread.currentThread().isInterrupted()) {
            if (audio_level != null ) {
                audio_level.setText(String.format(Locale.getDefault(), "%.2f", level));
            }

            try {
                Thread.sleep(400);
            }
            catch (InterruptedException e) {
                return;
            }

            /*
             * Calculate Decibels
             */
            level = getDecibels(recorder);
            System.out.println("+++" + level);
        }

        recorder.stop();
        recorder.release();
    }

    private double getDecibels(MediaRecorder recorder){
        int amplitude = recorder.getMaxAmplitude();
        return amplitude;
    }

}
