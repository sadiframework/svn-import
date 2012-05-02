package org.sadiframework.utils.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.Map;

import javax.xml.ws.http.HTTPException;

import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.sadiframework.utils.ContentType;
import org.sadiframework.utils.JsonUtils;
import org.sadiframework.utils.SPARQLStringUtils;


import com.hp.hpl.jena.rdf.model.Model;

/**
 * <p>Provides convenience methods for issuing HTTP GET/POST requests, and 
 * for performing other HTTP related tasks. The provided GET/POST methods automatically
 * follow redirects and do not support HTTP authentication.</p>
 * 
 * <p>If you want to manually handle redirects or need to do authentication,
 * or you need to customize the behaviour of the HTTP client in some way, 
 * you should create an instance of {@link ca.wilkinsonlab.utils.http.HttpClient} and 
 * use that instead. It supports the same interface for issuing GET/POST requests.</p>  
 * 
 * @author Ben Vandervalk
 */
public class HttpUtils
{
	private static final Logger log = Logger.getLogger(HttpUtils.class);
	protected static final String ENCODING_UTF8 = "UTF-8";
	
	static public HttpResponse GET(URL url) throws IOException	{
		return new HttpClient().GET(url);
	}

	static public HttpResponse GET(URL url, Map<String,String> params) throws IOException {
		return new HttpClient().GET(url, params);
	}

	static public HttpResponse GET(URL url, Map<String,String> params, Map<String,String> headers) throws IOException {
		return new HttpClient().GET(url, params, headers);
	}
	
	static HttpResponse POST(URL url, InputStream postData, String contentType) throws IOException {
		return new HttpClient().POST(url, postData, contentType);
	}

	static HttpResponse POST(URL url, InputStream postData, String contentType, Map<String,String> headers) throws IOException {
		return new HttpClient().POST(url, postData, contentType, headers);
	}

	static public HttpResponse POST(URL url, Map<String,String> params) throws IOException {
		return new HttpClient().POST(url, params);
	}
	
	static public HttpResponse POST(URL url, Map<String,String> params, Map<String,String> headers) throws IOException {
		return new HttpClient().POST(url, params, headers);
	}
	
	public static boolean isHttpError(int statusCode) 
	{
		return (statusCode >= 400 && statusCode < 600);
	}
	
	static public boolean isHttpServerError(Throwable e) {
		if(e instanceof HTTPException) {
			HTTPException asHTTPException = (HTTPException)e;
			if(asHTTPException.getStatusCode() >= 500 && asHTTPException.getStatusCode() <= 600) {
				return true;
			}
		}
		return false;
	}

	public static InputStream postToURL(URL url, Model data)
	throws IOException
	{
		log.trace(String.format("posting RDF data to %s", url));
		ContentType rdfXML = ContentType.RDF_XML;
		PipedInputStream in = new PipedInputStream();
		PipedOutputStream out = new PipedOutputStream(in);
		new Thread(new ModelWriter(data, out, rdfXML.getJenaLanguage())).start();
		HttpResponse response = POST(url, in, rdfXML.getHTTPHeader());
		int statusCode = response.getStatusLine().getStatusCode();
		if (isHttpError(statusCode)) 
			throw new HttpStatusException(statusCode);
		return response.getEntity().getContent();
	}
	
	public static Object postAndFetchJson(URL url, Map<String, String> params)
	throws IOException
	{
		log.trace(String.format("posting form data to %s\n%s", url, params));

		HttpResponse response = POST(url, params);
		int statusCode = response.getStatusLine().getStatusCode();
		if (isHttpError(statusCode)) 
			throw new HttpStatusException(statusCode);
		InputStream is = response.getEntity().getContent();
		
		log.trace("reading response");
		String json = SPARQLStringUtils.readFully(is);
		is.close();

		log.trace(String.format("converting JSON object\n%s", json));
		return JsonUtils.read(json);
	}
	
	public static BufferedReader getReader(String url) throws IOException
	{
		HttpResponse response = GET(new URL(url));
		int statusCode = response.getStatusLine().getStatusCode();
		if (isHttpError(statusCode)) 
			throw new HttpStatusException(statusCode);
		
		InputStream is = response.getEntity().getContent();
		return new BufferedReader(new InputStreamReader(is));
	}

	/* Unused, depends on commons httpclient 3.x 
	public static String getHeaderValue(HttpMethod method, String headerName)
	{
		return getHeaderValue(method, headerName, null);
	}
	*/

	/* Unused, depends on commons httpclient 3.x
	public static String getHeaderValue(HttpMethod method, String headerName, String headerValuePrefix)
	{
		for (Header header: method.getResponseHeaders(headerName)) {
			String headerValue = header.getValue();
			if (headerValue == null)
				continue;
			if (headerValuePrefix != null) {
				if (headerValue.startsWith(headerValuePrefix))
					return headerValue.substring(headerValuePrefix.length());
			} else {
				return headerValue;
			}
		}
		return null;
	}
	*/
	
	/**
	 * @author Ben Vandervalk
	 */
	public static class HttpStatusException extends IOException
	{
		private static final long serialVersionUID = 1L;
		int statusCode;
		
		public HttpStatusException(int statusCode)
		{
			super("HTTP status code " + String.valueOf(statusCode));
			this.statusCode = statusCode;
		}
		
		public int getStatusCode()
		{
			return statusCode;
		}
	}
	
	/**
	 * @author Luke McCarthy
	 */
	public static final class ModelWriter implements Runnable
	{
		private static final Logger log = Logger.getLogger(ModelWriter.class);
		
		private Model model;
		private OutputStream out;
		private String lang;
		
		public ModelWriter(Model model, OutputStream out, String lang)
		{
			this.model = model;
			this.out = out;
			this.lang = lang;
		}

		@Override
		public void run()
		{
			model.write(out, lang);
			try {
				out.flush();				
				out.close();
			} catch (IOException e) {
				log.error(e.toString(), e);
			}
		}
	}
}
