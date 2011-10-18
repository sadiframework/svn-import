package ca.wilkinsonlab.sadi.service.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation holding the service secondary parameter class.
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterClass
{
	String value();
}
