package ca.wilkinsonlab.sadi.stats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Command line interface to predicate stats DB.
 * Currently, the only supported operation is 
 * recomputing the summary stats from the 
 * available samples. 
 * 
 * @author Ben Vandervalk
 */
public class PredicateStatsDBCommandLineClient 
{
	public static class CommandLineOptions 
	{
		public enum OperationType {
			RECOMPUTE_STATS,
			CLEAR_ALL,
			PURGE_SAMPLES,
			PRINT_NUM_SAMPLES,
		};

		public List<Operation> operations = new ArrayList<Operation>();
		
		public static class Operation
		{
			public OperationType opType;
			public Object opArg;
			
			Operation(OperationType opType) 
			{
				this.opType = opType;
				this.opArg = null;
			}
			
			Operation(OperationType opType, Object opArg) 
			{
				this.opType = opType;
				this.opArg = opArg;
			}
		}
		
		@Argument(index=0, metaVar="<ENDPOINT URL>", required=true, usage="stats DB endpoint URL")
		String endpointURL = null;
		
		@Option(name="-u", aliases={"--username"}, usage="username")
		String username = null;
		
		@Option(name="-p", aliases={"--password"}, usage="password")
		String password = null;

		@Option(name="-s", metaVar="<URI>", aliases={"--samples-graph"}, usage="the named graph in the statsdb that contains the samples (i.e. predicate resolution times)")
		String statsSamplesGraph = PredicateStatsDB.DEFAULT_SAMPLES_GRAPH;
		
		@Option(name="-S", metaVar="<URI>", aliases={"--summary-stats-graph"}, usage="the named graph in the statsdb that contains the summary stats for predicates (i.e. regression lines)")
		String statsSummaryGraph = PredicateStatsDB.DEFAULT_STATS_GRAPH;
		
		@Option(name="-r", aliases={"--recompute-stats"}, usage="recompute summary statistics from samples")
		protected void recomputeStats(boolean unused) { operations.add(new Operation(OperationType.RECOMPUTE_STATS)); }

		@Option(name="-n", aliases={"--print-num-samples"}, usage="print the current number of samples in the stats database on STDOUT")
		protected void printNumSamples(boolean unused) { operations.add(new Operation(OperationType.PRINT_NUM_SAMPLES)); }
		
		@Option(name="-P", metaVar="<NUM_SAMPLES>", aliases={"--purge-oldest-samples"}, usage="purge the oldest NUM_SAMPLES samples from the stats database")
		protected void purgeOldestSamples(int numSamples) { operations.add(new Operation(OperationType.PURGE_SAMPLES, new Integer(numSamples))); }
				
		@Option(name="-c", aliases={"--clear-all"}, usage="delete all data in the stats database (both the samples and summary statistics)")
		protected void clearAll(boolean unused) { operations.add(new Operation(OperationType.CLEAR_ALL)); }
		
	}
	
	public static void main(String[] args)
	{
		
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);
		
		try {

			cmdLineParser.parseArgument(args);
			
			PredicateStatsDB statsDB = new PredicateStatsDB(
					options.endpointURL,
					options.username,
					options.password,
					options.statsSamplesGraph,
					options.statsSummaryGraph);
			
			if(options.operations.size() == 0) {
				throw new CmdLineException("no actions specified");
			}
				
			for(CommandLineOptions.Operation op : options.operations) {
				
				switch(op.opType) {
				
				case RECOMPUTE_STATS:
					statsDB.recomputeStats();
					break;
					
				case PURGE_SAMPLES:
					int numSamples = (Integer)op.opArg;
					statsDB.purgeSamples(numSamples);
					break;
					
				case PRINT_NUM_SAMPLES:
					System.out.print(statsDB.getNumSamples());
					break;
					
				case CLEAR_ALL:
					statsDB.clear();
					break;
				
				default:
					System.err.println(String.format("operation %s not yet implemented", op.opType.toString()));
					break;
				
				}
			}
		
		} catch(IOException e) {

			System.err.println("error communicating with stats DB: " + ExceptionUtils.getStackTrace(e));
			System.exit(1);
		
		} catch (CmdLineException e) {
		
			System.err.println(e.getMessage());
			System.err.println("usage: java -jar statsdb.jar [options] <endpoint URL>");
			cmdLineParser.printUsage(System.err);
			System.exit(1);
		}
		
		System.exit(0);
		
	}	
}
