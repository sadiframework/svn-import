package ca.wilkinsonlab.sadi.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.AccessException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.HttpUtils;
import ca.wilkinsonlab.sadi.utils.JsonUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.StringUtil;
import ca.wilkinsonlab.sadi.utils.HttpUtils.HttpResponseCodeException;

import com.hp.hpl.jena.graph.Triple;

/**
 * 
 * @author Ben Vandervalk
 */
public class VirtuosoSPARQLEndpoint extends SPARQLService
{
	public final static Log log = LogFactory.getLog(VirtuosoSPARQLEndpoint.class);

	public VirtuosoSPARQLEndpoint(String endpointURI)
	{
		super(endpointURI);
	}

	@Override
	public void updateQuery(String query) throws HttpException, IOException, HttpResponseCodeException, AccessException
	{
		try {
			HttpUtils.POST(getURI(), getHTTPArgsForUpdateQuery(query));
		}
		catch(HttpResponseCodeException e) {
			// Virtuoso indicates a failed SPARUL query due to lack
			// of permission as HTTP response code 500 ("Internal 
			// Server Error").  
			if(e.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) 
				throw new AccessException("No permission to perform update query on endpoint",e);
			throw e;
		}
	}

	protected Collection<Triple> convertConstructResponseToTriples(InputStream response)
	{
		return RdfUtils.getTriples(response, "N3");
	}

	protected List<Map<String,String>> convertSelectResponseToBindings(InputStream response) throws IOException 
	{
		String responseAsString = IOUtils.toString(response);
		return JsonUtils.convertJSONToResults(JsonUtils.read(responseAsString));
	}
	
	protected Collection<NameValuePair> getHTTPArgsForConstructQuery(String query) 
	{
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair("query",query));
		params.add(new NameValuePair("format","text/rdf+n3"));
		return params;
	}
	
	protected Collection<NameValuePair> getHTTPArgsForSelectQuery(String query)
	{
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair("query",query));
		params.add(new NameValuePair("format","JSON"));
		return params;
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
				String query = StringUtil.strFromTemplate(deleteQuery.toString(), graphURI, URI, graphURI, URI);
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
			String query = StringUtil.strFromTemplate(depthQuery.toString(), graphURI, URI);
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

}
