package ca.wilkinsonlab.sadi.commandline;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.share.SHAREQueryClient;

public class CommandLineClient
{
	public final static int EXIT_CODE_SUCCESS = 0;
	public final static int EXIT_CODE_NO_RESULTS = 1;
	public final static int EXIT_CODE_FAILURE = 2;
	
	public final static Log log = LogFactory.getLog(CommandLineClient.class);

	private static final String USAGE = "java -jar SHARE.jar \"<SPARQL query>\"";
	
	public static void main(String[] args) 
	{
		try {
			
			if(args.length < 0 || args.length > 1) 
				throw new Exception("incorrect number of arguments");

			String query = args[0];
			List<Map<String, String>> results = new SHAREQueryClient().synchronousQuery(query);
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
