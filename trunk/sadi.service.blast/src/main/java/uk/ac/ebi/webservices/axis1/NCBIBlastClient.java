/* $Id: NCBIBlastClient.java 1589 2010-09-24 19:23:44Z hpm $
 * ======================================================================
 * JDispatcher NCBI BLAST (SOAP) web service Java client using Axis 1.x.
 * ----------------------------------------------------------------------
 * Tested with:
 *   Sun Java 1.5.0_17 with Apache Axis 1.4 on CentOS 5.2.
 * ====================================================================== */
package uk.ac.ebi.webservices.axis1;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import javax.xml.rpc.ServiceException;
import org.apache.commons.cli.*;
import uk.ac.ebi.webservices.axis1.stubs.ncbiblast.*;

/** <p>JDispatcher NCBI BLAST (SOAP) web service Java client using Apache 
 * Axis 1.x.</p>
 * 
 * <p>See:</p>
 * <ul>
 * <li><a href="http://www.ebi.ac.uk/Tools/webservices/services/sss/ncbi_blast_soap">http://www.ebi.ac.uk/Tools/webservices/services/sss/ncbi_blast_soap</a></li>
 * <li><a href="http://www.ebi.ac.uk/Tools/webservices/tutorials/06_programming/java">http://www.ebi.ac.uk/Tools/webservices/tutorials/06_programming/java</a></li>
 * <li><a href="http://ws.apache.org/axis/">http://ws.apache.org/axis/</a></li>
 * </ul>
 */
public class NCBIBlastClient extends uk.ac.ebi.webservices.AbstractWsToolClient {
	/** Proxy object for web service. */
	private JDispatcherService_PortType srvProxy = null;
	/** Client version/revision for use in user-agent string. */
	private String revision = "$Revision: 1589 $";
	/** Tool specific usage for help. */
	private static final String usageMsg = "NCBI BLAST\n"
		+ "==========\n"
		+ "\n"
		+ "Rapid sequence database search programs utilizing the BLAST algorithm\n"
		+ "\n"
		+ "[Required]\n"
		+ "\n"
		+ "  -p, --program        : str  : BLAST program to use: see --paramDetail program\n"
		+ "  -D, --database       : str  : database(s) to search, space seperated: see\n"
		+ "                                --paramDetail database\n"
		+ "      --stype          : str  : query sequence type\n"
		+ "  seqFile              : file : query sequence (\"-\" for STDIN)\n"
		+ "\n"
		+ "[Optional]\n"
		+ "\n"
		+ "  -m, --matrix         : str  : scoring matrix, see --paramDetail matrix\n"
		+ "  -e, --exp            : real : 0<E<= 1000. Statistical significance threshold\n"
		+ "                                for reporting database sequence matches.\n"
		+ "  -f, --filter         :      : low complexity sequence filter, see\n"
		+ "                                --paramDetail filter\n"
		+ "  -A, --align          : int  : alignment format, see --paramDetail align\n"
		+ "  -s, --scores         : int  : maximum number of scores to report\n"
		+ "  -n, --alignments     : int  : maximum number of alignments to report\n"
		+ "  -u, --match          : int  : score for a match (BLASTN only)\n"
		+ "  -v, --mismatch       : int  : score for a missmatch (BLASTN only)\n"
		+ "  -o, --gapopen        : int  : gap open penalty\n"
		+ "  -x, --gapext         : int  : gap extension penalty\n"
		+ "  -d, --dropoff        : int  : drop-off score\n"
		+ "  -g, --gapalign       :      : optimise gapped alignments\n"
		+ "      --seqrange       : str  : region in query sequence to use for search\n";

	/** Default constructor.
	 */
	public NCBIBlastClient() {
		// Set the HTTP user agent string for (java.net) requests.
		this.setUserAgent();
	}
	
	/** <p>Set the HTTP User-agent header string for the client.</p>
	 * 
	 * <p><b>Note</b>: this affects all java.net based requests, but not the 
	 * Axis requests. The user-agent used by Axis is set from the 
	 * /org/apache/axis/i18n/resource.properties file included in the JAR.</p>
	 */
	private void setUserAgent() {
		printDebugMessage("setUserAgent", "Begin", 1);
		// Java web calls use the http.agent property as a prefix to the default user-agent.
		String clientVersion = this.revision.substring(11, this.revision.length() - 2);
		String clientUserAgent = "EBI-Sample-Client/" + clientVersion + " (" + this.getClass().getName() + "; " + System.getProperty("os.name") +")";
		if(System.getProperty("http.agent") != null) {
			System.setProperty("http.agent", clientUserAgent + " " + System.getProperty("http.agent"));
		}
		else System.setProperty("http.agent", clientUserAgent);
		printDebugMessage("setUserAgent", "End", 1);
	}

