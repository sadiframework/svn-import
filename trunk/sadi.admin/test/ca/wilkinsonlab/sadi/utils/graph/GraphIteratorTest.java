package ca.wilkinsonlab.sadi.utils.graph;

import java.util.HashSet;
import java.util.Set;

public class GraphIteratorTest {
	
	/** Adjacency list for a test graph.  Nodes are labeled with Integers, edges are unlabelled.
	 * See testgraph.svg and testgraph.png in ca.wilkinsonlab.sadi.utils.graph.resource for a picture. */
	
	protected static Integer[][] testGraph = {
			{ 1, 2, 3 },		// adjacent to node 0
			{ 4, 5 },			// adjacent to node 1
			{ 3, 8 },			// ...
			{ 2, 6, 7 },
			{ },
			{ 8 },
			{ },
			{ },
			{ }
		};
	
	protected static class TestSearchNode extends SearchNode<Integer> {

		public TestSearchNode(Integer node) {
			super(node);
		}
		
		@Override
		public Set<SearchNode<Integer>> getSuccessors() {
			
			Set<SearchNode<Integer>> successors = new HashSet<SearchNode<Integer>>();
			for(Integer successor : GraphIteratorTest.testGraph[getNode()]) {
				successors.add(new TestSearchNode(successor));
			}
			return successors;
		}
		
	}
}
