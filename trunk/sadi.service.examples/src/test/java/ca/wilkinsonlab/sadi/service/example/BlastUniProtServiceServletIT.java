package ca.wilkinsonlab.sadi.service.example;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.ServiceImpl;
import ca.wilkinsonlab.sadi.service.ServiceServletTestBase;
import ca.wilkinsonlab.sadi.service.annotations.URI;

@URI("http://localhost:8180/sadi-examples/blastUniprot")
public class BlastUniProtServiceServletIT extends ServiceServletTestBase 
{
	private static final Logger log = Logger.getLogger(BlastUniProtServiceServletIT.class);
	
	@Override
	protected void sanityCheckOutput(ServiceImpl service, Model output) throws SADIException {
		log.info("skipping sanity check for memory reasons");
	}
}
