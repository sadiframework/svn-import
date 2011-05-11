package ca.wilkinsonlab.sadi.service.validation;


import java.util.Collection;

import org.apache.commons.validator.EmailValidator;
import org.stringtree.util.StringUtils;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.service.ontology.MyGridServiceOntologyHelper;
import ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyException;
import ca.wilkinsonlab.sadi.utils.QueryableErrorHandler;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.JenaException;

public class ServiceValidator
{
	public static ValidationResult validateService(String serviceURI) throws SADIException
	{
		Model model = ModelFactory.createDefaultModel();
		QueryableErrorHandler errorHandler = new QueryableErrorHandler();
		model.getReader().setErrorHandler(errorHandler);
		model.read(serviceURI);
		if (errorHandler.hasLastError())
			throw new SADIException(String.format("error reading RDF model from %s: %s",
					serviceURI, errorHandler.getLastError().getMessage()));
		
		Resource serviceNode = model.getResource(serviceURI);
		if (!model.containsResource(serviceNode))
			throw new SADIException(String.format("RDF model at %s does not contain any statements about %s",
					serviceURI, serviceNode));
		
		return validateService(serviceNode);
	}
	
	public static ValidationResult validateService(Resource serviceNode) throws ServiceOntologyException
	{
		ServiceBean service = new ServiceBean();
		MyGridServiceOntologyHelper mygrid = new MyGridServiceOntologyHelper();
		mygrid.copyServiceDescription(serviceNode, service);
		ValidationResult result = validateService(service);
		
		Collection<RDFNode> authoritative = mygrid.getAuthoritativePath().getValuesRootedAt(serviceNode);
		if (authoritative.isEmpty())
			result.getWarnings().add(new ValidationWarning("no authoritative value"));
		else if (authoritative.size() > 1)
			result.getWarnings().add(new ValidationWarning("multiple authoritative values"));
		else
			try {
				authoritative.iterator().next().asLiteral().getBoolean();
			} catch (JenaException e) {
				result.getWarnings().add(new ValidationWarning("authoritative value is not a boolean typed literal"));
			}
		
		return result;
	}
	
	public static ValidationResult validateService(ServiceBean service)
	{
		ValidationResult result = new ValidationResult(service);
		
		if (StringUtils.isBlank(service.getName()) || service.getName().equals("noname"))
			result.getWarnings().add(new ValidationWarning("no service name"));
		if (StringUtils.isBlank(service.getDescription()) || service.getDescription().equals("no description"))
			result.getWarnings().add(new ValidationWarning("no service description"));
		
		if (StringUtils.isBlank(service.getContactEmail()))
			result.getWarnings().add(new ValidationWarning("no contact email"));
		else if (!EmailValidator.getInstance().isValid(service.getContactEmail()))
			result.getWarnings().add(new ValidationWarning(String.format("%s doesn't look like a valid email address", service.getContactEmail())));
		
		return result;
	}
	
	public static void main(String args[])
	{
		for (String arg: args) {
			System.out.println(String.format("validating %s...", arg));
			try {
				ValidationResult result = validateService(arg);
				if (result.getWarnings().isEmpty()) {
					System.out.println("\t...validated");
				} else {
					System.out.println(String.format("\t...validated with warnings:"));
					for (ValidationWarning warning: result.getWarnings()) {
						System.out.print("\t   ");
						System.out.println(warning.getMessage());
					}
				}
			} catch (SADIException e) {
				System.out.println(String.format("\t...error: %s", e.getMessage()));
			}
		}
	}
}
