package org.sadiframework.service.example;

import org.apache.log4j.Logger;
import org.sadiframework.SADIException;
import org.sadiframework.client.ServiceImpl;
import org.sadiframework.service.ServiceServletTestBase;
import org.sadiframework.service.annotations.URI;

import com.hp.hpl.jena.rdf.model.Model;


@URI("http://localhost:8180/blastUniprotById")
public class BlastUniProtByIdServiceServletIT extends ServiceServletTestBase 
{
	private static final Logger log = Logger.getLogger(BlastUniProtByIdServiceServletIT.class);
	
	@Override
	public boolean compareOutput(Model output, Model expected) {
		log.info("skipping BLAST ouptut check because metadata will change on every invocation");
		return true;
	}

	@Override
	protected void sanityCheckOutput(ServiceImpl service, Model output) throws SADIException {
		log.info("skipping sanity check for memory reasons");
	}
}
