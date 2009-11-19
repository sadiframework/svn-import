package ca.wilkinsonlab.sadi.client;

import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ca.wilkinsonlab.sadi.client.QueryClient;
import ca.wilkinsonlab.sadi.test.ExampleQueries;

public abstract class QueryClientTest extends TestCase
{
	public final static Log log = LogFactory.getLog(QueryClientTest.class);
	
	protected QueryClient client;
	
	@Before
	public abstract void setUp() throws Exception;

	@After
	public abstract void tearDown() throws Exception;

	/* TODO find a better way to iterate over the example queries...
	 */
	private void testQuery(String query)
	{
		log.info("Query: " + query + "\n\n");
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		List<Map<String, String>> results = client.synchronousQuery(query);
		stopWatch.stop();
		log.info(String.format("query finished in %d seconds", stopWatch.getTime()/1000));
		assertFalse(String.format("query \"%s\" returned no results", query), results.isEmpty());
		
		for (Map<String, String> binding : results)
			System.out.println(binding.toString());
	}
	
	@Test
	public void testQuery1()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(1));
	}
	
	@Test
	public void testQuery2()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(2));
	}
	
	@Test
	public void testQuery3()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(3));
	}
	
	@Test
	public void testQuery4()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(4));
	}
	
	@Test
	public void testQuery5()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(5));
	}
	
	@Test
	public void testQuery6()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(6));
	}
	
	@Test
	public void testQuery7()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(7));
	}
	
	@Test
	public void testQuery8()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(8));
	}
	
	@Test
	public void testQuery9()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(9));
	}
	
	@Test
	public void testQuery10()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(10));
	}
	
	@Test
	public void testQuery11()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(11));
	}
	
	@Test
	public void testQuery12()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(12));
	}
	
	@Test
	public void testQuery13()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(13));
	}
	
	@Test
	public void testQuery14()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(14));
	}
	
	@Test
	public void testQuery15()
	{
		testQuery(ExampleQueries.getQueryByHtmlListIndex(15));
	}
}
