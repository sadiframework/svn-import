package ca.wilkinsonlab.sadi.utils;

import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class SIOUtils
{
	/**
	 * Attach an attribute to the root node according to the SIO scheme.
	 * Specifically, create a structure that looks like this:
	 *   root [
	 *     has attribute (SIO_000008)
	 *       [
	 *         rdf:type $attributeClass
	 *         has value (SIO_000300) $value
	 *       ]
	 *   ]
	 * @param root the root resource
	 * @param attributeClass the type of attribute to create
	 * @param value the value of the attribute
	 * @return the created attribute node
	 */
	public static Resource createAttribute(Resource root, Resource attributeClass, String value)
	{
		Model model = root.getModel();
		Resource attribute = model.createResource(attributeClass);
		root.addProperty(SIO.has_attribute, attribute);
		if (value != null)
			attribute.addLiteral(SIO.has_value, value);
		return attribute;
	}
	
	/**
	 * Attach an attribute to the root node according to the SIO scheme.
	 * Specifically, create a structure that looks like this:
	 *   root [
	 *     $p, subProperty of 'has attribute' (SIO_000008)
	 *       [
	 *         rdf:type $attributeClass
	 *         'has value' (SIO_000300) $value
	 *       ]
	 *   ]
	 * @param root the root resource
	 * @param p some sub-property of 'has attribute' (SIO_000008)
	 * @param attributeClass the type of attribute to create
	 * @param value the value of the attribute
	 * @return the created attribute node
	 */
	public static Resource createAttribute(Resource root, Property p, Resource attributeClass, String value)
	{
		Model model = root.getModel();
		Resource attribute = model.createResource(attributeClass);
		root.addProperty(p, attribute);
		if (value != null)
			attribute.addLiteral(SIO.has_value, value);
		return attribute;
	}
}
