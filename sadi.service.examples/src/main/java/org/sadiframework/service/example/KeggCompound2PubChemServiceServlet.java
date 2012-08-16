package org.sadiframework.service.example;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.utils.KeggUtils;
import org.sadiframework.utils.ServiceUtils;
import org.sadiframework.vocab.LSRN;
import org.sadiframework.vocab.Properties;
import org.sadiframework.vocab.LSRN.LSRNRecordType;


import com.hp.hpl.jena.rdf.model.Resource;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/keggCompound2PubChem.input.1.rdf",
				output = "http://sadiframework.org/examples/t/keggCompound2PubChem.output.1.rdf"
		)
)
public class KeggCompound2PubChemServiceServlet extends KeggServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(KeggCompound2PubChemServiceServlet.class);
	private static final String PUBCHEM_CROSSREFS_LABEL = "PubChem";

	@Override
	protected LSRNRecordType getInputRecordType() {
		return LSRN.KEGG.Compound;
	}

	@Override
	protected void processInput(String keggCompoundId, String keggCompoundRecord, Resource output)
	{
		Map<String,List<String>> crossRefs = KeggUtils.getCrossReferences(keggCompoundRecord);

		if(crossRefs.containsKey(PUBCHEM_CROSSREFS_LABEL)) {
			for(String pubChemId : crossRefs.get(PUBCHEM_CROSSREFS_LABEL)) {
				Resource pubchemNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.PubChem.Substance, pubChemId);
				output.addProperty(Properties.isSubstance, pubchemNode);
			}
		}
	}

}
