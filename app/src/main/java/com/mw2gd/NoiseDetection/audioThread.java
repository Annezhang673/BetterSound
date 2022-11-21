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

//        if (amp <=1.1) dec = 0;
//        else if (amp <= 6584.41) {
//            dec=8.58136*Math.log(0.997309*amp);
//        }
//        else {
//            dec=6.16788e-8*(amp*amp) - 0.000809727*amp + 77.8971;
//        }

        if (amp <= 65.76481596) {
            dec = 2.72589257685387e-06*Math.pow(amp,3)-0.00101950684725297*Math.pow(amp,2)+0.526634835504476*amp;
        }
        else if (amp <= 80.62998095) {
            dec = -0.000647377604358068*Math.pow((amp-65.76481596),3)+0.00799003903265281*Math.pow((amp-65.76481596),2)+0.427908021433771*(amp-65.76481596)+31;
        }
        else if (amp <= 157.7233429) {
            dec = -2.84478266991650e-06*Math.pow((amp-80.62998095),3)-0.000994943888905628*Math.pow((amp-80.62998095),2)+0.236295351497476*(amp-80.62998095)+37;
        }
        else if (amp <= 1143.042214) {
            dec = 1.30516747974038e-08*Math.pow((amp-157.7233429),3)-3.18568163531216e-05*Math.pow((amp-157.7233429),2)+0.0321652844244236*(amp-157.7233429)+48;
        }
        else if (amp <= 2695.782844) {
            dec = -9.64683344579413e-11*Math.pow((amp-1143.042214),3)-1.48503082953953e-06*Math.pow((amp-1143.042214),2)+0.00740082354402243*(amp-1143.042214)+61.2500;
        }
        else if (amp <= 8312.673073) {
            dec = 2.43595731294802e-11*Math.pow((amp-2695.782844),3)-3.06298031622021e-07*Math.pow((amp-2695.782844),2)+0.00209133166671665*(amp-2695.782844)+68.800;
        }
        else if (amp <= 15823.80865) {
            dec = 6.11993627608928e-12*Math.pow((amp-8312.673073),3)-6.51276398656832e-08*Math.pow((amp-8312.673073),2)+0.000956040655285463*(amp-8312.673073)+75.200;
        }
        else if (amp <= 23984.55577) {
            dec = 5.89949306246699e-12*Math.pow((amp-15823.80865),3)-5.66216022452558e-09*Math.pow((amp-15823.80865),2)+0.00101348381880290*(amp-15823.80865)+81.300;
        }
        else if (amp <=24454.80466) {
            dec = -5.16556335801354e-10*Math.pow((amp-23984.55577),3)+2.10872684565387e-06*Math.pow((amp-23984.55577),2)+0.00209974856650045*(amp-23984.55577)+92.400;
        }
        else {
            dec = -5.09285564650298e-11*Math.pow((amp-24454.80466),3)+1.84053534832653e-06*Math.pow((amp-24454.80466),2)+0.00374031694844051*(amp-24454.80466)+93.800;
        }

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
