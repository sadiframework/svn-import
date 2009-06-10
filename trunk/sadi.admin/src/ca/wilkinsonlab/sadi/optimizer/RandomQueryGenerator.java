package ca.wilkinsonlab.sadi.optimizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;

import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;
import ca.wilkinsonlab.sadi.vocab.W3C;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.sparql.SPARQLRegistry;
import ca.wilkinsonlab.sadi.sparql.SPARQLService;
import ca.wilkinsonlab.sadi.sparql.VirtuosoSPARQLRegistry;

import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

public class RandomQueryGenerator {

	public final static Log LOGGER = LogFactory.getLog(RandomQueryGenerator.class);
	SPARQLRegistry registry;

	public RandomQueryGenerator(SPARQLRegistry registry) {
		setRegistry(registry);
	}

	public List<Triple> generateRandomBasicGraphPattern(int maxConstants, int queryDepth, int maxFanout) throws HttpException, HttpResponseCodeException, IOException 
	{
		
		// Tracks the depth of each edge from the root. 
		Map<Triple,Integer> depth = new HashMap<Triple,Integer>();

		while(true) {

			List<Triple> workingQuery = new ArrayList<Triple>();

			// Do a breadth first search starting from a random URI
			LOGGER.trace("Selecting random starting node...");
			String node = getRandomURI();
			
			// Breadth-first search state variables
			Set<Triple> visitedEdges = new HashSet<Triple>();
			Queue<ReversibleTriple> q = new LinkedList<ReversibleTriple>();
			Collection<ReversibleTriple> neighbors = getRandomNeighboringEdges(node, maxFanout);
			for(Triple triple : neighbors) 
				depth.put(triple, 1);
			q.addAll(neighbors);
			
			int currentDepth = 1;
			
			while(!q.isEmpty()) {

				ReversibleTriple nextEdge = q.remove();

				if(visitedEdges.contains(nextEdge))
					continue;
				
				// Some predicates will have been missed in the indexing. Queries with these edges will not generate any results.
				/*
				String pred = nextEdge.getPredicate().toString();
				if(!registry.hasPredicate(pred)) {
					LOGGER.warn("Skipping non-indexed edge: " + pred);
					continue;
				}
				*/
				
				currentDepth = depth.get(nextEdge);
				LOGGER.trace("Current depth: " + currentDepth);
				
				workingQuery.add(nextEdge);
				visitedEdges.add(nextEdge);
				
				LOGGER.trace("Edges selected so far: ");
				for(Triple triple : workingQuery) 
					LOGGER.trace(triple.toString());
				
				if(currentDepth < queryDepth) {
					Node newNode;
					if(nextEdge.isInverted()) 
						newNode = nextEdge.getSubject();
					else 
						newNode = nextEdge.getObject();

					String newNodeStr = RdfUtils.getPlainString(newNode);
					Collection<ReversibleTriple> newNeighbors = getRandomNeighboringEdges(newNodeStr, maxFanout);
					for(Triple triple : newNeighbors) 
						depth.put(triple, depth.get(nextEdge) + 1);
					q.addAll(newNeighbors);
				}
			}

			if(currentDepth < queryDepth) {
				LOGGER.trace("Traversal was unable to find a subgraph of the required depth.  Starting a new traversal.");
			}
			else {
				//List<Triple> finalQuery = substituteVarsForConstants(workingQuery, maxConstants);
				//return finalQuery;
				return workingQuery;
			}
		}
	}
	
	public List<Triple> substituteVarsForConstants(List<Triple> triples, int maxConstants) 
	{
		// Record the triples where each distinct constant (URI or literal) occurs

		Map<String, Set<Integer>> occurences = new HashMap<String, Set<Integer>>();
		List<String> constants = new LinkedList<String>();
		
		for(int i = 0; i < triples.size(); i++) {
			Triple triple = triples.get(i);
			String s = RdfUtils.getPlainString(triple.getSubject());
			String o = RdfUtils.getPlainString(triple.getObject());
			
			String pos[] = { s, o };
			for(int j = 0; j < pos.length; j++) {
				String constant = pos[j];
				constants.add(constant);
				Set<Integer> set;
				if(!occurences.containsKey(constant)) {
					set = new HashSet<Integer>();
					occurences.put(constant, set);
				}
				else {
					set = occurences.get(constant);
				}
				set.add(i);
			}
		}
		
		// Randomly choose a set of constants, ensuring that no triple patterns in the
		// resulting query will have both a constant subject and a constant object
		
		Set<Integer> triplesWithAConstant = new HashSet<Integer>();
		Set<String> chosenConstants = new HashSet<String>();
		
		RandomData generator = new RandomDataImpl();
		
		while(triplesWithAConstant.size() < triples.size() && 
			  chosenConstants.size() < maxConstants &&
			  constants.size() > 0) 
		{
			int index = constants.size() > 1 ? generator.nextInt(0, constants.size() - 1) : 0;
			String candidate = constants.remove(index);
			boolean candidateValid = true;
			for(Integer i : occurences.get(candidate)) {
				if(triplesWithAConstant.contains(i)) {
					candidateValid = false;
					break;
				}
			}
			
			if(candidateValid) {
				chosenConstants.add(candidate);
				triplesWithAConstant.addAll(occurences.get(candidate));
			}
		}
		
		// Build the triple patterns, replacing anything that wasn't chosen as a constant with a variable.
		
		Map<String,String> constantToVarname = new HashMap<String,String>();
		List<Triple> outputTriples = new ArrayList<Triple>();
		
		for(Triple triple : triples) {

			Node s = triple.getSubject();
			Node p = triple.getPredicate();
			Node o = triple.getObject();

			Node pos[] = { s, o };
			for(int i = 0; i < pos.length; i++) {
				String str = pos[i].toString();
				if(!chosenConstants.contains(str)) {
					String varName;
					if(!constantToVarname.containsKey(str))
						varName = "?var" + constantToVarname.size();
					else
						varName = constantToVarname.get(str);
					pos[i] = NodeCreateUtils.create(varName);
					constantToVarname.put(str, varName);
				}
			}
			
			outputTriples.add(new Triple(pos[0], p, pos[1]));
		}
		
		return outputTriples;
	}
	
