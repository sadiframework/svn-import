/**
 * 
 */
package ca.wilkinsonlab.sadi.optimizer.statistics;

public class ExceededMaxAttemptsException extends Exception
{
	private static final long serialVersionUID = 1L;
	public ExceededMaxAttemptsException(String msg) { super(msg); }
}