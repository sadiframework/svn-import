package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;

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
	
	@Test
	public void testCopyValues()
	{
		Model fromModel = ModelFactory.createDefaultModel();
		Resource from = fromModel.createResource();
		from.addProperty(RDF.type, FOAF.Person);
		from.addProperty(FOAF.name, "Guy Incognito");
		Resource jj = fromModel.createResource("http://example.com/jj", FOAF.Person);
		from.addProperty(FOAF.knows, jj);
		jj.addProperty(FOAF.name, "Joey Joe Joe Jr. Shabadoo");
		
		Model toModel = ModelFactory.createDefaultModel();
		Resource to = toModel.createResource();
//		RdfUtils.copyValues(from, to, true);
//		if (!fromModel.isIsomorphicWith(toModel)) {
////			ModelDiff diff = ModelDiff.diff(fromModel, toModel);
////			System.out.println("In from, not to:\n" + RdfUtils.logStatements(diff.inXnotY));
////			System.out.println("In to, not from:\n" + RdfUtils.logStatements(diff.inYnotX));
//			fail("models are not isomorphic");
//		}
		to.addProperty(FOAF.name, "Max Power");
		RdfUtils.copyValues(from, to, false);
		assertTrue("destination resource did not receive rdf:type property",
		           to.hasProperty(RDF.type, FOAF.Person));
		assertFalse("destination resource should not have new name",
		            to.hasProperty(FOAF.name, "Guy Incognito"));
		Resource toJJ = to.getPropertyResourceValue(FOAF.knows);
		assertNotNull("destination resource did not receive foaf:knows property", toJJ);
		assertTrue("destination resource did not receive foaf:name property",
		           toJJ.hasProperty(FOAF.name, "Joey Joe Joe Jr. Shabadoo"));
	}
}