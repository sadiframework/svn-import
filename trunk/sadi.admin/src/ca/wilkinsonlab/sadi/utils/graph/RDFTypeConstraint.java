package ca.wilkinsonlab.sadi.utils.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.VirtuosoSPARQLRegistryAdmin;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.graph.OpenGraphIterator.NodeVisitationConstraint;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * When traversing a SPARQL endpoint, avoid visiting nodes with
 * the same rdf:type more than once.
 */
public class RDFTypeConstraint implements NodeVisitationConstraint<Resource> 
{
	protected SPARQLEndpoint endpoint;
	protected Set<Resource> visitedTypes = new HashSet<Resource>();
	
	public RDFTypeConstraint(SPARQLEndpoint endpoint) {
		setEndpoint(endpoint);
	}
	
	@Override
	public boolean isVisitable(Resource node) {

		try {
			String query = "CONSTRUCT { %u% %u% ?type } WHERE { %u% %u% ?type }";
			query = SPARQLStringUtils.strFromTemplate(query, node.getURI(), RDF.type.getURI(), node.getURI(), RDF.type.getURI());
			Collection<Triple> triples = getEndpoint().constructQuery(query);
			for(Triple triple : triples) {
				Node o = triple.getObject();
				if(o.isURI()) {
					Resource type = ResourceFactory.createResource(o.getURI());
					if(!typeIsVisited(type)) {
						VirtuosoSPARQLRegistryAdmin.log.trace("encountered unvisited rdf:type " + type.getURI());
						return true;
					}
				}
			}
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		VirtuosoSPARQLRegistryAdmin.log.trace("skipping node " + node.getURI() + ", all rdf:types have already been visited");
		return false;
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