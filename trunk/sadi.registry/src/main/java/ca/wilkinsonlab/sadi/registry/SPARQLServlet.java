package ca.wilkinsonlab.sadi.registry;

import java.io.IOException;
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
		if (query != null) {
			try {
				ResultSet resultSet = Registry.getRegistry().doSPARQL(query); 
				List<Map<String, String>> bindings = QueryUtils.convertResultSet(resultSet);
				if (callback != null) { // they're expecting JSONP...
					JSONWriter jsonWriter = new JSONWriter();
					response.setContentType("text/javascript");
					response.getWriter().format("%s(%s)", callback, jsonWriter.write(bindings));
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
