package ca.wilkinsonlab.sadi.test;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

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
	private static final Logger log = Logger.getLogger( SingleQueryTest.class );
	
	private static final String OUTPUT_FILENAME = "/tmp/SingleQueryTest.rdf";
	
	public static void main(String[] args)
	{
		OntDocumentManager.getInstance().setCacheModels(true);
		
		String query;
		
		query = ExampleQueries.getQueryByHtmlListIndex(6);
		
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
		
//		query = "PREFIX pred: <http://sadiframework.org/ontologies/predicates.owl#> " +
//				"PREFIX uniprot: <http://bio2rdf.org/uniprot:> " +
//				"SELECT ?name " +
//				"WHERE { " +
//					"uniprot:P15923 pred:hasName ?name " +
//				"}";
		
//		query = 
//			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//			"PREFIX ss: <http://semanticscience.org/resource/> " +
//			"SELECT ?s " +
//			"FROM <http://sadiframework.org/ontologies/lipinski-modified.owl> " +
//			"FROM <http://semanticscience.org/sadi/ontology/lipinski_test.rdf> " +
//			"WHERE { " +
//			"    ?s rdf:type <http://semanticscience.org/sadi/ontology/lipinskiserviceontology.owl#lipinskismilesmolecule> . " +
//			"}";

//		query = 
//			"SELECT ?p " +
//			"WHERE { " +
//				"<http://elmonline.ca/sw/explore.rdf#id> ?p [] " +
//			"}";
		
//		query = 
//			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//			"SELECT ?x " +
//			"FROM <http://semanticscience.org/sadi/ontology/lipinski_test.rdf> " +
//			"WHERE { " +
////			"	?x rdf:type <http://semanticscience.org/sadi/ontology/lipinskiserviceontology.owl#alogpsmilesmolecule> " + 
//			" ?x <http://semanticscience.org/resource/SIO_000008> ?attr . " +
//			" ?attr rdf:type <http://semanticscience.org/resource/CHEMIN_000251> . " +
//			" ?attr <http://semanticscience.org/resource/SIO_000300> ?value . " +
//			"}";
		
//		query = 
//			"SELECT ?impact ?mut_spec " +
//			"FROM <http://www.freewebs.com/riazanov/SHARE_input1.rdf> " +
//			"WHERE { " +
//			"   ?impact a <http://www.unbsj.ca/sase/csas/mutationOntology.owl#MutationImpact> . " +
//			"   ?impact <http://www.unbsj.ca/sase/csas/mutationOntology.owl#impactIsSpecifiedBy> ?mut_spec " +
//			"}";
		
//		query = 
//			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//			"PREFIX ss: <http://semanticscience.org/resource/> " +
//			"SELECT ?s ?value " +
//			"FROM <http://semanticscience.org/sadi/ontology/lipinski_test.rdf> " +
//			"WHERE { " +
//			"  ?s ss:SIO_000008 ?attr . " +
//			"  ?attr rdf:type ss:CHEMINF_000245 . " +
//			"  ?attr ss:SIO_000300 ?value . " +
//			"}";

//		query =
//			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//			"PREFIX lm: <http://bio2rdf.org/lipidmaps:> " +
//			"SELECT ?lipidType " +
//			"FROM <http://unbsj.biordf.net/lipids/service-data/sample_input_cl1.rdf> " +
//			"WHERE { " +
//			"  lm:LMPR02030001 rdf:type ?lipidType " +
//			"}";
		
//		query = 
//			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//			"PREFIX ss: <http://semanticscience.org/resource/> " +
//			"PREFIX lso: <http://semanticscience.org/sadi/ontology/lipinskiserviceontology.owl#> " +
//			"SELECT ?s ?value " +
//			"FROM <http://semanticscience.org/sadi/ontology/lipinskiserviceontology.owl> " +
//			"FROM <http://semanticscience.org/sadi/ontology/lipinski_test.rdf> " +
//			"WHERE { " +
//			" ?s rdf:type lso:smilesmolecule . " +
//			" ?s lso:hasChemicalDescriptor ?attr . " +
//			" ?attr rdf:type ss:CHEMINF_000244 . " +
//			" ?attr ss:SIO_000300 ?value . " +
//			"}";
		
//		query =
//			"PREFIX test: <http://unbsj.biordf.net/test/test-sadi-service-ontology.owl#> " +
//			"SELECT ?b " +
//			"FROM <http://unbsj.biordf.net/test/test-sadi-service-ontology.owl> " +
//			"WHERE { " +
//			"  ?b a test:B . " +
//			"  test:a1 test:R1 ?b . " +
//			"}";
//		
//		query =
//			"PREFIX test: <http://unbsj.biordf.net/test/test-sadi-service-ontology.owl#> " +
//			"SELECT ?b " +
//			"FROM <http://unbsj.biordf.net/test/test-sadi-service-ontology.owl> " +
//			"WHERE { " +
//			"  ?b a test:B . " +
//			"  ?b test:R2 test:a1 . " +
//			"}";
//		
//		query =
//			"PREFIX test: <http://unbsj.biordf.net/test/test-sadi-service-ontology.owl#> " +
//			"ASK " +
//			"FROM <http://unbsj.biordf.net/test/test-sadi-service-ontology.owl> " +
//			"WHERE { " +
//			"  test:a1 test:R1 test:b1 . " +
//			"}";
//		
//		query = 
//			"SELECT * " +
//			"WHERE { " +
//			"  <http://bio2rdf.org/uniprot:P12345> <http://purl.uniprot.org/core/classifiedWith> ?term . " +
//			"  ?term <http://semanticscience.org/resource/SIO_000226> ?protein " +
//			"}";
//		
//		query = 
//			"SELECT ?gName\n" + 
//			"WHERE {\n" + 
//			"  ?drug drugbank:brandName \"Vasotec\" .\n" + 
//			"  ?drug drugbank:genericName ?gName\n" + 
//			"}";
//		
//		query =
//			"PREFIX sadi: <http://sadiframework.org/ontologies/properties.owl#> " +
//			"PREFIX sio: <http://semanticscience.org/resource/> " +
//			"PREFIX taxon: <http://lsrn.org/taxon:> " +
//			"SELECT *" +
//			"FROM <http://tomcat.dev.biordf.net/sadi-blast/t/dragon-input.rdf>" +
//			"WHERE {" +
//			"  ?qseq sio:SIO_000028 ?qsubseq . " +
//			"  ?qsubseq sio:SIO_000068 ?blasthit . " +
//			"  ?blasthit sio:SIO_000028 ?alignment . " +
//			"  ?alignment sio:SIO_000028 ?hsubseq . " +
//			"  ?hsubseq sio:SIO_000068 ?hseq . " +
//			"  ?hseq sadi:fromOrganism taxon:4151 . " +
//			"}";
//		
//		query =
//			"PREFIX sadi: <http://sadiframework.org/ontologies/properties.owl#> \n" + 
//			"PREFIX sio: <http://semanticscience.org/resource/> \n" + 
//			"PREFIX taxon: <http://lsrn.org/taxon:> \n" + 
//			"SELECT DISTINCT ?hseq\n" + 
//			"FROM <http://tomcat.dev.biordf.net/sadi-blast/t/dragon-input.rdf>\n" + 
//			"WHERE {  \n" + 
//			"  ?qseq sio:SIO_000028 ?qsubseq . \n" + 
//			"  ?qsubseq sio:SIO_000068 ?alignment . \n" + 
//			"  ?alignment sio:SIO_000028 ?hsubseq . \n" + 
//			"  ?hsubseq sio:SIO_000068 ?hseq . \n" + 
//			"  ?hseq sadi:fromOrganism taxon:4151 . \n" + 
//			"}";
//		
//		query =
//			"PREFIX info: <http://sadiframework.org/ontologies/service_objects.owl#>\n" + 
//			"PREFIX an: <http://sadiframework.org/ontologies/AntirrhinumServices.owl#>\n" + 
//			"PREFIX genetics:  <http://sadiframework.org/ontologies/helper.owl#>\n" + 
//			"PREFIX locus: <http://lsrn.org/DragonDB_Locus:>\n" + 
//			"SELECT ?allele  \n" + 
//			"where {\n" + 
//			"      locus:DEF      an:has_allele        ?allele         \n" + 
//			"}";
//		
//		query = 
//			"PREFIX sio: <http://semanticscience.org/resource/> \n" + 
//			"SELECT * \n" +
//			"WHERE { \n" +
//			"    <http://opencitations.net/id/expression:pmid/12682359> sio:SIO_000252 ?o .\n" +
//			"}";
//		
//		query =
//			"PREFIX cardio: <http://es-01.chibi.ubc.ca/~soroush/framingham/cardiorisk.owl#>\n" + 
//			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
//			"\n" + 
//			"SELECT  ?originalvalue ?originalunit ?convertedvalue ?canonicalunit\n" + 
//			"FROM <http://es-01.chibi.ubc.ca/~soroush/framingham/pressuredata.rdf>\n" + 
//			"WHERE {\n" + 
//			"    ?pressure rdf:type cardio:SystolicBloodPressure  .\n" + 
//			"    ?pressure cardio:hasMeasurement ?measurement1  .\n" + 
//			"    ?measurement1 rdf:type cardio:CanonicalPressureMeasurement . \n" + 
//			"    ?measurement1 cardio:hasValue ?convertedvalue .\n" + 
//			"    ?measurement1 cardio:hasUnit ?canonicalunit .\n" + 
//			"    \n" + 
//			"    ?pressure cardio:hasMeasurement ?measurement2  .\n" + 
//			"    ?measurement2 cardio:hasValue ?originalvalue .\n" + 
//			"    ?measurement2 cardio:hasUnit ?originalunit .\n" + 
//			"    FILTER regex(str(?measurement2), \"http://es-01.chibi.ubc.ca/~soroush/framingham/\")\n" + 
//			"}";
		
		log.info( String.format("executing query\n%s", query) );
		
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		
		SHAREQueryClient client = new SHAREQueryClient();
		List<Map<String, String>> results = client.synchronousQuery(query);
		
		stopWatch.stop();
		
		StringBuffer buf = new StringBuffer("query finished in ");
		buf.append( DurationFormatUtils.formatDurationHMS(stopWatch.getTime()) );
		if (results.isEmpty())
			buf.append("\nno results");
		else
			for (Map<String, String> binding: results) {
				buf.append("\n");
				buf.append(binding);
			}
		log.info( buf.toString() );
		
		try {
			client.getDataModel().write(new FileOutputStream(OUTPUT_FILENAME), "N3");
		} catch (Exception e) {
			log.error( String.format("error writing to %s: %s", OUTPUT_FILENAME, e) );
		}
	}
}
