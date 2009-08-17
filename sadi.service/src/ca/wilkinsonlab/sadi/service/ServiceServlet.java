package ca.wilkinsonlab.sadi.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * This class encapsulates things common to both synchronous and asynchronous services.
 * SADI services should generally not extend this class directly; use either 
 * SynchronousServiceServlet or AsynchronousServiceServlet.
 * TODO this isn't really true; if you need to batch your input, you need to extend this...
 * @author Luke McCarthy
 */
public abstract class ServiceServlet extends HttpServlet implements InputProcessor
{
	private static final Log log = LogFactory.getLog(ServiceServlet.class);
	
	protected Configuration config;
	protected String serviceUrl;
	protected Model serviceModel;
	protected OntClass inputClass;
	protected OntClass outputClass;
	protected OntModel ontologyModel;
	protected ModelMaker modelMaker;
	protected ServiceOntologyHelper serviceOntologyHelper;
	
	public ServiceServlet()
	{
		super();
		
		try {
			config = Config.getConfiguration().getServiceConfiguration(this);
		} catch (ConfigurationException e) {
			log.fatal(e);
			throw new RuntimeException(e);
		}
		
		modelMaker = createModelMaker();  // TODO support persistent models in properties file?
		serviceUrl = config.getString("url");
		serviceModel = modelMaker.createModel(serviceUrl);
		
		String serviceModelUrl = config.getString("rdf");
		if (serviceModelUrl == null) {
			/* create the service model from the information in the config...
			 */
			serviceOntologyHelper = new MyGridServiceOntologyHelper(serviceModel, serviceUrl);
			serviceOntologyHelper.setName(config.getString("name"));
			serviceOntologyHelper.setDescription(config.getString("description"));
			serviceOntologyHelper.setInputClass(config.getString("inputClass"));
			serviceOntologyHelper.setOutputClass(config.getString("outputClass"));
		} else {
			/* read the service model from the local classpath or a remote URL...
			 */
			try {
				new URL(serviceModelUrl);
				serviceModel.read(serviceModelUrl);
			} catch (MalformedURLException e) {
				serviceModel.read(getClass().getResourceAsStream(serviceModelUrl), "");
			}
			
			serviceOntologyHelper = new MyGridServiceOntologyHelper(serviceModel.getResource(serviceUrl));
		}

		String inputClassUri = serviceOntologyHelper.getInputClass().getURI();
		String outputClassUri = serviceOntologyHelper.getOutputClass().getURI();
		ontologyModel = createOntologyModel();
		OwlUtils.loadOntologyForUri(ontologyModel, inputClassUri);
		OwlUtils.loadOntologyForUri(ontologyModel, outputClassUri);
		
		inputClass = ontologyModel.getOntClass(inputClassUri);
		outputClass = ontologyModel.getOntClass(outputClassUri);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		outputServiceModel(response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		/* TODO surround model read with try/catch...
		 */
		Model inputModel = readInput(request);
		
		try {
			Model outputModel = processInput(inputModel);
			outputSuccessResponse(response, outputModel);
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
	
	protected Model processInput(Model inputModel)
	{
		/* 2009-03-17 Luke and Mark decide that inputs will be explicitly typed,
		 * so reasoning is not required here...
		 */
		Model outputModel = createOutputModel();
		Map<Resource, Resource> inputOutputMap = new HashMap<Resource, Resource>();
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, inputModel);
		for (Iterator i = ontModel.listIndividuals(inputClass); i.hasNext(); ) {
			Resource inputNode = inputModel.getResource(((Resource)i.next()).getURI());
			Resource outputNode = outputModel.createResource(inputNode.getURI(), outputClass);
			inputOutputMap.put(inputNode, outputNode);
		}
		
		if (inputOutputMap.isEmpty()) {
			String message = "POST data contained no instances of " + inputClass;
			log.warn(message);
			throw new IllegalArgumentException(message);
		} else {
			processInput(inputOutputMap);
			return outputModel;
		}
	}
	
	protected abstract void processInput(Map<Resource, Resource> inputOutputMap);
	
//	/* (non-Javadoc)
//	 * @see ca.wilkinsonlab.sadi.service.InputProcessor#processInput(com.hp.hpl.jena.rdf.model.Resource, com.hp.hpl.jena.rdf.model.Resource)
//	 */
//	public void processInput(Resource input, Resource output)
//	{
//		throw new UnsupportedOperationException("Subclasses of SynchronousServiceServlet must override either processInput or getInputProcessor");
//	}
	
	protected InputProcessor getInputProcessor()
	{
		return this;
	}
	
	protected void outputServiceModel(HttpServletResponse response) throws IOException
	{
		response.setContentType("application/rdf+xml");
		serviceModel.write(response.getWriter());
	}
	
	protected void outputSuccessResponse(HttpServletResponse response, Model outputModel) throws IOException
	{
		/* TODO should probably have a more complex Servlet that stores the final response
		 * somewhere on disk and redirects there...
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
	
	protected Model createInputModel()
	{
		return modelMaker.createFreshModel();
	}
	
	protected Model createOutputModel()
	{
		return modelMaker.createFreshModel();
	}
}
