package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.go.Go;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.go.OntologyType;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/uniprot2go-input.rdf", 
				output = "http://sadiframework.org/examples/t/uniprot2go.output.1.rdf"
		)
)
public class UniProt2GoServiceServlet extends UniProtServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2GoServiceServlet.class);
	
	private static final String OLD_GO_PREFIX = "http://biordf.net/moby/GO/";
	private static final String GO_PREFIX = "http://lsrn.org/GO:";
	private static final Resource GO_Type = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/GO_Record");
	private static final Resource GO_Identifier = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/GO_Identifier");
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (Go goTerm: input.getGoTerms()) {
			Resource goNode = createGoNode(output.getModel(), goTerm);
			Property p = getRelationalProperty(goTerm);
			output.addProperty(p, goNode);
		}
	}
	
	private Resource createGoNode(Model model, Go goTerm)
	{
		Resource goNode = model.createResource(getGoUri(goTerm), GO_Type);
		
		// add identifier structure...
		SIOUtils.createAttribute(goNode, GO_Identifier, goTerm.getGoId().getValue());
		
		// add label...
		goNode.addProperty(RDFS.label, getLabel(goTerm));
		
		// add relationship to old URI scheme...
		goNode.addProperty(OWL.sameAs, model.createResource(getOldGoUri(goTerm)));
		
		return goNode;
	}
	
	private Property getRelationalProperty(Go goTerm)
	{
		OntologyType goOntology = goTerm.getOntologyType();
		if (goOntology.equals(OntologyType.P)) { // biological process
			return SIO.is_participant_in;
		} else if (goOntology.equals(OntologyType.C)) { // cellular component
			return SIO.is_located_in;
		} else if (goOntology.equals(OntologyType.F)) { // molecular function
			return SIO.has_function;
		} else {
			return SIO.is_related_to;
		}
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

	private static String getLabel(Go goTerm)
	{
		return goTerm.getGoTerm().getValue();
	}
}
