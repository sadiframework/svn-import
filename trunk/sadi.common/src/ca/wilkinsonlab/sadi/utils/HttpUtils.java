package ca.wilkinsonlab.sadi.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

public class HttpUtils
{
	private static final Logger log = Logger.getLogger(HttpUtils.class);
	
	private static HttpClient theClient;
	private static int HTTP_CONNECTION_TIMEOUT = 30 * 1000; // in milliseconds
	private static int MAX_CONNECTIONS_PER_HOST = 10;
	
	static 
	{
		HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		connectionManager.getParams().setConnectionTimeout(HTTP_CONNECTION_TIMEOUT);
		// maximum number of simultaneous connections to the same host (the default value is 2)
		connectionManager.getParams().setMaxConnectionsPerHost(HostConfiguration.ANY_HOST_CONFIGURATION, MAX_CONNECTIONS_PER_HOST);
		theClient = new HttpClient(connectionManager);
	}

	public static HttpClient getHttpClient() 
	{ 
		return theClient; 
	}
	
	public static InputStream postToURL(URL url, Model data)
	throws IOException
	{
		log.trace(String.format("posting RDF data to %s", url));
		
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		java.io.OutputStream os = conn.getOutputStream();
		data.write(os);
		os.flush();
		os.close();
	    
		return conn.getInputStream();
	}
	
	public static Object postAndFetchJson(URL url, Map<String, String> params)
	throws IOException
	{
		log.trace(String.format("posting form data to %s", url));
//		log.trace(params);
		InputStream is = POST(url.toString(), toNameValuePairArray(params));
		
		log.trace("reading response");
		String json = SPARQLStringUtils.readFully(is);
		is.close();

		log.trace("converting JSON object");
		return JsonUtils.read(json);
	}
	
	public static NameValuePair[] toNameValuePairArray(Map<String, String> map)
	{
		NameValuePair[] params = new NameValuePair[map.size()];
		int i=0;
		for (String key: map.keySet())
			params[i++] = new NameValuePair(key, map.get(key));
		return params;
	}
	
	public static HttpInputStream POST(String url, NameValuePair[] params) throws IOException
	{
		PostMethod method = new PostMethod(url);
		method.setQueryString(params);
		return HTTP(method);
	}

	public static HttpInputStream POST(String url, Collection<NameValuePair> params) throws IOException
	{
		return HttpUtils.POST(url, params, HttpUtils.DEFAULT_SOCKET_TIMEOUT);
	}

	public static HttpInputStream POST(String url, Collection<NameValuePair> params, int socketTimeout) throws IOException
	{
		PostMethod method = new PostMethod(url);
		method.setRequestBody( params.toArray(new NameValuePair[0]) );
		method.getParams().setParameter("http.socket.timeout", socketTimeout);
		return HTTP(method);
	}

	public static HttpInputStream GET(String url, Collection<NameValuePair> params) throws IOException
	{
		return GET(url, params, HttpUtils.DEFAULT_SOCKET_TIMEOUT);
	}

	public static HttpInputStream GET(String url, Collection<NameValuePair> params, int socketTimeout)  throws IOException
	{
		GetMethod method = new GetMethod(url);
		method.setQueryString( params.toArray(new NameValuePair[0]) );
		method.getParams().setParameter("http.socket.timeout", socketTimeout);
		return HTTP(method);
	}

	public static HttpInputStream HTTP(HttpMethod method) throws IOException
	{
		// automatically authenticate with the target server, if required 
		method.setDoAuthentication(true);
		int statusCode = HttpStatus.SC_OK;
		
		try {
			statusCode = getHttpClient().executeMethod(method);
		}
		catch(IOException e) {
			method.releaseConnection();
			throw e;
		}
		
		if (statusCode != HttpStatus.SC_OK)  {
			method.releaseConnection();
			throw new HttpResponseCodeException(statusCode, "HTTP request failed: " + method.getStatusLine());
		}
		
		return new HttpInputStream(method.getResponseBodyAsStream(),method);
	}

	public static boolean isProxyTimeout(HttpResponseCodeException e)
	{
		if(e.getStatusCode() == HttpStatus.SC_BAD_GATEWAY || e.getStatusCode() == HttpStatus.SC_GATEWAY_TIMEOUT)
			return true;
		else 
			return false;
	}

	public static boolean isHTTPTimeout(Exception e) 
	{
		if(e instanceof HttpResponseCodeException)
			return isProxyTimeout((HttpResponseCodeException)e);
		else if(e instanceof SocketTimeoutException)
			return true;
		else
			return false;
	}
	
	public static boolean isHttpError(Exception e)
	{
		if (e instanceof HttpResponseCodeException)
			return 500 <= ((HttpResponseCodeException)e).statusCode  &&
			              ((HttpResponseCodeException)e).statusCode < 600;
		else
			return false;
	}
	
	public static boolean isResourceUnavailableError(Exception e)
	{
		if (e instanceof HttpResponseCodeException) {
			HttpResponseCodeException e2 = (HttpResponseCodeException)e;
			if(e2.getStatusCode() == HttpStatus.SC_NOT_FOUND || e2.getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE)
				return true;
			else
				return false;
		}
		else
			return false;
	}

	public static final int DEFAULT_SOCKET_TIMEOUT = 2 * 60 * 1000; // in ms
	
	/**
	 * InputStream that closes an associated HTTP connection, at the 
	 * same time that the stream is closed. This class was needed because 
	 * the Apache Commons HTTPClient does not allow an HTTP response to be 
	 * read unless the associated connection remains open.  This poses
	 * a problem if the caller of an HTTP request wants to read the response
	 * at some arbitrarily later time.
	 * @author Ben Vandervalk 
	 */
	public static class HttpInputStream extends BufferedInputStream
	{
		 HttpMethod method;
		 
		 public HttpInputStream(InputStream inputStream, HttpMethod method)
		 {
			 super(inputStream);
			 
			 this.method = method;
		 }
		 
		 public void close() throws IOException
		 {
			 try {
				 super.close();
			 } finally {
				 method.releaseConnection();
			 }
		 }
	}
	
	public static class HttpResponseCodeException extends HttpException
	{
		private static final long serialVersionUID = 1L;
		int statusCode;
		
		public HttpResponseCodeException(int statusCode, String message) 
		{
			super(message);
			
			this.statusCode = statusCode;
		}
		
		public int getStatusCode()
		{
			return statusCode;
		}
		
		public boolean isProxyTimeout()
		{
			if (getStatusCode() == HttpStatus.SC_BAD_GATEWAY || getStatusCode() == HttpStatus.SC_GATEWAY_TIMEOUT)
				return true;
			else 
				return false;
		}
	}
}
