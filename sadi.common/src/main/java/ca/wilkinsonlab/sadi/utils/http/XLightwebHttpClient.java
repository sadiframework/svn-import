package ca.wilkinsonlab.sadi.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.AuthChallengeParser;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import ca.wilkinsonlab.sadi.common.Config;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils.HttpStatusException;

public class XLightwebHttpClient implements HttpClient {

	protected static final String CONFIG_ROOT = "sadi.http";
	protected static final String RESPONSE_TIMEOUT_CONFIG_KEY = "responseTimeout";
	protected static final String MAX_CONNECTIONS_PER_HOST_CONFIG_KEY = "maxConnectionsPerHost";
	/**
	 * Maximum number of retries for failed HTTP requests. 
	 * Note that only synchronous requests will be 
	 * automatically retried, not requests issued with 
	 * batchRequest().
	 */
	protected static final String MAX_RETRIES_CONFIG_KEY = "maxRetries";
	/**
	 * Timeout when waiting for the next chunk of an HTTP response body.
	 */
	protected static final String DATA_WAIT_TIMEOUT_CONFIG_KEY = "dataWaitTimeout";
	
	
	protected static Logger log = Logger.getLogger(XLightwebHttpClient.class);
	protected org.xlightweb.client.HttpClient xLightWebClient;
	
	/** Stores username/password pairs for HTTP authentication. */ 
	protected HttpState httpState = new HttpState();
	
	/** Caches authorization headers for repeated accesses to the same URL */
	protected Map<URL,String> cachedAuthHeaderMap = new Hashtable<URL,String>();
	
	protected XLightwebHttpClient() {
		
		Configuration config = Config.getConfiguration().subset(CONFIG_ROOT);
		
		xLightWebClient = new org.xlightweb.client.HttpClient();
		xLightWebClient.setMaxActive(200);
		xLightWebClient.setMaxActivePerServer(config.getInt(MAX_CONNECTIONS_PER_HOST_CONFIG_KEY, 30));
		xLightWebClient.setConnectTimeoutMillis(20 * 1000); 
		xLightWebClient.setResponseTimeoutMillis(config.getInt(RESPONSE_TIMEOUT_CONFIG_KEY, 30 * 1000));
		
		// NOTE:
		//
		// setCallReturnOnMessage(true) forces xLightweb to retrieve the entire content of the response body before
		// returning from a synchronous HTTP request (i.e. xLightWebClient.call()). Ordinarily,
		// xLightweb returns a stream for reading the response as soon as it gets the response headers.
		// However, this prevents xLightweb from automatically retrying if there is a failure
		// when reading the response body.  For more info, see tutorial at:
		//
		// http://xlightweb.sourceforge.net/core/tutorial/V2/TutorialCore.htm 
		//
		// UPDATE: xLightweb v2.10 does *not* automatically retry when there is a timeout reading
		// the response body (even when setCallReturnOnMessage is true), although it is supposed to.
		
		xLightWebClient.setCallReturnOnMessage(true);
		xLightWebClient.setBodyDataReceiveTimeoutMillis(config.getInt(DATA_WAIT_TIMEOUT_CONFIG_KEY, 120 * 1000));

		xLightWebClient.setMaxRetries(config.getInt(MAX_RETRIES_CONFIG_KEY, 0));

		xLightWebClient.setCacheMaxSizeKB(0);
		xLightWebClient.setFollowsRedirect(false);
	}
	
	public InputStream GET(URL url) throws IOException {
		return request(new GetRequest(url));
	}
	
	public InputStream GET(URL url, Map<String,String> params) throws IOException {
		return request(new GetRequest(url, params));
	}
	
	public InputStream POST(URL url, InputStream postData, String contentType) throws IOException {
		return request(new PostRequest(url, postData, contentType));
	}
	
	public InputStream POST(URL url, Map<String,String> params) throws IOException {
		return request(new PostRequest(url, params));
	}

	public InputStream POST(PostRequest request) throws IOException {
		return request(request);
	}

