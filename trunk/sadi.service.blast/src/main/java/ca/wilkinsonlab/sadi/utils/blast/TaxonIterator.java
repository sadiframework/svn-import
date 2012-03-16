package ca.wilkinsonlab.sadi.utils.blast;

import java.util.Iterator;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class TaxonIterator extends TransformIterator
{
	private static final Logger log = Logger.getLogger(TaxonIterator.class);
	
	private static final Transformer transformer = new TaxonTransformer();
	
	static final String PREFIX = "blast.taxon";
	
	private static Iterator<?> getKeys()
	{
		try {
			return new PropertiesConfiguration("blast.properties").subset(PREFIX).getKeys();
		} catch (ConfigurationException e) {
			log.error("error reading blast.properties", e);
			return EmptyIterator.INSTANCE;
		}
	}
	
	public TaxonIterator()
	{
		super(getKeys(), transformer);
	}
	
	public static class TaxonTransformer implements Transformer
	{
		public Object transform(Object input)
		{
			log.debug(input);
			String s = (String)input;
			return s.replace("+", " ");
		}
	}
}
