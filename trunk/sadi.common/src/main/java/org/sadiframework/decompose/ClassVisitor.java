package org.sadiframework.decompose;

import com.hp.hpl.jena.ontology.OntClass;

/**
 * @author Luke McCarthy
 */
public interface ClassVisitor
{
	/**
	 * Returns true if we should ignore the specified class.
	 * If a class is ignored, it will not be visited and it will not be
	 * recursively decomposed.
	 * @param c the class
	 * @return true if we should ignore the specified class
	 */
	boolean ignore(OntClass c);
	
	/**
	 * Visit the specified class (called before the class is decomposed).
	 * @param c the class
	 */
	void visitPreDecompose(OntClass c);
	
	/**
	 * Visit the specified class (called after the class is decomposed).
	 * @param c the class
	 */
	void visitPostDecompose(OntClass c);
}
