package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.AccessException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.utils.JsonUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLResultsXMLUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.http.GetRequest;
import ca.wilkinsonlab.sadi.utils.http.HttpRequest;
import ca.wilkinsonlab.sadi.utils.http.HttpResponse;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils.HttpStatusException;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * <p>Encapsulates access to a SPARQL endpoint (via HTTP).</p>
 * 
 * <p>This is an abstract base class.  The specific behaviour for each type
 * of SPARQL endpoint (e.g. Jena, Virtuoso, etc.) will differ based 
 * on the parameters that are used in the HTTP GET/POST request.</p>
 * 
 * @author Ben Vandervalk
 */
public class SPARQLEndpoint
{
	public enum EndpointType { VIRTUOSO, D2R, UNKNOWN };
	public enum SelectQueryResultsFormat  { SPARQL_RESULTS_XML, JSON };
	public enum ConstructQueryResultsFormat { RDFXML, N3 };
	
	public final static Logger log = Logger.getLogger(SPARQLEndpoint.class);
	
	String endpointURI;
	
	protected SelectQueryResultsFormat selectResultsFormat;
	protected ConstructQueryResultsFormat constructResultsFormat;
	protected EndpointType endpointType;
	
	protected final static String RESULTS_LIMIT_KEY = "sadi.sparqlResultsLimit";
	public final static long NO_RESULTS_LIMIT = -1;
	

	public SPARQLEndpoint(String uri) 
	{
		this(uri, EndpointType.UNKNOWN, SelectQueryResultsFormat.SPARQL_RESULTS_XML, ConstructQueryResultsFormat.RDFXML);
	}

	public SPARQLEndpoint(String uri, EndpointType type) {
		this(uri, type, SelectQueryResultsFormat.SPARQL_RESULTS_XML, ConstructQueryResultsFormat.RDFXML);
	}
	
	public SPARQLEndpoint(String uri, EndpointType type, SelectQueryResultsFormat selectFormat,  ConstructQueryResultsFormat constructFormat)
	{
		endpointURI = uri;
		setEndpointType(type);
		setSelectResultsFormat(selectFormat);
		setConstructResultsFormat(constructFormat);
		
	}

	protected SelectQueryResultsFormat getSelectResultsFormat()	{ 
		return selectResultsFormat; 
	}
	protected void setSelectResultsFormat(SelectQueryResultsFormat selectResultsFormat) { 
		this.selectResultsFormat = selectResultsFormat; 
	}
	
	protected ConstructQueryResultsFormat getConstructResultsFormat() { 
		return constructResultsFormat; 
	}
	protected void setConstructResultsFormat(ConstructQueryResultsFormat constructResultsFormat) { 
		this.constructResultsFormat = constructResultsFormat; 
	}

	public String getURI()  { 
		return endpointURI; 
	}
	
	public String toString() { 
		return getURI(); 
	}
	
	public EndpointType getEndpointType() {
		return endpointType;
	}

	public void setEndpointType(EndpointType type) {
		this.endpointType = type;
	}

	public boolean ping() 
	{
		try {
			selectQuery("SELECT * WHERE { ?s ?p ?o } LIMIT 1");
			return true;
		} catch(IOException e) {
			return false;
		} 
	}

	
	public Collection<Triple> getTriplesMatchingPattern(Triple pattern, long resultsLimit) throws IOException 
	{
		Node s = pattern.getSubject();
		Node o = pattern.getObject();
		
		if(s.isVariable() && o.isVariable()) 
			throw new IllegalArgumentException("Not allowed to query {?s ?p ?o} or {?s <predicate> ?o} against a SPARQL endpoint");
		
		String patternStr = SPARQLStringUtils.getTriplePattern(pattern);
		StringBuilder query = new StringBuilder();
		query.append("CONSTRUCT { ");
		query.append(patternStr);
		query.append(" } WHERE { ");
		query.append(patternStr);
		query.append(" }");
		
		if(resultsLimit != NO_RESULTS_LIMIT) {
			query.append(" LIMIT ");
			query.append(resultsLimit);
		}
		
		Collection<Triple> triples = constructQuery(query.toString());
		
		if(resultsLimit != NO_RESULTS_LIMIT && triples.size() == resultsLimit)
			log.warn("query results may have been truncated at " + resultsLimit + " triples");
		
		return triples;
	}
	
	public List<Map<String,String>> selectQuery(String query) throws IOException 
	{
		InputStream is = HttpUtils.GET(new URL(getURI()), getParamsForSelectQuery(query));
		
		try {
			return convertSelectResponseToBindings(is);
		}
		finally {
			is.close();
		}
	}

