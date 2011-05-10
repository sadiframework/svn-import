package ca.wilkinsonlab.sadi.client.virtual.biomoby;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import ca.wilkinsonlab.sadi.client.Config;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.virtual.biomoby.BioMobyRegistry;

public class BioMobyServiceTest
{
	static Service service;
	static Resource inputClass;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		BioMobyRegistry registry = (BioMobyRegistry)Config.getConfiguration().getRegistry("biomoby");
		service = registry.getService("http://biomoby.org/RESOURCES/MOBY-S/ServiceInstances/cnio.es,getSymbolInfo");
		inputClass = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/UniProt_Record");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		service = null;
	}
	
	@Test
	public void testIsInputInstance()
	{
		Model model = ModelFactory.createDefaultModel();
		Resource r = model.createResource("http://lsrn.org/UniProt:P12345", inputClass);
		
		assertTrue("failed to recognize input instance", service.isInputInstance(r));
	}

	@Test
	public void testDiscoverInputInstances()
	{
		Model model = ModelFactory.createDefaultModel();
		Resource r = model.createResource("http://lsrn.org/UniProt:P12345", inputClass);
		
		assertTrue("failed to discover input instance", service.discoverInputInstances(model).contains(r));
	}

}
