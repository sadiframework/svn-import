package ca.wilkinsonlab.sadi.rdfpath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class RDFPathUtilsTest
{
	@Test
	public void testGetLeafPaths()
	{
		Model model = ModelFactory.createDefaultModel();
		Resource root = model.createResource(); 
		RDFPath path1 = new RDFPath(
				"http://semanticscience.org/resource/SIO_000552, " +
				"http://unbsj.biordf.net/fishtox/BLAST-sadi-service-ontology.owl#E_Value, " +
				"http://semanticscience.org/resource/SIO_000300, " +
				"http://www.w3.org/2001/XMLSchema#double"
		);
		path1.createLiteralRootedAt(root, "0.0001");
		RDFPath path2 = new RDFPath(
				"http://semanticscience.org/resource/SIO_000552, " +
				"http://unbsj.biordf.net/fishtox/BLAST-sadi-service-ontology.owl#BitScore, " +
				"http://semanticscience.org/resource/SIO_000300, " +
				"http://www.w3.org/2001/XMLSchema#double"
		);
		path2.createLiteralRootedAt(root, "25");
		Collection<RDFPath> leafPaths = RDFPathUtils.getLeafPaths(root);
//		model.write(System.err, "N3");
//		for (RDFPath path: leafPaths)
//			System.err.println(path);
		assertTrue(leafPaths.contains(path1));
		assertTrue(leafPaths.contains(path2));
		assertEquals(2, leafPaths.size());
	}
}
