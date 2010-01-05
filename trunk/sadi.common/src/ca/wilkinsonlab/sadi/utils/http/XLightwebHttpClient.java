package ca.wilkinsonlab.sadi.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.xlightweb.HttpRequestHeader;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHeader;
import org.xlightweb.IHttpResponse;
import org.xlightweb.IHttpResponseHandler;

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

import ca.wilkinsonlab.sadi.utils.http.HttpUtils.HttpStatusException;

public class XLightwebHttpClient implements HttpClient {

	protected static Logger log = Logger.getLogger(XLightwebHttpClient.class);
	protected org.xlightweb.client.HttpClient xLightWebClient;
	
	/** Stores username/password pairs for HTTP authentication. */ 
	protected HttpState httpState = new HttpState();
	
	/** Caches authorization headers for repeated accesses to the same URL */
	protected Map<URL,String> cachedAuthHeaderMap = new Hashtable<URL,String>();
	
	protected XLightwebHttpClient() {
		xLightWebClient = new org.xlightweb.client.HttpClient();
		xLightWebClient.setMaxActive(200);
		xLightWebClient.setMaxActivePerServer(30);
		xLightWebClient.setConnectTimeoutMillis(10 * 1000);
		xLightWebClient.setResponseTimeoutMillis(20 * 1000);
		xLightWebClient.setMaxRetries(0);
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
		
		// fire off each request asynchronously
		for(HttpRequest request : requests) {
			try {
				IHttpRequest xLightwebRequest = getXLightwebHttpRequest(request);
				xLightWebClient.send(xLightwebRequest, new XLightwebCallback(request, responses));
			} catch(IOException e) {
				responses.add(new HttpResponse(request, e));
			}
		}
		
		// wait until all responses have failed or succeeded
		while(responses.size() < requests.size()) {
			try {
				Thread.sleep(300);
			} catch(InterruptedException e) {
				throw new RuntimeException("unhandled InterruptedException for thread " + Thread.currentThread(), e);
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
	
	/*
	public long getResponseTimeout() {
		return theXLightWebClient.getResponseTimeoutMillis();
	}
	
	public long setResponseTimeout(long timeoutInMilliseconds) {
		long oldValue = getResponseTimeout();
		theXLightWebClient.setResponseTimeoutMillis(timeoutInMilliseconds);
		return oldValue;
	}
	*/
	
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
			log.warn("exception occurred for asynchronous HTTP request " + originalRequest + ": " + e.getMessage()); 
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
