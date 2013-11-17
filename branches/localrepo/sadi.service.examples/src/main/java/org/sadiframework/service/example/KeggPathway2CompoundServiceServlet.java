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
				input = "http://sadiframework.org/examples/t/keggPathway2Compound.input.1.rdf",
				output = "http://sadiframework.org/examples/t/keggPathway2Compound.output.1.rdf"
		)
)
public class KeggPathway2CompoundServiceServlet extends KeggServiceServlet
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(KeggPathway2CompoundServiceServlet.class);
	private static final String COMPOUND_RECORD_SECTION = "COMPOUND";

	@Override
	protected Resource getInputLSRNIdentifierType() {
		return LSRNUtils.getIdentifierClass(LSRN.Namespace.KEGG_PATHWAY);
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
				Resource keggCompoundNode = LSRNUtils.createInstance(output.getModel(), LSRNUtils.getClass(LSRN.Namespace.KEGG_COMPOUND), keggCompoundId);
				output.addProperty(SIO.has_participant, keggCompoundNode);
			}
		}
	}

}
