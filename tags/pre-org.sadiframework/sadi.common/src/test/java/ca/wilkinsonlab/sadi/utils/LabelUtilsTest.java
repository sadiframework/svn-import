package ca.wilkinsonlab.sadi.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class LabelUtilsTest
{
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
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
	public void testGetLabelsResource()
	{
		assertFalse(LabelUtils.getLabels(ResourceFactory.createResource()).hasNext());
	}

	@Test
	public void testGetLabelsResourceString()
	{
//		fail("Not yet implemented");
	}

	@Test
	public void testGetLabelResource()
	{
		assertEquals("label text", LabelUtils.getLabel(ResourceFactory.createResource("urn:label:label%20text")));
	}

	@Test
	public void testGetLabelResourceString()
	{
//		fail("Not yet implemented");
	}

	@Test
	public void testGetDefaultLabel() throws Exception
	{
		assertEquals("label text", LabelUtils.getDefaultLabel(ResourceFactory.createResource("urn:label:label%20text")));
	}

	@Test
	public void testGetDescriptionResource()
	{
//		fail("Not yet implemented");
	}

	@Test
	public void testGetDescriptionResourceString()
	{
//		fail("Not yet implemented");
	}

	@Test
	public void testToStringRDFNode()
	{
//		fail("Not yet implemented");
	}

	@Test
	public void testGetRestrictionString()
	{
//		fail("Not yet implemented");
	}

	@Test
	public void testGetClassString()
	{
//		fail("Not yet implemented");
	}
}
