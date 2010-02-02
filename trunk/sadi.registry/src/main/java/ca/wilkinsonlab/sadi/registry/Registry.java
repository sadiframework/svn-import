package ca.wilkinsonlab.sadi.registry;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import virtuoso.jena.driver.VirtModel;
import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.rdf.RdfService;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyException;
import ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * The server side registry logic.  See RegistryServlet for the communication
 * interface.
 * TODO separate interface and implementation completely?
 * Not really necessary since no one else will be extending or implementing
 * another of these, but it might be a good idea if I ever find myself doing so.
 * @author Luke McCarthy
 */
public class Registry
{
	private static final Log log = LogFactory.getLog(Registry.class);
	
	private static final Registry defaultRegistry = new Registry();
	
	private static final String propertiesFile = "registry.properties";
	
	private Model model;
	private File backupDirectory;
	
	/**
	 * Constructs a Registry from information in the properties config.
	 * This method is only used to construct the default singleton registry.
	 */
	private Registry()
	{
		Configuration config;
		try {
			config = new PropertiesConfiguration(propertiesFile);
		} catch (ConfigurationException e) {
			log.warn( String.format("error reading registry configuration: %s", e) );
			config = new PropertiesConfiguration();
		}
		
		String driver = config.getString("driver");
		String graph = config.getString("graph");
		String dsn = config.getString("dsn");
		String username = config.getString("username");
		String password = config.getString("password");
		
		if (driver == null) {
			/* TODO create file-backed model somewhere...
			 */
			log.warn("no database driver specified; creating transient registry model");
			model = ModelFactory.createDefaultModel();
		} else if (driver.equals("virtuoso.jdbc3.Driver")) {
			log.info(String.format("creating Virtuoso-backed registry model from %s(%s)", dsn, graph));
			try {
				model = initVirtuosoRegistryModel(graph, dsn, username, password);
			} catch (Exception e) {
				log.error(String.format("error connecting to Virtuoso registry at %s", dsn), e);
			}
		} else {
			log.info(String.format("creating JDBC-backed registry model from %s", dsn));
			try {
				model = initJDBCRegistryModel(driver, dsn, username, password);
			} catch (Exception e) {
				log.error(String.format("error connecting to JDBC registry at %s", dsn), e);
			}
		}
		
		String backupPath = config.getString("backupPath");
		if (backupPath != null) {
			backupDirectory = new File(backupPath);
			if ( !(backupDirectory.isDirectory() && backupDirectory.canWrite()) ) {
				log.error(String.format("specified backup path %s is not a writeable directory", backupDirectory));
				backupDirectory = null;
			}
		}
	}
	
	/**
	 * Constructs a Registry from the specified Virtuoso connection details.
	 * @param graph
	 * @param dsn
	 * @param username
	 * @param password
	 */
	public Registry(String graph, String dsn, String username, String password)
	{
		model = initVirtuosoRegistryModel(graph, dsn, username, password);
	}
	
	/**
	 * Constructs a Registry from the specified MySQL connection details.
	 * @param dsn
	 * @param username
	 * @param password
	 */
	public Registry(String dsn, String username, String password)
	{
		model = initJDBCRegistryModel("com.mysql.jdbc.driver", dsn, username, password);
	}
	
	/**
	 * Constructs a Registry from the specified Jena model.
	 * @param model
	 */
	public Registry(Model model)
	{
		this.model = model;
	}
	
	private Model initVirtuosoRegistryModel(String graph, String dsn, String username, String password)
	{
		return VirtModel.createDatabaseModel(
				graph,
				dsn,
				username,
				password
		);
	}
	
	private Model initJDBCRegistryModel(String driver, String dsn, String username, String password)
	{
		// load the driver class
		try {
			Class.forName(driver);
		} catch ( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		}
		
		// create a database connection
		IDBConnection conn = new DBConnection(
				dsn,
				username,
				password,
				driver.matches("(?i).*mysql.*") ? "MySQL" : null
		);
		
		// create a model maker with the given connection parameters
		ModelMaker maker = ModelFactory.createModelRDBMaker(conn);

		// create a default model
		return maker.createDefaultModel();
	}
	
