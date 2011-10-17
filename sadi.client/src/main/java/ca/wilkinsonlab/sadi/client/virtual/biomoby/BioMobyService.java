package ca.wilkinsonlab.sadi.client.virtual.biomoby;

import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.biomoby.shared.MobyData;
import org.biomoby.shared.MobyNamespace;
import org.biomoby.shared.MobyPrimaryData;
import org.biomoby.shared.MobyService;
import org.biomoby.shared.data.MobyContentInstance;
import org.biomoby.shared.data.MobyDataInstance;
import org.biomoby.shared.data.MobyDataJob;
import org.biomoby.shared.data.MobyDataObject;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.ServiceBase;
import ca.wilkinsonlab.sadi.client.ServiceInvocationException;
import ca.wilkinsonlab.sadi.utils.LabelUtils;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class BioMobyService extends ServiceBase
{
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(BioMobyService.class);
	
	/* The wrapped Moby service.
	 */
	private MobyService mobyService;
	
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
	
	private BioMobyRegistry sourceRegistry;
	
	private OntClass inputClass;
	private OntClass outputClass;
	private Collection<Restriction> restrictions;
	
	BioMobyService(BioMobyRegistry sourceRegistry, MobyService mobyService)
	{
		super();
		
		this.sourceRegistry = sourceRegistry;
		this.mobyService = mobyService;
		inputMap = new HashMap<String,MobyData>();
		outputMap = new HashMap<String,MobyData>();
		predicateInputMap = new HashMap<String, String>();
		predicateOutputMap = new HashMap<String, String>();
		
		setName(mobyService.getName());
		setDescription(mobyService.getDescription());
		setServiceProvider(mobyService.getAuthority());
		setContactEmail(mobyService.getEmailContact());
		setAuthoritative(mobyService.isAuthoritative());
		setInputClassURI(getInputClass().getURI());
		setInputClassLabel(LabelUtils.getLabel(getInputClass()));
		setOutputClassURI(getOutputClass().getURI());
		setOutputClassLabel(LabelUtils.getLabel(getOutputClass()));
		// TODO restriction beans...
		// TODO try to express parameters as a class?
	}
	
	void setSourceRegistry(BioMobyRegistry sourceRegistry)
	{
		this.sourceRegistry = sourceRegistry;
	}
	
	public void addInput(String name, MobyData input)
	{
		// Add to the map.
		inputMap.put(name, input);
		// Add to the original Vector (see comment above).
//		super.addInput(input);
		mobyService.addInput(input);
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
//		super.addOutput(output);
		mobyService.addOutput(output);
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
		MobyPrimaryData[] inputs = mobyService.getPrimaryInputs();
		int numInputs = 0;
		if (inputs != null)
			numInputs = inputs.length;
		
		// FIXME if this is really important, use logging, please...
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
		MobyPrimaryData[] outputs = mobyService.getPrimaryOutputs();
		int numOutputs = 0;
		if(outputs != null)
			numOutputs = outputs.length;

		// TODO if this is really important, use logging, please...
//		if(numOutputs== 0)
//			System.err.println("Warning: Service " + this.getName() + " has no inputs!");
		
		return numOutputs;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getInputClass()
	 */
	@Override
	public OntClass getInputClass()
	{
		if (inputClass == null) {
			if (getNumPrimaryInputs() > 1) {
				log.warn("this interface is invalid for BioMoby services with more than one primary input");
				return null;
			} 

			MobyNamespace[] namespaces = mobyService.getPrimaryInputs()[0].getNamespaces();
			if (namespaces.length > 1) {
				inputClass = sourceRegistry.getUnionType(String.format("%s#inputClass", getURI()), namespaces);
			} else {
				inputClass = sourceRegistry.getTypeByNamespace(namespaces[0]);
			}
		}
		return inputClass;
	}	
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getOutputClass()
	 */
	@Override
	public OntClass getOutputClass()
	{
		log.warn("getOutputClass not yet implemented");
		if (outputClass == null) {
			String outputClassUri = getURI() + "#output";
			outputClass = sourceRegistry.getTypeOntology().getOntClass(outputClassUri);
			if (outputClass == null) {
				outputClass = sourceRegistry.getTypeOntology().createClass(outputClassUri);
			}
		}
		return outputClass;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#getRestrictions()
	 */
	@Override
	public Collection<Restriction> getRestrictions() throws SADIException
	{
		if (restrictions == null) {
			restrictions = OwlUtils.listRestrictions(getOutputClass(), getInputClass());
		}
		return restrictions;
	}
	
//	public Collection<Triple> invokeService(String inputURI, Map<String, String> secondaryParameters)
//	throws ServiceInvocationException
//	{
//		Collection<Triple> triples = new ArrayList<Triple>();
//	
//		MobyDataObject input;
//		try {
//			input = sourceRegistry.convertUriToMobyDataObject(inputURI);
//		} catch (URISyntaxException e) {
//			throw new ServiceInvocationException(String.format("unable to convert input URI to MobyDataObject: %s", e.getMessage()));
//		}
//		
//		/* TODO don't call the service if it doesn't take this input namespace;
//		 * eventually we won't need this here because the registry query should
//		 * take the input into account when finding appropriate services...
//		 */
//		
//		MobyContentInstance response;
//		try {
//			response = BioMobyHelper.callService(this, input, secondaryParameters);
//		} catch (Exception e) {
//			throw new ServiceInvocationException(String.format("failed to invoke BioMoby service: %s", e.getMessage()));
//		}
//		
//		convertMobyContentInstanceToRdf(response, inputURI, triples);
//		return triples;
//	}
//	
//	public Collection<Triple> invokeService(String inputURI) throws ServiceInvocationException
//	{
//		Collection<Triple> triples = new ArrayList<Triple>();
//		
//		MobyDataObject input;
//		try {
//			input = sourceRegistry.convertUriToMobyDataObject(inputURI);
//		} catch (URISyntaxException e) {
//			throw new ServiceInvocationException(String.format("unable to convert input URI to MobyDataObject: %s", e.getMessage()));
//		}
//		
//		/* TODO don't call the service if it doesn't take this input namespace;
//		 * eventually we won't need this here because the registry query should
//		 * take the input into account when finding appropriate services...
//		 */
//		
//		MobyContentInstance response;
//		try {
//			response = BioMobyHelper.callService(this, input);
//		} catch (Exception e) {
//			throw new ServiceInvocationException(String.format("failed to invoke BioMoby service: %s", e.getMessage()));
//		}
//		
//		convertMobyContentInstanceToRdf(response, inputURI, triples);
//		return triples;
//	}
//	
//	public Collection<Triple> invokeService(String inputURI, String predicate) throws ServiceInvocationException
//	{
//		Collection<Triple> results = invokeService(inputURI);
//		Collection<Triple> filtered = new ArrayList<Triple>();
//		for (Triple triple : results) {
//			if (triple.getPredicate().toString().equals(predicate))
//				filtered.add(triple);
//		}
//		return filtered;
//	}
//	/**
//	 * 
//	 * @param inputNode
//	 * @param predicate
//	 * @return
//	 * @throws ServiceInvocationException
//	 * @deprecated
//	 */
//	public Collection<Triple> invokeService(Resource inputNode, String predicate) throws ServiceInvocationException
//	{
//		return filterByPredicate(invokeService(inputNode), predicate);
//	}
//
//	/**
//	 * 
//	 * @param inputNode
//	 * @param predicate
//	 * @param secondaryParameters
//	 * @return
//	 * @throws ServiceInvocationException
//	 * @deprecated
//	 */
//	public Collection<Triple> invokeService(Resource inputNode, String predicate, Map<String, String> secondaryParameters) throws ServiceInvocationException
//	{
//		return filterByPredicate(invokeService(inputNode, secondaryParameters), predicate);
//	}

	/**
	 * 
	 * @param inputNodes
	 * @param secondaryParameters
	 * @return
	 * @throws ServiceInvocationException
	 */
	public Model invokeService(Iterator<Resource> inputNodes, Map<String, String> secondaryParameters) throws ServiceInvocationException
	{
		MobyContentInstance contentInstance = new MobyContentInstance();
		while (inputNodes.hasNext()) {
			Resource inputNode = inputNodes.next();
			MobyDataObject input;
			try {
				input = sourceRegistry.convertUriToMobyDataObject(inputNode.getURI());
			} catch (URISyntaxException e) {
				throw new ServiceInvocationException(String.format("unable to convert input URI %s to MobyDataObject: %s", inputNode.getURI(), e.getMessage()));
			}
			
			MobyDataJob job = new MobyDataJob();
			job.put(mobyService.getPrimaryInputs()[0].getName(), input); // TODO make sure this isn't null...
			job.setID(inputNode.getURI());
			contentInstance.put(inputNode.getURI(), job);
		}
		
		MobyContentInstance response;
		try {
			response = BioMobyHelper.callService(mobyService, contentInstance, secondaryParameters);
		} catch (Exception e) {
			throw new ServiceInvocationException(String.format("failed to invoke BioMoby service: %s", e.getMessage()), e);
		}
		
		Model result = ModelFactory.createDefaultModel();
		convertMobyContentInstanceToRdf(response, result);
		return result;
	}

//	/**
//	 * 
//	 * @param inputNodes
//	 * @param predicate
//	 * @return
//	 * @throws ServiceInvocationException
//	 * @deprecated
//	 */
//	public Collection<Triple> invokeService(Collection<Resource> inputNodes, String predicate) throws ServiceInvocationException
//	{
//		return filterByPredicate(invokeService(inputNodes), predicate);
//	}
//	
//	/**
//	 * 
//	 * @param inputNodes
//	 * @param predicate
//	 * @param secondaryParameters
//	 * @return
//	 * @throws ServiceInvocationException
//	 * @deprecated
//	 */
//	public Collection<Triple> invokeService(Collection<Resource> inputNodes, String predicate, Map<String, String> secondaryParameters) throws ServiceInvocationException
//	{
//		return filterByPredicate(invokeService(inputNodes, secondaryParameters), predicate);
//	}
	
	/* Convert a MobyContentInstance to RDF, where the ID of each job in the
	 * instance is the URI of the input submitted in that job.  The converted
	 * RDF is added (in triple form) to the specified accumulator.
	 */
	private void convertMobyContentInstanceToRdf(MobyContentInstance response, Model accum)
	{
		for (MobyDataJob job: response.values()) {
			/* we stored the URI of the input as the job ID;
			 * hopefully it's preserved...
			 */
			String id = job.getID();
			Resource subject = accum.getResource(id);
			
			/* if there's only one output, convert that (service might not set the article name);
			 * if there's more than one, convert only the article we're interested in...
			 */
			if (job.size() == 1) {
				/* if there's only one output, the service might not set the article name,
				 * but all predicates will map to the one output, so just iterate over them...
				 */
				MobyDataInstance output = (MobyDataInstance)job.values().iterator().next();
				for (String p: predicateOutputMap.keySet()) {
					Property predicate = accum.getProperty(p);
					convertMobyDataInstanceToRdf(output, subject, predicate, accum);
				}
			} else {
				for (String article: job.keySet()) {
					MobyDataInstance output = job.get(article);
					for (String p: getPredicatesForOutput(article)) {
						Property predicate = accum.getProperty(p);
						convertMobyDataInstanceToRdf(output, subject, predicate, accum);
					}
				}
			}
		}
	}

	/* Convert a MobyDataInstance to RDF using the specified subject and predicate.
	 * The converted RDF is added (in triple form) to the specified accumulator.
	 */
	private void convertMobyDataInstanceToRdf(MobyDataInstance outer, Resource subject, Property predicate, Model accum)
	{
		Object inner = outer.getObject();
		if (outer instanceof MobyDataObject) { // output object is Simple
			MobyDataObject data = (MobyDataObject)outer;
			convertMobyDataObjectToRdf(data, subject, predicate, accum);
		} else if (inner instanceof Collection<?>) { // output object is Collection
			for (Object o: (Collection<?>)inner) {
				MobyDataObject data = (MobyDataObject)o;
				convertMobyDataObjectToRdf(data, subject, predicate, accum);
			}
		} else {
			log.warn("MobyDataInstance contained an unhandled " + inner.getClass());
		}
	}

	/* Convert a MobyDataObject to RDF using the specified subject and predicate.
	 * The converted RDF is added (in triple form) to the specified accumulator.
	 */
	private void convertMobyDataObjectToRdf(MobyDataObject output, Resource subject, Property predicate, Model accum)
	{
		if (BioMobyHelper.isPrimitive(output.getDataType().getName())) {
			accum.add(subject, predicate, output.getValue());
			return;
		}

		String query = getConstructQuery(output, predicate);
		if (query == null) {
			/* If there isn't a query, just make a triple linking the URIs of the input
			 * and output objects.  (However, we're not allowed to do that if the property
			 * is a datatype property).  -- B.V.
			 */
			if (sourceRegistry.isDatatypeProperty(predicate.getURI())) {
				log.error(String.format("datatype property <%s> doesn't have a construct query", predicate));
				return;
			} else {
				/* TODO fix convertMobyDataObjectToUri to throw an exception if there's
				 * no id?
				 */
				if (!StringUtils.isEmpty(output.getId())) {
					String objectURI = sourceRegistry.convertMobyDataObjectToUri(output);
					accum.add(subject, predicate, accum.createResource(objectURI));
				}
			}
		} else {
			try {
				query = SPARQLStringUtils.strFromTemplate(query, subject.getURI(), predicate.getURI());
				String rdfXml = BioMobyHelper.convertMobyDataObjectToRdf(output);
				Model model = runConstructQuery(query, rdfXml);
				accum.add(model);
				model.close();
			} catch (TransformerException e) {
				log.error("failed to convert MobyDataObject to RDF", e);
			} catch (IllegalArgumentException e) {
				log.error(String.format("failed to parse URI <%s>", subject), e);
			}
		}
	}

	private String getConstructQuery(MobyDataObject output, Property predicate)
	{
		String outputTypeURI = BioMobyHelper.MOBY_DATATYPE_PREFIX + output.getDataType().getName();
		try {
			return sourceRegistry.getConstructQueryForPredicate(predicate.getURI(), outputTypeURI);
		} catch (SADIException e) {
			log.error(String.format("error fetching construct query for %s/%s", predicate.getURI(), outputTypeURI), e);
			return null;
		}
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

//	/* Filter a collection of triples to pass only those with the
//	 * specified predicate.
//	 */
//	private Collection<Triple> filterByPredicate(Model results, String predicate)
//	{
//		Collection<Triple> filtered = new ArrayList<Triple>();
//		StmtIterator i = results.listStatements(null, results.getProperty(predicate), (RDFNode)null);
//		while (i.hasNext()) {
//			filtered.add(i.nextStatement().asTriple());
//		}
//		i.close();
//		return filtered;
//	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#isInputInstance(com.hp.hpl.jena.rdf.model.Resource)
	 */
	public boolean isInputInstance(Resource resource)
	{
		/* TODO this can maybe be done better now that the input class is
		 * properly created...
		 */
		MobyNamespace[] namespaces = mobyService.getPrimaryInputs()[0].getNamespaces();
		for (MobyNamespace namespace: namespaces) {
			OntClass namespaceType = sourceRegistry.getTypeByNamespace(namespace);
			if (resource.hasProperty(RDF.type, namespaceType))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#discoverInputInstances(com.hp.hpl.jena.rdf.model.Model)
	 */
	public Collection<Resource> discoverInputInstances(Model inputModel)
	{
		/* TODO this can maybe be done better now that the input class is
		 * properly created...
		 */
		Collection<Resource> instances = new ArrayList<Resource>();
		MobyNamespace[] namespaces = mobyService.getPrimaryInputs()[0].getNamespaces();
		for (MobyNamespace namespace: namespaces) {
			OntClass namespaceType = sourceRegistry.getTypeByNamespace(namespace);
			instances.addAll(inputModel.listResourcesWithProperty(RDF.type, namespaceType).toList());
		}
		return instances;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(java.util.Iterator)
	 */
	@Override
	public Model invokeService(Iterator<Resource> inputNodes) throws ServiceInvocationException
	{
		return invokeService(inputNodes, BioMobyHelper.EMPTY_PARAMETER_MAP);
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.ServiceBase#getLog()
	 */
	@Override
	protected Logger getLog()
	{
		return log;
	}
}
