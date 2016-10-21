package jorgeluis.smsmassivemx;

/**
 * Created by lesthack on 16/08/16.
 */

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
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
import java.util.Random;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.telephony.SmsManager;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class CoreService extends Service {

    static final boolean WEBSERVER_ACTIVE = false;
    private final int SMS_LENGTH_MAX = 140;
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
    private boolean free = true;
    private SmsManager sm;
    private Thread listener_sms;
    private Thread listener_dispatch;
    private int id_service;

    private DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd");
    private DateFormat hour_format = new SimpleDateFormat("HH:mm");

    @Override
    public void onCreate(){
        super.onCreate();

        id_service = (new Random()).nextInt(100);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Manager SMS
        sm = SmsManager.getDefault();

        // Abriendo base de datos
        localdb = new DataBaseOpenHelper(this);

        // Valores de parametros
        HOST_WS = localdb.getParameter("host_ws");
        TIME_SCAN_HOST = Integer.parseInt(localdb.getParameter("time_scan_host"))*1000;
        TIME_DISPATCH = Integer.parseInt(localdb.getParameter("time_dispatch"))*1000;
        TIME_SLEEP_DISPATCH = (int) (Float.parseFloat(localdb.getParameter("time_sleep_dispatch"))*1000);
        SMS_BY_DISPATCH = Integer.parseInt(localdb.getParameter("sms_by_dispatch"))*1000;
        WEBHOOK = localdb.getParameter("webhook");

        addLog("CoreService created: " + id_service, 3);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);

        addLog("CoreService started: " + id_service, 3);

        if(WEBSERVER_ACTIVE){
            try {
                webserver.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        listener_dispatch = new Thread() {
            public void run() {
                while (free) {
                    try {
                        addLog("Thread Dispatch (Service " + id_service + ")");
                        String launch_date = String.format("%s %s", new String[]{date_format.format(new Date()), hour_format.format(new Date())});
                        free = false;
                        JSONArray list_sms = localdb.getSMSListUnSent(launch_date, SMS_BY_DISPATCH);
                        // Send sms
                        for(int i=0; i<list_sms.length(); i++){
                            JSONObject item = list_sms.getJSONObject(i);
                            try{
                                //sm.sendTextMessage(item.getString("phone"), null, item.getString("message"), null, null);
                                addLog("Message (id: " + item.getInt("id") + ") sent (Campaign: \"" + item.getString("campaign") + "\"): " + item.getString("message") + " -> " + item.getString("phone"), 1);
                                localdb.markSentSMS(item.getInt("id"));
                            }
                            catch(Exception e){
                                addLog("Error to try send message: " + e.getMessage());
                                localdb.markErrorSMS(item.getInt("id"));
                                JSONObject exception_parameters = new JSONObject();
                                try{
                                    exception_parameters.put("type", "error");
                                    exception_parameters.put("code", 332);
                                    exception_parameters.put("description", e.getMessage());
                                }
                                catch(Exception f){
                                    f.printStackTrace();
                                }
                                dispatch_webhook(exception_parameters);
                            }
                            Thread.sleep(TIME_SLEEP_DISPATCH);
                        }
                        Thread.sleep(TIME_DISPATCH);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    free =  true;
                }
            }
        };

        listener_sms = new Thread() {
            public void run() {
                while(free){
                    try {
                        addLog("Thread Reader (Service " + id_service + ")");
                        JSONArray json_content = readSMS();
                        JSONObject parameters = new JSONObject();
                        if(json_content != null){
                            free = false;
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
                                            if(campaign_sms.getString(j).length()>SMS_LENGTH_MAX){
                                                throw new CampaignsException(222, campaign_id, String.format("The sms \"%s\" is longer than %d characters", campaign_sms.getString(j), SMS_LENGTH_MAX));
                                            }
                                        }

                                        // Adding campaigns
                                        int sms_inserted = localdb.addCampaignSMS(campaign_id, campaign_launch_date, campaign_dest, campaign_sms, campaign_cast);
                                        if(sms_inserted>0){
                                            addLog("Adding " + sms_inserted + " SMS's of Campaign " + campaign_id);
                                            JSONObject exception_parameters = new JSONObject();
                                            try{
                                                exception_parameters.put("type", "ok");
                                                exception_parameters.put("code", 100);
                                                exception_parameters.put("description", String.format("Campaign \"%s\" added successfull and waiting for send %d SMS's.", campaign_id, sms_inserted));
                                            }
                                            catch(Exception f){
                                                f.printStackTrace();
                                            }
                                            dispatch_webhook(exception_parameters);
                                        }

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
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    free = true;
                }

            }
        };

        listener_sms.start();
        listener_dispatch.start();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        addLog("CoreService destroyed: " + id_service, 3);
        // Close database connection
        localdb.close();
        // Turn Off Webserver
        if(WEBSERVER_ACTIVE){
            webserver.stop();
        }
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

    private String getIMEI(){
        String imei = android.provider.Settings.System.getString(this.getContentResolver(), android.provider.Settings.System.ANDROID_ID);
        if(imei==null) return "0000000000";
        return imei;
    }

    public void dispatch_webhook(JSONObject parameters){
        addLog(parameters.toString(), 2);
        /*if(WEBHOOK.length()>0){}*/
    }

    private void addLog(String log_text){
        addLog(log_text, 0);
    }

    private void addLog(String log_text, int log_type){
        Log.i("CoreService", log_text);
        localdb.addLog(log_text, log_type);
    }

}