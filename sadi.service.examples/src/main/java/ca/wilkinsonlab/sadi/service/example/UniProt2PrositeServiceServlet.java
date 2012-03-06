package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.prosite.Prosite;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.Properties;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDFS;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/uniprot2prosite-input.rdf", 
				output = "http://sadiframework.org/examples/t/uniprot2prosite-output.rdf"
		)
)
public class UniProt2PrositeServiceServlet extends UniProtServiceServlet 
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2PrositeServiceServlet.class);
	
	private static final String PROSITE_PREFIX = "http://lsrn.org/Prosite:";
	private static final Resource PROSITE_Type = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/Prosite_Record");
	private static final Resource PROSITE_Identifier = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/Prosite_Identifier");
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (DatabaseCrossReference xref: input.getDatabaseCrossReferences(DatabaseType.PROSITE)) {
			Resource prositeNode = createPrositeNode(output.getModel(), (Prosite)xref);
			output.addProperty(Properties.hasMotif, prositeNode);
		}
	}

	private Resource createPrositeNode(Model model, Prosite prosite)
	{
		Resource prositeNode = model.createResource(getPrositeUri(prosite), PROSITE_Type);
		
		// add identifier structure...
		SIOUtils.createAttribute(prositeNode, PROSITE_Identifier, prosite.getPrositeAccessionNumber().getValue());

		// add label...
		prositeNode.addProperty(RDFS.label, getLabel(prosite));
		
		return prositeNode;
	}
	
	private static String getPrositeUri(Prosite prosite)
	{
		String prositeId = prosite.getPrositeAccessionNumber().getValue();
		return String.format("%s%s", PROSITE_PREFIX, prositeId);
	}

	private static String getLabel(Prosite prosite)
	{
		return prosite.toString();
	}
}
