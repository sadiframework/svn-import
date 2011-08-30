package ca.wilkinsonlab.sadi.registry.test;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceFactory;
import ca.wilkinsonlab.sadi.registry.Registry;
import ca.wilkinsonlab.sadi.service.validation.ServiceValidator;
import ca.wilkinsonlab.sadi.service.validation.ValidationResult;
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
		ResIterator i = registry.getRegisteredServiceNodes();
		while (i.hasNext()) {
			Resource serviceNode = i.next();
			log.info(String.format("validating %s against registry", serviceNode));
			try {
				ValidationResult result = ServiceValidator.validateService(serviceNode);
				if (!result.getWarnings().isEmpty())
					log.warn(String.format("warnings validating %s\n\t%s", serviceNode,
							StringUtils.join(result.getWarnings(), "\n\t")));
			} catch (SADIException e) {
				log.error(String.format("error validating %s against registry", serviceNode), e);
			}
			try {
				log.info(String.format("validating %s against endpoint", serviceNode));
				Service service = ServiceFactory.createService(serviceNode.getURI());
				log.info("validating input/output OWL for %s");
				try {
					for (Restriction r: service.getRestrictions())
						log.debug(String.format("found restriction %s", LabelUtils.getRestrictionString(r)));
				} catch (Exception e) {
					log.error(String.format("error validating %s input/output OWL", serviceNode), e);
				}
			} catch (SADIException e) {
				log.error(String.format("error validating %s against endpoint", serviceNode), e);
			}
		}
		i.close();
	}
}
