package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.geneid.GeneId;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/uniprot2EntrezGene-input.rdf", 
				output = "http://sadiframework.org/examples/t/uniprot2EntrezGene.output.1.rdf"
		)
)
public class UniProt2EntrezGeneServiceServlet extends UniProtServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2EntrezGeneServiceServlet.class);
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (DatabaseCrossReference xref: input.getDatabaseCrossReferences(DatabaseType.GENEID)) {

			GeneId entrezGene = (GeneId)xref;
			String entrezGeneId = entrezGene.getGeneIdAccessionNumber().getValue();
			
			Resource entrezGeneNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.Entrez.Gene, entrezGeneId);
			entrezGeneNode.addProperty(RDFS.label, entrezGene.toString());
			
			output.addProperty(SIO.is_encoded_by, entrezGeneNode);

		}
	}
}
