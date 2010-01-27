/**
 * 
 */
package ca.wilkinsonlab.sadi.utils.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class SPARQLSearchNode extends SearchNode<Resource> {

	protected SPARQLEndpoint endpoint;
	
	public SPARQLSearchNode(SPARQLEndpoint endpoint, Resource node) {
		super(node);
		setEndpoint(endpoint);
	}
	
	@Override
	public Set<SearchNode<Resource>> getSuccessors() {
		
		Set<SearchNode<Resource>> successors = new HashSet<SearchNode<Resource>>();
		try {
			String query = "CONSTRUCT { %u% ?p ?o } WHERE { %u% ?p ?o }";
			query = SPARQLStringUtils.strFromTemplate(query, getNode().getURI(), getNode().getURI());
			Collection<Triple> triples = getEndpoint().constructQuery(query);
			for(Triple triple : triples) {
				Node o = triple.getObject();
				if(o.isURI()) {
					Resource successor = ResourceFactory.createResource(o.getURI());
					successors.add(new SPARQLSearchNode(getEndpoint(), successor));
				}
			}
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return successors;
	}

	public SPARQLEndpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(SPARQLEndpoint endpoint) {
		this.endpoint = endpoint;
	}
}