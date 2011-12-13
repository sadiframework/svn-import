package org.sadiframework.utils.virtuoso;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.sadiframework.utils.ConsoleUtils;
import org.sadiframework.utils.WordUtils;

import virtuoso.jdbc3.VirtuosoException;

public class VirtuosoRDFCopy {

	protected static final String USAGE = "Usage: java -jar virtuoso.rdf.copy.jar <src host:port> <dest host:port>";

	protected static final Logger log = Logger.getLogger(VirtuosoRDFCopy.class);

	protected static final int EXIT_CODE_SUCCESS = 0;
	protected static final int EXIT_CODE_FAILURE = 1;

	protected static class CommandLineOptions {

		@Option(name = "-h", aliases = { "--help" }, usage = "display this help message")
		boolean help;

		@Option(name = "-b", aliases = { "--backup-dir" }, metaVar = "DIRECTORY", usage = "directory for backing up all overwritten graphs in dest Virtuoso instance")
		String backupDir;

		@Option(name = "-p", aliases = { "--precopy-cmd" }, metaVar = "SHELL_COMMAND", usage = "run SHELL_COMMAND after locking src and/or dest, but prior to rdf copy")
		List<String> precopyCmds = new ArrayList<String>();

		@Option(name = "-P", aliases = { "--postcopy-cmd" }, metaVar = "SHELL_COMMAND", usage = "run SHELL_COMMAND after rdf copy, but prior to unlocking src and/or dest")
		List<String> postcopyCmds = new ArrayList<String>();

		@Option(name = "-i", aliases = { "--include-graph" }, metaVar = "GRAPH_URI", usage = "include named graph in RDF copy (by default all graphs are copied)")
		List<String> includedGraphs = new ArrayList<String>();

		@Option(name = "-e", aliases = { "--exclude-graph" }, metaVar = "GRAPH_URI", usage = "exclude named graph in RDF copy (by default all graphs are copied)")
		List<String> excludedGraphs = new ArrayList<String>();

		@Option(name = "-o", aliases = { "--overwrite-graphs" }, usage = "clear named graphs that already exist in the dest Virtuoso instance")
		boolean overwriteGraphs = false;

		@Option(name = "-l", aliases = { "--lock-src" }, usage = "lock the src Virtuoso instance prior to copying data")
		boolean lockSrc = false;

		@Option(name = "-L", aliases = { "--lock-dest" }, usage = "lock the dest Virtuoso instance prior to copying data")
		boolean lockDest = false;

		@Option(name = "-d", aliases = { "--data-only" }, usage = "copy only the triples in the named graphs, not the associated users and permissions" )
		boolean dataOnly = false;

		@Argument(required = true, index = 0, metaVar = "<src host:port>", usage = "isql host:port for source Virtuoso triple store")
		String src;

		@Argument(required = true, index = 1,  metaVar = "<dest host:port>", usage = "isql host:port for destination Virtuoso triple store")
		String dest;

	}

