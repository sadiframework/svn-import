package org.sadiframework.service.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation indicating that a service is asynchronous.
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Asynchronous
{
	boolean value() default true; // so just @Asynchronous means you are
}
