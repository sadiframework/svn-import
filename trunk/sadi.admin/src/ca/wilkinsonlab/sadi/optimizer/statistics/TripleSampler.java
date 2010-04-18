package ca.wilkinsonlab.sadi.optimizer.statistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.random.RandomDataImpl;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry;
import ca.wilkinsonlab.sadi.common.SADIException;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.expr.E_IsBlank;
import com.hp.hpl.jena.sparql.expr.E_LogicalNot;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.aggregate.AggCount;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.TemplateGroup;

/**
 * Obtain a randomly selected triple matching the given triple pattern, from
 * the set of available SPARQL endpoints.
 */
public class TripleSampler
{
	public final static Logger log = Logger.getLogger(TripleSampler.class);
	
	protected SPARQLRegistry registry;
	protected SPARQLEndpoint endpoint;
	protected static final int MAX_ATTEMPTS = 3;
	
	protected UpperSampleLimitCache upperSampleLimitCache = new UpperSampleLimitCache();

	/** 
	 * Use this constructor to sample triples across all SPARQL endpoints known to the registry.
	 * @param registry
	 */
	public TripleSampler(SPARQLRegistry registry) {
		setRegistry(registry);
		setEndpoint(null);
	}
	
	/**
	 * Use this constructor to sample triples from the given SPARQL endpoint.
	 * @param endpoint
	 */
	public TripleSampler(SPARQLEndpoint endpoint) {
		setRegistry(null);
		setEndpoint(endpoint);
	}

	public SPARQLRegistry getRegistry() {
		return registry;
	}

	public void setRegistry(SPARQLRegistry registry) {
		this.registry = registry;
	}

	public SPARQLEndpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(SPARQLEndpoint endpoint) {
		this.endpoint = endpoint;
	}
	
	public Triple getSample(Triple triplePattern) throws SADIException, IOException, NoSampleAvailableException, ExceededMaxAttemptsException
	{
		if(getRegistry() != null) {
			return getSampleFromAnyEndpoint(triplePattern);
		} else {
			return getSampleFromEndpoint(getEndpoint(), triplePattern);
		}
	}
	
	public Triple getSampleFromEndpoint(SPARQLEndpoint endpoint, Triple triplePattern) throws SADIException, IOException, NoSampleAvailableException 
	{
		long upperSampleLimit = getUpperSampleLimit(endpoint, triplePattern);

		if(upperSampleLimit == 0) {
			throw new NoSampleAvailableException(String.format("no matching triples in %s", endpoint));
		}
		
		RandomDataImpl generator = new RandomDataImpl();
		long sampleIndex = upperSampleLimit > 1 ? generator.nextLong(0, upperSampleLimit - 1) : 0;
		
		return getSampleFromEndpoint(endpoint, triplePattern, sampleIndex);
	}
	
	public Triple getSampleFromAnyEndpoint(Triple triplePattern) throws SADIException, IOException, NoSampleAvailableException, ExceededMaxAttemptsException
	{
		List<SPARQLEndpoint> endpoints = new ArrayList<SPARQLEndpoint>(getRegistry().findEndpointsByTriplePattern(triplePattern));
		
		RandomDataImpl generator = new RandomDataImpl();
		int attempts = 0;
		SPARQLEndpoint endpoint = null;
		
		while (attempts < MAX_ATTEMPTS) {

			if (endpoints.size() == 0)
				throw new NoSampleAvailableException("there are no triples matching " + triplePattern + " in the data (without blank nodes)");

			int endpointIndex = endpoints.size() > 1 ? generator.nextInt(0, endpoints.size() - 1) : 0;
			endpoint = endpoints.get(endpointIndex);

			if(getRegistry().getServiceStatus(endpoint.getURI()) == ServiceStatus.DEAD) {
				endpoints.remove(endpointIndex);
				continue;
			}
			
			try {
				return getSampleFromEndpoint(endpoint, triplePattern);
			}
			catch(NoSampleAvailableException e) {
				log.warn(String.format("failed to retrieve sample from %s", endpoint), e);
				if(getRegistry().isWritable()) {
					getRegistry().setServiceStatus(endpoint.getURI(), ServiceStatus.DEAD);
				}
			}
			catch(IOException e) {
				log.warn(String.format("failed to retrieve sample from %s", endpoint), e);
				if(getRegistry().isWritable()) {
					getRegistry().setServiceStatus(endpoint.getURI(), ServiceStatus.DEAD);
				}
			}
			
			endpoints.remove(endpointIndex);
			attempts++;
		}

		throw new ExceededMaxAttemptsException("exceeded " + MAX_ATTEMPTS + " attempts when trying to retrieve triples matching " + triplePattern);
	}
	