	public Collection<Triple> constructQuery(String query) throws IOException
	{
		InputStream is = HttpUtils.GET(new URL(getURI()), getParamsForConstructQuery(query));

		try {
			return convertConstructResponseToTriples(is);
		}
		finally {
			is.close();
		}
	}
	
	public Collection<ConstructQueryResult> constructQueryBatch(Collection<String> constructQueries) {
		
		Collection<HttpRequest> requests = new ArrayList<HttpRequest>();
		Collection<ConstructQueryResult> results = new ArrayList<ConstructQueryResult>();
		
		for(String query : constructQueries) {
			try {
				requests.add(new GetRequest(new URL(getURI()), getParamsForConstructQuery(query)));
			} catch(MalformedURLException e) {
				results.add(new ConstructQueryResult(query, e));
			}
		}
		
		Collection<HttpResponse> responses = HttpUtils.batchRequest(requests);
		
		for(HttpResponse response : responses) {
			String originalQuery = response.getOriginalRequest().getParams().get("query");
			if(response.exceptionOccurred()) {
				results.add(new ConstructQueryResult(originalQuery, response.getException()));
			} else {
				String lang = getJenaRDFLangString(getConstructResultsFormat());
				Model resultTriples = ModelFactory.createDefaultModel();
				resultTriples.read(response.getInputStream(), "", lang);
				results.add(new ConstructQueryResult(originalQuery, resultTriples));
				try {
					response.getInputStream().close();
				} catch(IOException e) {
					log.warn("failed to close InputStream for response to query: " + originalQuery, e);
				}
			}
		}
		
		return results;
	}
	
	protected static String getJenaRDFLangString(ConstructQueryResultsFormat format) {
		switch(format) {
		case N3:
			return "N3";
		case RDFXML:
			return "RDF/XML";
		default:
			throw new RuntimeException("unrecognized value for ContructQueryResultsFormat");
		}
	}
	
	/**
	 * Issue a SPARQL update (SPARUL) query.  These queries are using for inserting triples,
	 * updating triples, deleting triples, etc.
	 * @param query
	 * @throws HttpException if an HTTP protocol error occurs (this will probably never happen)
	 * @throws HttpResponseCodeException if a non-success HTTP response code is generated (e.g. 404: File not found)
	 * @throws IOException if there is a communication problem (e.g. request timeout)
	 * @throws AccessException if the client doesn't have permission to perform the update query
	 */
	public void updateQuery(String query) throws IOException
	{
		InputStream is = HttpUtils.POST(new URL(getURI()), getParamsForUpdateQuery(query));
		is.close();
	}

	public Set<String> getNamedGraphs() throws IOException
	{
		log.trace("querying named graphs from " + getURI());
		Set<String> graphURIs = new HashSet<String>();
		List<Map<String,String>> results = selectQuery("SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } }");
		for(Map<String,String> binding : results)
			graphURIs.add(binding.get("g"));
		return graphURIs;
	}	

	public Set<String> getPredicates() throws IOException
	{
		log.trace("querying predicate list from " + getURI());
		Set<String> predicates = new HashSet<String>();
		List<Map<String,String>> results = selectQuery("SELECT DISTINCT ?p WHERE { ?s ?p ?o }");
		for(Map<String,String> binding : results) 
			predicates.add(binding.get("p"));
		return predicates;
	}

	public Set<String> getPredicates(String graphURI) throws IOException
	{
		log.trace("querying predicate list from graph " + graphURI + " of " + getURI());
		Set<String> predicates = new HashSet<String>();
		List<Map<String,String>> results = null;
		String query = SPARQLStringUtils.strFromTemplate("SELECT DISTINCT ?p FROM %u% WHERE { ?s ?p ?o }", graphURI);
		results = selectQuery(query);
		for(Map<String,String> binding : results)
			predicates.add(binding.get("p"));
		return predicates;
	}
	

	public Set<String> getPredicatesPartial() throws IOException
	{
		log.trace("querying partial predicate list from " + getURI());
		
		Set<String> predicates = new HashSet<String>();
		String query = "SELECT DISTINCT ?p WHERE { ?s ?p ?o }";
		List<Map<String,String>> results = getPartialQueryResults(query);
		for(Map<String,String> binding : results)
			predicates.add(binding.get("p"));
		return predicates;
	}

	/**
	 * Get the largest result set possible for the given query.  This method is a fallback 
	 * if we cannot get the complete answer to a query due to HTTP timeouts.
	 */
	public List<Map<String,String>> getPartialQueryResults(String query) throws IOException 
	{
		return getPartialQueryResults(query, 1);
	}
	
