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
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

@Name("Get cited articles")
@Description("Fetch a list of PubMed articles that are cited by the input PubMed article")
@ContactEmail("elmccarthy@gmail.com")
@InputClass("http://purl.oclc.org/SADI/LSRN/PMID_Record")
@OutputClass("http://sadiframework.org/ontologies/opencitations.owl#GetCitedArticlesOutputClass")
@TestCases({
	@TestCase(
			input = "/t/getCitedArticles-input.rdf", 
			output = "/t/getCitedArticles-output.rdf"
	)
})
public class GetCitedArticles extends OpenCitationsServiceServlet
{
	private static final Logger log = Logger.getLogger(GetCitedArticles.class);
	private static final long serialVersionUID = 1L;

	private static final String queryTemplate =
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
		"PREFIX cito: <http://purl.org/spar/cito/>\n" + 
		"PREFIX fabio: <http://purl.org/spar/fabio/>\n" + 
		"SELECT DISTINCT ?citedPubmedId\n" + 
		"WHERE { " +
		"  ?article cito:cites ?cites .\n" + 
		"  ?article fabio:hasPubMedId %s% .\n" +
		"  ?cites fabio:hasPubMedId ?citedPubmedId .\n" +
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
				RDFNode citedPubmedId = binding.get("citedPubmedId");
				if (citedPubmedId != null && citedPubmedId.isLiteral()) {
					Resource cited = LSRNUtils.createInstance(
							output.getModel(), Vocab.PMID_Record, 
							citedPubmedId.asLiteral().getLexicalForm());
					output.addProperty(SIO.cites, cited);
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
