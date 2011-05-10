package ca.wilkinsonlab.sadi.share;

/**
 * An exception that indicates a SPARQL query is unresolvable.
 */
@SuppressWarnings("serial")
public class UnresolvableQueryException extends SHAREException
{
	public UnresolvableQueryException(String message)
	{
		super(message);
	}
}
