package ca.wilkinsonlab.sadi.utils.graph;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;


public class BoundedBreathFirstIteratorTest extends GraphIteratorTest {

	protected static final Logger log = Logger.getLogger( BoundedBreathFirstIteratorTest.class );

	@Test
	public void testBreadthFirstIterator() {
		
		// keeps track of the order of iteration over the nodes
		Map<Integer,Integer> position = new HashMap<Integer,Integer>();
		
		// Nodes in the test graph are labelled with Integers; we choose 0 as the starting node.
		// Limit traversal to a depth of 1. (Node: the starting node has depth 0.)
		Iterator<Integer> i = new BoundedBreadthFirstIterator<Integer>(new TestSearchNode(0), 1);
		
		int curPosition = 0;

		while(i.hasNext()) {
			Integer currentNode = i.next();
			position.put(currentNode, curPosition++);
		}
		
		// make sure no level 2 nodes were visited
		assertTrue(!position.containsKey(4));
		assertTrue(!position.containsKey(5));
		assertTrue(!position.containsKey(6));
		assertTrue(!position.containsKey(7));
		assertTrue(!position.containsKey(8));
		
		// check for correct ordering of iteration
		assertTrue(position.get(0) < position.get(1));
		assertTrue(position.get(0) < position.get(2));
		assertTrue(position.get(0) < position.get(3));
		
	}
	
}
