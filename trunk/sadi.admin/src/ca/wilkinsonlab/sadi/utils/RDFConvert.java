package ca.wilkinsonlab.sadi.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class RDFConvert {
	
	public static class CommandLineOptions {
		
		@Argument(required = true, multiValued = true, index = 0, usage = "input RDF file")
		public List<String> inputFiles;
		
		@Option(name = "-i", required = true, usage = "input RDF format (options: \"RDF/XML\", \"N3\", \"N-TRIPLES\"")
		public String inputFormat = "RDF/XML";

		@Option(name = "-o", required = true, usage = "output RDF format (options: \"RDF/XML\", \"N3\", \"N-TRIPLES\", \"TSV\"")
		public String outputFormat = "RDF/XML";

	}
	
	public static void main(String[] args) throws IOException 
	{
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
		}
		catch(CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("rdfconvert -i INPUTFORMAT -o OUTPUTFORMAT <input file 1> <input file 2> ...");
			System.exit(1);
		}
		
		Model model = ModelFactory.createMemModelMaker().createFreshModel();
		
		for(String filename : options.inputFiles) {
			
			boolean isURL;
			try {
				new URL(filename);
				isURL = true;
			} catch(MalformedURLException e) {
				isURL = false;
			}
			
			if(isURL) {
				model.read(filename, options.inputFormat);
			} else {
				Reader reader = new BufferedReader(new FileReader(filename));
				model.read(reader, "", options.inputFormat);
				reader.close();
			}
		}
		
		Writer writer = new PrintWriter(System.out);
		
		if(options.outputFormat.toUpperCase().equals("TSV")) {
			for(StmtIterator i = model.listStatements(); i.hasNext(); ) {
				Statement stmt = i.next();
				writer.write(stmt.getResource() + " " + stmt.getPredicate() + " " + stmt.getObject() + "\n");
			}
		} else {
			model.write(writer, options.outputFormat);
		}
		
		model.close();
		writer.close();
	}
	
}
