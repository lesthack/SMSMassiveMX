package jorgeluis.smsmassivemx;

/**
 * Created by lesthack on 16/08/16.
 */

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import 	java.util.Random;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class CoreService extends Service {

    static final String URL_SMS_LIST = "https://gist.githubusercontent.com/lesthack/3706336e5e3a69b8878e6a57b3c21ad5/raw/3813960214316f140cee1a3b9c1b504490da8769/sms.json";
    static final boolean WEBSERVER_ACTIVE = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Integer TIME_SCAN_HOST;
    private Integer TIME_DISPATCH;
    private Integer TIME_SLEEP_DISPATCH;
    private Integer SMS_BY_DISPATCH;
    private DataBaseOpenHelper localdb;
    private HttpService webserver = new HttpService(8080);
    private int id_service;

    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);

        Log.i("CoreService", "CoreService started: " + id_service);

        if(WEBSERVER_ACTIVE){
            try {
                webserver.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Thread listener_sms = new Thread() {
            public void run() {
                try {
                    while(true){
                        Log.i("CoreService", "Running Service: " + id_service);
                        JSONArray json_content = readSMS();
                        if(json_content != null){
                            for (int i = 0; i < json_content.length(); i++) {
                                try {
                                    JSONObject campaign = json_content.getJSONObject(i);
                                    Log.i("Dev", campaign.get("sms").toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        Thread.sleep(TIME_SCAN_HOST);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        listener_sms.start();
    }

    @Override
    public void onCreate(){
        super.onCreate();

        id_service = (new Random()).nextInt(100);
        Log.i("CoreService", "CoreService created: " + id_service);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Abriendo base de datos
        localdb = new DataBaseOpenHelper(this);

        // Valores de parametros
        TIME_SCAN_HOST = Integer.parseInt(localdb.getParameter("time_scan_host"))*1000;
        TIME_DISPATCH = Integer.parseInt(localdb.getParameter("time_dispatch"))*1000;
        TIME_SLEEP_DISPATCH = (int) (Float.parseFloat(localdb.getParameter("time_sleep_dispatch"))*1000);
        SMS_BY_DISPATCH = Integer.parseInt(localdb.getParameter("sms_by_dispatch"))*1000;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.i("CoreService", "CoreService destroyed: " + id_service);
        // Close database connection
        localdb.close();
        // Turn Off Webserver
        if(WEBSERVER_ACTIVE){
            webserver.stop();
        }
    }

    private String getIMEI(){
        String imei = android.provider.Settings.System.getString(this.getContentResolver(), android.provider.Settings.System.ANDROID_ID);
        if(imei==null) return "0000000000";
        return imei;
    }

    private StringBuilder getContent(String URI){
        String line;
        StringBuilder builder = new StringBuilder();
        try {
            URL url = new URL(URI);
            URLConnection urlc = url.openConnection();
            BufferedReader bfr = new BufferedReader(new InputStreamReader(urlc.getInputStream()));

            while((line = bfr.readLine())!=null){
                builder.append(line);
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(Exception e){
            Log.e("CoreService", e.toString());
            e.printStackTrace();
        }

        return builder;
    }

    private JSONArray readSMS(){
        JSONArray json_content;
        try {
            StringBuilder text_content = getContent(URL_SMS_LIST);
            json_content = new JSONArray(text_content.toString());
            return json_content;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }
}
