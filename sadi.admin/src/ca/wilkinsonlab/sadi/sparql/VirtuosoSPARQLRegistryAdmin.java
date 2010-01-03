package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.ws.http.HTTPException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;

import ca.wilkinsonlab.sadi.utils.RegExUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.http.HttpClient;
import ca.wilkinsonlab.sadi.utils.http.HttpResponse;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils;
import ca.wilkinsonlab.sadi.utils.http.XLightwebHttpClient;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils.HttpStatusException;
import ca.wilkinsonlab.sadi.vocab.SPARQLRegistryOntology;
import ca.wilkinsonlab.sadi.vocab.W3C;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpointFactory;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * <p>Class for performing administrative tasks on a Virtuoso SPARQL endpoint
 * registry (adding new endpoints, reindexing existing endpoints, etc.)</p>
 * 
 * <p>NOTE: This class  be used if other threads are issuing HTTP requests
 * .  The issue is 
 * that the response timeout value for httpClient is global and shared.  
 * VirtuosoSPARQLRegistryAdmin changes this value during some operations, such
 * as pinging SPARQL endpoits.  Unfortunately, there is no fix for this because
 * the current httpclient (XLightwebHttpClient) does not support setting 
 * timeouts on a per-request basis.</p> 
 *
 * @author Ben Vandervalk
 */
public class VirtuosoSPARQLRegistryAdmin extends VirtuosoSPARQLRegistry implements SPARQLRegistryAdmin 
{
	public final static Log log = LogFactory.getLog(VirtuosoSPARQLRegistryAdmin.class);
	public final static String CONFIG_ROOT = "sadi.registry.sparql";
	
	/** Maximum allowable length for any subject/object regex, in characters */
	public final static int REGEX_MAX_LENGTH = 2048; 
	
	protected final static long DEFAULT_RESULTS_LIMIT = 50000; // triples
	
	public VirtuosoSPARQLRegistryAdmin() throws IOException 
	{
		this(Config.getConfiguration().subset(CONFIG_ROOT).getString(ENDPOINT_CONFIG_KEY));
	}

	public VirtuosoSPARQLRegistryAdmin(String URI) throws IOException 
	{
		this(URI, 
			Config.getConfiguration().subset(CONFIG_ROOT).getString(INDEX_GRAPH_CONFIG_KEY), 
			Config.getConfiguration().subset(CONFIG_ROOT).getString(ONTOLOGY_GRAPH_CONFIG_KEY)); 
	}

	public VirtuosoSPARQLRegistryAdmin(String URI, String indexGraphURI, String ontologyGraphURI) throws IOException 
	{
		super(URI, indexGraphURI, ontologyGraphURI, 
			Config.getConfiguration().subset(CONFIG_ROOT).getString(USERNAME_CONFIG_KEY), 
			Config.getConfiguration().subset(CONFIG_ROOT).getString(PASSWORD_CONFIG_KEY));
	}

	public void clearRegistry() throws IOException 
	{
		clearIndexes();
		clearOntology();
	}

	public void clearIndexes() throws IOException 
	{
		log.trace("clearing indexes from registry at " + getURI());
		clearGraph(getIndexGraphURI());
	}

	public void clearOntology() throws IOException 
	{
		log.trace("clearing predicate ontology from registry at " + getURI());
		clearGraph(getOntologyGraphURI());
	}

	public void addEndpoint(String endpointURI, EndpointType type) throws IOException 
	{
		if (hasEndpoint(endpointURI))
			return;
		
		// check if the endpoint is alive or dead, and record the result.
		updateEndpointStatus(endpointURI, type);
		// record the type of endpoint.
		setEndpointType(endpointURI, type);
		// record the fact that we have not yet computed an index
		setPredicateListIsComplete(endpointURI, false);
	}

	public void indexEndpoint(String endpointURI, EndpointType type) throws IOException
	{
		indexEndpoint(endpointURI, type, DEFAULT_RESULTS_LIMIT);
	}
	
	public void indexEndpoint(String endpointURI, EndpointType type, long maxResultsPerQuery) throws IOException 
	{
		log.trace("indexing SPARQL endpoint " + endpointURI);
		
		ServiceStatus status = ServiceStatus.OK;
		boolean indexIsComplete = true;
		
		if(updateEndpointStatus(endpointURI, type) == ServiceStatus.DEAD) {
			log.warn("Skipping indexing of endpoint " + endpointURI + " (did not respond to ping)");
			return;
		}
		
		setEndpointType(endpointURI, type);

		// D2R doesn't have support for regular expression FILTERs,
		// so in order to compute the subject/object patterns, we must
		// use the iteration method. -- BV
		
		if(type == EndpointType.D2R) {
			try {
				indexEndpointByIteration(endpointURI, type, maxResultsPerQuery);
			}
			catch(IOException e) {
				log.warn("failed to compute full index by iteration", e);
				status = ServiceStatus.SLOW;
				indexIsComplete = false;
			}
		}
		else {
			try {
				indexEndpointByQuery(endpointURI, type);
			}
			catch(IOException e) {

				log.warn("failed to index endpoint by querying, now attempting to index by iteration", e);
				status = ServiceStatus.SLOW; 
				try {
					indexEndpointByIteration(endpointURI, type, maxResultsPerQuery);
				}
				catch(IOException e2) {
					log.warn("failed to compute full index by iteration", e2);
					indexIsComplete = false;
				}
			}
		}

		setEndpointStatus(endpointURI, status);
		
		if(indexIsComplete)
			log.trace("complete index for SPARQL endpoint " + endpointURI + " successfully computed");
	}

