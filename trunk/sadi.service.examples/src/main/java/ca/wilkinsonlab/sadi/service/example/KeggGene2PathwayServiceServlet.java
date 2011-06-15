package ca.wilkinsonlab.sadi.service.example;

import java.util.Map;

import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.KeggUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;
import ca.wilkinsonlab.sadi.vocab.LSRN.LSRNRecordType;

import com.hp.hpl.jena.rdf.model.Resource;

@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/keggGene2Pathway-input.rdf", 
				output = "http://sadiframework.org/examples/t/keggGene2Pathway-output.rdf"
		)
)
public class KeggGene2PathwayServiceServlet extends KeggServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(KeggGene2PathwayServiceServlet.class);
	private static final String PATHWAY_RECORD_SECTION = "PATHWAY";

	@Override
	protected LSRNRecordType getInputRecordType() {
		return LSRN.KEGG.Gene;
	}
	
	@Override
	protected void processInput(String keggGeneId, String keggGeneRecord, Resource output)
	{
		Map<String,String> recordSections = KeggUtils.getSectionsFromKeggRecord(keggGeneRecord);
		StrTokenizer tokenizer = new StrTokenizer();
		
		if(recordSections.containsKey(PATHWAY_RECORD_SECTION)) {
			for(String line : recordSections.get(PATHWAY_RECORD_SECTION).split("\\r?\\n")) {
				String keggPathwayId = tokenizer.reset(line).nextToken(); 
				Resource keggPathwayNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.KEGG.Pathway, keggPathwayId);
				output.addProperty(SIO.is_participant_in, keggPathwayNode);
			}
		}
	}
	
}
