package ca.wilkinsonlab.sadi.utils.http;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;

import ca.wilkinsonlab.sadi.utils.ExceptionUtils;

import junit.framework.TestCase;

public class XLightwebHttpClientTest extends TestCase {

	protected static final Logger log = Logger.getLogger(XLightwebHttpClientTest.class);

	protected static final String TEST_URL = "http://dev.biordf.net/sparql";
	protected static final String TEST_QUERY = "SELECT * WHERE { ?s ?p ?o } LIMIT 1";
	protected static final int BATCH_SIZE = 50;

	protected static HttpClient theClient = new XLightwebHttpClient();
	
	@Test
	public void testGET() {
		try {
			theClient.GET(new URL(TEST_URL));
		} catch (IOException e) {
			fail("HTTP request failed: " + ExceptionUtils.getStackTrace(e));
		}
	}

	@Test
	public void testPOST() {
		try {
			Map<String,String> params = new HashMap<String,String>();
			params.put("query", TEST_QUERY);
			theClient.POST(new URL(TEST_URL), params);
		} catch(IOException e) {
			fail("HTTP request failed: " + ExceptionUtils.getStackTrace(e));
		}
	}

	@Test
	public void testBatchGET() {
		try {
			Set<HttpRequest> requests = new HashSet<HttpRequest>();
			for (int i = 0; i < BATCH_SIZE; i++) {
				requests.add(new GetRequest(new URL(TEST_URL)));
			}

			Set<HttpResponse> responses = theClient.batchRequest(requests);

			for (HttpResponse response : responses) {
				assertFalse(response.exceptionOccurred());
			}
		} catch (IOException e) {
			fail("HTTP request failed: " + ExceptionUtils.getStackTrace(e));
		}
	}

	@Test
	public void testBatchPOST() {
		try {

			Map<String,String> params = new HashMap<String,String>();
			params.put("query", TEST_QUERY);

			Set<HttpRequest> requests = new HashSet<HttpRequest>();
			for (int i = 0; i < BATCH_SIZE; i++) {
				requests.add(new PostRequest(new URL(TEST_URL), params));
			}

			Set<HttpResponse> responses = theClient.batchRequest(requests);

			for (HttpResponse response : responses) {
				assertFalse(response.exceptionOccurred());
			}
		} catch (IOException e) {
			fail("HTTP request failed: " + ExceptionUtils.getStackTrace(e));
		}
	}
	
}
