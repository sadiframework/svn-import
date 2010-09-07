package ca.wilkinsonlab.sadi.registry;

import org.junit.Test;

import ca.wilkinsonlab.sadi.registry.utils.Twitter;

public class TwitterTest
{
	@Test
	public void testTweetService() throws Exception
	{
//		ServiceBean service = Registry.getRegistry().getRegisteredServices().iterator().next();
		ServiceBean service = Registry.getRegistry().getServiceBean("http://sadiframework.org/examples/uniprot2pubmed");
		Twitter.tweetService(service);
	}
}
