package ca.wilkinsonlab.sadi.restrictiontree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.swing.tree.TreePath;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.stringtree.json.JSONWriter;

import ca.wilkinsonlab.sadi.beans.RestrictionBean;
import ca.wilkinsonlab.sadi.rdfpath.RDFPath;
import ca.wilkinsonlab.sadi.rdfpath.RDFPathElement;

/**
 * 
 * @author Luke McCarthy
 */
public class RestrictionTreeUtils
{
	private static final Logger log = Logger.getLogger(RestrictionTreeUtils.class);
	
	public static RDFPath convertTreePathToRDFPath(TreePath treePath)
	{
		RDFPath path = new RDFPath();
		// i=1 because we ignore the root node...
		for (int i=1; i<treePath.getPathCount(); ++i) {
			RestrictionTreeNode node = (RestrictionTreeNode)treePath.getPathComponent(i);
			path.add(new RDFPathElement(node.onProperty, node.valuesFrom));
		}
		return path;
	}
	
	public static TreePath convertRDFPathToTreePath(RestrictionTreeNode root, RDFPath path)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static String toJSON(RestrictionTreeModel model)
	{
		return new JSONWriter(false).write(new RestrictionTreeJSON(model));
	}
	
	public static class RestrictionTreeJSON
	{	
		private List<RestrictionTreeNodeJSON> nodesJSON;
		private List<String> selectedPathsJSON;

		private RestrictionTreeJSON(RestrictionTreeModel model)
		{
			nodesJSON = convertNodes(model.getRoot());
			selectedPathsJSON = convertSelectedPaths(model.getSelectedPaths());
		}
		
		private List<RestrictionTreeNodeJSON> convertNodes(RestrictionTreeNode root)
		{
			List<RestrictionTreeNodeJSON> nodesJSON = new ArrayList<RestrictionTreeNodeJSON>();
			Set<Integer> seen = new HashSet<Integer>();
			for (ListIterator<RestrictionTreeNode> i = Arrays.asList(root).listIterator(); i.hasNext(); ) {
				RestrictionTreeNode node = i.next();
				RestrictionTreeNodeJSON nodeJSON = new RestrictionTreeNodeJSON(node);
				nodesJSON.add(nodeJSON);
				for (RestrictionTreeNode child: node.getChildren()) {
					Integer hashCode = new HashCodeBuilder(23, 11)
						.append(child.onProperty)
						.append(child.valuesFrom)
						.toHashCode();
					child.id = hashCode;
					nodeJSON.children.add(child.id);
					if (!seen.contains(hashCode)) {
						seen.add(hashCode);
						i.add(child);
					}
				}
			}
			return nodesJSON;
		}
		
		private List<String> convertSelectedPaths(Collection<RDFPath> paths)
		{
			List<String> pathsJSON = new ArrayList<String>();
			for (RDFPath path: paths) {
				pathsJSON.add(path.toString());
			}
			return pathsJSON;
		}
		
		public List<RestrictionTreeNodeJSON> getNodes()
		{
			return nodesJSON;
		}

		public List<String> getSelectedPaths()
		{
			return selectedPathsJSON;
		}
	}
	
	public static class RestrictionTreeNodeJSON extends RestrictionBean
	{
		private static final long serialVersionUID = 1L;
		
		int id;
		List<Integer> children;
		
		public RestrictionTreeNodeJSON(RestrictionTreeNode node)
		{
			id = node.id;
			children = new ArrayList<Integer>();
			try {
				BeanUtils.copyProperties(this, node.getRestrictionBean());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				setOnPropertyURI(node.getRestrictionBean().getOnPropertyURI());
				setOnPropertyLabel(node.getRestrictionBean().getOnPropertyLabel());
				setValuesFromURI(node.getRestrictionBean().getValuesFromURI());
				setValuesFromLabel(node.getRestrictionBean().getValuesFromLabel());
			}
		}
		
		public int getId() { return id; }
		public List<Integer> getChildren() { return children;}
	}
}
