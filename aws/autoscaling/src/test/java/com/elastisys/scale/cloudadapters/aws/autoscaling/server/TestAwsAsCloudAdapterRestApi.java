package com.elastisys.scale.cloudadapters.aws.autoscaling.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.elastisys.scale.cloudadapers.api.CloudAdapter;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterOptions;
import com.elastisys.scale.cloudadapers.api.server.CloudAdapterServer;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.AwsAsScalingGroup;
import com.elastisys.scale.cloudadapters.aws.autoscaling.scalinggroup.client.AwsAutoScalingClient;
import com.elastisys.scale.cloudadapters.commons.adapter.BaseCloudAdapter;
import com.elastisys.scale.commons.net.host.HostUtils;
import com.elastisys.scale.commons.rest.client.RestClients;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;

/**
 * Verifies some basic properties of the cloud adapter's REST API.
 */
public class TestAwsAsCloudAdapterRestApi {

	private static final String SERVER_KEYSTORE = Resources.getResource(
			"security/server/server_keystore.p12").toString();
	private static final String SERVER_KEYSTORE_PASSWORD = "serverpass";

	/** Web server to use throughout the tests. */
	private static Server server;
	/** Server port to use for HTTPS. */
	private static int httpsPort;

	private static CloudAdapter cloudAdapter;

	@BeforeClass
	public static void onSetup() throws Exception {
		List<Integer> freePorts = HostUtils.findFreePorts(1);
		httpsPort = freePorts.get(0);

		cloudAdapter = new BaseCloudAdapter(new AwsAsScalingGroup(
				new AwsAutoScalingClient()));

		CloudAdapterOptions options = new CloudAdapterOptions();
		options.httpsPort = httpsPort;
		options.sslKeyStore = SERVER_KEYSTORE;
		options.sslKeyStorePassword = SERVER_KEYSTORE_PASSWORD;
		options.requireClientCert = false;
		options.enableConfigHandler = true;

		server = CloudAdapterServer.createServer(cloudAdapter, options);
		server.start();
	}

	@AfterClass
	public static void onTeardown() throws Exception {
		server.stop();
		server.join();
	}

	/**
	 * Verifies that a {@code GET /config} request against the cloud adapter
	 * before it has had its configuration set results in a {@code 404} (Not
	 * Found) error response.
	 * <p/>
	 * At one time, this caused a bug due to conflicting versions of the Jackson
	 * library (pulled in by Jersey and aws-sdk, respectively).
	 */
	@Test
	public void testGetConfigBeforeSet() throws IOException {
		Client client = RestClients.httpsNoAuth();
		Response response = client.target(getUrl("/config")).request().get();
		assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
		assertNotNull(response.readEntity(JsonObject.class));
	}

	/**
	 * URL to do a {@code GET /pool} request.
	 *
	 * @param path
	 *            The resource path on the remote server.
	 * @return
	 */
	private static String getUrl(String path) {
		return String.format("https://localhost:%d%s", httpsPort, path);
	}

}
