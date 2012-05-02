package org.sadiframework.restrictiontree;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.TreePath;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sadiframework.rdfpath.RDFPath;
import org.sadiframework.restrictiontree.RestrictionTreeModel;
import org.sadiframework.restrictiontree.RestrictionTreeNode;
import org.sadiframework.restrictiontree.RestrictionTreeUtils;


import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class RestrictionTreeUtilsTest
{
	private static final String NS = "http://sadiframework.org/ontologies/RestrictionTreeModelTest.owl#";
	
	private static OntModel ontModel;
	private static RestrictionTreeModel treeModel;
	private static Property p;
	private static Resource pRange;
	private static Property q;
	private static Resource qRange;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
		ontModel.read(RestrictionTreeModelTest.class.getResourceAsStream("RestrictionTreeModelTest.owl"), NS);
		treeModel = new RestrictionTreeModel(ontModel.getOntClass(NS + "Root"));
		p = ontModel.getProperty(NS + "p");
		q = ontModel.getProperty(NS + "q");
		pRange = ontModel.getResource(NS + "pRange");
		qRange = ontModel.getResource(NS + "qRange");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		ontModel.close();
		treeModel = null;
		p = null;
		q = null;
		pRange = null;
		qRange = null;
	}

	@Before
	public void setUp() throws Exception
	{
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testConvertTreePathToRDFPath1()
	{
		TreePath treePath = new TreePath(treeModel.getRoot());
		RestrictionTreeModelTest.assertCollectionsAreEquivalent(
				new RDFPath(),
				RestrictionTreeUtils.convertTreePathToRDFPath(treePath));
	}
	
	@Test
	public void testConvertTreePathToRDFPath2()
	{
		List<RestrictionTreeNode> nodes = new ArrayList<RestrictionTreeNode>();
		nodes.add(treeModel.getRoot());
		for (RestrictionTreeNode child: treeModel.getRoot().getChildren(true)) {
			if (child.onProperty.equals(p)) {
				nodes.add(child);
				for (RestrictionTreeNode grandchild: child.getChildren(true)) {
					if (grandchild.onProperty.equals(q)) {
						nodes.add(grandchild);
					}
				}
			}
		}
		TreePath treePath = new TreePath(nodes.toArray());
		RestrictionTreeModelTest.assertCollectionsAreEquivalent(
				new RDFPath(p, pRange, q, qRange),
				RestrictionTreeUtils.convertTreePathToRDFPath(treePath));
	}
}
