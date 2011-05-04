package ca.wilkinsonlab.sadi.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.service.simple.SimpleAsynchronousServiceServlet;

import com.hp.hpl.jena.rdf.model.Resource;

public class AsyncLinearRegressionServiceServlet extends SimpleAsynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(AsyncLinearRegressionServiceServlet.class);
	
	public void processInput(Resource input, Resource output)
	{
		RegressionUtils.processInput(input, output);
		
		log.info("waiting 5 seconds to facilitate debugging");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.warn(e);
		}
	}
}