	/** Print usage message. */
	private static void printUsage() {
		System.out.println(usageMsg);
		printGenericOptsUsage();
	}
	
	/** Ensure that a service proxy is available to call the web service.
	 * 
	 * @throws ServiceException
	 */
	protected void srvProxyConnect() throws ServiceException {
		printDebugMessage("srvProxyConnect", "Begin", 11);
		if(this.srvProxy == null) {
			JDispatcherService_Service service =  new JDispatcherService_ServiceLocator();
			if(this.getServiceEndPoint() != null) {
				try {
					this.srvProxy = service.getJDispatcherServiceHttpPort(new java.net.URL(this.getServiceEndPoint()));
				}
				catch(java.net.MalformedURLException ex) {
					System.err.println(ex.getMessage());
					System.err.println("Warning: problem with specified endpoint URL. Default endpoint used.");
					this.srvProxy = service.getJDispatcherServiceHttpPort();					
				}
			}
			else {
				this.srvProxy = service.getJDispatcherServiceHttpPort();
			}
		}
		printDebugMessage("srvProxyConnect", "End", 11);
	}

	/** Get the web service proxy so it can be called directly.
	 * 
	 * @return The web service proxy.
	 * @throws javax.xml.rpc.ServiceException
	 */
	public JDispatcherService_PortType getSrvProxy() throws javax.xml.rpc.ServiceException {
		printDebugMessage("getSrvProxy", "", 1);
		this.srvProxyConnect(); // Ensure the service proxy exists
		return this.srvProxy;
	}
	
	/** Get list of tool parameter names.
	 * 
	 * @return String array containing list of parameter names
	 * @throws ServiceException
	 * @throws RemoteException
	 */
	public String[] getParams() throws ServiceException, RemoteException {
		printDebugMessage("getParams", "Begin", 1);
		String[] retVal = null;
		this.srvProxyConnect(); // Ensure the service proxy exists
		retVal = this.srvProxy.getParameters();
		printDebugMessage("getParams", retVal.length + " params", 2);
		printDebugMessage("getParams", "End", 1);
		return retVal;
	}

	/** Get detailed information about the specified tool parameter.
	 * 
	 * @param paramName Tool parameter name
	 * @return Object describing tool parameter
	 * @throws ServiceException
	 * @throws RemoteException
	 */
	public WsParameterDetails getParamDetail(String paramName) throws ServiceException, RemoteException {
		printDebugMessage("getParamDetail", paramName, 1);
		this.srvProxyConnect(); // Ensure the service proxy exists
		return this.srvProxy.getParameterDetails(paramName);
	}

	/** Print detailed information about a tool parameter.
	 * 
	 * @param paramName Name of the tool parameter to get information for.
	 * @throws RemoteException
	 * @throws ServiceException
	 */
	protected void printParamDetail(String paramName) throws RemoteException, ServiceException {
		printDebugMessage("printParamDetail", "Begin", 1);
		WsParameterDetails paramDetail = getParamDetail(paramName);
		// Print object
		System.out.println(paramDetail.getName() + "\t" + paramDetail.getType());
		System.out.println(paramDetail.getDescription());
		WsParameterValue[] valueList = paramDetail.getValues();
		for(int i = 0; i < valueList.length; i++) {
			System.out.print(valueList[i].getValue());
			if(valueList[i].isDefaultValue()) {
				System.out.println("\tdefault");
			}
			else {
				System.out.println();
			}
			System.out.println("\t" + valueList[i].getLabel());
			WsProperty[] valuePropertiesList = valueList[i].getProperties();
			if(valuePropertiesList != null) {
				for(int j = 0; j < valuePropertiesList.length; j++) {
					System.out.println("\t" + valuePropertiesList[j].getKey() + "\t" + valuePropertiesList[j].getValue());
				}
			}
		}
		printDebugMessage("printParamDetail", "End", 1);
	}

