package ca.elmonline.util.swing.lazytree;

import java.util.List;

import ca.elmonline.util.swing.BasicTreeModel;

public class LazyLoadingTreeModel<T extends LazyLoadingTreeNode<T>> extends BasicTreeModel<T>
{
	public LazyLoadingTreeModel(T root)
	{
		super(root);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<T> getChildren(Object parent)
	{
		return ((T)parent).getChildren(true);
	}
}
