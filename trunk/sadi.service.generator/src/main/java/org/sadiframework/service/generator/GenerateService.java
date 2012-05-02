package org.sadiframework.service.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.sadiframework.service.Config;
import org.sadiframework.service.ServiceServlet;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.sadiframework.SADIException;
import org.sadiframework.ServiceDescription;
import org.sadiframework.beans.ServiceBean;
import org.sadiframework.service.ServiceDefinitionException;
import org.sadiframework.service.ontology.MyGridServiceOntologyHelper;
import org.sadiframework.service.ontology.ServiceOntologyHelper;
import org.sadiframework.service.validation.ServiceValidator;
import org.sadiframework.service.validation.ValidationResult;
import org.sadiframework.service.validation.ValidationWarning;
import org.sadiframework.utils.OwlUtils;
import org.sadiframework.utils.SPARQLStringUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * A goal that generates the skeleton of a SADI service.
 * @author Luke McCarthy
 * @goal generate-service
 */
public class GenerateService extends AbstractMojo
{
	private static final String SERVICE_PROPERTIES = "src/main/resources/sadi.properties";
	private static final String SOURCE_DIRECTORY = "src/main/java";
	private static final String WEB_XML_PATH = "src/main/webapp/WEB-INF/web.xml";
	private static final String INDEX_PATH = "src/main/webapp/index.jsp";
	
	/**
	 * The name of the service, which will also be used in the path to the
	 * service servlet. This parameter is required.
	 * @parameter expression="${serviceName}"
	 */
	private String serviceName;
	private static final String SERVICE_NAME_KEY = "serviceName"; // different than properties
	
	/**
	 * The fully-qualified name of the Java class that will implement the
	 * service. This parameter is required.
	 * @parameter expression="${serviceClass}"
	 */
	private String serviceClass;
	private static final String SERVICE_CLASS_KEY = "serviceClass";
	
	/**
	 * The URL of the service. This parameter is optional and not normally
	 * required, except in certain baroque network configurations.
	 * @parameter expression="${serviceURL}"
	 */
	private String serviceURL;
//	private static final String SERVICE_URL_KEY = "serviceURL"; // different than properties
	
	/**
	 * A URL or local path to a service description in RDF. This parameter is
	 * optional, but can be used instead of specifying all of the other
	 * parameters separately.
	 * @parameter expression="${serviceRDF}"
	 */
	private String serviceRDF;
//	private static final String SERVICE_RDF_KEY = "serviceRDF"; // different than properties
	
	/**
	 * The service description. This parameter is optional.
	 * @parameter expression="${serviceDescription}"
	 */
	private String description;
//	private static final String SERVICE_DESCRIPTION_KEY = "serviceDescription"; // different than properties
	
	/**
	 * The service provider. This parameter is optional.
	 * @parameter expression="${serviceProvider}"
	 */
	private String serviceProvider;
//	private static final String SERVICE_PROVIDER_KEY = "serviceProvider";
	
	/**
	 * A contact email address for the service. This parameter is required.
	 * @parameter expression="${contactEmail}"
	 */
	private String contactEmail;
//	private static final String CONTACT_EMAIL_KEY = "contactEmail";
	
	/**
	 * Whether or not the service is authoritative. This parameter is optional,
	 * defaulting to false.
	 * @parameter expression="${authoritative}" default-value="false"
	 */
	private boolean authoritative;
//	private static final String AUTHORITATIVE_KEY = "authoritative";
	
	
	/**
	 * Whether or not the service is asynchronous.  This parameter is optional,
	 * defaulting to false.
	 * @parameter expression="${async}" default-value="false"
	 */
	private boolean async;
	
	/**
	 * The URI of the service input class. This parameter is required
	 * and the URI must resolve to an OWL class definition.
	 * @parameter expression="${inputClass}"
	 */
	private String inputClassURI;
//	private static final String INPUT_CLASS_KEY = "inputClass";
	
