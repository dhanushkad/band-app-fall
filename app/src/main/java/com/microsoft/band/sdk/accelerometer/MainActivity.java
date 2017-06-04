package com.microsoft.band.sdk.accelerometer;


import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;

import  com.microsoft.band.sensors.BandDistanceEvent;
import  com.microsoft.band.sensors.BandDistanceEventListener;

import com.microsoft.band.sensors.BandHeartRateEventListener;
//import com.microsoft.band.sensors.BandHeartRateEvent;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    private BandClient client = null;
    private Button btnStart;
    private TextView textStatus;
    private TextView textView;
    private TextView textView2;
    /**
     * The file to write the data
     * and the outputstream to use for data writing
     **/
    File accelGyroFile;
    FileOutputStream gyroFileStream;

    private  BandDistanceEventListener mDistantEventListener = new BandDistanceEventListener() {
        @Override
        public void onBandDistanceChanged(BandDistanceEvent bandDistanceEvent) {
            if(bandDistanceEvent != null){

                try{

                    appendTOTextView("Movement Status  :  "+ bandDistanceEvent.getMotionType().toString());
                }catch (Exception e){

                    System.out.println(e);
                }
            }
        }
    };
    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {

        @Override
        public void onBandHeartRateChanged( BandHeartRateEvent event) {
            if (event != null) {
                 try{

                   appendToUI(String.format("Heart Rate : %d beats per minute\n"
                             + "Quality : %s\n", event.getHeartRate(), event.getQuality()));
                 }catch (Exception e){

                     appendToUI("Event or gyroFileStream is null");
                }


            }
        }
    };

    private BandGyroscopeEventListener mGyroscopeEventListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(final BandGyroscopeEvent event) {
            if (event != null && gyroFileStream != null) {

                /*appendTOTextStatus(String.format(" X = %.3f \n Y = %.3f\n Z = %.3f \n GX = %.3f\n GY = %.3f\n GZ = %.3f"
                               , event.getAccelerationX(),
                        event.getAccelerationY(), event.getAccelerationZ(),event.getAngularVelocityX(),event.getAngularVelocityY(),event.getAngularVelocityZ()));*/


                /**
                 * This block inst very good
                 * rewrite this.
                 * is try in a finally block accepted?
                 * */
                try {

                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = new Date(event.getTimestamp());


                    String sensorDateEntry = dateFormat.format(date) + "&" +
                            String.valueOf(event.getAccelerationX()) + "&" +
                            String.valueOf(event.getAccelerationY()) + "&" +
                            String.valueOf(event.getAccelerationZ()) + "&" +
                            String.valueOf(event.getAngularVelocityX()) + "&" +
                            String.valueOf(event.getAngularVelocityY()) + "&" +
                            String.valueOf(event.getAngularVelocityZ()) + "\n";
                    appendTOTextStatus("Entry : " + sensorDateEntry);

                    //String sensorDateEntry = "PRINT \n";

                    gyroFileStream.write(sensorDateEntry.getBytes());

                } catch (IOException e) {
                    appendTOTextStatus("IOx" + e.getMessage());
                } catch (Exception ex) {
                    appendTOTextStatus("Exception in gyroFileStream write" + ex.getLocalizedMessage() + ex.toString() + ex.getMessage());
                }

            } else {
                appendTOTextStatus("Event or gyroFileStream is null");
            }
        }
    };


    /**
     * This returns a file created in the documents directory.
     */
    public File getFileCreated(String fileName) throws IOException {
        // Get the directory for the user's documents directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), fileName);
        if (!file.createNewFile()) {
            appendTOTextStatus("Error occured in file writing");
        }
        return file;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView2 = (TextView) findViewById(R.id.textView2);
        textStatus = (TextView) findViewById(R.id.textStatus);
        textView = (TextView) findViewById(R.id.textView);
        final WeakReference<Activity> reference = new WeakReference<Activity>(this);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new HeartRateConsentTask().execute(reference);
                new SubscriptionTasks().execute();

            }
        });


        try {
            accelGyroFile = getFileCreated("gyro.txt");

            appendTOTextStatus("File created in" + accelGyroFile.getPath());
            gyroFileStream = new FileOutputStream(accelGyroFile, true);

        } catch (Exception ex) {
            appendTOTextStatus("Error file creation " + ex.getMessage());
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
               client.getSensorManager().unregisterDistanceEventListener(mDistantEventListener);
                client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);

            } catch (BandIOException e) {
                appendTOTextStatus("Band Exception" + e.getMessage());
            }
        }
    }
    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {

                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                            }
                        });
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
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
    private class SubscriptionTasks extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    //("Band is connected.\n");
                    client.getSensorManager().registerGyroscopeEventListener(mGyroscopeEventListener, SampleRate.MS128);
                    client.getSensorManager().registerDistanceEventListener(mDistantEventListener);
                    client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                } else {
                    appendTOTextStatus("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
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
                appendTOTextStatus(exceptionMessage);

            } catch (Exception e) {
                appendTOTextStatus(e.getMessage());
            }
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
                gyroFileStream.close();

            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            } catch (IOException e) {

            }
        }
        super.onDestroy();
    }

    private void appendTOTextStatus(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textStatus.setText(string);
            }
        });
    }
    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView2.setText(string);
            }
        });
    }
    private void appendTOTextView(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(string);
            }
        });
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendTOTextStatus("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        appendTOTextStatus("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

}

