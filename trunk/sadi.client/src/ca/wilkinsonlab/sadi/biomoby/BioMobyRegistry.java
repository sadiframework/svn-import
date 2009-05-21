package ca.wilkinsonlab.sadi.biomoby;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomoby.shared.MobyData;
import org.biomoby.shared.MobyDataType;
import org.biomoby.shared.MobyNamespace;
import org.biomoby.shared.MobyPrimaryData;
import org.biomoby.shared.MobyPrimaryDataSet;
import org.biomoby.shared.MobyPrimaryDataSimple;
import org.biomoby.shared.MobySecondaryData;

import ca.wilkinsonlab.sadi.client.Registry;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInputPair;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.StringUtil;
import ca.wilkinsonlab.sadi.virtuoso.VirtuosoRegistry;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class BioMobyRegistry extends VirtuosoRegistry implements Registry
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(BioMobyRegistry.class);
	
	private static final String INPUT_ARGUMENT_URI = "http://www.mygrid.org.uk/mygrid-moby-service#inputParameter";
	private static final String OUTPUT_ARGUMENT_URI = "http://www.mygrid.org.uk/mygrid-moby-service#outputParameter";
	private static final String DEFAULT_SPARQL_ENDPOINT = "http://biordf.net/sparql";
	private static final String ENDPOINT_CONFIG_KEY = "endpoint";
	private static final String GRAPH_CONFIG_KEY = "graph";
	private static final boolean CACHE_ENABLED = true;

	private Map<String, BioMobyService> serviceCache;
	private OntModel predicateOntology;
	
	public BioMobyRegistry() throws IOException
	{
		this(DEFAULT_SPARQL_ENDPOINT);
	}
	
	public BioMobyRegistry(Configuration config) throws IOException
	{
		this(config.getString(ENDPOINT_CONFIG_KEY));
		
		graphName = config.getString(GRAPH_CONFIG_KEY);
	}
	
	public BioMobyRegistry(String sparqlEndpoint) throws IOException
	{
		this(new URL(sparqlEndpoint));
	}
	
	public BioMobyRegistry(URL sparqlEndpoint) throws IOException
	{
		super(sparqlEndpoint);
		
		/* TODO replace this with some more sophisticated caching mechanism;
		 * I assume EHCache.
		 */
		serviceCache = new HashMap<String, BioMobyService>();
		
		/* TODO we'll probably have to manage this more carefully as the
		 * number of ontologies we're importing predicates from grows.
		 */
		predicateOntology = ModelFactory.createOntologyModel(); 
		
		refreshPredicates();
	}
	
	public OntModel getPredicateOntology()
	{
		return predicateOntology;
	}
	
	boolean isDatatypeProperty(String predicate)
	{
		OntProperty p = getPredicateOntology().getOntProperty(predicate);
		return p.isDatatypeProperty();
	}
	
	/**
	 * Returns the set of predicates used to annotate services in the registry.
	 * @return the set of predicates used to annotate services in the registry
	 * @throws IOException
	 */
	private Set<String> getReferencedPredicates() throws IOException
	{
		String query = StringUtil.readFully(BioMobyRegistry.class.getResource("resources/select.all.predicates.sparql"));

		Set<String> predicates = new HashSet<String>();
		for (Map<String, String> binding: executeQuery(query))
			predicates.add(binding.get("pred"));
		
		return predicates;
	}
	
	private void refreshPredicates() throws IOException
	{
		/* TODO clear predicateOntology...
		 */
		OwlUtils.loadOWLFilesForPredicates(predicateOntology, getReferencedPredicates());
	}

	/**
	 * Returns a Service object corresponding to the specified service URI.
	 * @param serviceURI a URI identifying the desired service
	 * @return a Service object corresponding to the specified service URI
	 * @throws IOException if there is a problem communicating with the registry
	 */
	public Service getService(String serviceURI)
	throws IOException
	{
		BioMobyService service = serviceCache.get(serviceURI);
		if (service != null)
			return service;
			
		service = new BioMobyService();
		service.setSourceRegistry(this);
		// TODO store service URI somewhere in the object ffs
		fillBasicInfo(service, serviceURI);
		fillArgumentInfo(service, serviceURI, INPUT_ARGUMENT_URI);
		fillArgumentInfo(service, serviceURI, OUTPUT_ARGUMENT_URI);
		fillPredicateInfo(service, serviceURI);
		
		if (CACHE_ENABLED)
			serviceCache.put(serviceURI, service);
		
		return service;
	}
	
	/**
	 * Return a collection of services that can attach the specified predicate
	 * to the specified subject.
	 * @param subject the subject URI
	 * @param predicate the predicate URI
	 * @return the collection of matching services
	 * @throws IOException 
	 */
	public Collection<Service> findServices(String subject, String predicate) throws IOException
	{
		/* TODO actually restrict this by the input namespace...
		 */
		return findServicesByPredicate(predicate);
	}
	
	/**
	 * Given a list of equivalent OWL predicate URIs, return a list
	 * services that are annotated with those predicates.  Each service 
	 * returned	has been annotated with at least one predicate in the 
	 * list of given synonyms.
	 * 
	 * NOTE: A predicate annotation connects one input argument to 
	 * one output argument, so if a service has multiple inputs 
	 * or outputs, it may be capable of generating multiple 
	 * predicates.  To find out which input and output correspond
	 * to a particular predicate, the caller must use 
	 * Moby2Service.getInputForPredicate(predicateURI) and 
	 * Moby2Service.getOutputForPredicate(predicateURI).
	 * 
	 * @return a list of service matching ths list of predicates
	 * @throws URIException if any of the predicate URIs are invalid
	 * @throws IOException if there is a problem communicating with the registry
	 */
	Collection<Service> findServicesByPredicate(List<String> predicates)
	throws IOException
	{
		List<Service> matches = new ArrayList<Service>();
		
		/* TODO I don't like that the text in this string has to match text
		 * contained in another file; we should aim for a situation where
		 * all of the SPARQL is in one place, either all in code or all in
		 * resource files.
		 */
		String query = StringUtil.strFromTemplate(
				BioMobyRegistry.class.getResource("resources/select.bypred.sparql"),
				getSynonymSubquery("?inputarg %u% ?outputarg", predicates)
		);
		for (Map<String, String> binding: executeQuery(query))
			matches.add(getService(binding.get("service")));
		
		return matches;
	}

	/**
	 * Returns a collection of services mapped to the specified predicate.
	 * @param predicate the unescaped URI of the predicate
	 * @return a collection of matching services
	 * @throws URIException if the predicate URI is invalid
	 * @throws IOException if there is a problem communicating with the registry
	 */
	public Collection<Service> findServicesByPredicate(String predicate)
	throws IOException
	{
		return findServicesByPredicate( Collections.singletonList(predicate));
	}
	
	/**
	 * Returns a collection of predicates that are mapped to services that
	 * can take the specified subject as input.
	 * @param subject the subject URI
	 * @return a collection of matching predicates
	 * @throws IOException if there is a problem communicating with the registry
	 */
	public Collection<String> findPredicatesBySubject(String subject)
	throws IOException
	{
		try {
			String namespace = BioMobyHelper.convertUriToMobyDataObject(subject).getNamespaces()[0].getName();
			return findPredicatesByInputNamespace(namespace);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
		
	}
	
	Collection<String> findPredicatesByInputNamespace(String namespace)
	throws IOException
	{
		String query = StringUtil.strFromTemplate(
				BioMobyRegistry.class.getResource("resources/select.predbyns.sparql"),
				namespace
		);
		List<Map<String, String>> bindings = executeQuery(query);
		Collection<String> results = new ArrayList<String>(bindings.size());
		for (Map<String, String> binding: bindings)
			results.add(binding.get("predicate"));
		return results;
	}
	
	/**
	 * Retrieve a SPARQL construct query for a given list of predicate synonyms
	 * and Moby output object type. The construct query is used to build desired
	 * RDF triples from an RDF representation of a Moby output object.
	 * 
	 * For example, suppose we make a service call to retrieve the domains for a
	 * UniProt protein, and receive a MotifAnnotatedAASequence as output. The
	 * accession IDs of the domains are "buried" in this object, and must be
	 * extracted somehow, in order to build the triple: <protein> hasDomain
	 * <domainID>. We first convert the object to RDF; then we build the desired
	 * triples with a SPARQL construct query on this RDF.
	 * 
	 * @param predicateURI the service predicate we are constructing RDF for.
	 * @param outputObjType the Moby datatype that is the object of the predicate.
	 * @return a SPARQL construct query, if one is available, null otherwise.
	 * @throws IOException 
	 */
	String getConstructQueryForPredicate(List<String> predicates, String outputDatatypeURI)
	throws IOException
	{
		/* TODO cache these; they are probably slowing us down...
		 */
		
		/* TODO I don't like that the text in this string has to match text
		 * contained in another file; we should aim for a situation where
		 * all of the SPARQL is in one place, either all in code or all in
		 * resource files.
		 */
		String query = StringUtil.strFromTemplate(
				BioMobyRegistry.class.getResource("resources/select.constructquery.for.pred.sparql"),
				getSynonymSubquery("%u% moby:hasOutputType ?outputType", predicates),
				outputDatatypeURI
		);
		List<Map<String, String>> bindings = executeQuery(query);
		if (bindings.size() != 1) {
			log.warn(String.format("%s construct queries for %s/%s", bindings.size(), outputDatatypeURI, predicates));
			return null;
		}
		
		Map<String, String> binding = bindings.get(0);
		return binding.get("construct_query");
	}
	
	/**
	 * Returns the SPARQL construct query for the given predicate and BioMoby output
	 * datatype.
	 * @param predicateURI the unescaped URI of the predicate
	 * @return the SPARQL construct query
	 * @throws IOException 
	 */
	String getConstructQueryForPredicate(String predicateURI, String outputDatatypeURI)
	throws IOException
	{
		return getConstructQueryForPredicate( Collections.singletonList(predicateURI), outputDatatypeURI);
	}

	private void fillBasicInfo(BioMobyService service, String serviceURI)
	throws IOException
	{
		String query = StringUtil.strFromTemplate(
				BioMobyRegistry.class.getResource("resources/select.servicebasic.sparql"),
				serviceURI,
				serviceURI,
				serviceURI,
				serviceURI,
				serviceURI,
				serviceURI,
				serviceURI,
				serviceURI
		);
		List<Map<String, String>> bindings = executeQuery(query);
		if (bindings.size() != 1)
			throw new RuntimeException(String.format("URI %s mapped to %s services", serviceURI, bindings.size()));
		// TODO create a RegistryException type?
		
		Map<String, String> binding = bindings.get(0);
		service.setAuthoritative(StringUtils.equals(binding.get("authoritative"), "true"));
		service.setAuthority(binding.get("authority"));
		service.setName(binding.get("name"));
		service.setDescription(binding.get("desc"));
		service.setEmailContact(binding.get("email"));
		service.setLSID(binding.get("lsid"));
		service.setSignatureURL(binding.get("rdfurl"));
		service.setURL(binding.get("url"));
		service.setCategory(binding.get("format"));
		service.setType(StringUtils.substringAfter(binding.get("servicetype"), "#"));
	}

	private void fillArgumentInfo(BioMobyService service, String serviceURI, String inOutURI)
	throws IOException
	{
		boolean input = inOutURI == INPUT_ARGUMENT_URI;
		String query = StringUtil.strFromTemplate(
				BioMobyRegistry.class.getResource("resources/select.args.sparql"),
				serviceURI,
				inOutURI
		);
		for (Map<String, String> binding: executeQuery(query)) {
			String articleName = binding.get("articlename");
			
			// argument is simple, parameter or collection
			String argType = StringUtils.substringAfter(binding.get("paramtype"), "#");
			boolean isSecondary = argType.equals("secondaryParameter");
			boolean isCollection = argType.equals("collectionParameter");

			MobyData dataObj;
			if (isSecondary) { // current argument is parameter
				String datatype = binding.get("datatype");
				if (datatype == null)
					throw new RuntimeException(String.format("datatype for %s : %s is null", serviceURI, articleName));

				MobySecondaryData secondaryArg = new MobySecondaryData(articleName);
				try {
					secondaryArg.setDataType(datatype);
				} catch (Exception e) {
					// TODO look at jMoby and figure out what this exception could be...
					throw new RuntimeException(e);
				}
				String defaultValue = binding.get("default");
				if (defaultValue != null)
					secondaryArg.setDefaultValue(defaultValue);
				
				dataObj = secondaryArg;
			} else { // current argument is simple or collection
				String objType = StringUtils.substringAfter(binding.get("objtype"), "#");
				if (objType == null)
					throw new RuntimeException(String.format("objtype for %s : %s is null", serviceURI, articleName));

				MobyPrimaryData primaryArg; 
				if (isCollection) {
					MobyPrimaryDataSet collectionArg = new MobyPrimaryDataSet(articleName);
					
					/* HACK: For some reason, the MobyPrimaryDataSet stores the namespace
					 * information in its child MobyPrimaryDataSimple objects, rather
					 * than in the MobyPrimaryDataSet object itself.  (When you ask for
					 * the allowable namespaces with getNamespaces(), it returns those of
					 * the first child.)
					 * For this reason, the collection needs to contain at least one 
					 * MobyPrimaryDataSimple object, so we insert a dummy one here.
					 */
					MobyPrimaryDataSimple dummy = new MobyPrimaryDataSimple();
					collectionArg.addElement(dummy);
					
					primaryArg = collectionArg;
				} else {
					primaryArg = new MobyPrimaryDataSimple(articleName);
				}
				
				primaryArg.setDataType(new MobyDataType(objType));
				fillNamespaceInfo(primaryArg, serviceURI, inOutURI, articleName);
				
				dataObj = primaryArg;
			}

			if (input)
				service.addInput(articleName, dataObj);
			else
				service.addOutput(articleName, dataObj);
		}
	}
	
	/**
	 * Retrieve all the legal namespaces for this argument.
	 * If it is an output argument, there can only be one; if
	 * it is an input, there can be many.
	 * @param primaryArg
	 * @param serviceURI
	 * @param inOutURI
	 * @param articleName
	 * @throws IOException 
	 */
	private void fillNamespaceInfo(MobyPrimaryData primaryArg, String serviceURI, String inOutURI, String articleName)
	throws IOException
	{
		String query = StringUtil.strFromTemplate(
				BioMobyRegistry.class.getResource("resources/select.namespaces.sparql"),
				serviceURI,
				inOutURI,
				articleName
		);
		for (Map<String, String> binding: executeQuery(query)) {
			primaryArg.addNamespace(new MobyNamespace(StringUtils.substringAfter(binding.get("namespace"), "#")));
		}
	}

	private void fillPredicateInfo(BioMobyService service, String serviceURI)
	throws IOException
	{
		String query = StringUtil.strFromTemplate(
				BioMobyRegistry.class.getResource("resources/select.predicates.sparql"),
				serviceURI
		);
		for (Map<String, String> binding: executeQuery(query)) {
			String predicate = binding.get("predicate");
			String inputName = binding.get("inputname");
			String outputName = binding.get("outputname");
			
			service.addPredicate(predicate, inputName, outputName);
			

			/*
			// Add all of the registered synonyms for the predicate as well.   
			// (Synonyms are asserted in the registry by owl:sameAs triples.)
			for (String synonym : getPredicateSynonyms(predicate))
				service.addPredicate(synonym, inputName, outputName);
			*/
		}
	}

	/**
	 * Creates the predicate-matching triples of the WHERE clause, when querying for 
	 * services by predicate.   This set of triples must be created programmatically
	 * because a predicate URI may have any number of synonymous URIs, and 
	 * each must be tested for separately.
	 * 
	 * NOTE: Virtuoso has a switch to turn on support for "owl:sameAs", but
	 * it is only implemented for the subjects and objects of triples.   On top of that,
	 * the manual warns of a significant performance hit and potentially incomplete results
	 * (depending on the join order used for the query).  For these reasons,
	 * I've decided to implement the behaviour for "owl:sameAs" manually.
	 * 
	 * @return the SPARQL subquery accounting for all the synonyms
	 * @throws URISyntaxException 
	 */
	private static String getSynonymSubquery(String tripleTemplate, List<String> predSynonyms)
	throws URIException
	{
		StringBuilder buf = new StringBuilder();
		for(String uri: predSynonyms) {	
			if (buf.length() > 0)
				buf.append("\n\tUNION ");
			else
				buf.append("\n\t");
			
			buf.append("{");
			buf.append(StringUtil.strFromTemplate(tripleTemplate, uri));
			buf.append("}");
		}
		return buf.toString();
	}

	public Collection<String> findPredicatesBySubject(Resource subject) throws IOException
	{
		return findPredicatesBySubject(subject.getURI());
	}

	public Collection<? extends Service> findServices(Resource subject, String predicate) throws IOException
	{
		return findServices(subject.getURI(), predicate);
	}

	public Collection<ServiceInputPair> discoverServices(Model model) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public Collection<? extends Service> findServices(Resource subject) throws IOException
	{
		throw new UnsupportedOperationException();
	}
}
