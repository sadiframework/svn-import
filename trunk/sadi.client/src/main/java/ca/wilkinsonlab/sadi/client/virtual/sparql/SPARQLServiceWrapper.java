package ca.wilkinsonlab.sadi.client.virtual.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInvocationException;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * A proxy object which exposes a SPARQL endpoint as a Service.
 * @author Ben Vandervalk
 */
public class SPARQLServiceWrapper implements Service
{
	private static final Logger log = Logger.getLogger(SPARQLServiceWrapper.class);

	protected static final String RESULTS_LIMIT_CONFIG_KEY = "sadi.sparql.resultsLimit";
	protected static final String BATCH_QUERIES_CONFIG_KEY = "share.sparql.batchQueries";
	protected long resultsLimit;
	protected boolean batchQueries;
	
	protected SPARQLEndpoint endpoint;
	protected SPARQLRegistry registry;
	protected boolean mapInputsToObjectPosition;
	
	public SPARQLServiceWrapper(SPARQLEndpoint endpoint, SPARQLRegistry registry) {
		this(endpoint, registry, false);
	}
	
	public SPARQLServiceWrapper(SPARQLEndpoint endpoint, SPARQLRegistry registry, boolean mapInputsToObjectPosition) 
	{
		setEndpoint(endpoint);
		setRegistry(registry);
		setMapInputsToObjectPosition(mapInputsToObjectPosition);

		setResultsLimit(Config.getConfiguration().getLong(RESULTS_LIMIT_CONFIG_KEY, SPARQLEndpoint.NO_RESULTS_LIMIT));
		setBatchQueries(Config.getConfiguration().getBoolean(BATCH_QUERIES_CONFIG_KEY, true));
	}
	
