package ca.wilkinsonlab.sadi.utils.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.graph.OpenGraphIterator.NodeVisitationConstraintBase;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * When traversing a SPARQL endpoint, avoid visiting nodes with
 * the same rdf:type more than once.
 */
public class RDFTypeConstraint extends NodeVisitationConstraintBase<Resource> 
{
	protected final static Logger log = Logger.getLogger(RDFTypeConstraint.class);
	
	protected SPARQLEndpoint endpoint;
	protected Set<Resource> visitedTypes = new HashSet<Resource>();
	
	public RDFTypeConstraint(SPARQLEndpoint endpoint) {
		setEndpoint(endpoint);
	}
	
	@Override
	public boolean isVisitable(Resource node) 
	{
		boolean foundUnvisitedType = false;
		for(Resource type : getTypes(node)) {
			if(!typeIsVisited(type)) {
				log.trace("encountered unvisited rdf:type " + type.getURI());
				foundUnvisitedType = true;
			}
		}
		
		if(!foundUnvisitedType) {
			log.trace("skipping node " + node.getURI() + ", all rdf:types have already been visited");
		}
		return foundUnvisitedType;
	}
	
	@Override
	public void visit(Resource node) {
		for(Resource type : getTypes(node)) {
			setTypeAsVisited(type);
		}
	}

	protected Set<Resource> getTypes(Resource node) {

		Set<Resource> types = new HashSet<Resource>();

		try {
			Triple queryPattern = new Triple(node.asNode(), RDF.type.asNode(), NodeCreateUtils.create("?type"));
			String query = SPARQLStringUtils.getConstructQueryString(Collections.singletonList(queryPattern), Collections.singletonList(queryPattern));
			Collection<Triple> triples = getEndpoint().constructQuery(query);
			
			for(Triple triple : triples) {
				Node o = triple.getObject();
				if(o.isURI()) {
					types.add(ResourceFactory.createResource(o.getURI()));
				}
			}
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return types;
	}
	
	protected SPARQLEndpoint getEndpoint() {
		return endpoint;
	}

	protected void setEndpoint(SPARQLEndpoint endpoint) {
		this.endpoint = endpoint;
	}
	
	protected boolean typeIsVisited(Resource type) {
		return visitedTypes.contains(type);
	}
	
	public void setTypeAsVisited(Resource type) {
		visitedTypes.add(type);
	}
}