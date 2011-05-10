package ca.wilkinsonlab.sadi.utils;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntProperty;

import ca.wilkinsonlab.sadi.client.MultiRegistry;

/*
 * A class which keeps track of which properties are resolvable
 * to web services. This class is used by the optimizer to do
 * query planning, and also by the optimizer benchmarking
 * code (in sadi.admin).
 * 
 * This class is not thread safe, due to the possibility of
 * concurrent access to a Jena model via OntProperties.
 */

public class PropertyResolvabilityCache 
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PropertyResolvabilityCache.class);
	
	private MultiRegistry registry;
	private Map<OntProperty, Boolean> cache = new Hashtable<OntProperty, Boolean>();
	
	public PropertyResolvabilityCache(MultiRegistry registry) 
	{
		this.registry = registry;
	}
	
	public boolean isResolvable(OntProperty property) 
	{
		Set<OntProperty> equivalentProperties = OwlUtils.getEquivalentProperties(property);
		
		boolean isResolvable = false;
		
		for(OntProperty equivalentProperty : equivalentProperties) {

			/*
			 * NOTE: Generally, if a property has a true/false value in 
			 * the cache, all of its equivalent properties should also be
			 * present in the cache and have the same value. 
			 * 
			 * However, if new equivalent properties are defined between
			 * calls to isResolvable, this may not hold true.
			 */
			
			if(cache.containsKey(equivalentProperty)) {

				if(cache.get(property) == true) {
					isResolvable = true;
					break;
				} else {
					continue;
				}

			}
			
			if(registry.findServicesByPredicate(equivalentProperty.getURI()).size() > 0) {
				isResolvable = true;
				break;
			} 
				
		}
		
		if(isResolvable) {
		
			for(OntProperty equivalentProperty : equivalentProperties) {
				cache.put(equivalentProperty, true);
			}
	
		} else {

			for(OntProperty equivalentProperty : equivalentProperties) {
				cache.put(equivalentProperty, false);
			}
		
		}

		return isResolvable;
	}
	
	/**
	 * Return true if one or more of the given properties resolves to
	 * a web service.
	 * 
	 * @param properties the properties
	 * @return true if one or more of the properties is resolvable, 
	 * false otherwise
	 */
	public boolean isResolvable(Collection<OntProperty> properties) 
	{
		for(OntProperty property : properties) {
			if(isResolvable(property)) {
				return true;
			}
		}

		return false;
	}
	
}
