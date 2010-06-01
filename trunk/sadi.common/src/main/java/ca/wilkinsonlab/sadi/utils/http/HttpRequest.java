package ca.wilkinsonlab.sadi.utils.http;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class HttpRequest 
{
	URL url;
	Map<String,String> params;
	
	public static final String HTTP_METHOD_POST = "POST";
	public static final String HTTP_METHOD_GET = "GET";

	private static long nextRequestID = 0;
	private long requestID;
	
	String method;
	
	public HttpRequest(String method, URL url, Map<String,String> params) {
		setMethod(method);
		setURL(url);
		setParams(params);
		setRequestID(getNextRequestID());
	}
	
	public HttpRequest(String method, URL url) {
		this(method, url, new HashMap<String,String>());
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public URL getURL() {
		return url;
	}
	
	public void setURL(URL url) {
		this.url = url;
	}
	
	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}
	
	public long getRequestID() {
		return requestID;
	}
	
	protected void setRequestID(long requestID) {
		this.requestID = requestID;
	}

	public String toString() {
		
		StringBuilder str = new StringBuilder();
		str.append(getURL());
		if(getParams() != null && getParams().size() > 0) {
			str.append("?");
			boolean firstParam = true;
			for(String key : getParams().keySet()) {
				if(!firstParam) {
					str.append("&");
				}
				str.append(key);
				str.append("=");
				str.append(getParams().get(key));
				firstParam = false;
			}
		}
		return str.toString();
	}
	
	protected static long getNextRequestID() 
	{
		if(nextRequestID == Long.MAX_VALUE) {
			nextRequestID = 0;
			return nextRequestID;
		}
		
		return ++nextRequestID;
	}
	
	@Override
	public boolean equals(Object other) 
	{
		if(other instanceof HttpRequest) {
			return (getRequestID() == ((HttpRequest)other).getRequestID());
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return (int)(getRequestID() % Integer.MAX_VALUE);
	}
	

}
