package ca.elmonline.util.swing;

import java.util.List;

public interface BasicTreeNode<T extends BasicTreeNode<?>>
{
	List<T> getChildren();
}
