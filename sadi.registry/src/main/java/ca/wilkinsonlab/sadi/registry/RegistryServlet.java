package ca.wilkinsonlab.sadi.registry;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.JsonUtils;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

@SuppressWarnings("serial")
public class RegistryServlet extends HttpServlet
{
	private static final Log log = LogFactory.getLog(RegistryServlet.class);
	
	/* application/json is more correct, but this isn't incorrect and it will
	 * display in browsers instead of forcing a download...
	 */
	private static final String jsonContentType = "application/javascript";
	
	private Registry registry;
	
	public void init() throws ServletException
	{
		registry = Registry.getRegistry();
		
//		String uriPrefix = "http://sadiframework.org/examples/";
//		String altPrefix = "http://localhost:8080/sadi-examples/";
//		log.info( String.format("mapping URI prefix %s to %s", uriPrefix, altPrefix) );
//		LocationMapper.get().addAltPrefix(uriPrefix, altPrefix);
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serviceURI = request.getParameter("service");
		if (serviceURI != null) {
			ServiceBean service = getRegistry().getServiceBean(serviceURI);
			if (service != null)
				outputServiceAsJSON(response, service);
			else
				outputErrorAsJSON(response, new Exception(String.format("service %s is not registered", serviceURI)));
		} else {
			request.setAttribute("services", getRegistry().getRegisteredServices());
			getServletConfig().getServletContext().getRequestDispatcher("/services.jsp").forward(request, response);
		}
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serviceURI = request.getParameter("url");
		String query = request.getParameter("query");
		if (serviceURI != null) {
			log.info( String.format("attempting to register service %s", serviceURI) );
			try {
				ServiceBean service = getRegistry().registerService(serviceURI);
				request.setAttribute("registered", service);
				doGet(request, response);
//				outputServiceAsJSON(response, service);
			} catch (final Exception e) {
				log.error( String.format("registration failed for %s: %s", serviceURI, e) );
				ServiceBean service = new ServiceBean();
				service.setServiceURI(serviceURI);
				request.setAttribute("registered", service);
				request.setAttribute("error", e.getMessage());
				doGet(request, response);
//				outputErrorAsJSON(response, e);
			}
		} else if (query != null) {
			ResultSet results = getRegistry().doSPARQL(query);
			response.setContentType("application/sparql-results+xml");
			ResultSetFormatter.outputAsXML(response.getOutputStream(), results);
		} else {
			doGet(request, response);
		}
	}
	
	void outputServiceAsJSON(HttpServletResponse response, ServiceBean service) throws IOException
	{
		String json = JsonUtils.write(service);
		response.setContentType(jsonContentType);
		response.getWriter().print(json);
	}
	
	void outputErrorAsJSON(HttpServletResponse response, final Throwable error) throws IOException
	{
		String json = JsonUtils.write(new HashMap<String, String>() {{
			put("error", error.getMessage());
		}});
		response.setContentType(jsonContentType);
		response.getWriter().print(json);
	}
	
	Registry getRegistry()
	{
		return registry;
	}
}
