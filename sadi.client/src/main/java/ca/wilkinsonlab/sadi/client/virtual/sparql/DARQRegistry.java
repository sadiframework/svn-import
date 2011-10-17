package ca.wilkinsonlab.sadi.client.virtual.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.RegistryBase;
import ca.wilkinsonlab.sadi.client.RegistrySearchCriteria;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInputPair;
import ca.wilkinsonlab.sadi.client.ServiceStatus;
import ca.wilkinsonlab.sadi.utils.QueryExecutor;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.util.NodeFactory;

/**
 * @author Ben Vandervalk
 */
public class DARQRegistry extends RegistryBase implements SPARQLRegistry 
{
	protected static final Logger log = Logger.getLogger(DARQRegistry.class);

	public DARQRegistry(Configuration config) throws IOException 
	{
		super(config);
	}
	
	protected DARQRegistry(QueryExecutor executor) 
	{
		super(executor);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.RegistryBase#createService(java.lang.String)
	 */
	@Override
	protected Service createService(String serviceURI) throws SADIException 
	{
		return createService(serviceURI, false);
	}

	protected Service createService(String serviceURI, boolean mapInputsToObjectPosition) throws SADIException 
	{
		return new SPARQLServiceWrapper(getSPARQLEndpoint(serviceURI), this, mapInputsToObjectPosition);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.RegistryBase#getLog()
	 */
	@Override
	protected Logger getLog() 
	{
		return log;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry#findEndpointsByTriplePattern(com.hp.hpl.jena.graph.Triple)
	 */
	@Override
	public Collection<SPARQLEndpoint> findSPARQLEndpointsByTriplePattern(Triple triplePattern) throws SADIException 
	{
		Collection<SPARQLEndpoint> matches = new ArrayList<SPARQLEndpoint>();
		
		Node s = triplePattern.getSubject();
		Node p = triplePattern.getPredicate();
		Node o = triplePattern.getObject();

		if(s.isBlank() || p.isBlank() || o.isBlank()) {
			throw new SADIException(String.format("blank nodes are not allowed in triple pattern (given pattern was: %s)", triplePattern));
		}
		
		StringBuilder query = new StringBuilder()
			.append("PREFIX darq: <http://darq.sf.net/dose/0.1#>\n")
			.append("PREFIX darqx: <http://sadiframework.org/ontologies/DARQ/darq-extensions.owl#>\n")
			.append("PREFIX sadi: <http://sadiframework.org/ontologies/sadi.owl#>\n")
			.append("\n")
			.append("SELECT DISTINCT ?url\n")
			.append("WHERE {\n")
			.append("\t_:endpoint a darq:Service .\n")
			.append("\t_:endpoint sadi:serviceStatus ?status .")
			.append("\t_:endpoint darq:url ?url .\n")
			.append("\t_:endpoint darqx:graph ?graph .\n")
			.append("\t?graph darq:capability ?capability .")
			.append("\n");
			
		if(!s.isVariable()) {
			query.append("\t?capability darqx:subjectRegex ?subjectRegex .\n");
			query.append(String.format("\tFILTER regex('%s', str(?subjectRegex))\n", s.getURI()));
		}
		
		if(!p.isVariable()) {
			query.append(SPARQLStringUtils.strFromTemplate("\t?capability darq:predicate %u% .\n", p.getURI()));
		}
		
		if(!o.isVariable() && o.isURI()) {
			query.append("\t?capability darqx:objectRegex ?objectRegex .\n");
			query.append(String.format("\tFILTER regex('%s', str(?objectRegex))\n", o.getURI()));
		}

		query.append("FILTER (!sameTerm(?status, sadi:dead))");
		query.append("}");
		
		List<Map<String,String>> bindings = executeQuery(query.toString());
		
		for(Map<String,String> binding : bindings) {
			matches.add(getSPARQLEndpoint(binding.get("url")));
		}

		return matches;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry#getAllEndpoints()
	 */
	@Override
	public Collection<SPARQLEndpoint> getAllSPARQLEndpoints() throws SADIException 
	{
		Collection<SPARQLEndpoint> endpoints = new ArrayList<SPARQLEndpoint>();
		
		String query = buildQuery("getAllSPARQLEndpoints.sparql");
		List<Map<String,String>> bindings = executeQuery(query);
		
		for(Map<String,String> binding : bindings) {
			endpoints.add(getSPARQLEndpoint(binding.get("url")));
		}
		
		return endpoints;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry#getEndpoint(java.lang.String)
	 */
	@Override
	public SPARQLEndpoint getSPARQLEndpoint(String endpointURL) throws SADIException
	{
		if (!hasSPARQLEndpoint(endpointURL)) {
			throw new SADIException(String.format("no such service %s in this registry", endpointURL));
		} 

		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(endpointURL);
		endpoint.setResultsLimit(getResultsLimit(endpointURL));

		return endpoint;
	}

	protected boolean hasSPARQLEndpoint(String endpointURL) throws SADIException 
	{
		String query = buildQuery("hasSPARQLEndpoint.sparql", endpointURL);
		List<Map<String, String>> bindings = executeQuery(query);
		
		if(bindings.size() > 1) {
			throw new SADIException(String.format("URL %s maps to more than one service in this registry", endpointURL));
		}
		
		return (bindings.size() == 1);
	}
	
	protected long getResultsLimit(String endpointURL) throws SADIException
	{
		String query = buildQuery("getResultsLimit.sparql", endpointURL);
		List<Map<String, String>> bindings = executeQuery(query);
		
		if(bindings.size() > 1) {
			throw new SADIException(String.format("URL %s has multiple result limits in this registry", endpointURL));
		}
		
		// specifying a results limit in a DARQ index is optional  
		if(bindings.size() == 0) {
			return SPARQLEndpoint.NO_RESULTS_LIMIT;
		} 

		return Long.valueOf(bindings.get(0).get("resultsLimit"));
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry#objectMatchesRegEx(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean objectMatchesRegEx(String endpointURI, String uri) throws SADIException 
	{
		Node s = NodeFactory.parseNode("?s");
		Node p = NodeFactory.parseNode("?p");
		Node o = NodeFactory.parseNode(String.format("<%s>", uri));
		
		Collection<SPARQLEndpoint> matches = findSPARQLEndpointsByTriplePattern(new Triple(s, p, o));
		
		for(SPARQLEndpoint match : matches) {
			if(match.getURI().equals(endpointURI)) {
				return true;
			}
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry#subjectMatchesRegEx(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean subjectMatchesRegEx(String endpointURI, String uri) throws SADIException 
	{
		Node s = NodeFactory.parseNode(String.format("<%s>", uri));
		Node p = NodeFactory.parseNode("?p");
		Node o = NodeFactory.parseNode("?o");
		
		Collection<SPARQLEndpoint> matches = findSPARQLEndpointsByTriplePattern(new Triple(s, p, o));
		
		for(SPARQLEndpoint match : matches) {
			if(match.getURI().equals(endpointURI)) {
				return true;
			}
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#discoverServices(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	public Collection<? extends ServiceInputPair> discoverServices(Model model)	throws SADIException 
	{
		/* Note: This method does not attempt to match Literals in the model to
		 * specific endpoints, because any Literal will match *all* endpoints.  
		 * Only the subject/object URIs in the model are used for service discovery. 
		 */ 

		Map<Service, Collection<Resource>> serviceToInputs = new HashMap<Service, Collection<Resource>>();

		Collection<RDFNode> potentialInputs = new ArrayList<RDFNode>();
		potentialInputs.addAll(model.listSubjects().toList());
		potentialInputs.addAll(model.listObjects().toList());
		
		for(RDFNode potentialInput : potentialInputs) {
			if (potentialInput.isURIResource()) {
				for (Service service : discoverServices(potentialInput.asResource())) {
					if (!serviceToInputs.containsKey(service)) 
						serviceToInputs.put(service, new ArrayList<Resource>());
					serviceToInputs.get(service).add(potentialInput.asResource());
				}
			}
		}
		
		Collection<ServiceInputPair> serviceInputPairs = new ArrayList<ServiceInputPair>();
		for (Service service : serviceToInputs.keySet()) {
			serviceInputPairs.add(new ServiceInputPair(service, serviceToInputs.get(service)));
		}
		
		return serviceInputPairs; 
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#findPredicatesBySubject(com.hp.hpl.jena.rdf.model.Resource)
	 */
	
	@Deprecated
	@Override
	public Collection<String> findPredicatesBySubject(Resource subject)	throws SADIException 
	{
		Collection<? extends Service> services = discoverServices(subject);
		Set<String> attachedProperties = new HashSet<String>();
		for (Service service : services) {
			for (Property p : getAttachedProperties(service)) {
				attachedProperties.add(p.getURI());
			}
		}
		return attachedProperties;
	}

	protected Collection<Property> getAttachedProperties(Service service) throws SADIException
	{
		SPARQLServiceWrapper sparqlService = (SPARQLServiceWrapper)service;
		Set<Property> attachedProperties = new HashSet<Property>();
				
		// Tricky: In the case of SPARQL services where the service inputs are
		// mapped to the object position of the triples in the endpoint, we have 
		// no way of knowing what the attached properties are.  In order to 
		// know that, we would need to know the inverses of all the predicates
		// in the endpoint are and that information is not available
		// in the registry.
		
		if (sparqlService.mapInputsToObjectPosition()) {
			return attachedProperties; 
		}

		SPARQLEndpoint endpoint = sparqlService.getEndpoint();
		
		String query = buildQuery("getAttachedProperties.sparql", endpoint.getURI());
		List<Map<String,String>> bindings = executeQuery(query.toString());
		
		for(Map<String,String> binding : bindings) {
			attachedProperties.add(ResourceFactory.createProperty(binding.get("predicate")));
		}

		return attachedProperties;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#findServices(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
	 */
	@Deprecated
	@Override
	public Collection<? extends Service> findServices(Resource subject, String predicate) throws SADIException 
	{
		Collection<Service> services = new ArrayList<Service>();
		Triple pattern = new Triple(subject.asNode(), NodeCreateUtils.create(predicate), NodeCreateUtils.create("?x"));
		pattern = undoSHAREInversePredicateHack(pattern);
		for (SPARQLEndpoint endpoint : findSPARQLEndpointsByTriplePattern(pattern)) {
			services.add(createService(endpoint.getURI()));
		}
		return services;
	}
	
	protected Triple undoSHAREInversePredicateHack(Triple pattern) 
	{
		String uri = pattern.getPredicate().getURI(); 
		if (uri.endsWith("-inverse")) {
			if (pattern.getSubject().isLiteral()) { 
				log.error("failed to invert predicate, literal in object position");
				return pattern;
			}
			Node p = NodeCreateUtils.create(StringUtils.removeEnd(uri, "-inverse"));
			return new Triple(pattern.getObject(), p, pattern.getSubject());
		}
		return pattern;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#findServicesByPredicate(java.lang.String)
	 */
	@Deprecated
	@Override
	public Collection<? extends Service> findServicesByPredicate(String predicate) throws SADIException 
	{
		Collection<Service> services = new ArrayList<Service>();
		Triple pattern = new Triple(NodeCreateUtils.create("?x"), NodeCreateUtils.create(predicate), NodeCreateUtils.create("?y"));
		pattern = undoSHAREInversePredicateHack(pattern);
		for (SPARQLEndpoint endpoint : findSPARQLEndpointsByTriplePattern(pattern)) {
			services.add(createService(endpoint.getURI()));
		}
		return services;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Registry#getAllServices()
	 */
	@Override
	public Collection<? extends Service> getAllServices() throws SADIException 
	{
		Collection<Service> services = new ArrayList<Service>();
		for (SPARQLEndpoint endpoint : getAllSPARQLEndpoints()) {
			services.add(createService(endpoint.getURI(), false));
			services.add(createService(endpoint.getURI(), true));
		}
		return services;
	}

	@Override 
	public Collection<? extends Service> findServicesByAttachedProperty(Property property) throws SADIException
	{
		Collection<Service> services = new HashSet<Service>();
		Collection<Property> properties = new HashSet<Property>();
		Collection<Property> inverseProperties = new HashSet<Property>();
		
		if (property.isURIResource()) {
			properties.add(property);
		}
		
		if (property.canAs(OntProperty.class)) {
			OntProperty ontProperty = property.as(OntProperty.class);
			for (Property subproperty : ontProperty.listSubProperties().toList()) {
				if (subproperty.isURIResource()) {
					properties.add(subproperty);
				}
			}
			OntProperty inverseProperty = ontProperty.getInverse();
			if (inverseProperty != null) {
				for (Property subproperty : inverseProperty.listSubProperties().toList()) {
					if (subproperty.isURIResource()) {
						inverseProperties.add(subproperty);
					}
				}
			}
		}

		for (Property p : properties) {
			Triple pattern = new Triple(NodeCreateUtils.create("?s"), p.asNode(), NodeCreateUtils.create("?o"));
			for (SPARQLEndpoint endpoint : findSPARQLEndpointsByTriplePattern(pattern)) {
				services.add(createService(endpoint.getURI(), false));
			}
		}
		
		for (Property p : inverseProperties) {
			Triple pattern = new Triple(NodeCreateUtils.create("?s"), p.asNode(), NodeCreateUtils.create("?o"));
			for (SPARQLEndpoint endpoint : findSPARQLEndpointsByTriplePattern(pattern)) {
				services.add(createService(endpoint.getURI(), true));
			}
		}
		
		return services;
	}

	@Override 
	public Collection<? extends Service> findServicesByInputClass(Resource clazz) throws SADIException
	{
		// TODO: It may be possible to support by including the rdf:types for
		// the subjects/objects of each predicate in the endpoint index.  
		return Collections.emptyList();
	}

	@Override 
	public Collection<? extends Service> findServicesByConnectedClass(Resource clazz) throws SADIException
	{
		// TODO: It may be possible to support this by storing rdf:types for
		// the subjects/objects of each predicate in the endpoint index.  
		return Collections.emptyList();
	}

	@Override
	protected Collection<? extends Service> findServicesByAttachedProperty(Iterable<String> propertyURIs) throws SADIException 
	{
		// I expect this method will soon disappear from RegistryBase, because it uses
		// Strings for predicates, instead of Jena Property objects.
		throw new UnsupportedOperationException();
	}

	@Override
	protected Collection<? extends Service> findServicesByConnectedClass(Iterable<String> classURIs) throws SADIException 
	{
		// I expect this method will soon disappear from RegistryBase, because it uses
		// Strings for predicates, instead of Jena Property objects.
		throw new UnsupportedOperationException();
	}

	@Override
	protected Collection<? extends Service> findServicesByInputClass(Iterable<String> classURIs) throws SADIException 
	{
		// I expect this method will soon disappear from RegistryBase, because it uses
		// Strings for predicates, instead of Jena Property objects.
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<? extends Service> discoverServices(Resource subject) throws SADIException 
	{
		Collection<Service> services = new HashSet<Service>();
		
		if (subject.isURIResource()) {
			Triple subjectPattern = new Triple(subject.asNode(), NodeCreateUtils.create("?p"), NodeCreateUtils.create("?o"));
			for (SPARQLEndpoint endpoint : findSPARQLEndpointsByTriplePattern(subjectPattern)) {
				services.add(createService(endpoint.getURI(), false));
			}
			Triple objectPattern = new Triple(NodeCreateUtils.create("?s"), NodeCreateUtils.create("?p"), subject.asNode());
			for (SPARQLEndpoint endpoint : findSPARQLEndpointsByTriplePattern(objectPattern)) {
				services.add(createService(endpoint.getURI(), true));
			}
		}
		
		return services;
	}

	@Override
	public Collection<Property> findAttachedProperties(RegistrySearchCriteria criteria) throws SADIException 
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Collection<? extends Service> findServices(RegistrySearchCriteria criteria) throws SADIException 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ServiceStatus getServiceStatus(String serviceURI) throws SADIException 
	{
		if (!hasSPARQLEndpoint(serviceURI)) {
			throw new SADIException(String.format("no such service %s in this registry", serviceURI));
		} 
		
		String query = buildQuery("getServiceStatus.sparql", serviceURI);
		List<Map<String,String>> bindings = executeQuery(query);
		
		if (bindings.isEmpty()) {
			getLog().error(String.format("registry is corrupt, no status in registry for service %s!", serviceURI));
			return ServiceStatus.OK;
		} 
		
		if (bindings.size() > 1) {
			getLog().error(String.format("registry is corrupt, more than one status for service %s!", serviceURI));
		}

		return ServiceStatus.uriToStatus(bindings.get(0).get("status"));
	}

}