	public Collection<ReversibleTriple> getRandomNeighboringEdges(String node, int maxFanout) throws HttpException, HttpResponseCodeException, IOException
	{
		List<ReversibleTriple> neighborEdges = getIncomingAndOutgoingEdges(node, maxFanout);
		if(neighborEdges.size() <= 1)
			return neighborEdges;

		Collection<ReversibleTriple> selectedNeighbors = new ArrayList<ReversibleTriple>();
		
		// Don't select more than one edge with the same predicate. This leads to silly test queries
		// like: SELECT * WHERE { <gene_xyz> <inPathway> ?var0 . <gene_xyz> <inPathway> ?var1 }
		// However, it is okay to use the same predicate twice if the starting node is used as
		// the subject in one case, and as the object in the other.  That's why there are 
		// two sets here.
		
		Set<String> forwardPredicatesVisited = new HashSet<String>(); 
		Set<String> reversePredicatesVisited = new HashSet<String>();
		
		RandomData generator = new RandomDataImpl();
		int upperBound = Math.min(neighborEdges.size(), maxFanout);
		int numEdges = generator.nextInt(1, upperBound);
		while(selectedNeighbors.size() < numEdges && neighborEdges.size() > 0) {
			int index = (neighborEdges.size() > 1) ? generator.nextInt(0, neighborEdges.size() - 1) : 0;
			ReversibleTriple candidate = neighborEdges.remove(index);
			Set<String> predicatesVisited = candidate.isInverted() ? reversePredicatesVisited : forwardPredicatesVisited;
			String predicate = candidate.getPredicate().toString();
			
			// Including the rdf:type predicate often yields queries with
			// an enormous result set.  It is also tends to generate 
			// queries where nodes are related only by their rdf:type.  
			// (And these types of queries aren't very interesting.)
			if( predicate.equals(W3C.PREDICATE_RDF_TYPE) )
				continue;
			
			if(!predicatesVisited.contains(predicate)) {
				selectedNeighbors.add(candidate);
				predicatesVisited.add(predicate);
			}
		}
		return selectedNeighbors;
	}
	
	/**
	 * 
	 * @param node Can be a URI or a literal
	 * @return
	 */
	public List<ReversibleTriple> getIncomingAndOutgoingEdges(String node, int maxFanout) throws HttpException, HttpResponseCodeException, IOException
	{
		Collection<? extends Service> endpoints = getRegistry().getAllServices();

		// Use a set to avoid duplicate triples
		Set<ReversibleTriple> triples = new HashSet<ReversibleTriple>();

		String subjectQuery = null;
		
		String objectQuery;
		if(RdfUtils.isURI(node)) {
			subjectQuery = "CONSTRUCT { %u% ?p ?o } WHERE { %u% ?p ?o } LIMIT %v%";
			subjectQuery = SPARQLStringUtils.strFromTemplate(subjectQuery, node, node, String.valueOf(maxFanout));
			objectQuery = "CONSTRUCT { ?s ?p %u% } WHERE { ?s ?p %u% } LIMIT %v%";
		}
		else if(NumberUtils.isNumber(node)) {
			objectQuery = "CONSTRUCT { ?s ?p %v% } WHERE { ?s ?p %v% } LIMIT %v%";
		}
		else {
			objectQuery = "CONSTRUCT { ?s ?p %s% } WHERE { ?s ?p %s% } LIMIT %v%"; 
		}
		
		objectQuery = SPARQLStringUtils.strFromTemplate(objectQuery, node, node, String.valueOf(maxFanout));
		
		LOGGER.debug("subjectQuery: " + subjectQuery);
		LOGGER.debug("objectQuery: " + objectQuery);
		
		List<Triple> outgoingResults = new ArrayList<Triple>();
		List<Triple> incomingResults = new ArrayList<Triple>();

		for(Service endpoint : endpoints) {
			LOGGER.trace("Querying " + endpoint.getServiceURI() + " for triples related to " + node);
			try {
				if(RdfUtils.isURI(node)) {
					outgoingResults.addAll(((SPARQLEndpoint)endpoint).constructQuery(subjectQuery, 40 * 1000));
				}
				incomingResults.addAll(((SPARQLEndpoint)endpoint).constructQuery(objectQuery, 40 * 1000));
			}
			catch(Exception e) {
				LOGGER.warn("Failed to query endpoint: " + endpoint.getServiceURI(), e);
			}
		}
		
		LOGGER.trace("Found " + (incomingResults.size() + outgoingResults.size()) + " results");

		for(Triple triple : incomingResults)
			triples.add(new ReversibleTriple(triple, true));
		
		for(Triple triple : outgoingResults)
			triples.add(new ReversibleTriple(triple, false));
		
		return new ArrayList<ReversibleTriple>(triples);
	}

