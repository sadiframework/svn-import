package ca.wilkinsonlab.sadi.share;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.QueryClient;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class SHAREQueryClient extends QueryClient
{
	private static final Logger log = Logger.getLogger(SHAREQueryClient.class);
		
	/** allow/disallow use of ARQ extensions to the SPARQL query language (e.g. GROUP BY, HAVING, arithmetic expressions) */
	protected final static String ALLOW_ARQ_SYNTAX_CONFIG_KEY = "share.sparql.allowARQSyntax";
	protected Syntax querySyntax;
	protected SHAREKnowledgeBase kb;
	
	public SHAREQueryClient()
	{
		this(new SHAREKnowledgeBase(Config.getConfiguration().getBoolean(ALLOW_ARQ_SYNTAX_CONFIG_KEY, true)));
	}
	
	public SHAREQueryClient(SHAREKnowledgeBase kb)
	{
		this.kb = kb;
	}
	
	public SHAREKnowledgeBase getKB()
	{
		return kb;
	}
	
	public Model getDataModel()
	{
		return kb.getDataModel();
	}
	
	@Override
	protected QueryRunner getQueryRunner(String query, QueryClientCallback callback)
	{
		return new SHAREQueryRunner(query, callback);
	}
	
	public class SHAREQueryRunner extends QueryRunner
	{
		public SHAREQueryRunner(String query, QueryClientCallback callback)
		{
			super(query, callback);
		}
		
		protected QueryExecution getQueryExecution(String query, Model model)
		{
			return QueryExecutionFactory.create(query, kb.getQuerySyntax(), model);
		}
		
		public void run()
		{
			StopWatch stopWatch = new StopWatch();
			
			/* execute the query in the dynamic knowledge base, collecting
			 * the data that will be used by the actual reasoner...
			 */

			log.debug("populating SHARE knowledge base");

			stopWatch.start();
			kb.executeQuery(query);
			stopWatch.stop();
			log.debug(String.format("populated SHARE knowledge base in %dms", stopWatch.getTime()));
			
			kb.getReasoningModel().rebind();
			log.debug("using populated SHARE knowledge base to solve query");

			stopWatch.reset();
			stopWatch.start();			
			execQuery(results, query, kb.getReasoningModel());

			stopWatch.stop();
			log.debug(String.format("solved query against populated SHARE knowledge base in %dms", stopWatch.getTime()));
		}
		
		private void execQuery(List<Map<String, String>> results, String query, Model model)
		{
			QueryExecution qe = getQueryExecution(query, kb.getReasoningModel());
			Query q = QueryFactory.create(query, kb.getQuerySyntax());
			if (q.isSelectType()) {
				ResultSet resultSet = qe.execSelect();
				while (resultSet.hasNext()) {
					QuerySolution binding = resultSet.nextSolution();
					Map<String, String> bindingAsMap = new HashMap<String, String>();
					for (String var: resultSet.getResultVars()) {
						RDFNode varValue = binding.get(var);
						bindingAsMap.put(var, varValue != null ? RdfUtils.getPlainString(varValue.asNode()) : null);
					}
					results.add(bindingAsMap);
				}
			} else if (q.isAskType()) {
				boolean result = qe.execAsk();
				Map<String, String> binding = new HashMap<String, String>();
				binding.put("result", String.valueOf(result));
				results.add(binding);
			}
			qe.close();		
		}
	}
}
