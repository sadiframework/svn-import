package org.sadiframework.utils.http;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.sadiframework.utils.ExceptionUtils;
import org.sadiframework.utils.http.HttpClient;


public class HttpClientIT extends TestCase {

	protected static final Logger log = Logger.getLogger(HttpClientIT.class);

	protected static final String TEST_URL = "http://dev.biordf.net/sparql";
	protected static final String TEST_QUERY = "SELECT * WHERE { ?s ?p ?o } LIMIT 1";

	protected static HttpClient theClient = new HttpClient();
	
	@Test
	public void testGET() {
		try {
			HttpResponse response = theClient.GET(new URL(TEST_URL));
			response.getEntity().getContent().close();
		} catch (IOException e) {
			fail("HTTP request failed: " + ExceptionUtils.getStackTrace(e));
		}
	}

	@Test
	public void testPOST() {
		try {
			Map<String,String> params = new HashMap<String,String>();
			params.put("query", TEST_QUERY);
			HttpResponse response = theClient.POST(new URL(TEST_URL), params);
			response.getEntity().getContent().close();
		} catch(IOException e) {
			fail("HTTP request failed: " + ExceptionUtils.getStackTrace(e));
		}
	}

}