	public String getRandomURI() throws HttpException, HttpResponseCodeException, IOException
	{
		int maxTries = 10;
		SPARQLService endpoint = null; 
		String node = null;
		for(int i = 0; i < maxTries; i++) {
			try {
				endpoint = getRandomEndpoint(registry);
				node = getRandomSubjectURI(endpoint);
				break;
			}
			catch(Exception e) {
				LOGGER.trace("Failed to query random starting node from random endpoint", e);
//				if(!HttpHelper.isHTTPTimeout(e))
//					throw e;
			}
		}
		if(node == null)
			throw new RuntimeException("Failed to obtain random starting URI for traversal, after repeated attempts");

		LOGGER.trace("Random starting point: " + node + " from endpoint " + endpoint.getServiceURI());
		return node;
	}
	
	public String getRandomSubjectURI(SPARQLService endpoint) throws HttpException, HttpResponseCodeException, IOException 
	{
		Triple triple = getRandomTriple(endpoint);
		return triple.getSubject().toString();
	}

	public Triple getRandomTriple(SPARQLService endpoint) throws HttpException, HttpResponseCodeException, IOException 
	{
		SPARQLRegistry registry = getRegistry();
		long numTriples = registry.getNumTriples(endpoint.getServiceURI());
		RandomData generator = new RandomDataImpl();
		String query;
		Collection<Triple> results = null;
		long sampleIndex = generator.nextLong(0, numTriples - 1);
		LOGGER.trace("Querying " + endpoint.getServiceURI() + " for random subject URI #" + String.valueOf(sampleIndex));
		query = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o } OFFSET %v% LIMIT 1";
		query = SPARQLStringUtils.strFromTemplate(query, String.valueOf(sampleIndex));
		results = endpoint.constructQuery(query, 240 * 1000);
		if(results == null || results.size() == 0) 
			throw new RuntimeException("Query for triple #" + String.valueOf(sampleIndex) + " from " + endpoint.getURI() + " returned no result");
		return results.iterator().next();
	}

	public SPARQLService getRandomEndpoint(SPARQLRegistry registry) throws HttpException, HttpResponseCodeException, IOException 
	{
		Collection<? extends Service> endpoints = registry.getAllServices();
		Object endpointsArray[] = endpoints.toArray();
		RandomData generator = new RandomDataImpl();
		int maxTries = endpoints.size();
		for (int i = 0; i < maxTries; i++) {
			int index = generator.nextInt(0, endpoints.size() - 1);
			SPARQLService endpoint = (SPARQLService) endpointsArray[index];
			if (registry.getServiceStatus(endpoint.getServiceURI()) != ServiceStatus.DEAD)
				return endpoint;
		}
		throw new RuntimeException("Failed (after multiple attempts) to randomly select a SPARQL endpoint for sampling.");
	}

	public SPARQLRegistry getRegistry() {
		return registry;
	}

	public void setRegistry(SPARQLRegistry registry) {
		
		if(!(registry instanceof VirtuosoSPARQLRegistry))
			throw new RuntimeException("The random query generator can only generate queries using a SPARQL endpoint registry");
		
		this.registry = registry;
	}
	
	private static class ReversibleTriple extends Triple {
		boolean inverted;
		
		public ReversibleTriple(Triple triple, boolean inverted) 
		{
			super(triple.getSubject(), triple.getPredicate(), triple.getObject());
			setInverted(inverted);
		}
		
		public ReversibleTriple(Node s, Node p, Node o, boolean inverted) 
		{
			super(s, p, o);
			setInverted(inverted);
		}
		
		/*
		static ReversibleTriple create(String s, String p, String o, boolean inverted)
		{
			Triple triple = RdfUtils.createTriple(s, p, o);
			return new ReversibleTriple(triple, inverted);
		}
		*/
		
		public boolean isInverted() {
			return inverted;
		}
		public void setInverted(boolean inverted) {
			this.inverted = inverted;
		}
	}
	
}
