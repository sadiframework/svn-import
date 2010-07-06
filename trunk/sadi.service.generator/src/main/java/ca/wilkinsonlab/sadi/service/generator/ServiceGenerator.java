package ca.wilkinsonlab.sadi.service.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.service.Config;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;

/**
 * A Maven plugin to generate the skeleton of a SADI service.
 * @author Luke McCarthy
 * @goal generate-service
 */
public class ServiceGenerator extends AbstractMojo
{
	private static final String SERVICE_NAME_KEY = "serviceName";
	private static final String SERVICE_CLASS_KEY = "serviceClass";
	private static final String SERVICE_DESCRIPTION_KEY = "serviceDescription";
	private static final String SERVICE_URL_KEY = "serviceUrl";
	private static final String INPUT_CLASS_KEY = "inputClass";
	private static final String OUTPUT_CLASS_KEY = "outputClass";
	
	private static final String SERVICE_PROPERTIES = "src/main/resources/sadi.properties";
	private static final String SOURCE_DIRECTORY = "src/main/java";
	private static final String WEB_XML_PATH = "src/main/webapp/WEB-INF/web.xml";
	private static final String INDEX_PATH = "src/main/webapp/index.jsp";
	
	/**
	 * Expected properties:
	 * 	serviceName (required)
	 *  serviceClass (required)
	 *  serviceDescription (optional)
	 *  serviceURL (optional)
	 *  inputClassURI (required) the URI must resolve to the class description
	 *  outputClassURI (required) the URI must resolve to the class description
	 */
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		
		String serviceName = System.getProperty(SERVICE_NAME_KEY);
		if (serviceName == null)
			throw new MojoFailureException(String.format("missing required property %s", SERVICE_NAME_KEY));
		
		String serviceClass = System.getProperty(SERVICE_CLASS_KEY);
		if (serviceClass == null)
			throw new MojoFailureException(String.format("missing required property %s", SERVICE_CLASS_KEY));

		String inputClassURI = System.getProperty(INPUT_CLASS_KEY);
		if (inputClassURI == null)
			throw new MojoFailureException(String.format("missing required property %s", INPUT_CLASS_KEY));
		
		OntClass inputClass = null;
		try {
			inputClass = OwlUtils.getOntClassWithLoad(model, inputClassURI);
		} catch (SADIException e) {
			throw new MojoFailureException(e.getMessage());
		}
		if (inputClass == null)
			throw new MojoFailureException(String.format("undefined input class %s", inputClassURI));

		String outputClassURI = System.getProperty(OUTPUT_CLASS_KEY);
		if (outputClassURI == null)
			throw new MojoFailureException(String.format("missing required property %s", OUTPUT_CLASS_KEY));
		
		OntClass outputClass = null;
		try {
			outputClass = OwlUtils.getOntClassWithLoad(model, outputClassURI);
		} catch (SADIException e) {
			throw new MojoFailureException(e.getMessage());
		}
		if (outputClass == null)
			throw new MojoFailureException(String.format("undefined output class %s", outputClassURI));

		String serviceDescription = System.getProperty(SERVICE_DESCRIPTION_KEY);
		String serviceURL = System.getProperty(SERVICE_URL_KEY);
		
		// find existing service config, or create a new one...
		Config config = Config.getConfiguration(SERVICE_PROPERTIES);
		Configuration serviceConfig;
		try {
			serviceConfig = config.getServiceConfiguration(serviceClass);
		} catch (ConfigurationException e) {
			serviceConfig = config.subset(String.format("%s.%s", Config.SERVICE_SUBSET_KEY, serviceName));
			if (!serviceConfig.isEmpty()) {
				StringBuffer buf = new StringBuffer("overwriting previous service definition");
				for (Iterator<?> i = serviceConfig.getKeys(); i.hasNext(); ) {
					String key = (String)i.next();
					buf.append("\n\t");
					buf.append(key);
					buf.append("\t: ");
					buf.append(serviceConfig.getString(key));
				}
				getLog().warn(buf.toString());
			}
		}
		
		// update service properties...
		serviceConfig.setProperty("", serviceClass);
		serviceConfig.setProperty(SERVICE_URL_KEY, serviceURL);
		serviceConfig.setProperty("name", serviceName); // FIXME
		serviceConfig.setProperty("description", serviceDescription); // FIXME
		serviceConfig.setProperty(INPUT_CLASS_KEY, inputClass);
		serviceConfig.setProperty(OUTPUT_CLASS_KEY, outputClass);
		
		MavenProject project = (MavenProject)getPluginContext().get("project");
		File basePath = project.getBasedir();
		getLog().info("generating service files relative to " + basePath);
		
