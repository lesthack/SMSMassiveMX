package jorgeluis.smsmassivemx;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

/**
 * Created by lesthack on 19/08/16.
 */

public class DataBaseOpenHelper extends SQLiteOpenHelper {
    private final Context context;
    private static String DB_NAME = "smsmassivemx";
    private static String DB_PATH = Environment.getDataDirectory().getAbsolutePath() + "/data/jorgeluis.smsmassivemx/databases/";
    private SQLiteDatabase db_reader;
    private SQLiteDatabase db_writer;

    public DataBaseOpenHelper(Context context) {
        super(context, DB_NAME, null, 3);
        this.context = context;

        db_writer = this.getWritableDatabase();
        db_reader = this.getReadableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub

        Log.i("DataBaseOpenHelper", "Creating Database");

        db.execSQL("CREATE TABLE parameter(name TEXT, value TEXT);");
        db.execSQL("INSERT INTO parameter(name, value) values('version','0.1');");
        db.execSQL("INSERT INTO parameter(name, value) values('time_scan_host','10');");
        db.execSQL("INSERT INTO parameter(name, value) values('time_dispatch','60');");
        db.execSQL("INSERT INTO parameter(name, value) values('time_sleep_dispatch','0.5');");
        db.execSQL("INSERT INTO parameter(name, value) values('sms_by_dispatch','30');");

        db.execSQL("CREATE TABLE sms(id INTEGER PRIMARY KEY AUTOINCREMENT, campaign VARCHAR(5), launch_date DATETIME, phone VARCHAR(10), message VARCHAR(160), sent BOOLEAN);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
    }

    public boolean addSMS(String launch_date, String phone, String message){
        try{
            db_writer.execSQL("INSERT INTO sms(campaign, launch_date, phone, message);", new String[]{"simple", launch_date, phone, message});
            return true;
        }
        catch(Exception e){
            Log.w("DataBaseOpenHelper", e.getMessage());
        }
        return false;
    }

    /*
    *   Un mensaje - array numeros
    * */
    public boolean addCampaignSMS(String campaign, String launch_date, String phones[], String message, Boolean cast){
        db_writer.beginTransaction();
        try{
            for(int i=0;i<phones.length;i++){
                db_writer.execSQL("INSERT INTO sms(campaign, launch_date, phone, message);", new String[]{campaign, launch_date, phones[i], message});
            }
            db_writer.setTransactionSuccessful();
            return true;
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            db_writer.endTransaction();
        }
        return false;
    }

    /*
    *   array mensajes - array numeros donde si cast es verdadero, entonces len(array mensajes) == len(array numeros)
    * */
    public boolean addCampaignSMS(String campaign, String launch_date, String phones[], String message[], Boolean cast){
        db_writer.beginTransaction();
        try{
            if(cast){
                for(int i=0; i<message.length; i++){
                    for(int j=0; j<phones.length; j++){
                        db_writer.execSQL("INSERT INTO sms(campaign, launch_date, phone, message);", new String[]{campaign, launch_date, phones[j], message[i]});
                    }
                }
            }
            else{
                if(phones.length==message.length){
                    for(int i=0;i<phones.length;i++){
                        db_writer.execSQL("INSERT INTO sms(campaign, launch_date, phone, message);", new String[]{campaign, launch_date, phones[i], message[i]});
                    }

                }
                else{
                    throw new CampaignsException("The length between phones and messages is not equals.");
                }
            }
            db_writer.setTransactionSuccessful();
            return true;
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            db_writer.endTransaction();
        }
        return false;
    }

    public String getParameter(String name){
        Cursor cursor = db_reader.rawQuery("SELECT value FROM parameter WHERE name=? LIMIT 1 OFFSET 0", new String[]{name});
        cursor.moveToFirst();
        if(cursor.getCount()<1){
            return null;
        }
        return cursor.getString(0);
    }

    public boolean setParameter(String name, String value){
        String query;

        try{
            if(getParameter(name) == null){
                db_writer.execSQL("INSERT INTO parameter(name, value) VALUES(?, ?);", new String[]{name, value});
            }
            else{
                db_writer.execSQL("UPDATE parameter SET value=? WHERE name=?", new String[]{value, name});
            }
            return true;
        }
        catch(Exception e){
            Log.w("DataBaseOpenHelper", e.getMessage());
        }
        return false;
    }

    private boolean checkDataBase() {

        SQLiteDatabase checkDB = null;

        try {
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        }
        catch (SQLiteException e) {
            Log.w("DataBaseOpenHelper","No database found.");
        }

        if (checkDB != null) {
            checkDB.close();
        }

        return checkDB != null ? true : false;
    }
}

class CampaignsException extends Exception {

    public CampaignsException(String message){
        super(message);
    }

}