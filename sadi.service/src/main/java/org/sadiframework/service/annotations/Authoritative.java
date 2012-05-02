package org.sadiframework.service.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation indicating that a service is authoritative.
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Authoritative
{
	boolean value() default true; // so just @Authoritative means you are
}
