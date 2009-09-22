package de.akquinet.gomobile.dynamokos.client.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import de.akquinet.gomobile.dynamokos.prediction.Prediction;

/**
 * This class is exposed as a Servlet used by the Web Page to get Oracle
 * prediction.
 * 
 * It relies on the OSGi HTTP Service, and use the (proxied) Prediction service.
 * Service are tracked and injects by iPOJO, so no burden at all...
 * 
 */
@Component
public class OracleServlet extends HttpServlet {

	/**
	 * UUID.
	 */
	private static final long serialVersionUID = -3689561690119387172L;

	/**
	 * iPOJO injects the HTTP Service in this member.
	 */
	@Requires
	private HttpService http;

	/**
	 * Prediction service.
	 */
	private volatile Prediction m_oracle;

	/**
	 * iPOJO inject the Prediction service in this method when it's available.
	 * 
	 * @param oracle
	 */
	@Bind(optional = true)
	private void bindPrediction(Prediction oracle) {
		m_oracle = oracle;
	}

	/**
	 * iPOJO inject the Prediction service in this method when it's available.
	 * 
	 * @param oracle
	 */
	@Unbind(optional = true)
	private void unbindPrediction(Prediction oracle) {
		m_oracle = null;
	}

	/**
	 * Once service dependencies are satisfied, it registers the web page and
	 * the servlet.
	 * 
	 * @throws ServletException
	 *             if the servlet cannot be published
	 * @throws NamespaceException
	 *             if there is a name conflict
	 */
	@Validate
	public void start() throws ServletException, NamespaceException {
		// Expose itself
		http.registerServlet("/oracle", this, null, null);
		// Expose the web page
		http.registerResources("/dynamokos", "web", null);

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
			http.unregister("/oracle");
			http.unregister("/dynamokos");
		}
	}

	/**
	 * Just parses the question and ask the oracle.
	 * 
	 * @param req
	 *            the request
	 * @param resp
	 *            the response
	 * @throws ServletException
	 *             if something wrong happens.
	 * @throws IOException
	 *             if something really wrong happens.
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println("Contacting the oracle...");
		String q = req.getParameter("question");

		//Oracle is unavailable
		if (m_oracle == null) {
			resp.getOutputStream().print("Oracle temporary unavailable");
		}
		//OK, prediction !
		else {
			if (q == null || q.length() == 0) {
				resp.getOutputStream().print(m_oracle.getPrediction());
			} else {
				resp.getOutputStream().print(m_oracle.getPrediction(q));
			}
		}

	}

	/**
	 * Reply if the Prediction is available or not
	 * 
	 * @param req
	 *            the request
	 * @param resp
	 *            the response
	 * @throws ServletException
	 *             if something wrong happens.
	 * @throws IOException
	 *             if something really wrong happens.
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.getOutputStream().print(
				m_oracle == null ? "unavailable" : "available");
	}

}
