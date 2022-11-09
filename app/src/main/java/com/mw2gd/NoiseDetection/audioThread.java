package com.mw2gd.NoiseDetection;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.TextView;
import java.lang.Math;
import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Locale;

public class audioThread implements Runnable {
    private TextView audio_level;   // View that displays current decibels
    private Thread worker;          // Runtime Thread
    private MediaRecorder recorder; // Used to record audio and video
    private double level = 0.0;          // Field to hold decibel value
    private Context context;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.4;
    private int flush = 0;
//    ArrayList<Double> tmp = new ArrayList<Double>(100);

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
            flush++;

            /*
             * Calculate Decibels
             */
            level = getDecibels();

            if (flush == 10) {
                File myObj = new File(context.getExternalCacheDir().getAbsolutePath() + "/tmp.3gp");
                myObj.delete();

                Log.i("SUCCESS", "File Deleted");
                try{
                    myObj.createNewFile();
                }catch (IOException e) {
                    Log.i("ERROR", "File not Created");
                }

                flush = 0;
            }

            // update screen
            if (audio_level != null ) {
                audio_level.setText(String.format(Locale.getDefault(), "%.2f", level));
            }

            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                break;
            }
        }

//        // Wanted to check what ampl should be...
//        Double ave = 0.0;
//        for (int i = 0; i <10; i++) {
//            ave += tmp.get(i);
//        }
//        Log.i("TAG", "AVE=" + ave/10);

        recorder.stop();
        recorder.release();
    }

    private double getDecibels(){
        double amp = getAmplitudeEMA();
        //tmp.add(0, amp);
        double ampl = 2.330386054706662; // reference amplitude; Accuracy depends on this

        double dec = 20 * Math.log10(amp / ampl);
        Log.i("TAG", "AMP="+ amp);
        if (Double.isInfinite(dec)) {
            dec = 0.0;
        }
        return  dec;
    }

    public double getAmplitude() {
        if (recorder != null)
            return  (recorder.getMaxAmplitude());
        else
            return 0.0;
    }

    // Performs Exponential Moving Average on amplitudes
    public double getAmplitudeEMA() {
        double amp =  getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

}