	@SuppressWarnings("unused")
	private Model initFileRegistryModel(String path, String dsn, String username, String password)
	{
		ModelMaker maker = ModelFactory.createFileModelMaker(path);
		
		return maker.createDefaultModel();
	}
	
	/**
	 * Returns the default registry implementation.
	 * @return the default registry implementation
	 */
	public synchronized static Registry getRegistry()
	{
		return defaultRegistry;
	}
	
	/**
	 * Returns the Jena model containing the registry data.
	 * @return the Jena model containing the registry data
	 */
	public Model getModel()
	{
		return model;
	}
	
	public ResIterator getRegisteredServiceNodes()
	{
		return getModel().listResourcesWithProperty(RDF.type, SADI.Service);
	}
	
	public Collection<ServiceBean> getRegisteredServices()
	{
		Collection<ServiceBean> services = new ArrayList<ServiceBean>();
		for (ResIterator i = getRegisteredServiceNodes(); i.hasNext(); ) {
			Resource serviceNode = i.nextResource();
			ServiceBean service = getServiceBean(serviceNode);
			services.add(service);
		}
		return services;
	}
	
	public ServiceBean getServiceBean(String serviceURI)
	{
		Resource serviceNode = getModel().getResource(serviceURI);
		if (serviceNode != null && getModel().containsResource(serviceNode))
			return getServiceBean(serviceNode);
		else
			return null;
	}
	
	public ServiceBean getServiceBean(Resource serviceNode)
	{
		ServiceBean service = new ServiceBean();
		service.setServiceURI(serviceNode.getURI());
		try{
			ServiceOntologyHelper helper = new MyGridServiceOntologyHelper(serviceNode);
			service.setInputClassURI(helper.getInputClass().getURI());
			service.setOutputClassURI(helper.getOutputClass().getURI());
			service.setName(helper.getName());
			service.setDescription(helper.getDescription());
		} catch (ServiceOntologyException e) {
			log.error(String.format("error in registered definition for %s", serviceNode), e);
		}
		for (StmtIterator i = serviceNode.listProperties(SADI.decoratesWith); i.hasNext(); ) {
			Statement statement = i.nextStatement();
			try {
				Resource restrictionNode = statement.getResource();
				RestrictionBean restriction = new RestrictionBean();
				restriction.setOnProperty(restrictionNode.getRequiredProperty(OWL.onProperty).getObject().toString());
				if (restrictionNode.hasProperty(OWL.someValuesFrom)) {
					StringBuffer buf = new StringBuffer();
					for (Iterator<Statement> j = restrictionNode.listProperties(OWL.someValuesFrom); j.hasNext(); ) {
						Statement t = j.next();
						if (buf.length() > 0)
							buf.append(", ");
						buf.append(t.getObject());
					}
					restriction.setValuesFrom(buf.toString());
				}
				service.getRestrictions().add(restriction);
			} catch (Exception e) {
				log.error(String.format("bad restriction attached to %s", serviceNode), e);
			}
			
		}
		return service;
	}
	
	/**
	 * Registers the service at the specified URL as a SADI service that
	 * consumes and produces RDF.  If the service is already present in
	 * the registry, the old registration data is deleted before the new
	 * data is stored.
	 * @param serviceUrl the SADI service URL:
	 *   a GET on this URL should produce an RDF description of the service,
	 *   a POST of RDF data to this URL calls the service
	 * @throws Exception 
	 */
	public ServiceBean registerService(String serviceUrl) throws SADIException
	{
		log.debug(String.format("unregistering service %s", serviceUrl));
		if (model.containsResource(model.getResource(serviceUrl)))
			unregisterService(serviceUrl);
		
		/* fetch the service definition and cache in our model so it can be
		 * queried...
		 */
		log.debug(String.format("fetching service definition from %s", serviceUrl));
		RdfService service = new RdfService(serviceUrl);
		getModel().add(service.getServiceModel());
		
		/* attach SADI type only after the service definition has been
		 * successfully fetched...
		 */
		Resource serviceNode = getModel().createResource(serviceUrl, SADI.Service);
		
		attachMetaData(serviceNode, service);

		return getServiceBean(serviceNode);
	}
	
