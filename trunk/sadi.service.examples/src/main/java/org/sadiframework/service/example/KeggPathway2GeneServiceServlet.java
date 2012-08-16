package org.sadiframework.service.example;

import java.util.Map;

import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.utils.KeggUtils;
import org.sadiframework.utils.ServiceUtils;
import org.sadiframework.vocab.LSRN;
import org.sadiframework.vocab.SIO;
import org.sadiframework.vocab.LSRN.LSRNRecordType;


import com.hp.hpl.jena.rdf.model.Resource;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/keggPathway2Gene.input.1.rdf",
				output = "http://sadiframework.org/examples/t/keggPathway2Gene.output.1.rdf"
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
