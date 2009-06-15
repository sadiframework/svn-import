package ca.wilkinsonlab.sadi.biomoby;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomoby.shared.MobyData;
import org.biomoby.shared.MobyNamespace;
import org.biomoby.shared.MobyPrimaryData;
import org.biomoby.shared.MobyService;
import org.biomoby.shared.data.MobyContentInstance;
import org.biomoby.shared.data.MobyDataInstance;
import org.biomoby.shared.data.MobyDataJob;
import org.biomoby.shared.data.MobyDataObject;

import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class BioMobyService extends MobyService implements Service
{
	private static final Log log = LogFactory.getLog(BioMobyService.class);
	
	/* Maps article name => argument
	 * 
	 * The parent MobyService object uses Vectors to store
	 * inputs and outputs instead, which makes it necessary 
	 * to scan through the Vector to find an argument by name. 
	 *
	 * When the addInput/addInput methods are called, I add the new
	 * argument to the appropriate Vector as well as my map, 
	 * just so I don't break anything in the original 
	 * MobyService code.
	 */
	private Map<String, MobyData> inputMap;
	private Map<String, MobyData> outputMap;
	
	/* A "service predicate" describes the relationship
	 * between one input argument and one output argument 
	 * of a web service.  (Note: Both the input and the output
	 * may be a Moby Collection - that is, a set of two 
	 * or more Moby objects of the same Moby datatype.)
	 *
	 * Example: A service that takes the NCBI GI identifier
	 * for a protein as input, and returns a collection of 
	 * corresponding GO terms would have a "hasGOTerm" predicate 
	 * linking the input (gene id) to the output (collection 
	 * of GO terms).
	 *  
	 * Service predicates are used to translate
	 * predicates appearing SPARQL queries into corresponding
	 * web service invocations.
	 *
	 * We assume that no web service has the same predicate
	 * connecting more than one input/output pair. (If this
	 * does occur for some reason, only one such input/output 
	 * pair is stored.)
	 */
	private Map<String, String> predicateInputMap; // Maps predicate => input article name
	private Map<String, String> predicateOutputMap; // Maps predicate => output article name
	
	private ConstructQueryCache constructQueryCache;
	
	private BioMobyRegistry sourceRegistry;
	
	private OntClass inputClass;
	private OntClass outputClass;
	
	BioMobyService()
	{
		super();
		
		inputMap = new HashMap<String,MobyData>();
		outputMap = new HashMap<String,MobyData>();
		predicateInputMap = new HashMap<String, String>();
		predicateOutputMap = new HashMap<String, String>();
		constructQueryCache = new ConstructQueryCache();
	}
	
	void setSourceRegistry(BioMobyRegistry sourceRegistry)
	{
		this.sourceRegistry = sourceRegistry;
	}
	
	public String toString()
	{
		return BioMobyHelper.serviceToString(this);
	}
	
	public void addInput(String name, MobyData input)
	{
		// Add to the map.
		inputMap.put(name, input);
		// Add to the original Vector (see comment above).
		super.addInput(input);
	}
	
	public MobyData getInput(String name)
	{
		return inputMap.get(name);
	}
	
	public void addOutput(String name, MobyData output)
	{
		// Add to the map.
		outputMap.put(name, output);
		// Add to the original Vector (see comment above).
		super.addOutput(output);
	}

	public MobyData getOutput(String name)
	{
		return outputMap.get(name);
	}
	
	/**
	 * Record a service predicate relationship between 
	 * an input argument and an output argument. 
	 */
	public void addPredicate(String predicateURI, String inputArticleName, String outputArticleName)
	{
		predicateInputMap.put(predicateURI, inputArticleName);
		predicateOutputMap.put(predicateURI, outputArticleName);
	}
	
	/**
	 * Get the input argument for a given service predicate.
	 * Return the article name of the input argument, or
	 * null if this service isn't capable of generating 
	 * the given predicate.
	 */
	public String getInputForPredicate(String predicateURI)
	{
		return predicateInputMap.get(predicateURI);
	}
	
	/**
	 * Get the output argument for a given service predicate.
	 * Return the article name of the output argument, or
	 * null if this service isn't capable of generating 
	 * the given predicate.
	 */  
	public String getOutputForPredicate(String predicateURI)
	{
		return predicateOutputMap.get(predicateURI);
	}
	
	/**
	 * Return the list of predicates generated by the service.
	 */
	public List<String> getPredicates()
	{
		// TODO is there a reason this must be a List?
		return new ArrayList<String>(predicateInputMap.keySet());
	}
	
	/**
	 * Returns the predicates associated with the specified output article.
	 * @param outputArticle the output article of interest
	 * @return the predicate associated with the specified output article,
	 *         or null if the output article doesn't exist
	 */
	public Collection<String> getPredicatesForOutput(String outputArticle)
	{
		/* TODO figure out a way to organize the object data so this doesn't
		 * require iteration...
		 */
		Collection<String> results = new ArrayList<String>();
		for (String predicate: predicateOutputMap.keySet()) {
			if (predicateOutputMap.get(predicate).equals(outputArticle))
				results.add(predicate);
		}
		return results;
	}

	/**
	 * Get the number of primary (non-parameter) inputs to this
	 * service.
	 */
	public int getNumPrimaryInputs()
	{
		MobyPrimaryData[] inputs = this.getPrimaryInputs();
		int numInputs = 0;
		if (inputs != null)
			numInputs = inputs.length;
		
		// TODO if this is really important, use logging, please...
//		if (numInputs == 0)
//			System.err.println("Warning: Service " + this.getName() + " has no inputs!");
		
		return numInputs;
	}

	/**
	 * Get the number of outputs for this service.  (The word "primary"
	 * is actually meaningless for outputs, because there is only
	 * one kind of output.)
	 */
	public int getNumPrimaryOutputs()
	{
		MobyPrimaryData[] outputs = this.getPrimaryOutputs();
		int numOutputs = 0;
		if(outputs != null)
			numOutputs = outputs.length;

		// TODO if this is really important, use logging, please...
//		if(numOutputs== 0)
//			System.err.println("Warning: Service " + this.getName() + " has no inputs!");
		
		return numOutputs;
	}

	public String getServiceURI()
	{
		// TODO this works now, but there must be a better way...
		return toString();
	}
	
	public Collection<Triple> invokeService(String inputURI, Map<String, String> secondaryParameters)
	throws Exception
	{
		Collection<Triple> triples = new ArrayList<Triple>();
		MobyDataObject input = BioMobyHelper.convertUriToMobyDataObject(inputURI);
		/* TODO don't call the service if it doesn't take this input namespace;
		 * eventually we won't need this here because the registry query should
		 * take the input into account when finding appropriate services...
		 */
		MobyContentInstance response = BioMobyHelper.callService(this, input, secondaryParameters);
		convertMobyContentInstanceToRdf(response, inputURI, triples);
		return triples;
	}
	
	public Collection<Triple> invokeService(String inputURI) throws Exception
	{
		Collection<Triple> triples = new ArrayList<Triple>();
		MobyDataObject input = BioMobyHelper.convertUriToMobyDataObject(inputURI);
		/* TODO don't call the service if it doesn't take this input namespace;
		 * eventually we won't need this here because the registry query should
		 * take the input into account when finding appropriate services...
		 */
		MobyContentInstance response = BioMobyHelper.callService(this, input);
		convertMobyContentInstanceToRdf(response, inputURI, triples);
		return triples;
	}
	
	public Collection<Triple> invokeService(String inputURI, String predicate) throws Exception
	{
		Collection<Triple> results = invokeService(inputURI);
		Collection<Triple> filtered = new ArrayList<Triple>();
		for (Triple triple : results) {
			if (triple.getPredicate().toString().equals(predicate))
				filtered.add(triple);
		}
		return filtered;
	}
	
	private void convertMobyContentInstanceToRdf(MobyContentInstance response, String subject, Collection<Triple> accum)
	{
		for (MobyDataJob job: response.values()) {
			// if there's only one output, convert that (service might not set the article name)
			// if there's more than one, convert only the article we're interested in
			if (job.size() == 1) {
				/* if there's only one output, the service might not set the article name,
				 * but all predicates will map to the one output, so just iterate over them...
				 */
				MobyDataInstance output = (MobyDataInstance)job.values().iterator().next();
				for (String predicate: predicateOutputMap.keySet())
					convertMobyDataInstanceToRdf(output, subject, predicate, accum);
			} else {
				for (String article: job.keySet()) {
					MobyDataInstance output = job.get(article);
					for (String predicate: getPredicatesForOutput(article))
						convertMobyDataInstanceToRdf(output, subject, predicate, accum);
				}
			}
		}
	}

	private void convertMobyDataInstanceToRdf(MobyDataInstance outer, String subject, String predicate, Collection<Triple> accum)
	{
		Object inner = outer.getObject();
		if (outer instanceof MobyDataObject) { // output object is Simple
			MobyDataObject data = (MobyDataObject)outer;
			convertMobyDataObjectToRdf(data, subject, predicate, accum);
		} else if (inner instanceof Collection) { // output object is Collection
			for (Object o: (Collection)inner) {
				MobyDataObject data = (MobyDataObject)o;
				convertMobyDataObjectToRdf(data, subject, predicate, accum);
			}
		} else {
			log.warn("MobyDataInstance contained an unhandled " + inner.getClass());
		}
	}

	private String getConstructQuery(MobyDataObject output, String predicate)
	{
		String outputTypeURI = BioMobyHelper.MOBY_DATATYPE_PREFIX + output.getDataType().getName();
		return constructQueryCache.getQuery(predicate, outputTypeURI);
	}

	private void convertMobyDataObjectToRdf(MobyDataObject output, String subject, String predicate, Collection<Triple> accum)
	{
		if (BioMobyHelper.isPrimitive(output.getDataType().getName())) {
			String typedObject = String.format("'%s'xsd:string", output.getValue());
			addAndLogTriple(subject, predicate, typedObject, accum);
			return;
		}

		String query = getConstructQuery(output, predicate);
		if (query == null) {
			/* If there isn't a query, just make a triple linking the URIs of the input
			 * and output objects.  (However, we're not allowed to do that if the property
			 * is a datatype property).  -- B.V.
			 */
			if (sourceRegistry.isDatatypeProperty(predicate)) {
				log.error(String.format("datatype property <%s> doesn't have a construct query", predicate));
				return;
			} else {
				/* TODO fix convertMobyDataObjectToUri to throw an exception if there's
				 * no id?
				 */
				if (!StringUtils.isEmpty(output.getId())) {
					String objectURI = BioMobyHelper.convertMobyDataObjectToUri(output);
					addAndLogTriple(subject, predicate, objectURI, accum);
				}
			}
		} else {
			try {
				query = SPARQLStringUtils.strFromTemplate(query, subject, predicate);
				String rdfXml = BioMobyHelper.convertMobyDataObjectToRdf(output);
				Model model = runConstructQuery(query, rdfXml);
				for (StmtIterator i = model.listStatements(); i.hasNext();) {
					addAndLogTriple(i.nextStatement().asTriple(), accum);
				}
			} catch (TransformerException e) {
				log.error("failed to convert MobyDataObject to RDF", e);
			} catch (URIException e) {
				log.error(String.format("failed to parse URI <%s>", subject), e);
			}
		}
	}
	
	private static void addAndLogTriple(String subject, String predicate, String object, Collection<Triple> accum)
	{
		Triple triple = new Triple(
				NodeCreateUtils.create(subject),
				NodeCreateUtils.create(predicate),
				NodeCreateUtils.create(object)
		);
		addAndLogTriple(triple, accum);
	}
	
	private static void addAndLogTriple(Triple triple, Collection<Triple> accum)
	{
		log.trace("retrieved triple " + triple);
		accum.add(triple);
	}	
	
	private static Model runConstructQuery( String sparql, String rdfXml )
	{
		Model model = ModelFactory.createDefaultModel();
		model.read(new StringReader( rdfXml ), "", "RDF/XML");
		
		Query query = QueryFactory.create(sparql) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
		Model resultModel = qexec.execConstruct() ;
		qexec.close() ;		
		
		return resultModel;
	}

	public Collection<Triple> invokeService(Resource inputNode) throws Exception
	{
		return invokeService(inputNode.getURI());
	}

	public Collection<Triple> invokeService(Resource inputNode, String predicate) throws Exception
	{
		return invokeService(inputNode.getURI(), predicate);
	}

	public boolean isInputInstance(Resource resource)
	{
		log.warn("isInputInstance not yet implemented");
		return false;
	}

	public Collection<Resource> discoverInputInstances(Model inputModel)
	{
		log.warn("discoverInputInstances not yet implemented");
		return new ArrayList<Resource>(0);
	}
	
	/**
	 * Returns an OntClass describing the input this service can consume.
	 * Any input to this service must be an instance of this class.
	 * @return an OntClass describing the input this service can consume
	 */
	public OntClass getInputClass()
	{
		if (inputClass == null) {
			if (getNumPrimaryInputs() > 1)
				throw new UnsupportedOperationException("this interface is invalid for BioMoby services with more than one primary input");
			
			RDFList namespaceTypes = sourceRegistry.getTypeOntology().createList();
			for (MobyNamespace namespace: getPrimaryInputs()[0].getNamespaces())
				namespaceTypes.add(sourceRegistry.getTypeByNamespace(namespace));
			
			inputClass = sourceRegistry.getTypeOntology().createUnionClass(getServiceURI() + "#input", namespaceTypes);
		}
		return inputClass;
	}
	
	/**
	 * Returns an OntClass describing the output this service produces.
	 * Any output from this service will be an instance of this class.
	 * @return an OntClass describing the output this service produces
	 */
	public OntClass getOutputClass()
	{
		log.warn("getOutputClass not yet implemented");
		return outputClass;
	}
	
	private class ConstructQueryCache
	{
		private final Object NO_HIT = new Object();
		
		private Map<String, Object> cache;
		
		public ConstructQueryCache()
		{
			cache = new HashMap<String, Object>();
		}
		
		public String getQuery(String predicate, String outputTypeURI)
		{
			String key = getHashKey(predicate, outputTypeURI);
			Object query = cache.get(key);
			if (query == null) {
				try {
					query = sourceRegistry.getConstructQueryForPredicate(predicate, outputTypeURI);
				} catch (IOException e) {
					log.error(String.format("error retrieving construct query for predicate <%s>", predicate), e);
				}
				if (query == null)
					query = NO_HIT;
				cache.put(key, query);
			}
			return query.equals(NO_HIT) ? null : (String)query;
		}
		
		private String getHashKey(String predicate, String outputTypeURI)
		{
			return String.format("%s:%s", predicate, outputTypeURI);
		}
	}
}
