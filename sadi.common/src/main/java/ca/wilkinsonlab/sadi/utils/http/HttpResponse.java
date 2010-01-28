package ca.wilkinsonlab.sadi.utils.http;

import java.io.InputStream;

public class HttpResponse {
	
	public static final int HTTP_STATUS_SUCCESS = 200;
	public static final int HTTP_STATUS_AUTHORIZATION_REQUIRED = 401;
	public static final int HTTP_STATUS_GATEWAY_TIMEOUT = 504;
	
	protected HttpRequest originalRequest;
	protected Exception exception;
	protected InputStream inputStream;
	
	public HttpResponse(HttpRequest originalRequest, InputStream inputStream) { //String response) {
		setOriginalRequest(originalRequest);
		setInputStream(inputStream);
	}
	
	public HttpResponse(HttpRequest originalRequest, Exception exception) {
		setOriginalRequest(originalRequest);
		setException(exception);
	}
		
	public boolean exceptionOccurred() {
		return exception != null;
	}

	public HttpRequest getOriginalRequest() {
		return originalRequest;
	}

	public void setOriginalRequest(HttpRequest originalRequest) {
		this.originalRequest = originalRequest;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	/*
	protected void finalize() throws Throwable {
	    try {
	    	if(getInputStream() != null) {
		        getInputStream().close();
	    	}
	    } finally {
	        super.finalize();
	    }
	}
	*/	
}
