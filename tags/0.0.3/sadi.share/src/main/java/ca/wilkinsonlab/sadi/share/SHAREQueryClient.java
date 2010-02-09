package ca.wilkinsonlab.sadi.share;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.QueryClient;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class SHAREQueryClient extends QueryClient
{
	private static final Logger log = Logger.getLogger(SHAREQueryClient.class)
	;
	protected SHAREKnowledgeBase kb;
	
	public SHAREQueryClient()
	{
		this(new SHAREKnowledgeBase());
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
			return QueryExecutionFactory.create(query, model);
		}
		
		public void run()
		{
			/* execute the query in the dynamic knowledge base, collecting
			 * the data that will be used by the actual reasoner...
			 */
			log.debug("populating SHARE knowledge base");
			kb.executeQuery(query);
			
			log.debug("using populated SHARE knowledge base to solve query");
			QueryExecution qe = getQueryExecution(query, kb.getReasoningModel());
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
			qe.close();
		}
	}
}
