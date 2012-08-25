package org.sadiframework.service.idmapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.sadiframework.service.idmapper.Config.LSRNNamespacePair;
import org.stringtemplate.v4.ST;

public class MasterServlet extends HttpServlet
{
	private static final Logger log = Logger.getLogger(MasterServlet.class);
	private static final long serialVersionUID = 1L;

	private Map<LSRNNamespacePair,IdMapperServiceServlet> servletMap;

	public MasterServlet()
	{
		servletMap = new HashMap<LSRNNamespacePair,IdMapperServiceServlet>();
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		IdMapperServiceServlet servlet = getServlet(request);
		if (servlet == null) {
			outputIndex(response);
		} else {
			servlet.doGet(request, response);
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		IdMapperServiceServlet servlet = getServlet(request);
		if (servlet == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} else {
			servlet.doPost(request, response);
		}
	}

	private void outputIndex(HttpServletResponse response)
	{
		StringBuilder buf = new StringBuilder();
		for (LSRNNamespacePair namespacePair : Config.getInstance().getServiceNamespacePairs()) {
			String serviceName = String.format("%s-to-%s", namespacePair.getSourceNamespace(), namespacePair.getTargetNamespace());
			buf.append(String.format("<li><a href='./%s'>%s</a></li>\n", serviceName, serviceName));
		}
		try {
			String template = IOUtils.toString(MasterServlet.class.getResourceAsStream("/index.html.template"));
			ST templater = new ST(template, '$', '$');
			templater.add("service_list", buf.toString());
			response.setContentType("text/html");
			response.getWriter().print(templater.render());
		} catch (Exception e) {
			log.error("error writing index", e);
		}
	}

	private static final Pattern pathPattern = Pattern.compile("/(\\S+?)-to-(\\S+?)(\\.owl)?$");
	private IdMapperServiceServlet getServlet(HttpServletRequest request) throws ServletException, IOException
	{
		Matcher matcher = pathPattern.matcher(request.getServletPath());
		if (matcher.find()) {
			Config config = Config.getInstance();
			String inputNamespace = matcher.group(1);
			String outputNamespace = matcher.group(2);
			LSRNNamespacePair namespacePair = new LSRNNamespacePair(inputNamespace, outputNamespace);
			if (!config.getServiceNamespacePairs().contains(namespacePair)) {
				return null;
			}
			if (!servletMap.containsKey(namespacePair)) {
				IdMapperServiceServlet servlet = new IdMapperServiceServlet(inputNamespace, outputNamespace);
				servlet.init();
				servletMap.put(namespacePair, servlet);
			}
			return servletMap.get(namespacePair);
		}
		return null;
	}

}
