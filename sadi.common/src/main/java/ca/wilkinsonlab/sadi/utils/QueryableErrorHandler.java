package ca.wilkinsonlab.sadi.utils;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.shared.JenaException;

public class QueryableErrorHandler implements RDFErrorHandler
{
	private static final Logger log = Logger.getLogger(QueryableErrorHandler.class);
	
	private Exception lastError;
	private Exception lastWarning;
	
	public QueryableErrorHandler()
	{
	}

	public boolean hasLastError()
	{
		return lastError != null;
	}

	public Exception getLastError()
	{
		return lastError;
	}

	public boolean hasLastWarning()
	{
		return lastWarning != null;
	}

	public Exception getLastWarning()
	{
		return lastWarning;
	}

	public void clear()
	{
		lastError = null;
		lastWarning = null;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.rdf.model.RDFErrorHandler#warning(java.lang.Exception)
	 */
	@Override
	public void warning(Exception e)
	{
		log.warn(e);
		lastWarning = e;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.rdf.model.RDFErrorHandler#error(java.lang.Exception)
	 */
	@Override
	public void error(Exception e)
	{
		log.error(e);
		lastError = e;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.rdf.model.RDFErrorHandler#fatalError(java.lang.Exception)
	 */
	@Override
	public void fatalError(Exception e)
	{
		throw e instanceof RuntimeException ?
				(RuntimeException) e : new JenaException( e );
	}
}