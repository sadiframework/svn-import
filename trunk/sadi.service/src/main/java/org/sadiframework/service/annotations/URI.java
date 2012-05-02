package org.sadiframework.service.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation holding the service URI.
 * In most cases this won't be necessary, as the service URI can be taken
 * from the request URL...
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface URI
{
	String value();
}
