package ca.wilkinsonlab.sadi.share;

import ca.wilkinsonlab.sadi.client.QueryClientTest;

public class SHAREQueryClientTest extends QueryClientTest
{
	@Override
	public void setUp() throws Exception
	{
		client = new SHAREQueryClient();
	}

	@Override
	public void tearDown() throws Exception
	{
		client = null;
	}
}
