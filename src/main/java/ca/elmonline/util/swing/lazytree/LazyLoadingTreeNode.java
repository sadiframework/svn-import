package ca.elmonline.util.swing.lazytree;

import java.util.Collections;
import java.util.List;

import ca.elmonline.util.swing.BasicTreeNode;

public abstract class LazyLoadingTreeNode<T extends LazyLoadingTreeNode<?>> implements BasicTreeNode<T>
{
	private List<T> children;
	
	/**
	 * Returns the children of this node, or the empty list if they
	 * have not yet been loaded.
	 * @return
	 */
	public List<T> getChildren()
	{
		return getChildren(false);
	}
	
	/**
	 * Returns the children of this node, loading them first if necessary.
	 * @param withLoad
	 * @return
	 */
	public List<T> getChildren(boolean withLoad)
	{
		if (!isLoaded()) {
			if (withLoad) {
				children = loadChildren();
			} else {
				return Collections.emptyList();
			}
		}
		return children;
	}

	/**
	 * Returns true if the children of this node have been loaded.
	 * @return true if the children of this node have been loaded.
	 */
	public boolean isLoaded()
	{
		return children != null;
	}
	
	/**
	 * Load the children of this node.
	 * @return the loaded children
	 */
	public abstract List<T> loadChildren();
}
