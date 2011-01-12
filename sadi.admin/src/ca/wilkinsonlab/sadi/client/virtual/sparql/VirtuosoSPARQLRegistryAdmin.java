package ca.wilkinsonlab.sadi.client.virtual.sparql;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import ca.wilkinsonlab.sadi.utils.RegExUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.graph.BoundedBreadthFirstIterator;
import ca.wilkinsonlab.sadi.utils.graph.RDFTypeConstraint;
import ca.wilkinsonlab.sadi.utils.graph.SPARQLSearchNode;
import ca.wilkinsonlab.sadi.utils.graph.SearchNode;
import ca.wilkinsonlab.sadi.vocab.SPARQLRegistryOntology;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpointFactory;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint.EndpointType;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint.TripleIterator;
import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import virtuoso.jena.driver.VirtModel;

/**
 * <p>Class for performing administrative tasks on a Virtuoso SPARQL endpoint
 * registry (adding new endpoints, reindexing existing endpoints, etc.)</p>
 * 
 * @author Ben Vandervalk
 */
public class VirtuosoSPARQLRegistryAdmin implements SPARQLRegistryAdmin {
	
	public final static Log log = LogFactory.getLog(VirtuosoSPARQLRegistryAdmin.class);
	
	public final static String CONFIG_ROOT = "sadi.registry.sparql";
	protected final static Configuration config = Config.getConfiguration().subset(CONFIG_ROOT);
	
	protected final static String INDEX_GRAPH_CONFIG_KEY = "indexGraph";
	protected final static String PREDICATE_EXCLUDE_REGEX = "predicateExcludeRegEx";
		
	protected String indexGraph;

	public final static String VIRTUOSO_CONFIG_ROOT = "sadi.registry.sparql.virtuoso";
	protected final static Configuration virtuosoConfig = Config.getConfiguration().subset(VIRTUOSO_CONFIG_ROOT);

	protected final static String VIRTUOSO_HOSTNAME_CONFIG_KEY = "hostname";
	protected final static String VIRTUOSO_PORT_CONFIG_KEY = "port"; 
	protected final static String VIRTUOSO_USERNAME_CONFIG_KEY = "username";
	protected final static String VIRTUOSO_PASSWORD_CONFIG_KEY = "password";
	protected String virtuosoHost;
	protected int virtuosoPort;
	protected String virtuosoUsername;
	protected String virtuosoPassword;
	
	protected Model indexModel;
	protected Model ontologyModel;
	protected OntModel registryOntology;
	
	/** regular expressions for predicates that should *not* be indexed. */
	protected Collection<Pattern> predicateExcludeList;
	
	/** Maximum allowable length for any subject/object regex, in characters */
	public final static int REGEX_MAX_LENGTH = 2048; 
	protected final static long DEFAULT_RESULTS_LIMIT = 50000; // triples
	/** Maximum depth when performing ad-hoc indexing by traversal */
	protected static final int MAX_TRAVERSAL_DEPTH = 7;

	
	public VirtuosoSPARQLRegistryAdmin() throws IOException 
	{
		this(virtuosoConfig.getString(VIRTUOSO_HOSTNAME_CONFIG_KEY),
			virtuosoConfig.getInt(VIRTUOSO_PORT_CONFIG_KEY),
			config.getString(INDEX_GRAPH_CONFIG_KEY),
			virtuosoConfig.getString(VIRTUOSO_USERNAME_CONFIG_KEY),
			virtuosoConfig.getString(VIRTUOSO_PASSWORD_CONFIG_KEY));
	}
	
	public VirtuosoSPARQLRegistryAdmin(String virtuosoHost, int virtuosoPort, String virtuosoUsername, String virtuosoPassword) 
	{
		this(virtuosoHost, 
			virtuosoPort, 
			Config.getConfiguration().subset(CONFIG_ROOT).getString(INDEX_GRAPH_CONFIG_KEY),
			virtuosoUsername,
			virtuosoPassword);
	}
	
	public VirtuosoSPARQLRegistryAdmin(String virtuosoHost, int virtuosoPort, String indexGraph, String virtuosoUsername, String virtuosoPassword) {
		
		setVirtuosoHost(virtuosoHost);
		setVirtuosoPort(virtuosoPort);
		setIndexGraph(indexGraph);
		setVirtuosoUsername(virtuosoUsername);
		setVirtuosoPassword(virtuosoPassword);
		
		initPredicateExcludeList();
		initIndexModel();
		initRegistryOntology();
	}
	
	public String getVirtuosoConnectString(String hostname, int port) {
		return "jdbc:virtuoso://" + hostname + ":" + String.valueOf(port);
	}
	
	public String getVirtuosoHost() {
		return virtuosoHost;
	}

	public void setVirtuosoHost(String virtuosoHost) {
		this.virtuosoHost = virtuosoHost;
	}

	public int getVirtuosoPort() {
		return virtuosoPort;
	}

