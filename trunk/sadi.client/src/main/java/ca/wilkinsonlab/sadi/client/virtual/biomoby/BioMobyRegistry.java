package ca.wilkinsonlab.sadi.client.virtual.biomoby;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.biomoby.client.CentralImpl;
import org.biomoby.shared.Central;
import org.biomoby.shared.MobyData;
import org.biomoby.shared.MobyDataType;
import org.biomoby.shared.MobyException;
import org.biomoby.shared.MobyNamespace;
import org.biomoby.shared.MobyPrimaryData;
import org.biomoby.shared.MobyPrimaryDataSet;
import org.biomoby.shared.MobyPrimaryDataSimple;
import org.biomoby.shared.MobySecondaryData;
import org.biomoby.shared.data.MobyDataObject;

import ca.wilkinsonlab.sadi.client.RegistryBase;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInputPair;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class BioMobyRegistry extends RegistryBase
{
	private static final Logger log = Logger.getLogger(BioMobyRegistry.class);
	
	private static final String LSRN_PREFIX = "http://purl.oclc.org/SADI/LSRN/";

	private static final String INPUT_ARGUMENT_URI = "http://www.mygrid.org.uk/mygrid-moby-service#inputParameter";
	private static final String OUTPUT_ARGUMENT_URI = "http://www.mygrid.org.uk/mygrid-moby-service#outputParameter";
	private static final String INPUT_URI_PATTERN_KEY = "inputUriPattern";
	private static final String OUTPUT_URI_PATTERN_KEY = "outputUriPattern";
	
	private static final String NAMESPACE_PROPERTIES = "namespace.properties";

	private OntModel predicateOntology;
	private OntModel typeOntology;
	private Collection<Pattern> inputUriPatterns;
	private Map<String, String> namespaceMap;
	private String outputUriPattern;
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.RegistryBase#(java.lang.String)
	 */
	public BioMobyRegistry(Configuration config) throws IOException
	{
		super(config);
		
		inputUriPatterns = new ArrayList<Pattern>();
		for (Object o: config.getList(INPUT_URI_PATTERN_KEY)) {
			String pattern = (String)o;
			try {
				inputUriPatterns.add( Pattern.compile( pattern ) );
			} catch (Exception e) {
				log.error( String.format("error processing URL pattern: %s", pattern), e );
			}
		}
		outputUriPattern = config.getString(OUTPUT_URI_PATTERN_KEY);
		
		namespaceMap = new HashMap<String, String>();
		try {
			Properties namespaces = new Properties();
			namespaces.load( getClass().getResourceAsStream(NAMESPACE_PROPERTIES) );
			for (String fromNamespace: namespaces.stringPropertyNames()) {
				String toNamespace = namespaces.getProperty(fromNamespace);
				if (!StringUtils.isEmpty(toNamespace))
					namespaceMap.put(fromNamespace, toNamespace);
			}
		} catch (Exception e) {
			log.error("failed to load namespace map", e);
		}
		
		predicateOntology = createPredicateOntology();
		typeOntology = createTypeOntology();
	}
	
	public BioMobyRegistry() throws IOException
	{
		this(createDefaultConfig());
	}
	
	private static Configuration createDefaultConfig()
	{
		Configuration config = new BaseConfiguration();
		config.addProperty(SPARQL_ENDPOINT_KEY, "http://biordf.net/sparql");
		config.addProperty(INPUT_URI_PATTERN_KEY, "http://lsrn.org/(.+?):([\\S]+)");
		config.addProperty(OUTPUT_URI_PATTERN_KEY, "http://lsrn.org/$NS:$ID");
		return config;
	}
	
	/* TODO figure out why this method takes so long initially and maybe
	 * find a better way to do it; this is the first point that the following
	 * appears in the log:
	 *     Fetching data type ontology from http://biomoby.org/RESOURCES/MOBY-S/Objects
     *     ... done
	 */
	public MobyDataObject convertUriToMobyDataObject(String uri) throws URISyntaxException
	{
		log.debug( String.format("converting %s to MobyDataObject", uri) );
		for (Pattern pattern: inputUriPatterns) {
			log.trace( String.format("testing %s =~ / %s /", uri, pattern) );
			
			/* use find() and not matches() to better emulate perl semantics by
			 * not forcing a match to start at the beginning of the string...
			 */
			Matcher match = pattern.matcher(uri);
			if (match.find()) {
				try {
					String namespace = match.group(1);
					if (namespaceMap.containsKey(namespace))
						namespace = namespaceMap.get(namespace);
					String id = match.group(2);
					log.trace( String.format("matched on %s / %s", namespace, id) );
					return new MobyDataObject(namespace, id);
				} catch (Exception e) {
					log.error( String.format("error processing %s =~ / %s /", uri, pattern), e );
				}
			}
		}
		throw new URISyntaxException(uri, "unable to determine namespace/id");
	}
	
	public String convertMobyDataObjectToUri(MobyDataObject obj)
	{
		// TODO deal with the issue of multiple namespaces
		String uri = outputUriPattern;
		uri = StringUtils.replace(uri, "$NS", obj.getNamespaces()[0].getName());
		uri = StringUtils.replace(uri, "$ID", obj.getId());
		
		return uri;
	}

	private OntModel createPredicateOntology()
	{
		/* TODO remove this once we're passing around actual Jena properties...
		 */
		return ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
	}
	
	private OntModel createTypeOntology()
	{
		OntModel typeOntology = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		typeOntology.read(LSRN_PREFIX);
		return typeOntology;
	}
	
	public OntModel getPredicateOntology()
	{
		return predicateOntology;
	}
	
	public OntModel getTypeOntology()
	{
		return typeOntology;
	}
	
	Central central;
	public synchronized Central getMobyCentral()
	{
		if (central == null) {
			try {
				central = new CentralImpl();
			} catch (MobyException e) {
				log.error("error instantiating Moby Central", e);
			}
		}
		return central;
	}
	
	public OntClass getTypeByNamespace(MobyNamespace ns)
	{
		String uri = String.format("%s%s_Record", LSRN_PREFIX, ns.getName());
		OntClass type = getTypeOntology().getOntClass(uri);
		if (type == null) {
			log.trace(String.format("creating class %s", uri));
			type = getTypeOntology().createClass(uri);
		}
		return type;
	}
	
	boolean isDatatypeProperty(String predicate)
	{
		try {
			OntProperty p = OwlUtils.getOntPropertyWithLoad(getPredicateOntology(), predicate);
			return p.isDatatypeProperty();
		} catch (SADIException e) {
			log.error(e.getMessage());
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.RegistryBase#createService(java.lang.String)
	 */
	public Service createService(String serviceURI) throws SADIException
	{
		BioMobyService service = new BioMobyService();
		service.setSourceRegistry(this);
		// TODO store service URI somewhere in the object ffs
		fillBasicInfo(service, serviceURI);
		fillArgumentInfo(service, serviceURI, INPUT_ARGUMENT_URI);
		fillArgumentInfo(service, serviceURI, OUTPUT_ARGUMENT_URI);
		fillPredicateInfo(service, serviceURI);
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
	public Collection<Service> findServices(String subject, String predicate) throws SADIException
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
	Collection<Service> findServicesByPredicate(List<String> predicates) throws SADIException
	{
		List<Service> matches = new ArrayList<Service>();
		
		/* TODO I don't like that the text in this string has to match text
		 * contained in another file; we should aim for a situation where
		 * all of the SPARQL is in one place, either all in code or all in
		 * resource files.
		 */
		String query = buildQuery("select.bypred.sparql", getSynonymSubquery("?inputarg %u% ?outputarg", predicates));
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
	public Collection<Service> findServicesByPredicate(String predicate) throws SADIException
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
	public Collection<String> findPredicatesBySubject(String subject) throws SADIException
	{
		try {
			String namespace = convertUriToMobyDataObject(subject).getNamespaces()[0].getName();
			return findPredicatesByInputNamespace(namespace);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
		
	}
	
	Collection<String> findPredicatesByInputNamespace(String namespace) throws SADIException
	{
		String query = buildQuery("select.predbyns.sparql", namespace);
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
	String getConstructQueryForPredicate(List<String> predicates, String outputDatatypeURI) throws SADIException
	{
		/* TODO cache these; they are probably slowing us down...
		 */
		
		/* TODO I don't like that the text in this string has to match text
		 * contained in another file; we should aim for a situation where
		 * all of the SPARQL is in one place, either all in code or all in
		 * resource files.
		 */
		String query = buildQuery("select.constructquery.for.pred.sparql",
				getSynonymSubquery("%u% moby:hasOutputType ?outputType", predicates), outputDatatypeURI);
		List<Map<String, String>> bindings = executeQuery(query);
		if (bindings.size() != 1) {
			log.debug(String.format("%s construct queries for %s/%s", bindings.size(), outputDatatypeURI, predicates));
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
	String getConstructQueryForPredicate(String predicateURI, String outputDatatypeURI) throws SADIException
	{
		return getConstructQueryForPredicate( Collections.singletonList(predicateURI), outputDatatypeURI);
	}

	private void fillBasicInfo(BioMobyService service, String serviceURI) throws SADIException
	{
		String query = buildQuery("select.servicebasic.sparql",
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

	private void fillArgumentInfo(BioMobyService service, String serviceURI, String inOutURI) throws SADIException
	{
		boolean input = inOutURI == INPUT_ARGUMENT_URI;
		String query = buildQuery("select.args.sparql", serviceURI, inOutURI);
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
	private void fillNamespaceInfo(MobyPrimaryData primaryArg, String serviceURI, String inOutURI, String articleName) throws SADIException
	{
		String query = buildQuery("select.namespaces.sparql",
				serviceURI,
				inOutURI,
				articleName
		);
		for (Map<String, String> binding: executeQuery(query)) {
			primaryArg.addNamespace(new MobyNamespace(StringUtils.substringAfter(binding.get("namespace"), "#")));
		}
	}

	private void fillPredicateInfo(BioMobyService service, String serviceURI)
	throws SADIException
	{
		String query = buildQuery("select.predicates.sparql", serviceURI);
		for (Map<String, String> binding: executeQuery(query)) {
			String predicate = binding.get("predicate");
			String inputName = binding.get("inputname");
			String outputName = binding.get("outputname");
			
			service.addPredicate(predicate, inputName, outputName);
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
	{
		StringBuilder buf = new StringBuilder();
		for(String uri: predSynonyms) {	
			if (buf.length() > 0)
				buf.append("\n\tUNION ");
			else
				buf.append("\n\t");
			
			buf.append("{");
			buf.append(SPARQLStringUtils.strFromTemplate(tripleTemplate, uri));
			buf.append("}");
		}
		return buf.toString();
	}

	public Collection<String> findPredicatesBySubject(Resource subject) throws SADIException
	{
		return findPredicatesBySubject(subject.getURI());
	}

	public Collection<? extends Service> findServices(Resource subject, String predicate) throws SADIException
	{
		return findServices(subject.getURI(), predicate);
	}

	public Collection<ServiceInputPair> discoverServices(Model model) throws SADIException
	{
		return Collections.emptyList();
	}

	public Collection<? extends Service> findServicesByInputInstance(Resource subject) throws SADIException
	{
		return Collections.emptyList();
	}

	public Collection<? extends Service> getAllServices() throws SADIException {
		List<Service> matches = new ArrayList<Service>();
		
		/* TODO I don't like that the text in this string has to match text
		 * contained in another file; we should aim for a situation where
		 * all of the SPARQL is in one place, either all in code or all in
		 * resource files.
		 */
		String query = buildQuery("select.services.sparql");
		for (Map<String, String> binding: executeQuery(query))
			matches.add(getService(binding.get("service")));
		
		return matches;
	}

	public ServiceStatus getServiceStatus(String serviceURI) throws SADIException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<? extends Service> findServicesByInputClass(OntClass clazz, boolean withReasoning) throws SADIException
	{
		return Collections.emptyList();
	}

	@Override
	public Collection<BioMobyService> findServicesByConnectedClass(OntClass clazz, boolean withReasoning) throws SADIException
	{
		return Collections.emptyList();
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.RegistryBase#getLog()
	 */
	@Override
	protected Logger getLog()
	{
		return log;
	}
}
