package ca.wilkinsonlab.sadi.utils.blast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.utils.http.HttpUtils;

public abstract class NCBIClient
{
	private static final Logger log = Logger.getLogger(NCBIClient.class);
	
	private static volatile long lastRequest = System.currentTimeMillis();
	
	private static Map<String, String> params(String... args)
	{
		if (args.length % 2 != 0)
			throw new IllegalArgumentException("odd number of arguments to params");
		
		Map<String, String> params = new HashMap<String, String>();
		for (int i=0; i<args.length; )
			params.put(args[i++], args[i++]);
		return params;
	}
	
	protected static synchronized InputStream makeRequest(String method, String url, String... params)
	throws IOException
	{
		long timeToWait = lastRequest + 2000 - System.currentTimeMillis();
		if (timeToWait > 0) {
			if (log.isDebugEnabled())
				log.debug(String.format("waiting %dms before hitting NCBI again", timeToWait));
			try {
				Thread.sleep(timeToWait);
			} catch (InterruptedException e) {
				log.error("interrupted", e);
			}
		}
		if (log.isDebugEnabled())
			log.debug(String.format("sending %s request to %s: %s", method, url, Arrays.toString(params)));
		HttpResponse response = method.equalsIgnoreCase("POST") ?
				HttpUtils.POST(new URL(url), params(params)) :
				HttpUtils.GET(new URL(url), params(params));
		return response.getEntity().getContent();
	}
}