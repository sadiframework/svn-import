package org.sadiframework.service.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation holding the service provider.
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ContactEmail
{
	String value();
}
