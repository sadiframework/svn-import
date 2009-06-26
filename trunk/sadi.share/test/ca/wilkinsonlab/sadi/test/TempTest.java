package ca.wilkinsonlab.sadi.test;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;

import ca.wilkinsonlab.sadi.pellet.PelletClient;

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
		String query = ExampleQueries.getQueryByHtmlListIndex(12);
		
//		query = "PREFIX pred: <http://es-01.chibi.ubc.ca/~benv/predicates.owl#> " +
//				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
//				"SELECT ?term ?label " +
//				"WHERE { " +
//					"<http://biordf.net/moby/UniProt/A2ABK8> pred:hasGOTerm ?term . " +
//					"?term rdfs:label ?label " +
//				"}";
		
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		List<Map<String, String>> results = new PelletClient().synchronousQuery(query);
		for (Map<String, String> binding: results)
			System.out.println(binding);
		stopWatch.stop();
		System.out.println(String.format("query finished in %d seconds", stopWatch.getTime()/1000));
	}
}
