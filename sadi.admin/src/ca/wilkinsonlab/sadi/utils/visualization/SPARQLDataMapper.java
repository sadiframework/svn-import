package ca.wilkinsonlab.sadi.utils.visualization;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry;
import ca.wilkinsonlab.sadi.client.virtual.sparql.VirtuosoSPARQLRegistry;
import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.graph.MultiSPARQLEndpointIterator;
import ca.wilkinsonlab.sadi.utils.sparql.ExceededMaxAttemptsException;
import ca.wilkinsonlab.sadi.utils.sparql.NoSampleAvailableException;
import ca.wilkinsonlab.sadi.utils.sparql.TripleSampler;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_URI;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Utility to crawl data across distributed SPARQL endpoints and create an overarching schema.
 * 
 * @author Ben Vandervalk
  */
public class SPARQLDataMapper {

	public final static Logger log = Logger.getLogger(SPARQLDataMapper.class);
	public final static Property EXAMPLE_URI = ResourceFactory.createProperty("http://sadiframework.org/ontologies/sparqlmap.owl#exampleURI");
	/** Write out intermediate results to file, after every OUTPUT_FILE_UPDATE_INTERVAL statements are added to the schema. */  
	protected final static int OUTPUT_FILE_UPDATE_INTERVAL = 100;  
	
	SPARQLRegistry registry;
	long literalCounter = 0;
	
	protected final static int TYPE_CACHE_SIZE = 1000;
	protected List<Pattern> ignoreTypePatterns;
	
	public SPARQLDataMapper(SPARQLRegistry registry) {
		setRegistry(registry);
		
		ignoreTypePatterns = new ArrayList<Pattern>();
		Configuration adminConfig = ca.wilkinsonlab.sadi.admin.Config.getConfiguration();
		for(Object regex : adminConfig.getList("share.sparql.dataMapper.ignoreTypePattern")) {
			ignoreTypePatterns.add(Pattern.compile((String)regex));
		}
	}
	
	protected SPARQLRegistry getRegistry() {
		return registry;
	}

	protected void setRegistry(SPARQLRegistry sparqlRegistry) {
		this.registry = sparqlRegistry;
	}

	public void buildSchema(Collection<SPARQLEndpoint> endpoints, int maxTraversalDepth, int maxSamplesPerType, String outputFilename, String outputFormat) throws SADIException, IOException 
	{
		Model schema = ModelFactory.createMemModelMaker().createFreshModel();
		TypeCache typeCache = new TypeCache(getRegistry(), TYPE_CACHE_SIZE);
		MultiSPARQLEndpointIterator iterator = new MultiSPARQLEndpointIterator(getRegistry(), new HashSet<Node_URI>(), maxTraversalDepth, maxSamplesPerType);
		
		// used for tracking when to write out intermediate results.
		int statementCounter = 0;
			
		for(SPARQLEndpoint endpoint : endpoints) {

			//endpoint = SPARQLEndpointFactory.createEndpoint("http://reactome.bio2rdf.org/sparql", EndpointType.VIRTUOSO);
			
			if(getRegistry().getServiceStatus(endpoint.getURI()) == ServiceStatus.DEAD) {
				continue;
			}

			TripleSampler tripleSampler = new TripleSampler(endpoint);
			
			// obtain sample URIs for each rdf:type in the endpoint, to use as root nodes of the traversal
			Set<Node_URI> rootNodes = new HashSet<Node_URI>();
			try {
				for(Node_URI type : getTypesInEndpoint(endpoint)) {
					if(ignoreType(type)) {
						log.trace(String.format("ignoring rdf:type %s", type));
						continue;
					}
					Triple pattern = new Triple(NodeCreateUtils.create("?s"), RDF.type.asNode(), type);
					for(int i = 0; i < maxSamplesPerType; i++) {
						try {
							Node s = tripleSampler.getSample(pattern).getSubject();
							if(s.isURI()) {
								rootNodes.add((Node_URI)s);
							}
						} catch(NoSampleAvailableException e) {
							// this happens when all available subject URIs for the current rdf:type are b-nodes
							log.warn(String.format("skipping type %s (probably because all available subjects URIs are b-nodes)", type), e);
							break;
						}
					}
				}
			} 
			catch(IOException e) {
				log.trace(String.format("failed to query endpoint %s, skipping to next endpoint", endpoint.getURI()), e);
				if(getRegistry().isWritable()) {
					getRegistry().setServiceStatus(endpoint.getURI(), ServiceStatus.DEAD);
				}
				continue;
			}
			catch(ExceededMaxAttemptsException e) {
				// this exception should never happen in this context
				throw new RuntimeException(e);
			}

			// crawl the data and record all connections discovered between rdf:types 
			
			iterator.reset(rootNodes);
			while(iterator.hasNext()) {
				
				Triple triple = iterator.next();
				
				Node s = triple.getSubject();
				Node p = triple.getPredicate();
				Node o = triple.getObject();
				
				if(!s.isURI() || o.isBlank()) {
					log.trace(String.format("skipping triple with blank node(s), %s", triple));
				}
				
				
				Set<Node_URI> subjectTypes = typeCache.getTypes((Node_URI)s);
				Property predicate = schema.createProperty(p.getURI());
				Resource subject = schema.createResource(s.getURI());

				if(o.isURI()) {
					Set<Node_URI> objectTypes = typeCache.getTypes((Node_URI)o);
					for(Node_URI subjectType : subjectTypes) {
						Resource sType = schema.createResource(subjectType.getURI());
						addStatement(schema, sType, EXAMPLE_URI, subject);
						for(Node_URI objectType : objectTypes) {
							Resource oType = schema.createResource(objectType.getURI());
							addStatement(schema, sType, predicate, oType);
							statementCounter++;
						}
					}
				} else if(o.isLiteral()) {
					Literal oValue = schema.createTypedLiteral(o.getLiteralValue());
					for(Node_URI subjectType : subjectTypes) {
						Resource sType = schema.createResource(subjectType.getURI());
						addStatement(schema, sType, predicate, oValue);
						statementCounter++;
					}
				}
				
				if(statementCounter >= OUTPUT_FILE_UPDATE_INTERVAL) {
					log.trace(String.format("writing out intermediate results to %s", outputFilename));
					writeSchemaToFile(schema, outputFilename, outputFormat);
					statementCounter = 0;
				}
			}
			//break;
		}
		
		log.trace(String.format("schema generation complete, writing schema to %s", outputFilename));
		writeSchemaToFile(schema, outputFilename, outputFormat);
	}
	
