package ca.wilkinsonlab.daggoo.exceptions;

public class SoapClientException extends Exception {

    public SoapClientException(String string, Exception e) {
	super(string,e);

    }

}
