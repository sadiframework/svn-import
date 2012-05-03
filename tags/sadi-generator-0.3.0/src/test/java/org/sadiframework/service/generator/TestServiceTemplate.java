package org.sadiframework.service.generator;

import java.io.File;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.sadiframework.beans.ServiceBean;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TestServiceTemplate extends TestCase
{
	private static final Logger log = Logger.getLogger(TestServiceTemplate.class);
	
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
	
	@SuppressWarnings("restriction")
	public void testServiceTemplate() throws Exception
	{
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		model.read(TestServiceTemplate.class.getResourceAsStream("/hello.owl"), "http://example.com/hello.owl#");
		OntClass inputClass = model.getOntClass("http://example.com/hello.owl#NamedIndividual");
		OntClass outputClass = model.getOntClass("http://example.com/hello.owl#GreetedIndividual");
		File sourcePackage = File.createTempFile("package", "");
		sourcePackage.delete();
		log.debug(String.format("creating package directory %s", sourcePackage));
		sourcePackage.mkdir();
		File classFile = new File(sourcePackage, "ServiceClass.java");
		String serviceClass = String.format("%s.%s", sourcePackage.getName(), "ServiceClass");
		ServiceBean serviceBean = new ServiceBean();
		serviceBean.setName("hello");
		serviceBean.setInputClassURI(inputClass.getURI());
		serviceBean.setOutputClassURI(outputClass.getURI());
		serviceBean.setContactEmail("incognito@mailinator.com");
		log.debug(String.format("creating class file in %s", classFile));
		new GenerateService().writeClassFile(classFile, serviceClass, inputClass, outputClass, serviceBean, false);
		String[] args = new String[] { "-d", sourcePackage.getParent(), classFile.getAbsolutePath() };
		log.debug(String.format("executing javac with args %s", StringUtils.join(args, " ")));
		int status = com.sun.tools.javac.Main.compile(args);
		assertEquals(String.format("javac returned status %d", status), 0, status);
	}
}
