package ca.wilkinsonlab.sadi.pellet;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.utils.ATermUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;

import aterm.ATermAppl;

public class PelletHelperTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetNodeFromATerm_BlankNode() {
		ATermAppl atermBNode = ATermUtils.makeTermAppl(PelletOptions.BNODE + "111");
		Node jenaBNode = PelletHelper.getNodeFromATerm(atermBNode);
		assertTrue(jenaBNode.isBlank());
	}

	@Test
	public void testGetATermFromNode_BlankNode() {
		Node jenaBNode = NodeCreateUtils.create("_111");
		ATermAppl atermBNode = PelletHelper.getATermFromNode(jenaBNode);
		assertTrue(ATermUtils.isBnode(atermBNode));
	}

}
