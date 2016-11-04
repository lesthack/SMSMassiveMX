package jorgeluis.smsmassivemx;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by lesthack on 23/08/16.
 */

public class CampaignsException extends Exception {
    private JSONObject payload;
    private JSONObject parameters;

    public CampaignsException(String message){
        super(message);
    }

    public CampaignsException(Integer code, String description) throws JSONException {
        payload = new JSONObject();
        payload.put("type", "error");
        payload.put("code", code);
        payload.put("description", description);
    }

    public CampaignsException(Integer code, String campaign_id, String description) throws JSONException {
        payload = new JSONObject();
        parameters = new JSONObject();

        payload.put("type", "error");
        payload.put("code", code);
        payload.put("description", description);

        parameters.put("campaign_id", campaign_id);
        parameters.put("date", getDateTime());

        payload.put("parameters", parameters);
    }

    private String getDateTime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public JSONObject getParameters(){
        return payload;
    }
}
