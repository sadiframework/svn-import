package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;

import ca.wilkinsonlab.sadi.client.Registry;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInputPair;
import ca.wilkinsonlab.sadi.utils.StringUtil;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;
import ca.wilkinsonlab.sadi.vocab.SPARQLRegistry;
import ca.wilkinsonlab.sadi.vocab.W3C;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * 
 * @author Ben Vandervalk
 */
public class VirtuosoSPARQLRegistry extends VirtuosoSPARQLEndpoint implements Registry
{	
	private static final String ENDPOINT_CONFIG_KEY = "endpoint";
	private static final String INDEX_GRAPH_CONFIG_KEY = "indexGraph";
	private static final String ONTOLOGY_GRAPH_CONFIG_KEY = "ontologyGraph";
	
	private OntModel predicateOntology;
	private String indexGraphURI;
	private String ontologyGraphURI;
	
	public VirtuosoSPARQLRegistry(Configuration config) throws HttpException, IOException
	{
		this(config.getString(ENDPOINT_CONFIG_KEY),
				config.getString(INDEX_GRAPH_CONFIG_KEY),
				config.getString(ONTOLOGY_GRAPH_CONFIG_KEY));
	}
	
	public VirtuosoSPARQLRegistry() throws HttpException, IOException 
	{
		this(SPARQLRegistry.DEFAULT_REGISTRY_ENDPOINT, 
			SPARQLRegistry.DEFAULT_INDEX_GRAPH, 
			SPARQLRegistry.DEFAULT_ONTOLOGY_GRAPH);
	}
	
	public VirtuosoSPARQLRegistry(String URI) throws HttpException, IOException 
	{
		this(URI, 
			SPARQLRegistry.DEFAULT_INDEX_GRAPH, 
			SPARQLRegistry.DEFAULT_ONTOLOGY_GRAPH);
	}
	
	public VirtuosoSPARQLRegistry(String URI, String indexGraphURI, String ontologyGraphURI) throws HttpException, IOException 
	{
		super(URI);
		this.indexGraphURI = indexGraphURI;
		this.ontologyGraphURI = ontologyGraphURI;
		refreshCachedOntology();
	}
	
	public String getIndexGraphURI() 
	{
		return indexGraphURI;
	}

	public String getOntologyGraphURI()
	{
		return ontologyGraphURI;
	}

	public String getRegistryURI() { return this.getURI(); }

	private boolean refreshCachedOntology() throws HttpException, IOException 
	{
		/**
		 * I'm using the "micro" model here because all we need is reasoning 
		 * support for "equivalentProperty" and "inverseOf". See 
		 * http://jena.sourceforge.net/ontology/index.html#creatingModels for
		 * an explanation.
		 */
		predicateOntology = ModelFactory.createOntologyModel();
		String predicateQuery = "SELECT ?s ?o FROM %u% WHERE { ?s %u% ?o }";
		predicateQuery = StringUtil.strFromTemplate(predicateQuery, getOntologyGraphURI(), W3C.PREDICATE_RDF_TYPE);
		List<Map<String,String>> results = selectQuery(predicateQuery);
		for(Map<String,String> binding : results) {
			String predicateURI = binding.get("s");
			String typeURI = binding.get("o");
			if(typeURI.equals(W3C.OWL_PREFIX + "DatatypeProperty"))
				predicateOntology.createDatatypeProperty(predicateURI);
			else
				predicateOntology.createObjectProperty(predicateURI);
		}
		return true;
	}

	public Collection<Service> findServicesByPredicate(String predicate) throws URIException, HttpException, IOException
	{
		List<Service> matches = new ArrayList<Service>();
		
		boolean isInverted = false; 
		if(PelletHelper.isInverted(predicate)) {
			predicate = PelletHelper.invert(predicate);
			isInverted = true;
		}
		
		/** 
		 * TODO: Also look for SPARQL endpoints that contain inverses of 'predicate'.
		 * The code commented out below is a template for doing that.  It's turned off
		 * at the moment because the Jena reasoning is really slow.
		 */

		/*
		// Look for endpoints that match the forward predicate (or any synonyms).
		
		OntProperty ontProperty = getPredicateOntology().getOntProperty(predicate);
		List<String> synonyms = new ArrayList<String>();
		Iterator<OntProperty> i = ontProperty.listEquivalentProperties();
		synonyms.add(ontProperty.toString());
		while(i.hasNext()) 
			synonyms.add(i.next().toString());
		for(String synonym : synonyms) {
			Collection<SPARQLEndpoint> endpoints = findEndpointsByIndividualPredicate(synonym.toString());
			for(SPARQLEndpoint endpoint : endpoints)
				matches.add(new SPARQLServiceWrapper(endpoint, synonym, predicate, isInverted));
		}
			
		// Look for endpoints that match the reverse predicate (or any synonyms).
		
		Iterator<OntProperty> invProperties = ontProperty.listInverse();
		while(invProperties.hasNext()) {
			String inverse = invProperties.next().toString();
			Collection<SPARQLEndpoint> endpoints = findEndpointsByIndividualPredicate(inverse);
			for(SPARQLEndpoint endpoint : endpoints)
				matches.add(new SPARQLServiceWrapper(endpoint, inverse, predicate, !isInverted));
		}
		*/
		
		String predicatesQuery = "SELECT DISTINCT ?endpoint FROM %u% WHERE { ?endpoint %u% %u% }";
		predicatesQuery = StringUtil.strFromTemplate(predicatesQuery, getIndexGraphURI(), SPARQLRegistry.PREDICATE_HASPREDICATE, predicate);
		List<Map<String,String>> results = selectQuery(predicatesQuery);

		/*  This turns out to be too slow in pratice. -- BV 
		// Also include those endpoints that are in the registry, but haven't been indexed yet.
		String unindexedQuery = "SELECT ?endpoint FROM %u% WHERE { ?endpoint %u% %v% }";
		unindexedQuery = StringUtil.strFromTemplate(unindexedQuery, getIndexGraphURI(), SPARQLRegistry.PREDICATE_COMPUTEDINDEX, "0");
		results.addAll(selectQuery(unindexedQuery));
		*/
		
		for(Map<String,String> binding : results) {
			SPARQLService endpoint = (SPARQLService)getService(binding.get("endpoint"));
			matches.add(new SPARQLServiceWrapper(endpoint, predicate, isInverted));
		}

		return matches;
	}

