package org.sadiframework.service.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation holding the service secondary parameter defaults.
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterDefaults
{
	String[] value();
}
