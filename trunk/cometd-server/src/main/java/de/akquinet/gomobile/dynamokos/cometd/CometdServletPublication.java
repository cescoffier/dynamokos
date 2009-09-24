package de.akquinet.gomobile.dynamokos.cometd;

import java.net.URL;

public interface CometdServletPublication {
	String PROP_COMETD_URL = "cometd.server.url";

	public URL getURL();
}
