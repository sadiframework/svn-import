package ca.wilkinsonlab.sadi.service.ontology;

import ca.wilkinsonlab.sadi.common.SADIException;

@SuppressWarnings("serial")
public class ServiceOntologyException extends SADIException
{
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
