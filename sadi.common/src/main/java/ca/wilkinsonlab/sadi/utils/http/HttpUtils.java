package ca.wilkinsonlab.sadi.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Map;

import javax.xml.ws.http.HTTPException;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.utils.JsonUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.rdf.model.Model;

public class HttpUtils
{
	private static final Logger log = Logger.getLogger(HttpUtils.class);
	protected static HttpClient theClient;
	
	protected HttpUtils() {}
	
	public static synchronized HttpClient theClient() {
		if(theClient == null) {
			theClient = new XLightwebHttpClient();
		}
		return theClient;
	}
	
	static public InputStream GET(URL url) throws IOException {
		return theClient().GET(url);
	}
	
	static public InputStream GET(URL url, Map<String,String> params) throws IOException {
		return theClient().GET(url, params);
	}
	
	static InputStream POST(URL url, InputStream postData, String contentType) throws IOException {
		return theClient().POST(url, postData, contentType);
	}
	
	static public InputStream POST(URL url, Map<String,String> params) throws IOException {
		return theClient().POST(url, params);
	}
	
	static public InputStream request(HttpRequest request) throws IOException {
		return theClient().request(request);
	}
	
	static public Collection<HttpResponse> batchRequest(Collection<HttpRequest> requests) {
		return theClient().batchRequest(requests);
	}
	
	static public void setHttpAuthCredentials(String host, int port, String realm, String username, String password) {
		theClient().setHttpAuthCredentials(host, port, realm, username, password);
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
		InputStream is = POST(url, params);
		
		log.trace("reading response");
		String json = SPARQLStringUtils.readFully(is);
		is.close();

		log.trace("converting JSON object");
		return JsonUtils.read(json);
	}

	public static class HttpStatusException extends IOException {

		private static final long serialVersionUID = 1L;
		int statusCode;
		
		public HttpStatusException(int statusCode) {
			super("HTTP status code " + String.valueOf(statusCode));
			this.statusCode = statusCode;
		}
		
		public int getStatusCode() {
			return statusCode;
		}
	}
}
