package ca.wilkinsonlab.sadi.service.ontology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.wilkinsonlab.sadi.ServiceDescription;
import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.utils.ModelDiff;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Luke McCarthy
 */
public class MyGridServiceOntologyHelperTest
{
	private static Resource serviceNode;
	private static ServiceBean serviceDescription;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		String serviceURI = "http://sadiframework.org/examples/linear";
		Model model = ModelFactory.createDefaultModel();
		model.read(MyGridServiceOntologyHelperTest.class.getResourceAsStream("linear.rdf"), serviceURI);
		serviceNode = model.getResource(serviceURI);
		
		serviceDescription = new ServiceBean();
		serviceDescription.setURI(serviceURI);
		serviceDescription.setName("LinearRegression");
		serviceDescription.setDescription("Fits a least-squares regression line.");
		serviceDescription.setServiceProvider("wilkinsonlab.ca");
		serviceDescription.setContactEmail("mccarthy@elmonline.ca");
		serviceDescription.setAuthoritative(true);
		serviceDescription.setInputClassURI("http://sadiframework.org/examples/regression.owl#InputClass");
		serviceDescription.setOutputClassURI("http://sadiframework.org/examples/regression.owl#OutputClass");
		serviceDescription.setParameterClassURI("http://sadiframework.org/examples/regression.owl#ParameterClass");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		serviceNode.getModel().close();
		serviceNode = null;
		
		serviceDescription = null;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
	}

	/**
	 * Test method for {@link ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper#getServiceDescription(com.hp.hpl.jena.rdf.model.Resource)}.
	 */
	@Test
	public void testGetServiceDescription() throws ServiceOntologyException
	{
		ServiceDescription service = new MyGridServiceOntologyHelper().getServiceDescription(serviceNode);
		assertEquals("incorrect name", serviceDescription.getName(), service.getName());
		assertEquals("incorrect description", serviceDescription.getDescription(), service.getDescription());
		assertEquals("incorrect service provider", serviceDescription.getServiceProvider(), service.getServiceProvider());
		assertEquals("incorrect contact email", serviceDescription.getContactEmail(), service.getContactEmail());
		assertEquals("incorrect authoritative", serviceDescription.isAuthoritative(), service.isAuthoritative());
		assertEquals("incorrect input class", serviceDescription.getInputClassURI(), service.getInputClassURI());
		assertEquals("incorrect output class", serviceDescription.getOutputClassURI(), service.getOutputClassURI());
		assertEquals("incorrect parameter class", serviceDescription.getParameterClassURI(), service.getParameterClassURI());
	}

	/**
	 * Test method for {@link ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper#createServiceNode(ca.wilkinsonlab.sadi.ServiceDescription, com.hp.hpl.jena.rdf.model.Model)}.
	 */
	@Test
	public void testCreateServiceNode() throws ServiceOntologyException
	{
		Model model = ModelFactory.createDefaultModel();
		new MyGridServiceOntologyHelper().createServiceNode(serviceDescription, model);
		if (!model.isIsomorphicWith(serviceNode.getModel())) {
			ModelDiff diff = ModelDiff.diff(model, serviceNode.getModel());
			StringBuilder buf = new StringBuilder();
			buf.append("model created by getServiceNode isn't as expected:\n");
			buf.append("statements in expected model missing from created model:\n");
			buf.append(RdfUtils.logStatements("\t", diff.inYnotX));
			buf.append("\n");
			buf.append("unexpected statements in created model:\n");
			buf.append(RdfUtils.logStatements("\t", diff.inXnotY));
			fail(buf.toString());
		}
	}
}
