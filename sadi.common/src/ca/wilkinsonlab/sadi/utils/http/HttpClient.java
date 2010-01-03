package ca.wilkinsonlab.sadi.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;

public interface HttpClient 
{
	final static String HTTP_AUTH_ANY_REALM = null;
	final static int HTTP_AUTH_ANY_PORT = -1;

	/**
	 * Retrieve an URL by HTTP GET.
	 * 
	 * @param url
	 * @return an InputStream containing the contents of the HTTP response body. 
	 * @throws IOException if there is a network error or the response has a non-success HTTP status code. 
	 */
	abstract public InputStream GET(URL url) throws IOException;
	/**
	 * Retrieve an URL by HTTP GET.
	 * 
	 * @param url
	 * @param params the name/value pairs of the GET query string.
	 * @return an InputStream containing the contents of the HTTP response body. 
	 * @throws IOException if there is a network error or the response has a non-success HTTP status code. 
	 */
	abstract public InputStream GET(URL url, Map<String,String> params) throws IOException;
	
	/**
	 * Send data to an URL by HTTP POST.
	 * 
	 * @param url 
	 * @param postData an InputStream containing the data to be posted.
	 * @param contentType the content type (e.g. "text/xml")
	 * @return an InputStream containing the contents of the HTTP response body. 
	 * @throws IOException if there is a network error or the response has a non-success HTTP status code. 
	 */
	abstract public InputStream POST(URL url, InputStream postData, String contentType) throws IOException;
	/**
	 * Send form data to an URL by HTTP POST. Content type is "application/x-www-form-urlencoded".
	 * 
	 * @param url 
	 * @param params the 
	 * @return an InputStream containing the contents of the HTTP response body. 
	 * @throws IOException if there is a network error or the response has a non-success HTTP status code. 
	 */
	abstract public InputStream POST(URL url, Map<String,String> params) throws IOException;
	
	/**
	 * Send an HTTP request.
	 *   
	 * @param request an HTTP request (consists of the HTTP method, the URL, the parameters, etc.)
	 * @return an InputStream containing the contents of the HTTP response body.
	 * @throws IOException if there is a network error or the response has a non-success HTTP status code.
	 */
	abstract public InputStream request(HttpRequest request) throws IOException;
	/**
	 * <p>Issue a batch of HTTP requests in parallel.</p>
	 * 
	 * <p>Note that this method does not throw any exceptions. If an exception occurs during any particular
	 * HttpRequest, HttpResponse.exceptionOccured() will be true in the corresponding 
	 * HttpResponse.  Each HttpResponse contains a reference to the original HttpRequest.</p>
	 * 
	 * <p>The caller need not worry about overloading a server with too many requests.  There
	 * is an internally set limit on how many simultaneous connections can be made to a
	 * server at one time.</p>
	 * 
	 * @param requests a set of HTTP requests (each request consists of the HTTP method, the URL, the parameters, etc.)
	 * @return a set of responses for the input requests.
	 */
	abstract public Set<HttpResponse> batchRequest(Set<HttpRequest> requests);
	
	/**
	 * Set HTTP authentication credentials for a given (host,port,realm) combination.  Both
	 * Basic authentication and Digest authentication are supported.
	 *  
	 * @param host
	 * @param port
	 * @param realm
	 * @param username
	 * @param password
	 */
	abstract public void setHttpAuthCredentials(String host, int port, String realm, String username, String password);
}
