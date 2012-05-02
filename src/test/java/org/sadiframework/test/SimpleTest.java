package org.sadiframework.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.sadiframework.rdfpath.RDFPath;
import org.sadiframework.utils.OwlUtils;


import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class SimpleTest
{
	private static final RDFPath path1 = new RDFPath(
			"http://semanticscience.org/resource/SIO_000300",
				XSDDatatype.XSDstring.getURI());
	private static final RDFPath path2 = new RDFPath(
			"http://semanticscience.org/resource/SIO_000008",
				"http://semanticscience.org/resource/SIO_000116",
			"http://semanticscience.org/resource/SIO_000300",
				XSDDatatype.XSDstring.getURI());
	
	public static void writeOWL()
	{
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		OntClass r1 = OwlUtils.createRestrictions(model, path1, false);
		System.out.println(r1.getURI());
		OntClass r2 = OwlUtils.createRestrictions(model, path2, false);
		System.out.println(r2.getURI());
		try {
			model.write(new FileOutputStream("src/test/resources/ca/wilkinsonlab/sadi/test/SimpleTest.owl"), "RDF/XML-ABBREV", "");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void testOWL()
	{
		Model model = ModelFactory.createDefaultModel();
		Resource root1 = model.createResource("root1");
		path1.createLiteralRootedAt(root1, "value1");
		Resource root2 = model.createResource("root2");
		path2.createLiteralRootedAt(root2, "value2");
		model.write(System.out, "N3", "");
		
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
//		OntClass r1 = OwlUtils.createRestrictions(ontModel, path1, true);
//		OntClass r2 = OwlUtils.createRestrictions(ontModel, path2, true);
		ontModel.read(SimpleTest.class.getResourceAsStream("SimpleTest.owl"), "");
		ontModel.write(System.out, "RDF/XML-ABBREV", "");
		ontModel.addSubModel(model);
//		testInstance(root1.inModel(ontModel), r1, path1);
		OntClass read1 = getClass(ontModel, "urn:uuid:facb1221-ca6f-4e60-bfb8-2781f000d98c");
		testInstance(root1.inModel(ontModel), read1, path1);
//		testInstance(root2.inModel(ontModel), r2, path2);
		OntClass read2 = getClass(ontModel, "urn:uuid:e99379b7-3c7a-4c84-8de6-d8e43d4dc479");
		testInstance(root2.inModel(ontModel), read2, path2);
	}

	private static OntClass getClass(OntModel model, String uri)
	{
		OntClass c = model.getOntClass(uri);
		if (c != null)
			return c;
		else
			throw new NullPointerException();
	}
	
	private static void testInstance(Resource i, OntClass c, RDFPath path)
	{
		System.out.println(String.format("%s %s an instance of %s (created by %s)",
				i, i.hasProperty(RDF.type, c) ? "is" : "is not", c, path));
	}
	
	public static void main(String[] args)
	{
//		writeOWL();
		testOWL();
	}
}
