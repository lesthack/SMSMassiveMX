package jorgeluis.smsmassivemx;

import org.json.JSONException;
import org.json.JSONObject;
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

            JSONObject json_details = new JSONObject();
                json_details.put("method", session.getMethod());
                json_details.put("uri", session.getUri());
                json_details.put("parameters", session.getParameters());
                json_details.put("headers", session.getHeaders());
            json_data.put("details", json_details);
            output = json_data.toString(4);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, output);
    }


}
