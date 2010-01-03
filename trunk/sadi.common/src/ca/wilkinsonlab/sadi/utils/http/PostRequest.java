package ca.wilkinsonlab.sadi.utils.http;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public class PostRequest extends HttpRequest {

	static protected final String CONTENT_TYPE_URL_ENCODED_FORM = "application/x-www-form-urlencoded";

	String contentType; 
	InputStream inputStream;
	
	public PostRequest(URL url, InputStream inputStream, String contentType) {
		super(HTTP_METHOD_POST, url);
		setInputStream(inputStream);
		setContentType(contentType);
	}
	
	public PostRequest(URL url, Map<String,String> params) {
		super(HTTP_METHOD_POST, url, params);
		setContentType(CONTENT_TYPE_URL_ENCODED_FORM);
	}
	
	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	protected void finalize() throws Throwable {
	    try {
	    	if(getInputStream() != null) {
		        getInputStream().close();
	    	}
	    } finally {
	        super.finalize();
	    }
	}	

}