	/** Get the status of a submitted job given its job identifier.
	 * 
	 * @param jobid The job identifier.
	 * @return Job status as a string.
	 * @throws IOException
	 * @throws ServiceException
	 */
	public String checkStatus(String jobid) throws IOException, ServiceException {
		printDebugMessage("checkStatus", jobid, 1);
		this.srvProxyConnect(); // Ensure the service proxy exists
		return this.srvProxy.getStatus(jobid);
	}

	/** Get details of the available result types for a job.
	 * 
	 * @param jobId Job identifier to check for results types.
	 * @return Array of objects describing result types.
	 * @throws ServiceException
	 * @throws RemoteException
	 */
	public WsResultType[] getResultTypes(String jobId) throws ServiceException, RemoteException {
		printDebugMessage("getResultTypes", "Begin", 1);
		printDebugMessage("getResultTypes", "jobId: " + jobId , 2);
		WsResultType[] retVal = null;
		this.srvProxyConnect(); // Ensure the service proxy exists
		retVal = this.srvProxy.getResultTypes(jobId);
		printDebugMessage("getResultTypes", retVal.length + " result types", 2);
		printDebugMessage("getResultTypes", "End", 1);
		return retVal;
	}
	
	/** Print details of the available result types for a job.
	 * 
	 * @param jobId Job identifier to check for result types.
	 * @throws ServiceException
	 * @throws RemoteException
	 */
	protected void printResultTypes(String jobId) throws ServiceException, RemoteException {
		printDebugMessage("printResultTypes", "Begin", 1);
		WsResultType[] typeList = getResultTypes(jobId);
		for(int i = 0; i < typeList.length; i++) {
			System.out.print(
					typeList[i].getIdentifier() + "\n\t"
					+ typeList[i].getLabel() + "\n\t"
					+ typeList[i].getDescription() + "\n\t"
					+ typeList[i].getMediaType() + "\n\t"
					+ typeList[i].getFileSuffix() + "\n"
					);
		}
		printDebugMessage("printResultTypes", "End", 1);
	}
	
	/** Get the results for a job and save them to files.
	 * 
	 * @param jobid The job identifier.
	 * @param outfile The base name of the file to save the results to. If
	 * null the jobid will be used.
	 * @param outformat The name of the data format to save, e.g. toolraw 
	 * or toolxml. If null all available data formats will be saved.
	 * @return Array of filenames
	 * @throws IOException
	 * @throws javax.xml.rpc.ServiceException
	 */
	public String[] getResults(String jobid, String outfile, String outformat) throws IOException, javax.xml.rpc.ServiceException {
		printDebugMessage("getResults", "Begin", 1);
		printDebugMessage("getResults", "jobid: " + jobid + " outfile: " + outfile + " outformat: " + outformat, 2);
		String[] retVal = null;
		this.srvProxyConnect(); // Ensure the service proxy exists
		clientPoll(jobid); // Wait for job to finish
		// Set the base name for the output file.
		String basename = (outfile != null) ? outfile : jobid;
		// Get result types
		WsResultType[] resultTypes = getResultTypes(jobid);
		int retValN = 0;
		if(outformat == null) {
			retVal = new String[resultTypes.length];
		} else {
			retVal = new String[1];
		}
		for(int i = 0; i < resultTypes.length; i++) {
			printProgressMessage("File type: " + resultTypes[i].getIdentifier(), 2);
			// Get the results
			if(outformat == null || outformat.equals(resultTypes[i].getIdentifier())) {
				byte[] resultbytes = this.srvProxy.getResult(jobid, resultTypes[i].getIdentifier(), null);
				if(resultbytes == null) {
					System.err.println("Null result for " + resultTypes[i].getIdentifier() + "!");
				}
				else {
					printProgressMessage("Result bytes length: " + resultbytes.length, 2);
					// Write the results to a file
					String result = new String(resultbytes);
					if(basename.equals("-")) { // STDOUT
						if(resultTypes[i].getMediaType().startsWith("text")) { // String
							System.out.print(result);
						}
						else { // Binary
							System.out.print(resultbytes);
						}
					}
					else { // File
						String filename = basename + "." + resultTypes[i].getIdentifier() + "." + resultTypes[i].getFileSuffix();
						if(resultTypes[i].getMediaType().startsWith("text")) { // String
							writeFile(new File(filename), result);
						}
						else { // Binary
							writeFile(new File(filename), resultbytes);
						}
						retVal[retValN] = filename;
						retValN++;
					}
				}
			}
		}
		printDebugMessage("getResults", retVal.length + " file names", 2);
		printDebugMessage("getResults", "End", 1);
		return retVal;
	}

