package ca.wilkinsonlab.sadi.service.example;

import java.util.Map;

import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.KeggUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;
import ca.wilkinsonlab.sadi.vocab.LSRN.LSRNRecordType;

import com.hp.hpl.jena.rdf.model.Resource;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/keggPathway2Gene-input.rdf", 
				output = "http://sadiframework.org/examples/t/keggPathway2Gene-output.rdf"
		)
)
public class KeggPathway2GeneServiceServlet extends KeggServiceServlet
{
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(KeggPathway2GeneServiceServlet.class);
	private static final String GENE_RECORD_SECTION = "GENE";
	
	@Override
	protected LSRNRecordType getInputRecordType() {
		return LSRN.KEGG.Pathway;
	}
	
	@Override 
	protected String getInputIdPrefix() {
		return KeggUtils.PATHWAY_ID_PREFIX;
	}
	
	@Override
	protected void processInput(String keggPathwayId, String keggPathwayRecord, Resource output)
	{
		// organism-independent meta-pathways have identifiers starting with "ko"
		
		if(keggPathwayId.startsWith("ko")) {
			log.warn(String.format("skipping input pathway id %s, this service only works for organism specific pathways (e.g. hsa00010)", keggPathwayId));
		}
		
		String organismCode = KeggUtils.getOrganismCodeFromPathwayId(keggPathwayId); 
		if(organismCode == null) {
			log.warn(String.format("skipping input pathway id %s, unable to determine organism code for pathway", keggPathwayId));
			return;
		}

		Map<String,String> recordSections = KeggUtils.getSectionsFromKeggRecord(keggPathwayRecord);
		StrTokenizer tokenizer = new StrTokenizer();
		
		if(recordSections.containsKey(GENE_RECORD_SECTION)) {
			for(String line : recordSections.get(GENE_RECORD_SECTION).split("\\r?\\n")) {
				String keggGeneId = String.format("%s:%s", organismCode, tokenizer.reset(line).nextToken());
				Resource keggGeneNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.KEGG.Gene, keggGeneId);
				output.addProperty(SIO.has_participant, keggGeneNode);
			}
		}
	}

}
