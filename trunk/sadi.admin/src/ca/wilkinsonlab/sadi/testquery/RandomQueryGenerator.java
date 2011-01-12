package ca.wilkinsonlab.sadi.testquery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.shared.JenaException;

import ca.wilkinsonlab.sadi.vocab.SPARQLRegistryOntology;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry;
import ca.wilkinsonlab.sadi.SADIException;

import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.sparql.ExceededMaxAttemptsException;
import ca.wilkinsonlab.sadi.utils.sparql.NoSampleAvailableException;

public class RandomQueryGenerator {

	public final static Log log = LogFactory.getLog(RandomQueryGenerator.class);
	SPARQLRegistry registry;
	
	protected final static int SPARQL_RESULTS_LIMIT = 300; 

	protected Set<String> deadEndpoints = Collections.synchronizedSet(new HashSet<String>());
	
	public RandomQueryGenerator(SPARQLRegistry registry) {
		setRegistry(registry);
	}

	public List<Triple> generateRandomBasicGraphPattern(int maxConstants, int queryDepth, int maxFanout) throws SADIException, IOException, ExceededMaxAttemptsException 
	{
		// tracks the depth of each edge from the root. 
		Map<Triple,Integer> depth = new HashMap<Triple,Integer>();

		while(true) {

			List<Triple> workingQuery = new ArrayList<Triple>();

			// do a breadth first search starting from a random URI
			log.trace("selecting random starting node for subgraph traversal");
			Node node = getRandomURI();
			
			// breadth-first search state variables
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
					
				workingQuery.add(nextEdge);
				visitedEdges.add(nextEdge);

				currentDepth = depth.get(nextEdge);
				log.trace("current traversal depth: " + currentDepth);

				log.trace("edges selected so far: ");
				for(Triple triple : workingQuery) 
					log.trace(triple.toString());
				
				if(currentDepth < queryDepth) {
					Node newNode;
					if(nextEdge.isInverted()) 
						newNode = nextEdge.getSubject();
					else 
						newNode = nextEdge.getObject();
					
					// can't get outgoing edges from anything but a URI 
					if(newNode.isURI()) {
						//String newNodeStr = RdfUtils.getPlainString(newNode);
						Collection<ReversibleTriple> newNeighbors = getRandomNeighboringEdges(newNode, maxFanout);
						for(Triple triple : newNeighbors) 
							depth.put(triple, depth.get(nextEdge) + 1);
						q.addAll(newNeighbors);
					}
				}
			}

			if(currentDepth < queryDepth || workingQuery.size() == 0) {
				log.trace("Traversal was unable to find a subgraph of the required depth.  Starting a new traversal.");
			}
			else {
				return workingQuery;
			}
		}
	}
	
	public List<Triple> substituteVarsForConstants(List<Triple> triples, int maxConstants) 
	{
		// record the triples where each distinct constant (URI or literal) occurs

		Set<Node> constants = new HashSet<Node>();
		
		for(Triple triple: triples) {
			//String s = RdfUtils.getPlainString(triple.getSubject());
			//String o = RdfUtils.getPlainString(triple.getObject());
			constants.add(triple.getSubject());
			constants.add(triple.getObject());
		}

		// randomly choose a set of constants in the query 

		RandomData generator = new RandomDataImpl();
		//List<String> constantsList = new ArrayList<String>(constants);
		List<Node> constantsList = new ArrayList<Node>(constants);
		//Set<String> chosenConstants = new HashSet<String>();
		Set<Node> chosenConstants = new HashSet<Node>();
		
		for(int i = 0; (i < maxConstants) && (constantsList.size() > 0); i++) {
			int index = constantsList.size() > 1 ? generator.nextInt(0, constantsList.size() - 1) : 0;
			chosenConstants.add(constantsList.get(index));
			constantsList.remove(index);
		}
		
		// build the triple patterns, replacing anything that wasn't chosen as a constant with a variable

		//Map<String,String> constantToVarname = new HashMap<String,String>();
		Map<Node,Node> constantToVar = new HashMap<Node,Node>();
		List<Triple> outputTriples = new ArrayList<Triple>();
		
		for(Triple triple : triples) {

			Node s = triple.getSubject();
			Node p = triple.getPredicate();
			Node o = triple.getObject();

			Node pos[] = { s, o };
			for(int i = 0; i < pos.length; i++) {
				/*
				String str = RdfUtils.getPlainString(pos[i]);
				if(!chosenConstants.contains(str)) {
					String varName;
					if(!constantToVarname.containsKey(str))
						varName = "?var" + constantToVarname.size();
					else
						varName = constantToVarname.get(str);
					pos[i] = NodeCreateUtils.create(varName);
					constantToVarname.put(str, varName);
				}
				*/
				if(!chosenConstants.contains(pos[i])) {
					Node var;
					if(!constantToVar.containsKey(pos[i]))
						var = NodeCreateUtils.create("?var" + constantToVar.size());
					else
						var = constantToVar.get(pos[i]);
					constantToVar.put(pos[i], var);
					pos[i] = var;
				}
			}
			
			outputTriples.add(new Triple(pos[0], p, pos[1]));
		}
		
		return outputTriples;
	}
	
	public Collection<ReversibleTriple> getRandomNeighboringEdges(Node node, int maxFanout) throws SADIException, IOException 
	{
		List<ReversibleTriple> neighborEdges = getOutgoingEdges(node); //getIncomingAndOutgoingEdges(node, maxFanout);
		if(neighborEdges.size() <= 1)
			return neighborEdges;

		Collection<ReversibleTriple> selectedNeighbors = new ArrayList<ReversibleTriple>();
		
		// Don't select more than one edge with the same predicate. This leads to silly test queries
		// like: SELECT * WHERE { <gene_xyz> <inPathway> ?var0 . <gene_xyz> <inPathway> ?var1 }
		// However, it is okay to use the same predicate twice if the starting node is used as
		// the subject in one case, and as the object in the other.  That's why there are 
		// two sets here.
		
		/*
		Set<String> forwardPredicatesVisited = new HashSet<String>(); 
		Set<String> reversePredicatesVisited = new HashSet<String>();
		*/
		RandomData generator = new RandomDataImpl();
		int upperBound = Math.min(neighborEdges.size(), maxFanout);
		int numEdges = generator.nextInt(1, upperBound);
		while(selectedNeighbors.size() < numEdges && neighborEdges.size() > 0) {
			int index = (neighborEdges.size() > 1) ? generator.nextInt(0, neighborEdges.size() - 1) : 0;
			ReversibleTriple candidate = neighborEdges.remove(index);
			//Set<String> predicatesVisited = candidate.isInverted() ? reversePredicatesVisited : forwardPredicatesVisited;
			String predicate = candidate.getPredicate().toString();
			
			// Including the rdf:type predicate often yields queries with
			// an enormous result set.  It is also tends to generate 
			// queries where nodes are related only by their rdf:type.  
			// (And these types of queries aren't very interesting.)
			/*
			if( predicate.equals(W3C.PREDICATE_RDF_TYPE) )
				continue;
			*/
			
			// some predicates may not be indexed in the registry; queries with these edges will not 
			// generate any results, so skip them.
			if(!registry.hasPredicate(predicate)) {
				log.warn("Skipping non-indexed edge: " + predicate);
				continue;
			}
			/*
			if(!predicatesVisited.contains(predicate)) {
				selectedNeighbors.add(candidate);
				predicatesVisited.add(predicate);
			}
			*/
			selectedNeighbors.add(candidate);
		}
		return selectedNeighbors;
	}

	
	public List<ReversibleTriple> getOutgoingEdges(Node node) throws SADIException, IOException
	{
		if(!node.isURI()) {
			log.warn("outgoing edges cannot be retrieved for a blank node or a literal, returning empty list");
			return new ArrayList<ReversibleTriple>();
		}

		SPARQLRegistry registry = getRegistry();
		Collection<SPARQLEndpoint> endpoints = registry.getAllEndpoints();
		Set<ReversibleTriple> triples = new HashSet<ReversibleTriple>();
		List<Triple> outgoingEdges = new ArrayList<Triple>();
		
		String nodeStr = RdfUtils.getPlainString(node);
		String query = "CONSTRUCT { %u% ?p ?o } WHERE { %u% ?p ?o . FILTER (!isBlank(?o)) } LIMIT %v%";
		query = SPARQLStringUtils.strFromTemplate(query, nodeStr, nodeStr, String.valueOf(SPARQL_RESULTS_LIMIT));

		log.debug("query for outgoing edges: " + query);

		for(SPARQLEndpoint endpoint : endpoints) {

			String endpointURI = endpoint.getURI();
			if((registry.getServiceStatus(endpointURI) == ServiceStatus.DEAD) || deadEndpoints.contains(endpointURI))
				continue;
			
			try {
				if(node.isURI() && getRegistry().subjectMatchesRegEx(endpointURI, nodeStr)) {
					log.trace("querying " + endpointURI + " for triples with subject " + nodeStr);
					outgoingEdges.addAll(endpoint.constructQuery(query));
				}
			}
			catch(IOException e) {
				log.warn("failed to query endpoint: " + endpoint.getURI(), e);
				deadEndpoints.add(endpointURI);
			}
			catch(JenaException e) {
				// Jena occasionally barfs when it's parsing RDF/XML from Virtuoso.
				// In this case it's not a big deal; we can carry on.
				log.warn("parsing error", e);
			}
		}
		
		log.trace("found " + outgoingEdges.size() + " results");

		// first add the edges to a set, to avoid duplicate triples from different endpoints
		for(Triple triple : outgoingEdges)
			triples.add(new ReversibleTriple(triple, false));
		
		return new ArrayList<ReversibleTriple>(triples);
	}

	public Node getRandomURI() throws IOException, ExceededMaxAttemptsException
	{
		final int MAX_ATTEMPTS = 20;
		SPARQLEndpoint endpoint = null; 
		Node node = null;
		for(int i = 0; i < MAX_ATTEMPTS; i++) {
			try {
				endpoint = getRandomEndpoint();
				node = getRandomSubjectURI(endpoint);
				break;
			}
			catch(IOException e) {
				log.trace("failed to retrieve random subject URI from " + endpoint.getURI(), e);
				deadEndpoints.add(endpoint.getURI());
			}
			catch(NoSampleAvailableException e) {
				log.trace("failed to retrieve random subject URI from " + endpoint.getURI(), e);
				deadEndpoints.add(endpoint.getURI());
			}
			catch(ExceededMaxAttemptsException e) {
				log.trace("failed to retrieve random subject URI from " + endpoint.getURI(), e);
			}
		}
		
		if(node == null)
			throw new ExceededMaxAttemptsException("failed to obtain random subject URI for traversal, after repeated attempts");

		log.trace("Random starting point: " + node + " from endpoint " + endpoint.getURI());
		return node;
	}
	
	public Node getRandomSubjectURI(SPARQLEndpoint endpoint) throws IOException, NoSampleAvailableException, ExceededMaxAttemptsException
	{
		final int MAX_ATTEMPTS = 5;

		for(int i = 0; i < MAX_ATTEMPTS; i++) {
			Triple triple = getRandomTriple(endpoint);
			if(!triple.getSubject().isBlank())
				return triple.getSubject();
		}
		
		throw new ExceededMaxAttemptsException("exceeded maximum attempts when trying to get a random non-blank-node subject from " + endpoint.getURI());
		
	}

	public Triple getRandomTriple(SPARQLEndpoint endpoint) throws IOException, NoSampleAvailableException
	{
		long numTriples = getRegistry().getNumTriplesOrLowerBound(endpoint.getURI());
		
		if(numTriples == SPARQLRegistryOntology.NO_VALUE_AVAILABLE) {
			deadEndpoints.add(endpoint.getURI());
			throw new NoSampleAvailableException("cannot sample triples from endpoint, because registry does not know number of triples in " + endpoint.getURI());
		}
		
		RandomData generator = new RandomDataImpl();
		long sampleIndex = generator.nextLong(0, numTriples - 1);
		
		log.trace("Querying " + endpoint.getURI() + " for randomly selected triple #" + String.valueOf(sampleIndex));
		String query = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o } OFFSET %v% LIMIT 1";
		query = SPARQLStringUtils.strFromTemplate(query, String.valueOf(sampleIndex));
		Collection<Triple> results = endpoint.constructQuery(query);

		if(results.size() == 0) 
			throw new RuntimeException("query for triple #" + String.valueOf(sampleIndex) + " from " + endpoint.getURI() + " returned no result");
		return results.iterator().next();

	}
	
	public SPARQLEndpoint getRandomEndpoint() throws IOException 
	{
		List<SPARQLEndpoint> endpoints = new ArrayList<SPARQLEndpoint>(getRegistry().getAllEndpoints());
		RandomData generator = new RandomDataImpl();

		while(endpoints.size() > 0) {
			int index = endpoints.size() > 1 ? generator.nextInt(0, endpoints.size() - 1) : 0;
			SPARQLEndpoint endpoint = endpoints.get(index);
			if (!endpoint.ping()) {
				deadEndpoints.add(endpoint.getURI());
				endpoints.remove(index);
				continue;
			}
			return endpoint;
		}
		throw new RuntimeException("failed (after many attempts) to randomly select a SPARQL endpoint for sampling");
	}

	public SPARQLRegistry getRegistry() {
		return registry;
	}

	public void setRegistry(SPARQLRegistry registry) {
		
		if(!(registry instanceof SPARQLRegistry))
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
		
		public boolean isInverted() {
			return inverted;
		}
		public void setInverted(boolean inverted) {
			this.inverted = inverted;
		}
	}
	
}
