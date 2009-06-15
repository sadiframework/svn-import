package ca.wilkinsonlab.sadi.rdf;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper;
import ca.wilkinsonlab.sadi.utils.DurationUtils;
import ca.wilkinsonlab.sadi.utils.HttpUtils;
import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpInputStream;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

public class RdfService implements Service
{
	private static final Log log = LogFactory.getLog(RdfService.class);
	
	private static final ModelMaker modelMaker = ModelFactory.createMemModelMaker();
	
	String serviceUri;
	URL serviceUrl;
	RdfRegistry sourceRegistry;
	String name;
	String description;
	Model model;
	OntModel ontModel;
	OntClass inputClass;
	OntClass outputClass;
	Collection<String> predicates;
	
	/**
	 * Construct a new RdfService from the service description located at
	 * the specified URL.
	 * @param serviceURL the service URL
	 * @throws MalformedURLException
	 */
	public RdfService(String serviceURL) throws MalformedURLException
	{
		this.serviceUri = serviceURL;
		this.serviceUrl = new URL(serviceURL);
	}
	
	/**
	 * Construct a new RdfService from a service URI, an input class
	 * and an output class. This method should only be used by the
	 * unit tests.
	 * @param serviceUri the service URI
	 * @param inputClassUri the input class URI
	 * @param outputClassUri the output class URI
	 */
	RdfService(String serviceUri, String inputClassUri, String outputClassUri)
	{
		model = modelMaker.createFreshModel();
		ServiceOntologyHelper helper = new MyGridServiceOntologyHelper(model, serviceUri);
		helper.setInputClass(inputClassUri);
		helper.setOutputClass(outputClassUri);
		
		createOntologyModel(helper);
	}

	private boolean isInitialized()
	{
		return model != null;
	}
	
	private void fetchServiceModel()
	{
		log.debug("fetching service model from " + getServiceURL());
		
		/* TODO is this the best spec here?
		 * TODO is this the best import strategy here?
		 */
		model = modelMaker.createFreshModel();
		model.read(getServiceURI());
		Resource serviceRoot = model.getResource(getServiceURI());
		ServiceOntologyHelper helper = new MyGridServiceOntologyHelper(serviceRoot);
		name = helper.getName();
		description = helper.getDescription();
		
		createOntologyModel(helper);
	}
	
