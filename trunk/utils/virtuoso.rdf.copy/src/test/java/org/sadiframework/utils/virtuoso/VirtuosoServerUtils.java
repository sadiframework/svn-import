package org.sadiframework.utils.virtuoso;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.sadiframework.utils.NetworkUtils;
import org.sadiframework.utils.ProcessUtils;

import com.google.common.io.Files;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class VirtuosoServerUtils {

	protected static final Logger log = Logger.getLogger(VirtuosoServerUtils.class);
	protected static final String VIRTUOSO_CONFIG_FILE = "virtuoso.properties";
	protected static final String VIRTUOSO_BINARY_CONFIG_KEY = "virtuoso.bin";
	protected static final String VIRTUOSO_HTTP_ROOT_CONFIG_KEY = "virtuoso.httproot";
	protected static final String VIRTUOSO_INI_TEMPLATE = "virtuoso.ini.ftl";
	protected static final String VIRTUOSO_INI_FILE = "virtuoso.ini";

	protected static VirtuosoProcess startVirtuosoInstance() throws IOException {

		/* allocate ports */

		List<Integer> freePorts = NetworkUtils.getUnusedLocalPorts(2);
		int isqlPort = freePorts.get(0);
		int httpPort = freePorts.get(1);

		/* create a temp directory for virtuoso db */

		File tempDir = Files.createTempDir();

		// get paths to the virtuoso binary and the virtuoso http root dir

		Properties properties = new Properties();
		properties.load(VirtuosoServerUtils.class.getResourceAsStream("/" + VIRTUOSO_CONFIG_FILE));

		String virtuosoBinary = properties.getProperty(VIRTUOSO_BINARY_CONFIG_KEY);
		String virtuosoHttpRoot = properties.getProperty(VIRTUOSO_HTTP_ROOT_CONFIG_KEY);

		/* make a virtuoso.ini */

		File iniFile = new File(tempDir, VIRTUOSO_INI_FILE);
		createIniFile(iniFile, isqlPort, httpPort, virtuosoHttpRoot);

		/* startup the server */

		log.info(String.format("starting up virtuoso instance (isql port = %d, http port = %d, working dir = %s)", isqlPort, httpPort, tempDir.toString()));

		ProcessBuilder pb = new ProcessBuilder(virtuosoBinary, "+foreground");
		pb.directory(tempDir);

		Process process = pb.start();

		log.info("waiting for server startup (10 seconds)");
		try { Thread.sleep(10000); } catch(InterruptedException e) {}

		for(File file : tempDir.listFiles())
			file.deleteOnExit();
		tempDir.deleteOnExit();

		return new VirtuosoProcess(process, "localhost", isqlPort, httpPort);

	}

	protected static void createIniFile(File outputFile, int isqlPort, int httpPort, String virtuosoHttpRoot) throws IOException {


		// build virtuoso.ini from the template

        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading(VirtuosoServerUtils.class, "/");
        cfg.setObjectWrapper(new DefaultObjectWrapper());

        Template template = cfg.getTemplate(VIRTUOSO_INI_TEMPLATE);

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("isql_port", String.valueOf(isqlPort));
        vars.put("http_port", String.valueOf(httpPort));
        vars.put("http_root_dir", virtuosoHttpRoot);

        Writer out = new BufferedWriter(new FileWriter(outputFile));

        try {
        	template.process(vars, out);
        } catch(TemplateException e) {
        	throw new RuntimeException(e);
        }

        out.close();

	}

	public static class VirtuosoProcess {

		protected Process process;
		protected String host;
		protected int isqlPort;
		protected int httpPort;

		public VirtuosoProcess(Process process, String host, int isqlPort, int httpPort) {
			this.process = process;
			this.host = host;
			this.isqlPort = isqlPort;
			this.httpPort = httpPort;
		}

		public void shutdown() {
			if (process != null)
				ProcessUtils.killAndWait(process);
		}

		public String getHost() {
			return host;
		}

		public int getISQLPort() {
			return isqlPort;
		}

		public int getHTTPPort() {
			return httpPort;
		}

	}
}
