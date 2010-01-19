package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.go.Go;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

@SuppressWarnings("serial")
public class UniProt2GoServiceServlet extends UniProtServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2GoServiceServlet.class);
	
	private static final String OLD_GO_PREFIX = "http://biordf.net/moby/GO/";
	private static final String GO_PREFIX = "http://lsrn.org/GO:";
	
	private final Property hasGOTerm = ResourceFactory.createProperty("http://sadiframework.org/ontologies/predicates.owl#hasGOTerm");
	private final Resource GO_Record = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/GO_Record");
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (Go term: input.getGoTerms()) {
			attachGoTerm(output, term);
		}
	}
	
	private void attachGoTerm(Resource uniprotNode, Go goTerm)
	{
		Resource goNode = uniprotNode.getModel().createResource(getGoUri(goTerm), GO_Record);
		goNode.addProperty(OWL.sameAs, uniprotNode.getModel().createResource(getOldGoUri(goTerm)));
		goNode.addProperty(RDFS.label, getGoLabel(goTerm));
		uniprotNode.addProperty(hasGOTerm, goNode);
	}
	
	private static String getGoUri(Go goTerm)
	{
		String goId = goTerm.getGoId().getValue();
		if (goId.startsWith("GO:"))
			goId = goId.substring(3);
		return String.format("%s%s", GO_PREFIX, goId);
	}
	
	private static String getOldGoUri(Go goTerm)
	{
		String goId = goTerm.getGoId().getValue();
		if (goId.startsWith("GO:"))
			goId = goId.substring(3);
		return String.format("%s%s", OLD_GO_PREFIX, goId);
	}

	private static String getGoLabel(Go goTerm)
	{
		return goTerm.getGoTerm().getValue();
	}
}
