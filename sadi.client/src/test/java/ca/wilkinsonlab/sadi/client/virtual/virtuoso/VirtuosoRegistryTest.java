package ca.wilkinsonlab.sadi.client.virtual.virtuoso;

import java.net.URL;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;

import ca.wilkinsonlab.sadi.virtuoso.VirtuosoSPARQLEndpoint;

public class VirtuosoRegistryTest extends TestCase
{
	private static final String SELECT_ALL = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 50";
	private VirtuosoSPARQLEndpoint registry;
	
	protected void setUp() throws Exception
	{
		registry = new VirtuosoSPARQLEndpoint(new URL("http://biordf.net/sparql"));
	}

	protected void tearDown() throws Exception
	{
		registry = null;
	}
	
	@Test
	public void testExecuteQuery() throws Exception
	{
		List<Map<String, String>> bindings = registry.executeQuery(SELECT_ALL);
		assertEquals(50, bindings.size());
	}
}
