package ca.wilkinsonlab.sadi.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.client.testing.TestCase;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.utils.ExceptionUtils;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.QueryableErrorHandler;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils.HttpStatusException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.DoesNotExistException;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * The native SADI Service class.
 * 
 * @author Luke McCarthy
 */
public class ServiceImpl extends ServiceBase
{
	private static final Logger log = Logger.getLogger(ServiceImpl.class);
	
	String serviceURI;
	String name;
	String description;
	String inputClassURI;
	String outputClassURI;
	
	RegistryImpl sourceRegistry;
	
	Model model;
	OntModel ontModel;
	QueryableErrorHandler errorHandler;
	
	OntClass inputClass;
	OntClass outputClass;
	Collection<Restriction> restrictions;
	Collection<TestCase> tests;
	
	/**
	 * Construct a new SADI service from the service description located at
	 * the specified URL.
	 * @param serviceURL the service URL
	 * @throws SADIException
	 */
	public ServiceImpl(String serviceURL) throws SADIException
	{
		this();
		
		serviceURI = serviceURL;
		
		try {
			loadServiceModel();
		} catch (Exception e) {
			if (e instanceof SADIException)
				throw (SADIException)e;
			else
				throw new SADIException(e.toString(), e);
		} finally {
			errorHandler.clear();
		}
	}
	
	/**
	 * Construct a new RdfService from a service URI, an input class URI
	 * and an output class URI. This method is used by the test cases and
	 * by the registry.
	 * @param serviceInfo a map containing the service information
	 */
	ServiceImpl(Map<String, String> serviceInfo) throws SADIException
	{
		this();
		
		serviceURI = serviceInfo.get("serviceURI");
		name = serviceInfo.get("name");
		description = serviceInfo.get("description");
		inputClassURI = serviceInfo.get("inputClassURI");
		outputClassURI = serviceInfo.get("outputClassURI");
		
		ServiceBean serviceBean = new ServiceBean();
		serviceBean.setURI(serviceURI);
		serviceBean.setName(name);
		serviceBean.setDescription(description);
		serviceBean.setInputClassURI(inputClassURI);
		serviceBean.setOutputClassURI(outputClassURI);
		new MyGridServiceOntologyHelper().createServiceNode(serviceBean, model);
	}
	
	/* Perform initialization common to all constructors. Jena models are
	 * created here so they can be used as locks for the thread-safe blocks.
	 */
	ServiceImpl()
	{
		model = ModelFactory.createDefaultModel();
		ontModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		errorHandler = new QueryableErrorHandler();
		model.getReader().setErrorHandler(errorHandler);
		ontModel.getReader().setErrorHandler(errorHandler);
	}
	
