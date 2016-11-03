package jorgeluis.smsmassivemx;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        db.execSQL("CREATE TABLE parameter(name TEXT, value TEXT, valid BOOLEAN);");
        db.execSQL("INSERT INTO parameter(name, value, valid) values('version','0.1', 1);");
        db.execSQL("INSERT INTO parameter(name, value, valid) values('time_scan_host','60', 1);");
        db.execSQL("INSERT INTO parameter(name, value, valid) values('time_dispatch','60', 1);");
        db.execSQL("INSERT INTO parameter(name, value, valid) values('time_sleep_dispatch','1', 1);");
        db.execSQL("INSERT INTO parameter(name, value, valid) values('sms_by_dispatch','30', 1);");
        //db.execSQL("INSERT INTO parameter(name, value) values('host_ws','https://gist.githubusercontent.com/lesthack/3706336e5e3a69b8878e6a57b3c21ad5/raw/9caff842440a6fbe767670f38deafa4b4348d436/sms.json');");
        db.execSQL("INSERT INTO parameter(name, value, valid) values('host_ws','http://www.estaciones.fundacionguanajuato.mx/Json_estaciones.php', 1);");
        //db.execSQL("INSERT INTO parameter(name, value, valid) values('host_ws','', 0);");
        db.execSQL("INSERT INTO parameter(name, value, valid) values('webhook','', 0);");

        db.execSQL("CREATE TABLE sms(id INTEGER PRIMARY KEY AUTOINCREMENT, campaign VARCHAR(15), launch_date DATETIME, phone VARCHAR(10), message VARCHAR(160), sent BOOLEAN, error BOOLEAN);");

        db.execSQL("CREATE TABLE log(id INTEGER PRIMARY KEY AUTOINCREMENT, log_date DATE, log_text TEXT, log_type INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
    }

    private String getDateTime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    private boolean insertSMS(String campaign, String launch_date, String phone, String message, SQLiteDatabase db_medium){
        Cursor cursor = db_reader.rawQuery("SELECT * FROM sms WHERE campaign=? AND phone=? AND message=?", new String[]{campaign, phone, message});
        if(cursor.getCount()==0){
            db_medium.execSQL("INSERT INTO sms(campaign, launch_date, phone, message, sent, error) VALUES(?,?,?,?,0,0)", new String[]{campaign, launch_date, phone, message});
            return true;
        }
        return false;
    }

    public void addLog(String log){
        addLog(log, 0);
    }

    public void addLog(String log, int type){
        try{
            db_writer.execSQL("INSERT INTO log(log_date, log_text, log_type) VALUES(?, ?, ?);", new Object[]{getDateTime(), log, type});
        }
        catch(Exception e){
            Log.w("DataBaseOpenHelper", e.getMessage());
        }
    }

    public List getLogsAfterMax(int max_id){
        Cursor cursor = db_reader.rawQuery("SELECT id, log_date, log_text, log_type FROM log WHERE id > " + max_id + " ORDER BY log_date ASC, id ASC", null);
        List<Object> list_logs = new ArrayList<Object>();
        if (cursor.moveToFirst()) {
            do {
                String[] log = new String[]{String.valueOf(cursor.getInt(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3)};
                list_logs.add(log);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list_logs;
    }

    public List getLogs(int last_n){
        Cursor cursor = db_reader.rawQuery("SELECT id, log_date, log_text, log_type FROM log ORDER BY log_date DESC, id DESC LIMIT ? OFFSET 0", new String[]{String.valueOf(last_n)});
        List<Object> list_logs = new ArrayList<Object>();
        if (cursor.moveToFirst()) {
            do {
                String[] log = new String[]{String.valueOf(cursor.getInt(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3)};
                list_logs.add(log);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return list_logs;
    }

    public int addCampaignSMS(String campaign, String launch_date, String phones[], String message, Boolean cast){
        int sms_inserted=0;
        try{
            db_writer.beginTransaction();
            for(int i=0;i<phones.length;i++){
                if(insertSMS(campaign, launch_date, phones[i], message, db_writer)){
                    sms_inserted++;
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
        return sms_inserted;
    }

    public int addCampaignSMS(String campaign, String launch_date, String phones[], String message[], Boolean cast){
        int sms_inserted = 0;
        try{
            db_writer.beginTransaction();
            if(!cast){
                for(int i=0; i<message.length; i++){
                    for(int j=0; j<phones.length; j++){
                        if(insertSMS(campaign, launch_date, phones[j], message[i], db_writer)){
                            sms_inserted++;
                        }
                    }
                }
            }
            else{
                if(phones.length==message.length){
                    for(int i=0;i<phones.length;i++){
                        if(insertSMS(campaign, launch_date, phones[i], message[i], db_writer)){
                            sms_inserted++;
                        }
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
        return sms_inserted;
    }

    public int addCampaignSMS(String campaign, String launch_date, JSONArray phones, JSONArray message, Boolean cast) {
        String[] phones_array = phones.toString().replace("[","").replace("]","").replace("\"","").split(",");
        String[] messages_array = message.toString().replace("[","").replace("]","").replace("\"","").split(",");
        return addCampaignSMS(campaign, launch_date, phones_array, messages_array, cast);
    }

    public void addSMS(String launch_date, String phone, String message){
        try{
            db_writer.execSQL("INSERT INTO sms(campaign, launch_date, phone, message) VALUES(?, ?, ?, ?);", new String[]{"simple", launch_date, phone, message});
        }
        catch(Exception e){
            Log.w("DataBaseOpenHelper", e.getMessage());
        }
    }

    public JSONArray getSMSList(String query, String[] parameters) throws JSONException {
        Cursor cursor = db_reader.rawQuery(query, parameters);
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

    public JSONArray getSMSListUnSent(String launch_date, int limit) throws JSONException {
        return getSMSList("SELECT id, campaign, phone, message FROM sms WHERE sent=0 AND error=0 AND launch_date <= ? LIMIT ? OFFSET 0", new String[]{launch_date, String.valueOf(limit)});
    }

    public JSONArray getSMSList(int sent) throws JSONException {
        return getSMSList("SELECT id, campaign, phone, message FROM sms WHERE sent=? AND error=0", new String[]{String.valueOf(sent)});
    }

    public void markSentSMS(Integer id){
        db_writer.execSQL("UPDATE sms SET sent = 1 WHERE id = " + id);
    }

    public void markErrorSMS(Integer id){
        db_writer.execSQL("UPDATE sms SET error = 1 WHERE id = " + id);
    }

    public String getParameter(String name){
        Cursor cursor = db_reader.rawQuery("SELECT value FROM parameter WHERE name=? LIMIT 1 OFFSET 0", new String[]{name});
        cursor.moveToFirst();
        if(cursor.getCount()<1){
            return null;
        }
        return cursor.getString(0);
    }

    public Boolean getValidParameter(String name){
        Cursor cursor = db_reader.rawQuery("SELECT valid FROM parameter WHERE name=? LIMIT 1 OFFSET 0", new String[]{name});
        cursor.moveToFirst();
        if(cursor.getCount()<1){
            return false;
        }
        return cursor.getString(0)=="1"?true:false;
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

    public boolean setParameter(String name, String value, Boolean valid){
        String query;

        try{
            if(getParameter(name) == null){
                db_writer.execSQL("INSERT INTO parameter(name, value, valid) VALUES(?, ?, ?);", new String[]{name, value, String.valueOf(valid)});
            }
            else{
                db_writer.execSQL("UPDATE parameter SET value=? and valid=? WHERE name=?", new String[]{value, name, String.valueOf(valid)});
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