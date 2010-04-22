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

	public static class CommandLineOptions {
		
		public enum OperationType {
			RECOMPUTE_STATS
		};

		public List<OperationType> operations = new ArrayList<OperationType>();
		
		@Argument(index=0, required=true, usage="stats DB endpoint URL")
		String endpointURL = null;
		
		@Option(name="-u", aliases={"--username"}, usage="username")
		String username = null;
		
		@Option(name="-p", aliases={"--password"}, usage="password")
		String password = null;

		@Option(name="-r", aliases={"--recompute-stats"}, usage="recompute summary statistics from samples")
		protected void recomputeStats(boolean unused) { operations.add(OperationType.RECOMPUTE_STATS); }
		
	}
	
	public static void main(String[] args)
	{
		
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);
		
		try {

			cmdLineParser.parseArgument(args);
			
			PredicateStatsDB statsDB = new PredicateStatsDB(options.endpointURL, options.username, options.password);
			
			if(options.operations.size() == 0) {
				throw new CmdLineException("no actions specified");
			}
				
			for(CommandLineOptions.OperationType operationType : options.operations) {
				
				switch(operationType) {
				
				case RECOMPUTE_STATS:
					statsDB.recomputeStats();
					break;
				
				default:
					System.err.println(String.format("operation %s not yet implemented", operationType.toString()));
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
