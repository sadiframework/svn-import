package ca.wilkinsonlab.sadi.share;

import ca.wilkinsonlab.sadi.client.QueryClient;
import ca.wilkinsonlab.sadi.client.QueryClientTest;

public class SHAREQueryClientTest extends QueryClientTest
{
	@Override
	public QueryClient getClient()
	{
		return new SHAREQueryClient();
	}
}
