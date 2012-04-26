package ca.wilkinsonlab.sadi.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.decompose.RestrictionVisitor;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;

public class RestrictedPropertyCollector implements RestrictionVisitor
{
	private static final Logger log = Logger.getLogger(RestrictedPropertyCollector.class);
	
	private Set<OntProperty> properties;
	
	public RestrictedPropertyCollector()
	{
		properties = new HashSet<OntProperty>();
	}
	
	public Set<OntProperty> getProperties()
	{
		return properties;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.decompose.RestrictionVisitor#visit(com.hp.hpl.jena.ontology.Restriction)
	 */
	public void visit(Restriction restriction)
	{
		try {
			OntProperty p = restriction.getOnProperty();
			if (p != null)
				properties.add(p);
		} catch (ConversionException e) {
			// we should already have warned about this above, but just in case...
			log.warn(String.format("undefined restricted property %s"), e);
		}
	}
}