	void loadServiceModel() throws Exception
	{
		log.debug("fetching service model from " + getURI());
		if (!model.isEmpty())
			log.warn(String.format("service %s may have been initialized twice; this could indicate multithreading problems", getURI()));
		
		try {
			model.read(getURI());
		} catch (JenaException e) {
			if (e instanceof DoesNotExistException) {
				throw new ServiceConnectionException("does not exist");//String.format("no such service %s", getURI()));
			} else if (e.getMessage().endsWith("Connection refused")) {
				throw new ServiceConnectionException("connection refused");//String.format("connection refused to service %s", getURI()));
			}
		}
		if (errorHandler.hasLastError())
			throw errorHandler.getLastError();
		
		Resource serviceRoot = model.getResource(getURI());
		if (serviceRoot == null) {
			throw new SADIException(String.format("service model contains no such resource %s", getURI()));
		} else {
			ServiceBean serviceBean = new ServiceBean();
			new MyGridServiceOntologyHelper().copyServiceDescription(serviceRoot, serviceBean);
			name = serviceBean.getName();
			description = serviceBean.getDescription();
			inputClassURI = serviceBean.getInputClassURI();
			outputClassURI = serviceBean.getOutputClassURI();
		}
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getServiceURI()
     */
	public String getURI()
	{
		return serviceURI;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getName()
     */
	public String getName()
	{
		return name;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getDescription()
     */
	public String getDescription()
	{
		return description;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getInputClassURI()
     */
	public String getInputClassURI()
	{
		return inputClassURI;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getOutputClassURI()
     */
	public String getOutputClassURI()
	{
		return outputClassURI;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getInputClass()
     */
	public OntClass getInputClass() throws SADIException
	{
		synchronized (ontModel) {
			try {
				if (inputClass == null) {
					inputClass = OwlUtils.getOntClassWithLoad(ontModel, inputClassURI);
					if (errorHandler.hasLastError())
						throw errorHandler.getLastError();
					if (inputClass == null)
						throw new SADIException(String.format("class %s is not defined", inputClassURI));
				}
				return inputClass;
			} catch (Exception e) {
				if (e instanceof SADIException)
					throw (SADIException)e;
				else
					throw new SADIException(e.toString(), e);
			} finally {
				errorHandler.clear();
			}
		}
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getOutputClass()
     */
	public OntClass getOutputClass() throws SADIException
	{
		synchronized (ontModel) {
			try {
				if (outputClass == null) {
					outputClass = OwlUtils.getOntClassWithLoad(ontModel, outputClassURI);
					if (errorHandler.hasLastError())
						throw errorHandler.getLastError();
					if (outputClass == null)
						throw new SADIException(String.format("class %s is not defined", outputClassURI));
				}
				return outputClass;
			} catch (Exception e) {
				if (e instanceof SADIException)
					throw (SADIException)e;
				else
					throw new SADIException(e.toString(), e);
			} finally {
				errorHandler.clear();
			}
		}
	}
	
	/**
	 * Returns the property restrictions attached by this service.
	 * @return the property restrictions attached by this service
	 * @throws SADIException
	 */
	public Collection<Restriction> getRestrictions() throws SADIException
	{
		if (restrictions == null) {
			restrictions = OwlUtils.listRestrictions(getOutputClass(), getInputClass());
		}
		return restrictions;
	}
	
	/**
	 * Returns the service definition as a Jena Model.
	 * @return the service definition as a Jena Model
	 */
	public Model getServiceModel()
	{
		return model;
	}

	/**
	 * Returns the list of predicates this service attaches to its input.
	 * @return the list of predicates this service attaches to its input
	 */
	public Collection<String> getPredicates() throws SADIException
	{
		Collection<Restriction> restrictions = getRestrictions();
		Collection<String> predicates = new ArrayList<String>( restrictions.size() );
		for (Restriction restriction: restrictions) {
			try {
				predicates.add( restriction.getOnProperty().getURI() );
			} catch (Exception e) {
				// ConversionException if the property is undefined
				// NullPointerException maybe?
				log.error(String.format("error extracting property from restriction %s", restriction), e);
			}
		}
		return predicates;
	}
	
	public Collection<TestCase> getTestCases()
	{
		if (tests == null) {
			tests = new ArrayList<TestCase>();
			AbstractServiceOntologyHelper helper = new MyGridServiceOntologyHelper();
			for (RDFNode testCaseNode: helper.getTestCasePath().getValuesRootedAt(model.getResource(getURI()))) {
				try {
					if (!testCaseNode.isResource()) {
						throw new Exception("test case node is literal");
					}
					Resource testCaseResource = testCaseNode.asResource();
					Collection<RDFNode> inputs = helper.getTestInputPath().getValuesRootedAt(testCaseResource);
					if (inputs.isEmpty()) {
						throw new Exception("no input specified, but each test case needs one");
					} else if (inputs.size() > 1) {
						throw new Exception("multiple inputs specified, but each test case can only have one");
					}
					Collection<RDFNode> outputs = helper.getTestOutputPath().getValuesRootedAt(testCaseResource);
					if (outputs.isEmpty()) {
						throw new Exception("no output specified, but each test case needs one");
					} else if (outputs.size() > 1) {
						throw new Exception("multiple outputs specified, but each test case can only have one");
					}
					tests.add(new TestCase(inputs.iterator().next(), outputs.iterator().next()));
				} catch (Exception e) {
					log.warn("skipping test case", e);
				}
			}
		}
		return tests;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.client.Service#invokeService(java.util.Collection)
	 */
	public Collection<Triple> invokeService(Collection<Resource> inputNodes) throws ServiceInvocationException
	{
		OntClass inputClass;
		try {
			inputClass = getInputClass();
		} catch (SADIException e) {
			throw new ServiceInvocationException(e.getMessage(), e.getCause());
		}
		
		Model inputModel = ModelFactory.createDefaultModel();
		for (Resource inputNode: inputNodes) {
			log.debug(String.format("computing minimal RDF for %s as an instance of %s", inputNode, inputClass));
			inputModel.add(OwlUtils.getMinimalModel(inputNode, inputClass));
		}
		
		Model outputModel = null;
		try {
			outputModel = invokeServiceUnparsed(inputModel);
		} catch (Exception e) {
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

	/**
	 * Call this service using the specified Model as input, without
	 * any extra parsing/filtering.
	 * @param inputModel the input data
	 * @return the service output
	 * @throws IOException 
	 */
	public Model invokeServiceUnparsed(Model inputModel) throws IOException
	{
		if (log.isTraceEnabled()) {
			log.trace(String.format("posting RDF to %s:\n%s", getURI(), RdfUtils.logStatements("\t", inputModel)));
		}
		InputStream is = HttpUtils.postToURL(new URL(getURI()), inputModel);
		Model model = ModelFactory.createDefaultModel();
		model.read(is, "");
		is.close();
		
		/* resolve any rdfs:isDefinedBy URIs to fetch asynchronous data...
		 */
		resolveAsynchronousData(model);
		
		if (log.isTraceEnabled()) {
			log.trace(String.format("received output:\n%s", RdfUtils.logStatements("\t", model)));
		}
		return model;
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
				long toSleep = 10000; // sleep for ten seconds by default
				String retryAfter = HttpUtils.getHeaderValue(method, "Retry-After");
				if (retryAfter != null) {
					try {
						toSleep = 1000 * Long.valueOf(retryAfter);
					} catch (NumberFormatException e) {
						log.error(String.format("error parsing value of Retry-After header '%s'", retryAfter), e);
					}
				}
//				// look for legacy header...
//				String pleaseWait = HttpUtils.getHeaderValue(method, "Pragma", SADI.ASYNC_HEADER);
//				if (pleaseWait != null) {
//					// toSleep = (pleaseWait =~ /\s*=\s*(\d+)/)[0]
//				}
				try {
					log.trace("sleeping " + toSleep + "ms before following redirect");
					Thread.sleep(toSleep);
				} catch (InterruptedException e) {
					log.warn(e);
				}
				// get new location...
				String newURL = HttpUtils.getHeaderValue(method, "Location");
				if (newURL != null)
					url = newURL;
			} else if (statusCode >= 200 && statusCode < 300) {
				return method.getResponseBodyAsStream();
			} else {
				throw new HttpStatusException(statusCode);
			}
		}
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#isInputInstance(com.hp.hpl.jena.rdf.model.Resource)
     */
	public boolean isInputInstance(Resource resource)
	{
		Model inputModel = resource.getModel();
		OntModel reasoningModel = ontModel;
		try {
			OntClass inputClass = getInputClass();
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
		OntModel reasoningModel = ontModel;
		try {
			OntClass inputClass = getInputClass();
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getURI();
	}
}
