package ca.wilkinsonlab.sadi.optimizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.buffer.PriorityBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.PredicateUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.vocab.PredicateStats;
import ca.wilkinsonlab.sadi.vocab.W3C;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.ontology.OntModel;

public class PrimOptimizer extends StaticOptimizer {

	public final static Log log = LogFactory.getLog(PrimOptimizer.class);
	protected final static int VALUE_UNINITIALIZED = -2;
	
	private PredicateStatsDB statsDB;
	
	public PrimOptimizer() 
	{
		// Since we're using the optimizer within Pellet code, we can't throw non-runtime exceptions.
		try {
			statsDB = new PredicateStatsDB();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public List<Triple> optimize(List<Triple> triples, OntModel propertiesModel)
	{
		throw new UnsupportedOperationException("Method not yet implemented");
	}
	
	/**
	 * Return a string representation of a query.
	 * PrimOptimizer cannot use SPARQLStringUtils.getSPARQLQueryString()
	 * in lieu of this method because predicates may be surrounded
	 * by "inv()".  (This is how the optimizer specifies the direction
	 * in which filler edges should be evaluated.)
	 */
	protected String getPatternString(List<Triple> query) 
	{

		StringBuffer buf = new StringBuffer();
		for(Triple t : query) {
			buf.append("\n");
			buf.append(t.toString());
		}
		return buf.toString();
	}
	
	/** 
	 * Return an optimized ordering of the query (triple pattern list) using Prim's algorithm 
	 * for minimum spanning trees.  For a description of the algorithm, see: 
	 * http://en.wikipedia.org/wiki/Prim%27s_algorithm. 
	 *
	 */
	
	public List<Triple> optimize(List<Triple> query, OntModel propertiesModel, AdjacencyList adjacencyList)
	{
		if(!checkNoVarsInPredicatePositions(query))
			throw new RuntimeException("SADI queries do not support variables in the predicate position of a triple pattern");

		log.debug("Original query: " + getPatternString(query));
		
		// The estimated number of bindings for each variable in the query (computed by getPrimMST())
		Map<Node,Integer> numBindings = new HashMap<Node,Integer>();
		List<Triple> MST = getPrimMST(query, adjacencyList, numBindings);
		List<Triple> optimizedQuery = insertFillerEdges(query, MST, adjacencyList, propertiesModel, numBindings);
		
		return optimizedQuery;
	}
	
	private List<Triple> getPrimMST(List<Triple> query, AdjacencyList adjacencyList, Map<Node,Integer> numBindings)
	{
		List<Triple> MST = new ArrayList<Triple>();
		Set<Node> visitedNodes = new HashSet<Node>();
		
		/*
		 * Initialize the heap.  Constant nodes are starting nodes,
		 * and so they are given an initial cost of zero.  All other
		 * nodes are assigned VALUE_UNINITIALIZED.
		 */ 		

		Map<Node,NodeState> nodeStates = new HashMap<Node,NodeState>();
		Set<Node> nodes = new HashSet<Node>();
				
		for(Triple triple : query) {
			Node s = triple.getSubject();
			Node o = triple.getObject();
			int sWeight;
			if(s.isConcrete())
				sWeight = 0;
			else {
				// we must differentiate between a weight that is INFINITY
				// (indicating an extremely undesirable edge) and a weight
				// that is uninitialized.
				sWeight = VALUE_UNINITIALIZED;
			}
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
			else {
				// we must differentiate between a weight that is INFINITY
				// (indicating an extremely undesirable edge) and a weight
				// that is uninitialized.
				oWeight = VALUE_UNINITIALIZED;
			}
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
		
		/*
		 * Run Prim's algorithm.
		 */
		
		while(!heap.isEmpty()) 
		{
			Node newNode = (Node)heap.remove();
			visitedNodes.add(newNode);
			
			/* 
			 * Add the new edge to the output list of triples.
			 * parent will only be null for the starter nodes (i.e. the constants in the query).
			 */
			NodeState newNodeState = nodeStates.get(newNode);
			Node parent = newNodeState.getParent();
			if(parent != null) {
				Node edge = newNodeState.getEdge();
				if(edge == null)
					throw new RuntimeException("Unexpected null edge pointer; likely an error during construction of adjacency list");

				Triple origTriple; 
				
				if(newNodeState.edgeIsForward())
					origTriple = new Triple(parent, edge, newNode);
				else 
					origTriple = new Triple(newNode, edge, parent);
				MST.add(origTriple);
			}
			
			/*
			 * Add the best neighbour of the new node to the heap.
			 * Nodes connected by reversed edges are still considered neighbours.
			 */
			List<EdgeNodePair> neighbors = adjacencyList.getAdjacentEdgeNodePairs(newNode);
			for(EdgeNodePair edgeNodePair : neighbors) {
				
				Node neighbor = edgeNodePair.getNode();
				Node neighborEdge = edgeNodePair.getEdge();
				NodeState neighborState = nodeStates.get(neighbor);

				if(!visitedNodes.contains(neighbor)) {

					boolean edgeIsForward = edgeNodePair.edgeIsForward();
					int weight;
					try {
						weight = getCost(newNodeState.getSize(), neighborEdge, edgeIsForward);
					}
					catch(Exception e) {
						throw new RuntimeException(e);
					}
					
					//if(weight < neighborState.getWeight() || neighborState.getWeight() == PredicateStats.INFINITY) {
					if(NodeComparator.compareWeights(weight, neighborState.getWeight()) < 0) {
						
						int expectedSize;
						try {
							expectedSize = statsDB.getPredicateStat(neighborEdge.toString(), true, edgeIsForward);
						}
						catch(Exception e) {
							throw new RuntimeException(e);
						}
						neighborState.setEdge(neighborEdge);
						neighborState.setEdgeIsForward(edgeNodePair.edgeIsForward());
						neighborState.setWeight(weight);
						neighborState.setSize(newNodeState.getSize() * expectedSize);
						neighborState.setParent(newNode);
						
						// based on the new weight, update the node's position in the heap
						heap.remove(neighbor);
						heap.add(neighbor);
					}

				}
			}
		}

		/*
		 * remember the estimated number of bindings for each variable, for use in insertFillerEdges
		 */
		for(Node n: nodeStates.keySet()) 
			numBindings.put(n, nodeStates.get(n).getSize());

		log.debug("Query for minimum spanning tree: " + getPatternString(MST));
		
		return MST;
	}
	
	/**
	 * Add in any remaining triples, which were not incorporated into the minimum
	 * spanning tree above.  These edges have "pruning value" because 
	 * they test if two node values in the MST "work together" as part of a 
	 * solution. As such, we want to insert the edges into the query at the 
	 * earliest possible position where both the subject and object are bound.
	 */
	private List<Triple> insertFillerEdges(List<Triple> query, List<Triple> MST, AdjacencyList adjacencyList, OntModel propertiesModel, Map<Node,Integer> numBindings)
	{

		/* Make a list of all edges that were not included in the MST ("filler edges")  */
		
		List<Triple> fillerEdges = new ArrayList<Triple>();
		Set<Triple> inMST = new HashSet<Triple>();

		for(Triple t: MST) 
			inMST.add(t);
		
		for(Triple t: query) {
			if(!inMST.contains(t))
				fillerEdges.add(t);
		}
			
		/* Insert the filler edges into the MST */
		
		List<Triple> outputTriples = MST;
		
		if(fillerEdges.size() > 0) {
			
			outputTriples = new ArrayList<Triple>();
			
			// For each variable, record the index of the first triple in the MST where it occurs.
			
			Map<Node,Integer> firstVarOccurrence = new HashMap<Node, Integer>();
			int i = 0;
			for(Triple t : MST) {
				Node s = t.getSubject();
				Node o = t.getObject();
				if(s.isVariable() && !firstVarOccurrence.containsKey(s))
					firstVarOccurrence.put(s, Integer.valueOf(i));
				if(o.isVariable() && !firstVarOccurrence.containsKey(o))
					firstVarOccurrence.put(o, Integer.valueOf(i));
				i++;
			}
			
			/* Decide the position where each filler edge should be inserted.   In general,
			 * we want to insert the edge at the earliest possible position for which all
			 * of its variables have already been bound.
			 */
			
			Map<Integer, List<Triple>> insertPosition = new HashMap<Integer, List<Triple>>();
			for(Triple t : fillerEdges) {

				Node s = t.getSubject();
				Node o = t.getObject();
				Integer insertPos = MST.size();
//				boolean sIsBound = firstVarOccurrence.containsKey(s);
//				boolean oIsBound = firstVarOccurrence.containsKey(o);
				boolean sIsVar = s.isVariable();
				boolean oIsVar = o.isVariable();
				
				/* The first case can happen if a SADI service outputs an RDF graph
				 * with depth greater than 1, and there are triple patterns used to
				 * query that data. 
				 */
				
//				if((sIsVar && !sIsBound) || (oIsVar && !oIsBound))
//					insertPos = MST.size();
//				else {
					if(sIsVar && oIsVar)
						insertPos = Math.max(firstVarOccurrence.get(s), firstVarOccurrence.get(o)) + 1;
					else if(sIsVar)
						insertPos = firstVarOccurrence.get(s) + 1;
					else if(oIsVar)
						insertPos = firstVarOccurrence.get(o);
//				}
				
				if(insertPosition.containsKey(insertPos)) {
					insertPosition.get(insertPos).add(t);
				}
				else {
					List<Triple> newList = new ArrayList<Triple>();
					newList.add(t);
					insertPosition.put(insertPos, newList);
				}
			}
			
			for(int j = 0; j <= MST.size(); j++) {
				
				if(insertPosition.containsKey(Integer.valueOf(j))) {
					for(Triple t : insertPosition.get(j)) {

						// Reverse the original triple, if it will result in less service calls.

						Node s = t.getSubject();
						Node p = t.getPredicate();
						Node o = t.getObject();
						String pStr = t.getPredicate().toString();

						int forwardCost;
						int reverseCost;
						
						try {
							forwardCost = getCost(numBindings.get(s), p, true);
							reverseCost = getCost(numBindings.get(o), p, false);
						}
						catch(Exception e) {
							throw new RuntimeException(e);
						}
						
						boolean hasInverse = //adjacencyList.hasNode(o) && 
							adjacencyList.hasNeighbor(o, new EdgeNodePair(p,s,false))
							&& !pStr.equals(W3C.PREDICATE_RDF_TYPE) 
							&& (propertiesModel.getObjectProperty(pStr) != null);
						
						if(reverseCost < forwardCost && hasInverse) {
							Node invP = NodeCreateUtils.create(PredicateUtils.invert(pStr));
							outputTriples.add(new Triple(o, invP, s));
						}
						else 
							outputTriples.add(t);
							
					}
				}
				
				if (j < MST.size())
					outputTriples.add(MST.get(j));
			}

		}

		log.debug("Optimized query: " + getPatternString(outputTriples));
		return outputTriples;
	}
	
	private int getCost(int numInvocations, Node edge, boolean edgeIsForward) throws IOException
	{
		return statsDB.getPredicateStat(edge.toString(), false, edgeIsForward) * numInvocations;
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
			setWeight(PredicateStats.INFINITY);
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
			return NodeComparator.compareWeights(nodeStates.get(o1).getWeight(), nodeStates.get(o2).getWeight());
		}
		
		static public int compareWeights(int weight1, int weight2) 
		{
			// Note, first case includes case when both numbers are INFINITY, or both numbers are VALUE_UNINITIALIZED.
			if(weight1 == weight2)	
				return 0;
			else if(weight2 == VALUE_UNINITIALIZED) 
				return -1;
			else if(weight1 == VALUE_UNINITIALIZED) 
				return 1;
			else if(weight2 == PredicateStats.INFINITY) 
				return -1;
			else if(weight1 == PredicateStats.INFINITY)
				return 1;
			else if(weight1 < weight2)
				return -1;
			else
				return 1;
	
		}
	}
	
	public static class AdjacencyList {

		HashMap<Node,List<EdgeNodePair>> adjacencyList; 
		
		public AdjacencyList()
		{
			adjacencyList = new HashMap<Node,List<EdgeNodePair>>();
		}
		
		public AdjacencyList(List<Triple> triples) 
		{
			buildAdjacencyListFromTriples(triples);
		}

		private void buildAdjacencyListFromTriples(List<Triple> triples) 
		{
			adjacencyList = new HashMap<Node,List<EdgeNodePair>>();
			
			for(Triple triple : triples) {
				Node s = triple.getSubject();
				Node p = triple.getPredicate();
				Node o = triple.getObject();
				
				addNeighbor(s, new EdgeNodePair(p,o,true));
				addNeighbor(o, new EdgeNodePair(s,p,false));
			}
		}
		
		public void addNeighbor(Node node, EdgeNodePair neighbor) 
		{
			List<EdgeNodePair> neighbors;
			if(!hasNode(node)) {
				neighbors = new ArrayList<EdgeNodePair>();
				adjacencyList.put(node, neighbors);
			}
			else
				neighbors = adjacencyList.get(node);
			neighbors.add(neighbor);
		}
		
		public boolean hasNeighbor(Node node, EdgeNodePair neighbor)
		{
			if(!hasNode(node))
				throw new IllegalArgumentException("The node " + node.toString() + " is not in the adjacency list");
			
			for(EdgeNodePair n: adjacencyList.get(node)) {
				if(n.equals(neighbor))
					return true;
			}
			
			return false;
		}
		
		public List<EdgeNodePair> getAdjacentEdgeNodePairs(Node node) 
		{
			if(!hasNode(node))
				return new ArrayList<EdgeNodePair>();
			return adjacencyList.get(node);
		}
		
		public boolean hasNode(Node node) {
			return adjacencyList.containsKey(node);
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

		public boolean equals(Object other) 
		{
			if(other instanceof EdgeNodePair)
			{
				EdgeNodePair edgeNodePair = (EdgeNodePair)other;
				if(getEdge().equals(edgeNodePair.getEdge()) && getNode().equals(edgeNodePair.getNode()))
					return true;
			}
			return false;
		}
		
		public int hashCode()
		{
			// Not very efficient, but safe.
			return (getNode().toString() + getEdge().toString()).hashCode(); 
		}
		
	}
	
}