	/**
	 * The URI of the service output class. This parameter is required
	 * and the URI must resolve to an OWL class definition.
	 * @parameter expression="${outputClass}"
	 */
	private String outputClassURI;
//	private static final String OUTPUT_CLASS_KEY = "outputClass";
	
	/**
	 * The URI of the service parameter class. This parameter is optional,
	 * but if specified the URI must resolve to an OWL class definition.
	 * @parameter expression="${parameterClass}"
	 */
	private String parameterClassURI;
//	private static final String PARAMETER_CLASS_KEY = "parameterClass";
	
//	TODO Eddie may set these properties from the Protege plugin...
//	/**
//	 * inline RDF or a URI that must resolve.
//	 * @parameter expression="${testInput}"
//	 */
//	private String testInput;
//	
//	/**
//	 * inline RDF or a URI that must resolve.
//	 * @parameter expression="${testOutput}"
//	 */
//	private String testOutput;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
//		serviceName = System.getProperty(SERVICE_NAME_KEY);
		if (serviceName == null)
			throw new MojoFailureException(String.format("missing required property %s", SERVICE_NAME_KEY));
		
//		serviceClass = System.getProperty(SERVICE_CLASS_KEY);
		if (serviceClass == null)
			throw new MojoFailureException(String.format("missing required property %s", SERVICE_CLASS_KEY));

		/* initialize the service description with the specified URL, if any,
		 * because we might need that to read an RDF description...
		 */
		ServiceBean serviceBean = new ServiceBean();
//		serviceURL = System.getProperty(SERVICE_URL_KEY);
		if (serviceURL != null)
			serviceBean.setURI(serviceURL);
		
		/* first, check to see if this is an existing service; if so,
		 * populate the description with values from the existing config...
		 * note that we only do this for backwards compatibility; we're not
		 * writing new properties files anymore...
		 */
		Config config = new Config(SERVICE_PROPERTIES);
		Configuration serviceConfig = config.getServiceConfiguration(serviceClass);
		if (serviceConfig != null) {
			loadServiceDescriptionFromConfig(serviceBean, serviceConfig);
		}
		
//		serviceRDF = System.getProperty(SERVICE_RDF_KEY);
		/* next, if an RDF service description has been specified, populate
		 * values from that.
		 */
		if (serviceRDF != null) {
			try {
				loadServiceDescriptionFromLocation(serviceBean, serviceRDF);
			} catch (SADIException e) {
				throw new MojoFailureException(e.getMessage());
			}
		}
		
		/* last, populate values from the defined properties...
		 */
		loadServiceDescriptionFromProperties(serviceBean);
		
		/* make sure all of the required values are there...
		 */
		try {
			validateServiceDescription(serviceBean);
		} catch (SADIException e) {
			throw new MojoFailureException(e.getMessage());
		}
		
		/* load the input and output classes...
		 */
		OntModel model = null;
		try {
			model = OwlUtils.createDefaultReasoningModel();
		} catch (SADIException e) {
			getLog().warn(String.format("error configuring reasoner: %s", e.getMessage()));
			model = ModelFactory.createOntologyModel();
		}
		
		OntClass inputClass = null;
		try {
			inputClass = OwlUtils.getOntClassWithLoad(model, serviceBean.getInputClassURI());
		} catch (SADIException e) {
			throw new MojoFailureException(e.getMessage());
		}
		if (inputClass == null)
			throw new MojoFailureException(String.format("input class URI %s does not resolve to a class definition", serviceBean.getInputClassURI()));
		
		OntClass outputClass = null;
		try {
			outputClass = OwlUtils.getOntClassWithLoad(model, serviceBean.getOutputClassURI());
		} catch (SADIException e) {
			throw new MojoFailureException(e.getMessage());
		}
		if (outputClass == null)
			throw new MojoFailureException(String.format("output class URI %s does not resolve to a class definition", serviceBean.getOutputClassURI()));
		
