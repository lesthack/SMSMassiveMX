package jorgeluis.smsmassivemx;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

class SettingsActivity extends PreferenceActivity {

    /*
    @Override
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        private SharedPreferences.OnSharedPreferenceChangeListener listener;
        private EditTextPreference host;
        private EditTextPreference webhook;
        private EditTextPreference time_scan_host;
        private EditTextPreference time_dispatch;
        private EditTextPreference time_sleep_dispatch;
        private EditTextPreference sms_by_dispatch;

        private DataBaseOpenHelper localdb;
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            localdb = new DataBaseOpenHelper(getActivity());

            host = (EditTextPreference) getPreferenceScreen().findPreference("host");
            webhook = (EditTextPreference) getPreferenceScreen().findPreference("webhook");
            time_scan_host = (EditTextPreference) getPreferenceScreen().findPreference("time_scan_host");
            time_dispatch = (EditTextPreference) getPreferenceScreen().findPreference("time_dispatch");
            time_sleep_dispatch = (EditTextPreference) getPreferenceScreen().findPreference("time_sleep_dispatch");
            sms_by_dispatch = (EditTextPreference) getPreferenceScreen().findPreference("sms_by_dispatch");

            host.setText(localdb.getParameter("host_ws"));
            webhook.setText(localdb.getParameter("webhook"));
            time_scan_host.setText(localdb.getParameter("time_scan_host"));
            time_dispatch.setText(localdb.getParameter("time_dispatch"));
            time_sleep_dispatch.setText(localdb.getParameter("time_sleep_dispatch"));
            sms_by_dispatch.setText(localdb.getParameter("sms_by_dispatch"));

            addEvents();
        }

        private void addEvents(){
            host.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(host.getText() != newValue.toString()){
                        localdb.setParameter("host_ws", newValue.toString());
                        host.setText(newValue.toString());
                        //validHost(host.getText(), "Host");
                    }
                    return false;
                }
            });

            webhook.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    localdb.setParameter("webhook", newValue.toString());
                    webhook.setText(newValue.toString());
                    return false;
                }
            });

            time_scan_host.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    localdb.setParameter("time_scan_host", newValue.toString());
                    time_scan_host.setText(newValue.toString());
                    return false;
                }
            });

            time_dispatch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    localdb.setParameter("time_dispatch", newValue.toString());
                    time_dispatch.setText(newValue.toString());
                    return false;
                }
            });

            time_sleep_dispatch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    localdb.setParameter("time_sleep_dispatch", newValue.toString());
                    time_sleep_dispatch.setText(newValue.toString());
                    return false;
                }
            });

            sms_by_dispatch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    localdb.setParameter("sms_by_dispatch", newValue.toString());
                    sms_by_dispatch.setText(newValue.toString());
                    return false;
                }
            });
        }

        private Boolean validHost(String host, String type){
            JSONArray json_content;
            try {
                //addLog("Validando " + type + " " + host);
                StringBuilder text_content = getContent(host);
                Log.i("Settings", host);
                json_content = new JSONArray(text_content.toString());
                addLog(type + " " + host + "correcto");
                return true;
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            addLog("El " + type + " " + host + " no es válido.");
            return false;
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

        private void addLog(String log_text){
            addLog(log_text, 0);
        }

        private void addLog(String log_text, int log_type){
            Log.i("StartUpReceiver", log_text);
            localdb.addLog(log_text, log_type);
        }
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
    }

}