	public void setVirtuosoPort(int virtuosoPort) {
		this.virtuosoPort = virtuosoPort;
	}

	public String getVirtuosoUsername() {
		return virtuosoUsername;
	}

	public void setVirtuosoUsername(String virtuosoUsername) {
		this.virtuosoUsername = virtuosoUsername;
	}

	public String getVirtuosoPassword() {
		return virtuosoPassword;
	}

	public void setVirtuosoPassword(String virtuosoPassword) {
		this.virtuosoPassword = virtuosoPassword;
	}

	public String getIndexGraph() {
		return indexGraph;
	}

	public void setIndexGraph(String indexGraph) {
		this.indexGraph = indexGraph;
	}
	
	protected void initPredicateExcludeList() {
		predicateExcludeList = new ArrayList<Pattern>();
		Configuration config = ca.wilkinsonlab.sadi.admin.Config.getConfiguration().subset(CONFIG_ROOT);
		for(Object regex : config.getList(PREDICATE_EXCLUDE_REGEX)) {
			predicateExcludeList.add(Pattern.compile((String)regex));
		}
	}
	
	protected boolean predicateMatchesExcludeList(String predicate) {
		
		for(Pattern regex : predicateExcludeList) {
			if(regex.matcher(predicate).find()) {
				return true;
			}
		}
		return false;
	}

	protected void initIndexModel() {
		indexModel = VirtModel.createDatabaseModel(getIndexGraph(), 
				getVirtuosoConnectString(getVirtuosoHost(), getVirtuosoPort()), 
				getVirtuosoUsername(), 
				getVirtuosoPassword());
	}
	
	protected void initRegistryOntology() {
		// in memory OWL model with no reasoning
		registryOntology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		registryOntology.read(SPARQLRegistryOntology.URI);
	}
	
	protected OntModel getRegistryOntology() {
		return registryOntology;
	}
	
	protected Model getIndexModel() {
		return indexModel;
	}
	
	protected void closeAllModels() {
		getIndexModel().close();
		getRegistryOntology().close();
	}
	
	public void clearRegistry() throws IOException 
	{
		log.trace("clearing registry");
		getIndexModel().removeAll();
	}

	public void addEndpoint(String endpointURI, EndpointType type) throws IOException 
	{
		if (endpointInIndex(endpointURI)) {
			log.warn("cannot add " + endpointURI + ", endpoint is already in index");
			return;
		}
		
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		Model endpointIndex = ModelFactory.createMemModelMaker().createFreshModel();
		
		try {

			if(endpoint.ping()) {
				setEndpointStatus(endpointIndex, endpointURI, ServiceStatus.OK);
			} else {
				setEndpointStatus(endpointIndex, endpointURI, ServiceStatus.DEAD);
			}

			addEndpointType(endpointIndex, endpointURI, endpoint.getEndpointType());
			addPredicateListStatus(endpointIndex, endpointURI, false);
			addSubjectRegExStatus(endpointIndex, endpointURI, false);
			addObjectRegExStatus(endpointIndex, endpointURI, false);
			addIndexCreationTime(endpointIndex, endpointURI, new Date());

			updateEndpointIndex(endpointIndex, endpointURI);
			endpointIndex.close();
			log.trace("successfully added (empty) entry for " + endpointURI + " to registry");

		} finally {
			endpointIndex.close();
		}
	}
	
	protected void updateEndpointIndex(Model endpointEntry, String endpointURI) {
		log.trace("writing entry for " + endpointURI + " to registry");
		getIndexModel().removeAll(getResource(endpointURI), (Property)null, (RDFNode)null);
		getIndexModel().add(endpointEntry);
	}

	protected boolean endpointInIndex(String endpointURI) {
		Resource endpoint = getIndexModel().createResource(endpointURI);
		return getIndexModel().contains(endpoint, getProperty(SPARQLRegistryOntology.ENDPOINT_STATUS), (RDFNode)null);
	}
	
	protected Property getProperty(String uri) {
		
		Property p = getRegistryOntology().getProperty(uri);
		if(p == null) {
			throw new RuntimeException("property " + uri + " is not defined in " + SPARQLRegistryOntology.URI);
		}
		return p;
	}
	
	protected Resource getResource(String uri) {
		return getIndexModel().createResource(uri);
	}
	
	public void indexEndpoint(String endpointURI, EndpointType type) throws IOException
	{
		indexEndpoint(endpointURI, type, DEFAULT_RESULTS_LIMIT);
	}
	
