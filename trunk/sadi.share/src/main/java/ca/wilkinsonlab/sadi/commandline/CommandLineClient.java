package ca.wilkinsonlab.sadi.commandline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.share.SHAREQueryClient;

public class CommandLineClient
{
	public final static int EXIT_CODE_SUCCESS = 0;
	public final static int EXIT_CODE_NO_RESULTS = 1;
	public final static int EXIT_CODE_FAILURE = 2;
	
	public final static Logger log = Logger.getLogger(CommandLineClient.class);

	private static final String USAGE = "java -jar share.jar < sparql.query.txt";
	
	public static void main(String[] args) 
	{
		try {

			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String query = IOUtils.toString(in);
			
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			List<Map<String, String>> results = new SHAREQueryClient().synchronousQuery(query);
			stopWatch.stop();
			
			System.out.println(String.format("query finished in %d milliseconds", stopWatch.getTime()));
			
			for (Map<String, String> binding : results)
				System.out.println(binding);	
			
			
			System.exit((results.size() > 0) ? EXIT_CODE_SUCCESS : EXIT_CODE_NO_RESULTS);

		}
		catch(Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			System.err.println("\nUsage: " + USAGE);
			System.exit(EXIT_CODE_FAILURE);
		}
	}
}
