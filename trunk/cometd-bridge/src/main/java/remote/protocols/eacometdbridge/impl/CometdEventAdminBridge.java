package remote.protocols.eacometdbridge.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import remote.protocols.eacometdbridge.EABridge;
import de.akquinet.gomobile.dynamokos.cometd.CometdServletPublication;

public class CometdEventAdminBridge implements EventHandler, EABridge {

    public final static String HTTP_CONTENT_TYPE = "text/json;charset=UTF-8";
    public final static String HTTP_METHOD = "POST";
    public final static String HTTP_ACCEPT_CHARSET = "ISO-8859-1,utf-8;q=0.7,*;q=0.7";

    public final static String[] COMETD_SUPPORTED_CONNECTION_TYPE = {
            "long-polling", "long-polling-json-encoded", "callback-polling" };
    public final static String COMETD_VERSION = "1.0";
    public final static String COMETD_MINIMALVERSION = "0.9";
    public final static String CHANNEL_HANDSHAKE = "/meta/handshake";
    public final static String CHANNEL_CONNECT = "/meta/connect";
    public final static String CHANNEL_SUBSCRIBE = "/meta/subscribe";

    public final static int THREAD_TIME_OUT = 5000;
    public final static int NUMBER_OF_THREAD = 2;
    public final static int BLOCKING_QUEUE_SIZE = 20;

    private URL m_cometurl;
    private String m_clientId;
    private volatile boolean m_alive = false;
    private static int m_rid = 0;
    private ReadWriteLock m_rwlock;

    private Executor m_executor;

    private Map<String, String> m_importedtopic;
    private Map<String, String> m_exportedtopic;

    private EventAdmin m_eventadmin;
    private BundleContext m_context;
    
    //Small hack to start after the server
    private CometdServletPublication m_server;

    public CometdEventAdminBridge(BundleContext bc) {
        m_cometurl = m_server.getURL();
        
        m_executor = new ThreadPoolExecutor(1, NUMBER_OF_THREAD,
                THREAD_TIME_OUT, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(BLOCKING_QUEUE_SIZE));
        m_rwlock = new ReentrantReadWriteLock();
        m_context = bc;

        m_importedtopic = new ConcurrentHashMap<String, String>();
        m_exportedtopic = new ConcurrentHashMap<String, String>();
    }

    public void start() throws RuntimeException {
        HttpURLConnection connec = null;
        JSONObject request = null;
        JSONArray response = null;

        try {
            m_rwlock.writeLock().lock();
            m_alive = true;

            request = new JSONObject();
            request.put("version", COMETD_VERSION);
            request.put("minimumVersion", COMETD_MINIMALVERSION);
            request.put("channel", CHANNEL_HANDSHAKE);
            request.put("id", String.valueOf(m_rid++));
            request.put("supportedConnectionTypes",
                    COMETD_SUPPORTED_CONNECTION_TYPE);
            connec = Tools.openConnection(m_cometurl);

            Tools.doCometdRequest(connec, new JSONArray().put(request));

            response = Tools.handleCometdResponse(connec);
            JSONObject handshake = response.getJSONObject(0);

            if (!handshake.getBoolean("successful")) {
                throw new RuntimeException("Cometd Handshake failed");
            } else {
                m_clientId = handshake.getString("clientId");
            }

            connec.disconnect();

            connec = Tools.openConnection(m_cometurl);
            Tools.doSimpleConnect(connec, m_clientId, m_rid++);
            response = Tools.handleCometdResponse(connec);
            handshake = response.getJSONObject(0);

            if (!handshake.getBoolean("successful")) {
                throw new RuntimeException("Cometd connect failed");
            } 
            
	    connec.disconnect();

            System.out.println("START THREAD");
            m_executor.execute(new WaitReply());

        } catch (Exception e) {
            // TODO: LOG
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (connec != null) {
                connec.disconnect();
            }
            m_rwlock.writeLock().unlock();
        }

    }

    public void stop() {
        try {
            m_rwlock.writeLock().lock();
            m_alive = false;
            m_importedtopic.clear();
            m_exportedtopic.clear();
        } finally {
            m_rwlock.writeLock().unlock();
        }
    }

    public void publish(String topic, Map data) throws Exception {
        HttpURLConnection connec = null;
        try {
            if (m_alive) {
                connec = Tools.openConnection(m_cometurl);
                JSONObject pub = Tools.constructPublish(topic, new Hashtable(
                        data), m_clientId, m_rid++);
                Tools.doCometdRequest(connec, new JSONArray().put(pub));

                System.out.println("request: " + new JSONArray().put(pub));
                JSONArray response = Tools.handleCometdResponse(connec);
                connec.disconnect();
            }
        } finally {
            if (connec != null) {
                connec.disconnect();
            }
        }

    }

