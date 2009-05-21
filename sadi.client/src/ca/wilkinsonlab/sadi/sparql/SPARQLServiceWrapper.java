package ca.wilkinsonlab.sadi.sparql;

import java.util.Collection;

import ca.wilkinsonlab.sadi.client.Service;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * 
 * @author Ben Vandervalk
 */
public class SPARQLServiceWrapper implements Service
{
	SPARQLService endpoint;
	String predicate;
	boolean predicateIsInverse;

	public SPARQLServiceWrapper(SPARQLService endpoint, String predicate, boolean predicateIsInverse) 
	{
		setService(endpoint);
		setPredicate(predicate);
		setPredicateIsInverse(predicateIsInverse);
	}

	public String getServiceURI() {
		return getEndpoint().getServiceURI();
	}

	public Collection<Triple> invokeService(String inputURI) throws Exception 
	{
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
	}

	public Collection<Triple> invokeService(String inputURI, String predicate)
			throws Exception {

		if(!predicate.equals(getPredicate()) || PelletHelper.invert(predicate).equals(getPredicate()))
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
	}

	/*
	 * protected Collection<Triple> translateResults(Collection<Triple>
	 * results) { Collection<Triple> translatedResults = new ArrayList<Triple>();
	 * for(Triple triple : results) { Node s, p, o;
	 * if(!triple.getPredicate().toString().equals(getMatchingPredicate()))
	 * throw new RuntimeException(); p =
	 * NodeCreateUtils.create(getRequestedPredicate());
	 * if(matchingPredicateIsInverse()) { s = triple.getObject(); o =
	 * triple.getSubject(); } else { s = triple.getSubject(); o =
	 * triple.getObject(); } translatedResults.add(new Triple(s,p,o)); }
	 * 
	 * return translatedResults; }
	 */

	public Collection<Triple> invokeService(Model inputRDF) throws Exception {
		throw new RuntimeException("This method is not supported.");
	}

	public Collection<Triple> invokeService(Model inputRDF, String predicate)
			throws Exception {
		throw new RuntimeException("This method is not supported.");
	}

	public SPARQLService getEndpoint() {
		return endpoint;
	}

	public void setService(SPARQLService endpoint) {
		this.endpoint = endpoint;
	}

	public boolean predicateIsInverse() {
		return predicateIsInverse;
	}

	public void setPredicateIsInverse(boolean predicateInverted) {
		this.predicateIsInverse = predicateInverted;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<Triple> invokeService(Resource inputNode) throws Exception
	{
		return invokeService(inputNode.getURI());
	}

	public Collection<Triple> invokeService(Resource inputNode, String predicate) throws Exception
	{
		return invokeService(inputNode.getURI(), predicate);
	}
	
	public String toString()
	{
		return getServiceURI();
	}
}
