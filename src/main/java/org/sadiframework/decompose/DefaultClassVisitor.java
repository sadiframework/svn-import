package org.sadiframework.decompose;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * @author Luke McCarthy
 */
public class DefaultClassVisitor implements ClassVisitor
{
	public DefaultClassVisitor()
	{	
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.decompose.ClassVisitor#ignore(com.hp.hpl.jena.ontology.OntClass)
	 */
	@Override
	public boolean ignore(OntClass c)
	{
		/* bottom out explicitly at owl:Thing, or we'll have problems when
		 * we enumerate equivalent classes...
		 */
		return c.equals( OWL.Thing );
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.decompose.ClassVisitor#visitPreDecompose(com.hp.hpl.jena.ontology.OntClass)
	 */
	@Override
	public void visitPreDecompose(OntClass c)
	{
	}

	/* (non-Javadoc)
	 * @see org.sadiframework.decompose.ClassVisitor#visitPostDecompose(com.hp.hpl.jena.ontology.OntClass)
	 */
	@Override
	public void visitPostDecompose(OntClass c)
	{
	}
}
