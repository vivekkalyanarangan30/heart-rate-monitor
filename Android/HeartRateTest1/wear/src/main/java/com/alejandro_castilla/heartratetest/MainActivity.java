package com.alejandro_castilla.heartratetest;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends WearableActivity implements SensorEventListener,MessageApi.MessageListener {

    private TextView mTextView;
    private ImageButton btnStart;
    private ImageButton btnPause;
    private Drawable imgStart;
    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;

    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String MOBILE_PATH = "/mobile";

    private GoogleApiClient googleApiClient;
    private String nodeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGoogleApiClient();

        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.heartRateText);
                btnStart = (ImageButton) stub.findViewById(R.id.btnStart);
                btnPause = (ImageButton) stub.findViewById(R.id.btnPause);

                btnStart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        btnStart.setVisibility(ImageButton.GONE);
                        btnPause.setVisibility(ImageButton.VISIBLE);
                        mTextView.setText("Please wait...");
                        startMeasure();
                    }
                });

                btnPause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        btnPause.setVisibility(ImageButton.GONE);
                        btnStart.setVisibility(ImageButton.VISIBLE);
                        mTextView.setText("--");
                        stopMeasure();
                    }
                });

            }
        });

        setAmbientEnabled();
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

    }

    private void initGoogleApiClient() {
        Log.d("Google Client","Inititalized");
        googleApiClient = getGoogleApiClient(this);
        retrieveDeviceNode();
    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (googleApiClient != null && !(googleApiClient.isConnected() || googleApiClient.isConnecting())) {
                    googleApiClient.blockingConnect();//CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS
                    Log.d("Retrieve Data Node","First Access");
                }

                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

                List<Node> nodes = result.getNodes();
                Log.d("TAG",nodes.toString());
                if (nodes.size() > 0)
                    nodeId = nodes.get(0).getId();

                Log.v("Heart Rate", "Node ID of phone: " + nodeId);

            }
        }).start();
    }

    private void sendToast(final String message) {
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (googleApiClient != null && !(googleApiClient.isConnected() || googleApiClient.isConnecting()))
                        googleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);

                    Wearable.MessageApi.sendMessage(googleApiClient, nodeId, message, null).await();
                    Log.d("Wear","Message Sent = "+message);
                }
            }).start();
        }else{
            Log.e("Heart Rate","Node is null");
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }




    private void startMeasure() {
        boolean sensorRegistered = mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
        Log.d("Sensor Status:", " Sensor registered: " + (sensorRegistered ? "yes" : "no"));
    }

    private void stopMeasure() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float mHeartRateFloat = event.values[0];

        int mHeartRate = Math.round(mHeartRateFloat);

        mTextView.setText(Integer.toString(mHeartRate));
        Log.d("Heart Rate: ",String.valueOf(mHeartRateFloat));

        //Publish to CEP
        try {

            JSONObject root = new JSONObject();
            JSONObject event_root = new JSONObject();

            JSONObject timestamp = new JSONObject();
            timestamp.put("timestamp",System.currentTimeMillis());

            JSONObject id = new JSONObject();
            id.put("id","123231");

            /*JSONObject lat = new JSONObject();
            lat.put("latitude",123.0);

            JSONObject longi = new JSONObject();
            longi.put("longitude",121.2);

            JSONObject heart_rate = new JSONObject();
            timestamp.put("heart_rate",mHeartRateFloat);*/

            JSONObject payload_data = new JSONObject();
            payload_data.put("latitude",123.0);
            payload_data.put("longitude",121.2);
            payload_data.put("heart_rate",mHeartRateFloat);

            event_root.put("metaData", id);
            event_root.put("correlationData", timestamp);
            event_root.put("payloadData", payload_data);

            root.put("event",event_root);

            //new CallCEP().execute(root);
            sendToast(root.toString());

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        //Print all the sensors
//        if (mHeartRateSensor == null) {
//            List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//            for (Sensor sensor1 : sensors) {
//                Log.i("Sensor list", sensor1.getName() + ": " + sensor1.getType());
//            }
//        }
//    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Stop", "onStop");
        if (googleApiClient != null)
            if (googleApiClient.isConnected()) googleApiClient.disconnect();
    }
}
