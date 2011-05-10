package ca.wilkinsonlab.sadi.registry.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.registry.Registry;

/**
 * Update all service definitions in the registry.
 * @author Luke McCarthy
 */
public class Update
{
	private static Logger log = Logger.getLogger(Update.class);
	private static String BACKUP_PREFIX = "/tmp/registry-backup";
	
	public static void main(String[] args)
	{
		Registry registry = null;
		try {
			registry = Registry.getRegistry();
		} catch (SADIException e) {
			String message = String.format("error contacting registry: %s", e.getMessage());
			log.error(message, e);
			System.err.println(message);
			System.exit(1);
		} finally {
			if (registry != null)
				registry.getModel().close();
		}
		String filename = String.format("%s.%s.rdf", BACKUP_PREFIX, new SimpleDateFormat("yyyyMMdd.hhmmss").format(Calendar.getInstance().getTime()));
		try {
			Model model = registry.getModel();
			RDFWriter writer = model.getWriter("RDF/XML-ABBREV");
			writer.write(model, new FileOutputStream(filename), "");
		} catch (IOException e) {
			String message = String.format("error writing registry backup to %s: %s", filename, e.getMessage());
			log.error(message, e);
			System.err.println(message);
			System.exit(1);
		}
		
		List<Resource> services = registry.getRegisteredServiceNodes().toList();
		for (Resource service: services) {
			registry.getModel().begin();
			try {
				log.info(String.format("updating service %s", service));
				registry.registerService(service.getURI());
				registry.getModel().commit();
			} catch (SADIException e) {
				log.error(String.format("error updating service %s: %s", service, e.getMessage()), e);
				registry.getModel().abort();
			}
		}
		registry.getModel().close();
	}
}
