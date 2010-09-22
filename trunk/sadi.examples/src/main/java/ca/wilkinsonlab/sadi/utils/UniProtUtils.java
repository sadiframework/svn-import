package ca.wilkinsonlab.sadi.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtAccession;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.description.Field;
import uk.ac.ebi.kraken.uuw.services.remoting.Query;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryBuilder;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;
import ca.elmonline.util.BatchIterator;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class UniProtUtils
{
	private static final Log log = LogFactory.getLog(UniProtUtils.class);
	private static final Cache cache = getCache();
	
	public static final Resource UniProt_Identifier = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/UniProt_Identifier");
	
	/**
	 * The maximum number of IDs per request to the UniProt API.
	 */ 
	public static int MAX_IDS_PER_REQUEST = 1024;
	
	public static Pattern[] URI_PATTERNS = new Pattern[] {
		Pattern.compile("http://purl.uniprot.org/uniprot/([^\\s\\.\\?]*)"),
		Pattern.compile("http://www.uniprot.org/uniprot/([^\\s\\.\\?]*)"),
		Pattern.compile("http://biordf.net/moby/UniProt/(\\S*)"),
		Pattern.compile("http://lsrn.org/UniProt:(\\S*)"),
		Pattern.compile(".*[/:#]([^\\s\\.]*)") // failsafe best-guess pattern; don't remove this...
	};
	
	/**
	 * 
	 * @param fields
	 * @return
	 */
	public static final String getFieldString(List<Field> fields)
	{
		if (fields.isEmpty())
			return null;
		else if (fields.size() == 1)
			return fields.get(0).getValue();
		else
			return StringUtils.join(fields, ", ");
	}
	
	/**
	 * Returns the UniProt ID corresponding to the specified RDF
	 * node.  The node must have a URI.
	 * @param uniprotNode the RDF node
	 * @return the UniProt ID
	 * @throws IllegalArgumentException if the node does not have a URI
	 */
	public static String getUniProtId(Resource uniprotNode)
	{
		if (uniprotNode.getModel() != null) {
			for (Statement s: uniprotNode.listProperties(SIO.has_attribute).toList()) {
				RDFNode object = s.getObject();
				if (object.isResource()) {
					Resource attribute = object.as(Resource.class);
					if (attribute.hasProperty(RDF.type, UniProt_Identifier)) {
						if (attribute.hasProperty(SIO.has_value)) {
							return attribute.getProperty(SIO.has_value).getString();
						} else {
							log.warn("found 'has attribute' some 'UniProt_Identifier' missing 'has value'");
						}
					}
				}
			}
		}
		
		log.info("found input node with no explicit ID; attempting to parse URI");
		if (!uniprotNode.isURIResource())
			throw new IllegalArgumentException("resource has no explicit ID and no URI");
		
		String uri = uniprotNode.getURI();
		for (Pattern pattern: URI_PATTERNS) {
			Matcher matcher = pattern.matcher(uri);
			if (matcher.groupCount() < 1) {
				log.warn("URI pattern '%s' does not contain any capturing groups");
				continue;
			}
			if (matcher.find()) {
				String match = matcher.group(1);
				if (!StringUtils.isEmpty(match))
					return match;
			}
		}
		/* we shouldn't ever get here because the default best-guess pattern
		 * should always match...
		 */
		log.warn("failsafe URI pattern failed to match");
		return "";
	}

	/**
	 * Returns the UniProtEntries corresponding to the supplied UniProt IDs.
	 * Results are returned as a map from ID to Entry to get around the fact
	 * that multiple UniProt IDs can map to the same record.
	 * This isn't the most memory-efficient way to do this; it might be better
	 * at some point to implement an Iterator the way the UniProt API does.
	 * @param ids the UniProt ids of the records to retrieve
	 * @return the map of ID to UniProtEntry
	 */
	public synchronized static Map<String, UniProtEntry> getUniProtEntries(Collection<String> ids)
	{
		Map<String, UniProtEntry> entries = new HashMap<String, UniProtEntry>(ids.size());
		List<String> uncachedIds = new ArrayList<String>(ids.size());
		for (String id: ids) {
			Element element = cache.get(id);
			if (element == null) {
				uncachedIds.add(id);
			} else {
				entries.put(id, (UniProtEntry)element.getObjectValue());
			}
		}
		
		if (!uncachedIds.isEmpty()) {
			log.debug(String.format("calling UniProt query service on %d uncached UniProt records", uncachedIds.size()));
			UniProtQueryService uniProtQueryService = UniProtJAPI.factory.getUniProtQueryService();
			for (Collection<String> batch: BatchIterator.batches(uncachedIds, MAX_IDS_PER_REQUEST)) {
				log.trace("building UniProt query");
				Query query = UniProtQueryBuilder.buildIDListQuery((List<String>)batch);
				log.trace("executing UniProt query");
				for (UniProtEntry entry: uniProtQueryService.getEntryIterator(query)) {
					String id = entry.getPrimaryUniProtAccession().getValue();
					if (ids.contains(id)) {
						entries.put(id, entry);
					} else {
						for (UniProtAccession acc: entry.getSecondaryUniProtAccessions()) {
							id = acc.getValue();
							if (ids.contains(id))
								entries.put(id, entry);
						}
					}
				}
				log.trace("finished executing UniProt query");
			}
		}
		
		return entries;
	}
	
	private static Cache getCache()
	{
		String cacheName = "UniProtCache";
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
