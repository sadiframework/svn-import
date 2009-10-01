package ca.wilkinsonlab.sadi.pellet;

import ca.wilkinsonlab.sadi.client.QueryClientTest;

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
}
