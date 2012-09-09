package org.sadiframework.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.sadiframework.SADIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.shared.DoesNotExistException;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * @author Luke McCarthy
 */
public class QueryExecutorFactory
{
	private static final Logger log = LoggerFactory.getLogger(QueryExecutorFactory.class);
	
	protected static final String DRIVER_KEY = "driver";
	protected static final String SPARQL_ENDPOINT_KEY = "endpoint";
	protected static final String SPARQL_GRAPH_KEY = "graph";
	protected static final String DSN_KEY = "dsn";
	protected static final String USERNAME_KEY = "username";
	protected static final String PASSWORD_KEY = "password";
	protected static final String FILE_KEY = "file";
	
	/**
	 * 
	 * @param config
	 * @return
	 */
	public static QueryExecutor createQueryExecutor(Configuration config)
	{
		String endpointURL = config.getString(SPARQL_ENDPOINT_KEY);
		String graphName = config.getString(SPARQL_GRAPH_KEY);
		String dsn = config.getString(DSN_KEY);
		String file = config.getString(FILE_KEY);
		if (endpointURL != null) {
			if (log.isDebugEnabled())
				log.debug(String.format("creating Virtuoso-backed registry model from %s(%s)", endpointURL, graphName));
			return QueryExecutorFactory.createSPARQLEndpointQueryExecutor(endpointURL, graphName);
		} else if (dsn != null) {
			if (log.isDebugEnabled())
				log.debug(String.format("creating JDBC-backed registry model from %s(%s)", dsn, graphName));
			return QueryExecutorFactory.createJDBCJenaModelQueryExecutor(config.getString(DRIVER_KEY), dsn, config.getString(USERNAME_KEY), config.getString(PASSWORD_KEY), graphName);
		} else if (file != null) {
			if (log.isDebugEnabled())
				log.debug(String.format("creating file-backed registry model from %s", file));
			return QueryExecutorFactory.createFileModelQueryExecutor(file);
		} else {
			if (log.isDebugEnabled())
				log.warn("no configuration found; creating transient registry model");
			return QueryExecutorFactory.createJenaModelQueryExecutor();
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static QueryExecutor createJenaModelQueryExecutor()
	{
		return new JenaModelQueryExecutor(ModelFactory.createMemModelMaker(), null);
	}

	/**
	 * 
	 * @param path the path to the file containing the registry RDF
	 * @return
	 */
	public static QueryExecutor createFileModelQueryExecutor(String path)
	{
		File registryFile = new File(path);
		File parentDirectory = registryFile.getParentFile();
		if (parentDirectory == null)
			parentDirectory = new File(".");
		if (!parentDirectory.isDirectory())
			parentDirectory.mkdirs();
		
		return new JenaModelQueryExecutor(
				ModelFactory.createFileModelMaker(parentDirectory.getAbsolutePath()),
				registryFile.getName());
	}

	/**
	 * 
	 * @param driver the database driver class name
	 * @param dsn the data source name
	 * @param username the username to access the database
	 * @param password the password to access the database
	 * @return
	 */
	public static QueryExecutor createJDBCJenaModelQueryExecutor(String driver, String dsn, String username, String password)
	{
		return createJDBCJenaModelQueryExecutor(driver, dsn, username, password, null);
	}
	
	/**
	 * 
	 * @param driver the database driver class name
	 * @param dsn the data source name
	 * @param username the username to access the database
	 * @param password the password to access the database
	 * @param graphName the name of a graph, or null
	 * @return
	 */
	public static QueryExecutor createJDBCJenaModelQueryExecutor(String driver, String dsn, String username, String password, String graphName)
	{
		// load the driver class
		try {
			Class.forName(driver);
		} catch ( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		}
		
		// create a database connection
		IDBConnection conn = new DBConnection(
				dsn,
				username,
				password,
				driver.matches("(?i).*mysql.*") ? "MySQL" : null
		);
		
		// create a model maker with the given connection parameters
		return new JenaModelQueryExecutor(ModelFactory.createModelRDBMaker(conn), graphName);
	}
	
	/**
	 * 
	 * @param endpointURL the URL of the SPARQL endpoint
	 * @return
	 */
	public static QueryExecutor createSPARQLEndpointQueryExecutor(String endpointURL)
	{
		return createSPARQLEndpointQueryExecutor(endpointURL, null);
	}
	
	/**
	 * 
	 * @param endpointURL the URL of the SPARQL endpoint
	 * @param graphName the name of a graph, or null
	 * @return
	 */
	public static QueryExecutor createSPARQLEndpointQueryExecutor(String endpointURL, String graphName)
	{
		return new SPARQLEndpointQueryExecutor(endpointURL, graphName);
	}
	
	protected static List<Map<String, String>> convertResults(ResultSet resultSet)
	{
		List<Map<String, String>> ourBindings = new ArrayList<Map<String, String>>();
		while (resultSet.hasNext()) {
			QuerySolution binding = resultSet.next();
			Map<String, String> ourBinding = new HashMap<String, String>();
			for (Iterator<String> i = binding.varNames(); i.hasNext(); ) {
				String variable = i.next();
				ourBinding.put(variable, RdfUtils.getPlainString(binding.get(variable).asNode()));
			}
			ourBindings.add(ourBinding);
		}
		return ourBindings;
	}
	
	public static class JenaModelQueryExecutor implements QueryExecutor, UpdateQueryExecutor
	{
		private ModelMaker modelMaker;
		private String modelName;
		
		public JenaModelQueryExecutor(ModelMaker modelMaker, String modelName)
		{
			this.modelMaker = modelMaker;
			this.modelName = modelName;
		}
		
		public Model getModel()
		{
			if (modelName != null) {
				// this could possibly be replaced by maker.createModel(modelName, false);
				try {	
					return modelMaker.openModel(modelName);
				} catch (DoesNotExistException e) {
					return modelMaker.createModel(modelName);
				}
			} else {
				// according to the JavaDoc, "Multiple calls will yield the *same* model"
				return modelMaker.createDefaultModel();
			}
		}
		
		/* (non-Javadoc)
		 * @see org.sadiframework.utils.QueryExecutor#executeQuery(java.lang.String)
		 */
		@Override
		public List<Map<String, String>> executeQuery(String query) throws SADIException
		{
			Model model = getModel(); // refresh model (important for files...)
			try {
				QueryExecution qe = QueryExecutionFactory.create(query, model);
				try {
					ResultSet resultSet = qe.execSelect();
					return convertResults(resultSet);
				} finally {
					qe.close();
				}
			} catch (RuntimeException e) { // probably JenaException...
				throw new SADIException(e.getMessage(), e);
			} finally {
				model.close();
			}
		}

		/* (non-Javadoc)
		 * @see org.sadiframework.utils.QueryExecutor#executeConstructQuery(java.lang.String)
		 */
		@Override
		public Model executeConstructQuery(String query) throws SADIException
		{
			Model model = getModel(); // refresh model (important for files...)
			try {
				QueryExecution qe = QueryExecutionFactory.create(query, model);
				try {
					return qe.execConstruct();
				} finally {
					qe.close();
				}
			} catch (RuntimeException e) { // probably JenaException...
				throw new SADIException(e.getMessage(), e);
			} finally {
				model.close();
			}
		}

		/* (non-Javadoc)
		 * @see org.sadiframework.utils.UpdateQueryExecutor#executeUpdateQuery(java.lang.String)
		 */
		@Override
		public void executeUpdateQuery(String query) throws SADIException
		{
			Model model = getModel(); // refresh model (important for files...)
			try {
				UpdateRequest request = UpdateFactory.create(query);
				UpdateAction.execute(request, model);
			} catch (RuntimeException e) { // probably JenaException...
				throw new SADIException(e.getMessage(), e);
			} finally {
				model.close();
			}
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return String.format("%s(%s)", modelMaker.getClass(), modelName);
		}
	}
	
	/* TODO implement SPARQL UPDATE and authentication on SPARQLEndpointQueryExecutor...
	 */
	public static class SPARQLEndpointQueryExecutor implements QueryExecutor
	{
		protected String endpointURL;
		protected String graphName;
		
		public SPARQLEndpointQueryExecutor(String endpointURL, String graphName)
		{
			this.endpointURL = endpointURL;
			this.graphName = graphName;
		}
		
		/* (non-Javadoc)
		 * @see org.sadiframework.utils.QueryExecutor#executeQuery(java.lang.String)
		 */
		@Override
		public List<Map<String, String>> executeQuery(String query) throws SADIException
		{
			QueryExecution qe = graphName != null ?
					QueryExecutionFactory.sparqlService(endpointURL, query, graphName) :
					QueryExecutionFactory.sparqlService(endpointURL, query);
			try {
				ResultSet resultSet = qe.execSelect();
				return convertResults(resultSet);
			} catch (Exception e) {
				log.error("error executing query", e);
				throw new SADIException(e.getMessage(), e);
			} finally {
				qe.close();
			}
		}

		/* (non-Javadoc)
		 * @see org.sadiframework.utils.QueryExecutor#executeConstructQuery(java.lang.String)
		 */
		@Override
		public Model executeConstructQuery(String query) throws SADIException
		{
			QueryExecution qe = graphName != null ?
					QueryExecutionFactory.sparqlService(endpointURL, query, graphName) :
					QueryExecutionFactory.sparqlService(endpointURL, query);
			try {
				return qe.execConstruct();
			} catch (Exception e) {
				log.error("error executing query", e);
				throw new SADIException(e.getMessage(), e);
			} finally {
				qe.close();
			}
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return String.format("%s(%s)", endpointURL, graphName);
		}
	}
}
