package jorgeluis.smsmassivemx;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    private DataBaseOpenHelper localdb;
    private ListView list_log;
    private ListView list_status;
    private ItemStatusAdapter mItemStatusAdapter;
    private ItemLogAdapter mItemLogAdapter;

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
            startService(new Intent(this, CoreService.class));
            is_services_started = true;
        }
        else{
            is_services_started = true;
            Log.i("MainActivity", "CoreServices was active.");
        }

        //Asignaciones
        list_status = (ListView) findViewById(R.id.list_status);
        List<ItemStatus> items_status = new ArrayList<ItemStatus>();
            items_status.add(new ItemStatus("Servicio", is_services_started));
            items_status.add(new ItemStatus("Dispositivo rooteado", is_devices_rooted));
            //items_status.add(new ItemStatus("Limite de SMS desactivado", false));
        mItemStatusAdapter = new ItemStatusAdapter(getBaseContext(), items_status);
        list_status.setAdapter(mItemStatusAdapter);

        list_log = (ListView) findViewById(R.id.list_log);
        List<ItemLog> items_log = new ArrayList<ItemLog>();
            items_log.add(new ItemLog(1, "2016-10-20 00:41", "El servicio se ha activado satisfactoriamente"));
            items_log.add(new ItemLog(2, "2016-10-20 00:42", "Ejemplo de Item Agregado"));
        for(int i=3; i<100; i++){
            items_log.add(new ItemLog(i, "2016-10-20 00:41", "Elemento " + i));
        }
        mItemLogAdapter = new ItemLogAdapter(getBaseContext(), items_log);
        list_log.setAdapter(mItemLogAdapter);


    }

    @Override
    public void onBackPressed() {
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
            Log.i("MainActivity", "Tap on contact");
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
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

}