	public SPARQLEndpointType getEndpointType(String endpointURI) throws HttpException, IOException
	{
		if(!hasEndpoint(endpointURI))
			return null;
		String typeQuery = "SELECT ?type FROM %u% WHERE { %u% %u% ?type }";
		typeQuery = StringUtil.strFromTemplate(typeQuery, getIndexGraphURI(), endpointURI, W3C.PREDICATE_RDF_TYPE);
		List<Map<String,String>> results = selectQuery(typeQuery);
		if(results.size() == 0) 
			throw new RuntimeException("No type found in registry for endpoint " + endpointURI);
		return SPARQLEndpointType.valueOf(results.get(0).get("type"));
	}

	public boolean hasEndpoint(String endpointURI) throws HttpException, IOException 
	{
		String existsQuery = "SELECT * FROM %u% WHERE { %u% ?p ?o } LIMIT 1";
		existsQuery = StringUtil.strFromTemplate(existsQuery, getIndexGraphURI(), endpointURI);
		List<Map<String,String>> results = selectQuery(existsQuery);
		if(results.size() > 0)
			return true;
		return false;
	}
	
	public List<SPARQLService> getEndpoints() throws HttpException, IOException
	{
		List<SPARQLService> endpoints = new ArrayList<SPARQLService>();
		String endpointQuery = "SELECT DISTINCT ?endpoint ?type FROM %u% WHERE { ?endpoint %u% ?type }";
		endpointQuery = StringUtil.strFromTemplate(endpointQuery, getIndexGraphURI(), W3C.PREDICATE_RDF_TYPE);
		List<Map<String,String>> results = selectQuery(endpointQuery);
		for(Map<String,String> binding : results) {
			SPARQLEndpointType type = SPARQLEndpointType.valueOf(binding.get("type"));
			endpoints.add(SPARQLEndpointFactory.createEndpoint(binding.get("endpoint"), type));
		}
		return endpoints;
	}

	public List<String> getEndpointURIs() throws HttpException, IOException
	{
		List<String> endpoints = new ArrayList<String>();
		String endpointQuery = "SELECT DISTINCT ?endpoint ?type FROM %u% WHERE { ?endpoint %u% ?type }";
		endpointQuery = StringUtil.strFromTemplate(endpointQuery, W3C.PREDICATE_RDF_TYPE);
		List<Map<String,String>> results = selectQuery(endpointQuery);
		for(Map<String,String> binding : results)
			endpoints.add(binding.get("endpoint"));
		return endpoints;
	}
	
	public SPARQLEndpointStatus getEndpointStatus(String endpointURI) throws URIException, HttpException, HttpResponseCodeException, IOException 
	{
		String statusQuery = "SELECT ?status FROM %u% WHERE { %u% %u% ?status }";
		statusQuery = StringUtil.strFromTemplate(statusQuery, getIndexGraphURI(), endpointURI, SPARQLRegistry.PREDICATE_ENDPOINTSTATUS);
		List<Map<String,String>> results = selectQuery(statusQuery);
		return SPARQLEndpointStatus.valueOf(results.get(0).get("status"));
	}
	
	public OntModel getPredicateOntology() 
	{
		return predicateOntology; 
	}
	
	public boolean hasPredicate(String predicateURI) throws URIException, HttpException, HttpResponseCodeException, IOException 
	{
		String query = "SELECT * FROM %u% WHERE { %u% ?p ?o } LIMIT 1";
		query = StringUtil.strFromTemplate(query, getOntologyGraphURI(), predicateURI);
		List<Map<String,String>> results = selectQuery(query);
		if(results.size() > 0)
			return true;
		else 
			return false;
	}
	
	public boolean isDatatypeProperty(String predicateURI) throws URIException, HttpException, HttpResponseCodeException, IOException 
	{
		String query = "SELECT ?o FROM %u% WHERE { %u% %u% ?o } LIMIT 1";
		query = StringUtil.strFromTemplate(query,
				getOntologyGraphURI(),
				predicateURI,
				W3C.PREDICATE_RDF_TYPE);
		List<Map<String,String>> results = selectQuery(query);
		
		if(results.size() == 0) {
			throw new RuntimeException("The SPARQL registry have the predicate: " + predicateURI 
					+ ". You need to need to check with registry.hasPredicate() first.");
		}
		if(results.get(0).get("o").equals(W3C.TYPE_DATATYPE_PROPERTY))
			return true;
		else 
			return false;
	}

	public Collection<String> findPredicatesBySubject(String subject) throws IOException 
	{
		throw new RuntimeException("This method is not implemented.");
	}

	public Collection<Service> findServices(String subject, String predicate) throws URIException, HttpException, IOException
	{
		return findServicesByPredicate(predicate);
	}

	public Service getService(String serviceURI) throws HttpException, IOException 
	{
		if(!hasEndpoint(serviceURI))
			return null;
		return SPARQLEndpointFactory.createEndpoint(serviceURI, getEndpointType(serviceURI));
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
