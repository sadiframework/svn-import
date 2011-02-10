package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.geneid.GeneId;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

@SuppressWarnings("serial")
public class UniProt2EntrezGeneServiceServlet extends UniProtServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2EntrezGeneServiceServlet.class);
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (DatabaseCrossReference xref: input.getDatabaseCrossReferences(DatabaseType.GENEID)) {
			Resource entrezGeneNode = createEntrezGeneNode(output.getModel(), (GeneId)xref);
			output.addProperty(SIO.is_encoded_by, entrezGeneNode);
		}
	}

	private Resource createEntrezGeneNode(Model model, GeneId entrezGene)
	{
		Resource entrezGeneNode = model.createResource(getEntrezGeneUri(entrezGene), LSRN.EntrezGene.ENTREZ_GENE_TYPE);
		
		// add identifier structure...
		SIOUtils.createAttribute(entrezGeneNode, LSRN.EntrezGene.ENTREZ_GENE_IDENTIFIER, entrezGene.getGeneIdAccessionNumber().getValue());

		// add label...
		entrezGeneNode.addProperty(RDFS.label, getLabel(entrezGene));
		
		// add relationship to old URI scheme...
		entrezGeneNode.addProperty(OWL.sameAs, model.createResource(getOldEntrezGeneUri(entrezGene)));
		
		return entrezGeneNode;
	}
	
	private static String getEntrezGeneUri(GeneId entrezGene)
	{
		String entrezGeneId = entrezGene.getGeneIdAccessionNumber().getValue();
		return String.format("%s%s", LSRN.EntrezGene.ENTREZ_GENE_PREFIX, entrezGeneId);
	}
	
	private static String getOldEntrezGeneUri(GeneId entrezGene)
	{
		String entrezGeneId = entrezGene.getGeneIdAccessionNumber().getValue();
		return String.format("%s%s", LSRN.EntrezGene.OLD_ENTREZ_GENE_PREFIX, entrezGeneId);
	}

	private static String getLabel(GeneId entrezGene)
	{
		return entrezGene.toString();
	}
}
