package ca.wilkinsonlab.sadi.restrictiontree;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ca.elmonline.util.swing.lazytree.LazyLoadingTreeNode;
import ca.wilkinsonlab.sadi.beans.RestrictionBean;
import ca.wilkinsonlab.sadi.utils.LabelUtils;
import ca.wilkinsonlab.sadi.utils.OwlUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;

/**
 * 
 * @author Luke McCarthy
 */
public class RestrictionTreeNode extends LazyLoadingTreeNode<RestrictionTreeNode>
{
	private static final Logger log = Logger.getLogger(RestrictionTreeNode.class);
	
	OntProperty onProperty;
	OntResource valuesFrom;
	RestrictionBean restrictionBean;
	boolean selected;
	
	int id; // this field is used internally by RestrictionTreeModel...
	
	public RestrictionTreeNode()
	{
		super();
		onProperty = null;
		valuesFrom = null;
		restrictionBean = new RestrictionBean();
		selected = false;
	}
	
	public RestrictionTreeNode(Restriction restriction)
	{
		this();
		onProperty = restriction.getOnProperty();
		valuesFrom = OwlUtils.getValuesFrom(restriction);
		restrictionBean.setOnPropertyURI(onProperty.getURI());
		restrictionBean.setOnPropertyLabel(LabelUtils.getLabel(onProperty));
		if (valuesFrom != null) {
			restrictionBean.setValuesFromURI(valuesFrom.getURI());
			restrictionBean.setValuesFromLabel(LabelUtils.getLabel(valuesFrom));
		}
	}

	public RestrictionBean getRestrictionBean()
	{
		return restrictionBean;
	}

	public boolean isSelected()
	{
		return selected;
	}

	public void setSelected(boolean selected)
	{
		log.trace(String.format("node %s %s", toString(), selected ? "selected" : "deselected"));
		this.selected = selected;
	}
	
	/* (non-Javadoc)
	 * @see ca.elmonline.util.swing.lazytree.LazyLoadingTreeNode#loadChildren()
	 */
	@Override
	public List<RestrictionTreeNode> loadChildren()
	{
		log.trace(String.format("loading children for %s", toString()));
		List<RestrictionTreeNode> children = new ArrayList<RestrictionTreeNode>();
		if (valuesFrom != null && valuesFrom.canAs(OntClass.class)) {
			for (Restriction r: OwlUtils.listRestrictions(valuesFrom.as(OntClass.class))) {
				RestrictionTreeNode child = new RestrictionTreeNode(r);
				/* TODO check for overlapping restrictions and only display
				 * the most specific?  probably not always what you want...
				 */
				children.add(child);
			}
		}
		return children;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getRestrictionBean().toString();
	}
}
