package ca.wilkinsonlab.sadi.restrictiontree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.rdfpath.RDFPath;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class RestrictionTreeModelTest
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
		treeModel.clearSelectedPaths();
	}

	@After
	public void tearDown() throws Exception
	{
	}
	
	@Test
	public void testClearSelectedPaths()
	{
		treeModel.getRoot().setSelected(true);
		treeModel.clearSelectedPaths();
		assertTrue(treeModel.getSelectedPaths().isEmpty());
	}
	
	@Test
	public void testGetSelectedPaths1()
	{
		treeModel.getRoot().setSelected(true);
		assertCollectionsAreEquivalent(
				Collections.singleton(new RDFPath()), 
				treeModel.getSelectedPaths());
	}
	
	@Test
	public void testGetSelectedPaths2()
	{
		for (RestrictionTreeNode child: treeModel.getRoot().getChildren(true)) {
			if (child.onProperty.equals(p)) {
				for (RestrictionTreeNode grandchild: child.getChildren(true)) {
					if (grandchild.onProperty.equals(q)) {
						grandchild.setSelected(true);
					}
				}
			} else if (child.onProperty.equals(q)) {
				child.setSelected(true);
			}
		}
		assertCollectionsAreEquivalent(
				Arrays.asList(new RDFPath[] {
						new RDFPath(p, pRange, q, qRange),
						new RDFPath(q, qRange)}), 
				treeModel.getSelectedPaths());
	}
	
	@Test
	public void testSelectPaths1()
	{
		Collection<RDFPath> paths = Collections.singleton(new RDFPath());
		treeModel.selectPaths(paths);
		assertCollectionsAreEquivalent(paths, treeModel.getSelectedPaths());
	}
	
	@Test
	public void testSelectPaths2()
	{
		Collection<RDFPath> paths = Arrays.asList(new RDFPath[] {
				new RDFPath(p, pRange, q, qRange),
				new RDFPath(q, qRange)});
		treeModel.selectPaths(paths);
		assertCollectionsAreEquivalent(paths, treeModel.getSelectedPaths());
	}
	
	/**
	 * Passes if every element in one collection is equal to an element in
	 * a second collection, irrespective of order.
	 * @param col1
	 * @param col2
	 */
	static void assertCollectionsAreEquivalent(Collection<?> col1, Collection<?> col2)
	{
		assertEquals(new HashSet<Object>(col1), new HashSet<Object>(col2));
	}
}
