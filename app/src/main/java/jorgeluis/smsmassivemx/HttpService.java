package jorgeluis.smsmassivemx;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by lesthack on 18/08/16.
 */

public class HttpService extends NanoHTTPD {
    public HttpService(int port) {
        super(port);
    }

    public HttpService(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        JSONObject json_data = new JSONObject();
        String output = "";

        try {

            json_data.put("status", 200);

            Map parameters = getSimpleParameters(session.getParameters());
            Log.i("dev", parameters.toString());

            json_data.put("details", getHTTPDetails(session));
            output = json_data.toString(4);

            return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, output);
        } catch (JSONException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "");
        }

    }

    private HashMap<String, Object> getSimpleParameters(Map parameters){
        HashMap<String, Object> simple_parameters = new HashMap<String, Object> ();
        Iterator it = parameters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            List values = (List) pair.getValue();
            if(values.size()==1){
                simple_parameters.put(pair.getKey().toString(), values.get(0));
            }
            it.remove();
        }
        return simple_parameters;
    }

    private JSONObject getHTTPDetails(IHTTPSession session) throws JSONException {
        JSONObject json_details = new JSONObject();
            json_details.put("method", session.getMethod());
            json_details.put("uri", session.getUri());
            json_details.put("parameters", session.getParameters());
            json_details.put("headers", session.getHeaders());
            json_details.put("source_ip", session.getRemoteIpAddress());
        return json_details;
    }


}
