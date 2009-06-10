package ca.wilkinsonlab.sadi.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;

public class HttpUtils
{
	private static final Log log = LogFactory.getLog(HttpUtils.class);
	
	public static InputStream postToURL(URL url, Model data)
	throws IOException
	{
		//log.debug(String.format("posting RDF data to %s", url));
		
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
		//log.debug(String.format("posting form data to %s", url));
		//log.trace(params);
		InputStream is = POST(url.toString(), toNameValuePairArray(params));
		
		//log.debug("reading response");
		String json = SPARQLStringUtils.readFully(is);
		is.close();

		//log.debug("converting JSON object");
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
	
	public static HttpInputStream POST(String url, NameValuePair[] params) throws HttpResponseCodeException, HttpException, IOException
	{
		PostMethod method = new PostMethod(url);
		method.setQueryString(params);
		return HTTP(method);
	}

	public static HttpInputStream POST(String url, Collection<NameValuePair> params) throws HttpException, HttpResponseCodeException, IOException
	{
		return HttpUtils.POST(url, params, HttpUtils.DEFAULT_SOCKET_TIMEOUT);
	}

	public static HttpInputStream POST(String url, Collection<NameValuePair> params, int socketTimeout) throws HttpException, HttpResponseCodeException, IOException 
	{
		PostMethod method = new PostMethod(url);
		method.setQueryString( params.toArray(new NameValuePair[0]) );
		method.getParams().setParameter("http.socket.timeout", socketTimeout);
		return HTTP(method);
	}

	public static HttpInputStream GET(String url, Collection<NameValuePair> params) throws HttpException, HttpResponseCodeException, IOException
	{
		return GET(url, params, HttpUtils.DEFAULT_SOCKET_TIMEOUT);
	}

	public static HttpInputStream GET(String url, Collection<NameValuePair> params, int socketTimeout)  throws HttpException, HttpResponseCodeException, IOException
	{
		GetMethod method = new GetMethod(url);
		method.setQueryString( params.toArray(new NameValuePair[0]) );
		method.getParams().setParameter("http.socket.timeout", socketTimeout);
		return HTTP(method);
	}

	public static HttpInputStream HTTP(HttpMethod method) throws HttpException, HttpResponseCodeException, IOException 
	{
	    HttpClient client = new HttpClient();
		int statusCode = client.executeMethod(method);
		if (statusCode != HttpStatus.SC_OK) 
			throw new HttpResponseCodeException(statusCode, "HTTP request failed: " + method.getStatusLine());
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
