package remote.protocols.eacometdbridge.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Tools {

    public static Dictionary dataToDico(JSONObject data) {
        Dictionary<String, Object> dico = new Hashtable<String, Object>();

        Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                dico.put(key, data.get(key));
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        return dico;
    }

    public static JSONObject construcSubscribe(String topic, String clientId,
            int id) throws JSONException {
        JSONObject request = new JSONObject();

        request.put("channel", CometdEventAdminBridge.CHANNEL_SUBSCRIBE);
        request.put("clientId", clientId);
        request.put("id", String.valueOf(id));
        request.put("subscription", topic);

        return request;
    }

    public static JSONObject constructPublish(String topic, Map data,
            String clientId, int id) throws JSONException {
        JSONObject request = new JSONObject();

        request.put("channel", topic);
        request.put("clientId", clientId);
        request.put("id", String.valueOf(id));
        request.put("data", data);

        return request;
    }

    public static HttpURLConnection openConnection(URL cometurl)
            throws IOException {
        HttpURLConnection connec = (HttpURLConnection) cometurl
                .openConnection();

        connec.setRequestMethod(CometdEventAdminBridge.HTTP_METHOD);
        connec.setRequestProperty("Content-Type",
                CometdEventAdminBridge.HTTP_CONTENT_TYPE);
        connec.setRequestProperty("Accept-Charset",
                CometdEventAdminBridge.HTTP_ACCEPT_CHARSET);
        connec.setDoOutput(true);

        return connec;

    }

    public static JSONArray handleCometdResponse(HttpURLConnection connec)
            throws JSONException, IOException {
        String cometdResponse;
        InputStream iStrm = null;

        try {
            iStrm = connec.getInputStream();

            if (connec.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // Get body (data)
                int length = (int) connec.getContentLength();
                if (length != -1) {
                    byte responseData[] = new byte[length];
                    iStrm.read(responseData);
                    cometdResponse = new String(responseData);
                } else // Length not available...
                {
                    ByteArrayOutputStream bStrm = new ByteArrayOutputStream();

                    int ch;
                    while ((ch = iStrm.read()) != -1)
                        bStrm.write(ch);

                    cometdResponse = new String(bStrm.toByteArray());
                    bStrm.close();

                }
            } else {
                // FIXME log
                //cometdResponse = connec.getResponseMessage();
                return new JSONArray();
            }
            
            //FIXME do something better
            if (cometdResponse.charAt(0) == '['){
                return new JSONArray(cometdResponse);
            }else if (cometdResponse.charAt(0) == '{'){
                return new JSONArray().put(new JSONObject(cometdResponse));
            } else {
                // log error
                return new JSONArray();
            }

        } finally {
            if (iStrm != null) {
                iStrm.close();
            }
        }
    }

    public static void doCometdRequest(HttpURLConnection connec,
            JSONArray requests) throws IOException {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(connec.getOutputStream());
            writer.write(requests.toString());
        } catch (IOException e) {
            throw e;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static void doSimpleConnect(HttpURLConnection connec,
            String clientId, int id) throws IOException, JSONException {
        JSONObject request = new JSONObject();

        request.put("channel", CometdEventAdminBridge.CHANNEL_CONNECT);
        request.put("clientId", clientId);
        request.put("id", String.valueOf(id));
        request.put("connectionType",
                CometdEventAdminBridge.COMETD_SUPPORTED_CONNECTION_TYPE[0]);

        Tools.doCometdRequest(connec, new JSONArray().put(request));
    }

}
