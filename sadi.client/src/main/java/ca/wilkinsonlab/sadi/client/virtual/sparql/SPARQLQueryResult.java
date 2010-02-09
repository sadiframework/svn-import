package ca.wilkinsonlab.sadi.client.virtual.sparql;

abstract public class SPARQLQueryResult {

	String originalQuery;
	boolean exceptionOccurred;
	Exception exception;
	
	public SPARQLQueryResult(String originalQuery) {
		setOriginalQuery(originalQuery);
		setExceptionOccurred(false);
	}
	
	public SPARQLQueryResult(String originalQuery, Exception exception) {
		setOriginalQuery(originalQuery);
		setExceptionOccurred(true);
		setException(exception);
	}
	
	public String getOriginalQuery() {
		return originalQuery;
	}
	public void setOriginalQuery(String originalQuery) {
		this.originalQuery = originalQuery;
	}
	public boolean exceptionOccurred() {
		return exceptionOccurred;
	}
	public void setExceptionOccurred(boolean exceptionOccurred) {
		this.exceptionOccurred = exceptionOccurred;
	}
	public Exception getException() {
		return exception;
	}
	public void setException(Exception exception) {
		this.exception = exception;
	}

}
