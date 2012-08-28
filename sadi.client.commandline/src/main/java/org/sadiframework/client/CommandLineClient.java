package org.sadiframework.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.sadiframework.SADIException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class CommandLineClient {

	protected static final Logger log = Logger.getLogger(CommandLineClient.class);

	protected static final String USAGE = "Usage: java -jar sadi-client-commandline-$VERSION.jar [options] $SERVICE_URL < input.rdf > output.rdf";
	protected static final int EXIT_STATUS_SUCCESS = 0;
	protected static final int EXIT_STATUS_ERROR = 1;

	static public class CommandLineOptions
	{
		@Option(name="-h", aliases={"--help"}, usage="print this message")
		boolean help;

		@Option(name="-i", aliases={"--input-file"}, metaVar="<FILENAME>", usage="read input from file instead of STDIN")
		String inputFile = null;

		@Option(name="-o", aliases={"--output-file"}, metaVar="<FILENAME>", usage="write output to file instead of STDOUT")
		String outputFile = null;

		@Option(name="-I", aliases={"--input-format"}, metaVar="<RDF FORMAT>", usage="choices: 'RDF/XML' or 'N3'")
		String inputFormat = "RDF/XML";

		@Option(name="-O", aliases={"--output-format"}, metaVar="<RDF FORMAT>", usage="choices: 'RDF/XML' or 'N3'")
		String outputFormat = "RDF/XML";

		@Argument
		String serviceURL = null;
	}

	public static void main(String[] args) throws IOException, SADIException
	{
		CommandLineOptions options = parseCommandLineOptions(args);
		Model inputModel = readInput(options);
		Model outputModel = invokeService(options.serviceURL, inputModel);
		writeOutput(outputModel, options);
	}

	protected static Model readInput(CommandLineOptions options) throws FileNotFoundException
	{
		Model model = ModelFactory.createDefaultModel();
		InputStream is;
		if (options.inputFile != null) {
			is = new FileInputStream(options.inputFile);
		} else {
			is = System.in;
		}
		model.read(new BufferedInputStream(is), "", options.inputFormat);
		return model;
	}

	protected static Model invokeService(String serviceURL, Model inputModel) throws SADIException
	{
		Service service = ServiceFactory.createService(serviceURL);
		log.trace(String.format("invoking service %s", serviceURL));
		return service.invokeService(getInputNodes(service, inputModel));
	}

	protected static void writeOutput(Model model, CommandLineOptions options) throws FileNotFoundException
	{
		OutputStream os;
		if (options.outputFile != null) {
			os = new FileOutputStream(options.outputFile);
		} else {
			os = System.out;
		}
		model.write(new BufferedOutputStream(os), options.outputFormat);
	}

	protected static CommandLineOptions parseCommandLineOptions(String[] commandLineArgs)
	{
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(commandLineArgs);
		} catch(CmdLineException e) {
			exitWithUsage(parser, e.getMessage(), EXIT_STATUS_ERROR);
		}

		if (options.help) {
			exitWithUsage(parser, null, EXIT_STATUS_SUCCESS);
		} else if (options.serviceURL == null) {
			exitWithUsage(parser, "Missing argument: no service URL specified", EXIT_STATUS_ERROR);
		}

		return options;
	}

	protected static Collection<Resource> getInputNodes(Service service, Model model) throws SADIException
	{
		return model.listSubjectsWithProperty(RDF.type, service.getInputClass()).toList();
	}

	protected static void exitWithUsage(CmdLineParser parser, String messageBeforeUsage, int exitStatus)
	{
		if (messageBeforeUsage != null) {
			System.err.println(messageBeforeUsage);
			System.err.println();
		}
		System.err.println(USAGE);
		System.err.println();
		System.err.println("Options:");
		System.err.println();
		parser.printUsage(System.err);
		System.exit(exitStatus);
	}
}
