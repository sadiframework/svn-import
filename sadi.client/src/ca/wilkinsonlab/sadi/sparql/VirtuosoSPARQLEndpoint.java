package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.rmi.AccessException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.HttpUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpInputStream;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;


/**
 * 
 * @author Ben Vandervalk
 */
public class VirtuosoSPARQLEndpoint extends SPARQLEndpoint
{
	public final static Log log = LogFactory.getLog(VirtuosoSPARQLEndpoint.class);

	public VirtuosoSPARQLEndpoint(String endpointURI)
	{
		super(endpointURI);
	}
	
	public VirtuosoSPARQLEndpoint(String endpointURI, String username, String password)  throws URIException
	{
		this(endpointURI);
		if(username != null && password != null)
			initCredentials(username, password);
	}
	
	protected void initCredentials(String username, String password) throws URIException 
	{
		// Limit the use of this username/pass to only this endpoint.
		String hostname = new URI(getURI(), false).getHost();
		AuthScope authScope = new AuthScope(hostname, AuthScope.ANY_PORT, "SPARQL");
		Credentials credentials = new UsernamePasswordCredentials(username, password);
		HttpUtils.getHttpClient().getState().setCredentials(authScope, credentials);
	}
	
	@Override
	public void updateQuery(String query) throws HttpException, IOException, HttpResponseCodeException, AccessException
	{
		try {
			HttpInputStream response = HttpUtils.POST(getURI(), getHTTPArgsForUpdateQuery(query));
			response.close();
		}
		catch(HttpResponseCodeException e) {
			if(e.getStatusCode() == HttpStatus.SC_FORBIDDEN) 
				throw new AccessException("unauthorized to perform update query on " + getURI(), e);
			throw e;
		}
	}

	protected Collection<NameValuePair> getHTTPArgsForUpdateQuery(String query) 
	{
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair("query",query));
		return params;
	}

	public void deleteDirectedClosure(String URI, String graphURI) throws URIException, HttpException, HttpResponseCodeException, IOException, AccessException
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
	
	private int getClosureDepth(String URI, String graphURI) throws HttpException, IOException 
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
		String query = SPARQLStringUtils.strFromTemplate(	"CLEAR GRAPH %u%", graphURI);
		updateQuery(query);
	}
}
