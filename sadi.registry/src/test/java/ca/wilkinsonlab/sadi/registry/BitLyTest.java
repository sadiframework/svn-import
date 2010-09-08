package ca.wilkinsonlab.sadi.registry;

import org.apache.log4j.Logger;
import org.junit.Test;

import ca.wilkinsonlab.sadi.registry.utils.BitLy;

public class BitLyTest
{
	private static final Logger log = Logger.getLogger(BitLyTest.class);
	
	@Test
	public void testGetShortURL() throws Exception
	{
		String longURL = "http://elmonline.ca/";
		String shortURL = BitLy.getShortURL(longURL);
		log.info(String.format("%s should redirect to %s", shortURL, longURL));
	}
}
