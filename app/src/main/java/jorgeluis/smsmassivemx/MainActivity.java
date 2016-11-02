package jorgeluis.smsmassivemx;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DataBaseOpenHelper localdb;

    ListView listview_status;
    ItemStatusAdapter mItemStatusAdapter;
    updateLogAsyncTask mLogAsyncTask = new updateLogAsyncTask();

    private Intent service_intent;
    private Thread listener_log;
    private boolean is_active = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        localdb = new DataBaseOpenHelper(this);

        boolean is_services_started = false;
        boolean is_devices_rooted = isRootedDevice();

        // Starting service
        if(!isThisServiceRunning(CoreService.class)) {
            service_intent = new Intent(this, CoreService.class);
            startService(service_intent);
            is_services_started = true;
        }
        else{
            is_services_started = true;
            Log.i("MainActivity", "CoreServices was active.");
        }

        //Asignaciones
        listview_status = (ListView) findViewById(R.id.list_status);
        List<ItemStatus> items_status = new ArrayList<ItemStatus>();
            items_status.add(new ItemStatus("Servicio", is_services_started));
            items_status.add(new ItemStatus("Dispositivo rooteado", is_devices_rooted));
            //items_status.add(new ItemStatus("Limite de SMS desactivado", false));
        mItemStatusAdapter = new ItemStatusAdapter(getBaseContext(), items_status);
        listview_status.setAdapter(mItemStatusAdapter);

        startAsyncTaskLog();
    }

    private void startAsyncTaskLog(){
        is_active = true;
        Log.i("Main", "Inicio del Thread Log");
        mLogAsyncTask.execute();
    }

    @Override
    public void onBackPressed() {
        is_active = false;
        Log.i("Main", "Fin del Thread log");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
        } else if (id == R.id.nav_about) {
            Intent i = new Intent(MainActivity.this, AboutActivity.class);
            MainActivity.this.startActivity(i);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /*
    * http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
    */
    private boolean isThisServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isRootedDevice(){
        RootUtil root_util = new RootUtil();
        return root_util.isDeviceRooted();
    }

    // Vamonos por el camino de crear un Async
    private class updateLogAsyncTask extends AsyncTask<Void, Integer, Void> {

        ListView back_listview_log;
        List<ItemLog> back_items_log;
        ItemLogAdapter back_ItemLogAdapter;
        int back_max_log_id = -1;

        protected void onPreExecute(){
            back_listview_log = (ListView) findViewById(R.id.list_log);
            back_listview_log.setClickable(false);
            back_items_log = new ArrayList<ItemLog>();

            List list_logs = localdb.getLogs(50);
            for(int i=0; i<list_logs.size(); i++){
                String[] log = (String[]) list_logs.get(i);
                int log_id = Integer.valueOf(log[0]);
                int log_type = Integer.valueOf(log[3]);
                if(i==0){
                    back_max_log_id = log_id;
                }
                back_items_log.add(new ItemLog(log_id, log[1], log[2], log_type));
            }

            back_ItemLogAdapter = new ItemLogAdapter(getBaseContext(), back_items_log);
            back_listview_log.setAdapter(back_ItemLogAdapter);
        }

        protected Void doInBackground(Void... params) {
            while(is_active) {
                try {
                    List list_logs = localdb.getLogsAfterMax(back_max_log_id);
                    for (int i = 0; i < list_logs.size(); i++) {
                        String[] log = (String[]) list_logs.get(i);
                        int log_id = Integer.valueOf(log[0]);
                        int log_type = Integer.valueOf(log[3]);
                        back_max_log_id = log_id;
                        back_items_log.add(0, new ItemLog(log_id, log[1], log[2], log_type));
                    }
                    if(list_logs.size()>0){
                        publishProgress(0);
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onPostExecute(Void... voids){
            Log.i("Main","Me fui");
        }

        protected void onProgressUpdate(Integer... progress) {
            back_ItemLogAdapter.notifyDataSetChanged();
            back_listview_log.requestLayout();
        }

    }

    @Override
    public void onPause(){
        super.onPause();
        Log.i("Main", "Pause");
        //mLogAsyncTask.cancel(true);
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.i("Main", "Resume: " + is_active);
        //startAsyncTaskLog();
    }

}