	protected void writeSchemaToFile(Model schema, String outputFilename, String outputFormat) throws IOException
	{
		Writer outputWriter = new BufferedWriter(new FileWriter(outputFilename));
		schema.write(outputWriter, outputFormat);
	}
	
	protected void addStatement(Model model, Resource s, Property p, RDFNode o) 
	{
		if(o.isLiteral()) {
			// We represent literal nodes by their corresponding datatype URI (e.g. xsd:string).
			// However, it is not useful to have hundreds of datatype properties point to a common
			// node in the graph; thus we append a counter value to the datatype URI (e.g. xsd:string_99).
			if(model.contains(s, p, (RDFNode)null)) {
				return;
			}
			//o = model.createResource(((Literal)o).getDatatypeURI() + "_" + String.valueOf(literalCounter++));
			o = model.createTypedLiteral("example: " + o.asLiteral().getLexicalForm());
		}
		Statement statement = model.createStatement(s, p, o);
		if(!model.contains(statement)) {
			log.trace(String.format("adding new triple %s", statement));
		}
		model.add(statement);
	}
	
	protected boolean ignoreType(Node_URI type) 
	{
		String uri = type.getURI();
		for(Pattern pattern : ignoreTypePatterns) {
			if(pattern.matcher(uri).find()) {
				return true;
			}
		}
		return false;
	}
	
	protected Set<Node_URI> getTypesInEndpoint(SPARQLEndpoint endpoint) throws IOException 
	{
		log.trace(String.format("retrieving rdf:types of subject URIs in %s", endpoint));
		
		String query = "SELECT DISTINCT ?type WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type }";
		List<Map<String,String>> results = endpoint.selectQueryBestEffort(query);

		Set<Node_URI> types = new HashSet<Node_URI>();
		for(Map<String,String> binding : results) {
			types.add((Node_URI)Node.createURI(binding.get("type")));
		}
		return types;
	}
	
	/*
	protected Set<String> getEndpointsContainingType(Resource type) 
	{
		Set<String> endpointURIs = new HashSet<String>();
		String query = SPARQLStringUtils.strFromTemplate("SELECT * WHERE { ?s %u% %u% } LIMIT 1", RDF.type.getURI(), type.getURI());
		
		Triple queryPattern = new Triple(NodeCreateUtils.create("?s"), RDF.type.asNode(), type.asNode());
		for(SPARQLEndpoint endpoint : getRegistry().findEndpointsByTriplePattern(queryPattern)) {
			if(getRegistry().getServiceStatus(endpoint.getURI()) == ServiceStatus.DEAD) {
				continue;
			}
			try {
				if(endpoint.constructQuery(query).size() > 0) {
					endpointURIs.add(endpoint.getURI());
				}
			} 
			catch(IOException e) {
				log.trace(String.format("failed to query endpoint %s", endpoint), e);
			}
		}
		if(endpointURIs.size() == 0) {
			log.warn(String.format("no endpoints contain rdf:type %s", type.getURI()));
		}
	}
	*/
	
	protected static class TypeCache extends LinkedHashMap<Node_URI,Set<Node_URI>>
	{
		private static final long serialVersionUID = 1L;

		SPARQLRegistry registry;
		int maxEntries;
		
		public TypeCache(SPARQLRegistry registry, int maxEntries) {
			setRegistry(registry);
			setMaxEntries(maxEntries);
		}
		
		public int getMaxEntries() {
			return maxEntries;
		}

		public void setMaxEntries(int maxEntries) {
			this.maxEntries = maxEntries;
		}

		protected SPARQLRegistry getRegistry() {
			return registry;
		}

		protected void setRegistry(SPARQLRegistry registry) {
			this.registry = registry;
		}

