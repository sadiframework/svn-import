package ca.wilkinsonlab.sadi.rdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;

public class MySQLRdfRegistry extends RdfRegistry
{
	Logger log = Logger.getLogger(MySQLRdfRegistry.class);
	
	Model model;
	
	public MySQLRdfRegistry() throws IOException
	{
		super("http://biordf.net/sparql", "http://sadiframework.org/registry");
		
		log.info("instantiating MySQL registry");
		
		// load the driver class
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch ( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		}
		
		// create a database connection
		IDBConnection conn = new DBConnection(
				"jdbc:mysql://localhost/test_registry",
				"",
				"",
				"MySQL"
		);
		
		// create a model maker with the given connection parameters
		ModelMaker maker = ModelFactory.createModelRDBMaker(conn);

		// create a default model
		model = maker.createDefaultModel();
	}
	
	protected List<Map<String, String>> executeQuery(String query)
	throws IOException
	{
		List<Map<String, String>> localBindings = new ArrayList<Map<String, String>>();
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet resultSet = qe.execSelect();
		while (resultSet.hasNext()) {
			QuerySolution binding = resultSet.nextSolution();
			Map<String, String> ourBinding = new HashMap<String, String>();
			for (Iterator<String> i = binding.varNames(); i.hasNext(); ) {
				String variable = i.next();
				ourBinding.put(variable, RdfUtils.getPlainString(binding.get(variable).asNode()));
			}
			localBindings.add(ourBinding);
		}
		qe.close();
		return localBindings;
	}
}
