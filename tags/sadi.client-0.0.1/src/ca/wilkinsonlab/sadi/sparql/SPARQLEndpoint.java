package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.AccessException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.HttpUtils;
import ca.wilkinsonlab.sadi.utils.JsonUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLResultsXMLUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpInputStream;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;
import ca.wilkinsonlab.sadi.client.Config;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

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
	public enum EndpointType { VIRTUOSO, D2R  };
	public enum SelectQueryResultsFormat  { SPARQL_RESULTS_XML, JSON };
	public enum ConstructQueryResultsFormat { RDFXML, N3 };
	
	public final static Log log = LogFactory.getLog(SPARQLEndpoint.class);
	
	final static int DEFAULT_QUERY_TIMEOUT = 30 * 1000;
	final static int DEFAULT_PING_TIMEOUT = 10 * 1000;
	
	String endpointURI;
	
	protected SelectQueryResultsFormat selectResultsFormat;
	protected ConstructQueryResultsFormat constructResultsFormat;

	protected final static String RESULTS_LIMIT_KEY = "sadi.sparqlResultsLimit";
	public final static long NO_RESULTS_LIMIT = -1;
	

	public SPARQLEndpoint(String uri) 
	{
		this(uri, SelectQueryResultsFormat.SPARQL_RESULTS_XML, ConstructQueryResultsFormat.RDFXML);
	}
	
	public SPARQLEndpoint(String uri, SelectQueryResultsFormat selectFormat,  ConstructQueryResultsFormat constructFormat)
	{
		endpointURI = uri;
		setSelectResultsFormat(selectFormat);
		setConstructResultsFormat(constructFormat);
		
	}

	protected SelectQueryResultsFormat getSelectResultsFormat()	{ return selectResultsFormat; }
	protected void setSelectResultsFormat(SelectQueryResultsFormat selectResultsFormat) { this.selectResultsFormat = selectResultsFormat; }
	protected ConstructQueryResultsFormat getConstructResultsFormat() { return constructResultsFormat; }
	protected void setConstructResultsFormat(ConstructQueryResultsFormat constructResultsFormat) { this.constructResultsFormat = constructResultsFormat; }

	public String getURI()  { return endpointURI; }
	public String toString() { return getURI(); }
	
	public boolean ping()
	{
		return ping(DEFAULT_PING_TIMEOUT);  
	}

	public boolean ping(int timeout) 
	{
		try {
			selectQuery("SELECT * WHERE { ?s ?p ?o } LIMIT 1", timeout);
			return true;
		} catch(IOException e) {
			return false;
		}
	}
	
	public Collection<Triple> getTriplesMatchingPattern(Triple pattern) throws IOException
	{
		return getTriplesMatchingPattern(pattern, DEFAULT_QUERY_TIMEOUT, NO_RESULTS_LIMIT);
	}

	public Collection<Triple> getTriplesMatchingPattern(Triple pattern, int timeout, long resultsLimit) throws IOException 
	{
		Node s = pattern.getSubject();
		Node p = pattern.getPredicate();
		Node o = pattern.getObject();
		
		if(s.isVariable() && o.isVariable()) 
			throw new IllegalArgumentException("Not allowed to query {?s ?p ?o} or {?s <predicate> ?o} against a SPARQL endpoint");
		
		/*
		StringBuilder patternStr = new StringBuilder();
		Node pos[] = {s,p,o};
		for(int i = 0; i < pos.length; i++) {
			if(pos[i].isVariable()) {
				patternStr.append(pos[i].toString());
				patternStr.append(' ');
			}
			else {
				if(pos[i].isURI())
					patternStr.append(SPARQLStringUtils.strFromTemplate("%u% ", pos[i].toString()));
				else
					patternStr.append(SPARQLStringUtils.strFromTemplate("%v% ", pos[i].toString()));
			}
		}

		StringBuilder query = new StringBuilder();
		query.append("CONSTRUCT { ");
		query.append(patternStr);
		query.append(" } WHERE { ");
		query.append(patternStr);
		query.append(" }");
		*/

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
		
		Collection<Triple> triples = constructQuery(query.toString(), timeout);
		
		if(resultsLimit != NO_RESULTS_LIMIT && triples.size() == resultsLimit)
			log.warn("query results may have been truncated at " + resultsLimit + " triples");
		
		return triples;
	}
	
	public List<Map<String,String>> selectQuery(String query, int timeout) throws IOException 
	{
		InputStream response = HttpUtils.POST(getURI(), getHTTPArgsForSelectQuery(query), timeout);
		List<Map<String,String>> results = new ArrayList<Map<String,String>>();
		try {
			results = convertSelectResponseToBindings(response);
		}
		finally {
			response.close();
		}
		return results;
	}

	public List<Map<String,String>> selectQuery(String query) throws IOException 
	{
		InputStream response = HttpUtils.POST(getURI(), getHTTPArgsForSelectQuery(query));
		List<Map<String,String>> results = new ArrayList<Map<String,String>>();
		try {
			results = convertSelectResponseToBindings(response);
		}
		finally {
			response.close();
		}
		return results;
	}

	public Collection<Triple> constructQuery(String query, int timeout) throws IOException
	{
		InputStream response = HttpUtils.POST(getURI(), getHTTPArgsForConstructQuery(query), timeout);
		Collection<Triple> results = new ArrayList<Triple>();
		try { 
			results = convertConstructResponseToTriples(response);
		}
		finally {
			response.close();
		}
		return results;
	}

	public Collection<Triple> constructQuery(String query) throws IOException
	{
		InputStream response = HttpUtils.POST(getURI(), getHTTPArgsForConstructQuery(query));
		Collection<Triple> results = new ArrayList<Triple>();
		try {
			results = convertConstructResponseToTriples(response);
		}
		finally {
			response.close();
		}
		return results;
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
		HttpInputStream response = HttpUtils.POST(getURI(), getHTTPArgsForUpdateQuery(query));
		response.close();
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
	public List<Map<String,String>> getPartialQueryResults(String query) throws HttpException, HttpResponseCodeException, IOException 
	{
		return getPartialQueryResults(query, 1);
	}
	
	public List<Map<String,String>> getPartialQueryResults(String query, long startSize) throws HttpException, HttpResponseCodeException, IOException 
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
	public long getResultsCountLowerBound(String query, long startSize) throws HttpException, HttpResponseCodeException, IOException
	{
		return getResultsCountLowerBound(query, startSize, DEFAULT_QUERY_TIMEOUT);
	}
	
	public long getResultsCountLowerBound(String query, long startSize, int timeout) throws HttpException, HttpResponseCodeException, IOException 
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
				results = selectQuery(curQuery, timeout);
			}
			catch(IOException e) {
				if(HttpUtils.isHTTPTimeout(e)) 
				{
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
				}
				throw e;
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
	
	public boolean isDatatypeProperty(String predicateURI) throws IOException, AmbiguousPropertyTypeException 
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
			String query;
			if(isDatatypeProperty)
				query = "SELECT ?o WHERE { ?s %u% ?o . FILTER (!isLiteral(?o)) } LIMIT 1";
			else
				query = "SELECT ?o WHERE { ?s %u% ?o . FILTER isLiteral(?o) } LIMIT 1";
			
			query = SPARQLStringUtils.strFromTemplate(query, predicateURI);
			if(selectQuery(query).size() > 0)
				throw new AmbiguousPropertyTypeException(predicateURI + " is an RDF predicate which has both URIs and literals as values.");
		}
		return isDatatypeProperty;
	}
	
	protected Collection<NameValuePair> getHTTPArgsForUpdateQuery(String query) {
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

	protected Collection<NameValuePair> getHTTPArgsForConstructQuery(String query) 
	{
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair("query",query));
		return params;
	}

	protected Collection<NameValuePair> getHTTPArgsForSelectQuery(String query) 
	{
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair("query", query));
		return params;
	}
	
	public TripleIterator iterator() 
	{
		return new TripleIterator(this);
	}

	public TripleIterator iterator(int timeout, long blockSize)
	{
		return new TripleIterator(this, timeout, blockSize);
	}
	
	/**
	 * This exception is thrown when a predicate has both literal and URIs values, and thus cannot
	 * be typed as a datatype property or an object property.
	 */
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
		private final static int DEFAULT_TIMEOUT = 60 * 1000; // milliseconds
		private final static long  DEFAULT_BLOCK_SIZE = 50000; // triples
		private List<Triple> tripleCache = null;
		private int cacheIndex = 0;
		private int cacheOffset = 0;
		private SPARQLEndpoint endpoint;
		private int timeout;
		private long blockSize;

		public TripleIterator(SPARQLEndpoint endpoint) 
		{
			this(endpoint, DEFAULT_TIMEOUT, DEFAULT_BLOCK_SIZE);
		}
		
		public TripleIterator(SPARQLEndpoint endpoint, int timeout, long blockSize) 
		{
			this.endpoint = endpoint;
			this.timeout = timeout;
			this.blockSize = blockSize;
		}
		
		public boolean hasNext() throws IOException 
		{
			if(tripleCache == null || cacheIndex >= tripleCache.size())
				return retrieveNextBlock();
			else 
				return true;
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
			tripleCache.addAll(endpoint.constructQuery(query, timeout));
			cacheOffset += blockSize;
			cacheIndex = 0;
			if(tripleCache.size() > 0)
				return true;
			else 
				return false;
		}
	}
}
