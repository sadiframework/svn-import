package ca.wilkinsonlab.sadi.jowl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that holds the URI of the OWL Class that maps onto the
 * annotated Java class.
 * @author Luke McCarthy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OWLClass {
	String value();
}

