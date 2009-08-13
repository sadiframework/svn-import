package ca.wilkinsonlab.sadi.pellet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.jena.PelletQueryExecution;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import ca.wikinsonlab.sadi.client.QueryClient;
import ca.wilkinsonlab.sadi.biomoby.BioMobyRegistry;
import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Registry;
import ca.wilkinsonlab.sadi.jena.PredicateVisitor;
import ca.wilkinsonlab.sadi.sparql.VirtuosoSPARQLRegistry;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFVisitor;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.syntax.ElementWalker;
import com.hp.hpl.jena.sparql.syntax.ElementVisitor;

public class PelletClient extends QueryClient
{
	@Override
	protected QueryRunner getQueryRunner(String query, ClientCallback callback)
	{
		return new PelletClientQueryRunner(query, callback);
	}
	
	private static ResultSet executePelletQuery(String queryString) 
	{
		Query query = QueryFactory.create(queryString);
		OntModel model = createOntologyModel(query);
		QueryExecution qexec = new PelletQueryExecution(query, model);
        ResultSet resultSet = qexec.execSelect();
        qexec.close();
        
		return resultSet;
	}
	
	private static OntModel createOntologyModel(Query query) 
	{
		/* TODO alternatively, don't load our ontologies implicitly and force them
		 * into a FROM clause...
		 */
		OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );        
        model.setStrictMode( false );
        
        /* TODO figure out a better way to do this on a consistent basis
         * for each registry...
         */
//        for (Registry registry: Config.getRegistries())
//        	model.add(registry.getPredicateOntology());

		try {
			// Walk the query to get the set of predicates used within.
			Set<String> predicates = new HashSet<String>();
			ElementVisitor v = new PredicateVisitor(predicates);
			ElementWalker.walk(query.getQueryPattern(), v);
			
			/* TODO resolve each of the predicates referenced by the query
			 * into the OntModel to get us past the initial check in
			 * org.mindswap.pellet.query.QueryEngine
			 * ("Property ? used in the query is not defined in the KB.")
			 * After that, have the DynamicKnowledgeBase resolve new
			 * properties as it comes across them.  This might require
			 * the DynamicKnowledgeBase to have access to an OntModel
			 * view of itself...
			 * For now, just load the predicate definitions up front...
			 */
			model.read("http://sadiframework.org/ontologies/service_objects.owl");
			
			// Many predicates defined in SPARQL endpoints aren't resolvable,
			// so try to find definitions within the SPARQL registry before
			// trying to find them on the web.
//			VirtuosoSPARQLRegistry sparqlReg = new VirtuosoSPARQLRegistry();
			VirtuosoSPARQLRegistry sparqlReg = findSPARQLRegistry();
			if (sparqlReg != null) {
				List<String> remainingPredicates = new ArrayList<String>();
				for(String predicate : predicates) {
					if(sparqlReg.hasPredicate(predicate)) {
						if(sparqlReg.isDatatypeProperty(predicate))
							model.createDatatypeProperty(predicate);
						else
							model.createObjectProperty(predicate);
					}
					else
						remainingPredicates.add(predicate);
				}
			}
			
			// TODO: Instead of loading the whole ontology here, just resolve the
			// URIs in remainingPredicates on the web. Can't do this at
			// the moment because the synonyms to Dumontier's predicates
			// are in http://dev.biordf.net/~benv/predicates.owl, and the
			// service annotations use the old predicates.
//			BioMobyRegistry mobyRegistry = new BioMobyRegistry();
			BioMobyRegistry mobyRegistry = findMobyRegistry();
			if (mobyRegistry != null) {
				model.add(mobyRegistry.getPredicateOntology());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
        
        for (Iterator iter = query.getGraphURIs().iterator(); iter.hasNext();) {
        	String sourceURI = (String) iter.next();
        	model.read( sourceURI );
        }

        return model;
	}
	
	private static VirtuosoSPARQLRegistry findSPARQLRegistry()
	{
		for (Registry reg: Config.getRegistries())
			if (reg instanceof VirtuosoSPARQLRegistry)
				return (VirtuosoSPARQLRegistry)reg;
		return null;
	}
	
	private static BioMobyRegistry findMobyRegistry()
	{
		for (Registry reg: Config.getRegistries())
			if (reg instanceof BioMobyRegistry)
				return (BioMobyRegistry)reg;
		return null;
	}
	
	@SuppressWarnings("unused")
	protected static void printStatements(Model model)
	{
		StmtIterator iter = model.listStatements();
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();
		    Resource  subject   = stmt.getSubject();
		    Property  predicate = stmt.getPredicate();
		    RDFNode   object    = stmt.getObject();

		    System.out.print(subject.toString());
		    System.out.print(" " + predicate.toString() + " ");
		    if (object instanceof Resource) {
		       System.out.print(object.toString());
		    } else {
		        // object is a literal
		        System.out.print(" \"" + object.toString() + "\"");
		    }

		    System.out.println(" .");
		}
	}
	
	private static List<Map<String, String>> convertPelletResultSet(ResultSet resultSet)
	{
//		NodeFormatter formatter = new NodeFormatter(model, false); 
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		while (resultSet.hasNext()) {
			QuerySolution binding = resultSet.nextSolution();
			Map<String, String> genericBinding = new HashMap<String, String>();
			for (Iterator varNames = binding.varNames(); varNames.hasNext(); ) {
				String var = (String)varNames.next();
				RDFNode result = binding.get(var);
//				genericBinding.put(var, formatter.format(result));
				genericBinding.put(var, (String)result.visitWith(new RDFVisitor() {
					public Object visitBlank(Resource r, AnonId id) {
						return String.format("BNODE %s", id);
					}
					public Object visitLiteral(Literal l) {
						return l.getLexicalForm();
					}
					public Object visitURI(Resource r, String uri)
					{
						return uri;
					}
				}));
			}
			results.add(genericBinding);
		}
		return results;
	}
	
	private class PelletClientQueryRunner extends QueryRunner
	{
		public PelletClientQueryRunner(String query, ClientCallback callback)
		{
			super(query, callback);
		}

		public void run() 
		{
			/* TODO error handling...
			 */
			ResultSet resultSet = executePelletQuery(query);
			results = convertPelletResultSet(resultSet);
			if (callback != null)
				callback.onSuccess(results);
		}
	}
}