		MavenProject project = (MavenProject)getPluginContext().get("project");
		File basePath = project != null ? project.getBasedir() : new File(".").getAbsoluteFile();
		getLog().info("generating service files relative to " + basePath);
		
		/* create class file...
		 */
		File classFile = new File(basePath, String.format("%s/%s.java", SOURCE_DIRECTORY, serviceClass.replace(".", "/")));
		if (classFile.exists()) {
			try {
				backupClassFile(classFile);
			} catch (IOException e) {
				throw new MojoFailureException("failed to backup existing Java file: " + e.getMessage());
			}
		}
		try {
			writeClassFile(classFile, serviceClass, inputClass, outputClass, serviceBean, async);
		} catch (Exception e) {
			String message = String.format("failed to write new java file for %s", serviceClass);
			getLog().error(message, e);
			throw new MojoExecutionException(message);
		}
		
		serviceName = getSimpleServiceName(serviceName);
		
		/* write new web.xml...
		 */
		File webxmlPath = new File(basePath, WEB_XML_PATH);
		WebXmlParser webxml = new WebXmlParser();
		if (webxmlPath.exists()) {
			try {
				webxml.parse(webxmlPath);
			} catch (Exception e) {
				throw new MojoFailureException("failed to parse existing web.xml: " + e.getMessage());
			}
		}
		if (webxml.name2class.containsKey(serviceName)) {
			getLog().info(String.format("web.xml contains previous definition for servlet %s; it will be overwritten", serviceName));
		} else {
			getLog().info(String.format("adding servlet %s to web.xml", serviceName));
			webxml.name2class.put(serviceName, serviceClass);
			try {
				webxml.name2url.put(serviceName, String.format("/%s", URLEncoder.encode(serviceName, "UTF-8")));
			} catch (UnsupportedEncodingException e) {
				// this should never happen
				throw new MojoFailureException("failed to URL-encode service name: " + e.getMessage());
			}
		}
		try {
			writeWebXml(webxmlPath, webxml);
		} catch (Exception e) {
			throw new MojoExecutionException("failed to write web.xml", e);
		}

		// write new index.jsp...
		try {
			writeIndex(new File(basePath, INDEX_PATH), webxml.name2url);
		} catch (Exception e) {
			throw new MojoExecutionException("failed to write index.jsp", e);
		}
		
