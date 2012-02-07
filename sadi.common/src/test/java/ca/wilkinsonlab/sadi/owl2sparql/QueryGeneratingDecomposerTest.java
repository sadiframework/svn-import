package ca.wilkinsonlab.sadi.owl2sparql;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.utils.OwlUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class QueryGeneratingDecomposerTest
{
	private static final Logger log = Logger.getLogger(QueryGeneratingDecomposerTest.class);
	private static final String NS = "http://sadiframework.org/ontologies/dummy#";
	private static Model model;
	private static OntModel ontModel;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		model = ModelFactory.createDefaultModel();
		model.read(QueryGeneratingDecomposerTest.class.getResourceAsStream("QueryGeneratingDecomposerTest.rdf"), NS);
		ontModel = OwlUtils.createDefaultReasoningModel();
		ontModel.read(QueryGeneratingDecomposerTest.class.getResourceAsStream("QueryGeneratingDecomposerTest.owl"), NS);
		ontModel.addSubModel(model);
		ontModel.rebind(); // might not be necessary...
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		model.close();
		model = null;
		ontModel.close();
		ontModel = null;
	}

	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testGetQuery()
	{
		test("SuperClass", new String[] {
				"SuperClassInstance", 
				"SuperClassInstanceSuperPropertyRangeSuperClass",
				"SuperClassInstanceSubPropertyRangeSuperClass", 
				"SuperClassInstanceSubPropertyRangeSubClass",
				"SubClassInstance",
				"superPropertyRangeSuperClass",
				"superPropertyRangeSubClass",
				"subPropertyRangeSubClass"});
		test("SubClass", new String[] {
				"SubClassInstance"});
		test("UnionOfRestrictions", new String[] {
				"withP", "withQ", "withPQ"});
		test("IntersectionOfRestrictions", new String[] {
				"withPQ"});
	}
	
	private static void test(String className, String[] instanceNames)
	{
		OntClass c = ontModel.getOntClass(NS + className);
		String query = OwlUtils.getConstructQuery(c);
		if (log.isDebugEnabled()) {
			log.debug(String.format("generated query for %s\n%s", c, query));
		}
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		Model results = qe.execConstruct();
		if (log.isDebugEnabled()) {
			log.debug(String.format("results of construct for %s\n%s", c, RdfUtils.logModel(results)));
		}
		List<Resource> instances = new ArrayList<Resource>(instanceNames.length);
		for (String resourceName: instanceNames) {
			instances.add(model.getResource(NS + resourceName));
		}
		for (Resource r: instances) {
			assertTrue(String.format("missing instance %s of class %s", r, c), 
					r.inModel(results).hasProperty(RDF.type, c));	
		}
		for (Iterator<Resource> i = results.listSubjectsWithProperty(RDF.type, c); i.hasNext(); ) {
			Resource r = i.next();
			if (!r.inModel(ontModel).hasProperty(RDF.type, c))
				fail(String.format("superfluous instance %s of class %s", r, c));
		}
	}
}
