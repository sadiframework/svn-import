package ca.wilkinsonlab.sadi.service.example;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.KeggUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.Properties;
import ca.wilkinsonlab.sadi.vocab.LSRN.LSRNRecordType;

import com.hp.hpl.jena.rdf.model.Resource;

@SuppressWarnings("serial")
public class KeggCompound2PubChemServiceServlet extends KeggServiceServlet
{
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
