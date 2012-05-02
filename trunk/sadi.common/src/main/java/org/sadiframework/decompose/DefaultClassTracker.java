package org.sadiframework.decompose;

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
	
	public DefaultClassTracker(ClassTracker base)
	{
		this();
		if (base instanceof DefaultClassTracker)
			seen.addAll(((DefaultClassTracker)base).seen);
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.decompose.ClassTracker#seen(com.hp.hpl.jena.ontology.OntClass)
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
