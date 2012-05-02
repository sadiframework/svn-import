package org.sadiframework.client;

import org.sadiframework.SADIException;
import org.sadiframework.utils.http.HttpUtils;

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
		 * service as dead; have the service class set a flag on this
		 * exception if it means the service is dead.
		 * (see http://dev.biordf.net/cgi-bin/bugzilla/show_bug.cgi?id=13 for details)
		 */
		if (HttpUtils.isHttpServerError(getCause()))
			return true;
		
		return false;
	}
}
