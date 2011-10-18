package ca.wilkinsonlab.sadi.service.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation holding the service description.
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Description
{
	String value();
}