	private void createOntologyModel(ServiceOntologyHelper helper)
	{
		ontModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF );
		inputClass = ontModel.createClass( helper.getInputClass().getURI() );
		OwlUtils.loadOntologyForUri(ontModel, inputClass.getURI());
		outputClass = ontModel.createClass( helper.getOutputClass().getURI() );
		OwlUtils.loadOntologyForUri(ontModel, outputClass.getURI());
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
	@SuppressWarnings("deprecation")
	private static InputStream fetchAsyncData(String url) throws HttpException, IOException
	{
		while (true) {
			log.trace("fetching asynchronous data from " + url);
			GetMethod method = new GetMethod(url);
			method.setFollowRedirects(false);
			HttpClient client = new HttpClient();
			int statusCode = client.executeMethod(method);
			if (statusCode >= 300 && statusCode < 400) {
				long toSleep = 2000; // sleep for two seconds by default
				for (HeaderElement element: method.getResponseHeader("Pragma").getElements()) {
					if (element.getName().equals(SADI.ASYNC_HEADER)) {
						try {
							toSleep = DurationUtils.parse(element.getValue());
						} catch (NumberFormatException e) {
							log.error(e);
						}
					}
				}
				try {
					log.trace("sleeping " + toSleep + "ms before following redirect");
					Thread.sleep(toSleep);
				} catch (InterruptedException e) {
					log.warn(e);
				}
			} else if (statusCode == HttpStatus.SC_OK) {
				return new HttpInputStream(method.getResponseBodyAsStream(),method);
			} else {
				throw new HttpResponseCodeException(statusCode, method.getStatusLine().getReasonPhrase());
			}
		}
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getServiceURI()
     */
	public String getServiceURI()
	{
		return serviceUri;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getServiceURL()
     */
	public URL getServiceURL()
	{
		return serviceUrl;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getName()
     */
	public String getName()
	{
		if (!isInitialized())
			fetchServiceModel();
		
		return name;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#getDescription()
     */
	public String getDescription()
	{
		if (!isInitialized())
			fetchServiceModel();
		
		return description;
	}
	
	private OntModel getServiceOntologyModel()
	{
		if (!isInitialized())
			fetchServiceModel();
		
		return ontModel;
	}
	
	/**
	 * Returns an OntClass describing the input this service can consume.
	 * Any input to this service must be an instance of this class.
	 * @return an OntClass describing the input this service can consume
	 */
	public OntClass getInputClass()
	{
		if (!isInitialized())
			fetchServiceModel();
		
		return inputClass;
	}
	
	/**
	 * Returns an OntClass describing the output this service produces.
	 * Any output from this service will be an instance of this class.
	 * @return an OntClass describing the output this service produces
	 */
	public OntClass getOutputClass()
	{
		if (!isInitialized())
			fetchServiceModel();
		
		return outputClass;
	}

	/**
	 * Returns the list of predicates this service attaches to its input.
	 * @return the list of predicates this service attaches to its input
	 */
	public Collection<String> getPredicates()
	{
		if (predicates == null) {
			Set<OntProperty> properties = OwlUtils.listRestrictedProperties(getOutputClass().getURI());
			Set<OntProperty> inputProperties = OwlUtils.listRestrictedProperties(getInputClass().getURI());
			predicates = new ArrayList<String>(properties.size());
			for (OntProperty p: properties) {
				/* TODO will two equivalent OntProperties from different
				 * models return true here?
				 */
				if (!inputProperties.contains(p))
					predicates.add(p.getURI());
			}
		}
		return predicates;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource)
     */
	public Collection<Triple> invokeService(Resource inputNode) throws Exception
	{
		/* TODO instead of reachableClosure, post the minimal RDF required
		 * to satisfy the input class.
		 */
		Model model = invokeServiceUnparsed(OwlUtils.getMinimalModel(inputNode, getInputClass()));
		
		Collection<Triple> triples = new ArrayList<Triple>();
		for (StmtIterator i = model.listStatements(); i.hasNext(); ) {
			Statement statement = i.nextStatement();
			triples.add(statement.asTriple());
		}
		return triples;
	}

	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#invokeService(com.hp.hpl.jena.rdf.model.Resource, java.lang.String)
     */
	public Collection<Triple> invokeService(Resource inputNode, String predicate)
	throws Exception
	{
		Collection<Triple> filteredTriples = new ArrayList<Triple>();
		for (Triple triple: invokeService(inputNode))
			if (triple.getPredicate().getURI().equals(predicate))
				filteredTriples.add(triple);
		return filteredTriples;
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
		InputStream is = HttpUtils.postToURL(getServiceURL(), inputModel);
		Model model = modelMaker.createFreshModel();
		model.read(is, "");
		is.close();
		
		/* resolve any rdfs:isDefinedBy URIs to fetch asynchronous data...
		 * TODO don't visit a given URI more than once in each pass
		 */
		List<Statement> toRemove = new ArrayList<Statement>();
		for (StmtIterator i = model.listStatements((Resource)null, RDFS.isDefinedBy, (RDFNode)null); i.hasNext(); ) {
			Statement statement = i.nextStatement();
			try {
				String url = statement.getResource().getURI();
				InputStream data = fetchAsyncData(url);
				model.read(data, url);
				toRemove.add(statement);
				data.close();
			} catch (Exception e) {
				log.error("failed to fetch data for " + statement, e);
			}
		}
		model.remove(toRemove);
		
		return model;
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#isInputInstance(com.hp.hpl.jena.rdf.model.Resource)
     */
	public boolean isInputInstance(Resource resource)
	{
		/* TODO may have to be getInputClass().getURI()...
		 */
		return createReasoningModel(resource.getModel()).getIndividual(resource.getURI()).hasOntClass(getInputClass());
	}
	
	/* (non-Javadoc)
     * @see ca.wilkinsonlab.sadi.client.Service#discoverInputInstances(com.hp.hpl.jena.rdf.model.Model)
     */
	@SuppressWarnings("unchecked")
	public Collection<Resource> discoverInputInstances(Model inputModel)
	{
		return createReasoningModel(inputModel).listIndividuals(getInputClass()).toList();
	}

	private OntModel createReasoningModel(Model inputModel)
	{
		/* TODO is there some way we can avoid creating a new ontology
		 * model every time?
		 */
		OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF, getServiceOntologyModel() );
		model.add(inputModel);
		return model;
	}
	
	@Override
	public String toString()
	{
		return getServiceURI();
	}
}
