package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class RdfUtilsTest
{
	@Test
	public void testCreateTypedLiteral()
	{
		double doubleAsDouble = 0.1;
		Literal doubleAsLiteral = ResourceFactory.createTypedLiteral(doubleAsDouble);
		String doubleLiteralAsString = doubleAsLiteral.toString();
		Literal convertedLiteral = RdfUtils.createTypedLiteral(doubleLiteralAsString);
		assertEquals(convertedLiteral, doubleAsLiteral);
	}
}
