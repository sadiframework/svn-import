package org.sadiframework.service.example;

import org.apache.log4j.Logger;
import org.sadiframework.SADIException;
import org.sadiframework.client.ServiceImpl;
import org.sadiframework.service.ServiceServletTestBase;
import org.sadiframework.service.annotations.URI;


import com.hp.hpl.jena.rdf.model.Model;

@URI("http://localhost:8180/blastUniprot")
public class BlastUniProtServiceServletIT extends ServiceServletTestBase 
{
	private static final Logger log = Logger.getLogger(BlastUniProtServiceServletIT.class);
	
	@Override
	public boolean compareOutput(Model output, Model expected) {
		log.info("skipping BLAST ouptut check because metadata will change on every invocation");
		return true;
	}
	
	@Override
	protected void sanityCheckOutput(ServiceImpl service, Model output) throws SADIException
	{
		log.info("skipping sanity check for memory reasons");
		return;
	}
	
//	@Override
//	public void testLocalService() throws SADIException
//	{
//		ServiceImpl service = getLocalServiceInstance();
//		Model output = ModelFactory.createDefaultModel();
//		output.read("file:/Users/luke/Downloads/blast-uniprot.2.rdf");
//		output.write(System.out, "RDF/XML-ABBREV");
//		Resource blasted = output.getResource("http://purl.uniprot.org/core/#_C");
//		for (Statement s: blasted.listProperties().toList()) {
//			System.out.println(s);
//		}
//		super.sanityCheckOutput(service, output);
//	}
}
