package ca.wilkinsonlab.sadi.service;

import ca.wilkinsonlab.sadi.SADIException;

/**
 * TODO maybe move this to a different package now that it's in sadi.common?
 * @author Luke McCarthy
 */
public class ServiceDefinitionException extends SADIException
{
	private static final long serialVersionUID = 1L;
	
	public ServiceDefinitionException(String message)
	{
		super(message);
	}
	
	public ServiceDefinitionException(String message, Throwable cause)
	{
        super(message, cause);
    }
	
	public ServiceDefinitionException(Throwable cause)
	{
        super(cause);
    }

}
