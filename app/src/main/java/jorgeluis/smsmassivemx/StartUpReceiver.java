package jorgeluis.smsmassivemx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by lesthack on 18/08/16.
 */

public class StartUpReceiver extends BroadcastReceiver {
    private DataBaseOpenHelper localdb;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub

        //if(localdb.getValidParameter("host_ws")){
            addLog("Servicio encendido al iniciar el smartphone");
            Intent service_intent = new Intent(context, CoreService.class);
            context.startService(service_intent);
        //}
    }

    private void addLog(String log_text){
        addLog(log_text, 0);
    }

    private void addLog(String log_text, int log_type){
        Log.i("StartUpReceiver", log_text);
        localdb.addLog(log_text, log_type);
    }
}
