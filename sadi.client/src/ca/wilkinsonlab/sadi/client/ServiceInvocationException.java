package ca.wilkinsonlab.sadi.client;

import ca.wilkinsonlab.sadi.common.SADIException;

/**
 * An exception that indicates there was a problem calling a service.
 */
@SuppressWarnings("serial")
public class ServiceInvocationException extends SADIException
{
	public ServiceInvocationException(String message)
	{
		super(message);
	}
}
