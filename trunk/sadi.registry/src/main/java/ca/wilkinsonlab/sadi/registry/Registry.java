package ca.wilkinsonlab.sadi.registry;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import virtuoso.jena.driver.VirtModel;
import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.beans.RestrictionBean;
import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.client.ServiceImpl;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyException;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * The server side registry logic.
 * TODO separate interface and implementation completely?
 * Not really necessary since no one else will be extending or implementing
 * another of these, but it might be a good idea if I ever find myself doing so.
 * @author Luke McCarthy
 */
public class Registry
{
	private static final Logger log = Logger.getLogger(Registry.class);
	
	/**
	 * Returns the registry configuration object. 
	 * Properties are read from a file called sadi.registry.properties; 
	 * see {@link org.apache.commons.configuration.PropertiesConfiguration}
	 * for details about which locations will be searched for that file.
	 * @return the registry configuration object
	 */
	public static Configuration getConfig()
	{
		try {
			return new PropertiesConfiguration("sadi.registry.properties");
		} catch (ConfigurationException e) {
			log.warn( String.format("error reading registry configuration: %s", e) );
			return new PropertiesConfiguration();
		}
	}
	
	/**
	 * Returns the default registry implementation.
	 * The default  configuration is contained in a file called
	 * sadi.registry.properties located in the classpath.
	 * @return the default registry implementation
	 */
	public static Registry getRegistry() throws SADIException
	{
		Configuration config = getConfig();
		String file = config.getString("file");
		String driver = config.getString("driver");
		String graph = config.getString("graph");
		String dsn = config.getString("dsn");
		String username = config.getString("username");
		String password = config.getString("password");
		
		if (driver == null) {
			if (file == null) {
				log.warn("no database driver or file specified; creating transient registry model");
				return new Registry(ModelFactory.createDefaultModel());
			} else {
				return getFileRegistry(file);
			}
		} else if (driver.equals("virtuoso.jdbc3.Driver")) {
			return getVirtuosoRegistry(graph, dsn, username, password);
		} else {
			return getJDBCRegistry(driver, dsn, username, password);
		}
	}
	
	/**
	 * Returns a Registry backed by a Virtuoso model.
	 * @param graph
	 * @param dsn
	 * @param username
	 * @param password
	 * @return
	 */
	public static Registry getVirtuosoRegistry(String graph, String dsn, String username, String password) throws SADIException
	{
		log.debug(String.format("creating Virtuoso-backed registry model from %s(%s)", dsn, graph));
		try {
			Model model = initVirtuosoRegistryModel(graph, dsn, username, password);
			return new Registry(model);
		} catch (Exception e) {
			throw new SADIException(String.format("error connecting to Virtuoso registry at %s", dsn), e);
		}
	}
	
	private static Model initVirtuosoRegistryModel(String graph, String dsn, String username, String password)
	{
		return VirtModel.createDatabaseModel(graph, dsn, username, password);
	}
	
	/**
	 * Returns a Registry backed by a JDBC model.
	 * @param driver (e.g.: "com.mysql.jdbc.driver")
	 * @param dsn
	 * @param username
	 * @param password
	 * @return
	 */
	public static Registry getJDBCRegistry(String driver, String dsn, String username, String password) throws SADIException
	{
		log.debug(String.format("creating JDBC-backed registry model from %s", dsn));
		try {
			Model model = initJDBCRegistryModel(driver, dsn, username, password);
			return new Registry(model);
		} catch (Exception e) {
			throw new SADIException(String.format("error connecting to JDBC registry at %s", dsn), e);
		}
	}
	
	private static Model initJDBCRegistryModel(String driver, String dsn, String username, String password)
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
	
	/**
	 * Returns a registry backed by a file.
	 * @param path
	 * @return 
	 */
	public static Registry getFileRegistry(String path) throws SADIException
	{
		log.debug(String.format("creating file-backed registry model from %s", path));
		try {
			Model model = initFileRegistryModel(path);
			return new Registry(model);
		} catch (Exception e) {
			throw new SADIException(String.format("error reading registry from %s", path), e);
		}
	}
	
	private static Model initFileRegistryModel(String path)
	{
		File registryFile = new File(path);
		File parentDirectory = registryFile.getParentFile();
		if (parentDirectory == null)
			parentDirectory = new File(".");
		if (!parentDirectory.isDirectory())
			parentDirectory.mkdirs();
		
		ModelMaker maker = ModelFactory.createFileModelMaker(parentDirectory.getAbsolutePath());
		return maker.openModel(registryFile.getName());
	}
	
	private Model model;