	public static void main(String[] args) throws SQLException, IOException {

		/* parse command-line */

		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser cmdLineParser = new CmdLineParser(options);

		try {
			cmdLineParser.parseArgument(args);
		} catch(CmdLineException e) {
			printUsage(cmdLineParser);
			System.exit(EXIT_CODE_FAILURE);
		}

		/* show help message if requested */

		if (options.help) {
			printUsage(cmdLineParser);
			System.exit(EXIT_CODE_SUCCESS);
		}

		String srcHost = getHost(options.src);
		int srcPort = getPort(options.src);
		String destHost = getHost(options.dest);
		int destPort = getPort(options.dest);

		/* make sure src and dest aren't the same! */

		if (srcHost.equals(destHost) && srcPort == destPort) {
			log.error("Source and destination Virtuoso instances must be different!");
			log.error("");
			printUsage(cmdLineParser);
			System.exit(EXIT_CODE_FAILURE);
		}

		/* connect to src and dest */

		String srcPassword = getVerifiedPassword(srcHost, srcPort);
		String destPassword = getVerifiedPassword(destHost, destPort);

		Connection srcConn = VirtuosoUtils.getPersistentJDBCConnection(srcHost, srcPort, "dba", srcPassword);
		Connection destConn = VirtuosoUtils.getPersistentJDBCConnection(destHost, destPort, "dba", destPassword);

		/* determine the backup directory for graphs in dest Virtuoso */

		File backupDir = null;

		if (options.backupDir != null) {
			backupDir = new File(options.backupDir);
			if (!backupDir.exists()) {
				try {
					backupDir.mkdirs();
				} catch(SecurityException e) {
					log.error(String.format("Unable to create backup dir %s", backupDir.toString()));
					printUsage(cmdLineParser);
					System.exit(EXIT_CODE_FAILURE);
				}
			}
			if (!backupDir.canWrite()) {
				log.error(String.format("Backup dir %s is not writable", backupDir.toString()));
				printUsage(cmdLineParser);
				System.exit(EXIT_CODE_FAILURE);
			}
		} else {
			try {
				backupDir = createBackupDir(destHost, destPort);
			} catch (SecurityException e) {
				log.error("Unable to create a backup directory in the current working directory.");
				log.error("Please specify a backup directory explicitly with the --backup-dir switch.");
				printUsage(cmdLineParser);
				System.exit(EXIT_CODE_SUCCESS);
			}
		}

		log.info(String.format("using backup dir %s for existing graphs in dest Virtuoso (%s:%d)", backupDir.toString(), destHost, destPort));

		/* determine the list of graphs that will be copied */

		Set<String> graphsToCopy;

		if (options.includedGraphs.size() > 0)
			graphsToCopy = new HashSet<String>(options.includedGraphs);
		else
			graphsToCopy = new HashSet<String>(VirtuosoUtils.getGraphURIs(srcConn));

		for (String excludedGraph : options.excludedGraphs)
			graphsToCopy.remove(excludedGraph);

		/* do the copy */

		VirtuosoRDFLock srcLock = options.lockSrc ? VirtuosoRDFLock.getLock(srcConn) : null;
		VirtuosoRDFLock destLock = options.lockDest ? VirtuosoRDFLock.getLock(destConn) : null;

		try {

			/*
			 * locking changes all graph perms to read only, so copy graph
			 * perms from src to dest before locking src.
			 */

			if (!options.dataOnly) {
				for (String graphURI : graphsToCopy) {
					Map<String,Map<String,Integer>> srcGraphPerms = VirtuosoUtils.getGraphPerms(srcConn, graphURI, true);
					VirtuosoUtils.mergeGraphPerms(srcGraphPerms, destConn);
				}
			}

			if (srcLock != null)
				srcLock.lock();

			if (destLock != null)
				destLock.lock();

			log.info("running pre-copy shell commands");
			runShellCommands(options.precopyCmds);

			for (String graphURI : graphsToCopy) {

				VirtuosoUtils.copyGraph(
						graphURI,
						srcHost,
						srcPort,
						"dba",
						srcPassword,
						destHost,
						destPort,
						"dba",
						destPassword,
						backupDir,
						options.lockSrc);

			}

			log.info("running post-copy shell commands");
			runShellCommands(options.postcopyCmds);

		} finally {

			// in case we crash inside a password prompt (unlikely)
			ConsoleUtils.echoOn();

			if (srcLock != null)
				srcLock.unlock();

			if (destLock != null)
				destLock.unlock();

		}

	}

	protected static void printUsage(CmdLineParser cmdLineParser) throws IOException {
		String usageText = IOUtils.toString(VirtuosoRDFCopy.class.getResourceAsStream("/usage.txt"));
		usageText = WordUtils.wrap(usageText, 80);
		log.error(usageText);
		log.error("");
		log.error("OPTIONS:");
		log.error("");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		cmdLineParser.printUsage(baos);
		log.error(baos.toString());
	}

	protected static String normalizeHostString(String hostStr) {
		if (hostStr.contains(":"))
			return hostStr;
		return String.format("%s:%s", "localhost", hostStr);
	}

	protected static String getHost(String hostStr) {
		hostStr = normalizeHostString(hostStr);
		return hostStr.split(":")[0];
	}

	protected static int getPort(String hostStr) {
		hostStr = normalizeHostString(hostStr);
		return Integer.valueOf(hostStr.split(":")[1]);
	}

	protected static String getVerifiedPassword(String host, int port) throws SQLException {

		Connection conn = null;
		try {
			while(true) {
				try {
					String password = ConsoleUtils.getPassword(String.format("enter password for 'dba' at %s:%d: ", host, port), false);
				    Class.forName("virtuoso.jdbc3.Driver");
					conn = DriverManager.getConnection(VirtuosoUtils.getJDBCURL(host, port), "dba", password);
					return password;
				} catch(VirtuosoException e) {
					if (!e.getMessage().contains("Bad login"))
						throw e;
				} catch(ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		} finally {
			try { if (conn != null) conn.close(); } catch(Exception e) { }
		}

	}

	protected static void runShellCommands(List<String> cmds) {
		for (String cmd : cmds) {
			int exitCode = runShellCommand(cmd);
			if (exitCode > 0)
				throw new RuntimeException(String.format("command '%s' failed with exit code %d", cmd, exitCode));
		}
	}

	protected static int runShellCommand(String cmd) {
		try {
			ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
			return pb.start().waitFor();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected static File createBackupDir(String host, int port) {
		File backupDir = new File(String.format("backup.%s.%d.%d", host, port, System.currentTimeMillis()));
		backupDir.mkdir();
		backupDir.setWritable(true);
		return backupDir;
	}



}