	public void indexEndpoint(String endpointURI, EndpointType type, long maxResultsPerQuery) throws IOException 
	{
		log.trace("indexing SPARQL endpoint " + endpointURI);
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		
		Model endpointIndex = null;
		try {
			if(!endpoint.ping()) {
				throw new IOException(endpointURI + " did not respond to ping");
			}

			/* D2R doesn't have support for regular expression FILTERs,
			 * so in order to compute the subject/object regex patterns, we must
			 * use the iteration method. -- BV */

			if(type == EndpointType.D2R) {
				try {
					endpointIndex = buildEndpointIndexByIteration(endpoint, maxResultsPerQuery);
				} catch(IOException e) {
					log.warn("failed to compute index by iteration", e);
					return;
				}
			} else {
				try {
					endpointIndex = buildEndpointIndexByQuery(endpoint);
				} catch(IOException e) {
					log.warn("failed to complete index by querying, falling back to indexing by iteration", e);
					try {
						endpointIndex = buildEndpointIndexByIteration(endpoint, maxResultsPerQuery);
						setEndpointStatus(endpointIndex, endpointURI, ServiceStatus.SLOW);
					} catch(IOException e2) {
						log.warn("failed to compute index by iteration, try indexing by traversal instead", e2);
						return;
					}
				}
			}

			updateEndpointIndex(endpointIndex, endpointURI);
			log.trace("index for SPARQL endpoint " + endpointURI + " successfully computed");

		} finally {
			if(endpointIndex != null) {
				endpointIndex.close();
			}
		}
		
	}

	public Model buildEndpointIndexByIteration(SPARQLEndpoint endpoint, long maxResultsPerQuery) throws IOException
	{
		String endpointURI = endpoint.getURI();
		Model endpointIndex = ModelFactory.createMemModelMaker().createFreshModel();
		
		log.trace("building index for " + endpointURI + " by iteration");
		
		if(maxResultsPerQuery == SPARQLEndpoint.NO_RESULTS_LIMIT)
			maxResultsPerQuery = DEFAULT_RESULTS_LIMIT;
		
		long numTriples = 0;
		Set<String> predicates = new HashSet<String>();
		RegExUtils.URIRegExBuilder subjectRegExBuilder = new RegExUtils.URIRegExBuilder(REGEX_MAX_LENGTH);
		RegExUtils.URIRegExBuilder objectRegExBuilder = new RegExUtils.URIRegExBuilder(REGEX_MAX_LENGTH);

		TripleIterator i = endpoint.iterator(maxResultsPerQuery);
		while(i.hasNext()) {

			Triple t = i.next();
			Node s = t.getSubject();
			Node p = t.getPredicate();
			Node o = t.getObject();

			numTriples++;
			
			String predicate = p.toString();
			if(!predicates.contains(predicate)) {
				predicates.add(predicate);
				addPredicate(endpointIndex, endpoint, getResource(predicate));
			}

			// Check for uris that are empty or consist only of whitespace.
			// Believe it or not, this actually happens in some of the Bio2RDF data. -- BV

			if(s.isURI() && s.getURI().trim().length() > 0) {
				subjectRegExBuilder.addPrefixOfURIToRegEx(s.getURI());
			}
			
			if(o.isURI() && o.getURI().trim().length() > 0) {
				objectRegExBuilder.addPrefixOfURIToRegEx(o.getURI());
			}
		}

		addEndpointType(endpointIndex, endpointURI, endpoint.getEndpointType());
		setEndpointStatus(endpointIndex, endpointURI, ServiceStatus.OK);
		addPredicateListStatus(endpointIndex, endpointURI, true);
		addNumTriples(endpointIndex, endpointURI, numTriples);
		addSubjectRegEx(endpointIndex, endpointURI, subjectRegExBuilder.getRegEx());
		addSubjectRegExStatus(endpointIndex, endpointURI, true);
		addObjectRegEx(endpointIndex, endpointURI, objectRegExBuilder.getRegEx());
		addObjectRegExStatus(endpointIndex, endpointURI, true);
		addIndexCreationTime(endpointIndex, endpointURI, new Date());
		updateResultsLimitWithDefault(endpointIndex, endpoint, SPARQLEndpoint.NO_RESULTS_LIMIT);
		
		log.trace("completed building index for " + endpointURI + " by iteration");
		
		return endpointIndex;
	}
	
	public Model buildEndpointIndexByQuery(SPARQLEndpoint endpoint) throws IOException 
	{
		String endpointURI = endpoint.getURI();
		
		log.trace("building index for " + endpointURI + " by query");
		
		Model endpointIndex = ModelFactory.createMemModelMaker().createFreshModel();
		
		addEndpointType(endpointIndex, endpointURI, endpoint.getEndpointType());
		setEndpointStatus(endpointIndex, endpointURI, ServiceStatus.OK);
		addPredicateListByQuery(endpointIndex, endpoint);
		addPredicateListStatus(endpointIndex, endpointURI, true);
		addNumTriplesByQuery(endpointIndex, endpoint);
		
		boolean regExComplete;
		regExComplete = addRegexByQuery(endpointIndex, endpoint, true);
		addSubjectRegExStatus(endpointIndex, endpointURI, regExComplete);
		regExComplete = addRegexByQuery(endpointIndex, endpoint, false);
		addObjectRegExStatus(endpointIndex, endpointURI, regExComplete);
		updateResultsLimitWithDefault(endpointIndex, endpoint, SPARQLEndpoint.NO_RESULTS_LIMIT);
		
		addIndexCreationTime(endpointIndex, endpointURI, new Date());
		
		log.trace("completed building index for " + endpointURI + " by query");
		
		return endpointIndex;
	}