	private void attachMetaData(Resource serviceNode, RdfService service) throws SADIException
	{
		for (Restriction restriction: service.getRestrictions()) {
			attachRestriction(serviceNode, restriction);
		}
	}
	
	/* TODO in some cases, we're ending up with multiple copies of what is
	 * effectively the same restriction; filter these somehow...
	 */
	private void attachRestriction(Resource serviceNode, Restriction restriction)
	{
		Resource restrictionNode = getModel().createResource();
		serviceNode.addProperty(SADI.decoratesWith, restrictionNode);
		
		OntResource p;
		try {
			p = restriction.getOnProperty();
		} catch (ConversionException e) {
			/* the property is not actually defined in the ontology, nor was
			 * it resolved or created by OwlUtils.decompose (according to the
			 * configured policy), so just do what we can...
			 */
			p = restriction.getOntModel().getOntResource(restriction.getProperty(OWL.onProperty).getResource());
			restrictionNode.addProperty(OWL.onProperty, p);
		}
		restrictionNode.addProperty(OWL.onProperty, p);
		
		OntResource valuesFrom = OwlUtils.getValuesFrom(restriction);
		attachRestrictionValuesFrom(restrictionNode, valuesFrom);
	}
	
	/* The point of storing the restricted valuesFrom of attached properties
	 * in the registry is that they can be queried without reasoning.  It is
	 * therefore not useful to store anything anonymous.  The best we can do
	 * is store a human-readable description of that resource that will be
	 * displayed by the registry interface.
	 * Also, we should split up union classes into their components so they
	 * can be individually queried.  This feels like a slight abuse of
	 * owl:someValuesFrom, but there's no actual cardinality restriction on
	 * that property, so I'm okay with it.
	 * TODO equivalent classes? subclasses? superclasses? how much reasoning
	 * can we effectively do ahead of time?
	 */
	private void attachRestrictionValuesFrom(Resource restrictionNode, OntResource valuesFrom)
	{
		if (valuesFrom.isClass()) {
			OntClass valuesFromClass = valuesFrom.asClass();
			if (valuesFromClass.isUnionClass()) {
				for (Iterator<? extends OntClass> i = valuesFromClass.asUnionClass().listOperands(); i.hasNext(); ) {
					attachRestrictionValuesFrom(restrictionNode, i.next());
				}
			}
		}
		
		if (valuesFrom.isURIResource()) {
			restrictionNode.addProperty(OWL.someValuesFrom, valuesFrom);
		} else if (valuesFrom.isDataRange()) {
//			DataRange range = valuesFrom.asDataRange();
			restrictionNode.addProperty(OWL.someValuesFrom, "anonymous data range");
		}
	}
	
	/**
	 * Unregister the service at the specified URL.
	 * @param serviceUrl the SADI service URL
	 */
	public void unregisterService(String serviceUrl)
	{
		Resource service = model.getResource(serviceUrl);
		if (service != null) {
			Model serviceModel = ResourceUtils.reachableClosure(service);
			if (backupDirectory != null) {
				String modelName =  String.format("%s.rdf", serviceUrl);
				try {
					modelName = new URLCodec().encode(modelName);
				} catch (EncoderException e) {
					log.error( String.format("error encoding URL %s: %s", modelName, e) );
				}
				File file;
				while ( (file = new File(backupDirectory, modelName)).exists() ) {
					modelName = modelName + "~";
				}
				try {
					serviceModel.write(new FileOutputStream(file));
				} catch (Exception e) {
					log.error(String.format("error writing backup service model to %s", file));
				}
			}
			model.remove(serviceModel);
		} else {
			log.warn("attempt to unregister non-registered service " + serviceUrl);
		}
	}
	
	public ResultSet doSPARQL(String query)
	{
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet resultSet = qe.execSelect();
//		while (resultSet.hasNext()) {
//			QuerySolution binding = resultSet.nextSolution();
//			
//		}
		return resultSet;
	}
}