	public List<Map<String,String>> getPartialQueryResults(String query, long startSize) throws IOException 
	{
		long limit = getResultsCountLowerBound(query, startSize);
		String partialQuery = query + " LIMIT " + limit;
		return selectQuery(partialQuery);
	}
	
	/**
	 * <p>Get a lower bound for the number of results for the given query.  This method is a fallback
	 * if we cannot do a full COUNT(*) query on the endpoint due to HTTP timeouts.</p>
	 * 
	 * <p>We do this by adjusting the limit and seeing where we start to fail (i.e. timeout).  If 
	 * the current limit is good, we double it. If the current limit times out, we take the 
	 * halfway point between the current limit and the last successful limit.  And so on.</p>
	 *
	 * <p>We don't actually need to download the results each time we try a new limit, because the 
	 * majority of the processing time is on the server side (finding and materializing the results).</p>
	 * 
	 * @param query 
	 * @param startSize Start with the lower bound at this number, and then adjust it by factors of
	 * two to find the real limit. 
	 * @return A maximum lower bound for the number of results to the query
	 */
	public long getResultsCountLowerBound(String query, long startSize) throws IOException 
	{
		long curPoint = startSize;
		long lastSuccessPoint = 0;
		long lastFailurePoint = -1;
		String curQuery;
		boolean answerIsExact = false; 
		
		while(true) {
			if(curPoint == 0)
				break;

			List<Map<String,String>> results;
			try {
				curQuery = query + " OFFSET " + (curPoint - 1) + " LIMIT 1"; 
				results = selectQuery(curQuery);
			}
			catch(HttpStatusException e) {
				if(e.getStatusCode() == HttpResponse.HTTP_STATUS_GATEWAY_TIMEOUT) {
					log.debug("query timed out for LIMIT = " + curPoint);
					
					if(curPoint == lastSuccessPoint) {
						// The limit lastSuccessPoint succeeded last time but not 
						// this time. To be a bit safer, subtract 10%
						// from the limit value and call it good.
						curPoint = (curPoint*90)/100;
						break;
					}
						
					lastFailurePoint = curPoint;
					if(lastSuccessPoint > -1) 
						curPoint = lastSuccessPoint + ((curPoint - lastSuccessPoint) / 2);
					else
						curPoint /= 2;
					if(curPoint == 0) 
						throw e;
					continue;
				} else {
					throw e;
				}
			}
			if(results.size() == 0) {
				// A successful query with no results means that we have
				// gone beyond the total number of results for the given query.  
				// And that means that we could have done the full query in the
				// first place. Nothing we can do about that, so carry on anyhow.
				log.debug("query succeeded for LIMIT = " + curPoint + ", but " + String.valueOf(curPoint) + 
						" is greater than the full number of results for the query.");
				answerIsExact = true;
				lastFailurePoint = curPoint;
				curPoint = lastSuccessPoint + ((curPoint - lastSuccessPoint) / 2);
				continue;
			}
			else if(curPoint == lastFailurePoint - 1) {
				// If we can't go any higher, we've found the limit.
				break;
			}
			else if(lastFailurePoint > -1) { 
				log.debug("query succeeded for LIMIT = " + curPoint);
				lastSuccessPoint = curPoint;
				curPoint = curPoint + ((lastFailurePoint - curPoint) / 2);
			}
			else {
				log.debug("query succeeded for LIMIT = " + curPoint);
				lastSuccessPoint = curPoint;
				curPoint *= 2;
			}
		}
		
		if(answerIsExact) 
			log.warn("LIMIT testing indicates that it was possible to do the full query.");
		
		return curPoint;
	}
	
