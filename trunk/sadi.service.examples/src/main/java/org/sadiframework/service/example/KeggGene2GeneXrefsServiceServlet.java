package org.sadiframework.service.example;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.utils.KeggUtils;
import org.sadiframework.utils.LSRNUtils;
import org.sadiframework.vocab.LSRN;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/keggGene2GeneXrefs.input.1.rdf",
				output = "http://sadiframework.org/examples/t/keggGene2GeneXrefs.output.1.rdf"
		)
)
public class KeggGene2GeneXrefsServiceServlet extends KeggServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(KeggGene2GeneXrefsServiceServlet.class);

	// Note: This list is not comprehensive. KEGG has a separate record for a gene
	// in each species, and it covers hundreds of different species.

	protected static final Map<String, Resource> KEGG_XREF_TO_LSRN;
	static {
		Map<String, Resource> map = new HashMap<String, Resource>();
		map.put("DictyBase", LSRNUtils.getClass(LSRN.Namespace.DDB));
		map.put("EcoGene", LSRNUtils.getClass(LSRN.Namespace.ECOGENE));
		map.put("Ensembl", LSRNUtils.getClass(LSRN.Namespace.ENSEMBL));
		map.put("FlyBase", LSRNUtils.getClass(LSRN.Namespace.FLYBASE));
		map.put("HGNC", LSRNUtils.getClass(LSRN.Namespace.HGNC));
		map.put("MGI", LSRNUtils.getClass(LSRN.Namespace.MGI));
		map.put("NCBI-GeneID", LSRNUtils.getClass(LSRN.Namespace.ENTREZ_GENE));
		map.put("OMIM", LSRNUtils.getClass(LSRN.Namespace.OMIM));
		map.put("RGD", LSRNUtils.getClass(LSRN.Namespace.RGD));
		map.put("SGD", LSRNUtils.getClass(LSRN.Namespace.SGD));

		// TODO:
		//
		// => In LSRN: IDs for WormBase prefix are like WormBase:R13H7
		// => In KEGG: IDs for WormBase prefix are like WormBase:WBGene00001686
		//
		// To fix, add a new LSRN namespace called WormBase_GeneID and map to that instead.
		//
		//map.put("WormBase", LSRN.WormBase);

		map.put("ZFIN", LSRNUtils.getClass(LSRN.Namespace.ZFIN));
		KEGG_XREF_TO_LSRN = Collections.unmodifiableMap(map);
	}

	@Override
	protected Resource getInputLSRNIdentifierType() {
		return LSRNUtils.getIdentifierClass(LSRN.Namespace.KEGG_GENE);
	}

	@Override
	protected void processInput(String keggGeneId, String keggGeneRecord, Resource output)
	{
		Map<String,List<String>> xrefs = KeggUtils.getCrossReferences(keggGeneRecord);

		for (String xref : KEGG_XREF_TO_LSRN.keySet()) {
			if (xrefs.containsKey(xref)) {
				for(String id : xrefs.get(xref)) {
					Resource type = KEGG_XREF_TO_LSRN.get(xref);
					Resource geneRecordNode = LSRNUtils.createInstance(output.getModel(), type, id);
					output.addProperty(OWL.sameAs, geneRecordNode);
				}
			}
		}

	}

}
