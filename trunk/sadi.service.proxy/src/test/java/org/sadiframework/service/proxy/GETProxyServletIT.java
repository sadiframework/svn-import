package org.sadiframework.service.proxy;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.sadiframework.utils.ContentType;
import org.sadiframework.utils.ModelDiff;
import org.sadiframework.utils.RdfUtils;
import org.sadiframework.utils.http.HttpUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class GETProxyServletIT
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(GETProxyServletIT.class);
	
	private static final String LOCAL_SERVICE_URL = 
	    String.format("http://localhost:%s/get-proxy", System.getProperty("jetty-maven-plugin.port"));
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		System.setProperty("sadi.service.ignoreForcedURL", "true");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		System.clearProperty("sadi.service.ignoreForcedURL");
	}
	
	@Test
	public void testJSONP() throws Exception
	{
		Map<String, String> params = new HashMap<String, String>();
		params.put("serviceURL", "http://sadiframework.org/examples/hello");
		params.put("inputURL", "http://sadiframework.org/examples/t/hello.input.1.rdf");
		params.put("callback", "foo");
		HttpResponse response = HttpUtils.GET(new URL(LOCAL_SERVICE_URL), params);
		String jsonp = IOUtils.toString(response.getEntity().getContent());
		assertTrue(String.format("incorrect response %s", jsonp), jsonp.startsWith("foo("));
		assertTrue(String.format("incorrect response %s", jsonp), jsonp.endsWith(");"));
		Object rdf = null;
		try {
		    /* we should be able to use JsonUtils.read, but it assumes
		     * the JSON is an object and this is a plain string...
		     * rdf = JsonUtils.read(StringUtils.substring(jsonp, 4, -2)); 
		     */
		    rdf = new ObjectMapper().readValue(StringUtils.substring(jsonp, 4, -2), String.class);
		} catch (Exception e) {
			fail("argument to callback isn't valid JavaScript");
		}
		Model output = ModelFactory.createDefaultModel();
		try {
			RdfUtils.loadModelFromInlineRDF(output, rdf.toString(), ContentType.RDF_XML);
		} catch (Exception e) {
			fail("argument to callback isn't serialized RDF/XML");
		}
		Model expectedOutput = ModelFactory.createDefaultModel();
		expectedOutput.read("http://sadiframework.org/examples/t/hello.output.1.rdf");
		ModelDiff diff = ModelDiff.diff(expectedOutput, output);
		assertTrue("argument doesn't match expected output", 
				diff.inXnotY.isEmpty() && diff.inYnotX.isEmpty());
	}
}
