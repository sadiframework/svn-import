package ca.wilkinsonlab.sadi.rdf;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceInvocationException;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper;
import ca.wilkinsonlab.sadi.utils.DurationUtils;
import ca.wilkinsonlab.sadi.utils.ExceptionUtils;
import ca.wilkinsonlab.sadi.utils.HttpUtils;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpInputStream;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

public class RdfService implements Service
{
	private static final Log log = LogFactory.getLog(RdfService.class);
	
	private static final ModelMaker modelMaker = ModelFactory.createMemModelMaker();
	
	String serviceUri;
	URL serviceUrl;
	RdfRegistry sourceRegistry;
	String name;
	String description;
	Model model;
	OntModel ontModel;
	OntClass inputClass;
	OntClass outputClass;
	Collection<String> predicates;
	
	/**
	 * Construct a new RdfService from the service description located at
	 * the specified URL.
	 * @param serviceURL the service URL
	 * @throws MalformedURLException
	 */
	public RdfService(String serviceURL) throws MalformedURLException
	{
		this.serviceUri = serviceURL;
		this.serviceUrl = new URL(serviceURL);
	}
	
	/**
	 * Construct a new RdfService from a service URI, an input class
	 * and an output class. This method should only be used by the
	 * unit tests.
	 * @param serviceUri the service URI
	 * @param inputClassUri the input class URI
	 * @param outputClassUri the output class URI
	 */
	RdfService(String serviceUri, String inputClassUri, String outputClassUri)
	{
		model = modelMaker.createFreshModel();
		ServiceOntologyHelper helper = new MyGridServiceOntologyHelper(model, serviceUri);
		helper.setInputClass(inputClassUri);
		helper.setOutputClass(outputClassUri);
		
		createOntologyModel(helper);
	}

	private boolean isInitialized()
	{
		return model != null;
	}
	
	private void fetchServiceModel()
	{
		log.debug("fetching service model from " + getServiceURL());
		
		/* TODO is this the best spec here?
		 * TODO is this the best import strategy here?
		 */
		model = modelMaker.createFreshModel();
		model.read(getServiceURL().toExternalForm());
		Resource serviceRoot = model.getResource(getServiceURI());
		ServiceOntologyHelper helper = new MyGridServiceOntologyHelper(serviceRoot);
		name = helper.getName();
		description = helper.getDescription();
		
		createOntologyModel(helper);
	}
	
	private void createOntologyModel(ServiceOntologyHelper helper)
	{
		ontModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		inputClass = ontModel.createClass( helper.getInputClass().getURI() );
		OwlUtils.loadOntologyForUri(ontModel, inputClass.getURI());
		outputClass = ontModel.createClass( helper.getOutputClass().getURI() );
		OwlUtils.loadOntologyForUri(ontModel, outputClass.getURI());
	}
	
