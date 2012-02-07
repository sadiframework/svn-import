package ca.wilkinsonlab.sadi.client.virtual.sparql;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils.HttpStatusException;

/**
 * @author Ben Vandervalk
 */
public class VirtuosoSPARQLEndpoint extends SPARQLEndpoint
{
	public final static Logger log = Logger.getLogger(VirtuosoSPARQLEndpoint.class);
	public final static String VIRTUOSO_SPARQL_AUTH_REALM = "SPARQL";

	public VirtuosoSPARQLEndpoint(String endpointURI) throws MalformedURLException
	{
		this(endpointURI, null, null);
	}

	public VirtuosoSPARQLEndpoint(String endpointURI, String username, String password) throws MalformedURLException
	{
		super(endpointURI, EndpointType.VIRTUOSO);
		if(username != null && password != null) {
			String hostname = new URL(getURI()).getHost();
			httpClient.setAuthCredentials(hostname, AuthScope.ANY_PORT, VIRTUOSO_SPARQL_AUTH_REALM, username, password);
		}
	}

	@Override
	public void updateQuery(String query) throws IOException
	{
		HttpResponse response = httpClient.POST(new URL(getURI()), getParamsForUpdateQuery(query));
		response.getEntity().getContent().close();

		int statusCode = response.getStatusLine().getStatusCode();
		if (HttpUtils.isHttpError(statusCode))
			throw new HttpStatusException(statusCode);
	}

	protected Map<String,String> getParamsForUpdateQuery(String query)
	{
		Map<String,String> params = new HashMap<String,String>();
		params.put("query",query);
		return params;
	}

	public void deleteDirectedClosure(String URI, String graphURI) throws IOException
	{
		int closureDepth = getClosureDepth(URI, graphURI);
		// Delete the closure, starting with the deepest paths.
		StringBuilder depthClause = new StringBuilder(" %u% ?p0 ?o0 .");
		for(int i = 2; i <= closureDepth; i++) {
			depthClause.append(" ?o");
			depthClause.append(i - 2);
			depthClause.append(" ?p");
			depthClause.append(i - 1);
			depthClause.append(" ?o");
			depthClause.append(i - 1);
			depthClause.append(" .");
		}

		for(int i = closureDepth; i > 0; i--) {
			for(int j = 0; j < i; j++) {
				StringBuilder deleteQuery = new StringBuilder("DELETE FROM GRAPH %u% {");
				deleteQuery.append(depthClause);
				deleteQuery.append("} FROM %u% WHERE {");
				deleteQuery.append(depthClause);
				deleteQuery.append("}");
				String query = SPARQLStringUtils.strFromTemplate(deleteQuery.toString(), graphURI, URI, graphURI, URI);
				updateQuery(query);
				// Remove one level of depth from the delete query.
				int secondLastDot = depthClause.lastIndexOf(".",depthClause.lastIndexOf(".")-1);
				if(secondLastDot > 0)
					depthClause.delete(secondLastDot, depthClause.length()-1);
			}
		}
	}

	private int getClosureDepth(String URI, String graphURI) throws IOException
	{
		int closureDepth = 0;
		StringBuilder depthClause = new StringBuilder(" %u% ?p0 ?o0 .");
		while(true) {
			StringBuilder depthQuery = new StringBuilder("SELECT * FROM %u% WHERE { ");
			depthQuery.append(depthClause);
			depthQuery.append("} LIMIT 1");
			String query = SPARQLStringUtils.strFromTemplate(depthQuery.toString(), graphURI, URI);
			List<Map<String,String>> results = null;
			results = selectQuery(query);
			if(results.size() > 0) {
				depthClause.append(" ?o");
				depthClause.append(closureDepth);
				depthClause.append(" ?p");
				depthClause.append(closureDepth + 1);
				depthClause.append(" ?o");
				depthClause.append(closureDepth + 1);
				depthClause.append(" .");
				closureDepth++;
			}
			else
				break;
		}
		return closureDepth;
	}

	public void clearGraph(String graphURI) throws IOException
	{
		String query = SPARQLStringUtils.strFromTemplate("CLEAR GRAPH %u%", graphURI);
		updateQuery(query);
	}

	@Override
	protected Map<String, String> getParamsForConstructQuery(String query)
	{
		Map<String, String> params = super.getParamsForConstructQuery(query);
		String mimeType;

		switch(getConstructResultsFormat()) {

		case N3:
			mimeType = "text/rdf+n3";
			break;

		case RDFXML:
		default:
			mimeType = "application/rdf+xml";
			break;

		}

		params.put("format", mimeType);
		return params;
	}

}
