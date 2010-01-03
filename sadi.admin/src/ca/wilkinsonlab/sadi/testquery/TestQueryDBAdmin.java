package ca.wilkinsonlab.sadi.testquery;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.hp.hpl.jena.graph.Triple;

import ca.wilkinsonlab.sadi.optimizer.statistics.ExceededMaxAttemptsException;
import ca.wilkinsonlab.sadi.sparql.VirtuosoSPARQLEndpoint;
import ca.wilkinsonlab.sadi.vocab.TestQueryDB;
import ca.wilkinsonlab.sadi.utils.BasicGraphPatternUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.admin.Config;

public class TestQueryDBAdmin extends VirtuosoSPARQLEndpoint {

	public final static Log LOGGER = LogFactory.getLog(TestQueryDBAdmin.class);
	
	protected final static String CONFIG_ROOT = "sadi.testquerydb";
	protected final static String ENDPOINT_CONFIG_KEY = "endpoint";
	protected final static String GRAPH_CONFIG_KEY = "graph";
	protected final static String USERNAME_CONFIG_KEY = "username";
	protected final static String PASSWORD_CONFIG_KEY = "password";

	//protected final static int MAX_QUERY_LENGTH = 4096; // in chars

	private String graphName;
	
	public TestQueryDBAdmin() throws MalformedURLException
	{
		this(Config.getConfiguration().subset(CONFIG_ROOT).getString(ENDPOINT_CONFIG_KEY),
			Config.getConfiguration().subset(CONFIG_ROOT).getString(GRAPH_CONFIG_KEY),
			Config.getConfiguration().subset(CONFIG_ROOT).getString(USERNAME_CONFIG_KEY),
			Config.getConfiguration().subset(CONFIG_ROOT).getString(PASSWORD_CONFIG_KEY));
	}
	
	public TestQueryDBAdmin(String endpointURI, String graphName, String username, String password) throws MalformedURLException
	{
		super(endpointURI, username, password);
		setGraphName(graphName); 
	}
	
	public String getGraphName() 
	{
		return graphName;
	}
	
	public void setGraphName(String graphName)
	{
		this.graphName = graphName;
	}
	
	public void addQuery(String query, String subgraph, int numConstants, int queryDepth, int queryDiameter, int maxFanout) throws IOException
	{
		String insertQuery = 
			"INSERT INTO GRAPH %u% {" +
			" %u% %u% %v% ." +
			" %u% %u% %v% ." +
			" %u% %u% %s% ." +
			" %u% %u% %s% ." +
			" %u% %u% %v% ." +
			" %u% %u% %v% ." +
			" %u% %u% %v% ." +
			"}";

		long timestamp = System.currentTimeMillis();
		String queryURI = "query" + String.valueOf(timestamp);
		
		insertQuery = SPARQLStringUtils.strFromTemplate(insertQuery,
				getGraphName(),
				queryURI,
				TestQueryDB.PREDICATE_TIMESTAMP,
				String.valueOf(timestamp),
				queryURI,
				TestQueryDB.PREDICATE_NUMCONSTANTS,
				String.valueOf(numConstants),
				queryURI,
				TestQueryDB.PREDICATE_QUERYSTRING,
				query,
				queryURI,
				TestQueryDB.PREDICATE_SUBGRAPHSTRING,
				subgraph,
				queryURI,
				TestQueryDB.PREDICATE_QUERYDEPTH,
				String.valueOf(queryDepth),
				queryURI,
				TestQueryDB.PREDICATE_QUERYDIAMETER,
				String.valueOf(queryDiameter),
				queryURI,
				TestQueryDB.PREDICATE_MAXFANOUT,
				String.valueOf(maxFanout));

		LOGGER.trace("insertQuery: " + insertQuery);
		updateQuery(insertQuery);
	}
	
	public static class CommandLineOptions {
		
		@Option(name="-C", usage="delete all test queries")
		boolean clearTestQueryDB = false;
		
		@Option(name="-n", usage="number of queries to generate (default: 10)")
		public int numQueries = 10;
		
		@Option(name="-d", usage="depth of generated queries (default: 3)")
		public int queryDepth = 3;
		
		@Option(name="-m", usage="maximum number of constants per query (default: 3)")
		public int maxConstantsPerQuery = 3;
	
		@Option(name="-f", usage="maximum fanout at each level of a query (default: 4)")
		public int maxFanout = 4;
	
		@Option(name="-g", usage="named graph in which to record queries")
		public String graphName = Config.getConfiguration().subset(CONFIG_ROOT).getString(GRAPH_CONFIG_KEY);
	}
	
	public static void main(String[] args) throws IOException, ExceededMaxAttemptsException
	{
		
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);
		try {
				
			cmdLineParser.parseArgument(args);

			TestQueryDBAdmin queryDB = new TestQueryDBAdmin();
			RandomQueryGenerator generator = new RandomQueryGenerator(ca.wilkinsonlab.sadi.client.Config.getSPARQLRegistry());
			
			queryDB.setGraphName(options.graphName);
			
			if(options.clearTestQueryDB) {
				log.trace("clearing test query graph");
				queryDB.clearGraph(queryDB.getGraphName());
				return;
			}
			
			for(int i = 0; i < options.numQueries; i++) {

				List<Triple> subgraph = generator.generateRandomBasicGraphPattern(options.maxConstantsPerQuery, options.queryDepth, options.maxFanout);
				
				String subgraphStr = SPARQLStringUtils.getSPARQLQueryString(subgraph);
				List<Triple> bgp = generator.substituteVarsForConstants(subgraph, options.maxConstantsPerQuery);
				String query = SPARQLStringUtils.getSPARQLQueryString(bgp);
				
				int numConstants = BasicGraphPatternUtils.getNumConstants(bgp);
				int diameter = BasicGraphPatternUtils.getDiameter(bgp);
				
				queryDB.addQuery(query, subgraphStr, numConstants, options.queryDepth, diameter, options.maxFanout);
			}

		}
		catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: testquerydb [-n numQueries] [-d maxQueryDepth] [-m maxConstantsPerQuery] [-f maxFanout]");
			cmdLineParser.printUsage(System.err);
		}
		
	}
	
}
