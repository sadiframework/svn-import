package ca.wilkinsonlab.sadi.pellet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.jena.PelletQueryExecution;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import ca.wikinsonlab.sadi.client.QueryClient;
import ca.wilkinsonlab.sadi.biomoby.BioMobyRegistry;
import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.jena.PredicateVisitor;
import ca.wilkinsonlab.sadi.sparql.SPARQLRegistry;
import ca.wilkinsonlab.sadi.utils.OwlUtils;

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
	private static final Log log = LogFactory.getLog(PelletClient.class);
	
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

			
			// Many predicates referenced in SPARQL endpoints either aren't resolvable,
			// or are just plain RDF properties.   For this reason, we must try to 
			// determine the type of each predicate using the SPARQL registry before
			// we try to resolve them on the web. -- BV

			Set<String> unresolvedPredicates = new HashSet<String>();
			if(Config.getSPARQLRegistry() != null)
				unresolvedPredicates = addPredicatesFromSPARQLRegistry(model, predicates);

			// TODO: Instead of loading the whole ontology here, just resolve the
			// URIs in unresolvedPredicates on the web.   There are a couple of issues
			// that need to be worked out relating to this:
			//
			// 1) Importing entire ontologies for the sake of a single predicate can
			// lead to inconsistency errors.  (For example if there are domain/range 
			// restrictions on the imported predicates.)
			//
			// 2) Some predicates resolve to RDF files, rather than OWL files.  This
			// causes a SAXParseException with our current setup.
			// 
			// Also note, for some reason I don't understand, if I add predicates 
			// from the SPARQL registry *after* the moby registry ontology has been
			// added, the SPARQL predicates are reported as undefined.  This is baffling.
			//
			// -- BV
			
			BioMobyRegistry mobyRegistry = Config.getMobyRegistry();
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

	/**
	 * For a given list of predicate URIs, query the type of each predicate from the
	 * SPARQL registry and add a corresponding property to the given
	 * OntModel.
	 * 
	 * @param model
	 * @param predicates
	 * @return The set of predicates for which the SPARQL registry had no type 
	 * information (i.e. object property vs. datatype property).  The predicates in 
	 * this set are not added to the given OntModel. 
	 */
	private static Set<String> addPredicatesFromSPARQLRegistry(OntModel model, Set<String> predicates) throws IOException 
	{
		SPARQLRegistry sparqlReg = Config.getSPARQLRegistry();
		Set<String> unresolvedPredicates = new HashSet<String>();

		for(String predicate : predicates) {
			/* 
			 * NOTE: It seems sensible to skip properties that are
			 * already defined in the model here, but this ultimately causes
			 * queries which use annotation-type properties (e.g. dc:title) 
			 * to return zero results.  In such cases, we want the
			 * properties to be defined as both datatype properties 
			 * and annotation properties. 
			 * --BV
			 */
			/*
		 	if((model.getDatatypeProperty(predicate) != null) || 
		 	   (model.getObjectProperty(predicate) != null))
				continue;
			*/
			if(sparqlReg.hasPredicate(predicate)) {
				if(sparqlReg.isDatatypeProperty(predicate))
					model.createDatatypeProperty(predicate);
				else
					model.createObjectProperty(predicate);
			}
			else 
				unresolvedPredicates.add(predicate);
		}	
		
		return unresolvedPredicates;
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
