package ca.wilkinsonlab.sadi.test;

import ca.wilkinsonlab.sadi.rdfpath.RDFPath;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class RDFPathTest
{
	public static void main(String[] args)
	{
		Model model = ModelFactory.createDefaultModel();
		model.read("file:/tmp/out", "RDF/XML");
		Resource root = model.getResource("http://lsrn.org/UniProt:B7ZA85");
		RDFPath path = new RDFPath("http://semanticscience.org/resource/SIO_000061 some http://purl.oclc.org/SADI/LSRN/GO_Record, http://semanticscience.org/resource/SIO_000008 some http://purl.oclc.org/SADI/LSRN/GO_Identifier, http://semanticscience.org/resource/SIO_000300 some http://www.w3.org/2000/01/rdf-schema#Literal");
		System.out.println(path.getValuesRootedAt(root));
	}
}
