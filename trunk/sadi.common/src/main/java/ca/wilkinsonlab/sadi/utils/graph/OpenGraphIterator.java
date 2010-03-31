package ca.wilkinsonlab.sadi.utils.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** 
 * <p>Provides a graph traversal algorithm (e.g. breadth first traversal) in the form
 * of an iterator.  An OpenGraphIterator differs from iterators in other graph algorithm 
 * libraries (e.g. JGraphT, JUNG) because it does not need to know about all vertices 
 * and edges in the graph up front. Instead, the graph can be expanded as it is traversed.  
 * The user provides a starting node which is a subclass of SearchNode; the SearchNode has
 * a method called getSuccessors() that retrieves all neighbours of the node. Thus, one
 * can use an OpenGraphIterator for traversing graphs under many different scenarios.
 * </p>  
 *
 * <p>Note 1: The Java Search Library works in a similar manner, but the library is copyrighted 
 * by the authors, and so I was unable to use it.</p>
 * 
 * <p>Note 2: OpenGraphIterator's are not thread-safe.</p>
 * 
 * @author Ben Vandervalk
 */
public abstract class OpenGraphIterator<V> implements Iterator<V> {

	Set<SearchNode<V>> visitedNodes = new HashSet<SearchNode<V>>();
	NodeVisitationConstraint<V> nodeVistationConstraint;

	public OpenGraphIterator() {
		this(null);
	}

	public OpenGraphIterator(NodeVisitationConstraint<V> nodeVistationConstraint) {
		setNodeVistationConstraint(nodeVistationConstraint);
	}

	protected void addVisitedNode(SearchNode<V> node) {
		visitedNodes.add(node);
		if(getNodeVistationConstraint() != null) {
			getNodeVistationConstraint().visit(node.getNode());
		}
	}

	protected boolean nodeIsVisitable(SearchNode<V> node) {
		
		if(getNodeVistationConstraint() != null && !getNodeVistationConstraint().isVisitable(node.getNode())) {
			return false;
		}
		return !visitedNodes.contains(node);
	}
	
	public NodeVisitationConstraint<V> getNodeVistationConstraint() {
		return nodeVistationConstraint;
	}

	public void setNodeVistationConstraint(NodeVisitationConstraint<V> nodeVistationConstraint) {
		this.nodeVistationConstraint = nodeVistationConstraint;
	}

	public static interface NodeVisitationConstraint<V> {
		public abstract boolean isVisitable(V node);
		public abstract void visit(V node);
	}

	public static abstract class NodeVisitationConstraintBase<V> implements NodeVisitationConstraint<V> {
		public boolean isVisitable(V node) { return true; }
		public void visit(V node) {}
	}
}
