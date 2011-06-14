package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.mim.Mim;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

@SuppressWarnings("serial")
public class UniProt2OmimServiceServlet extends UniProtServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2OmimServiceServlet.class);
	
	private static final String OLD_OMIM_PREFIX = "http://biordf.net/moby/OMIM/";
	private static final String OMIM_PREFIX = "http://lsrn.org/OMIM:";
	
	private static final Resource OMIM_Type = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/OMIM_Record");
	private static final Resource OMIM_Identifier = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/OMIM_Identifier");
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (DatabaseCrossReference xref: input.getDatabaseCrossReferences(DatabaseType.MIM)) {
			Resource omimNode = createOmimNode(output.getModel(), (Mim)xref);
			output.addProperty(SIO.is_causally_related_with, omimNode);
		}
	}

	private Resource createOmimNode(Model model, Mim omim)
	{
		Resource omimNode = model.createResource(getOmimUri(omim), OMIM_Type);
		
		// add identifier structure...
		SIOUtils.createAttribute(omimNode, OMIM_Identifier, omim.getMimAccessionNumber().getValue());

		// add label...
		omimNode.addProperty(RDFS.label, getLabel(omim));
		
		// add relationship to old URI scheme...
		omimNode.addProperty(OWL.sameAs, model.createResource(getOldOmimUri(omim)));
		
		return omimNode;
	}
	
	private static String getOmimUri(Mim omim)
	{
		String omimId = omim.getMimAccessionNumber().getValue();
		return String.format("%s%s", OMIM_PREFIX, omimId);
	}
	
	private static String getOldOmimUri(Mim omim)
	{
		String omimId = omim.getMimAccessionNumber().getValue();
		return String.format("%s%s", OLD_OMIM_PREFIX, omimId);
	}

	private static String getLabel(Mim omim)
	{
		return omim.toString();
	}
}

