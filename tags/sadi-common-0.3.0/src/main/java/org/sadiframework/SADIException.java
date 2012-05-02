package org.sadiframework;

/**
 * A common superclass for all SADI exceptions.
 */
public class SADIException extends Exception
{
	private static final long serialVersionUID = 1L;

	public SADIException(String message)
	{
		super(message);
	}
	
	public SADIException(String message, Throwable cause)
	{
        super(message, cause);
    }
	
	public SADIException(Throwable cause)
	{
        super(cause);
    }
}
