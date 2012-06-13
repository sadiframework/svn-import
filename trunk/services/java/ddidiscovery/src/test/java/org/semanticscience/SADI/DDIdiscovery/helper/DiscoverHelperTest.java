package org.semanticscience.SADI.DDIdiscovery.helper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.semanticscience.SADI.DDIdiscovery.Discover;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import junit.framework.TestCase;

public class DiscoverHelperTest extends TestCase {

	@Test
	public void testOne(){
		/*final String ddiFilename = "ddi-0.0.3.csv";
		//read in the ddi.csv file
		InputStream is = Discover.class.getClassLoader().getResourceAsStream(
				ddiFilename);
		ArrayList<DrugDrugInteraction> ddi = DiscoverHelper.findDDIs(is, "DB00091");
		assertNotNull(ddi);*/
		/*
		String service = "http://cu.pharmgkb.bio2rdf.org/sparql";
		String query= "select DISTINCT ?ddi where {"
  +"?ddi <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc ."
  +"?ddi <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc2 ."
  +"?pcc <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut ."
  +"?dbc <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut ."
  +"?pcc2 <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut2 ."
  +"?dbc2 <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut2 ."
  +"FILTER(?pcc != ?pcc2) ." 
  +"FILTER(?dbc != ?dbc2) .}LIMIT 10";
	
		String query2 = "select ?dbc ?drugLabel ?dbc2 ?drug2Label ?umlsEvent ?umlsEventLabel where {"
				  +"<http://bio2rdf.org/twosides:3035> <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc ."
				  +"<http://bio2rdf.org/twosides:3035> <http://bio2rdf.org/pharmgkb_vocabulary:chemical> ?pcc2 ."
				  +"<http://bio2rdf.org/twosides:3035> <http://bio2rdf.org/pharmgkb_vocabulary:event> ?umlsEvent ."
				  +"?umlsEvent <http://www.w3.org/2000/01/rdf-schema#label> ?umlsEventLabel ."
				  +"?pcc <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut ."
				  +"?dbc <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut ."
				  +"?pcc <http://www.w3.org/2000/01/rdf-schema#label> ?drugLabel ."
				  +"?pcc2 <http://bio2rdf.org/pubchemcompound_vocabulary:has_tautomer> ?taut2 ."
				  +"?dbc2 <http://bio2rdf.org/drugbank_vocabulary:xref> ?taut2 ."
				  +"?pcc <http://www.w3.org/2000/01/rdf-schema#label> ?drug2Label ."
				  +"FILTER(?pcc != ?pcc2) ." 
				  +"FILTER(?dbc != ?dbc2) .}";
				
		QueryExecution qe = QueryExecutionFactory.sparqlService(service, query2);
		ResultSet rs = qe.execSelect();
		while(rs.hasNext()){
			QuerySolution qs = rs.next();
			System.out.println(qs.get("umlsEvent"));
		}
			
	*/
		List<String> ddis = DiscoverHelper.getDDIUris();
		DiscoverHelper.writeDDICSV(ddis);

	}
}