	/** Submit a job to the service.
	 * 
	 * @param params Input parameters for the job.
	 * @param content Data to run the job on.
	 * @return The job identifier.
	 * @throws RemoteException
	 * @throws ServiceException
	 */
	public String runApp(String email, String title, InputParameters params) throws RemoteException, ServiceException {
		printDebugMessage("runApp", "Begin", 1);
		printDebugMessage("runApp", "email: " + email + " title: " + title, 2);
		printDebugMessage("runApp", "params:\n" + objectFieldsToString(params), 2);
		String jobId = null;
		this.srvProxyConnect(); // Ensure the service proxy exists
		jobId = srvProxy.run(email, title, params);
		printDebugMessage("runApp", "jobId: " + jobId, 2);
		printDebugMessage("runApp", "End", 1);
		return jobId;
	}

	/** Populate input parameters structure from command-line options.
	 * 
	 * @param line Command line options
	 * @return input Input parameters structure for use with runApp().
	 * @throws IOException
	 */
	public InputParameters loadParams(CommandLine line) throws IOException {
		printDebugMessage("loadParams", "Begin", 1);
		InputParameters params = new InputParameters();
		// Tool specific options
		if (line.hasOption("stype")) params.setStype(line.getOptionValue("stype"));
		else params.setStype("protein");
		if (line.hasOption("p")) params.setProgram(line.getOptionValue("p"));
		if (line.hasOption("D")) {
			String[] dbList = line.getOptionValue("D").split(" +");
			params.setDatabase(dbList);
		}
		if (line.hasOption("m")) params.setMatrix(line.getOptionValue("m"));
		if (line.hasOption("e")) params.setExp(line.getOptionValue("e"));
		else params.setExp("10");
		if (line.hasOption("u") && line.hasOption("v")) {
			params.setMatch_scores(line.getOptionValue("u") + "," + line.getOptionValue("v"));
		}
		if (line.hasOption("o")) params.setGapopen(new Integer(line.getOptionValue("o"))); 
		if (line.hasOption("x")) params.setGapext(new Integer(line.getOptionValue("x")));
		if (line.hasOption("d")) params.setDropoff(new Integer(line.getOptionValue("d"))); 
		if (line.hasOption("A")) params.setAlign(new Integer(line.getOptionValue("A"))); 
		if (line.hasOption("s")) params.setScores(new Integer(line.getOptionValue("s")));
		else params.setScores(new Integer(50));
		if (line.hasOption("n")) params.setAlignments(new Integer(line.getOptionValue("n")));
		else params.setAlignments(new Integer(50));
		if (line.hasOption("g")) params.setGapalign(new Boolean(true)); 
		if (line.hasOption("f")) params.setFilter(line.getOptionValue("f"));
		//if (line.hasOption("F")) params.setFormat(new Boolean(true));
		if (line.hasOption("S")) params.setSeqrange(line.getOptionValue("S"));
		printDebugMessage("loadParams", "End", 1);
		return params;
	}