		// This override causes LinkedHashMap to behave as an LRU cache.
		// removeEldestEntry() is called when new entries are added to the map.
		@SuppressWarnings("unchecked")
		@Override
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return size() > getMaxEntries();
		}
		
		public Set<Node_URI> getTypes(Node_URI subject) throws SADIException, IOException 
		{
			if(!subject.isURI()) {
				throw new IllegalArgumentException("cannot obtain rdf:type for a Node that is not a URI");
			}
			
			if(this.containsKey(subject)) {
				return this.get(subject);
			} else {
				
				Set<Node_URI> types = new HashSet<Node_URI>();
				Triple queryPattern = new Triple(subject, RDF.type.asNode(), NodeCreateUtils.create("?type"));
				String query = SPARQLStringUtils.getConstructQueryString(Collections.singletonList(queryPattern), Collections.singletonList(queryPattern));
				
				for(SPARQLEndpoint endpoint : getRegistry().findEndpointsByTriplePattern(queryPattern)) {
					if(getRegistry().getServiceStatus(endpoint.getURI()) == ServiceStatus.DEAD) {
						continue;
					}
					try {
						for(Triple triple : endpoint.constructQuery(query)) {
							if(triple.getPredicate().equals(RDF.type.asNode())) {
								if(triple.getObject().isURI()) {
									types.add((Node_URI)triple.getObject());
								}
							}
						}
					} 
					catch(IOException e) {
						log.trace(String.format("failed to query endpoint %s", endpoint), e);
						/*
						if(getRegistry().isWritable()) {
							getRegistry().setServiceStatus(endpoint.getURI(), ServiceStatus.DEAD);
						}
						*/
					}
				}
				if(types.size() == 0) {
					log.warn(String.format("no rdf:types found for %s", subject.getURI()));
				}
				return types;
			}
		}
	}

	protected static class CommandLineOptions
	{
		protected static Configuration config = Config.getConfiguration().subset("sadi.registry.sparql");
		protected static final String REGISTRY_ENDPOINT_CONFIG_KEY = "endpoint";
		protected static final String REGISTRY_USERNAME_CONFIG_KEY = "username";
		protected static final String REGISTRY_PASSWORD_CONFIG_KEY = "password";
		protected static final String REGISTRY_GRAPH_CONFIG_KEY = "indexGraph";
		
		@Argument(required = true, index = 0, usage = "output RDF filename")
		public String outputFilename = null;

		@Option(name = "-r", usage = "SPARQL endpoint registry URL")
		public String registryURL = config.getString(REGISTRY_ENDPOINT_CONFIG_KEY, "http://localhost:8890/sparql");

		/*
		@Option(name = "-u", usage = "SPARQL endpoint registry username (optional, needed for updating status of dead endpoints)")
		public String registryUsername = config.getString(REGISTRY_USERNAME_CONFIG_KEY);
		
		@Option(name = "-p", usage = "SPARQL endpoint registry password (optional, needed for updating status of dead endpoints)")
		public String registryPassword = config.getString(REGISTRY_PASSWORD_CONFIG_KEY);
		*/
		
		@Option(name = "-g", usage = "SPARQL endpoint registry graph")
		public String registryGraphURI = config.getString(REGISTRY_GRAPH_CONFIG_KEY);
		
		@Option(name = "-f", usage = "output RDF format (allowed values: \"RDFXML\" or \"N3\")")
		public String outputFormat = "RDFXML";

		@Option(name = "-d", usage = "maximum depth of traversal")
		public int maxTraversalDepth = 1;

		@Option(name = "-t", usage = "maximum number of times to visit each rdf:type")
		public int maxVisitsPerType = 3; 

		@Option(name = "-e", metaVar = "<URI>", usage = "Specifies which SPARQL endpoint to build the map for. This switch may be used multiple times " +
			"to build a map for multiple endpoints. If no endpoints are specified with the -e switch, a map for all known endpoints in the SPARQL endpoint registry " +
			"will be built.")
		public List<String> endpointURIs;
		
	}
	

	public static void main(String[] args) throws SADIException, IOException 
	{
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);

		try {
			cmdLineParser.parseArgument(args);
			SPARQLRegistry registry = new VirtuosoSPARQLRegistry(options.registryURL, options.registryGraphURI);
			
			// instantiate the SPARQL endpoints that we are building a map for
			Collection<SPARQLEndpoint> endpoints;
			
			if(options.endpointURIs.size() == 0) {
				// if no endpoints were specified, built a map for all endpoints in the registry
				endpoints = registry.getAllEndpoints();
			} else {
				endpoints = new ArrayList<SPARQLEndpoint>();
				for(String uri : options.endpointURIs) {
					endpoints.add(registry.getEndpoint(uri));
				}
			}
			
			new SPARQLDataMapper(registry).buildSchema(endpoints, options.maxTraversalDepth, options.maxVisitsPerType, options.outputFilename, options.outputFormat);
		}
		catch (CmdLineException e) {
			log.error(e.getMessage());
			log.error("usage: sparqlmapper [options] <outputFilename>\n");
			cmdLineParser.printUsage(System.err);
		}
		
	}
	
}
