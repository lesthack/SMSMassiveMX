package jorgeluis.smsmassivemx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by lesthack on 18/08/16.
 */

public class StartUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        Log.i("Dev", "Servicio encendido al iniciar el smartphone");
        Intent service_intent = new Intent(context, CoreService.class);
        context.startService(service_intent);
        /*
        // Iniciando Actividad al bootear
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        */
    }

}
