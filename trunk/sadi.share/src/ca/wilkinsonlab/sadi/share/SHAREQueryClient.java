package ca.wilkinsonlab.sadi.share;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	protected SHAREKnowledgeBase kb;
	
	public SHAREQueryClient()
	{
		this(new SHAREKnowledgeBase());
	}
	
	public SHAREQueryClient(SHAREKnowledgeBase kb)
	{
		this.kb = kb;
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
		
		public void run()
		{
			/* execute the query in the dynamic knowledge base, collecting
			 * the data that will be used by the actual reasoner...
			 */
			kb.executeQuery(query);
			
			QueryExecution qe = QueryExecutionFactory.create(query, kb.getReasoningModel());
			ResultSet resultSet = qe.execSelect();
			while (resultSet.hasNext()) {
				QuerySolution binding = resultSet.nextSolution();
				Map<String, String> bindingAsMap = new HashMap<String, String>();
				for (Iterator<String> i = binding.varNames(); i.hasNext(); ) {
					String var = i.next();
					RDFNode varValue = binding.get(var);
					bindingAsMap.put(var, varValue != null ? RdfUtils.getPlainString(varValue.asNode()) : "");
				}
				results.add(bindingAsMap);
			}
			qe.close();
		}
	}
}
