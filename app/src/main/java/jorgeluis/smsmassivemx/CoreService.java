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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    static final boolean WEBSERVER_ACTIVE = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String HOST_WS;
    private String WEBHOOK;
    private Integer TIME_SCAN_HOST;
    private Integer TIME_DISPATCH;
    private Integer TIME_SLEEP_DISPATCH;
    private Integer SMS_BY_DISPATCH;
    private DataBaseOpenHelper localdb;
    private HttpService webserver = new HttpService(8080);
    private int id_service;

    private DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd");
    private DateFormat hour_format = new SimpleDateFormat("HH:mm");

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
        HOST_WS = localdb.getParameter("host_ws");
        TIME_SCAN_HOST = Integer.parseInt(localdb.getParameter("time_scan_host"))*1000;
        TIME_DISPATCH = Integer.parseInt(localdb.getParameter("time_dispatch"))*1000;
        TIME_SLEEP_DISPATCH = (int) (Float.parseFloat(localdb.getParameter("time_sleep_dispatch"))*1000);
        SMS_BY_DISPATCH = Integer.parseInt(localdb.getParameter("sms_by_dispatch"))*1000;
        WEBHOOK = localdb.getParameter("webhook");
    }

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
        Thread listener_dispatch = new Thread() {
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(TIME_DISPATCH);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread listener_sms = new Thread() {
            public void run() {
                try {
                    while(true){
                        Log.i("CoreService", "Running Service: " + id_service);
                        JSONArray json_content = readSMS();
                        JSONObject parameters = new JSONObject();
                        if(json_content != null){
                            for (int i = 0; i < json_content.length(); i++) {
                                try {
                                    JSONObject campaign = json_content.getJSONObject(i);
                                    if(campaign.has("id")){
                                        String campaign_id = campaign.getString("id");
                                        Boolean campaign_cast = campaign.has("cast")?campaign.getBoolean("cast"):false;
                                        String campaign_date = campaign.has("date")?campaign.getString("date"):date_format.format(new Date());
                                        String campaign_hour = campaign.has("hour")?campaign.getString("hour"):hour_format.format(new Date());
                                        String campaign_launch_date = String.format("%s %s", new String[]{ campaign_date, campaign_hour});
                                        JSONArray campaign_sms = campaign.getJSONArray("sms");
                                        JSONArray campaign_dest = campaign.getJSONArray("dest");

                                        //Check 160 digits
                                        for(int j = 0; j<campaign_sms.length(); j++){
                                            if(campaign_sms.getString(j).length()>160){
                                                throw new CampaignsException(222, campaign_id, String.format("the sms %s is longer than 160 characters", new String[]{campaign_sms.getString(j)}));
                                            }
                                        }

                                        // Adding campaigns
                                        localdb.addCampaignSMS(campaign_id, campaign_launch_date, campaign_dest, campaign_sms, campaign_cast);

                                        //dispatch_webhook(parameters);
                                    }
                                    else{
                                        throw new CampaignsException(221, "Id Campaign not found");
                                    }
                                } catch (CampaignsException e) {
                                    dispatch_webhook(e.getParameters());
                                } catch (JSONException e) {
                                    JSONObject exception_parameters = new JSONObject();
                                    try{
                                        exception_parameters.put("type", "error");
                                        exception_parameters.put("code", 331);
                                        exception_parameters.put("description", e.getMessage());
                                    }
                                    catch(Exception f){
                                        f.printStackTrace();
                                    }
                                    dispatch_webhook(exception_parameters);

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

        //listener_sms.start();
        listener_dispatch.start();
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
            StringBuilder text_content = getContent(HOST_WS);
            json_content = new JSONArray(text_content.toString());
            return json_content;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public void dispatch_webhook(JSONObject parameters){
        if(WEBHOOK.length()>0){
            Log.i("CoreService", parameters.toString());
        }
    }
}