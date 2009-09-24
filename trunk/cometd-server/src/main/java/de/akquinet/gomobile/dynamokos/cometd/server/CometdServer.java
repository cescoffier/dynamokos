package de.akquinet.gomobile.dynamokos.cometd.server;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.mortbay.cometd.continuation.ContinuationCometdServlet;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import de.akquinet.gomobile.dynamokos.cometd.CometdServletPublication;

/**
 * This class registered a ContinuationCometdServlet.
 * 
 * It relies on the OSGi HTTP Service and Jetty cometd-server.
 * 
 */
@Component
@Provides
public class CometdServer implements CometdServletPublication {

	/**
	 * UUID.
	 */
	private static final long serialVersionUID = -2289561690119387172L;

	/**
	 * The URL of the published Cometd Servlet
	 */
	@ServiceProperty(name = CometdServletPublication.PROP_COMETD_URL)
	private String cometd_url = null;

	/**
	 * iPOJO injects the HTTP Service in this member.
	 */
	@Requires (filter="(org.osgi.service.http.port=8080)")
	private HttpService http;

	/**
	 * Once service dependencies are satisfied, it registers the cometd servlet.
	 * 
	 * @throws ServletException
	 *             if the servlet cannot be published
	 * @throws NamespaceException
	 *             if there is a name conflict
	 */
	@Validate
	public void start() throws ServletException, NamespaceException {
		// Expose a Continuation cometd Servlet
		http.registerServlet("/cometd", new ContinuationCometdServlet(), null,
				null);
		
		cometd_url = "http://localhost:8080/cometd";
	}

	/**
	 * This methods is called either when the bundle is stopped, or if one of
	 * the two services disappears. This method has to be developed defensively
	 * to avoid null pointer exception because services are not necessary
	 * available.
	 */
	@Invalidate
	public void stop() {
		// check if the HTTP service is still there:
		if (http != null) { // If there, don't worry about the synchronization,
			// iPOJO manages that for you.
			http.unregister("/cometd");
		}
	}

	/**
	 * Return the URL of the published Cometd Servlet.
	 */
	public URL getURL() {
		try {
			return new URL(cometd_url);
		} catch (MalformedURLException e) {
			return null;
		}
	}
}
