package jorgeluis.smsmassivemx;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
        db.execSQL("INSERT INTO parameter(name, value) values('host_ws','https://gist.githubusercontent.com/lesthack/3706336e5e3a69b8878e6a57b3c21ad5/raw/39581b9c5e33406c3c46dc878638e5722b43d174/sms.json');");
        db.execSQL("INSERT INTO parameter(name, value) values('webhook','');");

        db.execSQL("CREATE TABLE sms(id INTEGER PRIMARY KEY AUTOINCREMENT, campaign VARCHAR(5), launch_date DATETIME, phone VARCHAR(10), message VARCHAR(160), sent BOOLEAN);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
    }

    public void addSMS(String launch_date, String phone, String message){
        try{
            db_writer.execSQL("INSERT INTO sms(campaign, launch_date, phone, message) VALUES(?, ?, ?, ?);", new String[]{"simple", launch_date, phone, message});
        }
        catch(Exception e){
            Log.w("DataBaseOpenHelper", e.getMessage());
        }
    }

    private void insertSMS(String campaign, String launch_date, String phone, String message, SQLiteDatabase db_medium){
        String[] parameters = {campaign, launch_date, phone, message, campaign, launch_date, phone, message};
        db_medium.execSQL("" +
            "INSERT INTO sms(campaign, launch_date, phone, message, sent)" +
            "SELECT ?,?,?,?,0 " +
            "WHERE NOT EXISTS(SELECT 1 FROM sms WHERE campaign=? AND launch_date=? AND phone=? AND message=?)" +
        "", parameters);
    }

    /*
    *   Un mensaje - array numeros
    * */
    public void addCampaignSMS(String campaign, String launch_date, String phones[], String message, Boolean cast){
        try{
            db_writer.beginTransaction();
            for(int i=0;i<phones.length;i++){
                insertSMS(campaign, launch_date, phones[i], message, db_writer);
            }
            db_writer.setTransactionSuccessful();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            db_writer.endTransaction();
        }
    }

    /*
    *   array mensajes - array numeros donde si cast es verdadero, entonces len(array mensajes) == len(array numeros)
    * */
    public void addCampaignSMS(String campaign, String launch_date, String phones[], String message[], Boolean cast){
        try{
            db_writer.beginTransaction();
            if(!cast){
                for(int i=0; i<message.length; i++){
                    for(int j=0; j<phones.length; j++){
                        insertSMS(campaign, launch_date, phones[j], message[i], db_writer);
                    }
                }
            }
            else{
                if(phones.length==message.length){
                    for(int i=0;i<phones.length;i++){
                        insertSMS(campaign, launch_date, phones[i], message[i], db_writer);
                    }
                }
                else{
                    throw new CampaignsException("The length between phones and messages is not equals.");
                }
            }
            db_writer.setTransactionSuccessful();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            db_writer.endTransaction();
        }
    }

    public void addCampaignSMS(String campaign, String launch_date, JSONArray phones, JSONArray message, Boolean cast) {
        String[] phones_array = phones.toString().split(",");
        String[] messages_array = message.toString().split(",");
        addCampaignSMS(campaign, launch_date, phones_array, messages_array, cast);
    }

    public JSONArray getSMSListUnSent(String launch_date, int limit) throws JSONException {
        Cursor cursor = db_reader.rawQuery("SELECT id, campaign, phone, message FROM sms WHERE sent=0 AND launch_date <= ? LIMIT ? OFFSET 0", new String[]{launch_date, String.valueOf(limit)});
        //Cursor cursor = db_reader.rawQuery("SELECT id, campaign, phone, message FROM sms WHERE sent=0 AND launch_date <", null);
        JSONArray sms_list = new JSONArray();
        if (cursor.moveToFirst()) {
            do {
                JSONObject item = new JSONObject();
                item.put("id", cursor.getString(0));
                item.put("campaign", cursor.getString(1));
                item.put("phone", cursor.getString(2));
                item.put("message", cursor.getString(3));
                sms_list.put(item);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return sms_list;
    }

    public void changeStatusSMS(JSONArray list_sms) throws JSONException {
        for(int i=0; i<list_sms.length(); i++){
            JSONObject item = list_sms.getJSONObject(i);
            if(item.has("sent")){

            }
        }
    }

    public void setSendedSMS(Integer id){
        db_writer.execSQL("UPDATE sms SET sended = 1 WHERE id = " + id);
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