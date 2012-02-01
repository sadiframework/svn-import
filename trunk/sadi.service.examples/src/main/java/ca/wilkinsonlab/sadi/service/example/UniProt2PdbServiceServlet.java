package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.pdb.Pdb;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.Properties;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/uniprot2pdb-input.rdf", 
				output = "http://sadiframework.org/examples/t/uniprot2pdb-output.rdf"
		)
)
public class UniProt2PdbServiceServlet extends UniProtServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2PdbServiceServlet.class);
	
	private static final String OLD_PDB_PREFIX = "http://biordf.net/moby/PDB/";
	private static final String PDB_PREFIX = "http://lsrn.org/PDB:";
	
	private static final Resource PDB_Type = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/PDB_Record");
	private static final Resource PDB_Identifier = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/PDB_Identifier");
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (DatabaseCrossReference xref: input.getDatabaseCrossReferences(DatabaseType.PDB)) {
			Resource pdbNode = createPdbNode(output.getModel(), (Pdb)xref);
//			output.addProperty(SIO.has_attribute, pdbNode);
			output.addProperty(Properties.has3DStructure, pdbNode);
		}
	}

	private Resource createPdbNode(Model model, Pdb pdb)
	{
		Resource pdbNode = model.createResource(getPdbUri(pdb), PDB_Type);
		
		// add identifier structure...
		SIOUtils.createAttribute(pdbNode, PDB_Identifier, pdb.getPdbAccessionNumber().getValue());

		// add label...
		pdbNode.addProperty(RDFS.label, getLabel(pdb));
		
		// add relationship to old URI scheme...
		pdbNode.addProperty(OWL.sameAs, model.createResource(getOldPdbUri(pdb)));
		
		return pdbNode;
	}
	
	private static String getPdbUri(Pdb pdb)
	{
		String pdbId = pdb.getPdbAccessionNumber().getValue();
		return String.format("%s%s", PDB_PREFIX, pdbId);
	}
	
	private static String getOldPdbUri(Pdb pdb)
	{
		String pdbId = pdb.getPdbAccessionNumber().getValue();
		return String.format("%s%s", OLD_PDB_PREFIX, pdbId);
	}

	private static String getLabel(Pdb pdb)
	{
		return pdb.toString();
	}
}
