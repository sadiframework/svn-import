package ca.wilkinsonlab.utils.darq.virtuoso;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import ca.wilkinsonlab.darq.index.Capability;
import ca.wilkinsonlab.darq.index.NamedGraphIndex;
import ca.wilkinsonlab.darq.index.ServiceDescription;
import ca.wilkinsonlab.utils.URIUtils;
import ca.wilkinsonlab.utils.VirtuosoUtils;
import ca.wilkinsonlab.vocab.DARQ;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;



public class DARQIndexer 
{
	protected static final Logger log = Logger.getLogger(DARQIndexer.class);
	
	protected static final int PROGRESS_INTERVAL_IN_TRIPLES = 10000; 
	protected static final String USAGE = "USAGE: java -jar darq.indexer.for.virtuoso.jar [--host hostname] [--port port] [--username username] [--password password]";
	
	public final static int EXIT_CODE_SUCCESS = 0;
	protected final static int EXIT_CODE_FAILURE = 1;
	
	protected static class CommandLineOptions 
	{
		@Option(name = "-h", aliases={"--help"}, usage = "Virtuoso hostname (default: localhost)")
		public boolean help = false;

		@Option(name = "-H", metaVar = "HOST", aliases={"--host"}, usage = "Virtuoso hostname (default: localhost)")
		public String host = "localhost";

		@Option(name = "-P", metaVar = "PORT", aliases={"--port"}, usage = "Virtuoso port (default: 1111)")
		public int port = 1111;
		
		@Option(name = "-u", metaVar = "USERNAME", aliases={"--username"}, usage = "Virtuoso username (default: \"dba\")")
		public String username = "dba";
		
		@Option(name = "-p", metaVar = "PASSWORD", aliases={"--password"}, usage = "Virtuoso password (default: \"dba\")")
		public String password = "dba";
		
		@Option(name = "-l", metaVar = "STRING", aliases={"--label"}, usage = "a human-readable label for the endpoint (e.g. \"Bio2RDF UniProt SPARQL Endpoint\")")
		public String label;
		
		@Option(name = "-d", metaVar = "STRING", aliases={"--description"}, usage = "a description for the endpoint (e.g. \"This endpoint publishes an RDF version of the UniProt protein database.\")")
		public String description;
		
		@Option(name = "-r", metaVar = "INTEGER", aliases={"--results-limit"}, usage = "the results limit for queries to the endpoint")
		public long resultsLimit = ServiceDescription.NO_RESULTS_LIMIT;
	}
	
	public static void main(String[] args) 
	{
		
		/* parse commandline options */
		
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);
		
		try {
			
			cmdLineParser.parseArgument(args);
			
		} catch(CmdLineException e) {
			
			log.error(e.getMessage());
			log.error("");
			printUsage(cmdLineParser);
			
			System.exit(EXIT_CODE_FAILURE);
			
		} 
		
		/* print help if requested */
		
		if(options.help) {
			printUsage(cmdLineParser);
			System.exit(EXIT_CODE_SUCCESS);
		}
		
		/* test that we can read from the database */
		
		if(!VirtuosoUtils.testRead(options.host, options.port, options.username, options.password)) {

			log.error(String.format("Unable to connect to Virtuoso at %s:%d. Please check that you have supplied correct values for host, port, username, and password.", options.host, options.port));
			log.error("");
			printUsage(cmdLineParser);
			
			System.exit(EXIT_CODE_FAILURE);
			
		}
		
		/* test that we can write to the database */
		
		if(!VirtuosoUtils.testWrite(DARQ.serviceDescriptionGraph.getURI(), options.host, options.port, options.username, options.password)) {
		
			log.error(String.format("No write permission for named graph <%s>, which will be used to store the DARQ index. Please check that you have supplied correct values for username and password.", DARQ.serviceDescriptionGraph));
			log.error("");
			printUsage(cmdLineParser);

			System.exit(EXIT_CODE_FAILURE);
		
		}
		
		/* build the index */
		
		ServiceDescription serviceDescription =	
			
			buildServiceDescription(
			
				options.host, 
				options.port, 
				options.username, 
				options.password,
				options.label,
				options.description,
				options.resultsLimit
				
			);

		/* store the index in the triple store */
		
		writeServiceDescription(options.host, options.port, options.username, options.password, serviceDescription);

		/* success! */
		
		log.info(String.format("successfully indexed %d triples in %d named graphs", serviceDescription.numTriples, serviceDescription.namedGraphs.keySet().size()));
		log.info(String.format("wrote updated index to named graph <%s>", DARQ.serviceDescriptionGraph));

