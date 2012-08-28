package org.sadiframework.service.blast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
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
import org.sadiframework.utils.SPARQLStringUtils;


/**
 * @author Luke McCarthy
 */
public class MasterServlet extends HttpServlet
{
	private static final Logger log = Logger.getLogger(MasterServlet.class); 
	private static final long serialVersionUID = 1L;
	
	private Map<Taxon, NCBIBLASTServiceServlet> genomeServletMap;
	private Map<Taxon, NCBIBLASTServiceServlet> transcriptomeServletMap;
	
	@Override
	public void init() throws ServletException
	{
		super.init();
		
		genomeServletMap = new HashMap<Taxon, NCBIBLASTServiceServlet>();
		transcriptomeServletMap = new HashMap<Taxon, NCBIBLASTServiceServlet>();
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		NCBIBLASTServiceServlet servlet = getServlet(request);
		if (servlet == null) {
			outputIndex(response);
		} else {
			servlet.doGet(request, response);
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		NCBIBLASTServiceServlet servlet = getServlet(request);
		if (servlet == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} else {
			servlet.doPost(request, response);
		}
	}
	
	private void outputIndex(HttpServletResponse response)
	{
		StringBuilder buf = new StringBuilder();
		for (Iterator<?> i = getConfig().getKeys("blast.taxon"); i.hasNext(); ) {
			String escapedTaxon = ((String)i.next()).substring(12);
			String taxon = escapedTaxon.replace("+", " ");
			buf.append("<li><a href='./");
			buf.append(escapedTaxon);
			buf.append("'>NCBI ");
			buf.append(taxon);
			buf.append(" BLAST</a></li>\n");
		}
		try {
			String index = SPARQLStringUtils.strFromTemplate(MasterServlet.class.getResource("/index.template"), buf.toString());
			response.setContentType("text/html");
			response.getWriter().print(index);
		} catch (Exception e) {
			log.error("error writing index", e);
		}
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
	
	private Taxon getTaxon(String name) throws ServletException
	{
		String id = getConfig().subset("blast.taxon").getString(name);
		if (id != null) {
			Taxon taxon = new Taxon();
			taxon.name = name;
			taxon.id = id;
			return taxon;
		} else {
			log.debug(String.format("unable to determine taxon id from %s", name));
			return null;
		}
	}

	private static final Pattern pathPattern = Pattern.compile("/([^./]+)((?=/)[^./]+)?");
	private NCBIBLASTServiceServlet getServlet(HttpServletRequest request) throws ServletException
	{
		/* url-pattern for MasterServlet has to be set to "/" (not "/*")
		 * or getServletPath() won't work...
		 */
		Matcher matcher = pathPattern.matcher(request.getServletPath());
		if (matcher.find()) {
			String taxonName = matcher.group(1);
			Taxon taxon = getTaxon(taxonName);
			if (taxon != null) {
				String dbName = matcher.group(2);
				if (dbName == null) {
					return getGenomeServlet(taxon);
				} else if (dbName.equals("transcriptome")) {
					return getTranscriptomeServlet(taxon);
				}
			}
		}
		return null;
	}
	
	private NCBIBLASTServiceServlet getGenomeServlet(Taxon taxon) throws ServletException
	{
		if (!genomeServletMap.containsKey(taxon)) {
			NCBIBLASTServiceServlet servlet = new NCBIBLASTServiceServlet(taxon);
			servlet.init();
			genomeServletMap.put(taxon, servlet);
		}
		return genomeServletMap.get(taxon);
	}
	
	private NCBIBLASTServiceServlet getTranscriptomeServlet(Taxon taxon) throws ServletException
	{
		if (!transcriptomeServletMap.containsKey(taxon)) {
			NCBIBLASTServiceServlet servlet = new NCBIBLASTServiceServlet(taxon);
			servlet.init();
			transcriptomeServletMap.put(taxon, servlet);
		}
		return transcriptomeServletMap.get(taxon);
	}
	
	public static class Taxon
	{
		public String id;
		public String name;
		
		@Override
		public int hashCode() 
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Taxon other = (Taxon) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
}