    public void subscribe(String topic) throws Exception {
        HttpURLConnection connec = null;
        try {
            m_rwlock.writeLock().lock();
            if (m_alive) {
                connec = Tools.openConnection(m_cometurl);
                JSONObject pub = Tools.construcSubscribe(topic, m_clientId,
                        m_rid++);
                Tools.doCometdRequest(connec, new JSONArray().put(pub));
                JSONArray response = Tools.handleCometdResponse(connec);
                connec.disconnect();
            }
        } finally {
            if (connec != null) {
                connec.disconnect();
            }
            m_rwlock.writeLock().unlock();
        }

    }

    private class WaitReply implements Runnable {

        public void run() {
            HttpURLConnection connec = null;
            JSONArray multiresponse;
            JSONObject message;
            JSONObject data;
            String channel;
            while (m_alive) {
                try {
                    connec = Tools.openConnection(m_cometurl);
                    connec.addRequestProperty("Connection", "keep-alive");
                    connec.addRequestProperty("Keep-Alive", "3000");

                    Tools.doSimpleConnect(connec, m_clientId, m_rid++);

                    multiresponse = keepAliveCometdResponse(connec);

                    int i = 0;
                    while (i < multiresponse.length()) {
                        message = multiresponse.getJSONObject(i);
                        channel = message.getString("channel");
                        // FIXME do something better
                        if (m_importedtopic.containsKey(channel)) {
                            data = message.getJSONObject("data");
                            m_eventadmin.sendEvent(new Event(
                                    (String) m_importedtopic.get(channel),
                                    Tools.dataToDico(data)));
                        }

                        i++;
                    }
                } catch (IOException e) {
                    // log (timeout), serveur unreachable etc...
                    // e.printStackTrace();
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    if (connec != null)
                        connec.disconnect();
                }
            }
        }

    }

    private JSONArray keepAliveCometdResponse(HttpURLConnection connec)
            throws IOException {
        String cometdResponse;
        InputStream iStrm = null;
        try {

            ByteArrayOutputStream bStrm = new ByteArrayOutputStream();
            iStrm = connec.getInputStream();

            // Handle the success response
            if (connec.getResponseCode() == HttpURLConnection.HTTP_OK) {

                int ch;
                while ((ch = iStrm.read()) != -1)
                    bStrm.write(ch);

                while ((ch = iStrm.read()) != -1)
                    bStrm.write(ch);

                bStrm.close();

                cometdResponse = new String(bStrm.toByteArray());

            } else {// Error
                try {
                    System.out.println("error: " + connec.getResponseMessage());
                    // XXX log an error
                    return new JSONArray().put(new JSONObject(connec
                            .getResponseMessage()));
                } catch (JSONException e) {
                    return new JSONArray();
                }
            }

            try {
                if (cometdResponse.length() != 0) {
                    return new JSONArray(cometdResponse);
                } else {
                    return new JSONArray();
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return new JSONArray();
            }
        } finally {
            if (iStrm != null) {
                iStrm.close();
            }
        }
    }

    public void importTopic(String cometdTopic, String osgiTopic) {
        try {
            if (m_alive && !m_importedtopic.containsKey(cometdTopic)) {
                m_importedtopic.put(cometdTopic, osgiTopic);
                subscribe(cometdTopic);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
        }

    }

    public void exportTopic(String osgiTopic, String cometdTopic) {

        if (!m_exportedtopic.containsKey(osgiTopic)) {
            m_exportedtopic.put(osgiTopic, cometdTopic);

            String[] topics = new String[] { osgiTopic };
            Hashtable<String, String[]> ht = new Hashtable<String, String[]>();
            ht.put(EventConstants.EVENT_TOPIC, topics);
            ServiceRegistration sr = m_context.registerService(
                    EventHandler.class.getName(), this, ht);

        }
    }

    public void handleEvent(Event event) {
        // TODO Auto-generated method stub
        try {
            String osgitopic = event.getTopic();

            if (m_alive && m_exportedtopic.containsKey(osgitopic)) {
                String cometdtopic = m_exportedtopic.get(osgitopic);

                Map<String, Object> data = new Hashtable<String, Object>();

                String[] keys = event.getPropertyNames();
                for (String key : keys) {
                    data.put(key, event.getProperty(key));
                }

                System.out.println("topic: " + cometdtopic);
                publish(cometdtopic, data);
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
}
