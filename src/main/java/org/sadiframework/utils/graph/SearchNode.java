package org.sadiframework.utils.graph;

import java.util.Set;

public abstract class SearchNode<V> {

	protected V node;

	abstract public Set<SearchNode<V>> getSuccessors();

	public SearchNode(V node) {
		setNode(node);
	}
	
	protected void setNode(V node) {
		this.node = node;
	}
	
	public V getNode() {
		return node;
	}
	
	@Override
	public int hashCode() {
		return getNode().hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		
		if(other instanceof SearchNode<?>) {
			SearchNode<?> asSearchNode = (SearchNode<?>)other;
			return getNode().equals(asSearchNode.getNode());
		}
		return false;
	}
}