		/* if there was a legacy properties file, remove this service... 
		 */
		if (serviceConfig != null) {
			try {
				serviceConfig.clear();
				writeProperties(basePath, ((SubsetConfiguration)serviceConfig).getParent(), SERVICE_PROPERTIES);
			} catch (IOException e) {
				getLog().warn(String.format("failed to write new properties file %s: %s", SERVICE_PROPERTIES, e.getMessage()), e);
			}
		}
	}

	private String getSimpleServiceName(String name)
	{
		String newName = Pattern.compile("[^\\w-]").matcher(name).replaceAll(" ");
		if (!newName.equals(name)) {
			newName = WordUtils.capitalizeFully(newName);
			newName = Pattern.compile("\\s+").matcher(newName).replaceAll("");
		}
		return newName;
	}

	private void loadServiceDescriptionFromConfig(ServiceBean serviceBean, Configuration serviceConfig)
	{
		serviceBean.setName(serviceConfig.getString(ServiceServlet.NAME_KEY));
		serviceBean.setDescription(serviceConfig.getString(ServiceServlet.DESCRIPTION_KEY));
		serviceBean.setServiceProvider(serviceConfig.getString(ServiceServlet.SERVICE_PROVIDER_KEY));
		serviceBean.setContactEmail(serviceConfig.getString(ServiceServlet.CONTACT_EMAIL_KEY));
		serviceBean.setAuthoritative(serviceConfig.getBoolean(ServiceServlet.AUTHORITATIVE_KEY, false));
		serviceBean.setInputClassURI(serviceConfig.getString(ServiceServlet.INPUT_CLASS_KEY));
		serviceBean.setOutputClassURI(serviceConfig.getString(ServiceServlet.OUTPUT_CLASS_KEY));
		serviceBean.setParameterClassURI(serviceConfig.getString(ServiceServlet.PARAMETER_CLASS_KEY));
	}
	
	private void loadServiceDescriptionFromLocation(ServiceBean serviceBean, String serviceRDF) throws SADIException
	{
		String serviceURL = serviceBean.getURI();
		ServiceOntologyHelper serviceOntologyHelper = new MyGridServiceOntologyHelper();
		Model serviceModel = ModelFactory.createDefaultModel();
		try {
			readIntoModel(serviceModel, StringUtils.defaultString(serviceURL), serviceRDF);
		} catch (Exception e) {
			String message = String.format("error reading service description from %s: %s", serviceRDF, e.getMessage());
			getLog().error(message, e);
			throw new SADIException(message);
		}
		if (serviceURL == null) {
			/* if there's exactly one instance of the service class in the
			 * model, we can assume that's us; if not, we have a problem...
			 */
			ResIterator services = serviceModel.listResourcesWithProperty(RDF.type, serviceOntologyHelper.getServiceClass());
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
		Resource serviceNode = serviceModel.getResource(serviceURL);
		serviceOntologyHelper.copyServiceDescription(serviceNode, serviceBean);
	}

	private void loadServiceDescriptionFromProperties(ServiceBean serviceBean)
	{
//		serviceName = System.getProperty(SERVICE_NAME_KEY);
		if (serviceName != null)
			serviceBean.setName(serviceName);
//		description = System.getProperty(SERVICE_DESCRIPTION_KEY);
		if (description != null)
			serviceBean.setDescription(description);
//		serviceProvider = System.getProperty(SERVICE_PROVIDER_KEY);
		if (serviceProvider != null)
			serviceBean.setServiceProvider(serviceProvider);
//		contactEmail = System.getProperty(CONTACT_EMAIL_KEY);
		if (contactEmail != null)
			serviceBean.setContactEmail(contactEmail);
//		authoritative = System.getProperty(AUTHORITATIVE_KEY);
//		if (authoritative != null)
			serviceBean.setAuthoritative(Boolean.valueOf(authoritative));
//		inputClassURI = System.getProperty(INPUT_CLASS_KEY);
		if (inputClassURI != null)
			serviceBean.setInputClassURI(inputClassURI);
//		outputClassURI = System.getProperty(OUTPUT_CLASS_KEY);
		if (outputClassURI != null)
			serviceBean.setOutputClassURI(outputClassURI);
//		parameterClassURI = System.getProperty(PARAMETER_CLASS_KEY);
		if (parameterClassURI != null)
			serviceBean.setParameterClassURI(parameterClassURI);
	}

	private void validateServiceDescription(ServiceBean serviceBean) throws SADIException
	{
		boolean unsetURI = false;
		if (serviceBean.getURI() == null) {
			serviceBean.setURI("");
			unsetURI = true;
		}
		Model model = ModelFactory.createDefaultModel();
		try {
			Resource serviceNode = new MyGridServiceOntologyHelper().createServiceNode(serviceBean, model);
			ValidationResult result = ServiceValidator.validateService(serviceNode);
			for (ValidationWarning warning: result.getWarnings()) {
				getLog().warn(warning.getMessage());
			}
		} finally {
			model.close();
		}
		if (unsetURI) {
			serviceBean.setURI(null);
		}
	}
	
	/**
	 * Reads RDF from the specified location into the specified model.
	 * The location can be an absolute URL, a path relative to the
	 * classpath or a path relative to the working directory.
	 * @param pathOrURL
	 */
	private void readIntoModel(Model model, String base, String pathOrURL)
	{
		try {
			URL url = new URL(pathOrURL);
			getLog().debug(String.format("identified %s as a URL", pathOrURL));
			model.read(url.toString());
		} catch (MalformedURLException e) {
			getLog().debug(String.format("%s is not a URL: %s", pathOrURL, e.getMessage()));
		}
		getLog().debug(String.format("identified %s as a path", pathOrURL));
		
		InputStream is = getClass().getResourceAsStream(pathOrURL);
		if (is != null) {
			getLog().debug(String.format("found %s in the classpath", pathOrURL));
			try {
				model.read(is, base);
			} catch (JenaException e) {
				getLog().error(String.format("error reading service description from %s: %s", pathOrURL, e.getMessage()));
			}
		} else {
			getLog().debug(String.format("looking for %s in the filesystem", pathOrURL));
			try {
				File f = new File(pathOrURL);
				model.read(new FileInputStream(f), base);
			} catch (FileNotFoundException e) {
				getLog().error(String.format("error reading service description from %s: %s", pathOrURL, e.toString()));
			}
		}
	}

	private void backupClassFile(File classFile) throws IOException
	{
		File newFile = classFile;
		while (newFile.exists())
			newFile = new File(getNextString(newFile.getAbsolutePath()));
		if (!classFile.renameTo(newFile)) {
			throw new IOException(String.format("failed to backup Java file %s to %s", classFile, newFile));
		}
	}

	private String getNextString(String s)
	{
		int i = s.length();
		while (i > 0 && Character.isDigit(s.charAt(i-1)))
			i--;
		String prefix = s.substring(0, i);
		String suffix = s.substring(i);
		return suffix.isEmpty() ?
			String.format("%s.1", s) : 
			String.format("%s%d", prefix, Integer.valueOf(suffix) + 1);
	}
	
	private void writeClassFile(File classFile, String serviceClass, OntClass inputClass, OntClass outputClass, ServiceDescription serviceDescription, boolean async) throws Exception
	{
		/* create a copy of the service description where we can escape stuff...
		 */
		ServiceBean escaped = new ServiceBean();
		escaped.setName(StringEscapeUtils.escapeJava(serviceDescription.getName()));
		escaped.setDescription(StringEscapeUtils.escapeJava(serviceDescription.getDescription()));
		escaped.setServiceProvider(StringEscapeUtils.escapeJava(serviceDescription.getServiceProvider()));
		escaped.setContactEmail(StringEscapeUtils.escapeJava(serviceDescription.getContactEmail()));
		escaped.setAuthoritative(serviceDescription.isAuthoritative());
		escaped.setInputClassURI(serviceDescription.getInputClassURI());
		escaped.setOutputClassURI(serviceDescription.getOutputClassURI());
		escaped.setParameterClassURI(serviceDescription.getParameterClassURI());
		
		/* collect the properties and classes for the Vocab class...
		 */
		Set<OntProperty> properties = new HashSet<OntProperty>();
		Set<OntClass> classes = new HashSet<OntClass>();
		collect(inputClass, properties, classes);
		collect(outputClass, properties, classes);
		
		/* write the file...
		 */
		createPath(classFile);
		FileWriter writer = new FileWriter(classFile);
		String template = SPARQLStringUtils.readFully(GenerateService.class.getResourceAsStream("templates/ServiceServletSkeleton"));
		VelocityContext context = new VelocityContext();
		context.put("description", escaped);
		context.put("package", StringUtils.substringBeforeLast(serviceClass, "."));
		context.put("class", StringUtils.substringAfterLast(serviceClass, "."));
		context.put("properties", properties);
		context.put("classes", classes);
		context.put("async", async);
		Velocity.init();
		Velocity.evaluate(context, writer, "SADI", template);
		writer.close();
	}
	
	private void collect(OntClass c, Collection<OntProperty> properties, Collection<OntClass> classes)
	{
		collect(c, properties, classes, new HashSet<OntClass>());
	}
	private void collect(OntClass c, Collection<OntProperty> properties, Collection<OntClass> classes, Set<OntClass> seen)
	{
		if (seen.contains(c))
			return;
		else
			seen.add(c);
		if (c.isURIResource())
			classes.add(c);
		for (Restriction r: OwlUtils.listRestrictions(c)) {
			OntProperty p = r.getOnProperty();
			if (p.isURIResource())
				properties.add(p);
			OntClass valuesFrom = OwlUtils.getValuesFromAsClass(r);
			if (valuesFrom != null) {
				collect(valuesFrom, properties, classes, seen);
			}
		}
	}

	private void writeProperties(File base, Configuration config, String propertiesPath) throws IOException
	{
		PropertiesConfiguration properties = new PropertiesConfiguration();
		properties.append(config);
		
		File outfile = new File(base, propertiesPath);
		createPath(outfile);
		FileWriter writer = new FileWriter(outfile);
		try {
			properties.save(writer);
		} catch (ConfigurationException e) {
			throw new IOException(e.getMessage());
		} finally {
			writer.close();
		}
	}

	private void writeWebXml(File webXml, WebXmlParser webxml) throws Exception
	{
		createPath(webXml);
		FileWriter writer = new FileWriter(webXml);
		String template = FileUtils.readWholeFileAsUTF8(GenerateService.class.getResourceAsStream("templates/webXmlSkeleton"));
		VelocityContext context = new VelocityContext();
		context.put("name2class", webxml.name2class);
		context.put("name2url", webxml.name2url);
		Velocity.init();
		Velocity.evaluate(context, writer, "SADI", template);
		writer.close();
	}

	private void writeIndex(File index, Map<String, String> name2url) throws Exception
	{
		createPath(index);
		FileWriter writer = new FileWriter(index);
		String template = FileUtils.readWholeFileAsUTF8(GenerateService.class.getResourceAsStream("templates/indexSkeleton"));
		VelocityContext context = new VelocityContext();
		
		context.put("servlets", name2url);
		Velocity.init();
		Velocity.evaluate(context, writer, "SADI", template);
		writer.close();
	}

	private static void createPath(File outfile) throws IOException
	{
		File parent = outfile.getParentFile();
		if (parent != null && !parent.isDirectory())
			if (!parent.mkdirs())
				throw new IOException(String.format("unable to create directory path ", parent));
	}
	
	private static class WebXmlParser extends DefaultHandler
	{
		Map<String, String> name2class;
		Map<String, String> name2url;

		public WebXmlParser()
		{
			name2class = new HashMap<String, String>();
			name2url = new HashMap<String, String>();
		}

		public void parse(File webxmlPath) throws Exception
		{
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setValidating(false);
			SAXParser sp = spf.newSAXParser();
			InputSource input = new InputSource(new FileReader(webxmlPath));
			input.setSystemId("file://" + webxmlPath.getAbsolutePath());
			sp.parse(input, this);
		}

		private StringBuffer accumulator = new StringBuffer();
		private String servletName;
		private String servletClass;
		private String servletUrl;

		@Override
		public void characters(char[] buffer, int start, int length)
		{
			accumulator.append(buffer, start, length);
		}

		@Override
		public void endElement (String uri, String localName, String qName) throws SAXException
		{
			if (localName.equals("servlet-name") || qName.equals("servlet-name")) {
				servletName = accumulator.toString().trim();
			} else if (localName.equals("servlet-class") || qName.equals("servlet-class")) {
				servletClass = accumulator.toString().trim();
			} else if (localName.equals("url-pattern") || qName.equals("url-pattern")) {
				servletUrl = accumulator.toString().trim();
			} else if (localName.equals("servlet") || qName.equals("servlet")) {
				name2class.put(servletName, servletClass);
			} else if (localName.equals("servlet-mapping") || qName.equals("servlet-mapping")) {
				name2url.put(servletName, servletUrl);
			}
			accumulator.setLength(0);
		}
	}
}
