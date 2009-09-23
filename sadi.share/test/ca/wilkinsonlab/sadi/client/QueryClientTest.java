package ca.wilkinsonlab.sadi.client;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertFalse;

import ca.wikinsonlab.sadi.client.QueryClient;

public abstract class QueryClientTest
{
	public final static Log LOGGER = LogFactory.getLog(QueryClientTest.class);
	
	protected QueryClient client;
	
	@Before
	public abstract void setUp() throws Exception;

	@After
	public abstract void tearDown() throws Exception;

	/* TODO find a better way to iterate over the example queries...
	 */
	protected void testQuery(String query)
	{
		List<Map<String, String>> results = client.synchronousQuery(query);
//		List<Map<String, String>> results = SimplePelletClient.selectQuery(query);
		assertFalse(String.format("query \"%s\" returned no results", query), results.isEmpty());
		
		LOGGER.info("Query: " + query + "\n\n");
		for (Map<String, String> binding : results)
			System.out.println(binding.toString());
		
	}

}