	public boolean isDatatypeProperty(String predicateURI, boolean checkForAmbiguousProperty) throws IOException, AmbiguousPropertyTypeException 
	{
		log.trace("checking if " + predicateURI + " is a datatype property or an object property");

		boolean isDatatypeProperty = false;
		String predicateQuery = "CONSTRUCT { ?s %u% ?o } WHERE { ?s %u% ?o } LIMIT 1";
		predicateQuery = SPARQLStringUtils.strFromTemplate(predicateQuery, predicateURI, predicateURI);
		Collection<Triple> results = constructQuery(predicateQuery);
		if(results.size() == 0) {
			throw new RuntimeException("the endpoint " + getURI() + " doesn't have any triples containing the predicate " + predicateURI);
		}
		else {
			Node o = results.iterator().next().getObject();
			if(o.isLiteral())
				isDatatypeProperty = true;
			else
				isDatatypeProperty = false;
			
			// Sanity check: Make sure this is not an RDF predicate that has both URIs and literals as values.  
			if(checkForAmbiguousProperty) {
				String query;
				if(isDatatypeProperty)
					query = "SELECT ?o WHERE { ?s %u% ?o . FILTER (!isLiteral(?o)) } LIMIT 1";
				else
					query = "SELECT ?o WHERE { ?s %u% ?o . FILTER isLiteral(?o) } LIMIT 1";

				query = SPARQLStringUtils.strFromTemplate(query, predicateURI);
				if(selectQuery(query).size() > 0)
					throw new AmbiguousPropertyTypeException(predicateURI + " is an RDF predicate which has both URIs and literals as values.");
			}
		}
		return isDatatypeProperty;
	}
	
	protected Map<String,String> getParamsForUpdateQuery(String query) {
		throw new UnsupportedOperationException();
	}

	protected Collection<Triple> convertConstructResponseToTriples(InputStream response) 
	{
		switch(getConstructResultsFormat()) {
		case N3:
			return RdfUtils.getTriples(response, "N3");
		default:
		case RDFXML:
			return RdfUtils.getTriples(response, "RDF/XML");
		}
	}

	protected List<Map<String,String>> convertSelectResponseToBindings(InputStream response) throws IOException 
	{
		switch(getSelectResultsFormat()) {
		case JSON: 
			String responseAsString = IOUtils.toString(response);
			return JsonUtils.convertJSONToResults(JsonUtils.read(responseAsString));
		default:
		case SPARQL_RESULTS_XML:
			return SPARQLResultsXMLUtils.getResultsFromSPARQLXML(response);
		}
	}

	protected Map<String,String> getParamsForConstructQuery(String query) 
	{
		Map<String,String> params = new HashMap<String,String>();
		params.put("query", query);
		return params;
	}

	protected Map<String,String> getParamsForSelectQuery(String query) 
	{
		Map<String,String> params = new HashMap<String,String>();
		params.put("query", query);
		return params;
	}
	
	public TripleIterator iterator() 
	{
		return new TripleIterator(this);
	}

	public TripleIterator iterator(long blockSize)
	{
		return new TripleIterator(this, blockSize);
	}
	
	/**
	 * This exception is thrown when a predicate has both literal and URIs values, and thus cannot
	 * be typed as a datatype property or an object property.
	 */
	@SuppressWarnings("serial")
	public static class AmbiguousPropertyTypeException extends Exception
	{
		public AmbiguousPropertyTypeException(String message) { super(message); }
	}
	
	/**
	 * This class iterates over all the triples in a given endpoint.  I couldn't implement the real Iterator 
	 * interface here, because I need to be able to throw an IOException from next() and hasNext().
	 */
	public static class TripleIterator 
	{
		private final static long  DEFAULT_BLOCK_SIZE = 50000; // triples
		private List<Triple> tripleCache = null;
		private int cacheIndex = 0;
		private int cacheOffset = 0;
		private SPARQLEndpoint endpoint;
		private long blockSize;

		public TripleIterator(SPARQLEndpoint endpoint) 
		{
			this(endpoint, DEFAULT_BLOCK_SIZE);
		}
		
		public TripleIterator(SPARQLEndpoint endpoint, long blockSize) 
		{
			this.endpoint = endpoint;
			this.blockSize = blockSize;
		}
		
		public boolean hasNext() throws IOException 
		{
			if(tripleCache == null) {
				return retrieveNextBlock();
			} else if(cacheIndex >= tripleCache.size()) {
				// if current block is not the full size, then it is the last one
				if(tripleCache.size() < blockSize) {
					return false;
				}
				return retrieveNextBlock();
			} else {
				return true;
			}
		}

		public Triple next() throws IOException 
		{
			if(!hasNext())
				throw new NoSuchElementException();
			return tripleCache.get(cacheIndex++);
		}
		
		private boolean retrieveNextBlock() throws IOException 
		{
			log.debug("retrieving triples " + cacheOffset + " through " + (cacheOffset + blockSize - 1) + " from " + endpoint.getURI());
			
			String query = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o } LIMIT " + blockSize + " OFFSET " + cacheOffset;
			tripleCache = new ArrayList<Triple>();
			tripleCache.addAll(endpoint.constructQuery(query));
			cacheOffset += blockSize;
			cacheIndex = 0;
			if(tripleCache.size() > 0)
				return true;
			else 
				return false;
		}
	}
}
