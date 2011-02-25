package ca.wilkinsonlab.sadi.service.example;

import java.util.Map;

import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.KeggUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;
import ca.wilkinsonlab.sadi.vocab.LSRN.LSRNRecordType;

import com.hp.hpl.jena.rdf.model.Resource;

@SuppressWarnings("serial")
public class KeggPathway2CompoundServiceServlet extends KeggServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(KeggPathway2CompoundServiceServlet.class);
	private static final String COMPOUND_RECORD_SECTION = "COMPOUND";
	
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
		Map<String,String> recordSections = KeggUtils.getSectionsFromKeggRecord(keggPathwayRecord);
		StrTokenizer tokenizer = new StrTokenizer();
		
		if(recordSections.containsKey(COMPOUND_RECORD_SECTION)) {
			for(String line : recordSections.get(COMPOUND_RECORD_SECTION).split("\\r?\\n")) {
				String keggCompoundId = String.format("%s%s", KeggUtils.COMPOUND_ID_PREFIX, tokenizer.reset(line).nextToken());
				Resource keggCompoundNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.KEGG.Compound, keggCompoundId);
				output.addProperty(SIO.has_participant, keggCompoundNode);
			}
		}
	}
	
}
