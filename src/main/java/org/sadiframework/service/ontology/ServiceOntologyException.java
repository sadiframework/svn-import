package org.sadiframework.service.ontology;

import org.sadiframework.service.ServiceDefinitionException;

public class ServiceOntologyException extends ServiceDefinitionException
{
	private static final long serialVersionUID = 1L;

	public ServiceOntologyException(String message)
	{
		super(message);
	}
	
	public ServiceOntologyException(String message, Throwable cause)
	{
        super(message, cause);
    }
	
	public ServiceOntologyException(Throwable cause)
	{
        super(cause);
    }
}
