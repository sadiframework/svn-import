package ca.wilkinsonlab.sadi.service.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation holding the location of the service description in RDF.
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceDefinition
{
	String value();
}
