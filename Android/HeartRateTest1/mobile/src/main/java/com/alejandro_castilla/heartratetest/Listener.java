package com.alejandro_castilla.heartratetest;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by VK046010 on 1/22/2017.
 */

public class Listener extends WearableListenerService {
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private static GoogleApiClient googleApiClient;

    private static final long CONNECTION_TIME_OUT_MS = 10000;
    private static final String WEAR_PATH = "/wear";
    private String nodeId;


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        nodeId = messageEvent.getSourceNodeId();
        Log.v(LOG_TAG, "Node ID of watch: " + nodeId);
        String message = messageEvent.getPath();
        //Log.d("MESSAGE:",message);
        new CallCEP().execute(message);

    }

    public class CallCEP extends AsyncTask<String, Integer, Long> {
        @Override
        protected Long doInBackground(String... params) {
            Log.d("Payload",params[0].toString());

            try {
                URL url = new URL("http://192.168.0.103:9763/endpoints/HeartRateReceiver");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                //conn.setRequestProperty("Authorization","Basic YWRtaW46YWRtaW4=");
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(params[0]);
                writer.flush();
                writer.close();
                os.close();
                //conn.connect();
                int responseCode=conn.getResponseCode();
                Log.d("Response: ",String.valueOf(responseCode));
            } catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }
}
