/**
 * 
 */
package org.sadiframework.service.validation;

import java.util.ArrayList;
import java.util.Collection;

import org.sadiframework.beans.ServiceBean;


public class ValidationResult
{	
	private Collection<ValidationWarning> warnings;
	private ServiceBean service;
	
	public ValidationResult(ServiceBean service)
	{
		this.service = service;
		warnings = new ArrayList<ValidationWarning>();
	}
	
	public Collection<ValidationWarning> getWarnings()
	{
		return warnings;
	}
	
	public ServiceBean getService()
	{
		return service;
	}
}