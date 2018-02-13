package jorgeluis.smsmassivemx;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import java.util.List;

public class SettingsActivity extends PreferenceActivity {
    //private CoreService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mService = getIntent().getParcelableExtra("mService");
        MyPreferenceFragment settings_fragment = new MyPreferenceFragment();
        getFragmentManager().beginTransaction().add(android.R.id.content, settings_fragment).commit();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
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
                    if(!host.getText().equals(newValue.toString())){
                        localdb.setParameter("host_ws", newValue.toString());
                        host.setText(newValue.toString());
                        localdb.addLog("Actualizando parámetro host_ws a " + host.getText());
                    }
                    return false;
                }
            });

            webhook.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(!webhook.getText().equals(newValue.toString())){
                        localdb.setParameter("webhook", newValue.toString());
                        webhook.setText(newValue.toString());
                        localdb.addLog("Actualizando parámetro webhook a " + webhook.getText());
                    }
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

    }

}