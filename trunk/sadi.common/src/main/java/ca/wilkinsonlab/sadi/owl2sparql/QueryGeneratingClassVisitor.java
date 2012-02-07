package ca.wilkinsonlab.sadi.owl2sparql;

import java.util.Iterator;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.vocabulary.OWL;

import ca.wilkinsonlab.sadi.decompose.DefaultClassVisitor;

public class QueryGeneratingClassVisitor extends DefaultClassVisitor
{
	private StringBuilder construct;
	private StringBuilder where;
	private String variable;
	
	public QueryGeneratingClassVisitor(StringBuilder construct, StringBuilder where, String variable)
	{
		super();
		this.construct = construct;
		this.where = where;
		this.variable = variable.startsWith("?") ? variable : String.format("?%s", variable);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.decompose.DefaultClassVisitor#visitPreDecompose(com.hp.hpl.jena.ontology.OntClass)
	 */
	@Override
	public void visitPreDecompose(OntClass c)
	{
		if (c.isURIResource()) {
			construct.append(variable).append(" a <").append(c.getURI()).append("> . \n");
			where.append("{\n")
			     .append(variable).append(" a <").append(c.getURI()).append("> . \n")
			     .append("} UNION {\n");
			// TODO just recurse into these?  might result in duplication...
			for (Iterator<OntClass> i = c.listSubClasses(); i.hasNext(); ) {
				OntClass subClass = i.next();
				if (subClass.isURIResource() && !subClass.equals(c) && !subClass.equals(OWL.Nothing)) {
					where.append(variable).append(" a <").append(subClass.getURI()).append("> . \n")
					     .append("} UNION {\n");
				}
			}
		} else {
			
		}
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.decompose.DefaultClassVisitor#visitPostDecompose(com.hp.hpl.jena.ontology.OntClass)
	 */
	@Override
	public void visitPostDecompose(OntClass c)
	{
		if (c.isURIResource()) {
			where.append("} . \n");
		} else {
			
		}
	}
}
