/**
 * 
 */
package ca.wilkinsonlab.sadi.jowl;

import ca.wilkinsonlab.sadi.common.SADIException;

/**
 * @author Luke McCarthy
 */
@SuppressWarnings("serial")
public class SerializationException extends SADIException
{	
	public SerializationException(String message)
	{
		super(message);
	}
}
