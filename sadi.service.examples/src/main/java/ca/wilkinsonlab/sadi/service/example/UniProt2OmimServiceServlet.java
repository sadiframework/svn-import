package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.mim.Mim;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDFS;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/uniprot2omim.input.1.rdf",
				output = "http://sadiframework.org/examples/t/uniprot2omim.output.1.rdf"
		)
)
public class UniProt2OmimServiceServlet extends UniProtServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2OmimServiceServlet.class);

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

		return omimNode;
	}

	private static String getOmimUri(Mim omim)
	{
		String omimId = omim.getMimAccessionNumber().getValue();
		return String.format("%s%s", OMIM_PREFIX, omimId);
	}

	private static String getLabel(Mim omim)
	{
		return omim.toString();
	}
}

