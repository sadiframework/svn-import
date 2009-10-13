package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A proxy object which exposes a SPARQL endpoint as a Service.
 * @author Ben Vandervalk
 */
public class SPARQLServiceWrapper implements Service
{
	private static final Log log = LogFactory.getLog(SPARQLServiceWrapper.class);

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
	
	public void initResultsLimit()
	{
		if(Config.getConfiguration().containsKey(RESULTS_LIMIT_CONFIG_KEY)) 
			setResultsLimit(Config.getConfiguration().getLong(RESULTS_LIMIT_CONFIG_KEY));
		else
			setResultsLimit(SPARQLEndpoint.NO_RESULTS_LIMIT);
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
	
	private Collection<Triple> invokeService(Node inputURIorLiteral) throws IOException
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
			
		Collection<Triple> results = getEndpoint().getTriplesMatchingPattern(queryPattern, QUERY_TIMEOUT, getResultsLimit());
		
		return results;
	}

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

	public Collection<Resource> discoverInputInstances(Model inputModel)
	{
		log.warn("discoverInputInstances not yet implemented");
		return new ArrayList<Resource>(0);
	}

	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServiceURI() {
		return getEndpoint().getURI();
	}

	public Collection<Triple> invokeService(Resource inputNode) throws Exception 
	{
		return invokeService(inputNode.asNode());
	}

	public Collection<Triple> invokeService(Resource inputNode, String predicate) throws Exception 
	{
		return invokeService(inputNode.asNode());
	}

	public Collection<Triple> invokeService(Literal inputNode) throws IOException 
	{
		return invokeService(inputNode.asNode());
	}

	public Collection<Triple> invokeService(Literal inputNode, String predicate) throws IOException 
	{
		return invokeService(inputNode.asNode());
	}

	@Override
	public String toString()
	{
		return getServiceURI();
	}

}
