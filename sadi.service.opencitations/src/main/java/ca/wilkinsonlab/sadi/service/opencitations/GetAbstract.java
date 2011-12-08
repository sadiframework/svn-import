package ca.wilkinsonlab.sadi.service.opencitations;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.LSRNUtils;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

@Name("Get PubMed abstract")
@Description("Fetch the abstract of a PubMed document as plain text")
@ContactEmail("elmccarthy@gmail.com")
@InputClass("http://purl.oclc.org/SADI/LSRN/PMID_Record")
@OutputClass("http://sadiframework.org/ontologies/opencitations.owl#GetAbstractOutputClass")
@TestCases({
	@TestCase(
			input = "/t/getAbstract-input.rdf", 
			output = "/t/getAbstract-output.rdf"
	)
})
public class GetAbstract extends OpenCitationsServiceServlet
{
	private static final Logger log = Logger.getLogger(GetAbstract.class);
	private static final long serialVersionUID = 1L;

	private static final String queryTemplate =
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
		"PREFIX fabio: <http://purl.org/spar/fabio/>\n" + 
		"PREFIX frbr: <http://purl.org/vocab/frbr/core#>\n" + 
		"SELECT DISTINCT ?article ?pmid ?abstractText\n" + 
		"WHERE { " +
		"  ?article frbr:part ?abstract .\n" + 
		"  ?article fabio:hasPubMedId %s% .\n" +
		"  ?abstract a fabio:Abstract .\n" + 
		"  ?abstract rdf:value ?abstractText .\n" + 
		"}";
	
	@Override
	public void processInput(Resource input, Resource output)
	{
		String pmid = LSRNUtils.getID(input);
		if (pmid != null) {
			String query = SPARQLStringUtils.strFromTemplate(queryTemplate, pmid);
			log.debug(String.format("sending query to %s:\n%s", SPARQL_ENDPOINT, query));
			QueryExecution qe = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query);
			ResultSet resultSet = qe.execSelect();
			while (resultSet.hasNext()) {
				QuerySolution binding = resultSet.nextSolution();
				RDFNode abstractText = binding.get("abstractText");
				if (abstractText != null) {
					SIOUtils.createAttribute(output, SIO.has_part, SIO._abstract,
							abstractText.asLiteral().getLexicalForm());
				}
			}
		}
	}
}
