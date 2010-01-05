package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInvocationException;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.TemplateGroup;

/**
 * A proxy object which exposes a SPARQL endpoint as a Service.
 * @author Ben Vandervalk
 */
public class SPARQLServiceWrapper implements Service
{
	protected static final Logger log = Logger.getLogger(SPARQLServiceWrapper.class);

	protected static final String RESULTS_LIMIT_CONFIG_KEY = "sadi.sparql.resultsLimit";
	protected static final String BATCH_QUERIES_CONFIG_KEY = "share.sparql.batchQueries";
	protected long resultsLimit;
	protected boolean batchQueries;
	
	protected SPARQLEndpoint endpoint;
	protected SPARQLRegistry registry;
	protected String predicate;
	protected boolean predicateIsInverse;
	
	public SPARQLServiceWrapper(SPARQLEndpoint endpoint, SPARQLRegistry registry) {
		this(endpoint, registry, null, false);
	}
	
	public SPARQLServiceWrapper(SPARQLEndpoint endpoint, SPARQLRegistry registry, String predicate, boolean predicateIsInverse) {
		
		setEndpoint(endpoint);
		setRegistry(registry);
		setPredicate(predicate);
		setPredicateIsInverse(predicateIsInverse);

		if(Config.getConfiguration().containsKey(RESULTS_LIMIT_CONFIG_KEY)) {
			setResultsLimit(Config.getConfiguration().getLong(RESULTS_LIMIT_CONFIG_KEY, SPARQLEndpoint.NO_RESULTS_LIMIT));
		}
		
		if(Config.getConfiguration().containsKey(BATCH_QUERIES_CONFIG_KEY)) {
			setBatchQueries(Config.getConfiguration().getBoolean(BATCH_QUERIES_CONFIG_KEY, true));
		}
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getServiceURI()
	 */
	public String getServiceURI() {
		return getEndpoint().getURI();
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getName()
	 */
	public String getName() {
		return getServiceURI();
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
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getOutputClass()
	 */
	public OntClass getOutputClass()
	{
		return null;
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
			if(predicateIsInverse()) {
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

	public boolean predicateIsInverse() { return predicateIsInverse; }
	public void setPredicateIsInverse(boolean predicateInverted) { this.predicateIsInverse = predicateInverted; }

	public String getPredicate() { return predicate; }
	public void setPredicate(String predicate) {this.predicate = predicate; }
	
	protected Collection<Triple> invokeServiceOnRDFNode(RDFNode inputURIorLiteral) throws ServiceInvocationException
	{
		try {
			Triple queryPattern = getTriplePatternRepresentingServiceInvocation(inputURIorLiteral);
			return getEndpoint().constructQuery(getConstructQuery(queryPattern));
		} catch (IOException e) {
			throw new ServiceInvocationException(e.getMessage(),e);
		}
	}
	
	protected Collection<Triple> invokeServiceOnRDFNode(RDFNode inputURIorLiteral, String predicate) throws ServiceInvocationException 
	{
		try {
			Triple queryPattern = getTriplePatternRepresentingServiceInvocation(inputURIorLiteral, predicate);
			return getEndpoint().constructQuery(getConstructQuery(queryPattern));
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
		return triples;
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
		return triples;
	}
	
	protected Collection<Triple> batchConstructQueries(Collection<String> queries) throws ServiceInvocationException
	{
		Collection<ConstructQueryResult> results = getEndpoint().constructQueryBatch(queries);
		
		// if something goes awry during a batch of queries, the best recovery strategy is to 
		// re-run the whole batch (if recovery is possible/appropriate for the given exception) -- BV
		
		for(ConstructQueryResult result : results) {
			if(result.exceptionOccurred()) {
				String originalQuery = result.getOriginalQuery();
				throw new ServiceInvocationException("exception occurred executing query: " +  originalQuery, result.getException());
			}			
		}
		
		Model mergedModel = ModelFactory.createDefaultModel();
		for(ConstructQueryResult result : results) {
			mergedModel.add(result.getResultModel());
		}

		return RdfUtils.modelToTriples(mergedModel);
	}
	
	@Override
	public String toString()
	{
		return getServiceURI();
	}
	
	protected Triple getTriplePatternRepresentingServiceInvocation(RDFNode inputURIorLiteral) 
	{
		Triple triplePattern;
		Node var1 = NodeCreateUtils.create("?var1");
		Node var2 = NodeCreateUtils.create("?var2");

		if(predicateIsInverse()) {
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

		if(predicateIsInverse()) {
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
		return getConstructQuery(triplePattern);
	}
	
	protected String getConstructQuery(RDFNode inputURIorLiteral) 
	{
		Triple triplePattern = getTriplePatternRepresentingServiceInvocation(inputURIorLiteral);
		return getConstructQuery(triplePattern);
	}
	
	protected String getConstructQuery(Triple triplePattern) 
	{
		Query constructQuery = new Query();
		constructQuery.setQueryConstructType();
		
		TemplateGroup constructTemplate = new TemplateGroup();
		constructTemplate.addTriple(triplePattern);
		constructQuery.setConstructTemplate(constructTemplate);

		ElementTriplesBlock queryPattern = new ElementTriplesBlock();
		queryPattern.addTriple(triplePattern);
		constructQuery.setQueryPattern(queryPattern);		

		return constructQuery.serialize();
	}
}