	@Override
	public int hashCode() {
		return (getURI() + ":" + String.valueOf(mapInputsToObjectPosition())).hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof SPARQLServiceWrapper) {
			SPARQLServiceWrapper asService = (SPARQLServiceWrapper)o;
			return getURI().equals(asService.getURI()) && (mapInputsToObjectPosition() == asService.mapInputsToObjectPosition());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getServiceURI()
	 */
	public String getURI() {
		return getEndpoint().getURI();
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getName()
	 */
	public String getName() {
		return getURI();
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getDescription()
	 */
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
		
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getInputClass()
	 */
	public OntClass getInputClass()
	{
		return OwlUtils.getOWLModel().getOntClass(OWL.Nothing.getURI());
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getOutputClass()
	 */
	public OntClass getOutputClass()
	{
		return OwlUtils.getOWLModel().getOntClass(OWL.Nothing.getURI());
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource)
	 */
	public Collection<Triple> invokeService(Resource inputNode) throws ServiceInvocationException {
		return invokeServiceOnRDFNode(inputNode);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(java.util.Collection)
	 */
	public Collection<Triple> invokeService(Collection<Resource> inputNodes) throws ServiceInvocationException {
		return invokeServiceOnRDFNodes(inputNodes);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
	 */
	public Collection<Triple> invokeService(Resource inputNode, String predicate) throws ServiceInvocationException {
		return invokeServiceOnRDFNode(inputNode, predicate);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(java.util.Collection, java.lang.String)
	 */
	public Collection<Triple> invokeService(Collection<Resource> inputNodes, String predicate) throws ServiceInvocationException
	{
		return invokeServiceOnRDFNodes(inputNodes, predicate);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#isInputInstance(com.hp.hpl.jena.rdf.model.Resource)
	 */
	public boolean isInputInstance(Resource resource)
	{
		boolean matches = false;

		try {
			if(mapInputsToObjectPosition()) {
				matches = getRegistry().objectMatchesRegEx(getEndpoint().getURI(), resource.getURI());
			}
			else {
				matches = getRegistry().subjectMatchesRegEx(getEndpoint().getURI(), resource.getURI());
			}
		} catch(IOException e) {
			throw new RuntimeException("error communicating with SPARQL registry: ", e);
		}

		return matches;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#discoverInputInstances(com.hp.hpl.jena.rdf.model.Model)
	 */
	public Collection<Resource> discoverInputInstances(Model inputModel)
	{
		log.warn("discoverInputInstances not yet implemented");
		return new ArrayList<Resource>(0);
	}

	public long getResultsLimit() { return resultsLimit; }
	public void setResultsLimit(long limit) { resultsLimit = limit; }
	
	public boolean getBatchQueries() { return batchQueries; }
	public void setBatchQueries(boolean batchQueries) { this.batchQueries = batchQueries; }
	
	public SPARQLEndpoint getEndpoint() { return endpoint; }
	public void setEndpoint(SPARQLEndpoint endpoint) {	this.endpoint = endpoint;	}

	public SPARQLRegistry getRegistry() { return registry; }
	public void setRegistry(SPARQLRegistry registry) { this.registry = registry; }

	public boolean mapInputsToObjectPosition() { return mapInputsToObjectPosition; }
	public void setMapInputsToObjectPosition(boolean predicateInverted) { this.mapInputsToObjectPosition = predicateInverted; }


	
	protected Collection<Triple> invokeServiceOnRDFNode(RDFNode inputURIorLiteral) throws ServiceInvocationException
	{
		try {
			Triple queryPattern = getTriplePatternRepresentingServiceInvocation(inputURIorLiteral);
			String query = SPARQLStringUtils.getConstructQueryString(Collections.singletonList(queryPattern), Collections.singletonList(queryPattern));
			return filterOutTriplesWithBlankNodes(getEndpoint().constructQuery(query));
		} catch (IOException e) {
			throw new ServiceInvocationException(e.getMessage(),e);
		}
	}
	
	protected Collection<Triple> invokeServiceOnRDFNode(RDFNode inputURIorLiteral, String predicate) throws ServiceInvocationException 
	{
		try {
			Triple queryPattern = getTriplePatternRepresentingServiceInvocation(inputURIorLiteral, predicate);
			String query = SPARQLStringUtils.getConstructQueryString(Collections.singletonList(queryPattern), Collections.singletonList(queryPattern));
			return filterOutTriplesWithBlankNodes(getEndpoint().constructQuery(query));
		} catch (IOException e) {
			throw new ServiceInvocationException(e.getMessage());
		}
	}

	public Collection<Triple> invokeServiceOnRDFNodes(Collection<? extends RDFNode> inputNodes) throws ServiceInvocationException 
	{
		Collection<Triple> triples; 

		if(inputNodes.size() > 1 && !getEndpoint().ping()) {
			throw new ServiceInvocationException("SPARQL endpoint not responding: " + getEndpoint());
		}

		if(getBatchQueries()) {
			Collection<String> queries = new ArrayList<String>();
			for(RDFNode inputNode : inputNodes) {
				queries.add(getConstructQuery(inputNode));
			}
			triples = batchConstructQueries(queries);
		} else {
			triples = new ArrayList<Triple>();
			for (RDFNode inputNode: inputNodes) {
				triples.addAll(invokeServiceOnRDFNode(inputNode));
			}
		}
		return filterOutTriplesWithBlankNodes(triples);
	}

	public Collection<Triple> invokeServiceOnRDFNodes(Collection<? extends RDFNode> inputNodes, String predicate) throws ServiceInvocationException 
	{
		Collection<Triple> triples; 
		
		if(inputNodes.size() > 1 && !getEndpoint().ping()) {
			throw new ServiceInvocationException("SPARQL endpoint not responding: " + getEndpoint());
		}
		
		if(getBatchQueries()) {
			Collection<String> queries = new ArrayList<String>();
			for(RDFNode inputNode : inputNodes) {
				queries.add(getConstructQuery(inputNode, predicate));
			}
			triples = batchConstructQueries(queries);
		} else {
			triples = new ArrayList<Triple>();
			for (RDFNode inputNode: inputNodes) {
				triples.addAll(invokeServiceOnRDFNode(inputNode, predicate));
			}
		}
		return filterOutTriplesWithBlankNodes(triples);
	}
	
	protected Collection<Triple> batchConstructQueries(Collection<String> queries) throws ServiceInvocationException
	{
		Collection<ConstructQueryResult> results = getEndpoint().constructQueryBatch(queries);
		
		Model mergedModel = ModelFactory.createDefaultModel();
		for(ConstructQueryResult result : results) {
			if(result.exceptionOccurred()) {
				log.error("exception occurred executing query: " + result.getOriginalQuery(), result.getException());
			} else {			
				mergedModel.add(result.getResultModel());
			}
		}

		return filterOutTriplesWithBlankNodes(RdfUtils.modelToTriples(mergedModel));
	}
	
	@Override
	public String toString()
	{
		return getURI();
	}
	
	protected Triple getTriplePatternRepresentingServiceInvocation(RDFNode inputURIorLiteral) 
	{
		Triple triplePattern;
		Node var1 = NodeCreateUtils.create("?var1");
		Node var2 = NodeCreateUtils.create("?var2");

		if(mapInputsToObjectPosition()) {
			triplePattern = new Triple(var1, var2, inputURIorLiteral.asNode());
		}
		else {
			if(inputURIorLiteral.isLiteral()) {
				throw new RuntimeException("used a triple pattern where the subject is a literal.");
			}
			triplePattern = new Triple(inputURIorLiteral.asNode(), var1, var2);
		}
		return triplePattern;
	}

	protected Triple getTriplePatternRepresentingServiceInvocation(RDFNode inputURIorLiteral, String predicate) 
	{
		Triple triplePattern;
		Node var1 = NodeCreateUtils.create("?var1");
		Node p = NodeCreateUtils.create(predicate);

		if(mapInputsToObjectPosition()) {
			triplePattern = new Triple(var1, p, inputURIorLiteral.asNode());
		}
		else {
			if(inputURIorLiteral.isLiteral()) {
				throw new RuntimeException("used a triple pattern where the subject is a literal.");
			}
			triplePattern = new Triple(inputURIorLiteral.asNode(), p, var1);
		}
		return triplePattern;
	}
	
	protected String getConstructQuery(RDFNode inputURIorLiteral, String predicate) 
	{
		Triple triplePattern = getTriplePatternRepresentingServiceInvocation(inputURIorLiteral, predicate);
		return SPARQLStringUtils.getConstructQueryString(Collections.singletonList(triplePattern), Collections.singletonList(triplePattern));
	}
	
	protected String getConstructQuery(RDFNode inputURIorLiteral) 
	{
		Triple triplePattern = getTriplePatternRepresentingServiceInvocation(inputURIorLiteral);
		return SPARQLStringUtils.getConstructQueryString(Collections.singletonList(triplePattern), Collections.singletonList(triplePattern));
	}
	
	/**
	 * Return the input collection of triples, less any triples that reference blank nodes.
	 * This filtering is necessary because blank nodes cannot be reconciled across 
	 * distributed RDF data sources. Further, they cannot be used as inputs to 
	 * SADI services or SPARQL endpoints. 
	 * 
	 * @param triples
	 * @return the input collection of triples, with any triples that reference blank
	 * nodes removed.
	 */
	protected Collection<Triple> filterOutTriplesWithBlankNodes(Collection<Triple> triples) {
		
		Collection<Triple> filtered = new ArrayList<Triple>();
		for(Triple triple : triples) {
			if(triple.getSubject().isBlank() || triple.getPredicate().isBlank() || triple.getObject().isBlank()) {
				log.warn("omitting triple with blank node(s) from results: " + triple);
				continue;
			}
			filtered.add(triple);
		}
		return filtered;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getInputClassURI()
	 */
	@Override
	public String getInputClassURI()
	{
		return getInputClass().getURI();
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getOutputClassURI()
	 */
	@Override
	public String getOutputClassURI()
	{
		return getOutputClass().getURI();
	}
}
