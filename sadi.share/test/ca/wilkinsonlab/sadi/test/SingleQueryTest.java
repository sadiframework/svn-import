package ca.wilkinsonlab.sadi.test;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.StopWatch;

import ca.wilkinsonlab.sadi.share.SHAREQueryClient;

import com.hp.hpl.jena.ontology.OntDocumentManager;

/**
 * This class executes a single example query and reports the results.
 * Use this for tracking down problems in specific queries, since Eclipse
 * apparently runs all JUnit tests even if you ask it to just run one of
 * them.
 * @author Luke McCarthy
 */
public class SingleQueryTest
{
	public static void main(String[] args)
	{
		OntDocumentManager.getInstance().setCacheModels(true);
		
		String query;
		
//		query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//				"PREFIX simple: <http://biordf.net/cardioSHARE/simple.owl#> " +
//				"SELECT ?gene " + //FROM <http://biordf.net/cardioSHARE/simple.owl> " +
//				"WHERE { " +
//					"?gene rdf:type simple:CaffeineMetabolismParticipant " +
//				"}";
		
//		query = "PREFIX pred: <http://sadiframework.org/ontologies/predicates.owl#> " +
//				"PREFIX uniprot: <http://biordf.net/moby/UniProt/> " +
//				"SELECT ?name " +
//				"WHERE { " +
//					"uniprot:P15923 pred:hasName ?name " +
//				"}";
		
//		query = "PREFIX pred: <http://sadiframework.org/ontologies/predicates.owl#> " +
//				"PREFIX uniprot: <http://biordf.net/moby/UniProt/> " +
//				"SELECT ?name " +
//				"WHERE { " +
//					"uniprot:P15923 pred:hasGOTerm ?name " +
//				"}";
		
//		query = "PREFIX pred: <http://es-01.chibi.ubc.ca/~benv/predicates.owl#> " +
//				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
//				"SELECT ?term ?label " +
//				"WHERE { " +
//					"<http://biordf.net/moby/UniProt/A2ABK8> pred:hasGOTerm ?term . " +
//					"?term rdfs:label ?label " +
//				"}";
		
//		query = "PREFIX ont: <http://sadiframework.org/ontologies/service_objects.owl#> " +
//				"PREFIX kegg: <http://biordf.net/moby/KEGG/hsa:> " +
//				"SELECT ?gene ?score ?ssdb " +
//				"WHERE { " +
//				"kegg:2919 ont:isOrthologOf ?gene . " +
//					"?gene ont:hasSSDBParticipant ?ssdb . " +
//					"?ssdb ont:bitScore  ?score " +
//				"}";
//		
//		query = "PREFIX pred: <http://sadiframework.org/ontologies/predicates.owl#> " +
//				"PREFIX ont: <http://ontology.dumontierlab.com/> " +
//				"PREFIX kegg: <http://biordf.net/moby/KEGG_PATHWAY/> " +
//				"SELECT ?participant ?protein ?chemical " +
//				"WHERE { " +
//					"kegg:hsa00232 ont:hasParticipant ?participant . " +
//					"OPTIONAL { " +
//						"?participant pred:encodes ?protein . " +
//					"} . " +
//					"OPTIONAL { " +
//						"?participant pred:isSubstance ?chemical . " +
//					"} . " +
//				"}";
//		
//		query = "SELECT * WHERE { " +
//					"<http://lsrn.org/UniProt:P47989> <http://sadiframework.org/ontologies/predicates.owl#isEncodedBy> ?gene " +
//				"}";
//		
//		query = "PREFIX pred: <http://sadiframework.org/ontologies/service_objects.owl#> " +
//				"PREFIX uniprot: <http://lsrn.org/UniProt:> " +
//				"SELECT ?name " +
//				"WHERE { " +
//					"uniprot:P15923 pred:fromNCBITaxon ?name . " +
//				"}";
//		
//		query = "PREFIX patients: <http://sadiframework.org/ontologies/patients.owl#> " +
//				"PREFIX regress: <http://sadiframework.org/examples/regression.owl#>" +
//				"PREFIX pred: <http://sadiframework.org/ontologies/predicates.owl#> " +
//				"SELECT ?patient ?bun ?creat ?slope " +
//				"FROM <http://sadiframework.org/ontologies/patients.rdf> " +
//				"WHERE { " +
//					"?patient patients:creatinineLevels ?collection . " +
//					"?collection regress:hasRegressionModel ?model . " +
//					"?model regress:slope ?slope " +
//					"	FILTER (?slope > 0) . " +
//					"?patient pred:latestBUN ?bun . " +
//					"?patient pred:latestCreatinine ?creat . " +
//				"}";
		
		query = ExampleQueries.getQueryByHtmlListIndex(13);
		
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		
		SHAREQueryClient client = new SHAREQueryClient();
		List<Map<String, String>> results = client.synchronousQuery(query);
		
		stopWatch.stop();
		
		try {
			client.getDataModel().write(new FileOutputStream("/tmp/SingleQueryTest.rdf"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for (Map<String, String> binding: results)
			System.out.println(binding);
		System.out.println(String.format("query finished in %s", DurationFormatUtils.formatDurationHMS(stopWatch.getTime())));
	}
}
