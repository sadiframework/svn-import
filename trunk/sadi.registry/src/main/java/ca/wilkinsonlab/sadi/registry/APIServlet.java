package ca.wilkinsonlab.sadi.registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.stringtree.json.JSONWriter;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.RegistryImpl;
import ca.wilkinsonlab.sadi.utils.QueryExecutorFactory;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class APIServlet extends HttpServlet
{
	private static final Logger log = Logger.getLogger(APIServlet.class);
	private static final long serialVersionUID = 1L;

	private Collection<APILocation> apiLocations;
	
	@Override
	public void init() throws ServletException
	{
		apiLocations = new ArrayList<APILocation>();
		apiLocations.add(new Services());
		apiLocations.add(new Register());
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		@SuppressWarnings("unchecked")
		Map<String, String> params = request.getParameterMap();
		String path = request.getPathInfo();
		if (StringUtils.isEmpty(path)) {
			request.setAttribute("apiLocations", apiLocations);
			getServletConfig().getServletContext().getRequestDispatcher("/api/index.jsp").forward(request, response);
		} else {
			for (APILocation apiLocation: apiLocations) {
				if (path.startsWith(apiLocation.getPath())) {
					Object result = null;
					if (apiLocation instanceof GettableLocation) {
						Registry registry = null;
						try {
							registry = Registry.getRegistry();
							result = ((GettableLocation)apiLocation).get(registry, path.substring(apiLocation.getPath().length()), params);
						} catch (SADIException e) {
							log.error(String.format("error executing %s %s", path, params), e);
						} finally {
							if (registry != null)
								registry.getModel().close();
						}
					}
					output(response, result);
					break;
				}
			}
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		@SuppressWarnings("unchecked")
		Map<String, String> params = request.getParameterMap();
		String path = request.getPathInfo();
		for (APILocation apiLocation: apiLocations) {
			if (path.startsWith(apiLocation.getPath())) {
				Object result = null;
				if (apiLocation instanceof PostableLocation) {
					Registry registry = null;
					try {
						registry = Registry.getRegistry();
						result = ((PostableLocation)apiLocation).post(registry, path.substring(apiLocation.getPath().length()), params);
					} catch (SADIException e) {
						log.error(String.format("error executing %s %s", path, params), e);
					} finally {
						if (registry != null)
							registry.getModel().close();
					}
				}
				output(response, result);
				return;
			}
		}
		throw new ServletException(String.format("invalid path %s", path));
	}
	
	private void output(HttpServletResponse response, Object result) throws IOException
	{
		new JSONWriter().write(result);
		JSONWriter jsonWriter = new JSONWriter();
		response.setContentType("text/javascript");
		response.getWriter().format("%s", jsonWriter.write(ObjectUtils.defaultIfNull(result, Collections.emptyList())));
	}
	
	private static class Services implements GettableLocation
	{
		@Override
		public String getPath()
		{
			return "/services";
		}

		@Override
		public String getDescription()
		{
			StringBuilder buf = new StringBuilder();
			buf.append("GET /services\treturns all registered services\n");
			buf.append("GET /services/attachedProperty/${propertyURI}\treturns services that attach ${propertyURI}\n");
			buf.append("GET /services/attachedPropertyLabel/${propertyLabel\treturns services that attach property with label ${propertyLabel}\n");
			buf.append("GET /services/connectedClass/${classURI}\treturns services that attach values from ${classURI}\n");
			buf.append("GET /services/connectedClassLabel/${classLabel}\treturns services that attach values from class with label ${classLabel}\n");
			buf.append("GET /services/inputClass/${classURI}\treturns services that consume instances of ${classURI}\n");
			return buf.toString();
		}
		
		@Override
		public Object get(Registry registry, String path, Map<String, String> params) throws SADIException
		{
			String[] pathElements = StringUtils.split(path, '/');
			if (pathElements.length == 0) {
				return registry.getRegisteredServices();
			} else if (pathElements.length == 1) {
				return registry.getServiceBean(pathElements[0]);
			} else if (pathElements[0].equals("attachedProperty")) {
				if (pathElements.length < 2)
					throw new IllegalArgumentException("expected /services/attachedProperty/${propertyURI}");
				else
					return new RegistryImpl(QueryExecutorFactory.createJenaModelQueryExecutor(registry.getModel()))
							.findServicesByAttachedProperty(ResourceFactory.createProperty(pathElements[1]));
			} else if (pathElements[0].equals("attachedPropertyLabel")) {
				if (pathElements.length < 2)
					throw new IllegalArgumentException("expected /services/attachedPropertyLabel/${propertyLabel}");
				else
					return new RegistryImpl(QueryExecutorFactory.createJenaModelQueryExecutor(registry.getModel()))
							.findServicesByAttachedPropertyLabel(pathElements[1]);
			} else if (pathElements[0].equals("connectedClass")) {
				if (pathElements.length < 2)
					throw new IllegalArgumentException("expected /services/connectedClass/${classURI}");
				else
					return new RegistryImpl(QueryExecutorFactory.createJenaModelQueryExecutor(registry.getModel()))
							.findServicesByConnectedClass(ResourceFactory.createResource(pathElements[1]));
			} else if (pathElements[0].equals("connectedClassLabel")) {
				if (pathElements.length < 2)
					throw new IllegalArgumentException("expected /services/connectedClassLabel/${classLabel}");
				else
					return new RegistryImpl(QueryExecutorFactory.createJenaModelQueryExecutor(registry.getModel()))
							.findServicesByConnectedClassLabel(pathElements[1]);
			} else if (pathElements[0].equals("inputClass")) {
				if (pathElements.length < 2)
					throw new IllegalArgumentException("expected /services/inputClass/${classURI}");
				else
					return new RegistryImpl(QueryExecutorFactory.createJenaModelQueryExecutor(registry.getModel()))
							.findServicesByInputClass(ResourceFactory.createResource(pathElements[1]));
			} else {
				throw new IllegalArgumentException(String.format("unknown method %s", path));
			}
		}
	}
	
	private static class Register implements PostableLocation
	{
		@Override
		public String getPath()
		{
			return "/register";
		}

		@Override
		public String getDescription()
		{
			StringBuilder buf = new StringBuilder();
			buf.append("POST /register/${serviceURL}\tregister service at ${serviceURL}\n");
			return buf.toString();
		}

		@Override
		public Object post(Registry registry, String path, Map<String, String> params) throws SADIException 
		{
			return registry.registerService(path);
		}
	}

	public static interface APILocation
	{
		String getPath();
		String getDescription();
	}
	public static interface GettableLocation extends APILocation
	{
		Object get(Registry registry, String path, Map<String, String> params) throws SADIException;
	}
	public static interface PostableLocation extends APILocation
	{
		Object post(Registry registry, String path, Map<String, String> params) throws SADIException;
	}
}
