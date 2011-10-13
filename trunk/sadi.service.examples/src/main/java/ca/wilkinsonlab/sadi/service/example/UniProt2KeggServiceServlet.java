package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.kegg.Kegg;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/uniprot2kegg-input.rdf", 
				output = "http://sadiframework.org/examples/t/uniprot2kegg-output.rdf"
		)
)
public class UniProt2KeggServiceServlet extends UniProtServiceServlet 
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2KeggServiceServlet.class);
	
	private static final String OLD_KEGG_PREFIX = "http://biordf.net/moby/KEGG/";
	private static final String KEGG_PREFIX = "http://lsrn.org/KEGG:";
	
	private static final Resource KEGG_Type = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/KEGG_Record");
	private static final Resource KEGG_Identifier = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/KEGG_Identifier");
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (DatabaseCrossReference xref: input.getDatabaseCrossReferences(DatabaseType.KEGG)) {
			Resource keggNode = createKeggNode(output.getModel(), (Kegg)xref);
			output.addProperty(SIO.is_encoded_by, keggNode);
		}
	}

	private Resource createKeggNode(Model model, Kegg kegg)
	{
		Resource keggNode = model.createResource(getKeggUri(kegg), KEGG_Type);
		
		// add identifier structure...
		SIOUtils.createAttribute(keggNode, KEGG_Identifier, kegg.getKeggAccessionNumber().getValue());

		// add label...
		keggNode.addProperty(RDFS.label, getLabel(kegg));
		
		// add relationship to old URI scheme...
		keggNode.addProperty(OWL.sameAs, model.createResource(getOldKeggUri(kegg)));
		
		return keggNode;
	}
	
	private static String getKeggUri(Kegg prosite)
	{
		String keggId = prosite.getKeggAccessionNumber().getValue();
		return String.format("%s%s", KEGG_PREFIX, keggId);
	}
	
	private static String getOldKeggUri(Kegg prosite)
	{
		String keggId = prosite.getKeggAccessionNumber().getValue();
		return String.format("%s%s", OLD_KEGG_PREFIX, keggId);
	}

	private static String getLabel(Kegg kegg)
	{
		return kegg.toString();
	}

}