package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.AccessException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.HttpUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.StringUtil;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;

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
public abstract class SPARQLEndpoint
{
	public final static Log log = LogFactory.getLog(SPARQLEndpoint.class);
	
	String endpointURI;
	
	public String getURI() {
		return endpointURI;
	}
	
	public Collection<Triple> getTriplesMatchingPattern(Triple pattern) throws Exception
	{
		return getTriplesMatchingPattern(pattern, SPARQLService.DEFAULT_QUERY_TIMEOUT);
	}

	public Collection<Triple> getTriplesMatchingPattern(Triple pattern, int timeout) throws Exception 
	{
		Node s = pattern.getSubject();
		Node p = pattern.getPredicate();
		Node o = pattern.getObject();
		if(s.isVariable() && o.isVariable()) 
			throw new IllegalArgumentException("Not allowed to query {?s ?p ?o} or {?s <predicate> ?o} against a SPARQL endpoint");
		StringBuilder patternStr = new StringBuilder();
		Node pos[] = {s,p,o};
		for(int i = 0; i < pos.length; i++) {
			if(pos[i].isVariable()) {
				patternStr.append(pos[i].toString());
				patternStr.append(' ');
			}
			else {
				if(pos[i].isURI())
					patternStr.append(StringUtil.strFromTemplate("%u% ", pos[i].toString()));
				else
					patternStr.append(StringUtil.strFromTemplate("%v% ", pos[i].toString()));
			}
		}
		StringBuilder query = new StringBuilder();
		query.append("CONSTRUCT { ");
		query.append(patternStr);
		query.append(" } WHERE { ");
		query.append(patternStr);
		query.append(" }");
		return constructQuery(query.toString(), timeout);
	}
	
	public List<Map<String,String>> selectQuery(String query, int timeout) throws HttpException, IOException 
	{
		InputStream response = HttpUtils.GET(getURI(), getHTTPArgsForSelectQuery(query), timeout);
		List<Map<String,String>> results = convertSelectResponseToBindings(response);
		response.close();
		return results;
	}

	public List<Map<String,String>> selectQuery(String query) throws HttpException, HttpResponseCodeException, IOException 
	{
		InputStream response = HttpUtils.GET(getURI(), getHTTPArgsForSelectQuery(query));
		List<Map<String,String>> results = convertSelectResponseToBindings(response);
		response.close();
		return results;
	}

	public Collection<Triple> constructQuery(String query, int timeout) throws HttpException, HttpResponseCodeException, IOException
	{
		InputStream response = HttpUtils.GET(getURI(), getHTTPArgsForConstructQuery(query), timeout);
		Collection<Triple> results = convertConstructResponseToTriples(response);
		response.close();
		return results;
	}

