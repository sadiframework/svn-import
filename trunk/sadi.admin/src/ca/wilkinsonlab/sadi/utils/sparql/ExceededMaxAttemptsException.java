/**
 * 
 */
package ca.wilkinsonlab.sadi.utils.sparql;

public class ExceededMaxAttemptsException extends Exception
{
	private static final long serialVersionUID = 1L;
	public ExceededMaxAttemptsException(String msg) { super(msg); }
}