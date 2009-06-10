package ca.wilkinsonlab.sadi.optimizer;

import java.io.IOException;
import java.rmi.AccessException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.hp.hpl.jena.graph.Triple;

import ca.wilkinsonlab.sadi.sparql.VirtuosoSPARQLEndpoint;
import ca.wilkinsonlab.sadi.sparql.VirtuosoSPARQLRegistry;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;
import ca.wilkinsonlab.sadi.vocab.TestQuery;
import ca.wilkinsonlab.sadi.utils.BasicGraphPatternUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

public class TestQueryDBAdmin extends VirtuosoSPARQLEndpoint {

	public final static Log LOGGER = LogFactory.getLog(TestQueryDBAdmin.class);
	
	public TestQueryDBAdmin() {
		this(TestQuery.DEFAULT_QUERYDB_ENDPOINT);
	}
	
	public TestQueryDBAdmin(String endpointURI) {
		super(endpointURI);
	}
	
	public void addQuery(String query, String subgraph, int numConstants, int queryDiameter, int maxFanout) throws HttpException, HttpResponseCodeException, IOException, AccessException
	{
		String insertQuery = 
			"INSERT INTO GRAPH %u% {" +
			" %u% %u% %v% ." +
			" %u% %u% %v% ." +
			" %u% %u% %s% ." +
			" %u% %u% %s% ." +
			" %u% %u% %v% ." +
			" %u% %u% %v% ." +
			"}";

		long timestamp = System.currentTimeMillis();
		String queryURI = "query" + String.valueOf(timestamp);
		
		insertQuery = SPARQLStringUtils.strFromTemplate(insertQuery,
				TestQuery.DEFAULT_QUERYDB_GRAPH,
				queryURI,
				TestQuery.PREDICATE_TIMESTAMP,
				String.valueOf(timestamp),
				queryURI,
				TestQuery.PREDICATE_NUMCONSTANTS,
				String.valueOf(numConstants),
				queryURI,
				TestQuery.PREDICATE_QUERYSTRING,
				query,
				queryURI,
				TestQuery.PREDICATE_SUBGRAPHSTRING,
				subgraph,
				queryURI,
				TestQuery.PREDICATE_QUERYDIAMETER,
				String.valueOf(queryDiameter),
				queryURI,
				TestQuery.PREDICATE_MAXFANOUT,
				String.valueOf(maxFanout));

		LOGGER.trace("insertQuery: " + insertQuery);
		updateQuery(insertQuery);
	}
	
	public void addQueries(Collection<String> queries, int numConstantsPerQuery, int queryDepth) throws HttpException, HttpResponseCodeException, IOException, AccessException
	{
//		for(String query : queries) 
//			addQuery(query, numConstantsPerQuery, queryDepth);
	}
	
	public void addRandomQueries(int numQueries, int numConstantsPerQuery, int queryDepth) throws HttpException, HttpResponseCodeException, IOException, AccessException
	{
		RandomQueryGenerator generator = new RandomQueryGenerator(new VirtuosoSPARQLRegistry());
//		Collection<String> queries = generator.generateQueries(numQueries, numConstantsPerQuery, queryDepth);
//		addQueries(queries, numConstantsPerQuery, queryDepth);
	}

	public static class CommandLineOptions {
		
		@Option(name="-n", usage="number of queries to generate")
		public int numQueries = 10;
		
		@Option(name="-d", usage="depth of generated queries")
		public int queryDepth = 3;
		
		@Option(name="-m", usage="maximum number of constants per query")
		public int maxConstantsPerQuery = 3;
	
		@Option(name="-f", usage="maximum fanout at each level of a query")
		public int maxFanout = 4;
		
	}
	
	public static void main(String[] args) throws HttpException, HttpResponseCodeException, IOException
	{
		
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);
		try {

			cmdLineParser.parseArgument(args);

			TestQueryDBAdmin queryDB = new TestQueryDBAdmin();
			RandomQueryGenerator generator = new RandomQueryGenerator(new VirtuosoSPARQLRegistry());
			
			for(int i = 0; i < options.numQueries; i++) {
				List<Triple> subgraph = generator.generateRandomBasicGraphPattern(options.maxConstantsPerQuery, options.queryDepth, options.maxFanout);
				String subgraphStr = BasicGraphPatternUtils.getSPARQLQueryString(subgraph);
				List<Triple> bgp = generator.substituteVarsForConstants(subgraph, options.maxConstantsPerQuery);
				String query = BasicGraphPatternUtils.getSPARQLQueryString(bgp);
				int numConstants = BasicGraphPatternUtils.getNumConstants(bgp);
				int diameter = BasicGraphPatternUtils.getDiameter(bgp);
				queryDB.addQuery(query, subgraphStr, numConstants, diameter, options.maxFanout);
			}

		}
		catch (CmdLineException e) {
			System.err.println(e.getMessage());
			cmdLineParser.printUsage(System.err);
		}
		
	}
	
}
