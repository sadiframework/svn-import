package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PredicateUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testIsInverted_withInverted() {
		assertTrue(PredicateUtils.isInverted("inv(http://es-01.chibi.ubc.ca/~benv/predicates.owl#hasName)"));
	}

	@Test
	public void testIsInverted_withNonInverted() {
		assertFalse(PredicateUtils.isInverted("http://es-01.chibi.ubc.ca/~benv/predicates.owl#hasName"));
	}

	@Test
	public void testIsInverted_withDoubleInverted() {
		assertTrue(PredicateUtils.isInverted("inv(inv(http://es-01.chibi.ubc.ca/~benv/predicates.owl#hasName))"));
	}

	@Test
	public void testInvert_withInverted() {
		assertTrue(PredicateUtils.invert("inv(http://es-01.chibi.ubc.ca/~benv/predicates.owl#hasName)").equals(
				"http://es-01.chibi.ubc.ca/~benv/predicates.owl#hasName"));
		
	}

	@Test
	public void testInvert_withNonInverted() {
		assertTrue(PredicateUtils.invert("http://es-01.chibi.ubc.ca/~benv/predicates.owl#hasName").equals(
				"inv(http://es-01.chibi.ubc.ca/~benv/predicates.owl#hasName)"));
		
	}

	@Test
	public void testInvert_withDoubleInverted() {
		assertTrue(PredicateUtils.invert("inv(inv(http://es-01.chibi.ubc.ca/~benv/predicates.owl#hasName))").equals(
				"inv(http://es-01.chibi.ubc.ca/~benv/predicates.owl#hasName)"));
		
	}
	
}
