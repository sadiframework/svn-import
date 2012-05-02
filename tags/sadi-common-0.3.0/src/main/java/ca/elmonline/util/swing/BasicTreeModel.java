package ca.elmonline.util.swing;

import java.util.List;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class BasicTreeModel<T extends BasicTreeNode<T>> implements TreeModel
{
	private T root;
	private EventListenerList eventListeners;
	
	/**
	 * Construct a new BasicTreeModel with the specified root.
	 * @param root
	 */
	public BasicTreeModel(T root)
	{
		this.root = root;
		eventListeners = new EventListenerList();
	}
	
	/**
	 * Set the new root of the tree.
	 * @param root
	 */
	public void setRoot(T root)
	{
		this.root = root;
		TreePath newRootPath = new TreePath(root);
		TreeModelEvent event = new TreeModelEvent(this, newRootPath);
		// TODO fire tree model events as appropriate...
		for (TreeModelListener listener: getTreeModelListeners()) {
			listener.treeStructureChanged(event);
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getRoot()
	 */
	@Override
	public T getRoot()
	{
		return root;
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
	 */
	@Override
	public boolean isLeaf(Object node)
	{
		return getChildren(node).isEmpty();
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
	 */
	@Override
	public int getChildCount(Object parent)
	{
		return getChildren(parent).size();
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
	 */
	@Override
	public T getChild(Object parent, int index)
	{
		return getChildren(parent).get(index);
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int getIndexOfChild(Object parent, Object child)
	{
		return getChildren(parent).indexOf(child);
	}
	
	@SuppressWarnings("unchecked")
	protected List<T> getChildren(Object parent)
	{
		return ((T)parent).getChildren();
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
	 */
	@Override
	public void valueForPathChanged(TreePath path, Object newValue)
	{
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
	 */
	@Override
	public void addTreeModelListener(TreeModelListener l)
	{
		eventListeners.add(TreeModelListener.class, l);
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
	 */
	@Override
	public void removeTreeModelListener(TreeModelListener l)
	{
		eventListeners.remove(TreeModelListener.class, l);
	}

	protected TreeModelListener[] getTreeModelListeners() {
		return eventListeners.getListeners(TreeModelListener.class);
	}
	
	protected void fireTreeStructureChanged(TreeModelEvent event)
	{
		for (TreeModelListener l: getTreeModelListeners())
			l.treeStructureChanged(event);
	}
	
	protected void fireTreeStructureChanged(TreePath changedPath)
	{
		fireTreeStructureChanged(new TreeModelEvent(this, changedPath));
	}
	
	protected void fireTreeNodesChanged(TreeModelEvent event)
	{
		for (TreeModelListener l: getTreeModelListeners())
			l.treeNodesChanged(event);
	}
	
	protected void fireTreeNodesChanged(TreePath changedPath)
	{
		fireTreeNodesChanged(new TreeModelEvent(this, changedPath));
	}

	protected void fireTreeNodesInserted(TreeModelEvent event)
	{
		for (TreeModelListener l: getTreeModelListeners())
			l.treeNodesInserted(event);
	}

	protected void fireTreeNodesRemoved(TreeModelEvent event)
	{
		for (TreeModelListener l: getTreeModelListeners())
			l.treeNodesRemoved(event);
	}
}
