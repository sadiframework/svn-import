package ca.wilkinsonlab.sadi.test;

import java.util.List;
import java.util.Map;

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
public class TempTest
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
		
		query = "PREFIX pred: <http://sadiframework.org/ontologies/predicates.owl#> " +
				"PREFIX ont: <http://ontology.dumontierlab.com/> " +
				"PREFIX kegg: <http://biordf.net/moby/KEGG_PATHWAY/> " +
				"SELECT ?participant ?protein ?chemical " +
				"WHERE { " +
					"kegg:hsa00232 ont:hasParticipant ?participant . " +
					"OPTIONAL { " +
						"?participant pred:encodes ?protein . " +
					"} . " +
					"OPTIONAL { " +
						"?participant pred:isSubstance ?chemical . " +
					"} . " +
				"}";
		
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		List<Map<String, String>> results = new SHAREQueryClient().synchronousQuery(query);
		for (Map<String, String> binding: results)
			System.out.println(binding);
		stopWatch.stop();
		System.out.println(String.format("query finished in %d seconds", stopWatch.getTime()/1000));
	}
}
