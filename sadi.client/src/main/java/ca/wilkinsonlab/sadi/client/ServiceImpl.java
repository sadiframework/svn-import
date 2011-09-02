package ca.wilkinsonlab.sadi.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.testing.TestCase;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.utils.ContentType;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.QueryableErrorHandler;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils.HttpStatusException;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.DoesNotExistException;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * The native SADI Service class.
 * 
 * @author Luke McCarthy
 */
public class ServiceImpl extends ServiceBase
{
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(ServiceImpl.class);
	
	Model model;
	OntModel ontModel;
	QueryableErrorHandler errorHandler;
	
	OntClass inputClass;
	OntClass outputClass;
	Collection<Restriction> restrictions;
	Collection<TestCase> tests;
	
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
	
	void loadServiceModel() throws SADIException
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
		if (errorHandler.hasLastError()) {
			Exception e = errorHandler.getLastError();
			errorHandler.clear();
			if (e instanceof SADIException)
				throw (SADIException)e;
			else
				throw new SADIException(e.toString(), e);
		}
		
		Resource serviceRoot = model.getResource(getURI());
		if (!model.containsResource(serviceRoot)) {
			throw new ServiceConnectionException(String.format("service description contains no such resource %s", getURI()));
		} else {
			new MyGridServiceOntologyHelper().copyServiceDescription(serviceRoot, this);
		}
	}
	
	/**
	 * Returns the service definition as a Jena Model.
	 * @return the service definition as a Jena Model
	 */
	public Model getServiceModel()
	{
		return model;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getInputClass()
     */
	public OntClass getInputClass() throws SADIException
	{
		synchronized (ontModel) {
			try {
				if (inputClass == null) {
					inputClass = OwlUtils.getOntClassWithLoad(ontModel, getInputClassURI(), true);
					if (errorHandler.hasLastError())
						throw errorHandler.getLastError();
					if (inputClass == null)
						throw new SADIException(String.format("class %s is not defined", getInputClassURI()));
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
					outputClass = OwlUtils.getOntClassWithLoad(ontModel, getOutputClassURI(), true);
					if (errorHandler.hasLastError())
						throw errorHandler.getLastError();
					if (outputClass == null)
						throw new SADIException(String.format("class %s is not defined", getOutputClassURI()));
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
	public Model invokeService(Iterator<Resource> inputNodes) throws ServiceInvocationException
	{
		OntClass inputClass;
		try {
			inputClass = getInputClass();
		} catch (SADIException e) {
			throw new ServiceInvocationException(e.getMessage(), e.getCause());
		}
		
		Model inputModel = ModelFactory.createDefaultModel();
		log.debug(String.format("assembling input to %s", this));
		while (inputNodes.hasNext()) {
			Resource inputNode = inputNodes.next();
			log.debug(String.format("computing minimal RDF for %s as an instance of %s", inputNode, inputClass));
			inputNode.addProperty(RDF.type, inputClass);
			inputModel.add(OwlUtils.getMinimalModel(inputNode, inputClass));
		}
		
		return invokeServiceUnparsed(inputModel);
	}

	/**
	 * Call this service using the specified Model as input, without
	 * any extra parsing/filtering.
	 * @param inputModel the input data
	 * @return the service output
	 * @throws IOException 
	 */
	public Model invokeServiceUnparsed(Model inputModel) throws ServiceInvocationException
	{
		if (log.isTraceEnabled()) {
			log.trace(String.format("posting RDF to %s:\n%s", getURI(), RdfUtils.logModel(inputModel)));
		}

		Model model = ModelFactory.createDefaultModel();
		try {
			InputStream is = HttpUtils.postToURL(new URL(getURI()), inputModel);
			model.read(is, "");
			is.close();
		} catch (IOException e) {
			throw new ServiceInvocationException(String.format("error communicating with service: %s", e.getMessage()), e);
		} catch (Exception e) {
			throw new ServiceInvocationException(String.format("error parsing service response: %s", e.getMessage()), e);
		}
		
		/* resolve any rdfs:isDefinedBy URIs to fetch asynchronous data...
		 */
		resolveAsynchronousData(model);
		
		if (log.isTraceEnabled()) {
			log.trace(String.format("received output:\n%s", RdfUtils.logModel(model)));
		}
		return model;
	}
	
	/**
	 * Resolve any rdfs:isDefinedBy URIs in the specified model to fetch 
	 * asynchronous data.
	 * @param model the model
	 */
	static void resolveAsynchronousData(Model model)
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
	static InputStream fetchAsyncData(String url) throws HttpException, IOException
	{
		while (true) {
			log.debug("fetching asynchronous data from " + url);
			GetMethod method = new GetMethod(url);
			method.setFollowRedirects(false);
			method.setRequestHeader("Accept", ContentType.RDF_XML.getHTTPHeader());
			HttpClient client = new HttpClient();
			int statusCode = client.executeMethod(method);
			if (statusCode == 202 || (statusCode >= 300 && statusCode < 400)) {
				long toSleep = 10000; // sleep for ten seconds by default
				String retryAfter = HttpUtils.getHeaderValue(method, "Retry-After");
				if (retryAfter != null) {
					try {
						toSleep = 1000 * Long.valueOf(retryAfter);
					} catch (NumberFormatException e) {
						log.error(String.format("error parsing value of Retry-After header '%s'", retryAfter), e);
					}
				}
				// TODO stop looking for for legacy header at some point?
				String pleaseWait = HttpUtils.getHeaderValue(method, "Pragma", SADI.ASYNC_HEADER);
				if (pleaseWait != null) {
					// toSleep = (pleaseWait =~ /\s*=\s*(\d+)/)[0]
				}
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
	 * @see ca.wilkinsonlab.sadi.client.ServiceBase#getLog()
	 */
	@Override
	protected Logger getLog()
	{
		return log;
	}

//	@Override
//	public int hashCode()
//	{
//		return getURI().hashCode();
//	}
//
//	@Override
//	public boolean equals(Object obj)
//	{
//		return obj != null &&
//		       obj instanceof ServiceImpl &&
//		       getURI().equals(((ServiceImpl)obj).getURI());
//	}
}
