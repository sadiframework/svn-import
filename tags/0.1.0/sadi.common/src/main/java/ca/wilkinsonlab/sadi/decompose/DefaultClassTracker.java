package ca.wilkinsonlab.sadi.decompose;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntClass;

/**
 * @author Luke McCarthy
 */
public class DefaultClassTracker implements ClassTracker
{
	private Set<OntClass> seen;
	
	public DefaultClassTracker()
	{
		seen = new HashSet<OntClass>();
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.decompose.ClassTracker#seen(com.hp.hpl.jena.ontology.OntClass)
	 */
	@Override
	public boolean seen(OntClass c)
	{
		if (seen.contains(c)) {
			return true;
		} else {
			seen.add(c);
			return false;
		}
	}
}
