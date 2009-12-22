package ca.wilkinsonlab.sadi.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpointFactory;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint.AmbiguousPropertyTypeException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Small command line utility that builds an OWL file for a SPARQL endpoint.
 * The OWL file defines each predicate that occurs in the endpoint as either
 * an owl:ObjectProperty or an owl:DatatypeProperty.
 * 
 * @author Ben Vandervalk
 */

public class BuildOWLForEndpoint {

	public final static Logger log = Logger.getLogger(BuildOWLForEndpoint.class);
	
	protected static OntModel buildOntologyByQuery(SPARQLEndpoint endpoint) throws IOException 
	{
		OntModel ontology = ModelFactory.createOntologyModel();
		Set<String> properties = endpoint.getPredicates();
		for(String property : properties) {
			try {
				if(endpoint.isDatatypeProperty(property, false)) {
					ontology.createDatatypeProperty(property);
				} else {
					ontology.createObjectProperty(property);
				}
			} catch(AmbiguousPropertyTypeException e) {
				log.warn("skipping property " + property + " (has both literal and non-literal object values)");
			}
		}
		return ontology;
	}
	
	public static class CommandLineOptions {

		public List<String> rootURIs = new ArrayList<String>();
		
		@Argument(required = true, index = 0, usage = "SPARQL endpoint URL")
		public String endpointURL = null;
		
		@Option(name = "-r", usage = "root URI for depth first traversal of triples")
		public void addRootURI(String rootURI) { rootURIs.add(rootURI); }

		@Option(name = "-o", usage = "output OWL filename")
		public String outputOWLFile = null;
		
	}
	

	public static void main(String[] args) 
	{

		// Parse command line args
		
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
		}
		catch(CmdLineException e) {
			log.error(e.getMessage());
			log.error("buildOWLForEndpoint [options...] endpointURL");
			log.error(BuildOWLForEndpoint.getUsageString(parser));
			System.exit(1);
		}
		
		// Open output file (defaults to STDOUT)
		
		Writer outputWriter = new PrintWriter(System.out);
				
		if(options.outputOWLFile != null) {
			try {
				outputWriter = new BufferedWriter(new FileWriter(options.outputOWLFile));
			}
			catch(IOException e) {
				log.error("unable to open output file: " + e.getMessage());
				System.exit(1);
			}
		}
		
		// Build the OWL ontology
		
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(options.endpointURL);
		OntModel ontology = null;
		
		try {
			if(options.rootURIs.size() == 0) {
				ontology = buildOntologyByQuery(endpoint);
			}
			else {
				for(String rootURI : options.rootURIs) {
					// TODO: For endpoints that are too large to be queried for the full set of
					// predicates, do a breadth-first search starting at the given root URI.  
					// Note: The Java Search Library looks like it would handy for this.
				}
			}
		} catch(IOException e) {
			log.error("error querying endpoint: " + e.getMessage());
			System.exit(1);
		}

		// Write out the OWL file
		
		ontology.write(outputWriter);
		
	}
	
	protected static String getUsageString(CmdLineParser parser) 
	{
		StringWriter writer = new StringWriter();
		parser.printUsage(writer, null);
		return writer.toString();
	}
}
