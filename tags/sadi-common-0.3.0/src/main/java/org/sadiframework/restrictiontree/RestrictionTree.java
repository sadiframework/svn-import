package org.sadiframework.restrictiontree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.ObjectUtils;
import org.sadiframework.SADIException;
import org.sadiframework.rdfpath.RDFPath;
import org.sadiframework.rdfpath.RDFPathElement;
import org.sadiframework.utils.OnymizeUtils;
import org.sadiframework.utils.OwlUtils;


import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * 
 * @author Luke McCarthy
 */
public class RestrictionTree extends JTree
{
	private static final long serialVersionUID = 1L;
	
	public RestrictionTree(RestrictionTreeModel model)
	{
		super();
		RestrictionTreeCellRenderer renderer = new RestrictionTreeCellRenderer();
		setModel(model);
		setCellRenderer(renderer);
		setCellEditor(renderer);
		setEditable(true);
		setSelectionModel(null); // no selection, just check boxes...
		setRootVisible(true);
		putClientProperty("JTree.lineStyle", "None");
	}
	
	public TreePath getTreePath(RDFPath path)
	{
		ArrayList<RestrictionTreeNode> nodes = new ArrayList<RestrictionTreeNode>();
		nodes.add((RestrictionTreeNode)getModel().getRoot());
		for (int i=0; i<path.size(); ++i) {
			RestrictionTreeNode child = getMatchingChild(nodes.get(i), path.get(i));
			if (child == null)
				return null;
			else
				nodes.add(child);
		}
		return new TreePath(nodes.toArray());
	}
	private static RestrictionTreeNode getMatchingChild(RestrictionTreeNode parent, RDFPathElement element)
	{
		for (RestrictionTreeNode child: parent.getChildren(true))
			if (ObjectUtils.equals(element.getProperty(), child.onProperty) &&
				ObjectUtils.equals(element.getType(), child.valuesFrom))
				return child;
		return null;
	}
	
	public static class RestrictionTreeCellRenderer extends AbstractCellEditor implements TreeCellRenderer, TreeCellEditor
	{
		private static final long serialVersionUID = 1L;
		private JCheckBox check;
		
		public RestrictionTreeCellRenderer()
		{
			check = new JCheckBox();
			check.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					stopCellEditing();
				}
			});
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
		 */
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			if (value instanceof RestrictionTreeNode) {
				RestrictionTreeNode node = (RestrictionTreeNode)value;
				check.setSelected(node.isSelected());
				check.setText(node.toString());
			} else {
				// this should never happen...
				check.setSelected(false);
				check.setText(tree.convertValueToText(value, selected, expanded, leaf, row, false));
			}
//			check.setEnabled(tree.isEnabled());
			if (selected) {
				check.setForeground(UIManager.getColor("Tree.selectionForeground"));
				check.setBackground(UIManager.getColor("Tree.selectionBackground"));
			} else {
				check.setForeground(UIManager.getColor("Tree.textForeground"));
				check.setBackground(UIManager.getColor("Tree.textBackground"));
			}
			return check;
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.CellEditor#getCellEditorValue()
		 */
		@Override
		public Object getCellEditorValue()
		{
			return check.isSelected();
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeCellEditor#getTreeCellEditorComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int)
		 */
		@Override
		public Component getTreeCellEditorComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row)
		{
			return getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, true);
		}
	}
	
	public static void main(String[] args) throws SADIException
	{
//		LocationMapper.get().addAltPrefix(
//				"http://sadiframework.org/examples/", 
//				"file:../sadi.service.examples/src/main/webapp/");
//		LocationMapper.get().addAltPrefix(
//				"http://sadiframework.org/ontologies/",
//				"file:/Users/luke/Sites/sadiframework.org/ontologies/");
		final RestrictionTreeModel model = getRestrictionTreeModel(args);
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(model);
            }
        });
	}
	
	private static RestrictionTreeModel getRestrictionTreeModel(String[] args) throws SADIException
	{
		if (args.length < 1)
			throw new SADIException("usage: java RestrictionTree.java ROOT_CLASS [RELATIVE_TO]");
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
		OntClass root = OwlUtils.getOntClassWithLoad(model, args[0]);
		if (root == null)
			throw new SADIException(String.format("no such class %s", args[0]));
		if (args.length > 1) {
			OntClass relativeTo = OwlUtils.getOntClassWithLoad(model, args[1]);
			return new RestrictionTreeModel(root, relativeTo);
		} else {
			return new RestrictionTreeModel(root);
		}
	}
	
	private static void createAndShowGUI(final RestrictionTreeModel model)
	{
		RestrictionTree tree = new RestrictionTree(model);
		model.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				System.out.println(String.format("%s %s",
						e.getPath().getLastPathComponent(),
						e.isAddedPath() ? "selected" : "deselected"));
			}
		});
		final JButton store = new JButton("Store selected nodes");
		final JButton restore = new JButton("Restore selected nodes");
		ActionListener listener = new ActionListener() {
			Collection<RDFPath> storedPaths = new ArrayList<RDFPath>();
			public void actionPerformed(ActionEvent event) {
				if (event.getSource().equals(store)) {
					System.out.println("storing paths...");
					storedPaths.clear();
					for (RDFPath path: model.getSelectedPaths()) {
						try {
							System.out.println("\tbefore onymizing");
							System.out.println(String.format("\t\t%s", path));
							RDFPath onymizedPath = OnymizeUtils.onymizePath(path, "UTF-8");
							System.out.println("\tafter onymizing");
							System.out.println(String.format("\t\t%s", onymizedPath));
							storedPaths.add(onymizedPath);
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					}
				} else if (event.getSource().equals(restore)) {
					System.out.println("restoring paths...");
					OntModel ontModel = (OntModel)model.getRoot().valuesFrom.getModel();
					Collection<RDFPath> pathsInModel = new ArrayList<RDFPath>();
					for (RDFPath path: storedPaths) {
						try {
							RDFPath pathInModel = OnymizeUtils.deonymizePath(ontModel, path, "UTF-8");
							pathsInModel.add(pathInModel);
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					}
					model.clearSelectedPaths();
					model.selectPaths(pathsInModel);
				}
			}
		};
		store.addActionListener(listener);
		restore.addActionListener(listener);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(store);
		buttonPanel.add(restore);
		
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout());
        content.add(new JScrollPane(tree), BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
		
		JFrame frame = new JFrame("RestrictionTree");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(content);
        frame.pack();
        frame.setVisible(true);
	}
}
