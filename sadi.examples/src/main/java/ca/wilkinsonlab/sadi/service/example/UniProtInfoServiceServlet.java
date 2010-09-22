package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.common.Sequence;
import uk.ac.ebi.kraken.interfaces.uniprot.NcbiTaxonomyId;
import uk.ac.ebi.kraken.interfaces.uniprot.ProteinDescription;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.description.FieldType;
import uk.ac.ebi.kraken.interfaces.uniprot.description.Name;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.utils.UniProtUtils;
import ca.wilkinsonlab.sadi.vocab.Properties;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

@SuppressWarnings("serial")
public class UniProtInfoServiceServlet extends UniProtServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProtInfoServiceServlet.class);

	private static final Resource Taxon_Type = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/taxon_Record");
	private static final Resource Taxon_Identifier = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/taxon_Identifier");
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		ProteinDescription description = input.getProteinDescription();
		if (description != null) {
			if (description.hasRecommendedName()) {
				attachNames(output, description.getRecommendedName(), true);
			}
			if (description.hasAlternativeNames()) {
				for (Name name: description.getAlternativeNames()) {
					attachNames(output, name, false);
				}
			}
		}
		
		attachOrganism(output, input);
		
		Sequence sequence = input.getSequence();
		if (sequence != null) {
			attachSequence(output, sequence);
		}
	}

	private void attachNames(Resource uniprotNode, Name name, boolean preferred)
	{
//		Resource nameNode = SIOUtils.createAttribute(uniprotNode, preferred ? SIO.preferred_name : SIO.name, getFullName(name));
		Resource nameNode = SIOUtils.createAttribute(uniprotNode, Properties.hasName, preferred ? SIO.preferred_name : SIO.name, getFullName(name));
		String shortName = getShortName(name);
		if (shortName != null) {
//			Resource shortNameNode = SIOUtils.createAttribute(uniprotNode, SIO.name, shortName);
			Resource shortNameNode = SIOUtils.createAttribute(uniprotNode, Properties.hasName, shortName);
			shortNameNode.addProperty(SIO.is_variant_of, nameNode);
		}
	}
	
	private String getFullName(Name name)
	{
		return UniProtUtils.getFieldString(name.getFieldsByType(FieldType.FULL));
	}
	
	private String getShortName(Name name)
	{
		return UniProtUtils.getFieldString(name.getFieldsByType(FieldType.SHORT));
	}
	
	private void attachSequence(Resource uniprotNode, Sequence sequence)
	{
		String value = sequence.getValue();
		if (!StringUtils.isEmpty(value)) {
//			SIOUtils.createAttribute(uniprotNode, SIO.amino_acid_sequence, value);
			SIOUtils.createAttribute(uniprotNode, Properties.hasSequence, SIO.amino_acid_sequence, value);
		}
	}
	
	private void attachOrganism(Resource uniprotNode, UniProtEntry input)
	{
		for (NcbiTaxonomyId taxonId: input.getNcbiTaxonomyIds()) {
			Resource taxonNode = uniprotNode.getModel().createResource(Taxon_Type);
			SIOUtils.createAttribute(taxonNode, Taxon_Identifier, taxonId.getValue());
			SIOUtils.createAttribute(taxonNode, Properties.hasName, SIO.scientific_name, StringUtils.defaultString(getScientificName(input), "null"));
//			uniprotNode.addProperty(SIO.is_located_in, taxonNode);
			uniprotNode.addProperty(Properties.fromOrganism, taxonNode);
		}
	}
	
	private String getScientificName(UniProtEntry input)
	{
		try {
			return input.getOrganism().getScientificName().getValue();
		} catch (Exception e) {
			// probably NPE...
			return null;
		}
	}
}
