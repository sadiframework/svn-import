package ca.wilkinsonlab.sadi.share;

import ca.wilkinsonlab.sadi.common.SADIException;

/**
 * A common superclass for all SHARE exceptions.
 */
@SuppressWarnings("serial")
public class SHAREException extends SADIException
{
	public SHAREException(String message)
	{
		super(message);
	}
}
