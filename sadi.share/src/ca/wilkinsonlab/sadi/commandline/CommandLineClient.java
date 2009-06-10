package ca.wilkinsonlab.sadi.commandline;

import java.util.List;
import java.util.Map;

import ca.wilkinsonlab.sadi.pellet.PelletClient;

public class CommandLineClient {
	
	private static final String USAGE = "java -jar SHARE.jar \"<SPARQL query>\"";
	
	public static void main(String[] args) 
	{
		try {
			
			if(args.length < 0 || args.length > 1) 
				throw new Exception("Incorrect number of arguments");
		
			String query = args[0];
			List<Map<String, String>> results = new PelletClient().synchronousQuery(query);
			for (Map<String, String> binding : results)
				System.out.println(binding);			

		}
		catch(Exception e) {
			System.out.println(e.getStackTrace());
			System.out.println("Usage: " + USAGE);
		}
	}
	
}
