package org.sadiframework.service.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation holding the service output class.
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OutputClass
{
	String value();
}
