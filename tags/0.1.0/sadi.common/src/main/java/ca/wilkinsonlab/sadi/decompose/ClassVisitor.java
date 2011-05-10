package ca.wilkinsonlab.sadi.decompose;

import com.hp.hpl.jena.ontology.OntClass;

/**
 * @author Luke McCarthy
 */
public interface ClassVisitor
{
	/**
	 * Return true if we should ignore the specified class.
	 * If a class is ignored, it will not be visited and it will not be
	 * recursively decomposed.
	 * @param c the class
	 */
	boolean ignore(OntClass c);
	
	/**
	 * Visit the specified class.
	 * @param c the class
	 */
	void visit(OntClass c);
}
