package org.sadiframework.exceptions;

public class SADIServiceException extends Exception {

    private static final long serialVersionUID = 8646636111620467368L;

    public SADIServiceException() {
        super();
    }
    
    public SADIServiceException(String msg) {
        super(msg);
    }
    
}
