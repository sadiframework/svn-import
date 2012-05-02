package org.sadiframework.utils;

import java.util.HashSet;
import java.util.Set;

import org.sadiframework.utils.graph.SearchNode;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * 
 * @author Ben Vandervalk
 */
public class MinimalOntologySearchNode extends SearchNode<Resource>
{
	Model sourceModel;
	Model targetModel;
	
	public MinimalOntologySearchNode(Model sourceModel, Model targetModel, Resource node)
	{
		super(node);
		this.sourceModel = sourceModel;
		this.targetModel = targetModel;
	}
	
	@Override
	public Set<SearchNode<Resource>> getSuccessors() 
	{
		Set<SearchNode<Resource>> successors = new HashSet<SearchNode<Resource>>();
		Resource r = getNode();
		
		for(Statement stmt : r.listProperties().toList()) { 
			targetModel.add(stmt);
			RDFNode o = stmt.getObject();
			if(o.isResource()) {
				successors.add(new MinimalOntologySearchNode(sourceModel, targetModel, o.as(Resource.class)));
			}
		}
		
		// If we just compute a normal directed closure starting from the root URI, we may miss  
		// equivalent/inverse/disjoint properties, and equivalent/complementary classes.  
		// (Since these relationships might be asserted in only one direction.)  
		// 
		// We also need to know all subproperties of an included property, or service discovery 
		// will not work correctly.
		
		Property reverseProperties[] = {
				OWL.equivalentProperty,
				OWL.inverseOf,
				OWL.disjointWith,
				OWL.complementOf,
				OWL.equivalentClass,
				OWL.sameAs,
				OWL.differentFrom,
				RDFS.subPropertyOf, 
			};
		
		for(Property reverseProperty : reverseProperties) {
			for(Statement stmt : sourceModel.listStatements(null, reverseProperty, r).toList()) { 
				targetModel.add(stmt);
				successors.add(new MinimalOntologySearchNode(sourceModel, targetModel, stmt.getSubject()));
			}
		}

		return successors;
	}
}