import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;


public class SPARQLEndpointTest
{
	public static void main(String[] args) 
	{
		String query =
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX cito: <http://purl.org/spar/cito/>\n" + 
			"PREFIX fabio: <http://purl.org/spar/fabio/>\n" + 
			"PREFIX frbr: <http://purl.org/vocab/frbr/core#>\n" + 
			"SELECT DISTINCT ?article ?pmid ?abst\n" + 
			"WHERE { " +
			"  ?article cito:cites <http://opencitations.net/id/expression:pmid/12682359> .\n" + 
			"  ?article fabio:hasPubMedId ?pmid .\n" + 
			"  ?article frbr:part ?a .\n" + 
			"  ?a rdf:type fabio:Abstract .\n" + 
			"  ?a rdf:value ?abst .\n" + 
			"}";
		query =
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX fabio: <http://purl.org/spar/fabio/>\n" + 
			"PREFIX frbr: <http://purl.org/vocab/frbr/core#>\n" + 
			"SELECT DISTINCT ?article ?pmid ?abstractText\n" + 
			"WHERE { " +
			"  ?article frbr:part ?abstract .\n" + 
			"  ?article fabio:hasPubMedId ?pmid .\n" +
			"  ?abstract a fabio:Abstract .\n" + 
			"  ?abstract rdf:value ?abstractText .\n" + 
			"} LIMIT 100";
		QueryExecution qe = QueryExecutionFactory.sparqlService(
			"http://opencitations.net/sparql/",
			query);
		ResultSet resultSet = qe.execSelect();
		ResultSetFormatter.out(resultSet);
	}
}