		/* FOR DEBUGGING
		 
		Model model = serviceDescription.asModel();
		
		model.setNsPrefix("sd", DARQ.URI_PREFIX);
		model.setNsPrefix("ext", DARQExt.URI_PREFIX);

		model.write(System.out, "TTL");
		System.out.flush();
		
		*/
		
		System.exit(EXIT_CODE_SUCCESS);
		
	}
	
	static protected ServiceDescription buildServiceDescription(
			
			String host, 
			int port, 
			String username, 
			String password,
			String label,
			String description,
			long resultsLimit
		
	) 
	{

		ServiceDescription serviceDescription = new ServiceDescription(label, description, resultsLimit);
		
		/*
		 * Iterate through each triple (quadruple, actually) in the endpoint and
		 * build a DARQ service description.
		 * 
		 * In principle, I could also have used Virtuoso's implementation of a
		 * Jena model (i.e. VirtModel) to do this.  However, for some reason
		 * the method VirtModel.openDefaultModel (which creates a Model for the 
		 * default graph) doesn't return any data.  VirtModel works fine if a
		 * specific named graph is used.
		 * 
		 * We could also iterate through the data using the Virtuoso JDBC 
		 * driver, although this raises the difficulty of determining whether
		 * the object values of triples are URIs or Literals. (It is possible to 
		 * issue SPARQL queries through the JDBC driver.)
		 */
		
		VirtGraph set = new VirtGraph(VirtuosoUtils.getJDBCConnectionString(host, port), username, password);
		
		Query sparql = QueryFactory.create("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } }"); // limit 100");
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (sparql, set);
		ResultSet results = vqe.execSelect();

		/* Build the index.  */
		
		
		while (results.hasNext()) {
			
			QuerySolution result = results.nextSolution();
			
			Resource s = result.get("s").asResource();
			Resource p = result.get("p").asResource();
			RDFNode o = result.get("o");
			Resource g = result.get("g").asResource();
			
			String predicateURI = p.getURI();
			String graphURI = g.getURI();

			//System.out.println(String.format("(s, p, o, g) = (%s, %s, %s, %s)", s, p, o, g));

			/* Skip over triples that are part of a previously created DARQ index. */  

			if(g.equals(DARQ.serviceDescriptionGraph)) {
				continue;
			}
			
			serviceDescription.numTriples++;
			
			if((serviceDescription.numTriples % PROGRESS_INTERVAL_IN_TRIPLES) == 0) {
				log.info(String.format("progress: indexed %d triples", serviceDescription.numTriples));
			}
			
			if(!serviceDescription.namedGraphs.containsKey(graphURI)) {
				serviceDescription.namedGraphs.put(graphURI, new NamedGraphIndex());
			}

			Map<String,Capability> graphIndex = serviceDescription.namedGraphs.get(graphURI);
			
			if(!graphIndex.containsKey(predicateURI)) {
				graphIndex.put(predicateURI, new Capability(predicateURI));
			}
			
			Capability capability = graphIndex.get(predicateURI);
			
			capability.numTriples++;
			
			if(s.isURIResource()) {
				String uriPrefix = URIUtils.getURIPrefix(s.getURI()); 
				String regex = String.format("^%s", Pattern.quote(uriPrefix));
				capability.subjectRegexes.add(regex);
			}
			
			if(o.isURIResource()) {
				String uriPrefix = URIUtils.getURIPrefix(o.asResource().getURI()); 
				String regex = String.format("^%s", Pattern.quote(uriPrefix));
				capability.objectRegexes.add(regex);
			}
		
		}
		
		vqe.close();
		
		return serviceDescription;
	}
	
	static protected void writeServiceDescription(String host, int port, String username, String password, ServiceDescription serviceDescription)
	{
		Model indexModel = serviceDescription.asModel();
		Model targetModel = VirtModel.openDatabaseModel(DARQ.serviceDescriptionGraph.getURI(), VirtuosoUtils.getJDBCConnectionString(host, port), username, password);
		targetModel.removeAll();
		targetModel.add(indexModel);
	
		indexModel.close();
		targetModel.close();
	}
	
	static protected void genericErrorMessage(Throwable e) 
	{
		log.error(String.format("Failed to build DARQ index: %s", e.getMessage()));
		log.error("If a DARQ index already exists in the endpoint, it has been left unchanged.");
	}
	
	static protected void printUsage(CmdLineParser cmdLineParser)
	{
		log.error(USAGE);
		log.error("");
		log.error("Options:");
		log.error("");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		cmdLineParser.printUsage(baos);
		
		log.error(baos.toString());
	}

	
}