	protected Triple getSampleFromEndpoint(SPARQLEndpoint endpoint, Triple triplePattern, long sampleIndex) throws IOException, NoSampleAvailableException
	{
		log.trace("retrieving triple #" + sampleIndex + " for " + triplePattern + " from " + endpoint.getURI());
 
		Query query = getConstructQuery(triplePattern); //SPARQLStringUtils.getConstructQuery(Collections.singletonList(triplePattern), Collections.singletonList(triplePattern));
		query.setOffset(sampleIndex);
		query.setLimit(1);
		
		log.trace(String.format("sample query: %s", query.serialize()));
		
		Collection<Triple> triples = endpoint.constructQuery(query.serialize());
		
		if (triples.size() == 0) {
			throw new RuntimeException("triple #" + sampleIndex + " doesn't exists in " + endpoint);
		}

		Triple triple = triples.iterator().next();

		// Sanity check. If the index is incomplete or out of date, the sample triple may not satisfy
		// the predicate list / regular expressions for the endpoint.  The simplest thing to do in 
		// this case is to fail and take another sample.

		if(getRegistry() != null && !getRegistry().findEndpointsByTriplePattern(triple).contains(endpoint)) {
			throw new NoSampleAvailableException("sample triple does not match the regular expressions for " + endpoint.getURI() + " (from which it was sampled!)");
		}

		return triple;
	}
	
	protected Query getConstructQuery(Triple triplePattern) {

		Query constructQuery = new Query();
		constructQuery.setQueryConstructType();
		
		TemplateGroup constructTemplate = new TemplateGroup();
		constructTemplate.addTriple(triplePattern);

		constructQuery.setConstructTemplate(constructTemplate);
		constructQuery.setQueryPattern(getWhereClauseWithBlankNodeFilter(triplePattern));		

		return constructQuery;
	}
	
	protected ElementGroup getWhereClauseWithBlankNodeFilter(Triple triplePattern) {

		Node s = triplePattern.getSubject();
		Node o = triplePattern.getObject();

		ElementGroup whereClause = new ElementGroup();
		whereClause.addTriplePattern(triplePattern);
		if(s.isVariable()) {
			whereClause.addElementFilter(new ElementFilter(new E_LogicalNot(new E_IsBlank(new ExprVar(s)))));
		} 
		if(o.isVariable()) {
			whereClause.addElementFilter(new ElementFilter(new E_LogicalNot(new E_IsBlank(new ExprVar(o)))));
		}
		
		return whereClause;
	}
	
