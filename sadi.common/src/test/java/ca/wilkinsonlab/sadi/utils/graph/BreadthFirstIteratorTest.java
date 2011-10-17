package ca.wilkinsonlab.sadi.utils.graph;

import static org.junit.Assert.*;
import ca.wilkinsonlab.sadi.utils.graph.OpenGraphIterator.NodeVisitationConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

public class BreadthFirstIteratorTest extends GraphIteratorTest {

	protected static final Logger log = Logger.getLogger( BreadthFirstIteratorTest.class );

	@Test
	public void testBreadthFirstIterator() {
		
		// keeps track of the order of iteration over the nodes
		Map<Integer,Integer> position = new HashMap<Integer,Integer>();
		
		// nodes in the test graph are labelled with Integers; we choose 0 as the starting node.
		Iterator<Integer> i = new BreadthFirstIterator<Integer>(new TestSearchNode(0));
		
		int curPosition = 0;
		int node2VisitCount = 0;
		int node8VisitCount = 0;
		
		while(i.hasNext()) {
			Integer currentNode = i.next();
			if(currentNode == 2) {
				node2VisitCount++;
			} else if(currentNode == 8) {
				node8VisitCount++;
			}
			position.put(currentNode, curPosition++);
		}
		
		// node 3 should not be visited more than once
		assertTrue(node2VisitCount == 1);
		// node 3 should not be visited more than once
		assertTrue(node8VisitCount == 1);
		
		// check for correct ordering of iteration
		assertTrue(position.get(0) < position.get(1));
		assertTrue(position.get(0) < position.get(2));
		assertTrue(position.get(0) < position.get(3));
		assertTrue(position.get(0) < position.get(4));
		assertTrue(position.get(0) < position.get(5));
		assertTrue(position.get(0) < position.get(6));
		assertTrue(position.get(0) < position.get(7));
		assertTrue(position.get(0) < position.get(8));
		
		assertTrue(position.get(1) < position.get(4));
		assertTrue(position.get(1) < position.get(5));
		assertTrue(position.get(1) < position.get(6));
		assertTrue(position.get(1) < position.get(7));
		assertTrue(position.get(1) < position.get(8));

		assertTrue(position.get(2) < position.get(4));
		assertTrue(position.get(2) < position.get(5));
		assertTrue(position.get(2) < position.get(6));
		assertTrue(position.get(2) < position.get(7));
		assertTrue(position.get(2) < position.get(8));
		
		assertTrue(position.get(3) < position.get(4));
		assertTrue(position.get(3) < position.get(5));
		assertTrue(position.get(3) < position.get(6));
		assertTrue(position.get(3) < position.get(7));
		assertTrue(position.get(3) < position.get(8));
		
	}

	@Test
	public void testBreadthFirstIteratorWithMultipleStartNodes() {
		
		// keeps track of the order of iteration over the nodes
		Map<Integer,Integer> position = new HashMap<Integer,Integer>();
		
		// nodes in the test graph are labelled with Integers; we choose nodes 1, 2, 3 as the starting nodes.
		Collection<SearchNode<Integer>> startNodes = new ArrayList<SearchNode<Integer>>();
		startNodes.add(new TestSearchNode(1));
		startNodes.add(new TestSearchNode(2));
		startNodes.add(new TestSearchNode(3));
		
		Iterator<Integer> i = new BreadthFirstIterator<Integer>(startNodes);
		
		int curPosition = 0;
		//  track the number of times each node is visited (should be at most once!)
		Map<Integer,Integer> visitCount = new HashMap<Integer,Integer>();

		while(i.hasNext()) {
			Integer currentNode = i.next();
			int count = visitCount.containsKey(currentNode) ? visitCount.get(currentNode) : 1;
			visitCount.put(currentNode, count);
			position.put(currentNode, curPosition++);
		}
		
		// make sure all nodes are visited exactly once (except 0, which is unreachable from the chosen start nodes)
		for(int node = 1; node <= 8; node++) {
			assertTrue(visitCount.get(node) == 1);
		}
		assertTrue(!visitCount.containsKey(0));
		
		// check for correct ordering of iteration
		
		assertTrue(position.get(1) < position.get(4));
		assertTrue(position.get(1) < position.get(5));
		assertTrue(position.get(1) < position.get(6));
		assertTrue(position.get(1) < position.get(7));
		assertTrue(position.get(1) < position.get(8));

		assertTrue(position.get(2) < position.get(4));
		assertTrue(position.get(2) < position.get(5));
		assertTrue(position.get(2) < position.get(6));
		assertTrue(position.get(2) < position.get(7));
		assertTrue(position.get(2) < position.get(8));
		
		assertTrue(position.get(3) < position.get(4));
		assertTrue(position.get(3) < position.get(5));
		assertTrue(position.get(3) < position.get(6));
		assertTrue(position.get(3) < position.get(7));
		assertTrue(position.get(3) < position.get(8));
		
	}
	
	@Test
	public void testBreadthFirstIteratorWithConstraint() {
		
		// keeps track of the order of iteration over the nodes
		Map<Integer,Integer> position = new HashMap<Integer,Integer>();
		
		// don't visit even nodes, except 0.
		NodeVisitationConstraint<Integer> constraint = new NodeVisitationConstraint<Integer>(){
				@Override
				public boolean isVisitable(Integer node) {
					if(node == 0 || node % 2 == 1) {
						return true;
					}
					return false;
				}
				public void visit(Integer node) {}
			};
		
		// nodes in the test graph are labelled with Integers; we choose 0 as the starting node.
		Iterator<Integer> i = new BreadthFirstIterator<Integer>(new TestSearchNode(0), constraint);
		
		int curPosition = 0;
		
		while(i.hasNext()) {
			Integer currentNode = i.next();
			position.put(currentNode, curPosition++);
		}
		
		// check that the even nodes were skipped (except starting node 0)
		assertTrue(!position.containsKey(2));
		assertTrue(!position.containsKey(6));
		assertTrue(!position.containsKey(8));
		
		// check for correct ordering of iteration
		assertTrue(position.get(0) < position.get(1));
		assertTrue(position.get(0) < position.get(3));
		assertTrue(position.get(0) < position.get(5));
		assertTrue(position.get(0) < position.get(7));
		
		assertTrue(position.get(1) < position.get(5));
		assertTrue(position.get(1) < position.get(7));

		assertTrue(position.get(3) < position.get(5));
		assertTrue(position.get(3) < position.get(7));
		
	}
}
