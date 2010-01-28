package ca.wilkinsonlab.sadi.utils.http;

import java.net.URL;
import java.util.Map;


public class GetRequest extends HttpRequest {

	public GetRequest(URL url, Map<String,String> params) {
		super(HTTP_METHOD_GET, url, params);
	}
	
	public GetRequest(URL url) {
		super(HTTP_METHOD_GET, url);
	}
	
}
