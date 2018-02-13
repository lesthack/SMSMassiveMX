package jorgeluis.smsmassivemx;

/**
 * Created by lesthack on 16/08/16.
 */

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.LocalServerSocket;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.telephony.SmsManager;
import android.util.Log;
import android.util.Pair;

import javax.net.ssl.HttpsURLConnection;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class CoreService extends Service {

    private final IBinder mBinder = new LocalBinder();
    static final boolean WEBSERVER_ACTIVE = false;
    private final int SMS_LENGTH_MAX = 700;
    private HttpService webserver = new HttpService(8080);

    private String HOST_WS;
    private String WEBHOOK;
    private Integer TIME_SCAN_HOST;
    private Integer TIME_DISPATCH;
    private Integer TIME_SLEEP_DISPATCH;
    private Integer SMS_BY_DISPATCH;
    private DataBaseOpenHelper localdb;
    private SmsManager sm;
    private Thread listener_sms;
    private Thread listener_dispatch;
    private boolean free = true;
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
        updParams();
    }

    public void updParams(){
        // Valores de parametros
        HOST_WS = localdb.getParameter("host_ws");
        TIME_SCAN_HOST = Integer.parseInt(localdb.getParameter("time_scan_host"))*1000;
        TIME_DISPATCH = Integer.parseInt(localdb.getParameter("time_dispatch"))*1000;
        TIME_SLEEP_DISPATCH = (int) (Float.parseFloat(localdb.getParameter("time_sleep_dispatch"))*1000);
        SMS_BY_DISPATCH = Integer.parseInt(localdb.getParameter("sms_by_dispatch"))*1000;
        WEBHOOK = localdb.getParameter("webhook");
        addLog("Parámetros Actualizados");
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
                        JSONArray list_sms_sent = new JSONArray();
                        JSONArray list_sms_nosent = new JSONArray();
                        // Send sms
                        for(int i=0; i<list_sms.length(); i++){
                            JSONObject item = list_sms.getJSONObject(i);
                            JSONObject item_json = new JSONObject();
                                item_json.put("campaign_id", item.get("campaign"));
                                item_json.put("phone", item.get("phone"));
                                item_json.put("date", localdb.getDateTime());
                            try{
                                // Primera version hasta 160
                                //sm.sendTextMessage(item.getString("phone"), null, item.getString("message"), null, null);

                                // Segunda version mas de 160, pero divide
                                ArrayList<String> arrSMS = sm.divideMessage(item.getString("message"));
                                /*ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
                                ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();

                                Intent mSentIntent = new Intent();
                                Intent mDeliveryIntent = new Intent();

                                for(int j=0; j<arrSMS.size(); j++){
                                    sentIntents.add(PendingIntent.getBroadcast(getBaseContext(), 0, mSentIntent, 0));
                                    deliveryIntents.add(PendingIntent.getBroadcast(getBaseContext(), 0, mDeliveryIntent, 0));
                                }

                                sm.sendMultipartTextMessage(item.getString("phone"), null, arrSMS, sentIntents, deliveryIntents);
                                */
                                sm.sendMultipartTextMessage(item.getString("phone"), null, arrSMS, null, null);

                                addLog("Mensaje (id: " + item.getInt("id") + ") enviado (Campaña: \"" + item.getString("campaign") + "\"): " + item.getString("message") + " -> " + item.getString("phone"), 1);
                                localdb.markSentSMS(item.getInt("id"));
                                list_sms_sent.put(item_json);
                            }
                            catch(Exception e){
                                addLog("Error al enviar el mensaje: " + e.getMessage());
                                localdb.markErrorSMS(item.getInt("id"));
                                item_json.put("error", e.getMessage());
                                list_sms_nosent.put(item_json);
                            }
                            Thread.sleep(TIME_SLEEP_DISPATCH);
                        }
                        if(list_sms_nosent.length()>0){
                            JSONObject payload = new JSONObject();
                            try{
                                payload.put("type", "error");
                                payload.put("code", 202);
                                payload.put("description", "SMS's no enviados");
                                payload.put("list", list_sms_nosent);
                            }
                            catch(Exception f){
                                f.printStackTrace();
                            }
                            dispatch_webhook(payload);
                        }
                        if(list_sms_sent.length()>0){
                            JSONObject payload = new JSONObject();
                            try{
                                payload.put("type", "ok");
                                payload.put("code", 101);
                                payload.put("description", "SMS's enviados satisfactoriamente.");
                                payload.put("list", list_sms_sent);
                            }
                            catch(Exception f){
                                f.printStackTrace();
                            }
                            dispatch_webhook(payload);
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
                                        /*
                                        for(int j = 0; j<campaign_sms.length(); j++){
                                            if(campaign_sms.getString(j).length()>SMS_LENGTH_MAX){
                                                throw new CampaignsException(201, campaign_id, String.format("El mensaje \"%s\" excede los %d caracteres.", campaign_sms.getString(j), SMS_LENGTH_MAX));
                                            }
                                        }
                                        */

                                        // Adding campaigns
                                        int sms_inserted = localdb.addCampaignSMS(campaign_id, campaign_launch_date, campaign_dest, campaign_sms, campaign_cast);
                                        if(sms_inserted>0){
                                            addLog("Agregando " + sms_inserted + " SMS's de la Campaña " + campaign_id);
                                        }
                                    }
                                    else{
                                        throw new CampaignsException(200, "Campaña sin ID");
                                    }
                                } catch (CampaignsException e) {
                                    dispatch_webhook(e.getParameters());
                                } catch (JSONException e) {
                                    JSONObject exception_parameters = new JSONObject();
                                    try{
                                        exception_parameters.put("type", "error");
                                        exception_parameters.put("code", 300);
                                        exception_parameters.put("description", e.getMessage());
                                    }
                                    catch(Exception f){
                                        f.printStackTrace();
                                    }
                                    dispatch_webhook(exception_parameters);
                                }
                            }
                        }
                        localdb.cleanLog();
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

            while ((line = bfr.readLine()) != null) {
                builder.append(line);
            }
        }
        catch (FileNotFoundException e){
            addLog("Ruta " + URI + " no encontrada.", 2);
            e.printStackTrace();
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

    private String getQuery(List<AbstractMap.SimpleEntry> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (AbstractMap.SimpleEntry pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getKey().toString(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue().toString(), "UTF-8"));
        }

        return result.toString();
    }

    public void dispatch_webhook(JSONObject payload) {
        // Execute AsyncTask for send payload to webhook
        JSONArray payloads = new JSONArray();
        payloads.put(payload);
        if(WEBHOOK.length() > 0){
            try {
                URL url = new URL(WEBHOOK);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                List<AbstractMap.SimpleEntry> params = new ArrayList<AbstractMap.SimpleEntry>();
                params.add(new AbstractMap.SimpleEntry("payload", payloads.toString()));

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getQuery(params));
                writer.flush();
                writer.close();
                os.close();

                String response = "";
                if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                } else {
                    response = "";
                }
                Log.i("CoreService", "Reponse: " + response);
            } catch (FileNotFoundException e){
                addLog("Ruta " + WEBHOOK + " no encontrada.", 2);
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("Core", e.getMessage());
            }
        }
        addLog(payload.toString(), 2);
    }

    private void addLog(String log_text){
        addLog(log_text, 0);
    }

    private void addLog(String log_text, int log_type){
        Log.i("CoreService", log_text);
        localdb.addLog(log_text, log_type);
    }

    public class LocalBinder extends Binder{
        CoreService getService(){
            return CoreService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void testPayLoad(){
        JSONObject payload = new JSONObject();
        try{
            payload.put("type", "ok");
            payload.put("code", 101);
            payload.put("description", "Prueba de envio desde la app SMS Massive MX.");
        }
        catch(Exception f){
            f.printStackTrace();
        }
        dispatch_webhook(payload);
    }
}