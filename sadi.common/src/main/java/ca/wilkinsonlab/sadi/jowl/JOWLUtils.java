package ca.wilkinsonlab.sadi.jowl;

import java.lang.reflect.Field;

import org.apache.commons.lang.StringUtils;

/**
 * @author Luke McCarthy
 */
public class JOWLUtils
{
	public static String getClassURI(Class<?> c, String namespace)
	{
		OWLClass owlClassAnnot = c.getAnnotation(OWLClass.class);
		if (owlClassAnnot != null) {
			return owlClassAnnot.value();
		} else {
			String className = c.getSimpleName();
			if (className != null)
				return StringUtils.defaultString(namespace) + c.getSimpleName();
			else
				throw new IllegalArgumentException("unable to auto-generate class URI for class with no OWLClass annotation");
		}
	}
	
	public static String getPropertyURI(Field f, String namespace)
	{
		OWLProperty owlPropertyAnnot = f.getAnnotation(OWLProperty.class);
		if (owlPropertyAnnot != null) {
			return owlPropertyAnnot.value();
		} else {
			return StringUtils.defaultString(namespace) + f.getName();
		}
	}
}
