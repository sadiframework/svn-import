package org.sadiframework.restrictiontree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.sadiframework.rdfpath.RDFPath;
import org.sadiframework.utils.LabelUtils;
import org.sadiframework.utils.OwlUtils;

import ca.elmonline.util.swing.lazytree.LazyLoadingTreeModel;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Restriction;

/**
 * 
 * @author Luke McCarthy
 */
public class RestrictionTreeModel extends LazyLoadingTreeModel<RestrictionTreeNode>
{
	private static final Logger log = Logger.getLogger(RestrictionTreeModel.class);
	
	private EventListenerList eventListeners;
	
	/**
	 * Create a RestrictionTreeModel containing the property restrictions
	 * of the specified OWL class.
	 * @param root the OWL class
	 */
	public RestrictionTreeModel(OntClass root)
	{
		this(new RestrictionTreeRootNode(root));
	}
	
	/**
	 * Create a RestrictionTreeModel containing the property restrictions
	 * of the specified OWL class relative to another OWL class.
	 * This is useful, for example, to show the restrictions that a SADI
	 * service adds to its output (i.e.: those restrictions on the output 
	 * OWL class that are not also on the input OWL class...)
	 * @param root
	 * @param relativeTo
	 */
	public RestrictionTreeModel(OntClass root, OntClass relativeTo)
	{
		this(new RestrictionTreeRootNode(root, relativeTo));
	}
	
	private RestrictionTreeModel(RestrictionTreeNode root)
	{
		super(root);
		eventListeners = new EventListenerList();
	}
	
	/* (non-Javadoc)
	 * @see ca.elmonline.util.swing.BasicTreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
	 */
	@Override
	public void valueForPathChanged(TreePath path, Object newValue)
	{
		RestrictionTreeNode node = (RestrictionTreeNode)path.getLastPathComponent();
		node.setSelected((Boolean)newValue);
		fireTreeSelectionEvent(path);
	}
	
	public Collection<RDFPath> getSelectedPaths()
	{
		Collection<RDFPath> selectedPaths = new ArrayList<RDFPath>();
		getSelectedPaths(getRoot(), new RDFPath(), selectedPaths);
		return selectedPaths;
	}
	private void getSelectedPaths(RestrictionTreeNode root, RDFPath rootPath, Collection<RDFPath> selectedPaths)
	{
		log.trace(String.format("checking node %s", root));
		if (root.isSelected()) {
			log.trace("\t...node is selected");
			selectedPaths.add(rootPath);
		}
		if (root.isLoaded()) {
			for (RestrictionTreeNode child: root.getChildren()) {
				RDFPath childPath = new RDFPath(rootPath, child.onProperty, child.valuesFrom);
				getSelectedPaths(child, childPath, selectedPaths);
			}
		}
	}
	
	public void clearSelectedPaths()
	{
		clearSelectedSubtree(getRoot(), new TreePath(getRoot()));
	}
	private void clearSelectedSubtree(RestrictionTreeNode root, TreePath treePath)
	{
		log.trace(String.format("checking node %s", root));
		if (root.isSelected()) {
			log.trace("\t...deselecting node");
			root.setSelected(false);
			fireTreeNodesChanged(treePath);
			fireTreeSelectionEvent(treePath);
		}
		if (root.isLoaded()) {
			for (RestrictionTreeNode child: root.getChildren()) {
				clearSelectedSubtree(child, treePath.pathByAddingChild(child));
			}
		}
	}
	
	public void selectPaths(Collection<RDFPath> paths)
	{
		for (RDFPath path: paths) {
			selectPath(getRoot(), path, new TreePath(getRoot()));
		}
	}
	private void selectPath(RestrictionTreeNode root, RDFPath path, TreePath treePath)
	{
		log.trace(String.format("checking node %s", root));
		if (path.isEmpty()) {
			if (!root.isSelected()) {
				log.trace("\t...selecting node");
				root.setSelected(true);
				fireTreeNodesChanged(treePath);
				fireTreeSelectionEvent(treePath);
			}
		} else {
			for (RestrictionTreeNode child: root.getChildren(true)) {
				if (ObjectUtils.equals(child.onProperty, path.getProperty()) && 
					ObjectUtils.equals(child.valuesFrom, path.getType())) {
					selectPath(child, path.getChildPath(), treePath.pathByAddingChild(child));
				}
			}
		}
	}

	public void addTreeSelectionListener(TreeSelectionListener l)
	{
		eventListeners.add(TreeSelectionListener.class, l);
	}

	public void removeTreeSelectionListener(TreeSelectionListener l)
	{
		eventListeners.remove(TreeSelectionListener.class, l);
	}
	
	public void fireTreeSelectionEvent(TreePath changedPath)
	{
		TreeSelectionEvent event = new TreeSelectionEvent(this, changedPath,
				((RestrictionTreeNode)changedPath.getLastPathComponent()).isSelected(), null, null);
		for (TreeSelectionListener l: eventListeners.getListeners(TreeSelectionListener.class))
			l.valueChanged(event);
	}
	
	/**
	 * A class representing the root node of the restriction tree.
	 * The root node is special because it represents the class whose
	 * restrictions are being displayed and has no associated property.
	 * 
	 * @author Luke McCarthy
	 */
	protected static class RestrictionTreeRootNode extends RestrictionTreeNode
	{
		private static final Logger log = Logger.getLogger(RestrictionTreeRootNode.class);
		
		OntClass relativeTo;
		
		public RestrictionTreeRootNode(OntClass root)
		{
			super();
			valuesFrom = root;
			restrictionBean.setValuesFromURI(valuesFrom.getURI());
			restrictionBean.setValuesFromLabel(LabelUtils.getLabel(valuesFrom));
		}
		
		public RestrictionTreeRootNode(OntClass root, OntClass relativeTo)
		{
			this(root);
			this.relativeTo = relativeTo;
		}

		@Override
		public List<RestrictionTreeNode> loadChildren()
		{
			if (relativeTo == null)
				return super.loadChildren();
			
			log.trace(String.format("loading children for %s", toString()));
			List<RestrictionTreeNode> children = new ArrayList<RestrictionTreeNode>();
			if (valuesFrom != null && valuesFrom.canAs(OntClass.class)) {
				for (Restriction r: OwlUtils.listRestrictions(valuesFrom.as(OntClass.class), relativeTo)) {
					children.add(new RestrictionTreeNode(r));
				}
			}
			return children;
		}
		
		@Override
		public String toString()
		{
			return restrictionBean.getValuesFromLabel();
		}
	}
}