	/**
	 * Constructs a Registry from the specified Jena model.
	 * @param model
	 */
	public Registry(Model model)
	{
		this.model = model;
		model.setNsPrefix("sadi", "http://sadiframework.org/ontologies/sadi.owl#");
		model.setNsPrefix("mygrid", "http://www.mygrid.org.uk/mygrid-moby-service#");
	}
	
	/**
	 * Returns the Jena model containing the registry data.
	 * @return the Jena model containing the registry data
	 */
	public Model getModel()
	{
		return model;
	}
	
	/**
	 * Returns an iterator over the registered service nodes.
	 * @return an iterator over the registered service nodes
	 */
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
	
	/**
	 * Returns true if the registry contains the specified service,
	 * false otherwise.
	 * @param serviceURI
	 * @return true if the registry contains the specified service, false otherwise
	 */
	public boolean containsService(String serviceURI)
	{
		return getServiceBean(serviceURI) != null;
	}
	
	/**
	 * Returns a bean describing the specified registered service, 
	 * or null if the service is not registered.
	 * @param serviceURI
	 * @return a bean describing the specified registered service, or null
	 */
	public ServiceBean getServiceBean(String serviceURI)
	{
		Resource serviceNode = getModel().getResource(serviceURI);
		if (serviceNode != null && getModel().containsResource(serviceNode))
			return getServiceBean(serviceNode);
		else
			return null;
	}
	
	/**
	 * Returns a bean describing the specified service node.
	 * @param serviceNode
	 * @return a bean describing the specified service node
	 */
	public ServiceBean getServiceBean(Resource serviceNode)
	{
		ServiceBean service = new ServiceBean();
		try{
			new MyGridServiceOntologyHelper().copyServiceDescription(serviceNode, service);
		} catch (ServiceOntologyException e) {
			log.error(String.format("error in registered definition for %s", serviceNode), e);
		}
		for (StmtIterator i = serviceNode.listProperties(SADI.decoratesWith); i.hasNext(); ) {
			Statement statement = i.nextStatement();
			try {
				Resource restrictionNode = statement.getResource();
				Resource onProperty = restrictionNode.getRequiredProperty(OWL.onProperty).getResource();
				String onPropertyURI = onProperty.getURI();
				String onPropertyLabel = null;
				StringBuffer buf = new StringBuffer();
				for (Iterator<Statement> j = onProperty.listProperties(RDFS.label); j.hasNext(); ) {
					buf.append(j.next().getLiteral().getLexicalForm());
					if (j.hasNext())
						buf.append(" / ");
				}
				if (buf.length() > 0)
					onPropertyLabel = buf.toString();
				if (restrictionNode.hasProperty(OWL.someValuesFrom)) {
					for (Iterator<Statement> j = restrictionNode.listProperties(OWL.someValuesFrom); j.hasNext(); ) {
						Resource valuesFromNode = j.next().getResource();
						RestrictionBean restriction = new RestrictionBean();
						restriction.setOnPropertyURI(onPropertyURI);
						restriction.setOnPropertyLabel(onPropertyLabel);
						restriction.setValuesFromURI(valuesFromNode.getURI());
						buf.setLength(0);
						for (Iterator<Statement> k = valuesFromNode.listProperties(RDFS.label); k.hasNext(); ) {
							buf.append(k.next().getLiteral().getLexicalForm());
							if (k.hasNext())
								buf.append(" / ");
						}
						if (buf.length() > 0)
							restriction.setValuesFromLabel(buf.toString());
						else
							restriction.setValuesFromLabel(null);
						service.getRestrictionBeans().add(restriction);
					}
				} else {
					RestrictionBean restriction = new RestrictionBean();
					restriction.setOnPropertyURI(onPropertyURI);
					restriction.setOnPropertyLabel(onPropertyLabel);
					restriction.setValuesFromURI(null);
					restriction.setValuesFromLabel(null);
					service.getRestrictionBeans().add(restriction);
				}
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
	 * @throws SADIException 
	 */
	public ServiceBean registerService(String serviceUrl) throws SADIException
	{
		if (getModel().containsResource(getModel().getResource(serviceUrl))) {
			log.debug(String.format("unregistering service %s", serviceUrl));
			unregisterService(serviceUrl);
		}
		
		/* fetch the service definition and cache in our model so it can be
		 * queried...
		 */
		log.debug(String.format("fetching service definition from %s", serviceUrl));
		ServiceImpl service = new ServiceImpl(serviceUrl);
		getModel().add(service.getServiceModel());
		
		/* attach SADI type only after the service definition has been
		 * successfully fetched...
		 */
		Resource serviceNode = getModel().createResource(serviceUrl, SADI.Service);
		log.info(String.format("registered service %s", serviceUrl));
		
		attachMetaData(serviceNode, service);

		return getServiceBean(serviceNode);
	}
	
	private void attachMetaData(Resource serviceNode, ServiceImpl service) throws SADIException
	{
		for (Restriction restriction: service.getRestrictions()) {
			attachRestriction(serviceNode, restriction);
		}
		Resource reg = serviceNode.getModel().createResource();
		serviceNode.addProperty(SADI.registration, reg);
		reg.addLiteral(DC.date, Calendar.getInstance().getTime());
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
		}
		restrictionNode.addProperty(OWL.onProperty, p);
		getModel().add(p, RDFS.label, OwlUtils.getLabel(p));
		
		OntResource valuesFrom = OwlUtils.getValuesFrom(restriction);
		if (valuesFrom != null)
			attachRestrictionValuesFrom(restrictionNode, valuesFrom);
	}
	
	/* The point of storing the restricted valuesFrom of attached properties
	 * in the registry is that they can be queried without reasoning.  It is
	 * therefore not useful to store anything anonymous.  Instead, we store
	 * the first named superclass, but it might be better to store all named
	 * superclasses (see TODO below...)
	 * Also, we split up union classes into their components so they
	 * can be individually queried.  This feels like a slight abuse of
	 * owl:someValuesFrom, but there's no actual cardinality restriction on
	 * that property, so I'm okay with it.
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
			/* TODO should we do this? can we usefully encapsulate reasoning
			 * in the registry?
			 */
//			for (Iterator<? extends OntClass> i = valuesFromClass.listSuperClasses(); i.hasNext(); ) {
//				attachRestrictionValuesFrom(restrictionNode, i.next());
//			}
//			for (Iterator<? extends OntClass> i = valuesFromClass.listEquivalentClasses(); i.hasNext(); ) {
//				attachRestrictionValuesFrom(restrictionNode, i.next());
//			}
		}
		
		if (valuesFrom.isURIResource()) {
			restrictionNode.addProperty(OWL.someValuesFrom, valuesFrom);
			getModel().add(valuesFrom, RDFS.label, OwlUtils.getLabel(valuesFrom));
		} else if (valuesFrom.isDataRange()) {
//			DataRange range = valuesFrom.asDataRange();
			restrictionNode.addProperty(OWL.someValuesFrom, "anonymous data range");
		} else if (valuesFrom.isClass()) {
			OntClass firstNamedSuperClass = OwlUtils.getFirstNamedSuperClass(valuesFrom.asClass());
			restrictionNode.addProperty(OWL.someValuesFrom, firstNamedSuperClass);
			getModel().add(valuesFrom, RDFS.label, OwlUtils.getLabel(firstNamedSuperClass));
		}
	}
	
