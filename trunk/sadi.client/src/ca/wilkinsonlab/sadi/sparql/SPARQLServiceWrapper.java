package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInvocationException;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A proxy object which exposes a SPARQL endpoint as a Service.
 * @author Ben Vandervalk
 */
public class SPARQLServiceWrapper implements Service
{
	private static final Logger log = Logger.getLogger(SPARQLServiceWrapper.class);

	private static final String RESULTS_LIMIT_CONFIG_KEY = "sadi.sparql.resultsLimit";
	private long resultsLimit;

	private static final int QUERY_TIMEOUT = 15 * 1000;  // in milliseconds  
	
	private SPARQLEndpoint endpoint;
	private SPARQLRegistry registry;
	private String predicate;
	private boolean predicateIsInverse;

	public SPARQLServiceWrapper(SPARQLEndpoint endpoint, SPARQLRegistry registry) 
	{
		this(endpoint, registry, null, false);
	}
	
	public SPARQLServiceWrapper(SPARQLEndpoint endpoint, SPARQLRegistry registry, String predicate, boolean predicateIsInverse) 
	{
		setEndpoint(endpoint);
		setRegistry(registry);
		setPredicate(predicate);
		setPredicateIsInverse(predicateIsInverse);

		initResultsLimit();
	}
	
	// FIXME if this really needs to be public, Javadoc it...
	public void initResultsLimit()
	{
		if(Config.getConfiguration().containsKey(RESULTS_LIMIT_CONFIG_KEY)) 
			setResultsLimit(Config.getConfiguration().getLong(RESULTS_LIMIT_CONFIG_KEY));
		else
			setResultsLimit(SPARQLEndpoint.NO_RESULTS_LIMIT);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getServiceURI()
	 */
	public String getServiceURI() {
		return getEndpoint().getURI();
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getName()
	 */
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getDescription()
	 */
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
		
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getInputClass()
	 */
	public OntClass getInputClass()
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getOutputClass()
	 */
	public OntClass getOutputClass()
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource)
	 */
	public Collection<Triple> invokeService(Resource inputNode) throws ServiceInvocationException
	{
		try {
			return invokeService(inputNode.asNode());
		} catch (Exception e) {
			throw new ServiceInvocationException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(java.util.Collection)
	 */
	public Collection<Triple> invokeService(Collection<Resource> inputNodes) throws ServiceInvocationException
	{
		// FIXME
		Collection<Triple> triples = new ArrayList<Triple>();
		for (Resource inputNode: inputNodes)
			triples.addAll(invokeService(inputNode));
		return triples;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
	 */
	public Collection<Triple> invokeService(Resource inputNode, String predicate) throws ServiceInvocationException
	{
		try {
			return invokeService(inputNode.asNode());
		} catch (Exception e) {
			throw new ServiceInvocationException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(java.util.Collection, java.lang.String)
	 */
	public Collection<Triple> invokeService(Collection<Resource> inputNodes, String predicate) throws ServiceInvocationException
	{
		// FIXME
		Collection<Triple> triples = new ArrayList<Triple>();
		for (Resource inputNode: inputNodes)
			triples.addAll(invokeService(inputNode, predicate));
		return triples;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#isInputInstance(com.hp.hpl.jena.rdf.model.Resource)
	 */
	public boolean isInputInstance(Resource resource)
	{
		boolean matches = false;

		try {
			if(predicateIsInverse()) {
				matches = getRegistry().objectMatchesRegEx(getEndpoint().getURI(), resource.getURI());
			}
			else {
				matches = getRegistry().subjectMatchesRegEx(getEndpoint().getURI(), resource.getURI());
			}
		} catch(IOException e) {
			throw new RuntimeException("Error communicating with SPARQL registry: ", e);
		}

		return matches;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#discoverInputInstances(com.hp.hpl.jena.rdf.model.Model)
	 */
	public Collection<Resource> discoverInputInstances(Model inputModel)
	{
		log.warn("discoverInputInstances not yet implemented");
		return new ArrayList<Resource>(0);
	}

	public long getResultsLimit() { return resultsLimit; }
	public void setResultsLimit(long limit) { resultsLimit = limit; }
	
	public SPARQLEndpoint getEndpoint() { return endpoint; }
	public void setEndpoint(SPARQLEndpoint endpoint) {	this.endpoint = endpoint;	}

	public SPARQLRegistry getRegistry() { return registry; }
	public void setRegistry(SPARQLRegistry registry) { this.registry = registry; }

	public boolean predicateIsInverse() { return predicateIsInverse; }
	public void setPredicateIsInverse(boolean predicateInverted) { this.predicateIsInverse = predicateInverted; }

	public String getPredicate() { return predicate; }
	public void setPredicate(String predicate) {this.predicate = predicate; }
	
	private Collection<Triple> invokeService(Node inputURIorLiteral) throws ServiceInvocationException
	{
		Triple queryPattern;
		Node var1 = NodeCreateUtils.create("?var1");
		Node var2 = NodeCreateUtils.create("?var2");
		
		if(predicateIsInverse())
			queryPattern = new Triple(var1, var2, inputURIorLiteral);
		else {
			// Sanity check.
			if(inputURIorLiteral.isLiteral())
				throw new RuntimeException("Attempted to query with a triple pattern where the subject is a literal.");
			queryPattern = new Triple(inputURIorLiteral, var1, var2);
		}
		
		try {
			return getEndpoint().getTriplesMatchingPattern(queryPattern, QUERY_TIMEOUT, getResultsLimit());
		} catch (IOException e) {
			throw new ServiceInvocationException(e.getMessage());
		}
	}
	
	public Collection<Triple> invokeService(String inputURI) throws Exception 
	{
		Node input = NodeCreateUtils.create(inputURI);
		return invokeService(input);
		
		/*
		Triple queryPattern;
		Node var1 = NodeCreateUtils.create("?var1");
		Node input = NodeCreateUtils.create(inputURI);
		Node var2 = NodeCreateUtils.create("?var2");
		
		if(predicateIsInverse())
			queryPattern = new Triple(var1, var2, input);
		else
			queryPattern = new Triple(input, var1, var2);
			
		Collection<Triple> results = getEndpoint().getTriplesMatchingPattern(queryPattern);
		
		return results;
		*/
	}

	public Collection<Triple> invokeService(String inputURI, String predicate)  throws Exception 
	{
		return invokeService(inputURI);
		
		/*
		if(!predicate.equals(getPredicate()) && !PredicateUtils.invert(predicate).equals(getPredicate()))
			throw new RuntimeException();
		
		Triple queryPattern;
		Node var = NodeCreateUtils.create("?var");
		Node input = NodeCreateUtils.create(inputURI);
		Node pred = NodeCreateUtils.create(getPredicate());
		
		if(predicateIsInverse())
			queryPattern = new Triple(var, pred, input);
		else
			queryPattern = new Triple(input, pred, var);
			
		Collection<Triple> results = getEndpoint().getTriplesMatchingPattern(queryPattern);
		
		return results;
		*/
	}

	public Collection<Triple> invokeService(Literal inputNode) throws ServiceInvocationException 
	{
		return invokeService(inputNode.asNode());
	}

	public Collection<Triple> invokeService(Literal inputNode, String predicate) throws ServiceInvocationException 
	{
		return invokeService(inputNode.asNode());
	}
	
	public Collection<Triple> invokeServiceOnRDFNodes(Collection<? extends RDFNode> inputNodes) throws ServiceInvocationException
	{
		// FIXME
		Collection<Triple> triples = new ArrayList<Triple>();
		for (RDFNode inputNode: inputNodes)
			triples.addAll(invokeService(inputNode.asNode()));
		return triples;
	}

	@Override
	public String toString()
	{
		return getServiceURI();
	}
}
