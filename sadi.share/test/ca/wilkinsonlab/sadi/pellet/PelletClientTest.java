package ca.wilkinsonlab.sadi.pellet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ca.wikinsonlab.sadi.client.QueryClient.ClientCallback;
import ca.wilkinsonlab.sadi.client.QueryClientTest;
import ca.wilkinsonlab.sadi.pellet.PelletClient;
import ca.wilkinsonlab.sadi.test.ExampleQueries;

import org.junit.Test;

public class PelletClientTest extends QueryClientTest
{
	@Override
	public void setUp() throws Exception
	{
		client = new PelletClient();
	}

	@Override
	public void tearDown() throws Exception
	{
		client = null;
	}
	
	// This is a hack, but I can't figure out a way to get Eclipse to
	// recognize these tests (or run them individually).  It's seems that 
	// the  test runner isn't able to recognize inherited @Test methods. -- BV
	
	@Test 
	public void testQuery1() { testQuery(ExampleQueries.getQueryByHtmlListIndex(1)); }
	@Test 
	public void testQuery2() { testQuery(ExampleQueries.getQueryByHtmlListIndex(2)); }
	@Test 
	public void testQuery3() { testQuery(ExampleQueries.getQueryByHtmlListIndex(3)); }
	@Test 
	public void testQuery4() { testQuery(ExampleQueries.getQueryByHtmlListIndex(4)); }
	@Test 
	public void testQuery5() { testQuery(ExampleQueries.getQueryByHtmlListIndex(5)); }
	@Test 
	public void testQuery6() { testQuery(ExampleQueries.getQueryByHtmlListIndex(6)); }
	@Test 
	public void testQuery7() { testQuery(ExampleQueries.getQueryByHtmlListIndex(7)); }
	@Test 
	public void testQuery8() { testQuery(ExampleQueries.getQueryByHtmlListIndex(8)); }
	@Test 
	public void testQuery9() { testQuery(ExampleQueries.getQueryByHtmlListIndex(9)); }
	@Test 
	public void testQuery10() { testQuery(ExampleQueries.getQueryByHtmlListIndex(10)); }
	@Test 
	public void testQuery11() { testQuery(ExampleQueries.getQueryByHtmlListIndex(11)); }
	@Test 
	public void testQuery12() { testQuery(ExampleQueries.getQueryByHtmlListIndex(12)); }
	
}