	/**
	 * Fetch asynchronous data from the specified URL.
	 * This method will block until the final data is available from the URL, waiting for
	 * the amount of time suggested by the service between redirects.
	 * @param url the URL from which to fetch data
	 * @return an InputStream
	 * @throws HttpException
	 * @throws IOException
	 */
	private static InputStream fetchAsyncData(String url) throws HttpException, IOException
	{
		while (true) {
			log.debug("fetching asynchronous data from " + url);
			GetMethod method = new GetMethod(url);
			method.setFollowRedirects(false);
			HttpClient client = new HttpClient();
			int statusCode = client.executeMethod(method);
			if (statusCode >= 300 && statusCode < 400) {
				long toSleep = 2000; // sleep for two seconds by default
				for (HeaderElement element: method.getResponseHeader("Pragma").getElements()) {
					if (element.getName().equals(SADI.ASYNC_HEADER)) {
						try {
							toSleep = DurationUtils.parse(element.getValue());
						} catch (NumberFormatException e) {
							log.error(String.format("error fetching asynchronous data from %s", url), e);
						}
					}
				}
				try {
					log.trace("sleeping " + toSleep + "ms before following redirect");
					Thread.sleep(toSleep);
				} catch (InterruptedException e) {
					log.warn(e);
				}
			} else if (statusCode == HttpStatus.SC_OK) {
				return new HttpInputStream(method.getResponseBodyAsStream(),method);
			} else {
				throw new HttpResponseCodeException(statusCode, method.getStatusLine().getReasonPhrase());
			}
		}
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getServiceURI()
     */
	public String getServiceURI()
	{
		return serviceUri;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getServiceURL()
     */
	public URL getServiceURL()
	{
		return serviceUrl;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getName()
     */
	public String getName()
	{
		if (!isInitialized())
			fetchServiceModel();
		
		return name;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getDescription()
     */
	public String getDescription()
	{
		if (!isInitialized())
			fetchServiceModel();
		
		return description;
	}
	
	private OntModel getServiceOntologyModel()
	{
		if (!isInitialized())
			fetchServiceModel();
		
		return ontModel;
	}
	
	/**
	 * Returns an OntClass describing the input this service can consume.
	 * Any input to this service must be an instance of this class.
	 * @return an OntClass describing the input this service can consume
	 */
	public OntClass getInputClass()
	{
		if (!isInitialized())
			fetchServiceModel();
		
		return inputClass;
	}
	
	/**
	 * Returns an OntClass describing the output this service produces.
	 * Any output from this service will be an instance of this class.
	 * @return an OntClass describing the output this service produces
	 */
	public OntClass getOutputClass()
	{
		if (!isInitialized())
			fetchServiceModel();
		
		return outputClass;
	}

	/**
	 * Returns the list of predicates this service attaches to its input.
	 * @return the list of predicates this service attaches to its input
	 */
	public Collection<String> getPredicates()
	{
		if (predicates == null) {
			Set<OntProperty> properties = OwlUtils.listRestrictedProperties(getOutputClass().getURI());
			Set<OntProperty> inputProperties = OwlUtils.listRestrictedProperties(getInputClass().getURI());
			predicates = new ArrayList<String>(properties.size());
			for (OntProperty p: properties) {
				/* TODO will two equivalent OntProperties from different
				 * models return true here?
				 */
				if (!inputProperties.contains(p))
					predicates.add(p.getURI());
			}
		}
		return predicates;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource)
     */
	public Collection<Triple> invokeService(Resource inputNode) throws ServiceInvocationException
	{
		return invokeService(Collections.singletonList(inputNode));
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
     */
	public Collection<Triple> invokeService(Resource inputNode, String predicate)
	throws ServiceInvocationException
	{
		return filterByPredicate(invokeService(inputNode), predicate);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(java.util.Collection)
	 */
	public Collection<Triple> invokeService(Collection<Resource> inputNodes) throws ServiceInvocationException
	{
		Model inputModel = ModelFactory.createDefaultModel();
		for (Resource inputNode: inputNodes) {
			inputModel.add(OwlUtils.getMinimalModel(inputNode, getInputClass()));
		}
		
		Model outputModel = null;
		try {
			outputModel = invokeServiceUnparsed(inputModel);
		} catch (IOException e) {
			String message = null;
			if (outputModel != null)
				message = ExceptionUtils.exceptionModelToString(outputModel);
			if (StringUtils.isEmpty(message))
				message = e.getMessage();
			throw new ServiceInvocationException(message);
		}
		
		Collection<Triple> triples = new ArrayList<Triple>();
		for (StmtIterator i = outputModel.listStatements(); i.hasNext(); ) {
			Statement statement = i.nextStatement();
			triples.add(statement.asTriple());
		}
		return triples;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(java.util.Collection, java.lang.String)
	 */
	public Collection<Triple> invokeService(Collection<Resource> inputNodes, String predicate) throws ServiceInvocationException
	{
		return filterByPredicate(invokeService(inputNodes), predicate);
	}

	/* Filter a collection of triples to pass only those with the
	 * specified predicate.
	 */
	private Collection<Triple> filterByPredicate(Collection<Triple> results, String predicate)
	{
		Collection<Triple> filteredTriples = new ArrayList<Triple>();
		for (Triple triple: results) {
			if (triple.getPredicate().getURI().equals(predicate))
				filteredTriples.add(triple);
		}
		return filteredTriples;
		
	}

	/**
	 * Call this service using the specified Model as input, without
	 * any extra parsing/filtering.
	 * @param inputModel the input data
	 * @return the service output
	 * @throws IOException 
	 */
	public Model invokeServiceUnparsed(Model inputModel) throws IOException
	{
		InputStream is = HttpUtils.postToURL(getServiceURL(), inputModel);
		Model model = modelMaker.createFreshModel();
		model.read(is, "");
		is.close();
		
		/* resolve any rdfs:isDefinedBy URIs to fetch asynchronous data...
		 */
		resolveAsynchronousData(model);
		
		return model;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#isInputInstance(com.hp.hpl.jena.rdf.model.Resource)
     */
	public boolean isInputInstance(Resource resource)
	{
		Model inputModel = resource.getModel();
		OntModel reasoningModel = getServiceOntologyModel();
		OntClass inputClass = getInputClass();
		try {
			reasoningModel.addSubModel(inputModel);
			return reasoningModel.getIndividual(resource.getURI()).hasOntClass(inputClass);
		} catch (Exception e) {
			/* we're probably here because the service definition is incorrect,
			 * and we don't want a bad service spoiling everything for everybody...
			 */
			log.error(String.format("error classifying %s as an instance of %s", resource, inputClass), e);
			return false;
		} finally {
			reasoningModel.removeSubModel(inputModel);
		}
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#discoverInputInstances(com.hp.hpl.jena.rdf.model.Model)
     */
	@SuppressWarnings("unchecked")
	public synchronized Collection<Resource> discoverInputInstances(Model inputModel)
	{	
		OntModel reasoningModel = getServiceOntologyModel();
		OntClass inputClass = getInputClass();
		try {
			reasoningModel.addSubModel(inputModel);
			Collection<Resource> instancesInInputModel = new ArrayList<Resource>();
			for (Iterator<? extends OntResource> instances = inputClass.listInstances(); instances.hasNext(); ) {
				OntResource instance = instances.next();
				instancesInInputModel.add(instance.inModel(inputModel).as(Resource.class));
			}
			return instancesInInputModel;
		} catch (Exception e) {
			/* we're probably here because the service definition is incorrect,
			 * and we don't want a bad service spoiling everything for everybody...
			 */
			log.error(String.format("error discovering instances of %s", inputClass), e);
			return Collections.EMPTY_LIST;
		} finally {
			reasoningModel.removeSubModel(inputModel);
		}
	}
	
	/**
	 * Resolve any rdfs:isDefinedBy URIs in the specified model to fetch 
	 * asynchronous data.
	 * @param model the model
	 */
	public static void resolveAsynchronousData(Model model)
	{
		Set<String> seen = new HashSet<String>();
		for (StmtIterator i = model.listStatements((Resource)null, RDFS.isDefinedBy, (RDFNode)null); i.hasNext(); ) {
			Statement statement = i.removeNext();
			if (!statement.getObject().isURIResource())
				continue;
			
			String url = statement.getResource().getURI();
			if (seen.contains(url))
				continue;
			else
				seen.add(url);
			
			try {
				InputStream data = fetchAsyncData(url);
				model.read(data, url);
				data.close();
			} catch (Exception e) {
				log.error("failed to fetch data for " + statement, e);
			}
		}
	}
	
	@Override
	public String toString()
	{
		return getServiceURI();
	}
}
