package ca.wilkinsonlab.sadi.registry.test;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.service.validation.ServiceValidator;
import ca.wilkinsonlab.sadi.service.validation.ValidationResult;

public class ValidateService
{
	public static void main(String[] args)
	{
		for (String service: args) {
			System.out.println(service);
			try {
				ValidationResult result = ServiceValidator.validateService(service);
				System.out.println("\t" + result.getWarnings());
			} catch (SADIException e) {
				e.printStackTrace();
			}
		}
	}
}
