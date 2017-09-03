package com.example.windwolf447.kef_measurement;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.test.suitebuilder.TestMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.uol.aig.fftpack.RealDoubleFFT;

import static java.lang.Math.abs;
import static java.lang.Math.log10;


public class MainActivity extends Activity implements OnClickListener {

    int frequency = 44100;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private RealDoubleFFT transformer;
    int blockSize = 512;
    double[] maxdb;
    double[] next_maxdb;
    double Base = 0.003;
    int counter = 0;


    Button startStopButton;
    boolean started = false;

    RecordAudio recordTask;
    MediaPlayer mp;

    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;
    Paint text_paint;
    Paint tag_paint;


    //AudioRecord audioRecord;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startStopButton = (Button) this.findViewById(R.id.start_stop_btn);
        startStopButton.setOnClickListener(this);

        transformer = new RealDoubleFFT(blockSize);

        imageView = (ImageView) this.findViewById(R.id.imageView1);
        bitmap = Bitmap.createBitmap((int) 512, (int) 200,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.WHITE);
        text_paint = new Paint();
        text_paint.setColor(Color.argb(255,255,165,0));
        tag_paint = new Paint();
        tag_paint.setColor(Color.argb(240,165,165,165));
        imageView.setImageBitmap(bitmap);
        maxdb = new double[blockSize];
        next_maxdb = new double[blockSize];


        mp = MediaPlayer.create(this, R.raw.white);
    }

    public class RecordAudio extends AsyncTask<Void, double[], Void> {

        @Override
        protected Void doInBackground(Void... arg0) {

            try {
                // int bufferSize = AudioRecord.getMinBufferSize(frequency,
                // AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                int bufferSize = AudioRecord.getMinBufferSize(frequency,
                        channelConfiguration, audioEncoding);

                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);

                short[] buffer = new short[blockSize];
                double[] toTransform = new double[blockSize];

                audioRecord.startRecording();

                // started = true; hopes this should true before calling
                // following while loop

                while (started) {
                    int bufferReadResult = audioRecord.read(buffer, 0,
                            blockSize);

                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed
                        // 16
                    }                                       // bit


                    transformer.ft(toTransform);
                    publishProgress(toTransform);



                }

                audioRecord.stop();

            } catch (Throwable t) {
                t.printStackTrace();
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(double[]... toTransform) {
            canvas.drawColor(Color.BLACK);
            canvas.drawLine(0, 0, 512, 0, text_paint);
            canvas.drawLine(0, 0, 0, 200, text_paint);
            canvas.drawLine(511, 0, 511, 199, text_paint);
            canvas.drawLine(0, 199, 511, 199, text_paint);
            for (int i = 1; i < toTransform[0].length-1; i++) {
                int x = i;
                double dbValue = (20*log10(abs(toTransform[0][i])/Base));
                double next_dbValue = (20*log10(abs(toTransform[0][i+1])/Base));
                if(dbValue > maxdb[i]){
                    maxdb[i] = dbValue;
                }
                if(next_dbValue > next_maxdb[i]){
                    next_maxdb[i] = next_dbValue;
                }

                int downy = (int) (200 - maxdb[i]*2);
                int upy = (int) (200 - next_maxdb[i]*2);
                canvas.drawLine(x, downy, x+1, upy, paint);
            }
            //canvas.drawText(String.valueOf(maxdb)+" db", maxdb_i,(int) (100 - maxdb), text_paint);
            canvas.drawText("25 db", 5,150, tag_paint);
            canvas.drawLine(0, 150, 512, 150, tag_paint);
            canvas.drawText("50 db", 5,100, tag_paint);
            canvas.drawLine(0, 100, 512, 100, tag_paint);
            canvas.drawText("75 db", 5,50, tag_paint);
            canvas.drawLine(0, 50, 512, 50, tag_paint);

            canvas.drawText("5500 Hz", 129,198, tag_paint);
            canvas.drawLine(128, 0, 128, 200, tag_paint);
            canvas.drawText("11000 Hz", 257,198, tag_paint);
            canvas.drawLine(256, 0, 256, 200, tag_paint);
            canvas.drawText("16500 Hz", 385,198, tag_paint);
            canvas.drawLine(384, 0, 384, 200, tag_paint);


            counter++;
            imageView.invalidate();
            if(counter > 1000){
                counter = 0;
                started = false;
                startStopButton.setText("Start Measurement");
                recordTask.cancel(true);
                mp.stop();
            }

            // TODO Auto-generated method stub
            // super.onProgressUpdate(values);
        }

    }

    @Override



    public void onClick(View arg0) {
        // TODO Auto-generated method stub
        if (started) {

        } else {
            for(int i = 0;i<blockSize;i++){
                maxdb[i] = 0;
            }
            for(int i = 0;i<blockSize;i++){
                next_maxdb[i] = 0;
            }

            started = true;
            startStopButton.setText("Measuring...");
            mp.release();
            mp = MediaPlayer.create(this, R.raw.white);
            mp.start();
            recordTask = new RecordAudio();
            recordTask.execute();

        }
    }

    public double maxValue(double array[]){
        List<Double> list = new ArrayList<Double>();
        for (int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }
        return Collections.max(list);
    }

}