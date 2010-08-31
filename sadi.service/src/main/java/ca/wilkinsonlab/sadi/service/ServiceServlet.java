package ca.wilkinsonlab.sadi.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyException;
import ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.QueryableErrorHandler;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * This class encapsulates things common to both synchronous and asynchronous services.
 * SADI services should generally not extend this class directly; use either 
 * SynchronousServiceServlet or AsynchronousServiceServlet.
 * TODO this isn't really true; if you need to batch your input, you need to extend this...
 * @author Luke McCarthy
 */
@SuppressWarnings("serial")
public abstract class ServiceServlet extends HttpServlet
{
	private static final Logger log = Logger.getLogger(ServiceServlet.class);

	protected QueryableErrorHandler errorHandler;
	protected ModelMaker modelMaker;
	protected Model serviceModel;
	
	protected Configuration config;
	protected String serviceName;
	protected String serviceUrl;
	protected OntModel ontologyModel;
	protected OntClass inputClass;
	protected OntClass outputClass;
	protected ServiceOntologyHelper serviceOntologyHelper;
	
	@Override
	public synchronized void init() throws ServletException
	{
		log.trace("entering ServiceServlet.init()");
		
		if (serviceModel != null) {
			log.fatal("multiple calls to ServiceServlet.init()");
			throw new ServletException("multiple calls to ServiceServlet.init()");
		}

		errorHandler = new QueryableErrorHandler();
		modelMaker = createModelMaker();  // TODO support persistent models in properties file?
		serviceModel = createServiceModel();
		
		try {
			log.trace("reading service configuration");
			config = Config.getConfiguration().getServiceConfiguration(this);
		} catch (ConfigurationException e) {
			log.fatal("error configuring service servlet", e);
			throw new ServletException("error configuring service servlet: " + e.toString(), e);
		}
		
		serviceName = config.getString("");
		
		/* In most cases, serviceUrl will be null, in which case the service
		 * model is constructed with a root node whose URI is "" (and the URL
		 * the service model is retrieved from will be the service URI...)
		 * In some cases, probably involving baroque network configurations,
		 * it might be necessary to supply an explicit service URI.
		 */
		serviceUrl = (System.getProperty("sadi.service.ignoreForcedURL") != null) ?
				null : config.getString("url");
		
		String serviceModelUrl = config.getString("rdf");
		if (serviceModelUrl != null) {
			try {
				/* if serviceModelUrl is a valid URL, read from that URL;
				 * if not, assume it is a location relative to the classpath...
				 */
				try {
					serviceModel.read(new URL(serviceModelUrl).toString());
					log.debug(String.format("read service model from URL %s", serviceModelUrl));
				} catch (MalformedURLException e) {
					log.debug(String.format("reading service model from classpath location %s", serviceModelUrl));
					serviceModel.read(getClass().getResourceAsStream(serviceModelUrl), StringUtils.defaultString(serviceUrl));
				}
				if (errorHandler.hasLastError())
					throw errorHandler.getLastError();

				serviceOntologyHelper = new MyGridServiceOntologyHelper(serviceModel, StringUtils.defaultString(serviceUrl), false);
			} catch (Exception e) {
				throw new ServletException(String.format("error reading service definition from %s: %s", serviceModelUrl, e.toString()));
			}
		} else {
			try {
				/* create the service model from the information in the config...
				 */
				log.trace("creating service description model");
				serviceOntologyHelper = new MyGridServiceOntologyHelper(serviceModel, serviceUrl, true);
				serviceOntologyHelper.setName(config.getString("name", "noname"));
				serviceOntologyHelper.setDescription(config.getString("description", "no description"));
				serviceOntologyHelper.setInputClass(config.getString("inputClass"));
				serviceOntologyHelper.setOutputClass(config.getString("outputClass"));
			} catch (ServiceOntologyException e) {
				throw new ServletException("error creating service definition from configuration: " + e.toString(), e);
			}
		}

		log.trace("creating service ontology model");
		ontologyModel = createOntologyModel();
		
		try {
			inputClass = loadInputClass();
		} catch (Exception e) {
			throw new ServletException("error loading input class: " + e.toString(), e);
		}
		
		try {
			outputClass = loadOutputClass();
		} catch (Exception e) {
			throw new ServletException("error loading input class: " + e.toString(), e);
		}
	}
	
	protected OntClass loadInputClass() throws Exception
	{
		String inputClassUri = serviceOntologyHelper.getInputClass().getURI();
		OwlUtils.loadOntologyForUri(ontologyModel, inputClassUri);
		OntClass inputClass = ontologyModel.getOntClass(inputClassUri);
		if (errorHandler.hasLastError())
			throw errorHandler.getLastError();
		else
			return inputClass;
	}
	