	public void indexEndpointByTraversal(String endpointURI, EndpointType type, List<String> rootURIs) throws IOException {

		log.trace("building index for " + endpointURI + " by traversal");
		
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		
		if(!endpoint.ping()) {
			log.warn("aborting indexing of " + endpoint.getURI() + " by traversal, did not respond to ping");
			return;
		}
	
		Model endpointIndex = buildEndpointIndexByTraversal(endpoint, rootURIs);
		updateEndpointIndex(endpointIndex, endpointURI);
		
		log.trace("indexing by traversal successful for " + endpoint.getURI());
	}
	
	public Model buildEndpointIndexByTraversal(SPARQLEndpoint endpoint, List<String> rootURIs) throws IOException {
		
		String endpointURI = endpoint.getURI();
		Model endpointIndex = ModelFactory.createMemModelMaker().createFreshModel();
		
		RDFTypeConstraint typeConstraint = new RDFTypeConstraint(endpoint);

		RegExUtils.URIRegExBuilder subjectRegExBuilder = new RegExUtils.URIRegExBuilder(REGEX_MAX_LENGTH);
		RegExUtils.URIRegExBuilder objectRegExBuilder = new RegExUtils.URIRegExBuilder(REGEX_MAX_LENGTH);
		
		Set<String> predicates = new HashSet<String>();
		
		for(String rootURI : rootURIs) {
			Resource root = ResourceFactory.createResource(rootURI);
			SearchNode<Resource> startNode = new SPARQLSearchNode(endpoint, root);
			try {
				Iterator<Resource> i = new BoundedBreadthFirstIterator<Resource>(startNode, typeConstraint, MAX_TRAVERSAL_DEPTH);
				while(i.hasNext()) {

					Resource subject = i.next();
					if(subject.isURIResource()) {
					
						String uri = subject.getURI();

						// Check for uris that are empty or consist only of whitespace.
						// Believe it or not, this actually happens in some of the Bio2RDF data. -- BV
						if(uri.trim().length() == 0) {
							continue;
						}
						
						subjectRegExBuilder.addPrefixOfURIToRegEx(uri);

						log.trace("visiting subject " + uri);
						
						String queryType = "CONSTRUCT { %u% ?p ?o } WHERE { %u% ?p ?o }";
						queryType = SPARQLStringUtils.strFromTemplate(queryType, uri, uri);
						Collection<Triple> triples = endpoint.constructQuery(queryType);

						for(Triple triple : triples) {
							Node p = triple.getPredicate();
							Node o = triple.getObject();
							if(!predicates.contains(p.getURI())) {
								log.trace("adding predicate " + p.getURI());
								predicates.add(p.getURI());
								addPredicate(endpointIndex, endpoint, getResource(p.getURI()));
							}
							if(o.isURI()) {
								objectRegExBuilder.addPrefixOfURIToRegEx(o.getURI());
							}
							if(p.toString().equals(RDF.type.getURI()) && o.isURI()) {
								Resource rdfType = ResourceFactory.createResource(o.getURI());
								typeConstraint.setTypeAsVisited(rdfType);
							}
						}
					}
				}
			} catch(RuntimeException e) {
				// the BreadthFirstIterator can only throw a RuntimeException, so IOExceptions in 
				// SPARQLSearchNode / VisitEachRDFTypeOnlyOnce are wrapped
				if(e.getCause() instanceof IOException) {
					throw (IOException)e.getCause();
				} else {
					throw e;
				}
			}
		}

		// Note: We have no idea what the number of triples is for the endpoint, so we don't set it here.
		
		addEndpointType(endpointIndex, endpointURI, endpoint.getEndpointType());
		setEndpointStatus(endpointIndex, endpointURI, ServiceStatus.OK);
		// it is impossible to be certain we have seen all predicates in the endpoint
		addPredicateListStatus(endpointIndex, endpointURI, false);
		addSubjectRegEx(endpointIndex, endpointURI, subjectRegExBuilder.getRegEx());
		// it is impossible to be certain that the subject/object regexes are complete 
		addSubjectRegExStatus(endpointIndex, endpointURI, false);
		addObjectRegEx(endpointIndex, endpointURI, objectRegExBuilder.getRegEx());
		// it is impossible to be certain that the subject/object regexes are complete 
		addObjectRegExStatus(endpointIndex, endpointURI, false);
		addIndexCreationTime(endpointIndex, endpointURI, new Date());
		updateResultsLimitWithDefault(endpointIndex, endpoint, SPARQLEndpoint.NO_RESULTS_LIMIT);

		return endpointIndex;
	}

