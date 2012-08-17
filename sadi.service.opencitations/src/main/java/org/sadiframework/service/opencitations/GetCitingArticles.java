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
import org.sadiframework.utils.SPARQLStringUtils;
import org.sadiframework.vocab.SIO;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

@Name("Get citing articles")
@Description("Fetch a list of PubMed articles that cite the input PubMed article")
@ContactEmail("elmccarthy@gmail.com")
@InputClass("http://purl.oclc.org/SADI/LSRN/PMID_Record")
@OutputClass("http://sadiframework.org/ontologies/opencitations.owl#GetCitingArticlesOutputClass")
@TestCases({
	@TestCase(
			input = "/t/getCitingArticles-input.rdf", 
			output = "/t/getCitingArticles-output.rdf"
	)
})
public class GetCitingArticles extends OpenCitationsServiceServlet
{
	private static final Logger log = Logger.getLogger(GetCitingArticles.class);
	private static final long serialVersionUID = 1L;

	private static final String queryTemplate =
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
		"PREFIX cito: <http://purl.org/spar/cito/>\n" + 
		"PREFIX fabio: <http://purl.org/spar/fabio/>\n" + 
		"SELECT DISTINCT ?citingPubmedId\n" + 
		"WHERE { " +
		"  ?article cito:cites ?cites .\n" + 
		"  ?article fabio:hasPubMedId ?citingPubmedId .\n" +
		"  ?cites fabio:hasPubMedId %s% .\n" +
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
				RDFNode citedPubmedId = binding.get("citingPubmedId");
				if (citedPubmedId != null && citedPubmedId.isLiteral()) {
					Resource cited = LSRNUtils.createInstance(
							output.getModel(), Vocab.PMID_Record, 
							citedPubmedId.asLiteral().getLexicalForm());
					output.addProperty(SIO.is_cited_by, cited);
				}
			}
		}
	}

	private static final class Vocab
	{
		private static Model m_model = ModelFactory.createDefaultModel();
		
		public static final Resource PMID_Record = m_model.createResource("http://purl.oclc.org/SADI/LSRN/PMID_Record");
	}
}
