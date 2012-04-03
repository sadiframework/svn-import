package ca.wilkinsonlab.sadi.service.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.uuw.services.remoting.EntryIterator;
import uk.ac.ebi.kraken.uuw.services.remoting.Query;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryBuilder;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;
import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceCall;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.LSRNUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.utils.UniProtUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;
import ca.wilkinsonlab.sadi.vocab.LSRN.LSRNRecordType;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/gene2uniprot.input.1.rdf",
				output = "http://sadiframework.org/examples/t/gene2uniprot.output.1.rdf"
		)
)
public class Gene2UniProtServiceServlet extends AsynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(Gene2UniProtServiceServlet.class);

	private static final Set<LSRNRecordType> inputLSRNTypes;
	static {
		Set<LSRNRecordType> set = new HashSet<LSRNRecordType>();
		set.add(LSRN.Ensembl);
		set.add(LSRN.FlyBase);
		set.add(LSRN.Entrez.Gene);
		set.add(LSRN.HGNC);
		set.add(LSRN.KEGG.Gene);
		set.add(LSRN.MGI);
		set.add(LSRN.RGD);
		set.add(LSRN.SGD);
		set.add(LSRN.ZFIN);
		inputLSRNTypes = Collections.synchronizedSet(Collections.unmodifiableSet(set));
	}

	private static final Map<DatabaseType, String> databaseTypeToQueryOp;
	static {
		Map<DatabaseType, String> map = new Hashtable<DatabaseType, String>();
		map.put(DatabaseType.ENSEMBL, "xref.ensembl");
		map.put(DatabaseType.FLYBASE, "xref.flybase");
		map.put(DatabaseType.GENEID, "xref.geneid");
		map.put(DatabaseType.HGNC, "xref.hgnc");
		map.put(DatabaseType.KEGG, "xref.kegg");
		map.put(DatabaseType.MGI, "xref.mgi");
		map.put(DatabaseType.RGD, "xref.rgd");
		map.put(DatabaseType.SGD, "xref.sgd");
		map.put(DatabaseType.ZFIN, "xref.zfin");
		databaseTypeToQueryOp = Collections.unmodifiableMap(map);
	}

	@Override
	protected Model createOutputModel()
	{
		Model model = super.createOutputModel();
		model.setNsPrefix("sio", "http://semanticscience.org/resource/");
		model.setNsPrefix("lsrn", "http://purl.oclc.org/SADI/LSRN/");
		model.setNsPrefix("sadi", "http://sadiframework.org/ontologies/properties.owl#");
		return model;
	}

	@Override
	public int getInputBatchSize()
	{
		/*
		 * There doesn't seem to be any official limit.
		 * However there's seems to be a practical limit on
		 * Lucene query size.  Querying with 1000 terms simultaneously
		 * causes server side exception, while 200 at a time seems to
		 * work fine.
		 */

		return 200;
	}

	@Override
	protected void processInputBatch(ServiceCall call)
	{

		Collection<Resource> inputNodes = call.getInputNodes();
		Model outputModel = call.getOutputModel();
		Set<String> visitedUniprotIds = new HashSet<String>();

		/*
		 * Build a Lucene query for all UniProt records
		 * that correspond to at least one input gene.
		 */

		StringBuilder luceneQuery = new StringBuilder();

		for (Resource inputNode : inputNodes) {
			for (Resource type : RdfUtils.getTypes(inputNode).toList()) {
				LSRNRecordType lsrnRecordType = getLSRNRecordType(type);
				if (lsrnRecordType == null || !inputLSRNTypes.contains(lsrnRecordType))
					continue;
				DatabaseType databaseType = UniProtUtils.getDatabaseType(lsrnRecordType);
				if (databaseType == null)
					continue;
				String queryOp = databaseTypeToQueryOp.get(databaseType);
				if (queryOp == null)
					continue;
				String id = ServiceUtils.getDatabaseId(inputNode, lsrnRecordType);
				if (id == null)
					continue;
				if (luceneQuery.length() > 0)
					luceneQuery.append(" OR ");
				luceneQuery.append(queryOp);
				luceneQuery.append(":");
				luceneQuery.append(id);
			}
		}

		if(luceneQuery.length() == 0) {
			log.warn("no valid inputs found for service");
			return;
		}

		/*
		 * Build a structure that will allow us to map the returned UniProt records
		 * to the correct input nodes. We use the canonical LSRN URIs (e.g.
		 * http://lsrn.org/GeneID:4567) as the keys of the map. An input gene may
		 * have multiple LSRN identifier attributes attached to it, and
		 * so an input node may have multiple keys in the map.
		 */

		Map<String, Resource> lsrnToOutputNode = new HashMap<String, Resource>();
		for (Resource inputNode : inputNodes) {
			for (String uri : getLSRNURIs(inputNode)) {
				LSRNRecordType lsrnRecordType = getLSRNRecordTypeForEntityURI(uri);
				if (lsrnRecordType == null || !inputLSRNTypes.contains(lsrnRecordType))
					continue;
				Resource outputNode = outputModel.getResource(inputNode.getURI());
				lsrnToOutputNode.put(uri, outputNode);
			}
		}

		/*
		 * Run the query and build the output.
		 */

		UniProtQueryService uniProtQueryService = UniProtJAPI.factory.getUniProtQueryService();
		Query query = UniProtQueryBuilder.buildQuery(luceneQuery.toString());
		log.info(String.format("submitting Lucene query to UniProt: '%s'", luceneQuery.toString()));
		EntryIterator<UniProtEntry> entryIterator = uniProtQueryService.getEntryIterator(query);

		for (UniProtEntry uniprotEntry : entryIterator) {
			String uniprotId = uniprotEntry.getPrimaryUniProtAccession().getValue();
			for (DatabaseCrossReference xref : uniprotEntry.getDatabaseCrossReferences()) {
				/* use the canonical LSRN URI to map back to correct output node */
				String lsrnURI = getLSRNURI(xref);
				if (lsrnURI == null)
					continue;
				Resource outputNode = lsrnToOutputNode.get(lsrnURI);
				if (outputNode == null)
					continue;
				Resource uniprotNode;
				/* keep track of uniprot IDs we've already added, to avoid creating duplicate blank nodes */
				if (!visitedUniprotIds.contains(uniprotId))
					LSRNUtils.createInstance(outputModel, LSRN.UniProt.getRecordTypeURI(), uniprotId);
				visitedUniprotIds.add(uniprotId);
				uniprotNode = outputModel.getResource(LSRNUtils.getURI(LSRN.UniProt.getNamespace(), uniprotId));
				log.debug(String.format("result: <%s> encodes <%s>", outputNode.getURI(), uniprotId));
				outputModel.add(outputNode, SIO.encodes, uniprotNode);
			}
		}

	}

	protected static LSRNRecordType getLSRNRecordType(Resource lsrnRecordTypeURI)
	{
		for (LSRNRecordType lsrnRecordType : inputLSRNTypes) {
			if(lsrnRecordType.getRecordTypeURI().equals(lsrnRecordTypeURI))
				return lsrnRecordType;
		}
		return null;
	}

	protected static LSRNRecordType getLSRNRecordTypeForEntityURI(String lsrnEntityURI)
	{
		String namespace = LSRNUtils.getNamespaceFromLSRNURI(lsrnEntityURI);
		if (namespace == null)
			return null;

		for (LSRNRecordType lsrnRecordType : inputLSRNTypes) {
			if (lsrnRecordType.getNamespace().equals(namespace))
				return lsrnRecordType;
		}

		return null;
	}

	protected static String getLSRNURI(DatabaseCrossReference xref)
	{
		DatabaseType databaseType = xref.getDatabase();

		LSRNRecordType lsrnRecordType = UniProtUtils.getLSRNType(databaseType);
		if (lsrnRecordType == null)
			return null;

		String namespace = LSRNUtils.getNamespaceFromLSRNTypeURI(lsrnRecordType.getRecordTypeURI().getURI());
		if (namespace == null)
			return null;

		String id = UniProtUtils.getDatabaseId(xref);
		if (id == null)
			return null;

		return LSRNUtils.getURI(namespace, id);
	}


	protected static Collection<String> getLSRNURIs(Resource lsrnNode)
	{
		Collection<String> uris = new ArrayList<String>();

		/*
		 * An input node is not required to have a URI that matches
		 * the LSRN form (i.e. http://lsrn.org/$NAMESPACE:$ID), but if it does,
		 * include that URI in the results.
		 *
		 * This makes the service forgiving in the case where the client
		 * provides only a bare LSRN URI as input, instead of a properly
		 * formed SIO/LSRN graph structure with attached identifier attribute(s).
		 */

		if (LSRNUtils.isLSRNURI(lsrnNode.getURI()))
			uris.add(lsrnNode.getURI());

		Collection<Resource> attribs = new ArrayList<Resource>();
		attribs.addAll(RdfUtils.getPropertyValues(lsrnNode, SIO.has_attribute, null).toList());
		attribs.addAll(RdfUtils.getPropertyValues(lsrnNode, SIO.has_identifier, null).toList());

		for (Resource attrib : attribs) {
			for (Resource type : RdfUtils.getTypes(attrib).toList()) {
				if (!type.isURIResource())
					continue;
				String namespace = LSRNUtils.getNamespaceFromLSRNIdentifierTypeURI(type.getURI());
				if (namespace == null)
					continue;
				Statement stmt = attrib.getProperty(SIO.has_value);
				if (stmt == null)
					continue;
				if (!stmt.getObject().isLiteral())
					continue;
				String id = stmt.getObject().asLiteral().getLexicalForm();
				String uri = LSRNUtils.getURI(namespace, id);
				if (uri == null)
					continue;
				uris.add(uri);
			}
		}

		return uris;
	}

}
