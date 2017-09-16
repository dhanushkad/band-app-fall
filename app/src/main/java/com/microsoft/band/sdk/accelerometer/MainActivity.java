package com.microsoft.band.sdk.accelerometer;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandContactEvent;
import com.microsoft.band.sensors.BandContactEventListener;
import com.microsoft.band.sensors.BandContactState;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;

import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;

import com.microsoft.band.sensors.BandHeartRateEventListener;

import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;

import com.microsoft.band.sensors.BandUVEvent;
import com.microsoft.band.sensors.BandUVEventListener;


import android.app.Activity;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    FileOutputStream gyroFileStream;
    LoadingCache<String, String> heartBeatCache;
    LoadingCache<String, String> motionTypeCache;
    boolean acceleroMeterLowerThresholdReached = false;
    long accelorMeterLowerThresholdMetTimestamp;
    BlockingQueue<AccelorometerAggregatedEvent> accelerometerEventList = new LinkedBlockingQueue<>();
    long fallTimeMilliseconds = 3000;
    BandContactState lastKnownBandContactState;
    String mostCommonMotionType;
    private BandClient client = null;
    private Button btnStart;
    private TextView textStatus;
    private TextView textFall;
    private TextView otherSensors;
    private BandUVEventListener UVEventListener = new BandUVEventListener() {
        @Override
        public void onBandUVChanged(BandUVEvent bandUVEvent) {
            if (bandUVEvent != null) {
                try {
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    };

    private BandSkinTemperatureEventListener skinTemperatureEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent bandSkinTemperatureEvent) {
            if (bandSkinTemperatureEvent != null) {
                try {
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    };

    private BandDistanceEventListener mDistantEventListener = new BandDistanceEventListener() {
        @Override
        public void onBandDistanceChanged(BandDistanceEvent bandDistanceEvent) {
            if (bandDistanceEvent != null) {

                try {
                    motionTypeCache.put(Long.toString(bandDistanceEvent.getTimestamp()), bandDistanceEvent.getMotionType().toString());
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    };  // Movement Status receiving

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {

        @Override
        public void onBandHeartRateChanged(BandHeartRateEvent event) {
            if (event != null) {
                try {
                    heartBeatCache.put(Long.toString(event.getTimestamp()), Long.toString(event.getHeartRate()));
                } catch (Exception e) {

                }
            }
        }
    }; // Heart rate receiving

    private BandContactEventListener mContactEventListener = new BandContactEventListener() {
        @Override
        public void onBandContactChanged(BandContactEvent bandContactEvent) {
            lastKnownBandContactState = bandContactEvent.getContactState();
        }
    };


    private BandGyroscopeEventListener mGyroscopeEventListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(final BandGyroscopeEvent event) {
            if (event != null) {
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

                    double squaredAcceleration = (event.getAccelerationX() * event.getAccelerationX()) + (event.getAccelerationY() * event.getAccelerationY()) + (event.getAccelerationZ() * event.getAccelerationZ());
                    double Resultantacceleration = Math.sqrt(squaredAcceleration);
                    double getResultantangularvelocity = (event.getAngularVelocityX() * event.getAngularVelocityX()) + (event.getAngularVelocityY() * event.getAngularVelocityX()) + (event.getAngularVelocityZ() * event.getAngularVelocityZ());
                    double Resultantangularvelocity = Math.sqrt(getResultantangularvelocity);

                    if (!acceleroMeterLowerThresholdReached && Resultantacceleration < 0.5) {
                        appendTOTextViewFall("Lower threshold peak met. Waiting for upper threshold");

                        /**
                         * This is when the lower threshold is met.
                         * Now we should check if within a certain time period , the higher one is met.
                         * For this we are gonna save the
                         * **/
                        acceleroMeterLowerThresholdReached = true;
                        accelorMeterLowerThresholdMetTimestamp = event.getTimestamp();

                        new java.util.Timer().schedule(
                                new java.util.TimerTask() {
                                    @Override

                                    public void run() {
                                        try {
                                            appendTOTextViewFall("Timed analysis starts.");
                                            for (AccelorometerAggregatedEvent a : accelerometerEventList
                                                    ) {
                                                if (a.resultantAcceleration > 2) {
                                                    //THIS IS A TWO PEAK FALL
                                                    //EVALUATE OTHER STUFF HERE
                                                    appendTOTextViewFall("Upper threshold had met. Evaluating other sensors");
                                                    client.getSensorManager().unregisterGyroscopeEventListener(mGyroscopeEventListener);
                                                    if (lastKnownBandContactState == BandContactState.WORN) {
                                                        appendTOTextViewFall("Upper threshold had met. User is wearing the band");
                                                        client.getSensorManager().unregisterDistanceEventListener(mDistantEventListener);
                                                        ArrayList<String> motionTypesList = new ArrayList<String>(motionTypeCache.asMap().values());

                                                        mostCommonMotionType = mostCommon(motionTypesList);
                                                        appendTOTextViewFall("Two peak fall event positive. User was mostly " + mostCommonMotionType + " recently");
                                                        if (mostCommonMotionType.toLowerCase().equals("idle")) {
                                                            //Probable fall after heart problem while stationary
                                                            //Check heart rate
                                                            appendTOTextViewOtherSensors("Probable collapse while being idle. Evaluating recent heart rates.");
                                                            client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
                                                            if (largerHeartRateDetected()) {
                                                                //A fall and a heart problem detected while idle
                                                                appendTOTextViewOtherSensors("Probable collapse while idle, due to a heart problem.");

                                                            }
                                                        } else {
                                                            //fall when moving. A trip and fall
                                                            appendTOTextViewOtherSensors("Probable collapse while moving");
                                                        }
                                                    }

                                                }
                                            }
                                            accelerometerEventList.clear();
                                            client.getSensorManager().registerGyroscopeEventListener(mGyroscopeEventListener, SampleRate.MS128);
                                            client.getSensorManager().registerDistanceEventListener(mDistantEventListener);
                                            client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                                        } catch (Exception e) {
                                        }

                                    }
                                },
                                fallTimeMilliseconds
                        );

                    } else if (acceleroMeterLowerThresholdReached && event.getTimestamp() > accelorMeterLowerThresholdMetTimestamp + fallTimeMilliseconds + 5000) {
                        // appendTOTextViewFall("Out of the time range");
                        acceleroMeterLowerThresholdReached = false;
                        accelerometerEventList.clear();
                    } else if (acceleroMeterLowerThresholdReached && event.getTimestamp() <= accelorMeterLowerThresholdMetTimestamp + fallTimeMilliseconds) {
                        AccelorometerAggregatedEvent ev = new AccelorometerAggregatedEvent();
                        ev.resultantAcceleration = Resultantacceleration;
                        ev.timestamp = event.getTimestamp();


                        accelerometerEventList.put(ev);
                    }


                    //String sensorDateEntry = "PRINT \n";
                    //  gyroFileStream.write(sensorDateEntry.getBytes());

                    //  } catch (IOException e) {
                    //    appendTOTextStatus("IOx" + e.getMessage());
                } catch (Exception ex) {
                    //appendTOTextStatus("Exception in gyroFileStream write" + ex.getLocalizedMessage() + ex.toString() + ex.getMessage());
                    appendTOTextViewOtherSensors(ex.getMessage());
                }

            } else {

            }
        }
    };


    /**
     * This returns a file created in the documents directory.
     */
   /* public File getFileCreated(String fileName) throws IOException {
        // Get the directory for the user's documents directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), fileName);
        if (!file.createNewFile()) {
            appendTOTextStatus("Error occured in file writing");
        }
        return file;
    }*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = (TextView) findViewById(R.id.textStatus);
        textFall = (TextView) findViewById(R.id.textView);
        otherSensors = (TextView) findViewById(R.id.textView2);
        final WeakReference<Activity> reference = new WeakReference<Activity>(this);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new HeartRateConsentTask().execute(reference);
                new SubscriptionTasks().execute();

            }
        });

        heartBeatCache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build(
                        new CacheLoader<String, String>() {
                            @Override
                            public String load(String key) throws Exception {
                                return null;
                            }
                        }
                );

        motionTypeCache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build(
                        new CacheLoader<String, String>() {
                            @Override
                            public String load(String key) throws Exception {
                                return null;
                            }
                        }
                );
       /* try {
            accelGyroFile = getFileCreated("gyro.txt");

            appendTOTextStatus("File created in" + accelGyroFile.getPath());
            gyroFileStream = new FileOutputStream(accelGyroFile, true);

        } catch (Exception ex) {
            appendTOTextStatus("Error file creation " + ex.getMessage());
        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private String mostCommon(ArrayList<String> array) {

        Map<String, Integer> map = new HashMap<String, Integer>();

        for (int i = 0; i < array.size(); i++) {
            if (map.get(array.get(i)) == null) {
                map.put(array.get(i), 1);
            } else {
                map.put(array.get(i), map.get(array.get(i)) + 1);
            }
        }
        int largest = 0;
        String stringOfLargest = "Unknown";
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            if (value > largest) {
                largest = value;
                stringOfLargest = key;
            }
        }
        return stringOfLargest;
    }

    private boolean largerHeartRateDetected() {
        try {
            long heartratetotal = 0;

            for (String heartRate : heartBeatCache.asMap().values()
                    ) {
                heartratetotal += Long.parseLong(heartRate);

            }

            long avgHeart = heartratetotal / heartBeatCache.size();

            for (String heartRate : heartBeatCache.asMap().values()
                    ) {
                if (Long.parseLong(heartRate) >= (avgHeart * 1.5)) {
                    return true;
                }

            }
            return false;
        } catch (Exception e) {

            return false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (client != null) {
            try {
                client.getSensorManager().unregisterGyroscopeEventListener(mGyroscopeEventListener);
                client.getSensorManager().unregisterDistanceEventListener(mDistantEventListener);
                client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
                client.getSensorManager().unregisterSkinTemperatureEventListener(skinTemperatureEventListener);
                client.getSensorManager().unregisterUVEventListener(UVEventListener);
                client.getSensorManager().unregisterContactEventListener(mContactEventListener);

            } catch (BandIOException e) {
                appendTOTextStatus("Band Exception" + e.getMessage());
            }
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

    private void appendTOTextViewFall(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textFall.setText(string);
            }
        });
    }

    private void appendTOTextViewOtherSensors(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                otherSensors.setText(string);
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

    private class SubscriptionTasks extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    //("Band is connected.\n");
                    client.getSensorManager().registerGyroscopeEventListener(mGyroscopeEventListener, SampleRate.MS128);
                    client.getSensorManager().registerDistanceEventListener(mDistantEventListener);
                    client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                    client.getSensorManager().registerSkinTemperatureEventListener(skinTemperatureEventListener);
                    client.getSensorManager().registerUVEventListener(UVEventListener);
                    client.getSensorManager().registerContactEventListener(mContactEventListener);


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

}

