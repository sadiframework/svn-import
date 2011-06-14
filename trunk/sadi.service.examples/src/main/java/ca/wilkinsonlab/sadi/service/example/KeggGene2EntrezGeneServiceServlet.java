package ca.wilkinsonlab.sadi.service.example;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.KeggUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.LSRN.LSRNRecordType;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

@SuppressWarnings("serial")
public class KeggGene2EntrezGeneServiceServlet extends KeggServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(KeggGene2EntrezGeneServiceServlet.class);
	private static final String ENTREZ_GENE_CROSSREFS_LABEL = "NCBI-GeneID";
	
	@Override
	protected LSRNRecordType getInputRecordType() {
		return LSRN.KEGG.Gene;
	}
	
	@Override
	protected void processInput(String keggGeneId, String keggGeneRecord, Resource output)
	{
		Map<String,List<String>> crossRefs = KeggUtils.getCrossReferences(keggGeneRecord);
		
		if(crossRefs.containsKey(ENTREZ_GENE_CROSSREFS_LABEL)) {
			for(String entrezGeneId : crossRefs.get(ENTREZ_GENE_CROSSREFS_LABEL)) {
				Resource entrezGeneNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.Entrez.Gene, entrezGeneId);
				output.addProperty(OWL.sameAs, entrezGeneNode);
			}
		}
	}

}
