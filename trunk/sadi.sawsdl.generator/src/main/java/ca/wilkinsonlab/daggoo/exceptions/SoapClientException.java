package ca.wilkinsonlab.daggoo.exceptions;

public class SoapClientException extends Exception {

	private static final long serialVersionUID = 1L;

	public SoapClientException(String string, Exception e) {
	super(string,e);

    }

}
