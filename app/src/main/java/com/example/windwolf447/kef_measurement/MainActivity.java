package com.example.windwolf447.kef_measurement;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
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
    double maxdb;
    double Base = 0.003;

    Button startStopButton;
    boolean started = false;

    RecordAudio recordTask;

    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;
    Paint text_paint;
    TextView disply;

    //AudioRecord audioRecord;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        disply = (TextView) this.findViewById(R.id.showdb);
        startStopButton = (Button) this.findViewById(R.id.start_stop_btn);
        startStopButton.setOnClickListener(this);

        transformer = new RealDoubleFFT(blockSize);

        imageView = (ImageView) this.findViewById(R.id.imageView1);
        bitmap = Bitmap.createBitmap((int) 512, (int) 100,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.WHITE);
        text_paint = new Paint();
        text_paint.setColor(Color.argb(255,255,165,0));
        imageView.setImageBitmap(bitmap);


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
                    maxdb = maxValue(toTransform);

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
            maxdb = 0;
            int maxdb_i = 0;
            for (int i = 1; i < toTransform[0].length; i++) {
                int x = i;
                double dbValue = (20*log10(abs(toTransform[0][i])/Base));
                if(dbValue > maxdb){
                    maxdb = dbValue;
                    maxdb_i = i;
                }
                int downy = (int) (100 - dbValue);
                if(downy == 0){
                    System.out.println(dbValue);
                }
                int upy = 100;
                canvas.drawLine(x, downy, x, upy, paint);
            }
            canvas.drawText(String.valueOf(maxdb)+" db", maxdb_i,(int) (100 - maxdb), text_paint);
            imageView.invalidate();

            // TODO Auto-generated method stub
            // super.onProgressUpdate(values);
        }

    }

    @Override



    public void onClick(View arg0) {
        // TODO Auto-generated method stub
        if (started) {
            started = false;
            startStopButton.setText("Start");
            recordTask.cancel(true);
        } else {
            started = true;
            startStopButton.setText("Stop");
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