package ca.wilkinsonlab.sadi.service.proxy;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stringtree.json.JSONReader;
import org.stringtree.util.StreamUtils;

import ca.wilkinsonlab.sadi.utils.ContentType;
import ca.wilkinsonlab.sadi.utils.ModelDiff;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class GETProxyServletIT
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(GETProxyServletIT.class);
	
	private static final String LOCAL_SERVICE_URL = "http://localhost:8480/sadi-proxy/get-proxy";
	
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
		String jsonp = StreamUtils.readStream(response.getEntity().getContent());
		assertTrue(String.format("incorrect response %s", jsonp), jsonp.startsWith("foo("));
		assertTrue(String.format("incorrect response %s", jsonp), jsonp.endsWith(");"));
		Object rdf = null;
		try {
			rdf = new JSONReader().read(StringUtils.substring(jsonp, 4, -2));
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
