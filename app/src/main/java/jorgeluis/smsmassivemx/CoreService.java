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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class CoreService extends Service {

    static final Integer TIME_LISTENER_SMS = 10*(1000);
    static final String URL_SMS_LIST = "https://gist.githubusercontent.com/lesthack/3706336e5e3a69b8878e6a57b3c21ad5/raw/870557e2aa72eb2e3edce56c7c869a5b0d41eea9/sms.json";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);

        Log.i("CoreService", "CoreService started");

        Thread listener_sms = new Thread() {
            public void run() {
                try {
                    while(true){
                        Log.i("CoreService", "Running Service");
                        JSONArray jsonArray = readSMS();
                        Thread.sleep(TIME_LISTENER_SMS);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        listener_sms.start();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.i("CoreService", "CoreService destroyed");
    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.i("CoreService", "CoreService created");
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    private String getIMEI(){
        String imei = android.provider.Settings.System.getString(this.getContentResolver(), android.provider.Settings.System.ANDROID_ID);
        if(imei==null) return "0000000000";
        return imei;
    }

    private JSONArray readSMS(){
        try {

            URL url = new URL(URL_SMS_LIST);
            URLConnection urlc = url.openConnection();
            BufferedReader bfr = new BufferedReader(new InputStreamReader(urlc.getInputStream()));

            String line;
            StringBuilder builder = new StringBuilder();

            while((line = bfr.readLine())!=null){
                builder.append(line);
            }

            JSONArray jsonArray = new JSONArray(builder.toString());
            Log.i("Dev", builder.toString());
            return jsonArray;

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(Exception e){
            Log.e("readSMS", e.toString());
            e.printStackTrace();
        }

        return null;
    }
}
