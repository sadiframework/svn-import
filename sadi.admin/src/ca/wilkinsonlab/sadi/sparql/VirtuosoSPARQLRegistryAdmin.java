package ca.wilkinsonlab.sadi.sparql;

import gnu.getopt.Getopt;

import java.io.IOException;
import java.rmi.AccessException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Property;

import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;
import ca.wilkinsonlab.sadi.utils.HttpUtils;
import ca.wilkinsonlab.sadi.vocab.SPARQLRegistryOntology;
import ca.wilkinsonlab.sadi.vocab.W3C;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpointType;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpointFactory;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint;

/**
 * Class for performing administrative tasks on a 
 * Virtuoso SPARQL endpoint registry (adding new endpoints,
 * reindexing existing endpoints, etc.)
 * 
 * The structure of the RDF in the registry is as follows.
 * 
 * In the graph <http://sparqlreg/endpoints/>:
 * -------------------------------------------
 *   
 * <endpointURI> ==indexComputed==> <0 | 1>
 * <endpointURI> ==hasPredicate==> <predicateURI>
 *
 * In the graph <http://sparqlreg/ontology/>:
 * -------------------------------------------
 * 
 * <predicateURI> ==rdf:type==> <DatatypeProperty | ObjectProperty>
 *  
 * 'indexComputed' indicates whether the index for a particular
 * endpoint has been successfully built.   The most
 * common cause of failure is an HTTP timeout when querying the
 * endpoint for a list of predicates.  In the Pellet/SADI SPARQL engine,
 * ALL unindexed endpoints are queried for ALL triple patterns during
 * query resolution (regardless of what the patterns are).
 * 
 * @author Ben Vandervalk
 */
public class VirtuosoSPARQLRegistryAdmin extends VirtuosoSPARQLRegistry implements SPARQLRegistryAdmin {

	public final static Log LOGGER = LogFactory.getLog(VirtuosoSPARQLRegistryAdmin.class);

	public VirtuosoSPARQLRegistryAdmin() throws HttpException, IOException 
	{
		super();
	}
	
	public VirtuosoSPARQLRegistryAdmin(String URI) throws HttpException, IOException 
	{
		super(URI);
	}
	
	public VirtuosoSPARQLRegistryAdmin(String URI, String indexGraphURI, String ontologyGraphURI) throws HttpException, IOException 
	{
		super(URI, indexGraphURI, ontologyGraphURI);
	}
	
	public void clearRegistry() throws HttpException, HttpResponseCodeException, IOException, AccessException 
	{
		clearIndexes();
		clearOntology();
	}

	public void clearIndexes() throws HttpException, HttpResponseCodeException, IOException, AccessException 
	{
		LOGGER.trace("Clearing indexes from registry at " + getURI());
		String query = SPARQLStringUtils.strFromTemplate("CLEAR GRAPH %u%", getIndexGraphURI());
		updateQuery(query);
	}

	public void clearOntology() throws HttpException, HttpResponseCodeException, IOException, AccessException
	{
		LOGGER.trace("Clearing predicate ontology from registry at " + getURI());
		String query = SPARQLStringUtils.strFromTemplate("CLEAR GRAPH %u%", getOntologyGraphURI());
		updateQuery(query);
	}
	
	public void addEndpoint(String endpointURI, SPARQLEndpointType type) throws HttpException, HttpResponseCodeException, IOException, AccessException
	{
		// clear any pre-existing index information.
		removeEndpoint(endpointURI);
		// record the type of endpoint.
		addEndpointTypeToIndex(endpointURI, type);
		// record the fact that we have not yet computed an index
		setIndexStatus(endpointURI, false);
	}
	