	public Collection<HttpResponse> batchRequest(Collection<HttpRequest> requests) {
		Collection<HttpResponse> responses = Collections.synchronizedList(new ArrayList<HttpResponse>());

		// xLightweb does not automatically keep the number of active connections below the
		// limits specified xLightWebClient.setMaxActive() and xLightWebClient.setMaxActivePerServer().
		// (I was very surprised by this!) It merely throws an MaxConnectionsExceededException when the 
		// limits are exceeded, and it's up to us to retry or whatever.
		//
		// Here, I handle this problem by waiting until the number of active connections is below the limit, before 
		// sending a new request.
		
		// TODO: In the future we can do something smarter here
		int maxActive = Math.min(xLightWebClient.getMaxActive(), xLightWebClient.getMaxActivePerServer());
		
		Queue<HttpRequest> requestQueue = new LinkedList<HttpRequest>(requests);
		while(responses.size() < requests.size()) {
			synchronized(this) {
				if(requestQueue.size() > 0 && (xLightWebClient.getNumActive() < maxActive)) {
					HttpRequest request = requestQueue.remove();
					try {
						IHttpRequest xLightwebRequest = getXLightwebHttpRequest(request);
						log.trace(String.format("issuing asynchronous request: %s", request));
						// fire off request asynchronously
						xLightWebClient.send(xLightwebRequest, new XLightwebCallback(request, responses));
					} catch(IOException e) {
						responses.add(new HttpResponse(request, e));
					}
				} else {
					try {
						Thread.sleep(300);
					} catch(InterruptedException e) {
						throw new RuntimeException("unhandled InterruptedException for thread " + Thread.currentThread(), e);
					}
				}
			}
		}
		
		return responses;
	}
	
	static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
	static final String WWW_AUTHORIZATION_HEADER = "Authorization";
	
	public InputStream request(HttpRequest request) throws IOException {
		
		IHttpRequest xLightwebRequest = getXLightwebHttpRequest(request);

		// Technically, this is not a correct implementation of cached authorization under the HTTP Digest authentication 
		// (qop="auth" or "auth-int") scheme.  The auth header contains a nonce counter value that is *supposed to be* incremented with 
		// each request to the same realm.  However, the commons HTTP client code (DigestScheme) always uses a value of 
		// "1" for this counter and it is not easy to fix that.  It seems to work fine with the fixed counter value. 
		// -- BV
		
		String cachedAuthHeader = getCachedAuthHeader(request.getURL());
		if(cachedAuthHeader != null) {
			xLightwebRequest.addHeader(WWW_AUTHORIZATION_HEADER, cachedAuthHeader);
		}
		
		IHttpResponse xLightwebResponse = xLightWebClient.call(xLightwebRequest);
		
		if(xLightwebResponse.getStatus() == HttpResponse.HTTP_STATUS_AUTHORIZATION_REQUIRED) {
		
			String challenge = xLightwebResponse.getHeader(WWW_AUTHENTICATE_HEADER);
			String challengeResponse = getChallengeResponse(challenge, request);
			
			// no username/password has been set for this host/port
			if(challengeResponse == null) {
				throw new HttpStatusException(xLightwebResponse.getStatus());
			}
			
			// xLightweb HTTP requests have a read-once message body; in order to try again, we must recreate the request 
			xLightwebRequest = getXLightwebHttpRequest(request);
			
			xLightwebRequest.addHeader(WWW_AUTHORIZATION_HEADER, challengeResponse);
			xLightwebResponse = xLightWebClient.call(xLightwebRequest);
		}
		
		if(xLightwebResponse.getStatus() != HttpResponse.HTTP_STATUS_SUCCESS) {
			xLightwebResponse.getBlockingBody().close();
			throw new HttpStatusException(xLightwebResponse.getStatus());
		}
		
		return Channels.newInputStream(xLightwebResponse.getBlockingBody());
	}
	
	public void setHttpAuthCredentials(String host, int port, String realm, String username, String password) {
	
		if(port == HttpClient.HTTP_AUTH_ANY_PORT) {
			port = AuthScope.ANY_PORT;
		}
		if(realm == HttpClient.HTTP_AUTH_ANY_REALM) {
			realm = AuthScope.ANY_REALM; 
		}
		httpState.setCredentials(new AuthScope(host, port, realm), new UsernamePasswordCredentials(username, password));
	}
	
	protected String getCachedAuthHeader(URL targetURL) {
		for(URL url : cachedAuthHeaderMap.keySet()) {
			if(targetURL.toString().startsWith(url.toString())) {
				return cachedAuthHeaderMap.get(url);
			}
		}
		return null;
	}
	
