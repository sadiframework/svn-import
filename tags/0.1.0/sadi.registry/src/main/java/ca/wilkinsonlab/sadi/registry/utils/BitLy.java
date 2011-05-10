package ca.wilkinsonlab.sadi.registry.utils;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.stringtree.json.JSONReader;

/**
 * Utility class to shorten URLs used to tweet service registrations.
 * @author Luke McCarthy
 */
public class BitLy
{
	private static final Logger log = Logger.getLogger(BitLy.class);
	
	private static String shortenURL = "http://api.bit.ly/v3/shorten";
	private static String login = "sadiframework";
	private static String apiKey = "R_686040b90c295ce340555f253837bb61";
	
	public static String getShortURL(String longURL) throws IOException
	{
		HttpClient client = new HttpClient();
		GetMethod get = new GetMethod(shortenURL);
		NameValuePair[] args = new NameValuePair[]{
				new NameValuePair("login", login),
				new NameValuePair("apiKey", apiKey),
				new NameValuePair("longUrl", longURL),
				new NameValuePair("format", "json")
		};
		get.setQueryString(args);
		int statusCode = client.executeMethod(get);
		if (statusCode != HttpStatus.SC_OK)
			throw new IOException(String.format("HTTP error %d from %s", statusCode, shortenURL));
		String json = get.getResponseBodyAsString();
		log.info(String.format("received response:\n%s", json));
		JSONReader reader = new JSONReader();
        Map<?, ?> topMap = (Map<?, ?>)reader.read(json);
        if (topMap == null)
        	throw new IOException(String.format("failed to parse JSON response\n\t%s", json));
        Map<?, ?> dataMap = (Map<?, ?>)topMap.get("data");
        if (dataMap == null)
        	throw new IOException(String.format("unexpected JSON response\n\t%s", json));
        return (String)dataMap.get("url");
	}
}
