package jorgeluis.smsmassivemx;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lesthack on 23/08/16.
 */

public class CampaignsException extends Exception {
    private JSONObject parameters;

    public CampaignsException(String message){
        super(message);
    }

    public CampaignsException(Integer code, String description) throws JSONException {
        parameters = new JSONObject();
        parameters.put("type", "error");
        parameters.put("code", code);
        parameters.put("description", description);
    }

    public CampaignsException(Integer code, String campaign_id, String description) throws JSONException {
        parameters = new JSONObject();
        parameters.put("type", "error");
        parameters.put("code", code);
        parameters.put("campaign_id", campaign_id);
        parameters.put("description", description);
    }

    public JSONObject getParameters(){
        return parameters;
    }
}