		// write new properties...
		try {
//			Map context = getPluginContext();
//			for (Object key: context.keySet()) {
//				getLog().info(String.format("%s -> %s", key, context.get(key)));
//			}
//			Object oProject = getPluginContext().get("project");
//			getLog().info("Project:");
//			getLog().info(oProject.getClass().getCanonicalName());
			createPath(new File(basePath, SERVICE_PROPERTIES));
			writeProperties(basePath, config, SERVICE_PROPERTIES);
		} catch (IOException e) {
			throw new MojoExecutionException(String.format("failed to write new properties file %s", SERVICE_PROPERTIES), e);
		}
		
		// create java skeleton...
		try {
			Set<OntProperty> properties = new HashSet<OntProperty>();
			Set<OntClass> classes = new HashSet<OntClass>();
			collect(inputClass, properties, classes);
			collect(outputClass, properties, classes);
			File classFile = new File(basePath, String.format("%s/%s.java", SOURCE_DIRECTORY, serviceClass.replace(".", "/")));
			createPath(classFile);
			FileWriter writer = new FileWriter(classFile);
			String template = SPARQLStringUtils.readFully(ServiceGenerator.class.getResourceAsStream("templates/ServiceServletSkeleton"));
			VelocityContext context = new VelocityContext();
			context.put("package", StringUtils.substringBeforeLast(serviceClass, "."));
			context.put("class", StringUtils.substringAfterLast(serviceClass, "."));
			context.put("properties", properties);
			context.put("classes", classes);
			Velocity.init();
			Velocity.evaluate(context, writer, "SADI", template);
			writer.close();
		} catch (Exception e) {
			throw new MojoExecutionException(String.format("failed to write new java file for %s", serviceClass), e);
		} 
		
		Collection<Map<String, String>> services = new ArrayList<Map<String, String>>();
		for (Entry<String, Configuration> entry: config.getServiceConfigurations().entrySet()) {
			Map<String, String> service = new HashMap<String, String>();
			service.put("key", entry.getKey());
			service.put("clazz", entry.getValue().getString(""));
			service.put("url", "/" + entry.getValue().getString("name"));
			services.add(service);
		}

		// write new web.xml...
		try {
			File webXml = new File(basePath, WEB_XML_PATH);
			createPath(webXml);
			FileWriter writer = new FileWriter(webXml);
			String template = FileUtils.readWholeFileAsUTF8(ServiceGenerator.class.getResourceAsStream("templates/webXmlSkeleton"));
			VelocityContext context = new VelocityContext();
			context.put("services", services);
			Velocity.init();
			Velocity.evaluate(context, writer, "SADI", template);
			writer.close();
		} catch (Exception e) {
			throw new MojoExecutionException("failed to write web.xml", e);
		}

		// write new index.jsp...
		try {
			File index = new File(basePath, INDEX_PATH);
			createPath(index);
			FileWriter writer = new FileWriter(index);
			String template = FileUtils.readWholeFileAsUTF8(ServiceGenerator.class.getResourceAsStream("templates/indexSkeleton"));
			VelocityContext context = new VelocityContext();
			context.put("services", services);
			Velocity.init();
			Velocity.evaluate(context, writer, "SADI", template);
			writer.close();
		} catch (Exception e) {
			throw new MojoExecutionException("failed to write web.xml", e);
		}
	}
	
	Set<OntClass> seen = new HashSet<OntClass>();
	private void collect(OntClass c, Collection<OntProperty> properties, Collection<OntClass> classes)
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
				collect(valuesFrom, properties, classes);
			}
		}
	}

	private void writeProperties(File base, Config config, String propertiesPath) throws IOException
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

	private static void createPath(File outfile) throws IOException
	{
		File parent = outfile.getParentFile();
		if (parent != null && !parent.isDirectory())
			if (!parent.mkdirs())
				throw new IOException(String.format("unable to create directory path ", parent));
	}
	
	public static void main(String args[]) throws MojoExecutionException, MojoFailureException
	{
		System.setProperty(SERVICE_CLASS_KEY, "ca.wilkinsonlab.sadi.service.example.LinearRegressionServiceServlet");
		System.setProperty(SERVICE_NAME_KEY, "linear");
		System.setProperty(INPUT_CLASS_KEY, "http://sadiframework.org/examples/regression.owl#InputClass");
		System.setProperty(OUTPUT_CLASS_KEY, "http://sadiframework.org/examples/regression.owl#OutputClass");
		new ServiceGenerator().execute();
	}
}
