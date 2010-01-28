package ca.wilkinsonlab.sadi.common;

/**
 * A common superclass for all SADI exceptions.
 */
@SuppressWarnings("serial")
public class SADIException extends Exception
{
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
