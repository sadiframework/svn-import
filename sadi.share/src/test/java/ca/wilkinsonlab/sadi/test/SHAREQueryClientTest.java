package ca.wilkinsonlab.sadi.test;

import ca.wilkinsonlab.sadi.client.QueryClient;
import ca.wilkinsonlab.sadi.share.SHAREQueryClient;

public class SHAREQueryClientTest extends QueryClientTest
{
	@Override
	public QueryClient getClient()
	{
		return new SHAREQueryClient();
	}
}
