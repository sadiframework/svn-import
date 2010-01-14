package ca.wilkinsonlab.sadi.utils.graph;

import java.util.HashSet;
import java.util.Set;

public class BoundedBreadthFirstIterator<V> extends BreadthFirstIterator<V> {
	
	protected int maxDepth;
	
	public BoundedBreadthFirstIterator(SearchNode<V> startNode, int maxDepth) {
		this(startNode, null, maxDepth);
		setCurrentNode(new DepthAnnotatedSearchNode<V>(startNode, 0));
	}
	
	public BoundedBreadthFirstIterator(SearchNode<V> startNode, NodeVisitationConstraint<V> nodeVistationConstraint, int maxDepth) {
		super(new DepthAnnotatedSearchNode<V>(startNode, 0), nodeVistationConstraint);
		setMaxDepth(maxDepth);
	}
	
	@Override
	protected boolean nodeIsVisitable(SearchNode<V> node) {
		if(!(node instanceof DepthAnnotatedSearchNode<?>)) {
			throw new RuntimeException("all search nodes must be DepthAnnotatedSearchNode's");
		}
		DepthAnnotatedSearchNode<V> depthNode = (DepthAnnotatedSearchNode<V>)node;
		return !visitedNodes.contains(depthNode) && (depthNode.getDepth() <= getMaxDepth());
	}
	
	@Override 
	protected boolean successorsRetrievedForCurrentNode() {
		// avoid unnecessarily retrieving neighbours when (depth == maxDepth)
		DepthAnnotatedSearchNode<V> depthNode = getCurrentNodeAsDepthAnnotatedNode();
		if(depthNode.getDepth() < getMaxDepth()) {
			return super.successorsRetrievedForCurrentNode();
		} else {
			return true;
		}
	}
	
	protected DepthAnnotatedSearchNode<V> getCurrentNodeAsDepthAnnotatedNode() {
		if(!(getCurrentNode() instanceof DepthAnnotatedSearchNode<?>)) {
			throw new RuntimeException("current node must always be a DepthAnnotatedSearchNode");
		}
		return (DepthAnnotatedSearchNode<V>)getCurrentNode();
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
		
		public DepthAnnotatedSearchNode(SearchNode<V> searchNode, int depth) {
			super(searchNode.getNode());
			setSearchNode(searchNode);
			setDepth(depth);
		}

		public int getDepth() {
			return depth;
		}
		public void setDepth(int depth) {
			this.depth = depth;
		}
		public SearchNode<V> getSearchNode() {
			return searchNode;
		}
		protected void setSearchNode(SearchNode<V> searchNode) {
			this.searchNode = searchNode;
		}
		public Set<SearchNode<V>> getSuccessors() {
			Set<SearchNode<V>> successors = new HashSet<SearchNode<V>>();
			for(SearchNode<V> successor : getSearchNode().getSuccessors()) {
				successors.add(new DepthAnnotatedSearchNode<V>(successor, getDepth() + 1));
			}
			return successors;
		}
		
	}
}
