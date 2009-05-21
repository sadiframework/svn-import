package ca.wilkinsonlab.sadi.sparql;

import org.mindswap.pellet.utils.ATermUtils;

/**
 * TODO need to fix the SPARQL services so they're not passing around Pellet-specific things;
 * this will involve an overhaul of how properties are converted in the query client.
 * 
 * @author Ben Vandervalk
 */
public class PelletHelper
{
	public static boolean isInverted(String predicate)
	{
		return ATermUtils.isInv(ATermUtils.makeTermAppl(predicate));
	}
	
	public static String invert(String predicate)
	{
		return ATermUtils.makeInv(ATermUtils.makeTermAppl(predicate)).toString();
	}
}
