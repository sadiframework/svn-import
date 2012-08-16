package org.sadiframework.utils;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import keggapi.KEGGLocator;
import keggapi.KEGGPortType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.elmonline.util.BatchIterator;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class KeggUtils 
{
	private static final Log log = LogFactory.getLog(KeggUtils.class);
	private static final Cache cache = getCache();

	public static final String PATHWAY_ID_PREFIX = "path:";
	public static final String COMPOUND_ID_PREFIX = "cpd:";

	/**
	 * The maximum number of IDs per request to the KEGG API.
	 */ 
	public static int MAX_IDS_PER_REQUEST = 100;
	
	/**
	 * This character sequence is used by KEGG to separate multiple 
	 * flat file records returned by a batch request.
	 */
	private final static String RECORD_SEPARATOR_REGEX = "///\\r?\\n"; 
	/**
	 * Extracts the ID from a KEGG record.
	 */
	private final static Pattern RECORD_ID_REGEX = Pattern.compile("ENTRY[ \t]+(\\S+)"); 

	private final static Pattern RECORD_SECTION_REGEX = Pattern.compile("(\\S+)[ \\t]+.*(\\r?\\n[ \\t]+.*)*");
	private final static Pattern LEADING_WHITESPACE_REGEX = Pattern.compile("^[ \t]+", Pattern.MULTILINE); 

	/**
	 * Retrieve string representations (i.e. flat files) of KEGG records 
	 * for the given IDs.  Identifers may be KEGG genes (e.g. hsa:7157), 
	 * KEGG pathways (e.g. path:hsa00232), KEGG compounds (e.g. cpd:C00791),
	 * etc. Note: All pathway IDs must be prefixed with "path:".
	 *
	 * @param ids the KEGG ids of the records to retrieve
	 * @return A map from the given ids to their corresponding records (in flat file
	 * form)
	 * @throws ServiceException if there is a problem initializing the KEGG API 
	 * @throws RemoteException if there is a problem contacting the KEGG API 
	 */
	public synchronized static Map<String,String> getKeggRecords(Collection<String> ids) throws ServiceException, RemoteException
	{
		Map<String,String> entries = new HashMap<String,String>(ids.size());
		List<String> uncachedIds = new ArrayList<String>(ids.size());
		for (String id : ids) {
			Element element = cache.get(id);
			if (element == null) {
				uncachedIds.add(id);
			} else {
				entries.put(id, (String)element.getObjectValue());
			}
		}
		
		if (!uncachedIds.isEmpty()) {
			
			log.debug(String.format("calling KEGG API to retrieve %d uncached records", uncachedIds.size()));
			KEGGPortType keggService = new KEGGLocator().getKEGGPort();
			
			for (Collection<String> batch: BatchIterator.batches(uncachedIds, MAX_IDS_PER_REQUEST)) {
			
				String idList = StringUtils.join(batch, " ");
				String[] records = keggService.bget(idList).split(RECORD_SEPARATOR_REGEX);
				
				for (String record : records) {
					
					Matcher idMatcher = RECORD_ID_REGEX.matcher(record);
					if(!idMatcher.find()) {
						log.error("excluding record from results, could not parse out ID");
					}

					/*
					 * Match the record to its ID.
					 * 
					 * We do this by parsing out the ID from the record. The ID that
					 * appears in the record is only the portion that appears after
					 * the colon (e.g. "7517" instead of "hsa:7157", "hsa00232" instead
					 * of "path:hsa00232", etc.)  However, this is not a problem 
					 * because that portion of the ID is unique across all KEGG records. 
					 */

					String idTail = String.format(":%s", idMatcher.group(1));

					for(String uncachedId : uncachedIds) {
						if(uncachedId.endsWith(idTail)) {
							entries.put(uncachedId, record);
							break;
						}
					}

				}

			}
		}
		
		return entries;
	}
	
	public static Map<String,String> getSectionsFromKeggRecord(String keggRecord) 
	{
		Map<String,String> sectionMap = new HashMap<String,String>();
		Matcher sectionMatcher = RECORD_SECTION_REGEX.matcher(keggRecord);
		
		while(sectionMatcher.find()) {
		
			String sectionLabel = sectionMatcher.group(1);
			
			/*
			 * TODO: We exclude REFERENCE sections because they are the
			 * only type of section that can occur more than once
			 * within a record. If we want to return REFERENCE sections,
			 * we should implement a separate method or return
			 * a Map<String,List<String>> instead.
			 */  
			if(sectionLabel.toUpperCase().equals("REFERENCE")) {
				continue;
			}
			
			/*
			 * Remove the section label and leading whitespace
			 * from each line in the section.
			 */
			String section = sectionMatcher.group();
			String sectionWithoutLabel = section.replaceFirst(sectionLabel, "");
			sectionWithoutLabel = LEADING_WHITESPACE_REGEX.matcher(sectionWithoutLabel).replaceAll("");
			
			sectionMap.put(sectionLabel, sectionWithoutLabel);
			
		}

		return sectionMap;
	}
	
	private static final Pattern EXTRACT_ORGANISM_CODE_REGEX = Pattern.compile("^[a-z]{3}");
	
	public static String getOrganismCodeFromPathwayId(String keggPathwayId) 
	{
		keggPathwayId = StringUtils.removeStart(keggPathwayId, "path:");
		
		Matcher organismMatcher = EXTRACT_ORGANISM_CODE_REGEX.matcher(keggPathwayId);
		if(!organismMatcher.find()) {
			return null;
		}
		
		return organismMatcher.group();
	}
	
	private static final String DBLINKS_RECORD_SECTION = "DBLINKS";
	
	public static Map<String,List<String>> getCrossReferences(String keggRecord) 
	{
		Map<String,List<String>> crossRefMap = new HashMap<String,List<String>>();
		Map<String,String> recordSections = KeggUtils.getSectionsFromKeggRecord(keggRecord);
		StrTokenizer tokenizer = new StrTokenizer();
		
		if(recordSections.containsKey(DBLINKS_RECORD_SECTION)) {
			
			for(String line : recordSections.get(DBLINKS_RECORD_SECTION).split("\\r?\\n")) {

				tokenizer.reset(line);
				
				if(!tokenizer.hasNext()) {
					continue;
				}

				String lineLabel = StringUtils.removeEnd(tokenizer.nextToken(), ":");

				if(!tokenizer.hasNext()) {
					continue;
				}
				
				List<String> crossRefs = new ArrayList<String>();
				while(tokenizer.hasNext()) {
					crossRefs.add(tokenizer.nextToken());
				}
				
				crossRefMap.put(lineLabel, crossRefs);
			}

		}
		
		return crossRefMap;
	}
	
	private static Cache getCache()
	{
		String cacheName = "KeggCache";
		CacheManager manager = CacheManager.create();
		Cache cache = new Cache(
				cacheName, // name
				500, // number of in-memory elements
				true, // overflow to disk
				false, // eternal
				14400, // time-to-live (in seconds)
				14400, // time-to-idle (in seconds)
				true, // disk-persistent
				14400 // disk-expiry interval (in seconds)
		);
		manager.addCache(cache);
		return manager.getCache(cacheName);
	}	
}