	protected void addPredicate(Model endpointIndex, SPARQLEndpoint endpoint, Resource predicate) {

		if(predicateMatchesExcludeList(predicate.getURI())) {
			log.trace("ignoring predicate " + predicate.getURI());
			return;
		}
		log.trace("adding predicate " + predicate.getURI());
		endpointIndex.add(getResource(endpoint.getURI()), getProperty(SPARQLRegistryOntology.HAS_PREDICATE), predicate);
	}
	
	public void addNumTriplesByQuery(Model endpointIndex, SPARQLEndpoint endpoint) throws IOException {

		String endpointURI = endpoint.getURI();
		
		log.trace("querying number of triples in " + endpoint.getURI());
		long numTriples = getNumTriples(endpoint);
		
		log.trace(endpoint.getURI() + " has exactly " + String.valueOf(numTriples) + " triples");
		addNumTriples(endpointIndex, endpointURI, numTriples);
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
	
	public void addPredicateListByQuery(Model endpointIndex, SPARQLEndpoint endpoint) throws IOException
	{
		log.trace("querying predicate list from " + endpoint.getURI());
		Set<String> predicates = endpoint.getPredicates();
		
		log.trace("retrieved full predicate list for " + endpoint.getURI());
		for(String predicate : predicates) {
			addPredicate(endpointIndex, endpoint, getResource(predicate));
		}
	}
	
	private boolean addRegexByQuery(Model endpointIndex, SPARQLEndpoint endpoint, boolean positionIsSubject) throws IOException
	{
		String endpointURI = endpoint.getURI();
		EndpointType type = endpoint.getEndpointType();
		RegExUtils.URIRegExBuilder regExBuilder = new RegExUtils.URIRegExBuilder(REGEX_MAX_LENGTH);

		// Query for the next subject/object URI that doesn't match any of the prefixes we've found so far.  
		// Get the prefix for that URI, add it to the list, and repeat.
		while(!regExBuilder.regExIsTruncated()) {
			
			String query;
			String filter;
			String regex = regExBuilder.getRegEx();
				
			// regular expressions in Virtuoso SPARQL queries require two backslashes for escaping metacharacters
			if(type == EndpointType.VIRTUOSO)
				regex = regex.replaceAll("\\\\", "\\\\\\\\");
			
			if(regex.length() == 0) {
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
			
			Collection<Triple> triples = endpoint.constructQuery(query);

			if(triples.size() == 0)
				break; // done!
			
			String uri;
			if(positionIsSubject)
				uri = triples.iterator().next().getSubject().getURI();
			else
				uri = triples.iterator().next().getObject().getURI();

			// Check for uris that are empty or consists only of whitespace.
			// Believe it or not, this actually happens in some of the Bio2RDF data. -- BV
			if(uri.trim().length() == 0) {
				log.warn("encountered empty uri in data, aborting computation of regex");
				return false;
			}
			
			regExBuilder.addPrefixOfURIToRegEx(uri);
		}

		String regex = regExBuilder.getRegEx();
		
		if(positionIsSubject) {
			addSubjectRegEx(endpointIndex, endpointURI, regex);
		} else {
			addObjectRegEx(endpointIndex, endpointURI, regex);
		}
		
		return !regExBuilder.regExIsTruncated();
	}

	public void addSubjectRegExStatus(Model model, String endpointURI, boolean regexIsComplete) {
		log.trace("adding subject regex status as " + (regexIsComplete ? "complete" : "incomplete"));
		model.add(getResource(endpointURI), getProperty(SPARQLRegistryOntology.SUBJECT_REGEX_IS_COMPLETE), model.createTypedLiteral(String.valueOf(regexIsComplete)));
	}

	public void addObjectRegExStatus(Model model, String endpointURI, boolean regexIsComplete) {
		log.trace("adding object regex status as " + (regexIsComplete ? "complete" : "incomplete"));
		model.add(getResource(endpointURI), getProperty(SPARQLRegistryOntology.OBJECT_REGEX_IS_COMPLETE), model.createTypedLiteral(String.valueOf(regexIsComplete)));
	}
	
	public void addIndexCreationTime(Model model, String endpointURI, Date date) {
		String dateString = DateFormat.getDateTimeInstance().format(date);
		log.trace("adding index creation time " + dateString);
		model.add(getResource(endpointURI), getProperty(SPARQLRegistryOntology.LAST_UPDATED), model.createTypedLiteral(dateString));
	}
	
	public void addSubjectRegEx(Model endpointIndex, String endpointURI, String regex) {
		log.trace("adding subject regex pattern " + regex + " for " + endpointURI);
		endpointIndex.add(getResource(endpointURI), getProperty(SPARQLRegistryOntology.SUBJECT_REGEX), endpointIndex.createTypedLiteral(regex));
	}

	public void addObjectRegEx(Model endpointIndex, String endpointURI, String regex) {
		log.trace("adding object regex pattern " + regex + " for " + endpointURI);
		endpointIndex.add(getResource(endpointURI), getProperty(SPARQLRegistryOntology.OBJECT_REGEX), endpointIndex.createTypedLiteral(regex));
	}

	void addEndpointType(Model endpointIndex, String endpointURI, EndpointType type) {
		log.trace("adding endpoint type of " + type.toString() + " for " + endpointURI);
		endpointIndex.add(getResource(endpointURI), RDF.type, endpointIndex.createTypedLiteral(type.toString()));
	}

	protected void addPredicateListStatus(Model endpointIndex, String endpointURI, boolean indexStatus) {
		log.trace("adding predicate list status as " + (indexStatus ? "complete" : "incomplete"));
		endpointIndex.add(getResource(endpointURI), getProperty(SPARQLRegistryOntology.PREDICATE_LIST_IS_COMPLETE), endpointIndex.createTypedLiteral(String.valueOf(indexStatus)));
	}

	protected void setEndpointStatus(Model endpointIndex, String endpointURI, ServiceStatus status) {
		log.trace("updating status to " + status.toString() + " for " + endpointURI);
		endpointIndex.removeAll(getResource(endpointURI), getProperty(SPARQLRegistryOntology.ENDPOINT_STATUS), (RDFNode)null);
		endpointIndex.add(getResource(endpointURI), getProperty(SPARQLRegistryOntology.ENDPOINT_STATUS), endpointIndex.createTypedLiteral(status.toString()));
	}

	protected void setResultsLimit(Model endpointIndex, String endpointURI, long resultsLimit) {
		log.trace(String.format("setting results limit to %d for %s", resultsLimit, endpointURI));
		endpointIndex.removeAll(getResource(endpointURI), getProperty(SPARQLRegistryOntology.RESULTS_LIMIT), (RDFNode)null);
		endpointIndex.add(getResource(endpointURI), getProperty(SPARQLRegistryOntology.RESULTS_LIMIT), endpointIndex.createTypedLiteral(resultsLimit));
	}

	protected void updateResultsLimitWithDefault(Model endpointIndex, SPARQLEndpoint endpoint, long defaultResultsLimit) 
	{

		try {
			
			setResultsLimit(endpointIndex, endpoint.getURI(), getResultsLimit(endpoint));
		
		} catch(IOException e) {
			
			log.trace(String.format("failed to determine result limit for %s (assuming value NO_RESULTS_LIMIT): ", endpoint.getURI()), e);
			setResultsLimit(endpointIndex, endpoint.getURI(), defaultResultsLimit);
			
		}

	}
	
	public void addNumTriples(Model endpointIndex, String endpointURI, long numTriples) {
		log.trace("adding number of triples = " + String.valueOf(numTriples) + " for " + endpointURI);
		endpointIndex.add(getResource(endpointURI), getProperty(SPARQLRegistryOntology.NUM_TRIPLES), endpointIndex.createTypedLiteral(numTriples));
	}

	public void addNumTriplesLowerBound(Model endpointIndex, String endpointURI, long numTriplesLowerBound) throws IOException	{
		log.trace("adding number of triples (lower bound) = " + String.valueOf(numTriplesLowerBound) + " for " + endpointURI);
		endpointIndex.add(getResource(endpointURI), getProperty(SPARQLRegistryOntology.NUM_TRIPLES), endpointIndex.createTypedLiteral(numTriplesLowerBound));
	}
	
	
	public void updateStatusOfAllEndpoints() throws IOException 
	{
		/* this operation needs to be fixed due to a problem with the Virtuoso Jena adapter.
		 * See getAllEndpoints() */
		
		throw new UnsupportedOperationException();
		
		/*
		Collection<SPARQLEndpoint> endpoints = getAllEndpoints();
		for (SPARQLEndpoint endpoint : endpoints) {
			updateEndpointStatus(endpoint);
		}
		*/

	}
	
	public void updateEndpointStatus(SPARQLEndpoint endpoint) 
	{
		ServiceStatus newStatus = endpoint.ping() ? ServiceStatus.OK : ServiceStatus.DEAD;
		setEndpointStatus(getIndexModel(), endpoint.getURI(), newStatus); 
	}
	
	public void setEndpointStatus(String endpointURI, ServiceStatus status) 
	{
		setEndpointStatus(getIndexModel(), endpointURI, status);
	}

	protected Set<SPARQLEndpoint> getAllEndpoints() 
	{
		/**
		 * TODO: This method should use the Virtuoso-backed Jena
		 * model, but this is not possible due to some problem with 
		 * the Virtuoso jar. (getIndexModel().listSubjects() fails with a
		 * java.lang.NoSuchField exception.) 
		 */

		throw new UnsupportedOperationException();
		
		/*
		Set<SPARQLEndpoint> endpoints = new HashSet<SPARQLEndpoint>();
		for(ResIterator i = getIndexModel().listSubjects(); i.hasNext(); ) {
			Resource endpoint = (Resource)i.next();
			Statement typeTriple = endpoint.getProperty(RDF.type);
			if(typeTriple == null) {
 				endpoints.add(SPARQLEndpointFactory.createEndpoint(endpoint.getURI()));
			} else {
				if(!typeTriple.getObject().isLiteral()) {
					throw new RuntimeException(String.format("registry corrupt!, endpoint type of %s is not a literal", endpoint.toString()));
				}
				Literal typeAsLiteral = (Literal)typeTriple.getObject().as(Literal.class);
				EndpointType type = EndpointType.valueOf(typeAsLiteral.getString());
				endpoints.add(SPARQLEndpointFactory.createEndpoint(endpoint.getURI(), type));
			}
		}

		return endpoints;
		*/
		
	}
	
	public void removeEndpoint(String uri) {
		log.trace("removing endpoint " + uri + " from registry");
		getIndexModel().removeAll(getResource(uri), (Property)null, (RDFNode)null);
	}

	public void setEndpointResultsLimit(String endpointURI, long resultsLimit) 
	{
		setResultsLimit(getIndexModel(), endpointURI, resultsLimit);
	}
	
	public void updateEndpointResultsLimit(String endpointURI, EndpointType type) throws IOException
	{
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURI, type);
		setResultsLimit(getIndexModel(), endpoint.getURI(), getResultsLimit(endpoint));
	}
	
	public long getResultsLimit(SPARQLEndpoint endpoint) throws IOException
	{

		log.trace(String.format("determining results limit for %s", endpoint.getURI()));

		if(!endpoint.ping()) {
			throw new IOException(String.format("%s not responding, unable to update results limit", endpoint.getURI()));
		}

		String query = "SELECT * WHERE { ?s ?p ?o }";

		log.trace(String.format("issuing probe query %s", query));
		List<Map<String, String>> results = endpoint.selectQuery(query);

		log.trace(String.format("%s has a result limit of %d", endpoint.getURI(), results.size()));
		return results.size();

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
			ADD_ROOT_URI_FOR_TRAVERSAL,
			UPDATE_STATUS_OF_ALL_ENDPOINTS,
			UPDATE_ENDPOINT_RESULTS_LIMIT,
			SET_ENDPOINT_RESULTS_LIMIT,
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

		@Option(name = "-H", usage = "virtuoso hostname")
		public String virtuosoHost = virtuosoConfig.getString(VIRTUOSO_HOSTNAME_CONFIG_KEY, "localhost");

		@Option(name = "-P", usage = "virtuoso port")
		public int virtuosoPort = virtuosoConfig.getInt(VIRTUOSO_PORT_CONFIG_KEY, 1111);
		
		@Option(name = "-u", usage = "virtuoso username")
		public String virtuosoUsername = virtuosoConfig.getString(VIRTUOSO_USERNAME_CONFIG_KEY);
		
		@Option(name = "-p", usage = "virtuoso password")
		public String virtuosoPassword = virtuosoConfig.getString(VIRTUOSO_PASSWORD_CONFIG_KEY);
		
		@Option(name = "-g", usage = "URI of index graph")
		public String indexGraph = Config.getConfiguration().subset(CONFIG_ROOT).getString(INDEX_GRAPH_CONFIG_KEY);
		
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

		@Option(name = "-S", usage = "manually set endpoint status (choices: '<endpoint>,DEAD', '<endpoint>,SLOW', '<endpoint>,OK')")
		public void setEndpointStatus(String endpointAndStatus) { operations.add(new Operation(endpointAndStatus, OperationType.SET_ENDPOINT_STATUS)); }

		@Option(name = "-U", usage = "update status of all endpoints") 
		public void updateStatusOfAllEndpoints(boolean unused) { operations.add(new Operation(null, OperationType.UPDATE_STATUS_OF_ALL_ENDPOINTS)); }

		@Option(name = "-R", usage = "specify a root URI for indexing-by-traversal")
		public void addRootURIForTraversal(String rootURI) { operations.add(new Operation(rootURI, OperationType.ADD_ROOT_URI_FOR_TRAVERSAL)); }
		
		@Option(name = "-r", usage = "update the results limit for the given endpoint")
		public void updateResultsLimit(String endpointURI) { operations.add(new Operation(endpointURI, OperationType.UPDATE_ENDPOINT_RESULTS_LIMIT)); }
		
		@Option(name = "-b", usage = "manually set the results limit for an given endpoint (arg format: '<endpointURL>,<resultsLimit>')")
		public void setResultsLimit(String endpointURI) { operations.add(new Operation(endpointURI, OperationType.SET_ENDPOINT_RESULTS_LIMIT)); }
	}

	public static void main(String[] args) throws IOException 
	{
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);
		
		// currently, there are two possible values: 0 (success) and 1 (some error occurred)
		int exitCode = 0;
		
		try {
			cmdLineParser.parseArgument(args);
			
			SPARQLRegistryAdmin registry = new VirtuosoSPARQLRegistryAdmin(
					options.virtuosoHost,
					options.virtuosoPort,
					options.indexGraph,
					options.virtuosoUsername,
					options.virtuosoPassword );
			
			log.trace("using registry at " + options.virtuosoHost + ":" + String.valueOf(options.virtuosoPort));

			/* The switches "-t" (endpoint type) and "-l" (query results limit) apply to all endpoints, unless there
			 * is more than one occurrence on the command line.  In the latter case, each instance of 
			 * "-t"/"-l" applies to the endpoints *following* it, up until the next occurence of "-t"/"-l".
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
			
			/*
			 * -R <rootURI> switches specify a root URI for indexing-by-breadth-first-traversal.
			 * This is an ad-hoc method of indexing for cases when the endpoint is too large
			 * to be indexed fully (due to query timeouts).  -R switches apply to the indexing
			 * (-i) switch that immediately precedes them, and one may specify multiple -R switches
			 * for the same indexing operation. (Typically, one would specify one -R switch for
			 * each type of record in the endpoint.)
			 */
			
			Map<Integer,List<String>> rootURIMap = new HashMap<Integer,List<String>>();
			Integer lastIndexOp = -1;
			Integer optionIndex = 0;
			for(CommandLineOptions.Operation op : options.operations) {
				if(op.opType == CommandLineOptions.OperationType.INDEX) {
					lastIndexOp = optionIndex; 
				}
				if(op.opType == CommandLineOptions.OperationType.ADD_ROOT_URI_FOR_TRAVERSAL) {
					if(lastIndexOp == -1) {
						throw new CmdLineException("-R option must follow a -i option");
					}
					List<String> rootURIs = null;
					if(rootURIMap.containsKey(lastIndexOp)) {
						rootURIs = rootURIMap.get(lastIndexOp);
					} else {
						rootURIs = new ArrayList<String>();
						rootURIMap.put(lastIndexOp, rootURIs);
					}
					rootURIs.add(op.arg);
				}
				optionIndex++;
			}

			/* Perform each operation, in the order specified on the command line */
			
			optionIndex = 0;
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
						if(rootURIMap.containsKey(optionIndex)) {
							registry.indexEndpointByTraversal(op.arg, endpointType, rootURIMap.get(optionIndex));
						} else {
							registry.indexEndpoint(op.arg, endpointType, resultsLimit);
						}
						break;
					case REMOVE:
						registry.removeEndpoint(op.arg);
						break;
					case SET_ENDPOINT_STATUS:
						String statusArg[] = op.arg.split(",");
						if(statusArg.length != 2) {
							throw new CmdLineException("format of arg to -S must be '<endpointURL>,<status>'");
						}
						ServiceStatus newStatus = ServiceStatus.valueOf(StringUtils.upperCase(statusArg[1]));
						registry.setEndpointStatus(statusArg[0], newStatus);
						break;
					/* this operation needs to be fixed (due to a problem with the Virtuoso Jena adapter) */
					/*
					case UPDATE_STATUS_OF_ALL_ENDPOINTS:
						registry.updateStatusOfAllEndpoints();
						break;
					 */
					case CLEAR_REGISTRY:
						registry.clearRegistry();
						break;
					case UPDATE_ENDPOINT_RESULTS_LIMIT:
						registry.updateEndpointResultsLimit(op.arg, endpointType);
						break;
					case SET_ENDPOINT_RESULTS_LIMIT:
						String arg[] = op.arg.split(",");
						if(arg.length != 2) {
							throw new CmdLineException("format of arg to -r must be '<endpointURL>,<results limit>'");
						}
						registry.setEndpointResultsLimit(arg[0], Long.valueOf(arg[1]));
						break;
					default:
						break;
					}
					optionIndex++;
					
				} catch (Exception e) {
					log.error("operation " + op.opType + " failed on " + op.arg, e);
					exitCode = 1;
				}
			}
			
		} catch (CmdLineException e) {
			
			log.error(e);
			log.error("Usage: sparqlreg [-t endpointType] [-r registryURI] [-i endpointURI] [-a endpointURI] [-d endpointURI] [-c] [-R]");
			cmdLineParser.printUsage(System.err);
			try {
				log.error(getUsageNotes());
			} catch(IOException e2) {
				log.error("could not read usage notes file: " + e2.getMessage());
			}
			System.exit(1);
		}
		
		System.exit(exitCode);
	}
	
	protected static String getUsageNotes() throws IOException {
		Reader reader = new BufferedReader(new FileReader(VirtuosoSPARQLRegistryAdmin.class.getResource("resource/usage.notes.txt").toString()));
		return IOUtils.toString(reader);
	}
}