	protected OntClass loadOutputClass() throws Exception
	{
		String outputClassUri = serviceOntologyHelper.getOutputClass().getURI();
		OwlUtils.loadOntologyForUri(ontologyModel, outputClassUri);
		outputClass = ontologyModel.getOntClass(outputClassUri);
		if (errorHandler.hasLastError())
			throw errorHandler.getLastError();
		else
			return outputClass;
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		outputServiceModel(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		/* create the service-call structure and execute it in a try/catch
		 * block that sends an appropriate error response...
		 */
		ServiceCall call = new ServiceCall();
		call.setRequest(request);
		
		Model inputModel = null;
		Model outputModel = null;
		try {
			inputModel = readInput(request);
			call.setInputModel(inputModel);
			outputModel = prepareOutputModel(inputModel);
			call.setOutputModel(outputModel);
			
			processInput(call);
			outputSuccessResponse(response, call.getOutputModel());
		} catch (Exception e) {
			outputErrorResponse(response, e);
			if (inputModel != null)
				closeInputModel(inputModel);
			if (outputModel != null)
				closeOutputModel(outputModel);
		}
	}
	
	protected Model readInput(HttpServletRequest request) throws IOException
	{
		Model inputModel = createInputModel();
		String contentType = request.getContentType();
		if (contentType.equals("application/rdf+xml")) {
			inputModel.read(request.getInputStream(), "", "RDF/XML");
		} else if (contentType.equals("text/rdf+n3")) {
			inputModel.read(request.getInputStream(), "", "N3");
		} else {
			inputModel.read(request.getInputStream(), "");
		}

		if (log.isTraceEnabled())
			log.trace("incoming input model:\n" + RdfUtils.logStatements(inputModel));
		
		return inputModel;
	}
	
	protected Model prepareOutputModel(Model inputModel)
	{
		/* 2009-03-17 Luke and Mark decide that inputs will be explicitly typed,
		 * so reasoning is not required here...
		 */
		Model outputModel = createOutputModel();
		for (ResIterator i = inputModel.listSubjectsWithProperty(RDF.type, inputClass); i.hasNext();) {
			outputModel.createResource(i.nextResource().getURI(), outputClass);
		}
		return outputModel;
	}
	
	protected abstract void processInput(ServiceCall call);
	
	protected void outputServiceModel(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		/* output the service model using the request URL as the base for an
		 * relative URIs...
		 */
		response.setContentType("application/rdf+xml");
		RDFWriter writer = serviceModel.getWriter("RDF/XML-ABBREV");
		writer.write(serviceModel, response.getWriter(), request.getRequestURL().toString());
	}
	
	protected void outputSuccessResponse(HttpServletResponse response, Model outputModel) throws IOException
	{
		/* TODO add a mechanism to specify a web-accessible location on disk
		 * where the servlet can dump the output model and redirect to it...
		 */
		response.setContentType("application/rdf+xml");
		RDFWriter writer = outputModel.getWriter("RDF/XML-ABBREV");
		writer.write(outputModel, response.getWriter(), "");
	}
	
	protected void outputErrorResponse(HttpServletResponse response, Throwable error) throws IOException
	{
		/* we can't just write to the response because Jena calls flush() on
		 * the writer or stream, which commits the response...
		 */
//		Model exceptionModel = ExceptionUtils.createExceptionModel(error);
//		StringWriter buffer = new StringWriter();
//		exceptionModel.write(buffer);
//		response.getWriter().print(buffer.toString());
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error.toString());
	}
	
	protected OntModel createOntologyModel()
	{
		/* according to the SADI spec, input nodes are explicitly typed, so
		 * we don't need any reasoning here...
		 */
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		model.getReader().setErrorHandler(errorHandler);
		return model;
	}
	
	protected ModelMaker createModelMaker()
	{
		return ModelFactory.createMemModelMaker();
	}
	
	protected Model createServiceModel()
	{
		Model model = modelMaker.createModel(serviceName);
		model.getReader().setErrorHandler(errorHandler);
		model.setNsPrefix("mygrid", "http://www.mygrid.org.uk/mygrid-moby-service#");
		return model;
	}
	
	protected Model createInputModel()
	{
		Model model = modelMaker.createFreshModel();
		log.trace(String.format("created input model %s", model.hashCode()));
		return model;
	}
	
	protected void closeInputModel(Model model)
	{
		model.close();
		log.trace(String.format("closed input model %s", model.hashCode()));
	}
	
	protected Model createOutputModel()
	{
		Model model = modelMaker.createFreshModel();
		log.trace(String.format("created output model %s", model.hashCode()));
		return model;
	}
	
	protected void closeOutputModel(Model model)
	{
		model.close();
		log.trace(String.format("closed output model %s", model.hashCode()));
	}
}
