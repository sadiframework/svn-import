package ca.wilkinsonlab.sadi.service.blast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.stringtree.util.StreamUtils;

import ca.wilkinsonlab.sadi.service.blast.NCBIBLASTServiceServlet.Taxon;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.blast.NCBIBLASTClient;

/**
 * @author Luke McCarthy
 */
public class MasterServlet extends HttpServlet
{
	private static final Logger log = Logger.getLogger(MasterServlet.class);
	private static final Pattern taxonPattern = Pattern.compile("/([^.]+)"); 
	private static final long serialVersionUID = 1L;
	
	private Map<Taxon, NCBIBLASTServiceServlet> servletMap;
	private NCBIBLASTClient client;
	
	@Override
	public void init() throws ServletException
	{
		super.init();
		
		servletMap = new HashMap<Taxon, NCBIBLASTServiceServlet>();
		client = new NCBIBLASTClient();
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String path = request.getPathInfo();
		Taxon taxon = getTaxon(path);
		if (path.endsWith(".owl")) {
			outputOWL(response, taxon);
		} else if (taxon == null || taxon.id == null) {
			outputIndex(response);
		} else {
			getServlet(taxon).doGet(request, response);
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String path = request.getPathInfo();
		Taxon taxon = getTaxon(path);
		getServlet(taxon).doPost(request, response);
	}
	
	private Configuration getConfig()
	{
		try {
			return new PropertiesConfiguration("blast.properties");
		} catch (ConfigurationException e) {
			log.warn(String.format("error reading BLAST configuration: %s", e));
			return new PropertiesConfiguration();
		}
	}
	
	private Taxon getTaxon(String path) throws ServletException
	{
		Matcher matcher = taxonPattern.matcher(path);
		if (matcher.find()) {
			Taxon taxon = new Taxon();
			taxon.name = matcher.group(1);
			taxon.id = getConfig().getString(String.format("blast.taxon.%s", taxon.name));
			return taxon;
		} else {
			log.debug(String.format("unable to determine taxon id from %s", path));
			return null;
		}
	}
	
	private void outputIndex(HttpServletResponse response)
	{
		try {
			String index = SPARQLStringUtils.strFromTemplate(MasterServlet.class.getResource("/index.html"));
			response.setContentType("text/html");
			response.getWriter().print(index);
		} catch (Exception e) {
			log.error("error writing index", e);
		}
	}
	
	private void outputOWL(HttpServletResponse response, Taxon taxon)
	{
		if (taxon == null || taxon.id == null) {
			try {
				response.setContentType("application/rdf+xml");
				String owl = StreamUtils.readStream(MasterServlet.class.getResourceAsStream("/ncbi-blast.owl"));
				response.setContentType("application/rdf+xml");
				response.getWriter().print(owl);
			} catch (IOException e) {
				log.error("error writing ncbi-blast.owl", e);
			}
		} else {
			try {
				String owl = SPARQLStringUtils.strFromTemplate(MasterServlet.class.getResource("/template.owl"), taxon.name, taxon.id);
				response.setContentType("application/rdf+xml");
				response.getWriter().print(owl);
			} catch (Exception e) {
				log.error(String.format("error writing owl for %s", taxon.name), e);
			}
		}
	}
	
	private NCBIBLASTServiceServlet getServlet(Taxon taxon) throws ServletException
	{
		if (!servletMap.containsKey(taxon)) {
			NCBIBLASTServiceServlet servlet = new NCBIBLASTServiceServlet(taxon, client);
			servlet.init();
			servletMap.put(taxon, servlet);
		}
		return servletMap.get(taxon);
	}
}
