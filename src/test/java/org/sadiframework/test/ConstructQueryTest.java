package org.sadiframework.test;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class ConstructQueryTest
{
	private static final String NS = "http://sadiframework.org/ontologies/dummy#";
	
	public static void main(String[] args)
	{
		Model model = ModelFactory.createDefaultModel();
		model.read(ConstructQueryTest.class.getResourceAsStream("../owl2sparql/QueryGeneratingDecomposerTest.rdf"), NS);
		String query =
			"CONSTRUCT { \n" + 
			"	?input a <http://sadiframework.org/ontologies/dummy#SuperClass> . \n" + 
			"	?input <http://sadiframework.org/ontologies/dummy#superProperty> ?rangesuperclass . \n" + 
			"	?rangesuperclass a <http://sadiframework.org/ontologies/dummy#RangeSuperClass> . \n" + 
			"} WHERE { \n" + 
			"	{\n" + 
			"		?input a <http://sadiframework.org/ontologies/dummy#SuperClass> . \n" + 
			"	} UNION {\n" + 
			"		{\n" + 
			"			?input <http://sadiframework.org/ontologies/dummy#superProperty> ?rangesuperclass . \n" + 
			"		} UNION {\n" + 
			"			?input <http://sadiframework.org/ontologies/dummy#subProperty> ?rangesuperclass . \n" + 
			"		} . " +
			"		{\n" + 
			"			?rangesuperclass a <http://sadiframework.org/ontologies/dummy#RangeSuperClass> . \n" + 
			"		} UNION {\n" + 
			"		} . \n" + 
			"	} . \n" + 
			"}";
		query =
			"CONSTRUCT { \n" + 
			"    ?input a <http://sadiframework.org/ontologies/dummy#SuperClass> . \n" + 
			"    ?input <http://sadiframework.org/ontologies/dummy#superProperty> ?RangeSuperClass . \n" + 
			"    ?RangeSuperClass a <http://sadiframework.org/ontologies/dummy#RangeSuperClass> . \n" + 
			"} WHERE { \n" + 
			"    {\n" + 
			"        ?input a <http://sadiframework.org/ontologies/dummy#SuperClass> . \n" + 
			"    } UNION {\n" + 
			"        {\n" + 
			"            ?input <http://sadiframework.org/ontologies/dummy#superProperty> ?RangeSuperClass . \n" + 
			"        } UNION {\n" + 
			"            ?input <http://sadiframework.org/ontologies/dummy#subProperty> ?RangeSuperClass . \n" + 
			"        } . \n" + 
			"        {\n" + 
			"            ?RangeSuperClass a <http://sadiframework.org/ontologies/dummy#RangeSuperClass> . \n" + 
			"        } UNION {\n" + 
			"        } . \n" + 
			"    } . \n" + 
			"}";
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		Model results = qe.execConstruct();
		results.getWriter("N3").write(results, System.out, NS);
	}
}
