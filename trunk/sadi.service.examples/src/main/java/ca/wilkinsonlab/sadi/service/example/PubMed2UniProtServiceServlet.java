package ca.wilkinsonlab.sadi.service.example;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.service.simple.SimpleAsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

@URI("http://sadiframework.org/examples/pubmed2uniprot")
@Name("PubMed2UniProt")
@Description("Maps PubMed IDs to UniProt proteins")
@ContactEmail("info@sadiframework.org")
@InputClass("http://purl.oclc.org/SADI/LSRN/PMID_Record")
@OutputClass("http://sadiframework.org/examples/pubmed2uniprot.owl#OutputClass")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/pubmed2uniprot.input.1.rdf", 
				output = "http://sadiframework.org/examples/t/pubmed2uniprot.output.1.rdf"
		)
)
public class PubMed2UniProtServiceServlet extends SimpleAsynchronousServiceServlet 
{
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(PubMed2UniProtServiceServlet.class);
	private static final Property is_reference_for = ResourceFactory.createProperty(SIO.NS, "SIO_000252");
	
	@Override
	public void processInput(Resource input, Resource output) 
	{
		String pubmedID = ServiceUtils.getDatabaseId(input, LSRN.PMID);
		if (pubmedID == null) {
			log.warn(String.format("unable to determine PubMed ID for %s", input));
			return;
		}

		String bio2rdfURL = String.format("http://bio2rdf.org/citations:%s", pubmedID);
		Model bio2rdfModel = ModelFactory.createDefaultModel();
		bio2rdfModel.read(bio2rdfURL);
		Resource bio2rdfNode = bio2rdfModel.getResource(bio2rdfURL);
		Property linkedToFrom = bio2rdfModel.getProperty("http://bio2rdf.org/bio2rdf_resource:linkedToFrom");
		Pattern uniprotPattern = Pattern.compile("^http://bio2rdf.org/uniprot:(\\w+)");
		for (Iterator<Statement> i = bio2rdfNode.listProperties(linkedToFrom); i.hasNext(); ) {
			RDFNode o = i.next().getObject();
			if (o.canAs(Resource.class)) {
				String uniprotURI = o.as(Resource.class).getURI();
				Matcher matcher = uniprotPattern.matcher(uniprotURI);
				if (matcher.matches()) {
					String uniprotID = matcher.group(1);
			    	Resource uniprotNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.UniProt, uniprotID);
			    	output.addProperty(is_reference_for, uniprotNode);
				}
			} else {
				log.warn(String.format("skipping non-resource object %s", o));
			}
		}
	}
}
