package ca.wilkinsonlab.sadi.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.ServiceDescription;
import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.rdfpath.RDFPath;
import ca.wilkinsonlab.sadi.rdfpath.RDFPathUtils;
import ca.wilkinsonlab.sadi.service.annotations.Authoritative;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.ParameterClass;
import ca.wilkinsonlab.sadi.service.annotations.ParameterDefaults;
import ca.wilkinsonlab.sadi.service.annotations.ServiceDefinition;
import ca.wilkinsonlab.sadi.service.annotations.ServiceProvider;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper;
import ca.wilkinsonlab.sadi.utils.ContentType;
import ca.wilkinsonlab.sadi.utils.QueryableErrorHandler;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.vocabulary.OWL;
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
	public static final String NAME_KEY = "name";
	public static final String DESCRIPTION_KEY = "description";
	public static final String SERVICE_PROVIDER_KEY = "serviceProvider";
	public static final String CONTACT_EMAIL_KEY = "contactEmail";
	public static final String AUTHORITATIVE_KEY = "authoritative";
	public static final String INPUT_CLASS_KEY = "inputClass";
	public static final String OUTPUT_CLASS_KEY = "outputClass";
	public static final String PARAMETER_CLASS_KEY = "parameterClass";
	public static final String PARAMETER_DEFAULTS_KEY = "parameterDefaults";
	public static final String SERVICE_DEFINITION_KEY = "rdf";
	public static final String SERVICE_URL_KEY = "url";
	
	public static final String SERVICE_DEFINITION_STYLESHEET = "http://sadiframework.org/style/current/service-definition.xsl";
	
	/**
	 * If this system property is set, any configured service URL will be 
	 * ignored. A service URL only needs to be set explicitly in baroque 
	 * network configurations where the request URL that the service servlet 
	 * receives is not the external service URL (and even then, only with 
	 * asynchronous services because they generate polling URLs from the 
	 * request URL...) In these cases, the configured service URL will often 
	 * not apply during service testing (which will usually take place on 
	 * localhost), so this property can simply be set on the local JVM and the
	 * service WAR can be run locally and on the server without modification.
	 */
	public static final String IGNORE_FORCED_URL_SYSTEM_PROPERTY = "sadi.service.ignoreForcedURL";
	
	private static final Logger log = Logger.getLogger(ServiceServlet.class);
	private static final long serialVersionUID = 1L;

	protected Configuration config;
	protected QueryableErrorHandler errorHandler;
	protected ModelMaker modelMaker;
	protected ServiceOntologyHelper serviceOntologyHelper;
	protected Model serviceModel;
	protected ServiceDescription serviceDescription;
	protected Resource inputClass;
	protected Resource outputClass;
	protected Resource parameterClass;
	protected Resource defaultParameters;
	
	private boolean ignoreForcedURL;
	
	@Override
	public void init() throws ServletException
	{
		log.trace("entering ServiceServlet.init()");
		
		/* getServiceURL() will usually return null; a service URL only needs
		 * to be set explicitly in baroque network configurations where the
		 * request URL that the service servlet receives is not the external
		 * service URL (and even then, only with asynchronous services that
		 * generate polling URLs from the request URL...)
		 * In these cases, the configured service URL will often not apply 
		 * during service testing (which will usually take place on localhost).
		 * To facilitate testing, the configured service URL will be ignored
		 * if the property "sadi.service.ignoreForcedURL" is set on the JVM.
		 */
		if (System.getProperty(IGNORE_FORCED_URL_SYSTEM_PROPERTY) != null) {
			log.info("ignoring specified service URL");
			ignoreForcedURL = true;
		} else {
			ignoreForcedURL = false;
		}
		
		config = Config.getConfiguration().getServiceConfiguration(this);
		if (config == null)
			log.debug(String.format("service servlet %s is not mapped to a configuration", this));

		errorHandler = new QueryableErrorHandler();
		modelMaker = createModelMaker();  // TODO support persistent models in properties file?
		serviceOntologyHelper = new MyGridServiceOntologyHelper();
		serviceModel = createServiceModel();

		try {
			String serviceRDF = getServiceRDF();
			if (serviceRDF != null) {
				Resource serviceNode = loadServiceModelFromLocation(serviceRDF);
				serviceDescription = getServiceOntologyHelper().getServiceDescription(serviceNode);
			} else {
				serviceDescription = createServiceDescription();
				getServiceOntologyHelper().createServiceNode(serviceDescription, serviceModel);
			}
		} catch (SADIException e) {
			String message = e.getMessage();
			log.error(message, e);
			throw new ServletException(message);
		}
		
		inputClass = serviceModel.getResource(serviceDescription.getInputClassURI());
		outputClass = serviceModel.getResource(serviceDescription.getOutputClassURI());
		
		String parameterClassURI = serviceDescription.getParameterClassURI();
		if (parameterClassURI != null) {
			parameterClass = serviceModel.getResource(parameterClassURI);
			try {
				defaultParameters = extractDefaultParameterInstanceFromModel();
				if (defaultParameters == null)
					defaultParameters = createDefaultParameterInstance();
			} catch (SADIException e) {
				String message = e.getMessage();
				log.error(message, e);
				throw new ServletException(message);
			}
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		/* XSL transformation is only performed when the stylesheet comes
		 * from the same domain as the XML it's styling. so we proxy the
		 * stylesheet from sadiframework.org...
		 */
		if (request.getParameter("xsl") != null) {
			outputXSL(response);
			return;
		}
		
		/* set the content type on the response so that methods that only 
		 * have access to the response object know what to output...
		 */
		response.setContentType(getContentType(request).getHTTPHeader());
		outputServiceModel(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		/* set the content type on the response so that methods that only 
		 * have access to the response object know what to output...
		 */
		response.setContentType(getContentType(request).getHTTPHeader());
		
		/* create the service-call structure and execute it in a try/catch
		 * block that sends an appropriate error response...
		 */
		ServiceCall call = new ServiceCall();
		call.setRequest(request);
		call.setResponse(response);
		
		try {
			Model inputModel = readInput(request);
			call.setInputModel(inputModel);
			call.setInputNodes(inputModel.listResourcesWithProperty(RDF.type, getInputClass()).toList());
			Model outputModel = prepareOutputModel(inputModel);
			call.setOutputModel(outputModel);
			Resource parameters = createParameters(inputModel);
			call.setParameters(parameters);
			processInput(call);
			outputSuccessResponse(response, call.getOutputModel());
			cleanupServiceCall(call);
		} catch (Exception e) {
			outputErrorResponse(response, e);
			cleanupServiceCall(call);
		}
	}

	public static ContentType getContentType(HttpServletRequest request)
	{
		ContentType contentType = null;
		for (Enumeration<?> headers = request.getHeaders("Accept"); headers.hasMoreElements(); ) {
			String headerString = (String)headers.nextElement();
			for (String header: headerString.split(",\\s*")) {
				contentType = ContentType.getContentType(header);
				if (contentType != null)
					return contentType;
			}
		}
		return ContentType.RDF_XML;
	}

	/**
	 * Process a service call.
	 * This method is overridden by SynchronousServiceServlet and
	 * AsynchronousServiceServlet as appropriate.
	 * @param call
	 * @throws Exception
	 */
	protected void processInput(ServiceCall call) throws Exception
	{
		// populate parameter instance, list of input instances, etc.
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
	
	protected void outputServiceModel(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		/* output the service model using the request URL as the base for an
		 * relative URIs...
		 */
		ContentType contentType = ContentType.getContentType(response.getContentType());
		if (contentType.equals(ContentType.RDF_XML)) {
			response.getWriter().println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
			response.getWriter().println("<?xml-stylesheet type=\"text/xsl\" href=\"?xsl\" ?>");
		}
		contentType.writeModel(serviceModel, response.getWriter(), request.getRequestURL().toString());
	}
	
	protected void outputSuccessResponse(HttpServletResponse response, Model outputModel) throws IOException
	{
		/* TODO add a mechanism to specify a web-accessible location on disk
		 * where the servlet can dump the output model and redirect to it...
		 */
		ContentType contentType = ContentType.getContentType(response.getContentType());
		RDFWriter writer = outputModel.getWriter(contentType.getJenaLanguage());
		QueryableErrorHandler errorHandler = new QueryableErrorHandler();
		writer.setErrorHandler(errorHandler);
		writer.write(outputModel, response.getWriter(), "");
		if (errorHandler.hasLastError()) {
			Exception e = errorHandler.getLastError();
			String message = String.format("error writing output RDF: %s", e.getMessage());
			log.error(message, e);
			throw new IOException(message);
		}
	}
	
	protected void outputErrorResponse(HttpServletResponse response, Throwable error) throws IOException
	{
		/* we can't just write to the response because Jena calls flush() on
		 * the writer or stream, which commits the response...
		 */
//		Model errorModel = ExceptionUtils.createExceptionModel(error);
		Model errorModel = ModelFactory.createDefaultModel();
		errorModel.add(errorModel.createResource(), SADI.error, error.toString());
		
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		ContentType contentType = ContentType.getContentType(response.getContentType());
		contentType.writeModel(errorModel, response.getWriter(), "");
	}
	
	protected void outputXSL(HttpServletResponse response) throws IOException
	{
		response.setContentType("application/xml");
		
		BufferedReader in = HttpUtils.getReader(SERVICE_DEFINITION_STYLESHEET);
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			response.getWriter().println(line);
		}
		in.close();
	}
	
	protected ModelMaker createModelMaker()
	{
		return ModelFactory.createMemModelMaker();
	}
	
	protected Model createServiceModel()
	{
		Model model = modelMaker.createFreshModel();
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

	protected void cleanupServiceCall(ServiceCall call)
	{
		if (call.getInputModel() != null)
			closeInputModel(call.getInputModel());
		if (call.getOutputModel() != null)
			closeOutputModel(call.getOutputModel());
	}
	
	protected ServiceOntologyHelper getServiceOntologyHelper()
	{
		return serviceOntologyHelper;
	}
	
	protected Resource getInputClass()
	{
		return inputClass;
	}
	
	protected Resource getOutputClass()
	{
		return outputClass;
	}
	
	protected Resource getParameterClass()
	{
		return parameterClass != null ? parameterClass : OWL.Nothing;
	}
	
	protected Resource getDefaultParameters()
	{
		return defaultParameters;
	}
	
	/**
	 * Creates the service description from the information available.
	 * The sources of information, in increasing order of priority, are:
	 *   the service configuration from the userspace properties file;
	 *   annotations on the servlet class itself;
	 * Information from a higher priority source will override lower
	 * priority sources; for example, an input class specified in an
	 * annotation will override an input class specified in the properties
	 * file.
	 */
	protected ServiceDescription createServiceDescription() throws SADIException
	{
		ServiceBean serviceBean = new ServiceBean();
		serviceBean.setURI(StringUtils.defaultString(getServiceURL(), ""));
		serviceBean.setName(getName());
		serviceBean.setDescription(getDescription());
		serviceBean.setServiceProvider(getServiceProvider());
		serviceBean.setContactEmail(getContactEmail());
		serviceBean.setAuthoritative(isAuthoritative());
		serviceBean.setInputClassURI(getInputClassURI());
		serviceBean.setOutputClassURI(getOutputClassURI());
		serviceBean.setParameterClassURI(getParameterClassURI());
		return serviceBean;
	}
	
	private Resource extractDefaultParameterInstanceFromModel() throws SADIException
	{
		RDFPath parameterInstancePath = ((AbstractServiceOntologyHelper)serviceOntologyHelper).getParameterInstancePath();
		Collection<Resource> instances = RdfUtils.extractResources(parameterInstancePath.getValuesRootedAt(serviceModel.getResource(getServiceURL())));
		if (instances.size() > 1) {
			throw new ServiceDefinitionException(String.format("found %d default parameter instances, refusing to pick one arbitrarily", instances.size()));
		} else if (instances.isEmpty()) {
			return null;
		} else {
			Resource parameters = instances.iterator().next();
			if (parameters.isURIResource() && !parameters.listProperties().hasNext()) {
				try {
					serviceModel.read(parameters.getURI());
					if (errorHandler.hasLastError())
						throw errorHandler.getLastError();
				} catch (Exception e) {
					String message = String.format("error reading default parameters from %s: %s", parameters.getURI(), e.getMessage());
					log.error(message, e);
					throw new SADIException(message);
				}
			}
			return parameters;
		}
	}
	
	private Resource createDefaultParameterInstance() throws SADIException
	{
		RDFPath parameterInstancePath = ((AbstractServiceOntologyHelper)serviceOntologyHelper).getParameterInstancePath();
		Resource parameters;
		List<String> spec = Arrays.asList(getDefaultParameterSpec());
		if (spec.size() == 1) {
			// assume this is the URL of a populated instance...
			parameters = serviceModel.createResource(spec.get(0), getParameterClass());
			try {
				serviceModel.read(parameters.getURI());
				if (errorHandler.hasLastError())
					throw errorHandler.getLastError();
			} catch (Exception e) {
				String message = String.format("error reading default parameters from %s: %s", parameters.getURI(), e.getMessage());
				log.error(message, e);
				throw new SADIException(message);
			}
		} else {
			parameters = serviceModel.createResource("#parameters", getParameterClass());
			Iterator<String> i = spec.iterator();
			while (i.hasNext()) {
				String pathSpec = i.next();
				String value;
				if (i.hasNext())
					value = i.next();
				else
					throw new SADIException("invalid default parameter spec; expected [path, value, path, value, ...]");

				RDFPath path;
				try {
					path = new RDFPath(StringUtils.split(pathSpec, ", <>"));
				} catch (Exception e) {
					throw new SADIException(String.format("invalid path specification \"%s\": %s", pathSpec, e.getMessage()));
				}

				if (RdfUtils.isURI(value) || value.startsWith("#")) { // this may not be sufficient...
					path.createResourceRootedAt(parameters, value);
				} else {
					path.createLiteralRootedAt(parameters, value);
				}
			}
		}
		parameterInstancePath.addValueRootedAt(serviceModel.getResource(getServiceURL()), parameters);
		return parameters;
	}
	
	private Resource createParameters(Model inputModel)
	{
		Resource parameters;
		ResIterator i = inputModel.listResourcesWithProperty(RDF.type, getParameterClass());
		if (i.hasNext())
			parameters = i.next();
		else
			parameters = inputModel.createResource(null, getParameterClass());
		if (i.hasNext()) {
			// TODO throw an exception instead?
			log.warn(String.format("input contained more than one instance of parameter class %s", getParameterClass()));
		}
		i.close();
		
		Resource defaults = getDefaultParameters();
		if (defaults != null) {
			// TODO inefficient; could cache leaf paths/values...
			Collection<RDFPath> leafPaths = RDFPathUtils.getLeafPaths(defaults);
			for (RDFPath leafPath: leafPaths){ 
				if (leafPath.getValuesRootedAt(parameters).isEmpty())
					leafPath.addValuesRootedAt(parameters, leafPath.getValuesRootedAt(defaults));
			}
		}
		
		return parameters;
	}
	
	private Resource loadServiceModelFromLocation(String serviceRDF) throws SADIException
	{
		String serviceURL = getServiceURL();
		try {
			readIntoModel(serviceModel, StringUtils.defaultString(serviceURL), serviceRDF);
			if (errorHandler.hasLastError())
				throw errorHandler.getLastError();
		} catch (Exception e) {
			String message = String.format("error reading service description from %s: %s", serviceRDF, e.getMessage());
			log.error(message, e);
			throw new SADIException(message);
		}
		if (serviceURL == null) {
			/* if there's exactly one instance of the service class in the
			 * model, we can assume that's us; if not, we have a problem...
			 */
			ResIterator services = serviceModel.listResourcesWithProperty(RDF.type, getServiceOntologyHelper().getServiceClass());
			try {
				if (services.hasNext()) {
					serviceURL = services.next().getURI();
					if (services.hasNext())
						throw new ServiceDefinitionException(String.format("no service URI specified and the model at %s contains multiple instances of service class %s", serviceRDF, serviceOntologyHelper.getServiceClass()));
				} else {
					throw new ServiceDefinitionException(String.format("no service URI specified and the model at %s contains no instances of service class %s", serviceRDF, serviceOntologyHelper.getServiceClass()));
				}
			} finally {
				services.close();
			}
		}
		return serviceModel.getResource(serviceURL);
	}
	
	/**
	 * Reads RDF from the specified location into the service model.
	 * The location can be an absolute URL, a path relative to the
	 * classpath or a path relative to the working directory.
	 * @param pathOrURL
	 */
	private void readIntoModel(Model model, String base, String pathOrURL)
	{
		try {
			URL url = new URL(pathOrURL);
			log.debug(String.format("identified %s as a URL", pathOrURL));
			model.read(url.toString());
		} catch (MalformedURLException e) {
			log.debug(String.format("%s is not a URL: %s", pathOrURL, e.getMessage()));
		}
		log.debug(String.format("identified %s as a path", pathOrURL));
		
		InputStream is = getClass().getResourceAsStream(pathOrURL);
		if (is != null) {
			log.debug(String.format("found %s in the classpath", pathOrURL));
			try {
				model.read(is, base);
			} catch (JenaException e) {
				log.error(String.format("error reading service description from %s: %s", pathOrURL, e.getMessage()));
			}
		} else {
			log.debug(String.format("looking for %s in the filesystem", pathOrURL));
			try {
				File f = new File(pathOrURL);
				model.read(new FileInputStream(f), base);
			} catch (FileNotFoundException e) {
				log.error(String.format("error reading service description from %s: %s", pathOrURL, e.toString()));
			}
		}
	}
	
	/* annoyingly, these methods can't be generalized because annotation
	 * interfaces can't have superclasses or superinterfaces, so there's no
	 * way to access the value() method generically...
	 * actually, I suppose one could use reflection, but that seems kind of
	 * gross when I can just macro these methods into being...
	 */
	private String getServiceRDF()
	{
		ServiceDefinition annotation = getClass().getAnnotation(ServiceDefinition.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getString(SERVICE_DEFINITION_KEY);
		} else {
			return null;
		}
	}
	
	protected String getServiceURL()
	{
		if (ignoreForcedURL)
			return null;
		
		URI annotation = getClass().getAnnotation(URI.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getString(SERVICE_URL_KEY);
		} else {
			return null;
		}
	}
	
	private String getName()
	{
		Name annotation = getClass().getAnnotation(Name.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getString(NAME_KEY);
		} else {
			return null;
		}
	}

	private String getDescription()
	{
		Description annotation = getClass().getAnnotation(Description.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getString(DESCRIPTION_KEY);
		} else {
			return null;
		}
	}

	private String getServiceProvider()
	{
		ServiceProvider annotation = getClass().getAnnotation(ServiceProvider.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getString(SERVICE_PROVIDER_KEY);
		} else {
			return null;
		}
	}

	private String getContactEmail()
	{
		ContactEmail annotation = getClass().getAnnotation(ContactEmail.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getString(CONTACT_EMAIL_KEY);
		} else {
			return null;
		}
	}

	private boolean isAuthoritative()
	{
		Authoritative annotation = getClass().getAnnotation(Authoritative.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getBoolean(AUTHORITATIVE_KEY, false);
		} else {
			return false;
		}
	}

	private String getInputClassURI() throws SADIException
	{
		InputClass annotation = getClass().getAnnotation(InputClass.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getString(INPUT_CLASS_KEY);
		} else {
			throw new SADIException("no input class specified");
		}
	}

	private String getOutputClassURI() throws SADIException
	{
		OutputClass annotation = getClass().getAnnotation(OutputClass.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getString(OUTPUT_CLASS_KEY);
		} else {
			throw new SADIException("no output class specified");
		}
	}

	private String getParameterClassURI()
	{
		ParameterClass annotation = getClass().getAnnotation(ParameterClass.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getString(PARAMETER_CLASS_KEY);
		} else {
			return null;
		}
	}
	
	private String[] getDefaultParameterSpec()
	{
		ParameterDefaults annotation = getClass().getAnnotation(ParameterDefaults.class);
		if (annotation != null) {
			return annotation.value();
		} else if (config != null) {
			return config.getStringArray(PARAMETER_DEFAULTS_KEY);
		} else {
			return null;
		}
	}
}