	public void addAndIndexEndpoint(String endpointURI, SPARQLEndpointType type) throws HttpException, HttpResponseCodeException, IOException, AccessException
	{
		LOGGER.trace("Indexing SPARQL endpoint " + endpointURI);

		boolean computedFullIndex = false;
		ServiceStatus endpointStatus = ServiceStatus.DEAD;

		// clear any pre-existing index information.
		removeEndpoint(endpointURI);

		// assume the worst until proven otherwise
		setEndpointStatus(endpointURI, endpointStatus);
		// indicates whether we have successfully computed the full index for this endpoint.  Set to true below.
		setIndexStatus(endpointURI, false);
		// record the type of endpoint.
		addEndpointTypeToIndex(endpointURI, type);

		// query the endpoint for a list of distinct predicates
		computedFullIndex = buildPredicateIndex(endpointURI, type);
		// query the endpoint for the number of triples
		initEndpointSize(endpointURI, type);
		
		endpointStatus = computedFullIndex ? ServiceStatus.OK : ServiceStatus.SLOW;
				
		setEndpointStatus(endpointURI, endpointStatus);
		setIndexStatus(endpointURI, computedFullIndex);
		
		if(computedFullIndex)
			LOGGER.trace("Index for SPARQL endpoint " + endpointURI + " successfully computed");
	}
	
	public boolean buildPredicateIndex(String endpointURI, SPARQLEndpointType type) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException 
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		Set<String> predicateURIs = null;
		boolean retrievedFullPredicateList = false;

		try {
			predicateURIs = endpoint.getPredicates();
			retrievedFullPredicateList = true;
		}
		catch(Exception e) {
			LOGGER.warn("Failed to get full predicate list for endpoint " + endpointURI + ": " + e);
			predicateURIs = endpoint.getPredicatesPartial();
		}
		
		for(String predicateURI : predicateURIs) {

			boolean isDatatypeProperty = false;
			try {
				isDatatypeProperty = endpoint.isDatatypeProperty(predicateURI);
			}
			catch(IOException e) {
				LOGGER.warn("Failed to determine whether " + predicateURI + " is a datatype or object property. Omitting this predicate from the endpoint index.");
				continue;
			}
			
			addPredicateToIndex(endpointURI, predicateURI);
			// record whether this predicate is a datatype property or an object property
			addPredicateToOntology(predicateURI, isDatatypeProperty);
		}
		
