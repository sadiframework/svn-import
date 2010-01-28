package ca.wilkinsonlab.sadi.client;

import org.biomoby.shared.SOAPException;

import ca.wilkinsonlab.sadi.common.SADIException;
import ca.wilkinsonlab.sadi.utils.http.HttpUtils;

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
	
	public ServiceInvocationException(String message, Throwable cause)
	{
        super(message, cause);
    }
	
	public ServiceInvocationException(Throwable cause)
	{
        super(cause);
    }
	
	public boolean isServiceDead()
	{
		/* TODO there are probably other cases where we want to mark a
		 * service as dead...
		 */
		if (HttpUtils.isHttpServerError(getCause()))
			return true;
		if (getCause() instanceof SOAPException)
			return true;
		
		return false;
	}
}
