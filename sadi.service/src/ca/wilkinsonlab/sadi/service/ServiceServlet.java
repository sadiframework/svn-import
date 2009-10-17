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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper;
import ca.wilkinsonlab.sadi.utils.OwlUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * This class encapsulates things common to both synchronous and asynchronous services.
 * SADI services should generally not extend this class directly; use either 
 * SynchronousServiceServlet or AsynchronousServiceServlet.
 * TODO this isn't really true; if you need to batch your input, you need to extend this...
 * @author Luke McCarthy
 */
public abstract class ServiceServlet extends HttpServlet
{
	private static final Log log = LogFactory.getLog(ServiceServlet.class);
	
	protected Configuration config;
	protected String serviceName;
	protected String serviceUrl;
	protected Model serviceModel;
	protected OntModel ontologyModel;
	protected OntClass inputClass;
	protected OntClass outputClass;
	protected ModelMaker modelMaker;
	protected ServiceOntologyHelper serviceOntologyHelper;
	
	public ServiceServlet()
	{
		super();
		
		try {
			config = Config.getConfiguration().getServiceConfiguration(this);
		} catch (ConfigurationException e) {
			log.fatal("Error configuring service servlet", e);
			throw new RuntimeException(e);
		}
		
		serviceName = config.getString("");
		
		/* In most cases, serviceUrl will be null, in which case the service
		 * model is constructed with a root node whose URI is "" (and the URL
		 * the service model is retrieved from will be the service URI...)
		 * In some cases, probably involving baroque network configurations,
		 * it might be necessary to supply an explicit service URI.
		 */
		serviceUrl = config.getString("url");
		
		modelMaker = createModelMaker();  // TODO support persistent models in properties file?
		serviceModel = createServiceModel();
		
		String serviceModelUrl = config.getString("rdf");
		if (serviceModelUrl != null) {
			/* if serviceModelUrl is a valid URL, read from that URL;
			 * if not, assume it is a location relative to the classpath...
			 */
			try {
				serviceModel.read(new URL(serviceModelUrl).toString());
			} catch (MalformedURLException e) {
				serviceModel.read(getClass().getResourceAsStream(serviceModelUrl), serviceUrl);
			}
			serviceOntologyHelper = new MyGridServiceOntologyHelper(serviceModel, serviceUrl, false);
		} else {
			/* create the service model from the information in the config...
			 */
			serviceOntologyHelper = new MyGridServiceOntologyHelper(serviceModel, serviceUrl, true);
			serviceOntologyHelper.setName(config.getString("name", "noname"));
			serviceOntologyHelper.setDescription(config.getString("description", "no description"));
			serviceOntologyHelper.setInputClass(config.getString("inputClass"));
			serviceOntologyHelper.setOutputClass(config.getString("outputClass"));
		}

		ontologyModel = createOntologyModel();
		
		String inputClassUri = serviceOntologyHelper.getInputClass().getURI();
		OwlUtils.loadOntologyForUri(ontologyModel, inputClassUri);
		inputClass = ontologyModel.getOntClass(inputClassUri);
		
		String outputClassUri = serviceOntologyHelper.getOutputClass().getURI();
		OwlUtils.loadOntologyForUri(ontologyModel, outputClassUri);
		outputClass = ontologyModel.getOntClass(outputClassUri);
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
		
		try {
			Model inputModel = readInput(request);
			call.setInputModel(inputModel);
			Model outputModel = prepareOutputModel(inputModel);
			call.setOutputModel(outputModel);
			
			processInput(call);
			outputSuccessResponse(response, call.getOutputModel());
		} catch (Exception e) {
			outputErrorResponse(response, e);
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
		serviceModel.write(response.getWriter(), "RDF/XML", request.getRequestURL().toString());
	}
	
	protected void outputSuccessResponse(HttpServletResponse response, Model outputModel) throws IOException
	{
		/* TODO add a mechanims to specify a web-accessible location on disk
		 * where the servlet can dump the output model and redirect to it...
		 */
		response.setContentType("application/rdf+xml");
		outputModel.write(response.getWriter());
	}
	
	protected void outputErrorResponse(HttpServletResponse response, Throwable error) throws IOException
	{
		/* TODO send errors in RDF in some form...
		 */
		error.printStackTrace(response.getWriter());
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}
	
	protected OntModel createOntologyModel()
	{
		// TODO do we actually need reasoning here?
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RULE_INF);
	}
	
	protected ModelMaker createModelMaker()
	{
		return ModelFactory.createMemModelMaker();
	}
	
	protected Model createServiceModel()
	{
		return modelMaker.createModel(serviceName);
	}
	
	protected Model createInputModel()
	{
		return modelMaker.createFreshModel();
	}
	
	protected Model createOutputModel()
	{
		return modelMaker.createFreshModel();
	}
}
