package ca.wilkinsonlab.sadi.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

/**
 * <p>A wrapped Apache Commons HTTP client with two differences:</p>
 * 
 * <ol>
 *    <li>additional convenience methods are provided for making GET/POST requests.</li>
 *    <li>the default connection manager is ThreadSafeClientConnManager, not SingleClientConnManager</li>
 * </ol>
 * 
 * <p>It is important to close the HTTP connections that are opened 
 * by the GET and POST methods, by calling:</p>
 * 
 * {@code response.getEntity().getContent().close()}
 * 
 * <p>once you are done with the response. This closes both the InputStream for the 
 * response content and also the associated HTTP connection. If you do not properly 
 * close your connections and the client hits its internal limits for the number of 
 * open connections, the next HTTP request will hang indefinitely.  You may
 * explicitly specify the connection limits in the constructor for
 * this class.</p>
 */
public class HttpClient extends DefaultHttpClient
{
	protected static final String ENCODING_UTF8 = "UTF-8";
	
	public HttpClient() 
	{
		super(new ThreadSafeClientConnManager());
	}
	
	public HttpClient(int maxConnectionsPerRoute, int maxConnectionsTotal)
	{
		super(getConnectionsManager(maxConnectionsPerRoute, maxConnectionsTotal));
	}
	
	protected static ClientConnectionManager getConnectionsManager(int maxConnectionsPerRoute, int maxConnectionsTotal)
	{
		ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager();
		connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
		connectionManager.setMaxTotal(maxConnectionsTotal);
		return connectionManager;
	}
	
	public HttpResponse GET(URL url) throws IOException
	{
		HttpGet get = new HttpGet(urlToUri(url));
		return execute(get);
	}
	
	public HttpResponse GET(URL url, Map<String,String> params) throws IOException
	{
		return GET(url, params, null);
	}

	public HttpResponse GET(URL url, Map<String,String> params, Map<String,String> headers) throws IOException
	{
		URI uri;
		try {

			// merge the query string in the url argument (if any) with the query string 
			// parameters provided in the params argument (if any)
			List<NameValuePair> mergedParams = new ArrayList<NameValuePair>();
			mergedParams.addAll(URLEncodedUtils.parse(urlToUri(url), ENCODING_UTF8));
			if (params != null)
				mergedParams.addAll(mapToNameValuePairs(params));
			
			String queryString = mergedParams.isEmpty() ? null : URLEncodedUtils.format(mergedParams, ENCODING_UTF8);
			
			// the port argument is set to zero here because the port is already included in the string 
			// returned by url.getAuthority() 
			uri = URIUtils.createURI(url.getProtocol(), url.getAuthority(), 0, url.getPath(), queryString, url.getRef());
		
		} catch(URISyntaxException e) {
			throw new IOException(e);
		}
		HttpGet get = new HttpGet(uri);
		if (headers != null) {
			for (String header : headers.keySet()) 
				get.setHeader(header, headers.get(header));
		}
		return execute(get);
	}

	public HttpResponse POST(URL url, InputStream postData, String contentType) throws IOException
	{
		return POST(url, postData, contentType, null);
	}
	
	public HttpResponse POST(URL url, InputStream postData, String contentType, Map<String,String> headers) throws IOException
	{
		InputStreamEntity entity = new InputStreamEntity(postData, -1);
		entity.setContentType(new BasicHeader("Content-Type", contentType));
		HttpPost post = new HttpPost(urlToUri(url));
		post.setEntity(entity);
		if (headers != null) {
			for (String header : headers.keySet()) 
				post.setHeader(header, headers.get(header));
		}
		return execute(post);
	}

	public HttpResponse POST(URL url, Map<String,String> params) throws IOException
	{
		return POST(url, params, null);
	}
	
	public HttpResponse POST(URL url, Map<String,String> params, Map<String,String> headers) throws IOException
	{
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(mapToNameValuePairs(params));
		HttpPost post = new HttpPost(urlToUri(url));
		post.setEntity(entity);
		if (headers != null) {
			for (String header : headers.keySet()) 
				post.setHeader(header, headers.get(header));
		}
		return execute(post);
	}

	public void setAuthCredentials(String hostname, int port, String realm, String username, String password) 
	{
		AuthScope authScope = new AuthScope(hostname, port, realm);
		Credentials credentials = new UsernamePasswordCredentials(username, password);
		getCredentialsProvider().setCredentials(authScope, credentials);
	}
	
	static protected List<NameValuePair> mapToNameValuePairs(Map<String,String> map)
	{
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for(String key : map.keySet()) {
			pairs.add(new BasicNameValuePair(key, map.get(key)));
		}
		return pairs;
	}
	
	protected static URI urlToUri(URL url) throws IOException 
	{
		URI uri;
		try {
			// The Java URI class allows Unicode chars, 
			// whereas the URI spec only allows ASCII chars.
			// URI.toASCIIString() does the conversion by %-encoding
			// any Unicode chars.
			uri = new URI(url.toURI().toASCIIString());
		} catch (URISyntaxException e) {
			// This exception should be rare; it happens when there
			// are illegal chars in an URL (e.g. a space). 
			// Wrap it in an IOException so users don't have to deal with 
			// it separately everywhere.
			throw new IOException(e);
		}
		return uri;
	}	
	
	
}
