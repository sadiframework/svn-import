package org.sadiframework.utils.blast;

import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.sadiframework.utils.blast.TaxonIterator;

public class TaxonIteratorTest
{
	private TaxonIterator taxons;
	
	@Before
	public void before()
	{
		taxons = new TaxonIterator();
	}
	
	@Test
	public void testKeys()
	{
		while (taxons.hasNext()) {
			String taxon = (String)taxons.next();
			assertFalse(String.format("key %s still has prefix", taxon), taxon.startsWith(TaxonIterator.PREFIX));
			assertFalse(String.format("key %s has untranslated +", taxon), taxon.contains("+"));
		}
	}
}
