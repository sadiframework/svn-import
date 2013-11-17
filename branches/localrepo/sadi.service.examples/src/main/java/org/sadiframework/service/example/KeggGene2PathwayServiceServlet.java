package org.sadiframework.service.example;

import java.util.Map;

import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.utils.KeggUtils;
import org.sadiframework.utils.LSRNUtils;
import org.sadiframework.vocab.LSRN;
import org.sadiframework.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Resource;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/keggGene2Pathway.input.1.rdf",
				output = "http://sadiframework.org/examples/t/keggGene2Pathway.output.1.rdf"
		)
)
public class KeggGene2PathwayServiceServlet extends KeggServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(KeggGene2PathwayServiceServlet.class);
	private static final String PATHWAY_RECORD_SECTION = "PATHWAY";

	@Override
	protected Resource getInputLSRNIdentifierType() {
		return LSRNUtils.getIdentifierClass(LSRN.Namespace.KEGG_GENE);
	}

	@Override
	protected void processInput(String keggGeneId, String keggGeneRecord, Resource output)
	{
		Map<String,String> recordSections = KeggUtils.getSectionsFromKeggRecord(keggGeneRecord);
		StrTokenizer tokenizer = new StrTokenizer();

		if(recordSections.containsKey(PATHWAY_RECORD_SECTION)) {
			for(String line : recordSections.get(PATHWAY_RECORD_SECTION).split("\\r?\\n")) {
				String keggPathwayId = tokenizer.reset(line).nextToken();
				Resource keggPathwayNode = LSRNUtils.createInstance(output.getModel(), LSRNUtils.getClass(LSRN.Namespace.KEGG_PATHWAY), keggPathwayId);
				output.addProperty(SIO.is_participant_in, keggPathwayNode);
			}
		}
	}

}
