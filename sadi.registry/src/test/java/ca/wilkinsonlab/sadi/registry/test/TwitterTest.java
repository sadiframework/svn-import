package ca.wilkinsonlab.sadi.registry.test;

import ca.wilkinsonlab.sadi.registry.utils.Twitter;

public class TwitterTest
{
	public static void main(String[] args) throws Exception
	{
		Twitter.getTwitter().sendDirectMessage("elmccarthy", "stumbled to the store");
	}
}