	protected String getChallengeResponse(String challenge, HttpRequest request) throws IOException {
	
		URL requestURL = request.getURL();
		int port = requestURL.getPort() == -1 ? 80 : requestURL.getPort();
		String realm = (String)(AuthChallengeParser.extractParams(challenge).get("realm"));
		
		AuthScope authScope = new AuthScope(requestURL.getHost(), port, realm);
		Credentials credentials = httpState.getCredentials(authScope);

		/* no username/password is available for this host/port */
		if(credentials == null) {
			return null;
		}
		
		AuthScheme authScheme = AuthPolicy.getAuthScheme(AuthChallengeParser.extractScheme(challenge));
		authScheme.processChallenge(challenge);
		
		return authScheme.authenticate(credentials, getApacheHttpMethod(request));
	}

	protected HttpMethod getApacheHttpMethod(HttpRequest request) throws IOException {

		String method = request.getMethod();
		if(!method.equals(HttpRequest.HTTP_METHOD_GET) && !method.equals(HttpRequest.HTTP_METHOD_POST)) {
			throw new UnsupportedOperationException("this method only supports conversion of GET or POST requests");
		}
		
		HttpMethod apacheHttpMethod;
		if(method.equals(HttpRequest.HTTP_METHOD_GET)) {
			apacheHttpMethod = new GetMethod();
		} else {
			PostMethod postMethod = new PostMethod();
			PostRequest asPostRequest = (PostRequest)request;
			String contentType = asPostRequest.getContentType();
			if(contentType != null && !contentType.equals(PostRequest.CONTENT_TYPE_URL_ENCODED_FORM)) {
				postMethod.setRequestHeader("Content-type", asPostRequest.getContentType());
			} 
			apacheHttpMethod = postMethod;
		}
		apacheHttpMethod.setQueryString(toNameValuePairArray(request.getParams()));

		return apacheHttpMethod;
	}
	
	protected NameValuePair[] toNameValuePairArray(Map<String, String> map)
	{
		NameValuePair[] params = new NameValuePair[map.size()];
		int i=0;
		for (String key: map.keySet())
			params[i++] = new NameValuePair(key, map.get(key));
		return params;
	}

	protected org.xlightweb.IHttpRequest getXLightwebHttpRequest(HttpRequest request) throws IOException {

		IHttpRequest xLightwebRequest; 
		String url = request.getURL().toString();
		
		if(request instanceof PostRequest) {
			PostRequest asPostRequest = (PostRequest)request;
			String contentType = asPostRequest.getContentType();
			if(contentType != null && !contentType.equals(PostRequest.CONTENT_TYPE_URL_ENCODED_FORM)) {
				IHttpRequestHeader requestHeader = new HttpRequestHeader(HttpRequest.HTTP_METHOD_POST, url, contentType);
				xLightwebRequest = new org.xlightweb.HttpRequest(requestHeader, asPostRequest.getInputStream());
			} else {
				xLightwebRequest = new org.xlightweb.PostRequest(url);
			}
		} else {
			xLightwebRequest = new org.xlightweb.GetRequest(url);
		}
		
		Map<String,String> params = request.getParams();
		for(String key : params.keySet()) {
			xLightwebRequest.setParameter(key, params.get(key));
		}
		return xLightwebRequest;
	}

	protected static class XLightwebCallback implements IHttpResponseHandler
	{
		HttpRequest originalRequest;
		Collection<HttpResponse> responses;
		
		public XLightwebCallback(HttpRequest originalRequest, Collection<HttpResponse> responses) 
		{
			this.originalRequest = originalRequest;
			this.responses = responses;
		}

		@Override
		public void onException(IOException e) throws IOException 
		{
			log.error(String.format("exception occurred for asynchronous HTTP request %s:", originalRequest.toString()), e);
			responses.add(new HttpResponse(originalRequest, e));
		}

		@Override
		public void onResponse(IHttpResponse response) throws IOException 
		{
			if(response.getStatus() == HttpResponse.HTTP_STATUS_SUCCESS) {
				log.trace("received asynchronous response for HTTP request " + originalRequest);
				InputStream inputStream = Channels.newInputStream(response.getBlockingBody());
				responses.add(new HttpResponse(originalRequest, inputStream));
			} else {
				if(response.getStatus() == HttpResponse.HTTP_STATUS_AUTHORIZATION_REQUIRED) {
					log.error("request failed, authentication is not implemented for asynchronous HTTP requests");
				} else {
					log.error("HTTP request " + originalRequest + " failed with HTTP status code " + response.getStatus());
				}
				responses.add(new HttpResponse(originalRequest, new HttpStatusException(response.getStatus())));
			}	
		}
    }
	
}
