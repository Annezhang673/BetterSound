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
    ArrayList<Double> tmp = new ArrayList<Double>(100);

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

            /*
             * Delete audio file every once in a while, to save memory
             */
            if (flush == 20) {
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
            if (audio_level != null) {
                audio_level.setText(String.format(Locale.getDefault(), "%.2f", level));
            }

            try {
                Thread.sleep(400);
            }
            catch (InterruptedException e) {
                break;
            }
        }

        // Wanted to check what ampl should be...
        Double ave = 0.0;
        for (int i = 0; i <10; i++) {
            ave += tmp.get(i);
        }
        Log.i("TAG", "RAW AVE=" + ave/10);

        recorder.stop();
        recorder.release();
    }

    private double getDecibels(){
        double amp = getAmplitudeEMA(); // ranges from 0-32767
        tmp.add(0, amp);

        double dec = 0;

        if (amp <=1.1) dec = 0;
        else if (amp <= 6584.41) {
            dec=8.58136*Math.log(0.997309*amp);
        }
        else {
            dec=6.16788e-8*(amp*amp) - 0.000809727*amp + 77.8971;
        }

//        if (amp <= 65.76481596) {
//            dec = 0.471377*amp;
//        }
//        else if (amp <= 80.62998095) {
//            dec = 4.45546 + 0.403628*amp;
//        }
//        else if (amp <= 157.7233429) {
//            dec = 25.4954 + 0.142684*amp;
//        }
//        else if (amp <= 1143.042214) {
//            dec = 45.879 + 0.0134474*amp;
//        }
//        else if (amp <= 2695.782844) {
//            dec = 55.6921 + 0.00486237*amp;
//        }
//        else if (amp <= 8312.673073) {
//            dec = 65.7284 + 0.00113942*amp;
//        }
//        else if (amp <= 15823.80865) {
//            dec = 68.449 + 0.000812128*amp;
//        }
//        else if (amp <= 23984.55577) {
//            dec = 59.7769 + 0.00136017*amp;
//        }
//        else if (amp <=24454.80466) {
//            dec = 20.9945 + 0.00297715*amp;
//        }
//        else {
//            dec = -42.4221 + 0.00557036*amp;
//        }

        Log.i("TAG", "RAW AMP="+ amp);

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

    private double map(double x, double in_min, double in_max, double out_min, double out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

}
