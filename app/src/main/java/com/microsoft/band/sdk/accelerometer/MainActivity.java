package com.microsoft.band.sdk.accelerometer;


import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.SampleRate;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;


import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    private BandClient client = null;
    private Button btnStart;
    private TextView textStatus;

    /**
     * The file to write the data
     * and the outputstream to use for data writing
     * **/
    File accelGyroFile;
    FileOutputStream stream;


    private BandGyroscopeEventListener mGyroscopeEventListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(final BandGyroscopeEvent event) {
            if (event != null) {

                /*appendToUI(String.format(" X = %.3f \n Y = %.3f\n Z = %.3f \n GX = %.3f\n GY = %.3f\n GZ = %.3f"
                               , event.getAccelerationX(),
                        event.getAccelerationY(), event.getAccelerationZ(),event.getAngularVelocityX(),event.getAngularVelocityY(),event.getAngularVelocityZ()));*/



                /**
                 * This block inst very good
                 * rewrite this.
                 * is try in a finally block accepted?
                 * */
                try {

                    if(stream == null){

                        appendToUI("stream is null");
                    }
                    if(accelGyroFile == null){

                        appendToUI("file is null");
                    }
                    //DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    //Date date = new Date(event.getTimestamp());
                    //dateFormat.format(date);

                    String sensorDateEntry = event.getTimestamp() + "&" +
                            String.valueOf(event.getAccelerationX()) + "&" +
                            String.valueOf(event.getAccelerationY()) + "&" +
                            String.valueOf(event.getAccelerationZ()) + "&" +
                            String.valueOf(event.getAngularVelocityX()) + "&" +
                            String.valueOf(event.getAngularVelocityY()) + "&" +
                            String.valueOf(event.getAngularVelocityZ()) + "\n";
                            appendToUI("Entry : "+sensorDateEntry);

                    //String sensorDateEntry = "PRINT \n";
                    stream = new FileOutputStream(accelGyroFile);
                    stream.write(sensorDateEntry.getBytes());
                    stream.close();
                } catch (IOException e) {
                   appendToUI("IOx"+e.getMessage());
                } catch (Exception ex)
                {
                    appendToUI("Exception in stream write" + ex.getLocalizedMessage()+ex.toString() + ex.getMessage());
                }

            }
        }
    };

    /**
     * This returns a file created in the documents directory.
     *
     * */
    public File getFileCreated(String fileName) throws IOException {
        // Get the directory for the user's documents directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), fileName);
        if (!file.createNewFile()) {
            appendToUI("Error occured in file writing");
        }
        return file;
    }



        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            textStatus = (TextView) findViewById(R.id.textStatus);
            btnStart = (Button) findViewById(R.id.btnStart);
            btnStart.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    new GyroscopeSubscriptionTask().execute();
                }
            });


            try {
                accelGyroFile = getFileCreated("accelerometer.txt");
                appendToUI("File created in" + accelGyroFile.getPath());

            }catch (Exception ex){
                appendToUI("Error file creation " + ex.getMessage());
            }
        }

        @Override
        protected void onResume() {
            super.onResume();

        }

        @Override
        protected void onPause() {
            super.onPause();
            if (client != null) {
                try {
                    client.getSensorManager().unregisterGyroscopeEventListener(mGyroscopeEventListener);
                } catch (BandIOException e) {
                    appendToUI("Band Exception"+e.getMessage());
                }
            }
        }

        private class GyroscopeSubscriptionTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    if (getConnectedBandClient()) {
                        appendToUI("Band is connected.\n");
                        client.getSensorManager().registerGyroscopeEventListener(mGyroscopeEventListener,SampleRate.MS128);
                    } else {
                        appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                    }
                } catch (BandException e) {
                    String exceptionMessage = "";
                    switch (e.getErrorType()) {
                        case UNSUPPORTED_SDK_VERSION_ERROR:
                            exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                            break;
                        case SERVICE_ERROR:
                            exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                            break;
                        default:
                            exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                            break;
                    }
                    appendToUI(exceptionMessage);

                } catch (Exception e) {
                    appendToUI(e.getMessage());
                }
                return null;
            }
        }

    @Override
        protected void onDestroy() {
            if (client != null) {
                try {
                    client.disconnect().await();
                    stream.close();
                    
                } catch (InterruptedException e) {
                    // Do nothing as this is happening during destroy
                } catch (BandException e) {
                    // Do nothing as this is happening during destroy
                } catch (IOException e) {

                }
            }
            super.onDestroy();
        }

        private void appendToUI(final String string) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textStatus.setText(string);
                }
            });
        }

        private boolean getConnectedBandClient() throws InterruptedException, BandException {
            if (client == null) {
                BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
                if (devices.length == 0) {
                    appendToUI("Band isn't paired with your phone.\n");
                    return false;
                }
                client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
            } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
                return true;
            }

            appendToUI("Band is connecting...\n");
            return ConnectionState.CONNECTED == client.connect().await();
        }

}

