package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class RdfUtilsTest
{
	@Test
	public void testCreateTypedLiteral()
	{
		assertTrue("failed for integer", createTypedLiteralIsEqual(17));
		assertTrue("failed for double", createTypedLiteralIsEqual(17.23));
		assertTrue("failed for boolean", createTypedLiteralIsEqual(true));
		assertTrue("failed for string", createTypedLiteralIsEqual("testing"));
		try {
			RdfUtils.createTypedLiteral("0^^invalidDatatype");
		} catch (Exception e) {
			fail(String.format("invalid datatype throws exception: %s", e.toString()));
		}
	}
	
	public boolean createTypedLiteralIsEqual(Object o)
	{
		Literal literal = ResourceFactory.createTypedLiteral(o);
		return literal.equals(RdfUtils.createTypedLiteral(literal.toString()));
	}
	
//	@Test
//	public void testCopyValues()
//	{
//		Model fromModel = ModelFactory.createDefaultModel();
//		Resource from = fromModel.createResource();
//		from.addProperty(RDF.type, FOAF.Person);
//		from.addProperty(FOAF.name, "Guy Incognito");
//		Resource jj = fromModel.createResource("http://example.com/jj", FOAF.Person);
//		from.addProperty(FOAF.knows, jj);
//		jj.addProperty(FOAF.name, "Joey Joe Joe Jr. Shabadoo");
//		
//		Model toModel = ModelFactory.createDefaultModel();
//		Resource to = toModel.createResource();
////		RdfUtils.copyValues(from, to, true);
////		if (!fromModel.isIsomorphicWith(toModel)) {
//////			ModelDiff diff = ModelDiff.diff(fromModel, toModel);
//////			System.out.println("In from, not to:\n" + RdfUtils.logStatements(diff.inXnotY));
//////			System.out.println("In to, not from:\n" + RdfUtils.logStatements(diff.inYnotX));
////			fail("models are not isomorphic");
////		}
//		to.addProperty(FOAF.name, "Max Power");
//		RdfUtils.copyValues(from, to, false);
//		assertTrue("destination resource did not receive rdf:type property",
//		           to.hasProperty(RDF.type, FOAF.Person));
//		assertFalse("destination resource should not have new name",
//		            to.hasProperty(FOAF.name, "Guy Incognito"));
//		Resource toJJ = to.getPropertyResourceValue(FOAF.knows);
//		assertNotNull("destination resource did not receive foaf:knows property", toJJ);
//		assertTrue("destination resource did not receive foaf:name property",
//		           toJJ.hasProperty(FOAF.name, "Joey Joe Joe Jr. Shabadoo"));
//	}
//	
//	@Test
//	public void testCopyValues2()
//	{
//		Model fromModel = ModelFactory.createDefaultModel();
//		Resource from = fromModel.createResource(); 
//		RDFPath path1 = new RDFPath(
//				"http://semanticscience.org/resource/SIO_000552, " +
//				"http://unbsj.biordf.net/fishtox/BLAST-sadi-service-ontology.owl#E_Value, " +
//				"http://semanticscience.org/resource/SIO_000300, " +
//				"http://www.w3.org/2001/XMLSchema#double"
//		);
//		path1.createLiteralRootedAt(from, "0.0001");
//		RDFPath path2 = new RDFPath(
//				"http://semanticscience.org/resource/SIO_000552, " +
//				"http://unbsj.biordf.net/fishtox/BLAST-sadi-service-ontology.owl#BitScore, " +
//				"http://semanticscience.org/resource/SIO_000300, " +
//				"http://www.w3.org/2001/XMLSchema#double"
//		);
//		path2.createLiteralRootedAt(from, "25");
//		
//		Model toModel = ModelFactory.createDefaultModel();		
//		Resource to = toModel.createResource();
//		RdfUtils.copyValues(from, to, false);
//		
//		assertTrue("destination model is not isomorphic to source model", 
//				toModel.isIsomorphicWith(fromModel));
//	}
	
	@Test
	public void testGetBoolean()
	{
		Model model = ModelFactory.createDefaultModel();
		assertTrue(RdfUtils.getBoolean(model.createTypedLiteral(true)));
		assertFalse(RdfUtils.getBoolean(model.createTypedLiteral(false)));
		assertTrue(RdfUtils.getBoolean(model.createTypedLiteral(1)));
		assertFalse(RdfUtils.getBoolean(model.createTypedLiteral(0)));
		assertTrue(RdfUtils.getBoolean(model.createTypedLiteral("1")));
		assertFalse(RdfUtils.getBoolean(model.createTypedLiteral("0")));
		assertTrue(RdfUtils.getBoolean(model.createLiteral("true")));
		assertFalse(RdfUtils.getBoolean(model.createLiteral("false")));
		assertTrue(RdfUtils.getBoolean(model.createLiteral("T")));
		assertFalse(RdfUtils.getBoolean(model.createLiteral("F")));
		assertTrue(RdfUtils.getBoolean(model.createLiteral("yes")));
		assertFalse(RdfUtils.getBoolean(model.createLiteral("no")));
		assertTrue(RdfUtils.getBoolean(model.createLiteral("y")));
		assertFalse(RdfUtils.getBoolean(model.createLiteral("n")));
	}
	
	@Test
	public void testAddNamespacePrefixes()
	{
		Model model = ModelFactory.createDefaultModel();
		RdfUtils.addNamespacePrefixes(model);
		assertEquals("http://sadiframework.org/ontologies/properties.owl#", model.getNsPrefixURI("sadi"));
		assertEquals("http://sadiframework.org/ontologies/predicates.owl#", model.getNsPrefixURI("sadi.old"));
		assertEquals("http://semanticscience.org/resource/", model.getNsPrefixURI("sio"));
	}
}
