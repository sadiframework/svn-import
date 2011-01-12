package ca.wilkinsonlab.sadi.utils.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;

public class MultiSPARQLEndpointSearchNode extends SearchNode<Triple> {

	public static final Logger log = Logger.getLogger(MultiSPARQLEndpointSearchNode.class);
	protected SPARQLRegistry registry;
	
	public MultiSPARQLEndpointSearchNode(SPARQLRegistry registry, Triple triple) {
		super(triple);
		setRegistry(registry);
	}
	
	protected SPARQLRegistry getRegistry() {
		return registry;
	}

	protected void setRegistry(SPARQLRegistry registry) {
		this.registry = registry;
	}

	@Override
	public Set<SearchNode<Triple>> getSuccessors() {

		Set<SearchNode<Triple>> successors = new HashSet<SearchNode<Triple>>();

		if(getNode().getObject().isLiteral() || getNode().getObject().isBlank()) {
			return successors;
		}

		try {
			
			Collection<Triple> triples = new ArrayList<Triple>();

			Node s = getNode().getObject();
			
			Triple queryPattern = new Triple(s, NodeCreateUtils.create("?p"), NodeCreateUtils.create("?o"));
			String query = SPARQLStringUtils.getConstructQueryString(Collections.singletonList(queryPattern), Collections.singletonList(queryPattern));
			
			for(SPARQLEndpoint endpoint : getRegistry().getAllEndpoints()) {
				if(getRegistry().getServiceStatus(endpoint.getURI()) == ServiceStatus.DEAD) {
					continue;
				}
				if(getRegistry().subjectMatchesRegEx(endpoint.getURI(), s.getURI())) {
					try {
						triples.addAll(endpoint.constructQuery(query));
					} catch(IOException e) {
						log.trace("failed to query endpoint " + endpoint, e);
					}
				}
			}
			
			for(Triple triple : triples) {
				successors.add(new MultiSPARQLEndpointSearchNode(getRegistry(), triple));
			}
			
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return successors;
	}

}
