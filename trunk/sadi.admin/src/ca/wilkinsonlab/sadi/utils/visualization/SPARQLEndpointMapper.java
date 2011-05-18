package ca.wilkinsonlab.sadi.utils.visualization;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import ca.wilkinsonlab.sadi.admin.Config;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpointFactory;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint.EndpointType;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.URIUtils;
import ca.wilkinsonlab.sadi.utils.graph.BoundedBreadthFirstIterator;
import ca.wilkinsonlab.sadi.utils.graph.BreadthFirstIterator;
import ca.wilkinsonlab.sadi.utils.graph.OpenGraphIterator;
import ca.wilkinsonlab.sadi.utils.graph.SearchNode;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class SPARQLEndpointMapper 
{
	public final static Logger log = Logger.getLogger(SPARQLEndpointMapper.class);

	/** Connects a node representing a URI pattern to an example URI. */
	public final static Property EXAMPLE_URI = ResourceFactory.createProperty("http://sadiframework.org/ontologies/sparqlmap.owl#exampleURI");

	protected static class MapperSearchNode extends SearchNode<Resource>
	{
		private static final int URI_TO_MODEL_CACHE_MEM_SIZE = 5000;
		private static final int URI_TO_MODEL_CACHE_DISK_SIZE = 100000;
		
		private static final int MAX_EXAMPLE_URIS = 3;

		private SPARQLEndpoint endpoint;
		private Map<Resource,Long> RDFTypeVisitCounter;
		private int maxVisitsPerRDFType;
		private Model map;
		private Cache URIToModelCache;
		
		public MapperSearchNode(SPARQLEndpoint endpoint, Model map, Resource node, int maxVisitsPerRDFType) 
		{
			super(node);

			this.endpoint = endpoint;
			this.map = map;
			this.maxVisitsPerRDFType = maxVisitsPerRDFType;
			
			RDFTypeVisitCounter = new HashMap<Resource,Long>();

			URIToModelCache = 

				new Cache(
						
					new CacheConfiguration(
						String.format("%s:%d", "URIToModelCache", System.currentTimeMillis()),
						URI_TO_MODEL_CACHE_MEM_SIZE
					)
					.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
					.overflowToDisk(true)
					.maxElementsOnDisk(URI_TO_MODEL_CACHE_DISK_SIZE)
					.eternal(true)
				
				);
			
			Config.getCacheManager().addCache(URIToModelCache);
		}

		protected MapperSearchNode(
		
				SPARQLEndpoint endpoint, 
				Model map, 
				Resource node, 
				int maxVisitsPerRDFType, 
				Map<Resource,Long> RDFTypeVisitCounter,
				Cache URIToModelCache
		
		) 
		{
			super(node);
			this.endpoint = endpoint;
			this.map = map;
			this.maxVisitsPerRDFType = maxVisitsPerRDFType;
			this.RDFTypeVisitCounter = RDFTypeVisitCounter;
			this.URIToModelCache = URIToModelCache;
		}
		
		@Override
		public Set<SearchNode<Resource>> getSuccessors() 
		{
			Set<SearchNode<Resource>> successors = new HashSet<SearchNode<Resource>>();
			
			log.trace(String.format("visiting %s", getNode()));
			
			// get a model containing all triples about the current node

			Model model = getModel(getNode());
			Resource node = getNode().inModel(model);

			// log when we hit a dead end 
			
			if (model.size() == 0) {
				log.trace(String.format("end of branch: no triples with subject %s", getNode()));
				return successors;
			}
			
			// skip this node if all rdf:types have been exhausted

			if(!isVisitable(node)) {
				log.trace(String.format("end of branch: all rdf:types exhausted for %s", getNode()));
				return successors; 
			}

			visit(node);

			// add the new info to the schema map

			for(Statement statement : model.listStatements().toList()) {
				addStatement(statement);
			}

			// build successor nodes for next step of traversal
			
			for(Statement statement : model.listStatements().toList()) {
				if(statement.getObject().isURIResource()) {
					successors.add(new MapperSearchNode(
							endpoint, 
							map, 
							statement.getObject().asResource(), 
							maxVisitsPerRDFType, 
							RDFTypeVisitCounter,
							URIToModelCache
					));
				}
			}

			return successors;
		}
		
		protected Model getModel(Resource node) 
		{
			// check for the model in the cache
			
			Element cacheElement = URIToModelCache.get(node);
			if(cacheElement != null) {
				return (Model)(cacheElement.getObjectValue());
			}

			// query the data from the endpoint 
			
			Model model;
				
			String query = "CONSTRUCT { %u% ?p ?o } WHERE { %u% ?p ?o }";
			query = SPARQLStringUtils.strFromTemplate(query, node.getURI(), node.getURI());
			Collection<Triple> triples = null;

			try {
				triples = endpoint.constructQuery(query);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
			
			model = RdfUtils.triplesToModel(triples);
			
			// we can't traverse statements about blank nodes, so cull them out
			
			removeStatementsWithBlankNodes(model);
			
			// cache the model
			
			URIToModelCache.put(new Element(node, model));
			
			return model;
		}
		
		protected void addStatement(Statement statement)
		{
			Resource s = statement.getSubject();
			Property p = statement.getPredicate();
			RDFNode o = statement.getObject();

			// Note: Storing the original rdfs:label / dc:title / foaf:name in the
			// scheam is confusing when viewing it in an RDF browser (e.g. Tabulator).
			// We explicitly add our own labels for the map nodes in getURIPatternNode.
			
			if (p.equals(RDFS.label) || p.equals(DC.title) || p.equals(FOAF.name)) {
				return;
			}
			
			Resource sForMap = getURIPatternNode(s);
			Property pForMap = p;
			RDFNode  oForMap;
			
			if (o.isLiteral()) {

				// store one example literal for each datatype property, ignore the rest
				if(map.contains(sForMap, pForMap, (RDFNode)null)) {
					return;
				}
				
				String oString = String.format("literal (e.g. '%s')", o.asLiteral().getLexicalForm());
				oForMap = map.createTypedLiteral(oString);
				
			} else {
			
				// store full URI of rdf:types, URI pattern for everything else
				oForMap = p.equals(RDF.type) ? o.asResource() : getURIPatternNode(o.asResource());

			}
			
			Statement mapEdge = ResourceFactory.createStatement(sForMap, pForMap, oForMap);
			
			if(!map.contains(mapEdge)) {

				log.trace(String.format("adding edge to schema map: %s", mapEdge));
				map.add(mapEdge);
				
			}
			
			// Store example URIs within the schema.
			//
			// Note: we don't store example URIs for the object URI pattern, as that adds 
			// tends to add too much clutter to the schema.
			
			if (sForMap.listProperties(EXAMPLE_URI).toList().size() < MAX_EXAMPLE_URIS) {
				map.add(sForMap, EXAMPLE_URI, s);
			}

		}
		
		protected Resource getURIPatternNode(Resource node)
		{
			String URI = node.getURI();
			String URIPrefix = URIUtils.getURIPrefix(URI);
			
			if (URIPrefix == null) {
				
				log.warn(String.format("unable to determine URI prefix for %s, using full URI", URI));
				return map.createResource(URI);
				
			} else {
				
				// Tricky part.
				// 
				// For each URI pattern, there may be more than one type of entity in
				// the endpoint.  For example, in http://uniprot.bio2rdf.org/sparql,
				// the URI pattern http://bio2rdf.org/uniprot:* is used for both
				// protein records and for specific annotations within those
				// records (e.g. a single amino acid polymorphism). In the schema map, 
				// we differentiate between different types of nodes with the same 
				// URI pattern by appending an underscore and a number to the URI, 
				// e.g. http://bio2rdf.org/uniprot:*_2.  

				// retrieve all statements about the current node 
				
				node = node.inModel(getModel(node));
				
				Set<Resource> RDFTypes = getRDFTypes(node);
				String mapURI;
				Resource mapNode;

				
				int typeCount = 1;
				for(; ;typeCount++) {

					// "*" indicates "something goes here"
					
					if (typeCount == 1) {
						mapURI = String.format("%s*", URIPrefix);
					} else {
						mapURI = String.format("%s*_%d", URIPrefix, typeCount);
					}
					
					mapNode = map.createResource(mapURI);

					if(mapNode.listProperties().toList().size() == 0) {
						break;
					}

					if (RDFTypes.equals(getRDFTypes(mapNode))) {
						break;
					}
					
				}
				
				for (Resource type : RDFTypes) {
					map.add(mapNode, RDF.type, type);
				}
		
				// For the benefit of RDF browsers, add a rdfs:label giving the URI
				// pattern and the associated rdf:types.)
				// 
				// Note: We don't add labels to subject URIs that don't have triples
				// in the endpoint. This avoids adding unnecessary clutter to the schema.
				
				if (node.listProperties().toList().size() > 0) {
					
					String label = getMapNodeLabel(node);
					map.add(mapNode, RDFS.label, map.createTypedLiteral(label));
					
				}

				return mapNode;
			}

		}
		
		protected String getMapNodeLabel(Resource node) 
		{
			String URIPrefix = URIUtils.getURIPrefix(node.getURI());
			
			if(URIPrefix == null) {
				return null;
			}
			
			Set<Resource> RDFTypes = getRDFTypes(node);
			
			StringBuilder label = new StringBuilder();
			label.append(URIPrefix);
			label.append("*");
			
			if (RDFTypes.size() > 0) {
				
				label.append(" (");
				
				int typeCount = 0;
				
				for(Resource type : RDFTypes) {
					
					String URISuffix = URIUtils.getURISuffix(type.getURI());
					
					if (URISuffix == null) {
						log.warn(String.format("omitting rdf:type %s from label, unable to determine URI suffix", type));
						continue;
					}
					
					label.append(URISuffix);
					
					if (typeCount < (RDFTypes.size() - 1)) {
						label.append(", ");
					}
					
					typeCount++;

				}
				
				label.append(")");
			}
		
			return label.toString();
		}
		
		protected Set<Resource> getRDFTypes(Resource node)
		{
			Set<Resource> types = new HashSet<Resource>();
			for (Statement statement : node.listProperties(RDF.type).toList()) {
				types.add(statement.getObject().asResource());
			}
			return types;
		}
		
		protected void removeStatementsWithBlankNodes(Model model) 
		{
			for (Statement statement : model.listStatements().toList()) {
				Resource s = statement.getSubject();
				RDFNode o = statement.getObject();
				if(s.isAnon() || o.isAnon()) {
					model.remove(statement);
				}
			}
		}
		
		protected boolean isVisitable(Resource node) 
		{
			// We determine if a node is visitable by the number of times 
			// we have visited each of its rdf:types.
			//
			// Special case: If node has no rdf:types, it is always visitable. 
			// (In some cases, this may result in infinite traversals.  This 
			// can be avoided by setting a maximum depth of traversal.)
			
			if (getRDFTypes(node).size() == 0) {
				return true;
			}
			
			boolean isVisitable = false;
			for (Resource type : getRDFTypes(node)) {
				if(!isRDFTypeExhausted(type)) {
					isVisitable = true;
					break;
				}
			}
			
			return isVisitable;
		}

		protected void visit(Resource node) 
		{
			for (Resource type : getRDFTypes(node)) {
				visitRDFType(type);
			}
		}

		protected boolean isRDFTypeExhausted(Resource type) 
		{
			if(!RDFTypeVisitCounter.containsKey(type)) {
				return false;
			}
			return (RDFTypeVisitCounter.get(type) >= maxVisitsPerRDFType);
		}
		
		protected void visitRDFType(Resource type) 
		{
			long visitCount;
			
			if(!RDFTypeVisitCounter.containsKey(type)) {
				visitCount = 1;
			} else {
				visitCount = RDFTypeVisitCounter.get(type) + 1;
			}
			
			if(visitCount <= maxVisitsPerRDFType) {
				log.trace(String.format("this is visit #%d for rdf:type %s (limit is %d)", visitCount, type, maxVisitsPerRDFType));
			}
			
			RDFTypeVisitCounter.put(type, visitCount);
		}
	}
	
	protected static class CommandLineOptions
	{
		public static final int NO_LIMIT = -1;
		
		@Argument(required = true, index = 0, metaVar = "URL", usage = "SPARQL endpoint URL")
		public String endpointURL = null;

		@Argument(required = true, index = 1, metaVar = "FILENAME", usage = "output RDF filename")
		public String outputFilename = null;

		@Argument(required = true, index = 2, metaVar = "URI", usage = "use a specific URI as the root node for the data traversal")
		public List<String> rootNodeURIs;

		@Option(name = "-n", metaVar = "N", aliases = { "--max-visits-per-type" }, usage = "visit the same rdf:type at most N times (default = 5)")
		public int maxVisitsPerType = 5;

		@Option(name = "-d", metaVar = "N", aliases = { "--max-depth" }, usage = "traversal proceeds at most N steps from root URIs (default: no limit)")
		public int maxDepth = NO_LIMIT;
	}
	
	private static final int EXIT_STATUS_SUCCESS = 0;
	private static final int EXIT_STATUS_FAILURE = 1;
	
	public static void main(String[] args)
	{
	
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);

		try {

			cmdLineParser.parseArgument(args);
			
			Model map = ModelFactory.createDefaultModel();
			SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(options.endpointURL, EndpointType.VIRTUOSO);
			
			List<MapperSearchNode> rootNodes = new ArrayList<MapperSearchNode>();
			
			for (String URI : options.rootNodeURIs) {
				Resource root = ResourceFactory.createResource(URI); 
				rootNodes.add(new MapperSearchNode(endpoint, map, root, options.maxVisitsPerType));
			}
			
			OpenGraphIterator<Resource> i;
			
			if (options.maxDepth == CommandLineOptions.NO_LIMIT) {
				i = new BreadthFirstIterator<Resource>(rootNodes);
			} else {
				i = new BoundedBreadthFirstIterator<Resource>(rootNodes, options.maxDepth);
			}
			
			// the side effect of the iteration is to build the schema map
			
			i.iterate();
			
			OutputStream os = new BufferedOutputStream(new FileOutputStream(options.outputFilename)); 
			map.write(os, "N3");

		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err.println("Usage: java -jar sparql-mapper.jar <endpoint URL> <output RDF filename>\n");
			cmdLineParser.printUsage(System.err);
			
			System.exit(EXIT_STATUS_FAILURE);
			
		} catch (Exception e) {
			
			e.printStackTrace();
			System.exit(EXIT_STATUS_FAILURE);
			
		}
		
		System.exit(EXIT_STATUS_SUCCESS);
	}	

}
