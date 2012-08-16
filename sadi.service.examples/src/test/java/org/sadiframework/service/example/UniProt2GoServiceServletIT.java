package org.sadiframework.service.example;

import org.apache.log4j.Logger;
import org.sadiframework.service.ServiceServletTestBase;
import org.sadiframework.service.annotations.URI;


@URI("http://localhost:8180/uniprot2go")
public class UniProt2GoServiceServletIT extends ServiceServletTestBase
{
	Logger log = Logger.getLogger(UniProt2GoServiceServletIT.class);
	
//	@Override
//	protected void sanityCheckOutput(ServiceImpl service, Model output) throws SADIException {
//		log.info("skipping sanity check because importing SIO causes an infinite loop");
//	}
}