	/** Entry point for running as an application.
	 * 
	 * @param args list of command-line options
	 */
	public static void main(String[] args) {
		int exitVal = 0; // Exit value
		int argsLength = args.length; // Number of command-line arguments

		// Configure the command-line options
		Options options = new Options();
		// Common options for EBI clients
		addGenericOptions(options);
		// Application specific options
		options.addOption("ids", "ids", false, "Get list of identifiers from result");
		options.addOption("p", "program", true, "Program to use");
		options.addOption("D", "database", true, "Database to search");
		options.addOption("m", "matrix", true, "Scoring matrix");
		options.addOption("e", "exp", true, "Expectation value threshold");
		options.addOption("f", "filter", true, "Low complexity sequence filter");
		options.addOption("g", "gapalign", true, "Perform gapped alignments");
		options.addOption("A", "align", true, "Alignment format");
		options.addOption("s", "scores", true, "Maximum number of scores to display");
		options.addOption("n", "alignments", true, "Maximum number of alignments to display");
		options.addOption("u", "match", true, "Match score");
		options.addOption("v", "mismatch", true, "Mismatch score");
		options.addOption("o", "gapopen", true, "Gap creation penalty");
		options.addOption("x", "gapext", true, "Gap extension penalty");
		options.addOption("d", "dropoff", true, "Drop off score");
		options.addOption("seqrange", "seqrange", true, "Region in query sequence to use for search");
		options.addOption("stype", "stype", true, "Sequence type");
		options.addOption("sequence", "sequence", true, "Query sequence");

		CommandLineParser cliParser = new GnuParser(); // Create the command line parser    
		// Create an instance of the client
		NCBIBlastClient client = new NCBIBlastClient();
		try {
			// Parse the command-line
			CommandLine cli = cliParser.parse(options, args);
			// User asked for usage info
			if(argsLength == 0 || cli.hasOption("help")) {
				printUsage();
				System.exit(0);
			}
			// Modify output level according to the quiet and verbose options
			if(cli.hasOption("quiet")) {
				client.outputLevel--;
			}
			if(cli.hasOption("verbose")) {
				client.outputLevel++;
			}
			// Set debug level
			if(cli.hasOption("debugLevel")) {
				client.setDebugLevel(Integer.parseInt(cli.getOptionValue("debugLevel")));
			}
			// Alternative service endpoint
			if(cli.hasOption("endpoint")) {
				client.setServiceEndPoint(cli.getOptionValue("endpoint"));
			}
			// Tool meta-data
			// List parameters
			if(cli.hasOption("params")) {
				client.printParams();
			}
			// Details of a parameter
			else if(cli.hasOption("paramDetail")) {
				client.printParamDetail(cli.getOptionValue("paramDetail"));
			}
			// Job related actions
			else if(cli.hasOption("jobid")) {
				String jobid = cli.getOptionValue("jobid");
				// Get results for job
				if(cli.hasOption("polljob")) {                
					String[] resultFilenames = client.getResults(jobid, cli.getOptionValue("outfile"), cli.getOptionValue("outformat"));
					boolean resultContainContent = false;
					for(int i = 0; i < resultFilenames.length; i++) {
						if(resultFilenames[i] != null) {
							System.out.println("Wrote file: " + resultFilenames[i]);
							resultContainContent = true;
						}
					}
					if (resultContainContent == false) {
						System.err.println("Error: requested result type " + cli.getOptionValue("outformat") + " not available!");
					}
				}
				// Get entry Ids from result
				else if(cli.hasOption("ids")) {
					client.getResults(jobid, "-", "ids");
				}
				// Get status of job
				else if(cli.hasOption("status")) {
					System.out.println(client.checkStatus(jobid));
				}
				// Get result types for job
				else if(cli.hasOption("resultTypes")) {
					client.printResultTypes(jobid);
				}
				// Unknown...
				else {
					System.err.println("Error: jobid specified without releated action option");
					printUsage();
					exitVal = 2;
				}
			}
			// Submit a job
			else if(cli.hasOption("email") && (cli.hasOption("sequence") || cli.getArgs().length > 0)) {
				// Create job submission parameters from command-line
				InputParameters params = client.loadParams(cli);
				String dataOption = (cli.hasOption("sequence")) ? cli.getOptionValue("sequence") : cli.getArgs()[0];
				params.setSequence(new String(client.loadData(dataOption)));
				// Submit the job
				String email = null, title = null;
				if (cli.hasOption("email")) email = cli.getOptionValue("email"); 
				if (cli.hasOption("title")) title = cli.getOptionValue("title"); 
				String jobid = client.runApp(email, title, params);
				// For asynchronous mode
				if (cli.hasOption("async")) {
					System.out.println(jobid); // Output the job id.
					System.err.println("To get status: java -jar NCBIBlast_Axis1.jar --status --jobid " + jobid);
				} else {
					// In synchronous mode try to get the results
					client.printProgressMessage(jobid, 1);
					String[] resultFilenames = client.getResults(jobid, cli.getOptionValue("outfile"), cli.getOptionValue("outformat"));
					for(int i = 0; i < resultFilenames.length; i++) {
						if(resultFilenames[i] != null) {
							System.out.println("Wrote file: " + resultFilenames[i]);
						}
					}
				}	
			}
			// Unknown action
			else {
				System.err.println("Error: unknown combination of arguments. See --help.");
				exitVal = 2;
			}
		}
		catch(UnrecognizedOptionException ex) {
			System.err.println("ERROR: " + ex.getMessage());
			printUsage();
			exitVal = 1;
		}
		// Catch all exceptions
		catch(Exception e) {
			System.err.println ("ERROR: " + e.getMessage());
			if(client.getDebugLevel() > 0) {
				e.printStackTrace();
			}
			exitVal = 3;
		}
		System.exit(exitVal);
	}
}
