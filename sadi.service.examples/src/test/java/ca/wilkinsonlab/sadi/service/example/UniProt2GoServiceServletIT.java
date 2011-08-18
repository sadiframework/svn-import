package ca.wilkinsonlab.sadi.service.example;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.ServiceImpl;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;
import ca.wilkinsonlab.sadi.service.annotations.URI;

@URI("http://localhost:8180/sadi-examples/uniprot2go")
public class UniProt2GoServiceServletIT extends ServiceServletTestBase
{
	Logger log = Logger.getLogger(UniProt2GoServiceServletIT.class);
	
	@Override
	protected void sanityCheckOutput(ServiceImpl service, Model output) throws SADIException {
		log.info("skipping sanity check because importing SIO causes an infinite loop");
	}
}
