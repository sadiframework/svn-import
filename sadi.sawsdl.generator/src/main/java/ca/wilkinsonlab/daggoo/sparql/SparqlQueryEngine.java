package ca.wilkinsonlab.daggoo.sparql;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class SparqlQueryEngine {
    
    public static final String INPUT_NODE_URI_KEY = "inputNodeURI";
    
    public static Map<String, Map<String, SparqlResult>> executeSPARQL(String input, String queryString) {
	// key => inputNodeURI
	// value => Map<variable, sparqlresults>
	Map<String, Map<String, SparqlResult>> resultMap = new HashMap<String, Map<String, SparqlResult>>();
	InputStream in = new ByteArrayInputStream(input.getBytes());
	// Create an empty in-memory model and populate it from the graph
	Model model = ModelFactory.createMemModelMaker().createDefaultModel();
	model.read(in,null); // null base URI, since model URIs are absolute
	com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString);
	// Execute the query and obtain results
	QueryExecution qe = QueryExecutionFactory.create(query, model);
	ResultSet results = qe.execSelect();
	// add to query ability to get inputURI to map each result
	// iterate over the results
	while (results.hasNext()) {
	    // variables -> values mapping
	    HashMap<String, SparqlResult> res = new HashMap<String, SparqlResult>();
	    // map res to inputNodeURI
	    String inputNodeURI = "";
	    // get the next solution
	    QuerySolution sol = results.nextSolution();
	    // get the names of variables in the solution
	    Iterator<String> variables = sol.varNames();
	    // iterate over the solution with oru variable names
	    while (variables.hasNext()) {
		String var = variables.next();
		RDFNode rdfNode = sol.get(var);
		if (rdfNode.isLiteral()) {
		    String s = ((Literal)rdfNode).getString();
		    if (res.containsKey(var)) {
			// add to sparqlresult
			res.get(var).addValue(s);
		    } else {
			// create a sparqlresult and add our value
			res.put(var, new SparqlResult(var, null));
			res.get(var).addValue(s);
		    }
		} else {
		    Resource r = ((Resource)rdfNode);
		    if (!r.isAnon()) {
			String s = r.getURI();
			if (var.trim().equals(SparqlQueryEngine.INPUT_NODE_URI_KEY)) {
			    inputNodeURI = s;
			} else {
			    if (res.containsKey(var)) {
				// add to sparqlresult
				res.get(var).addValue(s);
			    } else {
				// create a sparqlresult and add our value
				res.put(var, new SparqlResult(var, null));
				res.get(var).addValue(s);
			    }
			}
		    }
		}
	    }
	    // check if resultMap has our inputNodeURI
	    if (resultMap.containsKey(inputNodeURI)) {
		for (String key : res.keySet()) {
		    if (resultMap.get(inputNodeURI).containsKey(key)) {
			resultMap.get(inputNodeURI).get(key).addValues(res.get(key).getValues());
		    } else {
			resultMap.get(inputNodeURI).put(key, res.get(key));
		    }
		}
	    } else {
		resultMap.put(inputNodeURI, res);
	    }
	}
	// Important free up resources used running the query
	qe.close();
	try {
	    if (in != null) {
		in.close();
	    }
	    if (model != null) {
		model.close();
	    }
		
	} catch (IOException ioe) {
	    
	}
	System.out.println(resultMap);
	return resultMap;	
    }
}
