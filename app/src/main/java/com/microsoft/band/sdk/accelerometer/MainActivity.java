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
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;




public class MainActivity extends AppCompatActivity {
    private BandClient client = null;
    private Button btnStart;
    private TextView textStatus;

    private BandGyroscopeEventListener mGyroscopeEventListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(final BandGyroscopeEvent event) {
            if (event != null) {
                appendToUI(String.format(" X = %.3f \n Y = %.3f\n Z = %.3f \n GX = %.3f\n GY = %.3f\n GZ = %.3f"
                               , event.getAccelerationX(),
                        event.getAccelerationY(), event.getAccelerationZ(),event.getAngularVelocityX(),event.getAngularVelocityY(),event.getAngularVelocityZ()));
            }
        }
    };



        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            textStatus = (TextView) findViewById(R.id.textStatus);
            btnStart = (Button) findViewById(R.id.btnStart);
            btnStart.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    textStatus.setText("");
                    new GyroscopeSubscriptionTask().execute();
                }
            });
        }

        @Override
        protected void onResume() {
            super.onResume();
            textStatus.setText("");
        }

        @Override
        protected void onPause() {
            super.onPause();
            if (client != null) {
                try {
                    client.getSensorManager().unregisterGyroscopeEventListener(mGyroscopeEventListener);
                } catch (BandIOException e) {
                    appendToUI(e.getMessage());
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
                } catch (InterruptedException e) {
                    // Do nothing as this is happening during destroy
                } catch (BandException e) {
                    // Do nothing as this is happening during destroy
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

