package org.sadiframework.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.collections.bidimap.UnmodifiableBidiMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.vocab.LSRN;
import org.sadiframework.vocab.LSRN.LSRNRecordType;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtAccession;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.ensembl.Ensembl;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.flybase.FlyBase;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.geneid.GeneId;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.hgnc.Hgnc;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.kegg.Kegg;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.mgi.Mgi;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.rgd.Rgd;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.sgd.Sgd;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.zfin.Zfin;
import uk.ac.ebi.kraken.interfaces.uniprot.description.Field;
import uk.ac.ebi.kraken.uuw.services.remoting.Query;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryBuilder;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;
import ca.elmonline.util.BatchIterator;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

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
		Pattern.compile("http://lsrn.org/UniProt:(\\S*)"),
		Pattern.compile(".*[/:#]([^\\s\\.]*)") // failsafe best-guess pattern; don't remove this...
	};

	/**
	 * Mappings between UniProt crossreferences and LSRN record types.
	 * This list of mappings is not exhaustive; more database types may be
	 * added as necessary.
	 */
	private static final BidiMap databaseTypeToLSRNType;
	static {
		BidiMap map = new DualHashBidiMap();
		map.put(DatabaseType.ENSEMBL, LSRN.Ensembl);
		map.put(DatabaseType.FLYBASE, LSRN.FlyBase);
		map.put(DatabaseType.GENEID, LSRN.Entrez.Gene);
		map.put(DatabaseType.HGNC, LSRN.HGNC);
		map.put(DatabaseType.KEGG, LSRN.KEGG.Gene);
		map.put(DatabaseType.MGI, LSRN.MGI);
		map.put(DatabaseType.RGD, LSRN.RGD);
		map.put(DatabaseType.SGD, LSRN.SGD);
		map.put(DatabaseType.ZFIN, LSRN.ZFIN);
		databaseTypeToLSRNType = UnmodifiableBidiMap.decorate(map);
	}

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
		String id = ServiceUtils.getDatabaseId(uniprotNode, LSRN.UniProt);

		if(id == null) {
			log.warn("failsafe URI pattern failed to match");
			id = "";
		}

		return id;
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

	public static LSRNRecordType getLSRNType(DatabaseType databaseType)
	{
		return (LSRNRecordType)databaseTypeToLSRNType.get(databaseType);
	}

	public static DatabaseType getDatabaseType(LSRNRecordType lsrnType)
	{
		return (DatabaseType)databaseTypeToLSRNType.inverseBidiMap().get(lsrnType);
	}

	public static String getDatabaseId(DatabaseCrossReference xref)
	{
		DatabaseType databaseType = xref.getDatabase();
		String id;

		switch (databaseType) {
		case ENSEMBL:
			id = ((Ensembl)xref).getEnsemblGeneIdentifier().getValue();
			break;
		case FLYBASE:
			id = ((FlyBase)xref).getFlyBaseAccessionNumber().getValue();
			break;
		case GENEID:
			id = ((GeneId)xref).getGeneIdAccessionNumber().getValue();
			break;
		case HGNC:
			id = ((Hgnc)xref).getHgncAccessionNumber().getValue();
			id = StringUtils.removeStart(id, "HGNC:");
			break;
		case KEGG:
			id = ((Kegg)xref).getKeggAccessionNumber().getValue();
			break;
		case MGI:
			id = ((Mgi)xref).getMgiAccessionNumber().getValue();
			id = StringUtils.removeStart(id, "MGI:");
			break;
		case RGD:
			id = ((Rgd)xref).getRgdAccessionNumber().getValue();
			break;
		case SGD:
			id = ((Sgd)xref).getSgdAccessionNumber().getValue();
			break;
		case ZFIN:
			id = ((Zfin)xref).getZfinAccessionNumber().getValue();
			break;
		default:
			id = null;
			break;
		}

		if (id == null)
			return null;

		return id;
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
