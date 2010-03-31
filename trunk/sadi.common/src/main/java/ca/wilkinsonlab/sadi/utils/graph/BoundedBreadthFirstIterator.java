package ca.wilkinsonlab.sadi.utils.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BoundedBreadthFirstIterator<V> extends BreadthFirstIterator<V> {
	
	protected int maxDepth;
	
	public BoundedBreadthFirstIterator(SearchNode<V> startNode, int maxDepth) {
		this(Collections.singleton(startNode), null, maxDepth);
	}
	
	public BoundedBreadthFirstIterator(Collection<? extends SearchNode<V>> startNodes, int maxDepth) {
		this(startNodes, null, maxDepth);
	}
	
	public BoundedBreadthFirstIterator(SearchNode<V>startNode, NodeVisitationConstraint<V> nodeVistationConstraint, int maxDepth) {
		this(Collections.singleton(startNode), nodeVistationConstraint, maxDepth);
	}
	
	public BoundedBreadthFirstIterator(Collection<? extends SearchNode<V>> startNodes, NodeVisitationConstraint<V> nodeVistationConstraint, int maxDepth) 
	{
		super(startNodes, nodeVistationConstraint);

		// This is ugly.  I want to wrap the given startNodes in DepthAnnotatedNodes, but I'm not allowed to do that before 
		// calling the superclass constructor.  So instead I overwrite the BFSQueue.

		Collection<DepthAnnotatedSearchNode<V>> depthAnnotatedNodes = new ArrayList<DepthAnnotatedSearchNode<V>>();
		for(SearchNode<V> startNode : startNodes) {
			depthAnnotatedNodes.add(new DepthAnnotatedSearchNode<V>(startNode, 0, maxDepth));
		}
		getQueue().clear();
		getQueue().addAll(depthAnnotatedNodes);
		
		setMaxDepth(maxDepth);
	}
	
	@Override
	protected boolean nodeIsVisitable(SearchNode<V> node) {
		if(!(node instanceof DepthAnnotatedSearchNode<?>)) {
			throw new RuntimeException("all search nodes must be DepthAnnotatedSearchNode's");
		}
		DepthAnnotatedSearchNode<V> asDepthNode = (DepthAnnotatedSearchNode<V>)node;
		if(asDepthNode.getDepth() <= getMaxDepth()) {
			return super.nodeIsVisitable(node);
		} 
		return false;
	}
	
	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	protected static class DepthAnnotatedSearchNode<V> extends SearchNode<V> {

		protected SearchNode<V> searchNode;
		protected int depth;
		protected int maxDepth;
		
		public DepthAnnotatedSearchNode(SearchNode<V> searchNode, int depth, int maxDepth) 
		{
			super(searchNode.getNode());
			setSearchNode(searchNode);

			if(depth > maxDepth) {
				throw new IllegalArgumentException("current depth exceeds maximum depth of traversal");
			}
			setDepth(depth);
			setMaxDepth(maxDepth);
		}

		public int getDepth() {
			return depth;
		}
		public void setDepth(int depth) {
			this.depth = depth;
		}
		public int getMaxDepth() {
			return maxDepth;
		}
		public void setMaxDepth(int maxDepth) {
			this.maxDepth = maxDepth;
		}
		public SearchNode<V> getSearchNode() {
			return searchNode;
		}
		protected void setSearchNode(SearchNode<V> searchNode) {
			this.searchNode = searchNode;
		}

		public Set<SearchNode<V>> getSuccessors() {
			Set<SearchNode<V>> successors = new HashSet<SearchNode<V>>();
			if(getDepth() < getMaxDepth()) {
				for(SearchNode<V> successor : getSearchNode().getSuccessors()) {
					successors.add(new DepthAnnotatedSearchNode<V>(successor, getDepth() + 1, getMaxDepth()));
				}
			}
			return successors;
		}
		
	}
}
