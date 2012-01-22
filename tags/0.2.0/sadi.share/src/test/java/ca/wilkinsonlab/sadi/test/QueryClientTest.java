package ca.wilkinsonlab.sadi.test;

import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ca.wilkinsonlab.sadi.client.QueryClient;

public abstract class QueryClientTest
{
	public final static Logger log = Logger.getLogger(QueryClientTest.class);
	
	protected StopWatch stopWatch;
	protected QueryClient client;
	
	protected abstract QueryClient getClient();
	
	
	@Before
	public void setUp()
	{
		stopWatch = new StopWatch();
		client = getClient();
	}

	@After
	public void tearDown()
	{
		stopWatch.reset();
		client = null;
	}

	/* TODO find a better way to iterate over the example queries...
	 */
	protected void testQuery(String query)
	{
		log.info( String.format("executing query\n%s", query) );
		
		stopWatch.start();
		List<Map<String, String>> results = client.synchronousQuery(query);
		stopWatch.stop();
		
		StringBuffer buf = new StringBuffer("query finished in ");
		buf.append( DurationFormatUtils.formatDurationHMS(stopWatch.getTime()) );
		if (results.isEmpty())
			buf.append("\nno results");
		else
			for (Map<String, String> binding: results) {
				buf.append("\n");
				buf.append(binding);
			}
		log.info( buf.toString() );
		
		assertFalse(String.format("query \"%s\" returned no results", query), results.isEmpty());
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
	
//	@Test
//	public void testQuery15()
//	{
//		testQuery(ExampleQueries.getQueryByHtmlListIndex(15));
//	}
}
