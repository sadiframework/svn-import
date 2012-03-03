package ca.wilkinsonlab.sadi.utils.blast;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.stringtree.util.StreamUtils;

public class NCBIBLASTClient extends NCBIClient
{
	static final Logger log = Logger.getLogger(NCBIBLASTClient.class);
	
	private String match(String s, Pattern regex)
	{
		Matcher matcher = regex.matcher(s);
		if (matcher.find())
			return matcher.group(1);
		else
			return null;
	}
	
	private String readStream(InputStream in) throws IOException
	{
		try {
			return StreamUtils.readStream(in);
		} finally {
			in.close();
		}
	}
	
	private static final Pattern REQUEST_ID = Pattern.compile("^    RID = (.*$)", Pattern.MULTILINE);
	private static final Pattern TIME_TO_COMPLETION = Pattern.compile("^    RTOE = (.*$)", Pattern.MULTILINE);
	private static final Pattern STATUS = Pattern.compile("\\s+Status=(\\w+)", Pattern.MULTILINE);
	public InputStream doBLAST(String program, String database, String query) throws Exception
	{
		// FIXME for each header in the query string, URI encode the header
		// then keep a map of the encoded headers and sub them back out in
		// the report that comes back (can we wrap the InputStream in a translating stream?)
		
		// initial request...
		String response = readStream(
			makeRequest("POST", "http://www.ncbi.nlm.nih.gov/blast/Blast.cgi", 
				"CMD", "Put", "PROGRAM", program, "DATABASE", database, "QUERY", query));//URLEncoder.encode(query, "UTF-8")));
		if (log.isTraceEnabled())
			log.trace(String.format("response to initial request\n%s", response));
		String requestID = match(response, REQUEST_ID);
		String ttc = match(response, TIME_TO_COMPLETION);
		if (StringUtils.isEmpty(requestID) || StringUtils.isEmpty(ttc))
			throw new Exception("BLAST request not accepted");
		
		// wait as suggested by NCBI...
		int timeToCompletion = Integer.valueOf(ttc);
		if (log.isDebugEnabled())
			log.debug(String.format("waiting %dss for task %s", timeToCompletion, requestID));
		try {
			Thread.sleep(timeToCompletion * 1000);
		} catch (InterruptedException e) {
			log.warn("interrupted", e);
		}
		
		// start polling for results...
		boolean done = false;
		do {
			/* we shouldn't have to wait before polling because the 
			 * makeRequest method will ensure we don't hit NCBI too often...
			 */
			response = readStream(
				makeRequest("GET", "http://www.ncbi.nlm.nih.gov/blast/Blast.cgi", 
					"CMD", "Get", "FORMAT_OBJECT", "SearchInfo", "RID", requestID));
			if (log.isTraceEnabled())
				log.trace(String.format("response to poll request\n%s", response));
			String status = match(response, STATUS);
			if (status.equals("WAITING")) {
				done = false;
			} else if (status.equals("FAILED")) {
				throw new RuntimeException("BLAST request failed");
			} else if (status.equals("UNKNOWN")) {
				throw new RuntimeException("BLAST request expired");
			} else if (status.equals("READY")) {
				done = true;
			}
		} while (!done);
		
		return makeRequest("GET", "http://www.ncbi.nlm.nih.gov/blast/Blast.cgi", 
				"CMD", "Get", "FORMAT_TYPE", "XML", "RID", requestID);
	}
}
