package org.sadiframework.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;

import org.sadiframework.utils.http.HttpUtils;

public class UniProtIdMapperClient {

	private static final Log log = LogFactory.getLog(UniProtIdMapperClient.class);
	private static final String UNIPROT_MAPPING_SERVICE_URL = "http://www.uniprot.org/mapping/";

	static public Map<String,String> invoke(String inputUniprotNamespace, String outputUniprotNamespace, Collection<String> inputIds) throws IOException
	{
		/*
		 * Note: The protocol for interacting with the UniProt ID mapping service is as follows:
		 *
		 * 1) HTTP POST the input namespace, output namespace, and input IDs to http://www.uniprot.org/mapping/
		 * 2) receive HTTP 302 redirect to results URL (e.g. http://www.uniprot.org/jobs/2012081860JV2385SP.tab)
		 * 3) HTTP GET on results URL (e.g. http://www.uniprot.org/jobs/2012081860JV2385SP.tab)
		 *
		 * In spite of appearances to the contrary, the ID mapping service is *not* asynchronous.
		 * There is no polling involved here.  The response to the initial POST request (i.e. the redirect) is
		 * not returned until all of the input IDs have been processed and the full results file has been created.
		 * For this reason, UniProt advises against sending extremely large sets of input IDs (> 100,000) to
		 * the service, as the POST request will just time out.
		 */

		Map<String,String> mappings = new HashMap<String,String>();

 		// POST request

		Map<String,String> params = new HashMap<String,String>();
		params.put("from", inputUniprotNamespace);
		params.put("to", outputUniprotNamespace);
		params.put("format", "tab");
		params.put("query", StringUtils.join(inputIds, ','));
		log.info(String.format("posting %d input %s IDs to %s", inputIds.size(), inputUniprotNamespace, UNIPROT_MAPPING_SERVICE_URL));
		HttpResponse response = HttpUtils.POST(new URL(UNIPROT_MAPPING_SERVICE_URL), params);

		// 302 redirect to result URL

		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 302)
			throw new RuntimeException(String.format("unexpected response from UniProt ID mapping service: %s", response.getStatusLine()));
		Header locationHeader = response.getFirstHeader("Location");
		if (locationHeader == null)
			throw new RuntimeException(new HttpException("no Location header in HTTP 302 redirect!"));
		URL url = new URL(locationHeader.getValue());
		log.info(String.format("retrieving result from %s", url));
		response = HttpUtils.GET(url);
		statusCode = response.getStatusLine().getStatusCode();
		if (HttpUtils.isHttpError(statusCode) || statusCode != 200)
			throw new RuntimeException(String.format("unexpected response from UniProt ID mapping service: %s", response.getStatusLine()));

		// parse result document (tab-separated table with 2 cols)

		log.info("parsing response");
		Header encodingHeader = response.getEntity().getContentEncoding();
		String encoding = (encodingHeader == null) ? "ISO-8859-1" : encodingHeader.getValue();
		BufferedReader mapping = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), encoding));
		String line;
		boolean seenHeaderLine = false;
		while ((line = mapping.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty()) // skip blank lines
				continue;
			String cols[] = StringUtils.split(line);
			if (cols.length != 2)
				throw new RuntimeException(String.format("error parsing output from UniProt ID mapping service, line does not have 2 columns!: %s", line));
			if (!seenHeaderLine) {
				seenHeaderLine = true;
				continue;
			}
			mappings.put(cols[0], cols[1]);
		}

		return mappings;
	}

}
