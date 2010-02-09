package ca.wilkinsonlab.sadi.jowl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that holds the URI of the OWL Property that maps onto the
 * annotated field.  The property describes the relationship between the
 * class and the value of the field.
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OWLProperty {
	String value();
}
