package org.sadiframework.service.opencitations;

import org.apache.log4j.Logger;

import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.Description;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.utils.LSRNUtils;
import org.sadiframework.utils.SIOUtils;
import org.sadiframework.utils.SPARQLStringUtils;
import org.sadiframework.vocab.SIO;

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
