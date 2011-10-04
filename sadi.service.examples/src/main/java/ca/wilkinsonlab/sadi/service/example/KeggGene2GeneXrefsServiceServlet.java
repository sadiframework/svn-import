package ca.wilkinsonlab.sadi.service.example;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.KeggUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.LSRN.LSRNRecordType;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/keggGene2GeneXrefs-input.rdf", 
				output = "http://sadiframework.org/examples/t/keggGene2GeneXrefs-output.rdf"
		)
)
public class KeggGene2GeneXrefsServiceServlet extends KeggServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(KeggGene2GeneXrefsServiceServlet.class);

	// Note: This list is not comprehensive. KEGG has a separate record for a gene 
	// in each species, and it covers hundreds of different species.
	
	protected static final Map<String, LSRNRecordType> KEGG_XREF_TO_LSRN;
	static {
		Map<String, LSRNRecordType> map = new HashMap<String, LSRNRecordType>();
		map.put("DictyBase", LSRN.DDB);
		map.put("EcoGene", LSRN.EcoGene);
		map.put("Ensembl", LSRN.Ensembl);
		map.put("FlyBase", LSRN.FlyBase);
		map.put("HGNC", LSRN.HGNC);
		map.put("MGI", LSRN.MGI);
		map.put("NCBI-GeneID", LSRN.Entrez.Gene);
		map.put("OMIM", LSRN.OMIM);
		map.put("RGD", LSRN.RGD);
		map.put("SGD", LSRN.SGD);

		// TODO:
		//
		// => In LSRN: IDs for WormBase prefix are like WormBase:R13H7
		// => In KEGG: IDs for WormBase prefix are like WormBase:WBGene00001686
		// 
		// To fix, add a new LSRN namespace called WormBase_GeneID and map to that instead.
		//
		//map.put("WormBase", LSRN.WormBase);
		
		map.put("ZFIN", LSRN.ZFIN);
		KEGG_XREF_TO_LSRN = Collections.unmodifiableMap(map);
	}
	
	@Override
	protected LSRNRecordType getInputRecordType() {
		return LSRN.KEGG.Gene;
	}
	
	@Override
	protected void processInput(String keggGeneId, String keggGeneRecord, Resource output)
	{
		Map<String,List<String>> xrefs = KeggUtils.getCrossReferences(keggGeneRecord);

		for (String xref : KEGG_XREF_TO_LSRN.keySet()) {
			if (xrefs.containsKey(xref)) {
				for(String id : xrefs.get(xref)) {
					LSRNRecordType lsrnRecordType = KEGG_XREF_TO_LSRN.get(xref);
					Resource geneRecordNode = ServiceUtils.createLSRNRecordNode(output.getModel(), lsrnRecordType, id); 
					output.addProperty(OWL.sameAs, geneRecordNode);
				}
			}
		}
			
/*			
		if(crossRefs.containsKey(ENTREZ_GENE_CROSSREFS_LABEL)) {
			for(String entrezGeneId : crossRefs.get(ENTREZ_GENE_CROSSREFS_LABEL)) {
				Resource entrezGeneNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.Entrez.Gene, entrezGeneId);
				output.addProperty(OWL.sameAs, entrezGeneNode);
			}
		}
		
*/
	}

}
