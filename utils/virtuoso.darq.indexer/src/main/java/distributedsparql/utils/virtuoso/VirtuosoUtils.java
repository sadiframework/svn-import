package distributedsparql.utils.virtuoso;

import virtuoso.jdbc3.VirtuosoException;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.UpdateDeniedException;

import distributedsparql.vocab.DARQ;

public class VirtuosoUtils 
{

	static public boolean testRead(String host, int port, String username, String password) 
	{
		boolean readPermission = true;

		VirtuosoQueryExecution vqe = null;
		
		try {
			
			VirtGraph set = new VirtGraph(VirtuosoUtils.getJDBCConnectionString(host, port), username, password);
			
			/* try to retrieve a triple from the database */ 

			Query sparql = QueryFactory.create("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } } LIMIT 1"); 
			vqe = VirtuosoQueryExecutionFactory.create (sparql, set);
			ResultSet results = vqe.execSelect();

			if(results.hasNext()) {
				results.nextSolution();
			}
			
		} catch(JenaException e) {
		
			if(e.getCause() != null && e.getCause() instanceof VirtuosoException) {
				readPermission = false;
			} else {
				throw e;
			}
		
		} finally {

			if(vqe != null) {
				vqe.close();
			}
		
		}

		return readPermission;
	}

	static public boolean testWrite(String graphURI, String host, int port, String username, String password) 
	{
		boolean writePermission = true;
		
		Model model = null;
		try {

			model = VirtModel.openDatabaseModel(DARQ.serviceDescriptionGraph.getURI(), getJDBCConnectionString(host, port), username, password);

			/* try to add a triple to the database */
			
			Statement mockStatement = new StatementImpl(model.createResource("a"), model.createProperty("b"), model.createResource("c"));
			model.add(mockStatement);
			model.remove(mockStatement);
			
		} catch(UpdateDeniedException e) {
			
			writePermission = false;
			
		} catch(JenaException e) {
		
			if(e.getCause() != null && e.getCause() instanceof VirtuosoException) {
				writePermission = false;
			} else {
				throw e;
			}
		
		} finally {
			
			if(model != null) {
				model.close();
			}
		
		}
		
		return writePermission;
	}

	
	static public String getJDBCConnectionString(String host, int port) 
	{
		return String.format("jdbc:virtuoso://%s:%d", host, port);
	}
	
}
