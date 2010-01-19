package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.pdb.Pdb;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

@SuppressWarnings("serial")
public class UniProt2PdbServiceServlet extends UniProtServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2PdbServiceServlet.class);
	
	private static final String OLD_PDB_PREFIX = "http://biordf.net/moby/PDB/";
	private static final String PDB_PREFIX = "http://lsrn.org/PDB:";
	
	private final Property hasPDBId = ResourceFactory.createProperty("http://sadiframework.org/ontologies/predicates.owl#has3DStructure");
	private final Resource PDB_Record = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/PDB_Record");
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (DatabaseCrossReference xref: input.getDatabaseCrossReferences(DatabaseType.PDB)) {
			attachPdbId(output, (Pdb)xref);
		}
	}
	
	private void attachPdbId(Resource uniprotNode, Pdb pdb)
	{
		Resource pdbNode = uniprotNode.getModel().createResource(getPdbUri(pdb), PDB_Record);
		pdbNode.addProperty(OWL.sameAs, uniprotNode.getModel().createResource(getOldPdbUri(pdb)));
		pdbNode.addProperty(RDFS.label, getPdbLabel(pdb));
		uniprotNode.addProperty(hasPDBId, pdbNode);
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

	private static String getPdbLabel(Pdb pdb)
	{
		return pdb.toString();
	}
}
