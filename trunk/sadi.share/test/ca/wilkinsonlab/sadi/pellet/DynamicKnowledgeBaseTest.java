package ca.wilkinsonlab.sadi.pellet;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

public class DynamicKnowledgeBaseTest {

	DynamicKnowledgeBase kb = new DynamicKnowledgeBase();
	
	final static String PREFIX_PREDICATES = "http://es-01.chibi.ubc.ca/~benv/predicates.owl#";
	final static String PREFIX_DUMONTIER = "http://ontology.dumontierlab.com/";
	
	final static ATermAppl ATERM_A = ATermUtils.makeTermAppl("A");
	final static ATermAppl ATERM_B = ATermUtils.makeTermAppl("B");
	final static ATermAppl ATERM_C = ATermUtils.makeTermAppl("C");
	final static ATermAppl ATERM_D = ATermUtils.makeTermAppl("D");
	
	@Before
	public void setUp() throws Exception {
		
		kb = new DynamicKnowledgeBase();

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetAllEquivalentPropertiesIncludingInverses_withInverseProperty() {
	
		defineTestProperties(kb);
		
		ATermAppl input = ATermUtils.makeInv(ATERM_A);
	
		Set<ATermAppl> synonyms = kb.getAllEquivalentPropertiesIncludingInverses(input);
		
		assertTrue(synonyms.contains(ATermUtils.makeInv(ATERM_A)));
		assertTrue(synonyms.contains(ATermUtils.makeInv(ATERM_B)));
		assertTrue(synonyms.contains(ATERM_C));
		assertTrue(synonyms.contains(ATERM_D));

	}

	@Test
	public void testGetAllEquivalentPropertiesIncludingInverses_withProperty() {
		
		defineTestProperties(kb);
		
		Set<ATermAppl> synonyms = kb.getAllEquivalentPropertiesIncludingInverses(ATERM_A);
		
		assertTrue(synonyms.contains(ATERM_A));
		assertTrue(synonyms.contains(ATERM_B));
		assertTrue(synonyms.contains(ATermUtils.makeInv(ATERM_C)));
		assertTrue(synonyms.contains(ATermUtils.makeInv(ATERM_D)));

	}
	
	@Test
	public void testGetPredicateSynonyms() {
		
		defineTestProperties(kb);
		
		Set<String> synonyms = kb.getPredicateSynonyms(ATERM_A.toString());
		
		assertTrue(synonyms.contains(ATERM_A.toString()));
		assertTrue(synonyms.contains(ATERM_B.toString()));
		
	}
	
	private void defineTestProperties(KnowledgeBase kb)
	{
		kb.addObjectProperty(ATERM_A);
		kb.addObjectProperty(ATERM_B);
		kb.addObjectProperty(ATERM_C);
		kb.addObjectProperty(ATERM_D);
		
		kb.addEquivalentProperty(ATERM_A, ATERM_B);
		kb.addInverseProperty(ATERM_A, ATERM_C);
		kb.addEquivalentProperty(ATERM_C, ATERM_D);
	}

	
}
