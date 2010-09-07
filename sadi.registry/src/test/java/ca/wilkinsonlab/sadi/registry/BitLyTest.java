package ca.wilkinsonlab.sadi.registry;

import org.junit.Test;

import ca.wilkinsonlab.sadi.registry.utils.BitLy;

public class BitLyTest
{
	@Test
	public void testGetShortURL() throws Exception
	{
		System.out.println(BitLy.getShortURL("http://elmonline.ca/"));
	}
}