	public void indexEndpointByQuery(String endpointURI, EndpointType type) throws IOException 
	{
		updatePredicateListByQuery(endpointURI, type);
		updateNumTriplesByQuery(endpointURI, type);
		updateRegexByQuery2(endpointURI, type, true);
		updateRegexByQuery2(endpointURI, type, false);
	}
	
	public void indexEndpointByIteration(String endpointURI, EndpointType type, long maxResultsPerQuery) throws IOException
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);

		if(maxResultsPerQuery == NO_RESULTS_LIMIT)
			maxResultsPerQuery = DEFAULT_RESULTS_LIMIT;
		
		long numTriples = 0;
		Set<String> predicates = new HashSet<String>();
		Set<String> subjectPrefixes = new HashSet<String>();
		Set<String> objectPrefixes = new HashSet<String>();
		boolean subjectRegexExceededMaxLength = false;
		boolean objectRegexExceededMaxLength = false;

		// ensure that the endpoint is alive, before erasing any existing index information
		if(endpoint.ping()) {
			clearPredicateEntries(endpointURI);
			setPredicateListIsComplete(endpointURI, false);
			setURIRegEx(endpointURI, "", true, false);
			setURIRegEx(endpointURI, "", false, false);
		}
			
		TripleIterator i = endpoint.iterator(maxResultsPerQuery);
		while(i.hasNext()) {

			Triple t = i.next();
			Node s = t.getSubject();
			Node p = t.getPredicate();
			Node o = t.getObject();

			numTriples++;

			if((numTriples % maxResultsPerQuery) == 0) {
				setNumTriplesLowerBound(endpointURI, numTriples);
			}

			String predicate = p.toString();
			if(!predicates.contains(predicate)) {
				try {
					predicates.add(predicate);
					addPredicateToRegistry(endpointURI, type, predicate);
					log.trace("added predicate " + predicate);
				}
				catch(AmbiguousPropertyTypeException e) {
					log.warn("skipped predicate " + predicate + " (ambiguous predicate)", e);
				}
			}

			if(!subjectRegexExceededMaxLength && s.isURI()) {
				String prefix = getURIPrefix(s.getURI());
				if (!subjectPrefixes.contains(prefix)) {
					subjectPrefixes.add(prefix);
					String regex = buildRegExFromPrefixes(subjectPrefixes);
					if(regex.length() > REGEX_MAX_LENGTH) {
						log.debug("full regex for subject URIs exceeds max length of " + REGEX_MAX_LENGTH + " chars, regex will be incomplete");
						subjectPrefixes.remove(prefix);
						subjectRegexExceededMaxLength = true;
					}
					else {
						setURIRegEx(endpointURI, regex, true, false);
						log.debug("added subject prefix " + prefix);
					}
				}
			}

			if(!objectRegexExceededMaxLength && o.isURI()) {
				String prefix = getURIPrefix(o.getURI());
				if (!objectPrefixes.contains(prefix)) {
					objectPrefixes.add(prefix);
					String regex = buildRegExFromPrefixes(objectPrefixes);
					if(regex.length() > REGEX_MAX_LENGTH) {
						log.debug("full regex for object URIs exceeds max length of " + REGEX_MAX_LENGTH + " chars, regex will be incomplete");
						objectPrefixes.remove(prefix);
						objectRegexExceededMaxLength = true;
					}
					else {
						setURIRegEx(endpointURI, regex, false, false);
						log.debug("added object prefix " + prefix);
					}
				}
			}

		}

		setPredicateListIsComplete(endpointURI, true);
		setNumTriples(endpointURI, numTriples);

		String regex = buildRegExFromPrefixes(subjectPrefixes);
		setURIRegEx(endpointURI, regex, true, true);

		regex = buildRegExFromPrefixes(objectPrefixes);
		setURIRegEx(endpointURI, regex, false, true);

		log.trace("indexing by iteration succeeded");
	}
	
	public void addPredicatesBySubjectURI(String endpointURI, EndpointType type, String subjectURI) throws IOException
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		
		String query = "CONSTRUCT { %u% ?p ?o } WHERE { %u% ?p ?o }";
		query = SPARQLStringUtils.strFromTemplate(query, subjectURI, subjectURI);
		
		Collection<Triple> triples = endpoint.constructQuery(query);
		Set<Node> predicatesAdded = new HashSet<Node>();
			
		if(triples.size() == 0) {
			log.warn(endpointURI + " does not contain " + subjectURI);
			return;
		}
		
		for(Triple t : triples) {
			Node p = t.getPredicate();
			if(predicatesAdded.contains(p))
				continue;
			try {
				addPredicateToRegistry(endpointURI, type, p.toString());
			}
			catch(IOException e) {
				log.warn("skipping predicate: ", e);
			}
			catch(AmbiguousPropertyTypeException e) {
				log.warn("skipping predicate: ", e);
			}
			predicatesAdded.add(p);
		}
		
	}
	
	public void updateNumTriplesByQuery(String endpointURI, EndpointType type) throws IOException
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		long numTriples;

		log.trace("querying number of triples in " + endpointURI);
		numTriples = getNumTriples(endpoint);
		
		log.trace("determined exact number of triples in " + endpointURI + ": " + String.valueOf(numTriples));
		setNumTriples(endpointURI, numTriples);
	}
	
	public void updatePredicateListByQuery(String endpointURI, EndpointType type) throws IOException
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		Set<String> predicates = null;
		
		log.trace("querying predicate list from " + endpointURI);
		predicates = endpoint.getPredicates();
		
		log.trace("retrieved full predicate list for " + endpointURI);
		clearPredicateEntries(endpointURI);
		addPredicatesToRegistry(endpointURI, type, predicates);
		setPredicateListIsComplete(endpointURI, true);
	}
	
	public void addPredicatesToRegistry(String endpointURI, EndpointType type, Collection<String> predicates) throws IOException
	{

		for (String predicate : predicates) {

			try {
				addPredicateToRegistry(endpointURI, type, predicate);
			} 
			catch (IOException e) {
				log.warn("failed to determine type of " + predicate, e);
				continue;
			} 
			catch (AmbiguousPropertyTypeException e) {
				log.warn("omitting predicate " + predicate, e);
				continue;
			}
			
		}
	}
	
	public void addPredicateToRegistry(String endpointURI, EndpointType type, String predicate) throws IOException, AmbiguousPropertyTypeException
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		boolean isDatatypeProperty = endpoint.isDatatypeProperty(predicate, true);
		addPredicateToOntology(predicate, isDatatypeProperty);
		addPredicateToIndex(endpointURI, predicate);
	}

	private void updateRegexByQuery2(String endpointURI, EndpointType type, boolean positionIsSubject) throws IOException
	{
		Set<String> prefixes = new HashSet<String>();
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		boolean regexIsComplete = true;
		
		// Query for the next subject/object URI that doesn't match any of the prefixes we've found so far.  
		// Get the prefix for that URI, add it to the list, and repeat.
		while(true) {
			
			String query;
			String filter;
			String regex = buildRegExFromPrefixes(prefixes);
				
			// regular expressions in Virtuoso SPARQL queries require two backslashes for escaping metacharacters
			if(type == EndpointType.VIRTUOSO)
				regex = regex.replaceAll("\\\\", "\\\\\\\\");
			
			if(prefixes.size() == 0) {
				if(positionIsSubject)
					filter = "isURI(?s)";
				else 
					filter = "isURI(?o)";
			}
			else {
				if(positionIsSubject)
					filter = "(isURI(?s) && !regex(?s, '" + regex + "'))";
				else
					filter = "(isURI(?o) && !regex(?o, '" + regex + "'))";
			}	
			
			query = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o . FILTER " + filter + "} LIMIT 1";
			
			Collection<Triple> triples = null;
			try {
				triples = endpoint.constructQuery(query);
			} 
			catch(IOException e) {
				log.warn("query failed during computation of regex", e);
				if(prefixes.size() > 0) {
					log.warn("recording partial regex");
					setURIRegEx(endpointURI, regex, positionIsSubject, false);
				}
				throw e;
			}

			if(triples.size() == 0)
				break; // Done!
			
			String uri;
			if(positionIsSubject)
				uri = triples.iterator().next().getSubject().getURI();
			else
				uri = triples.iterator().next().getObject().getURI();
			
			String prefix = getURIPrefix(uri);

			if(!prefixes.contains(prefix)) {
				log.debug("adding URI prefix " + prefix);
				prefixes.add(prefix);
				
				if(buildRegExFromPrefixes(prefixes).length() > REGEX_MAX_LENGTH) {
					log.debug("full regex exceeds maximum length of " + REGEX_MAX_LENGTH + " chars, regex will be incomplete");
					prefixes.remove(prefix);
					regexIsComplete = false;
					break;
				}
				
			}
			
		}

		String regex = buildRegExFromPrefixes(prefixes);
		setURIRegEx(endpointURI, regex, positionIsSubject, regexIsComplete);
	}

	/*
	private void updateRegexByIteration(String endpointURI, EndpointType type, boolean positionIsSubject) throws IOException 
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);

		boolean regexIsComplete = true;
		Set<String> prefixes = new HashSet<String>();

		TripleIterator i = endpoint.iterator();
		while(i.hasNext()) {

			Triple t = i.next();
			Node s = t.getSubject();
			Node o = t.getObject();

			String uri;
			if (positionIsSubject && s.isURI())
				uri = s.getURI();
			else if (!positionIsSubject && o.isURI())
				uri = o.getURI();
			else
				continue;

			String prefix = getURIPrefix(uri);

			if (!prefixes.contains(prefix)) {
				log.debug("adding URI prefix " + prefix);
				prefixes.add(prefix);

				if(buildRegExFromPrefixes(prefixes).length() > REGEX_MAX_LENGTH) {
					log.debug("full regex exceeds maximum length of " + REGEX_MAX_LENGTH + " chars, regex will be incomplete");
					prefixes.remove(prefix);
					regexIsComplete = false;
					break;
				}
			}
		}
			
		String regex = buildRegExFromPrefixes(prefixes);
		if(regex.length() > 0)
			setURIRegEx(endpointURI, regex, positionIsSubject, regexIsComplete);
	}
	*/
	
	public void setURIRegEx(String endpointURI, String regex, boolean positionIsSubject, boolean regexIsComplete) throws IOException 
	{
		// Clear any pre-existing regex and regex status.

		String deleteTemplate = "DELETE FROM GRAPH %u% { %u% %u% ?o } WHERE { %u% %u% ?o }";
		
		String regexPredicate;
		String regexIsCompletePredicate;
		
		if(positionIsSubject) {
			regexPredicate = SPARQLRegistryOntology.PREDICATE_SUBJECT_REGEX;
			regexIsCompletePredicate = SPARQLRegistryOntology.PREDICATE_SUBJECT_REGEX_IS_COMPLETE;
		}
		else {
			regexPredicate = SPARQLRegistryOntology.PREDICATE_OBJECT_REGEX;
			regexIsCompletePredicate = SPARQLRegistryOntology.PREDICATE_OBJECT_REGEX_IS_COMPLETE;
		}
			
		String deleteRegEx = SPARQLStringUtils.strFromTemplate(deleteTemplate, 
								getIndexGraphURI(),
								endpointURI,
								regexPredicate,
								endpointURI,
								regexPredicate);
		
		String deleteRegExIsComplete = SPARQLStringUtils.strFromTemplate(deleteTemplate, 
								getIndexGraphURI(),
								endpointURI,
								regexIsCompletePredicate,
								endpointURI,
								regexIsCompletePredicate);
		
		updateQuery(deleteRegEx);
		updateQuery(deleteRegExIsComplete);
		
		// Insert the new regex and regex status.
		
		String insertQuery = 
			"INSERT INTO GRAPH %u% { \n" +
			"    %u% %u% %s% .\n" +
			"    %u% %u% %s% .\n" +
			"}";
		
		insertQuery = SPARQLStringUtils.strFromTemplate(insertQuery,  
							getIndexGraphURI(),
							endpointURI, regexPredicate, regex,
							endpointURI, regexIsCompletePredicate, String.valueOf(regexIsComplete));
							
		updateQuery(insertQuery);
		
		if(positionIsSubject) 
			log.debug("set subject regex pattern for " + endpointURI + " to " + regex);
		else
			log.debug("set object regex pattern for " + endpointURI + " to " + regex);
			
	}

	private String getURIPrefix(String uri) 
	{
		final String delimiters[] = { "/", "#", ":" };
		
		// ignore delimiters that occur as the last character
		if(StringUtils.lastIndexOfAny(uri, delimiters) == (uri.length() - 1))
			uri = StringUtils.left(uri, uri.length() - 1);
		
		int chopIndex = StringUtils.lastIndexOfAny(uri, delimiters);

		String prefix;
		if (chopIndex == -1)
			prefix = uri;
		else {
			chopIndex++; // we want to include the last "/", ":", or "#" in the prefix
			prefix = StringUtils.substring(uri, 0, chopIndex);
		}

		return prefix;
	}
	
	private String buildRegExFromPrefixes(Collection<String> prefixes) {
		StringBuffer regex = new StringBuffer();
		int count = 0;
		for (String prefix : prefixes) {
			regex.append("^");
			regex.append(RegExUtils.escapeRegEx(prefix));
			if (count < prefixes.size() - 1)
				regex.append("|");
			count++;
		}
		return regex.toString();
	}
	
	/**
	 * <p>Truncate the given string at the first occurrence of a regular expression
	 * metacharacter that isn't "." or "-". This method is needed to make safe regular
	 * expressions, because there doesn't seem to be a way to escape regex
	 * metacharacters in Virtuoso SPARQL queries.</p>
	 * 
	 * <p>"." is omitted because it is relatively harmless to match "any character"
	 * instead of a dot, in the context of matching URIs.  "-" is omitted because
	 * it is only a metacharacter when found within a character class (e.g. "[A-Z]"),
	 *  and both "[" or "]" are guaranteed to be absent from the truncated string.</p> 
	 * 
	 * @param str
	 * @return the truncated string
	 */
	/*
	private String truncateStringAtFirstRegExMetaChar(String str) 
	{
		final char metaChars[] = {  '[', ']', '$', '^', '(', ')', '+', '*', '|' };
		int chopIndex = StringUtils.indexOfAny(str, metaChars);
		if(chopIndex != -1)
			return StringUtils.substring(str, 0, chopIndex);
		else
			return str;
	}
	*/

	/*
	public boolean updatePredicateList(String endpointURI, EndpointType type) throws IOException 
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		Set<String> predicateURIs = null;
		boolean retrievedFullPredicateList = false;
		
		log.trace("querying predicate list from " + endpointURI);

		try {
			predicateURIs = endpoint.getPredicates();
			log.trace("retrieved full predicate list for " + endpointURI);
			retrievedFullPredicateList = true;
		} catch (Exception e) {
			log.warn("failed to retrieve full predicate list for endpoint " + endpointURI + ": ", e);
			try {
				predicateURIs = endpoint.getPredicatesPartial();
			} catch(IOException e2) {
				log.warn("failed to get partial predicate list by querying with limits.", e2);
				predicateURIs = updatePredicatesByIteration(endpointURI, type);
			}
		}

		// If we failed to get any predicates at all, keep the list that's already in the registry.
		if(predicateURIs.size() > 0)
			clearPredicateEntries(endpointURI);
			 
		for (String predicateURI : predicateURIs) {

			boolean isDatatypeProperty = false;
			try {
				isDatatypeProperty = endpoint.isDatatypeProperty(predicateURI);
			} catch (IOException e) {
				log.warn("failed to determine whether " + predicateURI + " is a datatype or object property, omitting this predicate from the endpoint index", e);
				continue;
			} catch (AmbiguousPropertyTypeException e) {
				log.warn("omitting ambiguous predicate " + predicateURI, e);
				continue;
			}

			addPredicateToOntology(predicateURI, isDatatypeProperty);
			addPredicateToIndex(endpointURI, predicateURI);
		}

		return retrievedFullPredicateList;
	}
	*/
	
	/*
	public Set<String> updatePredicatesByIteration(String endpointURI, EndpointType type) throws IOException 
	{
		log.trace("downloading triples by chunks to determine predicate list");
		Set<String> predicates = new HashSet<String>();
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		try {
			TripleIterator i = endpoint.iterator();
			while(i.hasNext()) {
				String uri = i.next().getPredicate().getURI();
				if(!predicates.contains(uri)) {
					log.trace("added predicate " + uri + " to index for " + endpointURI);
					predicates.add(uri);
				}
			}
		} catch(IOException e) {
			log.warn("failed to download full list of triples for " + endpointURI + ", keeping partial predicate list");
		}
		return predicates;
	}
	*/
	
	/*
	public boolean updateEndpointSize(String endpointURI, EndpointType type) throws IOException 
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		boolean retrievedFullTripleCount = false;
		long numTriples;

		log.trace("querying number of triples in " + endpointURI);

		try {
			numTriples = getNumTriples(endpoint);
			log.trace("determined exact number of triples in " + endpointURI + ": " + String.valueOf(numTriples));
			retrievedFullTripleCount = true;
		} catch (Exception e) {
			log.warn("failed to query exact size (in triples) for endpoint " + endpoint.getURI());
			numTriples = getNumTriplesLowerBound(endpoint);
		}

		setNumTriples(endpointURI, numTriples);

		return retrievedFullTripleCount;
	}
	*/
	
	public void clearPredicateEntries(String endpointURI) throws IOException 
	{
		String query = "DELETE FROM GRAPH %u% { %u% %u% ?o } FROM %u% WHERE { %u% %u% ?o }";
		query = SPARQLStringUtils.strFromTemplate(query,
				getIndexGraphURI(), 
				endpointURI, SPARQLRegistryOntology.PREDICATE_HASPREDICATE,
				getIndexGraphURI(), 
				endpointURI, SPARQLRegistryOntology.PREDICATE_HASPREDICATE);
		updateQuery(query);
	}

	void setEndpointType(String endpointURI, EndpointType type) throws IOException 
	{
		String deleteQuery = "DELETE FROM GRAPH %u% { %u% %u% ?o } WHERE { %u% %u% ?o }";
		deleteQuery = SPARQLStringUtils.strFromTemplate(deleteQuery, 
				getIndexGraphURI(),
				endpointURI, W3C.PREDICATE_RDF_TYPE,
				endpointURI, W3C.PREDICATE_RDF_TYPE);
		updateQuery(deleteQuery);
		
		String insertQuery = "INSERT INTO GRAPH %u% { %u% %u% %s%^^%u% }";
		insertQuery = SPARQLStringUtils.strFromTemplate(insertQuery, 
				getIndexGraphURI(),
				endpointURI, W3C.PREDICATE_RDF_TYPE, type.toString(),
				XSDDatatype.XSDstring.getURI());
		updateQuery(insertQuery);
	}

	void setPredicateListIsComplete(String endpointURI, boolean indexStatus) throws IOException 
	{
		// Delete the existing status value
		String queryDeletePrev = "DELETE FROM GRAPH %u% { %u% %u% ?o } FROM %u% WHERE { %u% %u% ?o }";
		queryDeletePrev = SPARQLStringUtils	.strFromTemplate(queryDeletePrev,
						getIndexGraphURI(),
						endpointURI,
						SPARQLRegistryOntology.PREDICATE_PREDICATE_LIST_IS_COMPLETE,
						getIndexGraphURI(),
						endpointURI,
						SPARQLRegistryOntology.PREDICATE_PREDICATE_LIST_IS_COMPLETE);
		updateQuery(queryDeletePrev);

		// Write the new status value
		String queryRecordSuccess = "INSERT INTO GRAPH %u% { %u% %u% %s% }";
		//String status = indexStatus ? "true" : "false";
		queryRecordSuccess = SPARQLStringUtils.strFromTemplate(queryRecordSuccess,
						getIndexGraphURI(),
						endpointURI, SPARQLRegistryOntology.PREDICATE_PREDICATE_LIST_IS_COMPLETE, String.valueOf(indexStatus));
		updateQuery(queryRecordSuccess);
	}

	public ServiceStatus updateEndpointStatus(String endpointURI, EndpointType type) throws IOException 
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		ServiceStatus status;
	
		if (endpoint.ping())
			status = ServiceStatus.OK;
		else
			status = ServiceStatus.DEAD;

		setEndpointStatus(endpointURI, status);
		return status;
	}

	public void setEndpointStatus(String endpointURI, ServiceStatus status) throws IOException 
	{
		log.trace("setting status of " + endpointURI + " to " + status.toString());
		
		// Delete the existing status value
		String queryDeletePrev = "DELETE FROM GRAPH %u% { %u% %u% ?o } FROM %u% WHERE { %u% %u% ?o }";
		queryDeletePrev = SPARQLStringUtils	.strFromTemplate(	queryDeletePrev,
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

	private void addPredicateToIndex(String endpointURI, String predicateURI) throws IOException 
	{
		// TEMP HACK: Ignore "junk" data that comes with all Virtuoso installations.
		if(predicateURI.matches("^http://www\\.openlinksw\\.com/.*")) { 
			log.trace("omitting " + predicateURI + " from index (Virtuoso sample data)");
			return;
		}
		
		String queryAddPredURI = "INSERT INTO GRAPH %u% { %u% %u% %u% }";
		queryAddPredURI = SPARQLStringUtils.strFromTemplate(
				queryAddPredURI, getIndexGraphURI(),
				endpointURI,
				SPARQLRegistryOntology.PREDICATE_HASPREDICATE,
				predicateURI);
		updateQuery(queryAddPredURI);
	}

	public void refreshStatusOfAllEndpoints() throws IOException 
	{
		Collection<SPARQLEndpoint> endpoints = getAllEndpoints();
		for (SPARQLEndpoint endpoint : endpoints) {
			try {
				endpoint.getPredicates();
				setEndpointStatus(endpoint.getURI(), ServiceStatus.OK);
			} catch (HttpStatusException e) {
				if(e.getStatusCode() == HttpResponse.HTTP_STATUS_GATEWAY_TIMEOUT) {
					setEndpointStatus(endpoint.getURI(), ServiceStatus.SLOW);
				}
			} catch (IOException e) {
				setEndpointStatus(endpoint.getURI(), ServiceStatus.DEAD);
			}
		}
	}

	public void removeEndpoint(String endpointURI) throws IOException 
	{
		log.trace("Removing endpoint " + endpointURI + " from SPARQL registry");
		deleteDirectedClosure(endpointURI, getIndexGraphURI());
	}

	public void addPredicateToOntology(String predicateURI, boolean isDatatypeProperty) throws IOException 
	{
		String type = isDatatypeProperty ? "DatatypeProperty" : "ObjectProperty";

		// Remove the previously assigned type (if any).
		removePredicateFromOntology(predicateURI);

		// Insert the new value.
		String addPredQuery = "INSERT INTO GRAPH %u% { %u% %u% %u% }";
		addPredQuery = SPARQLStringUtils.strFromTemplate(addPredQuery,
				getOntologyGraphURI(), 
				predicateURI, W3C.PREDICATE_RDF_TYPE, W3C.OWL_PREFIX + type);
		updateQuery(addPredQuery);
		
	}
	
	public void removePredicateFromOntology(String predicateURI) throws IOException 
	{
		// Remove the previously assigned type (if any).
		String deleteQuery = "DELETE FROM GRAPH %u% { %u% %u% ?type } WHERE { %u% %u% ?type }";
		deleteQuery = SPARQLStringUtils.strFromTemplate(deleteQuery,
				getOntologyGraphURI(), 
				predicateURI,	W3C.PREDICATE_RDF_TYPE, 
				predicateURI,	W3C.PREDICATE_RDF_TYPE);
		updateQuery(deleteQuery);
	}

	public long getNumTriples(SPARQLEndpoint endpoint) throws IOException 
	{
		String query = "SELECT COUNT(*) WHERE { ?s ?p ?o }";
		List<Map<String, String>> results = endpoint.selectQuery(query);
		if (results.size() == 0) 
			throw new RuntimeException("no value returned for COUNT query");

		String columnName = results.get(0).keySet().iterator().next();
		return Long.valueOf(results.get(0).get(columnName));
	}

	/*
	public long getNumTriplesLowerBound(SPARQLEndpoint endpoint) throws IOException 
	{
		String probeQuery = "SELECT * WHERE { ?s ?p ?o }";
		return endpoint.getResultsCountLowerBound(probeQuery, 1000000, 20 * 1000);
	}
	*/
	
	public void setNumTriples(String endpointURI, long numTriples) throws IOException 
	{
		// Delete any existing value for numTriples/numTriplesLowerBound first.
		clearNumTriples(endpointURI);
		
		// Insert the new value.
		String query = "INSERT INTO GRAPH %u% { %u% %u% %v% }";
		query = SPARQLStringUtils.strFromTemplate(query,
				getIndexGraphURI(), endpointURI,
				SPARQLRegistryOntology.PREDICATE_NUMTRIPLES,
				String.valueOf(numTriples));
		updateQuery(query);
	}

	public void setNumTriplesLowerBound(String endpointURI, long numTriples) throws IOException
	{
		// Delete any existing value for numTriples/numTriplesLowerBound first.
		clearNumTriples(endpointURI);
		
		// Insert the new value.
		String query = "INSERT INTO GRAPH %u% { %u% %u% %v% }";
		query = SPARQLStringUtils.strFromTemplate(query,
				getIndexGraphURI(), endpointURI,
				SPARQLRegistryOntology.PREDICATE_NUMTRIPLES_LOWER_BOUND,
				String.valueOf(numTriples));
		updateQuery(query);
	}
	
	private void clearNumTriples(String endpointURI) throws IOException 
	{
		String deleteQueryTemplate = "DELETE FROM GRAPH %u% { %u% %u% ?o } FROM %u% WHERE { %u% %u% ?o }";
		
		String deleteQuery1 = SPARQLStringUtils.strFromTemplate(deleteQueryTemplate,
									getIndexGraphURI(), endpointURI,
									SPARQLRegistryOntology.PREDICATE_NUMTRIPLES,
									getIndexGraphURI(), endpointURI,
									SPARQLRegistryOntology.PREDICATE_NUMTRIPLES);
		updateQuery(deleteQuery1);
		
		String deleteQuery2 = SPARQLStringUtils.strFromTemplate(deleteQueryTemplate,
									getIndexGraphURI(), endpointURI,
									SPARQLRegistryOntology.PREDICATE_NUMTRIPLES_LOWER_BOUND,
									getIndexGraphURI(), endpointURI,
									SPARQLRegistryOntology.PREDICATE_NUMTRIPLES_LOWER_BOUND);
		updateQuery(deleteQuery2);
	}
	
	public void removeAmbiguousPropertiesFromOntology() throws IOException
	{
		log.trace("detecting and removing all ambiguous properties");
		OntModel predicateOntology = getPredicateOntology();
		Iterator it = predicateOntology.listAllOntProperties();

		while (it.hasNext()) {
			OntProperty predicate = (OntProperty)it.next();
			String p = predicate.toString();
			if(isAmbiguousProperty(predicate)) {
				log.trace("removing ambiguous property " + p);
				removePredicateFromOntology(p);
			}
		}
		
	}
	
	protected boolean isAmbiguousProperty(OntProperty predicate) throws IOException
	{
		String p = predicate.toString();

		if(!predicate.isDatatypeProperty() && !predicate.isObjectProperty()) {
			log.warn(p + " is neither a datatype property or an object property, assuming it is unambiguous");
			return false;
		}
		
		log.trace("checking if " + p + " is an ambiguous property");
		
		Collection<SPARQLEndpoint> endpoints = findEndpointsByPredicate(p);
		
		String query = null;
		if(predicate.isDatatypeProperty()) 
			query = "SELECT * WHERE { ?s %u% ?o . FILTER (!isLiteral(?o)) } LIMIT 1";
		else
			query = "SELECT * WHERE { ?s %u% ?o . FILTER (isLiteral(?o)) } LIMIT 1";
		
		query = SPARQLStringUtils.strFromTemplate(query, p);
		
		for(SPARQLEndpoint endpoint: endpoints) {
			try {
				log.trace("querying for illegal object values for " + p + " from " + endpoint.getURI());
				if(endpoint.selectQuery(query).size() > 0)
					return true;
			}
			catch(IOException e) {
				log.warn("query failed on " + endpoint.getURI(), e);
			}
		}

		return false;
	}
	
	
	public static class CommandLineOptions {

		public enum OperationType {
			ADD, 
			INDEX, 
			REMOVE, 
			CLEAR_REGISTRY, 
			SET_ENDPOINT_TYPE, 
			SET_RESULTS_LIMIT,
			UPDATE_SUBJECT_REGEX,
			REMOVE_AMBIGUOUS_PROPERTIES,
			SET_ENDPOINT_STATUS,
			ADD_PREDICATES_BY_SUBJECT_URI,
		};

		public static class Operation {
			String arg;
			OperationType opType;

			public Operation(String arg, OperationType opType) {
				this.arg = arg;
				this.opType = opType;
			}
		};

		public List<Operation> operations = new ArrayList<Operation>();

		@Option(name = "-r", usage = "URI of registry to be updated")
		public String registryURI = Config.getConfiguration().subset(CONFIG_ROOT).getString(ENDPOINT_CONFIG_KEY);
		
		@Option(name = "-l", usage = "max results per query")
		public void setResultsLimit(long limit) { operations.add(new Operation(String.valueOf(limit), OperationType.SET_RESULTS_LIMIT)); }

		@Option(name = "-c", usage = "clear all contents of the registry")
		public void clearRegistry(boolean unused) { operations.add(new Operation(null, OperationType.CLEAR_REGISTRY)); }

		@Option(name = "-a", usage = "add an endpoint to the registry (without building an index)")
		public void addEndpoint(String endpointURI) { operations.add(new Operation(endpointURI, OperationType.ADD)); }

		@Option(name = "-i", usage = "index an endpoint")
		public void indexEndpoint(String endpointURI) { operations.add(new Operation(endpointURI, OperationType.INDEX)); }

		@Option(name = "-d", usage = "delete an endpoint from the registry")
		public void removeEndpoint(String endpointURI) { operations.add(new Operation(endpointURI, OperationType.REMOVE));	}

		@Option(name = "-t", usage = "specify endpoint type (options: \"VIRTUOSO\", \"D2R\"")
		public void setEndpointType(String type) { operations.add(new Operation(type, OperationType.SET_ENDPOINT_TYPE)); }

		@Option(name = "-s", usage = "update the regular expression for subject URIs of the given endpoint")
		public void updateSubjectRegEx(String endpointURI) { operations.add(new Operation(endpointURI, OperationType.UPDATE_SUBJECT_REGEX)); }

		@Option(name = "-R", usage = "detect and remove ambiguous properties (properties that have both URIs and literals as object values)")
		public void removeAmbiguousProperties(boolean unused) { operations.add(new Operation(null, OperationType.REMOVE_AMBIGUOUS_PROPERTIES)); } 

		// NOTE: you must always quote the values of this option on the commandline, since they contain spaces 
		@Option(name = "-S", usage = "manually set endpoint status (choices: '<endpoint>,DEAD', '<endpoint>,SLOW', '<endpoint>,OK')")
		public void updateEndpointStatus(String endpointAndStatus) { operations.add(new Operation(endpointAndStatus, OperationType.SET_ENDPOINT_STATUS)); }
		
		@Option(name = "-A", usage = "syntax: 'endpointURI,subjectURI'; add any predicates attached to subjectURI to the index for endpointURI")
		public void addPredicatesBySubjectURI(String endpointAndSubject) { operations.add(new Operation(endpointAndSubject, OperationType.ADD_PREDICATES_BY_SUBJECT_URI)); }
	}

	public static void main(String[] args) throws IOException 
	{
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);

		try {
			cmdLineParser.parseArgument(args);
			
			log.debug("Registry URI: " + options.registryURI);
			SPARQLRegistryAdmin registry = new VirtuosoSPARQLRegistryAdmin(options.registryURI);

			/* Switches like -t (endpoint type; e.g. "VIRTUOSO") apply to all endpoints, unless there
			 * is more than one occurrence on the command line.  In the latter case, each instance of 
			 * "-t" applies to the endpoints *following* it, up until the next occurence of "-t".
			 */
			
			int typeCount = 0;
			EndpointType endpointType = null;
			int limitCount = 0;
			long resultsLimit = DEFAULT_RESULTS_LIMIT;
			
			for (CommandLineOptions.Operation op : options.operations) {
				switch(op.opType) {
				case SET_ENDPOINT_TYPE:
					endpointType = EndpointType.valueOf(StringUtils.upperCase(op.arg));
					typeCount++;
					break;
				case SET_RESULTS_LIMIT:
					resultsLimit = Long.valueOf(op.arg);
					limitCount++;
					break;
				default:
					break;
				}
			}
			if(typeCount != 1)
				endpointType = EndpointType.VIRTUOSO; // the default
			if(limitCount != 1)
				resultsLimit = DEFAULT_RESULTS_LIMIT;
			
			
			for (CommandLineOptions.Operation op : options.operations) {
				
				try {
					switch (op.opType) {
					case SET_ENDPOINT_TYPE:
						endpointType = EndpointType.valueOf(StringUtils.upperCase(op.arg));
						break;
					case SET_RESULTS_LIMIT:
						resultsLimit = Long.valueOf(op.arg);
						break;
					case ADD:
						registry.addEndpoint(op.arg, endpointType);
						break;
					case INDEX:
						registry.indexEndpoint(op.arg, endpointType, resultsLimit);
						break;
					case REMOVE:
						registry.removeEndpoint(op.arg);
						break;
					/*
					case UPDATE_SUBJECT_REGEX:
						registry.updateRegex(op.arg, endpointType, true);
						break;
					*/
					case REMOVE_AMBIGUOUS_PROPERTIES:
						registry.removeAmbiguousPropertiesFromOntology();
						break;
					case SET_ENDPOINT_STATUS:
						String statusArg[] = op.arg.split(",");
						if(statusArg.length != 2)
							throw new CmdLineException("format of arg to -S must be '<endpointURI>,<status>'");
						ServiceStatus newStatus = ServiceStatus.valueOf(StringUtils.upperCase(statusArg[1]));
						registry.setEndpointStatus(statusArg[0], newStatus);
						break;
					case ADD_PREDICATES_BY_SUBJECT_URI:
						String subjectArg[] = op.arg.split(",");
						if(subjectArg.length != 2)
							throw new CmdLineException("format of arg to -A must be '<endpointURI>,<subjectURI>'");
						registry.addPredicatesBySubjectURI(subjectArg[0], endpointType, subjectArg[1]);
						break;
					case CLEAR_REGISTRY:
						registry.clearRegistry();
						break;
						
					default:
					}
				} catch (Exception e) {
					log.error("operation " + op.opType + " failed on " + op.arg, e);
				}
			}
		} catch (CmdLineException e) {
			log.error(e);
			log.error("Usage: sparqlreg [-t endpointType] [-r registryURI] [-i endpointURI] [-a endpointURI] [-d endpointURI] [-c] [-R]");
			cmdLineParser.printUsage(System.err);
		}

	}

}