	/**
	 * Unregister the service at the specified URL.
	 * @param serviceUrl the SADI service URL
	 */
	public void unregisterService(String serviceUrl)
	{
		Resource service = getModel().getResource(serviceUrl);
		if (service != null) {
			Model serviceModel = ResourceUtils.reachableClosure(service);
			maybeBackupServiceModel(serviceUrl, serviceModel);
			getModel().remove(serviceModel);
		} else {
			log.warn("attempt to unregister non-registered service " + serviceUrl);
		}
	}
	
	private void maybeBackupServiceModel(String serviceUrl, Model serviceModel)
	{
		Configuration config = getConfig();
		String backupPath = config.getString("backupDirectory");
		if (backupPath != null) {
			File backupDirectory = new File(backupPath);
			if ( backupDirectory.isDirectory() && backupDirectory.canWrite() ) {
				String modelName = String.format("%s.rdf", serviceUrl);
				log.trace(String.format("backing up service defintion to %s", modelName));
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
					serviceModel.getWriter("RDF/XML-ABBREV").write(serviceModel, new FileOutputStream(file), "");
				} catch (Exception e) {
					log.error(String.format("error writing backup service model to %s", file));
				}
			} else {
				log.error(String.format("specified backup path %s is not a writeable directory", backupDirectory));
				return;
			}
		}
	}

	/**
	 * Execute a SPARQL query on the registry.
	 * Note that only SELECT queries are supported.
	 * @param sparql the SPARQL query
	 * @return a Jena ResultSet
	 * @throws SADIException
	 */
	public ResultSet doSPARQL(String query) throws SADIException
	{
		Query q = QueryFactory.create(query);
		if (q.isSelectType())
			return QueryExecutionFactory.create(q, getModel()).execSelect();
		else
			throw new SADIException("only SELECT queries are supported");
	}
}
