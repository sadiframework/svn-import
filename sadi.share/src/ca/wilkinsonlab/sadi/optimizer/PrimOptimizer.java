package ca.wilkinsonlab.sadi.optimizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.buffer.PriorityBuffer;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class PrimOptimizer extends StaticOptimizer {
	
	private PredicateStatsDB statsDB;
	final static int INFINITY = -1;
	
	public PrimOptimizer() 
	{
		// Since we're using the optimizer within Pellet code, we can't throw
		// non-runtime exceptions.
		try {
			statsDB = new PredicateStatsDB();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** 
	 * Generate the optimal ordering of the query (triple list) 
	 * using Prim's algorithm for minimum spanning trees.  For
	 * an explanation, see: http://en.wikipedia.org/wiki/Prim%27s_algorithm
	 */
	
	public List<Triple> optimize(List<Triple> triples) 
	{
		
		if(!checkNoVarsInPredicatePositions(triples))
			throw new RuntimeException("SADI queries do not support variables in the predicate position of a triple pattern");

		List<Triple> minimumSpanningTree = new ArrayList<Triple>();
		Set<Node> visitedNodes = new HashSet<Node>();
		AdjacencyList adjacencyList = new AdjacencyList(triples);
		
		/**
		 * Keep track of which edges have been added to the minimum spanning tree.
		 */

		Set<Triple> triplesRemaining = new HashSet<Triple>();
		for(Triple triple : triples) 
			triplesRemaining.add(triple);
		
		/**
		 * Initialize the heap.  Constant nodes are starting nodes,
		 * and so they are given an initial cost of zero.  All other
		 * nodes are initialized to infinity (-1).
		 */ 		

		Map<Node,NodeState> nodeStates = new HashMap<Node,NodeState>();
		Set<Node> nodes = new HashSet<Node>();
				
		for(Triple triple : triples) {
			Node s = triple.getSubject();
			Node o = triple.getObject();
			int sWeight;
			if(s.isConcrete())
				sWeight = 0;
			else
				sWeight = INFINITY;
			NodeState sState = new NodeState();
			sState.setWeight(sWeight);
			sState.setSize(1);
			sState.setEdgeIsForward(true);
			sState.setEdge(null);
			sState.setParent(null);
			nodeStates.put(s, sState);
			nodes.add(s);
			
			int oWeight;
			if(o.isConcrete())
				oWeight = 0;
			else
				oWeight = INFINITY;
			NodeState oState = new NodeState();
			oState.setWeight(oWeight);
			oState.setSize(1);
			oState.setEdgeIsForward(true);
			oState.setEdge(null);
			oState.setParent(null);
			nodeStates.put(o, oState);
			nodes.add(o);
		}
		
		PriorityBuffer heap = new PriorityBuffer(new NodeComparator(nodeStates));
		heap.addAll(nodes);
		
		/**
		 * Run Prim's algorithm.
		 */
		
		while(!heap.isEmpty()) 
		{
			Node latestAddition = (Node)heap.remove();
			visitedNodes.add(latestAddition);
			
			/** 
			 * Add the new edge to the output list of triples.
			 * parent will only be null for the starter nodes (i.e. the constants in the query).
			 */
			NodeState latestAdditionState = nodeStates.get(latestAddition);
			Node parent = latestAdditionState.getParent();
			if(parent != null) {
				Node edge = latestAdditionState.getEdge();
				if(edge == null)
					throw new RuntimeException();
				Triple triple;
				if(latestAdditionState.edgeIsForward())
					triple = new Triple(parent, edge, latestAddition);
				else
					triple = new Triple(latestAddition, edge, parent);
				minimumSpanningTree.add(triple);
				triplesRemaining.remove(triple);
			}
			
			/**
			 * Add the best neighbour of the new node to the heap.
			 * Nodes connected by reversed edges are still considered neighbours.
			 */
			
			List<EdgeNodePair> neighbors = adjacencyList.getAdjacentEdgeNodePairs(latestAddition);
			for(EdgeNodePair edgeNodePair : neighbors) {
				Node neighbor = edgeNodePair.getNode();
				Node neighborEdge = edgeNodePair.getEdge();
				NodeState neighborState = nodeStates.get(neighbor);
				
				if(!visitedNodes.contains(neighbor)) {

					boolean edgeIsForward = edgeNodePair.edgeIsForward();
					int weight;
					try {
						if(edgeIsForward)
							weight = statsDB.getPredicateStat(neighborEdge.toString(), false, true) * latestAdditionState.getSize();
						else
							weight = statsDB.getPredicateStat(neighborEdge.toString(), false, false) * latestAdditionState.getSize();
					}
					catch(Exception e) {
						throw new RuntimeException(e);
					}
					
					if(weight < neighborState.getWeight() || neighborState.getWeight() == INFINITY) {
						int expectedSize;
						try {
							expectedSize = statsDB.getPredicateStat(neighborEdge.toString(), true, edgeNodePair.edgeIsForward());
						}
						catch(Exception e) {
							throw new RuntimeException(e);
						}
						neighborState.setEdge(neighborEdge);
						neighborState.setEdgeIsForward(edgeNodePair.edgeIsForward());
						neighborState.setWeight(weight);
						neighborState.setSize(latestAdditionState.getSize() * expectedSize);
						neighborState.setParent(latestAddition);
						
						// Based on the new weight, update the node's position in the heap
						heap.remove(neighbor);
						heap.add(neighbor);
					}
						
				}
			}
		}
		
		/**
		 * Add in any remaining triples, which were not incorporated into the minimum
		 * spanning tree above.  These edges have "pruning value" because 
		 * they test if two node values in the MST "work together" as part of a 
		 * solution. As such, we want to insert the edges into the query at the 
		 * earliest possible position where both the subject and object are bound.
		 */
		
		List<Triple> outputTriples = minimumSpanningTree;
		
		if(triplesRemaining.size() > 0) {
			
			outputTriples = new ArrayList<Triple>();
			
			Map<Node,Integer> firstVarOccurrence = new HashMap<Node,Integer>();
			int i = 0;
			for(Triple t : minimumSpanningTree) {
				Node s = t.getSubject();
				Node o = t.getObject();
				if(s.isVariable() && !firstVarOccurrence.containsKey(s))
					firstVarOccurrence.put(s, Integer.valueOf(i));
				if(o.isVariable() && !firstVarOccurrence.containsKey(o))
					firstVarOccurrence.put(o, Integer.valueOf(i));
				i++;
			}
			
			Map<Integer, List<Triple>> insertPosition = new HashMap<Integer, List<Triple>>();
			for(Triple t : triplesRemaining) {
				Node s = t.getSubject();
				Node o = t.getObject();
				Integer insertPos;
				if(s.isVariable() && o.isVariable()) 
					insertPos = Math.min(firstVarOccurrence.get(s), firstVarOccurrence.get(o));
				else if(s.isVariable())
					insertPos = firstVarOccurrence.get(s);
				else if(o.isVariable())
					insertPos = firstVarOccurrence.get(o);
				else
					insertPos = 0;
				if(insertPosition.containsKey(insertPos)) {
					insertPosition.get(insertPos).add(t);
				}
				else {
					List<Triple> newList = new ArrayList<Triple>();
					newList.add(t);
					insertPosition.put(insertPos, newList);
				}
			}
			
			for(int j = 0; j < minimumSpanningTree.size(); j++) {
				if(insertPosition.containsKey(Integer.valueOf(j))) {
					for(Triple t : insertPosition.get(j))
						outputTriples.add(t);
				}
				outputTriples.add(minimumSpanningTree.get(j));
			}

		}
			
		return outputTriples;
	}
	
	private boolean checkNoVarsInPredicatePositions(List<Triple> triples) {
		for(Triple triple : triples) {
			if(triple.getPredicate().isVariable())
				return false;
		}
		return true;
	}
	
	/** 
	 * Encapsulates state variables used to keep track of the
	 * minimum cost edge that we have come across so far which 
	 * connects to a particular node ('node'). 
	 **/
	public static class NodeState
	{
		private Node parent = null;
		private Node edge; 
		private boolean edgeIsForward; 
		private int weight;
		private int size;
		
		public NodeState() 
		{
			setSize(1);
			setWeight(INFINITY);
			setEdgeIsForward(true);
			setParent(null);
			setEdge(null);
		}

		public int getSize() { return size; }
		public void setSize(int size) { this.size = size; }
		public int getWeight() { return weight; }
		public void setWeight(int weight) { this.weight = weight; }
		public boolean edgeIsForward() { return edgeIsForward; }
		public void setEdgeIsForward(boolean edgeIsForward) { this.edgeIsForward = edgeIsForward; }
		public Node getParent() { return parent; }
		public void setParent(Node parent) { this.parent = parent; }
		public Node getEdge() { return edge; }
		public void setEdge(Node edge) { this.edge = edge; }
	}
	
	public static class NodeComparator implements Comparator<Node>
	{
		private Map<Node,NodeState> nodeStates;
		public NodeComparator(Map<Node,NodeState> nodeStates) { this.nodeStates = nodeStates; }
		public int compare(Node o1, Node o2) {
			NodeState o1State = nodeStates.get(o1);
			NodeState o2State = nodeStates.get(o2);
			// Note, first case includes case when both numbers are INFINITY.
			if(o1State.getWeight() == o2State.getWeight())	
				return 0;
			else if(o2State.getWeight() == INFINITY)
				return -1;
			else if(o1State.getWeight() == INFINITY)
				return 1;
			else if(o1State.getWeight() < o2State.getWeight())
				return -1;
			else
				return 1;
		}
	}
	
	public static class AdjacencyList {

		HashMap<Node,List<EdgeNodePair>> adjacencyList; 
		
		public AdjacencyList(List<Triple> triples) 
		{
			adjacencyList = new HashMap<Node,List<EdgeNodePair>>();
			for(Triple triple : triples) {
				Node s = triple.getSubject();
				Node p = triple.getPredicate();
				Node o = triple.getObject();

				List<EdgeNodePair> sList;
				if(!adjacencyList.containsKey(s)) {
					sList = new ArrayList<EdgeNodePair>();
					adjacencyList.put(s, sList);
				}
				else
					sList = adjacencyList.get(s);
				sList.add(new EdgeNodePair(p,o,true));
				
				List<EdgeNodePair> oList;
				if(!adjacencyList.containsKey(o)) {
					oList = new ArrayList<EdgeNodePair>();
					adjacencyList.put(o, oList);
				}
				else
					oList = adjacencyList.get(o);
				oList.add(new EdgeNodePair(p,s,false));
			}
		}
		
		public List<EdgeNodePair> getAdjacentEdgeNodePairs(Node node) 
		{
			if(!adjacencyList.containsKey(node))
				throw new IllegalArgumentException("The adjacency list does not contain " + node.toString());
			return adjacencyList.get(node);
		}
		
		
	}	
	
	public static class EdgeNodePair 
	{
		Node edge;
		Node node;
		boolean edgeIsForward;
		
		public EdgeNodePair(Node edge, Node node, boolean edgeIsForward)
		{
			if(edge == null || node == null)
				throw new NullPointerException();
			this.edge = edge;
			this.node = node;
			this.edgeIsForward = edgeIsForward;
		}
		
		public Node getEdge() { return edge; }
		public Node getNode() { return node; }
		public boolean edgeIsForward() { return edgeIsForward; }
	}
	
}
