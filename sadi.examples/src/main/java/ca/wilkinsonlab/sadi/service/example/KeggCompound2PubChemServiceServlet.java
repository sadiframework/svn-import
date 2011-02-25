package ca.wilkinsonlab.sadi.service.example;

import java.util.Map;

import org.apache.commons.lang.text.StrTokenizer;
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
	private static final String DBLINKS_RECORD_SECTION = "DBLINKS";
	
	@Override
	protected LSRNRecordType getInputRecordType() {
		return LSRN.KEGG.Compound;
	}
	
	@Override
	protected void processInput(String keggGeneId, String keggGeneRecord, Resource output)
	{
		Map<String,String> recordSections = KeggUtils.getSectionsFromKeggRecord(keggGeneRecord);
		StrTokenizer tokenizer = new StrTokenizer();
		
		if(recordSections.containsKey(DBLINKS_RECORD_SECTION)) {
			for(String line : recordSections.get(DBLINKS_RECORD_SECTION).split("\\r?\\n")) {
				tokenizer.reset(line);
				if (tokenizer.nextToken().toLowerCase().equals("pubchem:")) {
					while(tokenizer.hasNext()) {
						Resource pubchemNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.PubChem.Substance, tokenizer.nextToken());
						output.addProperty(Properties.isSubstance, pubchemNode);
					}
					break;
				}
			}
		}
	}

}