		return retrievedFullPredicateList;
	}
	
	public boolean initEndpointSize(String endpointURI, SPARQLEndpointType type)  throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		boolean retrievedFullTripleCount = false;
		long numTriples;
		
		try {
			numTriples = getNumTriples(endpoint);
			retrievedFullTripleCount = true;
		}
		catch(Exception e) {
			LOGGER.warn("Failed to query full size (in triples) of endpoint " + endpoint.getURI());
			numTriples = getNumTriplesLowerBound(endpoint);
		}

		setNumTriples(endpointURI, numTriples);
		
		return retrievedFullTripleCount;
	}
	
	/*
	public ServiceStatus rebuildPredicateIndex(String endpointURI, SPARQLEndpointType type)  throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException
	{
		return rebuildPredicateIndex(endpointURI, type, true);
	}
	
	public ServiceStatus rebuildPredicateIndex(String endpointURI, SPARQLEndpointType type, boolean refresh) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException 
	{
		LOGGER.trace("Rebuilding predicate index for: " + endpointURI);
		
		if(refresh && !hasEndpoint(endpointURI)) {
			throw new IllegalArgumentException("Attempted to refresh predicate list for " + endpointURI +
					", but an index for that endpoint doesn't exist.  Use addAndIndexEndpoint() instead.");
		}
		
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		ServiceStatus endpointStatus = ServiceStatus.DEAD;
		
		clearPredicateEntries(endpointURI);
		
		if(refresh) {
			// Assume the worst until we prove otherwise below.
			setEndpointStatus(endpointURI, ServiceStatus.DEAD);
			setIndexStatus(endpointURI, false);
		}

		Set<String> predicateURIs = null;
		
		try {
			predicateURIs = endpoint.getPredicates();
			endpointStatus = ServiceStatus.OK;
		}
		catch(Exception e) {
			LOGGER.warn("Failed to query full predicate list for endpoint " + endpointURI + ": " + e);
			predicateURIs = endpoint.getPredicatesPartial();

			// NOTE: We should really set computedIndex to false here, because we 
			// weren't successful in getting the full list of predicates from the endpoint.
			// For the time being, I am assuming that all indexes have been computed in their
			// entirety. 
			//
			// The alternative (correct) approach is to query all endpoints that
			// are not fully indexed, for every triple pattern that appears in a user query.
			// In practice, the performance implications of this are disastrous, because only
			// about half of the Bio2RDF endpoints can be fully indexed. -- BV
			
			endpointStatus = ServiceStatus.SLOW;
		}
		
		for(String predicateURI : predicateURIs) {
			addPredicateToIndex(endpointURI, predicateURI);
			// record whether this predicate is a datatype property or an object property
			addPredicateToOntology(predicateURI, endpoint.isDatatypeProperty(predicateURI));
		}

		if(refresh) {
			setIndexStatus(endpointURI, true);
			setEndpointStatus(endpointURI, endpointStatus);
		}
		
		return endpointStatus;
	}
	*/
	
	public void clearPredicateEntries(String endpointURI) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException 
	{
		String query = "DELETE FROM GRAPH %u% { %u% %u% ?o } FROM %u% WHERE { %u% %u% ?o }";
		query = SPARQLStringUtils.strFromTemplate(query,
				getIndexGraphURI(),
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_HASPREDICATE,
				getIndexGraphURI(),
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_HASPREDICATE);
	}
	
	/*
	public ServiceStatus refreshEndpointSize(String endpointURI, SPARQLEndpointType type) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException
	{
		return refreshEndpointSize(endpointURI, type, true);
	}
	
	public ServiceStatus refreshEndpointSize(String endpointURI, SPARQLEndpointType type, boolean update) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException
	{
		LOGGER.trace("Updating endpoint size (number of triples) for: " + endpointURI);
		
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		long numTriples;

		ServiceStatus endpointStatus = ServiceStatus.DEAD;
		
		if(update) {
			// Assume the worst until we prove otherwise below.
			setEndpointStatus(endpointURI, ServiceStatus.DEAD);
			setIndexStatus(endpointURI, false);
		}
		
		try {
			numTriples = getNumTriples(endpoint);
			endpointStatus = ServiceStatus.OK;
		}
		catch(Exception e) {
			LOGGER.warn("Failed to query full size (in triples) of endpoint " + endpoint.getServiceURI());
			numTriples = getNumTriplesLowerBound(endpoint);
			endpointStatus = ServiceStatus.SLOW;
		}
		
		setNumTriples(endpointURI, numTriples);

		if(update) {
			setEndpointStatus(endpointURI, endpointStatus);
			setIndexStatus(endpointURI, true);
		}
		
		return endpointStatus;
	}
	*/
	
	private void addEndpointTypeToIndex(String endpointURI, SPARQLEndpointType type) throws HttpException, HttpResponseCodeException, IOException, AccessException
	{
		String queryAddEndpointType = "INSERT INTO GRAPH %u% { %u% %u% %s%^^%u% }";
		queryAddEndpointType = SPARQLStringUtils.strFromTemplate(queryAddEndpointType,
			getIndexGraphURI(),
			endpointURI,
			W3C.PREDICATE_RDF_TYPE,
			type.toString(),
			XSDDatatype.XSDstring.getURI());
		updateQuery(queryAddEndpointType);
	}
	
	private void setIndexStatus(String endpointURI, boolean indexStatus) throws HttpException, HttpResponseCodeException, IOException, AccessException 
	{
		// Delete the existing status value
		String queryDeletePrev = "DELETE FROM GRAPH %u% { %u% %u% ?o } FROM %u% WHERE { %u% %u% ?o }";
		queryDeletePrev = SPARQLStringUtils.strFromTemplate(queryDeletePrev, 
				getIndexGraphURI(), 
				endpointURI, 
				SPARQLRegistryOntology.PREDICATE_COMPUTEDINDEX,
				getIndexGraphURI(),
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_COMPUTEDINDEX);
		updateQuery(queryDeletePrev);

		// Write the new status value
		String queryRecordSuccess = "INSERT INTO GRAPH %u% { %u% %u% %s%^^%u% }";
		String status = indexStatus ? "true" : "false";
		queryRecordSuccess = SPARQLStringUtils.strFromTemplate(queryRecordSuccess,
				getIndexGraphURI(),
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_COMPUTEDINDEX,
				status,
				XSDDatatype.XSDboolean.getURI());
		updateQuery(queryRecordSuccess);
	}

	public void setEndpointStatus(String endpointURI, ServiceStatus status) throws HttpException, HttpResponseCodeException, IOException, AccessException 
	{
		// Delete the existing status value
		String queryDeletePrev = "DELETE FROM GRAPH %u% { %u% %u% ?o } FROM %u% WHERE { %u% %u% ?o }";
		queryDeletePrev = SPARQLStringUtils.strFromTemplate(queryDeletePrev, 
				getIndexGraphURI(), 
				endpointURI, 
				SPARQLRegistryOntology.PREDICATE_ENDPOINTSTATUS,
				getIndexGraphURI(),
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_ENDPOINTSTATUS);
		updateQuery(queryDeletePrev);

		// Write the new status value
		String queryRecordSuccess = "INSERT INTO GRAPH %u% { %u% %u% %s% }";
		queryRecordSuccess = SPARQLStringUtils.strFromTemplate(queryRecordSuccess,
				getIndexGraphURI(),
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_ENDPOINTSTATUS,
				status.toString());
		updateQuery(queryRecordSuccess);
	}
	
	private void addNamedGraphToIndex(String endpointURI, String graphURI) throws HttpException, HttpResponseCodeException, IOException, AccessException  
	{
		String uniqueGraphURI = getUniqueGraphURI(endpointURI, graphURI);
		if(uniqueGraphURI == null)
			throw new RuntimeException("Failed to generate a unique graph URI for the graph <" + graphURI + ">");
		// We have to use %v% for the graph URI because Virtuoso graph URIs are not
		// necessarily properly formatted URIs.
		String queryAddGraphURI = "INSERT INTO GRAPH %u% { %u% %u% <%v%> }";
		queryAddGraphURI = SPARQLStringUtils.strFromTemplate(queryAddGraphURI, 
				getIndexGraphURI(), 
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_HASGRAPH,
				uniqueGraphURI);
		updateQuery(queryAddGraphURI);
	}

	private void addPredicateToIndex(String endpointURI, String graphURI, String predicateURI) throws HttpException, HttpResponseCodeException, IOException, AccessException 
	{
		String uniqueGraphURI = getUniqueGraphURI(endpointURI, graphURI);
		String queryAddPredURI = "INSERT INTO GRAPH %u% { <%v%> %u% %u% }";
		queryAddPredURI = SPARQLStringUtils.strFromTemplate(queryAddPredURI, 
				getIndexGraphURI(),
				uniqueGraphURI,
				SPARQLRegistryOntology.PREDICATE_HASPREDICATE,
				predicateURI);
		updateQuery(queryAddPredURI);
	}

	private void addPredicateToIndex(String endpointURI, String predicateURI) throws HttpException, HttpResponseCodeException, IOException, AccessException 
	{
		String queryAddPredURI = "INSERT INTO GRAPH %u% { %u% %u% %u% }";
		queryAddPredURI = SPARQLStringUtils.strFromTemplate(queryAddPredURI, 
				getIndexGraphURI(),
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_HASPREDICATE,
				predicateURI);
		updateQuery(queryAddPredURI);
	}

	/**
	 * <p>Generate a URI which uniquely identifies a graphURI within an endpoint.
	 * We want the RDF graphs describing different endpoints in the registry
	 * to be disjoint.  As a result, we need to make sure that two endpoints
	 * do not reference the same graph URI.</p>  
	 * 	
	 * @param endpointURI
	 * @param graphURI
	 * @return a graph URI which is guaranteed to be unique between endpoints.
	 */
	private String getUniqueGraphURI(String endpointURI, String graphURI)
	{
		// Make sure that neither of the URIs contain commas or round brackets.
		// (This will screw things up when we try to recover the original graph URI.)
		char badChars[] = {'(',',',')'};
		if( StringUtils.indexOfAny(endpointURI,badChars) >= 0 || 
			StringUtils.indexOfAny(graphURI,badChars) >= 0 ) 
		{
			throw new RuntimeException("Internal error: Unable to generate unique graph URI from (" + endpointURI + ", " + graphURI + ")");
		}
		return new String("(" + endpointURI + "," + graphURI + ")");
	}
	
	/**
	 * Extract a graphURI from a "unique graph URI".  See the method 
	 * "getUniqueGraphURI" in this class for an explanation.  
	 * 
	 * @param uniqueGraphURI
	 * @return the real graph URI corresponding to a unique graph URI.
	 */
	private String getGraphURIFromUniqueGraphURI(String uniqueGraphURI)
	{
		int startIndex = StringUtils.indexOfAnyBut(uniqueGraphURI, "(");
		int endIndex = StringUtils.indexOfAnyBut(uniqueGraphURI, ",");
		if(startIndex >= 0 && endIndex >= 0 && endIndex > startIndex)
			return StringUtils.substring(uniqueGraphURI, startIndex, endIndex);
		else
			return null;
	}
	
	public void refreshStatusOfEndpoints() throws HttpException, HttpResponseCodeException, AccessException, IOException
	{
		Collection<SPARQLService> endpoints = getAllServices();
		for(SPARQLService endpoint : endpoints) {
			try {
				endpoint.getPredicates();
				setEndpointStatus(endpoint.getServiceURI(), ServiceStatus.OK);
			}
			catch(IOException e) {
				if(HttpUtils.isHTTPTimeout(e))
					setEndpointStatus(endpoint.getServiceURI(), ServiceStatus.SLOW);
				else
					setEndpointStatus(endpoint.getServiceURI(), ServiceStatus.DEAD);
			}
			catch(Exception e) {
				setEndpointStatus(endpoint.getServiceURI(), ServiceStatus.DEAD);
			}
		}
	}
	
	/*
	public void refreshEndpointSizes() throws HttpException, HttpResponseCodeException, AccessException, IOException
	{n
		Collection<Service> endpoints = getServices();
		for(Service endpoint : endpoints) {
			String endpointURI = endpoint.getServiceURI();
			try {
				refreshEndpointSize(endpointURI, getEndpointType(endpointURI));
			}
			catch(Exception e) {
				LOGGER.warn("Failed to refresh endpoint size (number of triples): ", e);
			}
		}
	}
	*/
	
	public void removeEndpoint(String endpointURI) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException
	{
		deleteDirectedClosure(endpointURI, getIndexGraphURI());
	}


	public void addPredicateToOntology(String predicateURI, boolean isDatatypeProperty) throws HttpException, HttpResponseCodeException, IOException, AccessException
	{
		String type;
		if(isDatatypeProperty)
			type = W3C.OWL_PREFIX + "DatatypeProperty";
		else
			type = W3C.OWL_PREFIX + "ObjectProperty";
		
		// Remove the previously assigned type (if any).
		String deleteQuery = "DELETE FROM GRAPH %u% { %u% %u% ?type } WHERE { %u% %u% ?type }";
		deleteQuery = SPARQLStringUtils.strFromTemplate(deleteQuery, 
				getOntologyGraphURI(),
				predicateURI,
				W3C.PREDICATE_RDF_TYPE,
				predicateURI,
				W3C.PREDICATE_RDF_TYPE);
		updateQuery(deleteQuery);
				
		// Insert the new value.
		String addPredQuery = "INSERT INTO GRAPH %u% { %u% %u% %u% }";
		addPredQuery = SPARQLStringUtils.strFromTemplate(addPredQuery, 
				getOntologyGraphURI(),
				predicateURI,
				W3C.PREDICATE_RDF_TYPE,
				type);
		updateQuery(addPredQuery);
	}
	
	public long getNumTriples(SPARQLEndpoint endpoint) throws HttpException, HttpResponseCodeException, IOException 
	{
		String query = "SELECT COUNT(*) WHERE { ?s ?p ?o }";
		List<Map<String,String>> results = endpoint.selectQuery(query, 120 * 1000);
		if(results.size() == 0) {
			return 0;
		}
		else {
			String columnName = results.get(0).keySet().iterator().next();
			return Long.valueOf(results.get(0).get(columnName));
		}
			
	}
	
	public long getNumTriplesLowerBound(SPARQLEndpoint endpoint) throws HttpException, HttpResponseCodeException, IOException
	{
		String probeQuery = "SELECT * WHERE { ?s ?p ?o }";
		return endpoint.getResultsCountLowerBound(probeQuery, 1000000, 20 * 1000);
	}
	
	public void setNumTriples(String endpointURI, long numTriples) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException
	{
		// Delete any existing value for numTriples first.
		String deleteQuery = "DELETE FROM GRAPH %u% { %u% %u% ?o } FROM %u% WHERE { %u% %u% ?o }";
		deleteQuery = SPARQLStringUtils.strFromTemplate(deleteQuery,
				getIndexGraphURI(),
				endpointURI, 
				SPARQLRegistryOntology.PREDICATE_NUMTRIPLES,
				getIndexGraphURI(),
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_NUMTRIPLES);
		updateQuery(deleteQuery);
		
		String query = "INSERT INTO GRAPH %u% { %u% %u% %v% }";
		query = SPARQLStringUtils.strFromTemplate(query,
				getIndexGraphURI(),
				endpointURI, 
				SPARQLRegistryOntology.PREDICATE_NUMTRIPLES, 
				String.valueOf(numTriples));
		updateQuery(query);
	}
	
	/***
	 * Quick and dirty method. This should be removed.
	 * 
	 * @throws IOException
	 */
	/*
	public void refreshOntology() throws IOException
	{
		OntModel predicateOntology = getPredicateOntology();
		List<SPARQLService> endpoints = getAllServices();
		Iterator<OntProperty> i = predicateOntology.listAllOntProperties();
		while(i.hasNext()) {
			String predicate = i.next().toString();
			
			if(predicate.equals("http://www.w3.org/2000/01/rdf-schema#label"))
				predicate.equals("hi");
			
			LOGGER.trace("Typing predicate " + predicate);
			for(SPARQLService endpoint : endpoints) {
				boolean isDatatypeProperty = false;
				LOGGER.trace("endpoint " + endpoint.toString());
				try {
					isDatatypeProperty = endpoint.isDatatypeProperty(predicate);
					addPredicateToOntology(predicate, isDatatypeProperty);
					break;
				}
				catch(Exception e) {}
			}
		}
		
	}
	*/
	
	/**
	 * <p>Return an ontology graph for the given endpoint.  By "ontology graph" I mean a graph
	 * connecting rdf:types with named predicates, as they occur in the endpoint.</p>   
	 * 
	 * <p>This method is very ad hoc, and is not guaranteed to produce a complete ontology (i.e.
	 * some predicates and types may be missing).  The main difficulty is that it not possible
	 * to do any query that entails a full scan of the database on the larger endpoints 
	 * (e.g. Bio2RDF UniProt with ~340 million triples).  The HTTP request times out
	 * at an intermediate proxy.</p>
	 * 
	 * <p>As such, the method just downloads the first 5000 triples of the endpoint and does
	 * its analysis on that.   And there are other problems.  Rather than clean this up,
	 * I'm going to redo it entirely when I get a chance. -- BV </p>
	 * 
	 * @param endpoint
	 * @return
	 * @throws IOException
	 * @throws HttpException
	 * @throws IllegalArgumentException
	 * @throws Exception
	 */
	/*
	public Collection<Triple> getOntologyForEndpoint(SPARQLEndpoint endpoint) throws HttpException, IOException
	{
		final String PREDICATE_EXAMPLE_URI = "http://ontology/exampleURI";
		LOGGER.debug("GENERATING ONTOLOGY FOR ENDPOINT " + endpoint.getURI());
		Collection<Triple> outputGraph = new ArrayList<Triple>();
		Set<String> deadEndpointURIs = new HashSet<String>();
		Set<String> visitedTypes = new HashSet<String>();
		String sampleQuery = "SELECT * WHERE {?s ?p ?o} LIMIT 5000";
		List<Map<String,String>> sample = null;
		sample = endpoint.selectQuery(sampleQuery);
		int tripleCount = 0;
		for(Map<String,String> binding : sample) {
			String p = binding.get("p");
			if(p.equals(W3C.PREDICATE_RDF_TYPE)) {
				String currentType = binding.get("o");
				// If we have not yet examined a node of type currentType in the current endpoint
				if(!visitedTypes.contains(currentType)) {
					visitedTypes.add(currentType);
					String s = binding.get("s");
					Triple newNode = RdfUtils.createTriple(currentType,PREDICATE_EXAMPLE_URI,s); 
					outputGraph.add(newNode);
					LOGGER.debug("FOUND NEW TYPE: " + currentType + "(instance = " + s + ")");
					// s is an example node of type o.
					// Use s to find out the neighboring predicates/types
					// for objects of type o.
					String neighborQuery = "SELECT * WHERE { %u% ?p ?o }";
					neighborQuery = StringUtil.strFromTemplate(neighborQuery, s);
					List<Map<String,String>> neighbors = endpoint.selectQuery(neighborQuery);
					Set<String> visitedDatatypeProperties = new HashSet<String>();
					for(Map<String,String> neighbor : neighbors) {
						String p2 = neighbor.get("p");
						String o2 = neighbor.get("o");
						LOGGER.debug("INSTANCE " + s + " HAS EDGE <" + p2 + ", " + o2 + "> ");
						if(RdfUtils.isURI(o2) && StringUtils.startsWith(o2, "http://")) {
							// Current predicate is an object property.
							// Try to find out the rdf:type of the object URI.
							String typeQuery = "SELECT ?type WHERE { %u% %u% ?type }";
							typeQuery = StringUtil.strFromTemplate(typeQuery, o2, W3C.PREDICATE_RDF_TYPE);
							boolean typeFound = false;
							for(SPARQLService endpoint2 : getAllServices()) {
								if(deadEndpointURIs.contains(endpoint2.getServiceURI()))
									continue;
								LOGGER.debug("Querying " + endpoint2.getServiceURI() + " for rdf:type of " + o2);
								List<Map<String,String>> results = null;
								try {
									results = ((SPARQLEndpoint)endpoint2).selectQuery(typeQuery);
								}
								catch(Exception e) {
									LOGGER.debug("Failed to query endpoint " + endpoint2.getServiceURI());
									deadEndpointURIs.add(endpoint2.getServiceURI());
									continue;
								}
								if(results != null && results.size() > 0) {
									String type = results.get(0).get("type");
									Triple newEdge = RdfUtils.createTriple(currentType, p2 + "(" + endpoint.getURI() + ")", type);
									outputGraph.add(newEdge);
									LOGGER.debug(tripleCount + ": found rdf:type for <" + o2 + "> in " + endpoint2.getServiceURI());
									LOGGER.debug(tripleCount + ": added Triple " + newEdge.toString()); 
									outputGraph.add(RdfUtils.createTriple(type, PREDICATE_EXAMPLE_URI, o2));
									typeFound = true;
									break;
								}
							}
							if(!typeFound) {
								LOGGER.debug("Unable to find an rdf:type for URI " + o2);
								// If we unable to find an rdf:type for o2 in any of the endpoints, assume
								// its a datatype property with a URI string as its value.
								if(!visitedDatatypeProperties.contains(p2)) {
									visitedDatatypeProperties.add(p2);
									Triple newEdge = RdfUtils.createTriple(currentType, p2 + "(" + endpoint.getURI() + ")", o2);
									outputGraph.add(newEdge);
									LOGGER.debug(tripleCount + ": added Triple " + newEdge.toString());
								}

							}
						}
						else if(!visitedDatatypeProperties.contains(p2)){
							// Current predicate is a datatype property.
							visitedDatatypeProperties.add(p2);
							Triple newEdge = RdfUtils.createTriple(currentType, p2 + "(" + endpoint.getURI() + ")", o2);
							outputGraph.add(newEdge);
							LOGGER.trace(tripleCount + ": added Triple " + newEdge.toString());
						}
					}
				}
			}
			tripleCount++;
		}
		return outputGraph;
	}
	
	
	public Collection<Triple> getOntologyGraph() throws HttpException, IOException
	{
		Collection<SPARQLService> endpoints = getAllServices();
		Collection<Triple> outputGraph = new ArrayList<Triple>();
		for(Service endpoint : endpoints) { 
			try {
				getOntologyForEndpoint((SPARQLEndpoint)endpoint);
			}
			catch(Exception e) {
				LOGGER.warn("Failed to get ontology for " + endpoint.getServiceURI() + ": " + e);
			}
		}
		return outputGraph;
	}
	*/
	
	public static void main(String[] args) 
	{
		try {
			Getopt options = new Getopt("predstats", args, "a:i:d:cr:sR");
			int option;
			String arg;
			
			VirtuosoSPARQLRegistryAdmin registry = new VirtuosoSPARQLRegistryAdmin();
			
			/*
			// REMOVE THIS LINE
			registry.refreshOntology(); if(true) return;
			*/
			
			while ((option = options.getopt()) != -1) {
				arg = options.getOptarg();
				switch(option) 
				{
				case 'r':
					registry = new VirtuosoSPARQLRegistryAdmin(arg);
					break;
				case '?': 
					return; // getopt() already printed an error 
				default:
					break;
				}
			}

			options.setOptind(0);
			
			while ((option = options.getopt()) != -1) {
				switch(option) 
				{
				case 'c':
					registry.clearRegistry();
					break;
				case '?': 
					return; // getopt() already printed an error 
				default:
					break;
				}
			}

			options.setOptind(0);
			
			while ((option = options.getopt()) != -1) { 
				arg = options.getOptarg();
				switch(option) 
				{ 
				case 'a': 
					try {
						registry.addEndpoint(arg, SPARQLEndpointType.VIRTUOSO);
					}
					catch(Exception e) {
						System.err.println("Failed to add endpoint " + arg + " to registry: " + e.getStackTrace());
					}
					break;
				case 'i': 
					try {
						registry.addAndIndexEndpoint(arg, SPARQLEndpointType.VIRTUOSO);
					}
					catch(Exception e) {
						System.err.println("Failed to index endpoint " + arg + ": " + e.getStackTrace());
					}
					break;
				case 'd':
					try {
						registry.removeEndpoint(arg);
					}
					catch(Exception e) {
						System.err.println("Failed to remove endpoint " + arg + ": " + e.getStackTrace());
					}
					break;
				case 'R':
					registry.refreshStatusOfEndpoints();
					break;
				/*
				case 's':
					registry.refreshEndpointSizes();
					break;
				*/
				case '?': 
					return; // getopt() already printed an error 
				default:
					break;
				} 
			}		
		}
		catch(Exception e) {
			System.out.print("Error: ");
			e.printStackTrace();
		}
		
	}
	
	
}