	protected long getUpperSampleLimit(SPARQLEndpoint endpoint, Triple triplePattern) throws SADIException, IOException
	{
		String uri = endpoint.getURI();
		
		log.trace("determining number of triples matching " + triplePattern + " in " + uri);

		ServiceStatus status = ServiceStatus.OK;
		if(getRegistry() != null) {
			status = getRegistry().getServiceStatus(endpoint.getURI());
		}
		
		// check for a cached value first
		if(upperSampleLimitCache.contains(endpoint, triplePattern)) {
			log.trace("using previously cached value for upper sample limit");
			return upperSampleLimitCache.get(endpoint, triplePattern);
		}

		

		ElementGroup whereClause = getWhereClauseWithBlankNodeFilter(triplePattern);
		
		/*
		Node s = triplePattern.getSubject();
		Node o = triplePattern.getObject();

		// Build a Jena representation of the WHERE clause
		
		ElementGroup whereClause = new ElementGroup();
		whereClause.addTriplePattern(triplePattern);
		if(s.isVariable()) {
			whereClause.addElementFilter(new ElementFilter(new E_LogicalNot(new E_IsBlank(new ExprVar(s)))));
		} 
		if(o.isVariable()) {
			whereClause.addElementFilter(new ElementFilter(new E_LogicalNot(new E_IsBlank(new ExprVar(o)))));
		}
		*/
		
		if (status != ServiceStatus.SLOW) {
			try {
				// issue a SELECT COUNT(*) query
				Query countStarQuery = new Query();
				countStarQuery.setQuerySelectType();
				countStarQuery.setQueryPattern(whereClause);
				countStarQuery.addResultVar(countStarQuery.allocAggregate(AggCount.get()));
				
				List<Map<String, String>> results = endpoint.selectQuery(countStarQuery.serialize());
				

				Map<String, String> firstRow = results.iterator().next();
				String firstColumn = firstRow.keySet().iterator().next();
				long limit = Long.parseLong(firstRow.get(firstColumn));


				upperSampleLimitCache.put(endpoint, triplePattern, limit);
				log.trace(String.format("successful upper limit query: %s", countStarQuery.serialize()));
				log.trace(String.format("upper limit: %d", limit));
				return limit;
			}
			catch (IOException e) {
				log.warn("failed to COUNT number of triples matching " + triplePattern + " in " + uri + ", trying for a lower bound instead.");
			}
		}
		
		Query selectStarQuery = new Query();
		selectStarQuery.setQuerySelectType();
		selectStarQuery.setQueryPattern(whereClause);
		selectStarQuery.setQueryResultStar(true);

		long limit = endpoint.getResultsCountLowerBound(selectStarQuery.serialize(), 50000);
		upperSampleLimitCache.put(endpoint, triplePattern, limit);

		log.trace(String.format("successful upper limit query: %s", selectStarQuery.serialize()));
		log.trace(String.format("upper limit: %d", limit));
		
		return limit;
	}
	
	protected static class UpperSampleLimitCache {
		
		// endpoint => triple pattern => upper sample limit
		protected Map<SPARQLEndpoint,Map<Triple,Long>> cache = new HashMap<SPARQLEndpoint,Map<Triple,Long>>();

		public void put(SPARQLEndpoint endpoint, Triple triplePattern, long upperSampleLimit) 
		{
			if(!cache.containsKey(endpoint)) {
				cache.put(endpoint, new HashMap<Triple,Long>());
			}
			cache.get(endpoint).put(standardizeVarNames(triplePattern), upperSampleLimit);
		}
		public Long get(SPARQLEndpoint endpoint, Triple triplePattern) {
			return cache.get(endpoint).get(standardizeVarNames(triplePattern));
		}
		public boolean contains(SPARQLEndpoint endpoint, Triple triplePattern) {
			if(cache.containsKey(endpoint)) {
				return cache.get(endpoint).containsKey(standardizeVarNames(triplePattern));
			}
			return false;
		}
		
		/**
		 * Return an equivalent triple pattern with standardized variable names (?s, ?p, ?o).
		 * This method is needed to ensure that functionally equivalent triple patterns
		 * do not get assigned to different entries in the cache.
		 * @param triplePattern
		 * @return a Triple that is equivalent to triplePattern, but with standard variable names
		 */
		protected Triple standardizeVarNames(Triple triplePattern) 
		{
			Node s = triplePattern.getSubject();
			Node p = triplePattern.getPredicate();
			Node o = triplePattern.getObject();

			// make the key consistent, regardless of user's chosen variable names
			if(s.isVariable()) {
				s = NodeCreateUtils.create("?s");
			} 
			if(p.isVariable()) {
				p = NodeCreateUtils.create("?p");
			}
			if(o.isVariable()) {
				o = NodeCreateUtils.create("?o");
			}
			return new Triple(s, p, o);
		}
	}
}