	public Collection<Triple> constructQuery(String query) throws HttpException, HttpResponseCodeException, IOException
	{
		InputStream response = HttpUtils.GET(getURI(), getHTTPArgsForConstructQuery(query));
		Collection<Triple> results = convertConstructResponseToTriples(response);
		response.close();
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
	public void updateQuery(String query) throws HttpException, HttpResponseCodeException, IOException, AccessException
	{
		HttpUtils.POST(getURI(), getHTTPArgsForUpdateQuery(query));
	}

	public Set<String> getNamedGraphs() throws HttpException, IOException
	{
		log.trace("Querying named graphs from " + getURI());
		Set<String> graphURIs = new HashSet<String>();
		List<Map<String,String>> results = selectQuery("SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } }");
		for(Map<String,String> binding : results)
			graphURIs.add(binding.get("g"));
		return graphURIs;
	}	

	public Set<String> getPredicates() throws HttpException, IOException
	{
		log.trace("Querying predicate list from " + getURI());
		Set<String> predicates = new HashSet<String>();
		List<Map<String,String>> results = selectQuery("SELECT DISTINCT ?p WHERE { ?s ?p ?o }");
		for(Map<String,String> binding : results)
			predicates.add(binding.get("p"));
		return predicates;
	}

	public Set<String> getPredicates(String graphURI) throws HttpException, IOException
	{
		log.trace("Querying predicate list from graph " + graphURI + " of " + getURI());
		Set<String> predicates = new HashSet<String>();
		List<Map<String,String>> results = null;
		String query = StringUtil.strFromTemplate("SELECT DISTINCT ?p FROM %u% WHERE { ?s ?p ?o }", graphURI);
		results = selectQuery(query);
		for(Map<String,String> binding : results)
			predicates.add(binding.get("p"));
		return predicates;
	}
	

	public Set<String> getPredicatesPartial() throws HttpException, IOException
	{
		log.trace("Querying partial predicate list from " + getURI());
		Set<String> predicates = new HashSet<String>();
		List<Map<String,String>> results;
		/*
		String query = "SELECT ?p WHERE { ?s ?p ?o } LIMIT 5000";
		results = selectQuery(query);
		*/
		String query = "SELECT DISTINCT ?p WHERE { ?s ?p ?o }";
		results = getPartialQueryResults(query);
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
	
	final static int DEFAULT_QUERY_TIMEOUT = 30 * 1000;
	
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
			List<Map<String,String>> results;
			try {
				curQuery = query + " OFFSET " + (curPoint - 1) + " LIMIT 1"; 
				results = selectQuery(curQuery, timeout);
			}
			catch(HttpResponseCodeException e) {
				if(HttpUtils.isProxyTimeout(e)) 
				{
					log.debug("query timed out for curPoint = " + curPoint);
					
					if(curPoint == lastSuccessPoint) {
						// The limit lastSuccessPoint succeeded last time but not 
						// this time. To be a bit safer, subtract 5%
						// from the limit value and call it good.
						curPoint = (curPoint*95)/100;
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
			// If we can't go any higher, we've found the limit.
			if(curPoint == lastFailurePoint - 1)
				break;
			else if(results.size() == 0) {
				// A successful query with no results means that we have
				// gone beyond the total number of results for the given query.  
				// And that means that we could have done the full query in the
				// first place. Nothing we can do about that, so carry on anyhow.
				log.debug("query succeeded for curPoint = " + curPoint + ", but " + String.valueOf(curPoint) + 
						" is greater than the full number of results for the query.");
				answerIsExact = true;
				lastFailurePoint = curPoint;
				curPoint = lastSuccessPoint + ((curPoint - lastSuccessPoint) / 2);
				continue;
			}
			else if(lastFailurePoint > -1) { 
				log.debug("query succeeded for curPoint = " + curPoint);
				lastSuccessPoint = curPoint;
				curPoint = curPoint + ((lastFailurePoint - curPoint) / 2);
			}
			else {
				log.debug("query succeeded for curPoint = " + curPoint);
				lastSuccessPoint = curPoint;
				curPoint *= 2;
			}
		}
		
		if(answerIsExact) 
			log.warn("LIMIT testing indicates that it was possible to do the full query.");
		
		return curPoint;
	}
	
	public boolean isDatatypeProperty(String predicateURI) throws HttpException, IOException 
	{
		boolean isDatatypeProperty = false;
		String predicateQuery = "SELECT * WHERE { ?s %u% ?o } LIMIT 1";
		predicateQuery = StringUtil.strFromTemplate(predicateQuery, predicateURI);
		List<Map<String,String>> results = selectQuery(predicateQuery);
		if(results.size() == 0) {
			throw new RuntimeException("Internal error: The endpoint " + getURI() + 
					" have any triples containing the predicate <" + predicateURI + ">");
		}
		else {
			String object = results.iterator().next().get("o");
			if(RdfUtils.isURI(object))
				isDatatypeProperty = false;
			else
				isDatatypeProperty = true;
		}
		return isDatatypeProperty;
	}
	
	abstract protected Collection<Triple> convertConstructResponseToTriples(InputStream response) throws IOException;
	abstract protected List<Map<String,String>> convertSelectResponseToBindings(InputStream response) throws IOException;

	abstract protected Collection<NameValuePair> getHTTPArgsForConstructQuery(String query);
	abstract protected Collection<NameValuePair> getHTTPArgsForSelectQuery(String query);
	abstract protected Collection<NameValuePair> getHTTPArgsForUpdateQuery(String query);
}
