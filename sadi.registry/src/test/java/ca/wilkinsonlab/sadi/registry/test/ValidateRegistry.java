package ca.wilkinsonlab.sadi.registry.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceFactory;
import ca.wilkinsonlab.sadi.registry.Registry;
import ca.wilkinsonlab.sadi.service.validation.ServiceValidator;
import ca.wilkinsonlab.sadi.service.validation.ValidationResult;
import ca.wilkinsonlab.sadi.service.validation.ValidationWarning;
import ca.wilkinsonlab.sadi.utils.LabelUtils;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

public class ValidateRegistry
{
	private static final Logger log = Logger.getLogger(ValidateRegistry.class);
	
	public static void main(String[] args)
	{
		OntDocumentManager.getInstance().setCacheModels(true);
		try {
			validateServices(Registry.getRegistry());
		} catch (SADIException e) {
			log.error(e.toString(), e);
			System.exit(1);
		}
	}
	
	private static void validateServices(Registry registry)
	{
		Set<String> validated = getPreviouslyValidatedServices();
		ResIterator i = registry.getRegisteredServiceNodes();
		while (i.hasNext()) {
			Resource serviceNode = i.next();
			if (validated.contains(serviceNode.getURI())) {
				log.info(String.format("skipping %s", serviceNode));
				continue;
			}
			log.info(String.format("validating %s against registry", serviceNode));
			try {
				ValidationResult result = ServiceValidator.validateService(serviceNode);
				if (!result.getWarnings().isEmpty()) {
					log.warn(String.format("warnings validating %s\n\t%s", serviceNode,
							joinToString(result.getWarnings(), "\n\t")));
				}
			} catch (SADIException e) {
//				log.error(String.format("error validating %s against registry", serviceNode), e);
				log.error(String.format("error validating %s against registry: %s", serviceNode, e.getMessage()));
			}
			try {
				log.info(String.format("validating %s against endpoint", serviceNode));
				Service service = ServiceFactory.createService(serviceNode.getURI());
				if (System.getProperty("validateOWL") != null) {
					log.info("validating input/output OWL for %s");
					try {
						for (Restriction r: service.getRestrictions())
							log.debug(String.format("found restriction %s", LabelUtils.getRestrictionString(r)));
					} catch (Exception e) {
						log.error(String.format("error validating %s input/output OWL", serviceNode), e);
					}
				}
			} catch (SADIException e) {
//				log.error(String.format("error validating %s against endpoint", serviceNode), e);
				log.error(String.format("error validating %s against endpoint: %s", serviceNode, e.getMessage()));
			}
		}
		i.close();
	}
	
	private static Set<String> getPreviouslyValidatedServices()
	{
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream("target/registry-validation.log")));
		} catch (FileNotFoundException e) {
			log.warn("log file not found.");
		}

		Set<String> validated = new HashSet<String>();
		if (reader != null) {
			Pattern logPattern = Pattern.compile("validating (.*) against registry");
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					Matcher matcher = logPattern.matcher(line);
					if (matcher.find())
						validated.add(matcher.group(1));
				}
			} catch (IOException e) {
				log.error(String.format("error reading from log file: %s", e.toString()));
			}
		}
		return validated;
	}

	private static String joinToString(Iterable<ValidationWarning> warnings, String separator)
	{
		StringBuilder buf = new StringBuilder();
		Iterator<ValidationWarning> i = warnings.iterator();
		while (i.hasNext()) {
			buf.append(i.next().getMessage());
			if (i.hasNext())
				buf.append(separator);
		}
		return buf.toString();
	}
}
