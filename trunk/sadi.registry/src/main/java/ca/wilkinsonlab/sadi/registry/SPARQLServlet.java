package ca.wilkinsonlab.sadi.registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stringtree.json.JSONWriter;

import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.utils.QueryUtils;

import com.hp.hpl.jena.query.ResultSet;

@SuppressWarnings("serial")
public class SPARQLServlet extends HttpServlet
{
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String query = request.getParameter("query");
		String callback = request.getParameter("callback");
		String format = request.getParameter("format");
		if (query != null) {
			Registry registry = null;
			try {
				registry = Registry.getRegistry();
				ResultSet resultSet = registry.doSPARQL(query); 
				List<Map<String, String>> bindings = QueryUtils.convertResultSet(resultSet);
				if (callback != null) { // they're expecting JSONP...
					JSONWriter jsonWriter = new JSONWriter();
					response.setContentType("text/javascript");
					response.getWriter().format("%s(%s)", callback, jsonWriter.write(bindings));
					return;
				} else if (format.equals("JSON")) { // compatibility with Virtuoso SPARQL
					JSONWriter jsonWriter = new JSONWriter();
					response.setContentType("text/javascript");
					List<Map<String, Map<String, String>>> newBindings = new ArrayList<Map<String, Map<String, String>>>();
					for (Map<String, String> binding: bindings) {
						Map<String, Map<String, String>> newBinding = new HashMap<String, Map<String, String>>();
						for (String variable: binding.keySet()) {
							Map<String, String> valueMap = new HashMap<String, String>();
							valueMap.put("value", binding.get(variable));
							newBinding.put(variable, valueMap);
						}
						newBindings.add(newBinding);
					}
					Map<Object, Object> top = new HashMap<Object, Object>();
					Map<Object, Object> results = new HashMap<Object, Object>();
					top.put("results", results);
					results.put("bindings", newBindings);
					response.getWriter().print(jsonWriter.write(top));
					return;
				} else {
					request.setAttribute("variables", resultSet.getResultVars());
					request.setAttribute("bindings", bindings);
				}
			} catch (SADIException e) {
				if (callback != null) { // they're expecting JSON...
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("error executing SPARQL query: %s", e.getMessage()));
					return;
				} else {
					request.setAttribute("error", e.getMessage());
				}
			} finally {
				if (registry != null)
					registry.getModel().close();
			}
		}
		getServletConfig().getServletContext().getRequestDispatcher("/sparql/index.jsp").forward(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}
}
