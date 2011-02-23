package ca.wilkinsonlab.sadi.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;

public class SIOUtils
{
	protected static final Log log = LogFactory.getLog(SIOUtils.class);
	
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
	public static Resource createAttribute(Resource root, Resource attributeClass, Object value)
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
	public static Resource createAttribute(Resource root, Property p, Resource attributeClass, Object value)
	{
		Model model = root.getModel();
		Resource attribute = model.createResource(attributeClass);
		root.addProperty(p, attribute);
		if (value != null)
			attribute.addLiteral(SIO.has_value, value);
		return attribute;
	}
	
	/**
	 * Get all attribute values of the given root node that are strings and that 
	 * belong to the given attribute class. The root node, attribute node, attribute 
	 * type, and attribute class are related by the following structure:
	 * 
	 *   root [
	 *     $p, subProperty of 'has attribute' (SIO_000008)
	 *       [
	 *         rdf:type $attributeClass
	 *         'has value' (SIO_000300) $value
	 *       ]
	 *   ]
	 * 
	 * @param root the root resource
	 * @param attributeClass the type of attribute
	 * @return all String attribute values of the root node that belong to the given attribute class 
	 */
	public static Collection<String> getAttributeValues(Resource root, Resource attributeClass)
	{
		return getAttributeValues(root, SIO.has_attribute, attributeClass);
	}
	
	/**
	 * Get all attribute values of the given root node that are strings and that 
	 * belong to the given attribute class. The root node, attribute node, attribute 
	 * type, and attribute class are related by the following structure:
	 * 
	 *   root [
	 *     $p, subProperty of 'has attribute' (SIO_000008)
	 *       [
	 *         rdf:type $attributeClass
	 *         'has value' (SIO_000300) $value
	 *       ]
	 *   ]
	 * 
	 * @param root the root resource
	 * @param attributeClass the type of attribute
	 * @return all String attribute values of the root node that belong to the given attribute class 
	 */
	public static Collection<String> getAttributeValues(Resource root, Property p, Resource attributeClass)
	{
		Collection<String> values = new ArrayList<String>();

		if (root.getModel() == null) {
			log.warn(String.format("resource %s has no attributes, because it does not belong to a Model", root, attributeClass));
			return values;
		}

		for (Statement s: root.listProperties(p).toList()) {
			RDFNode object = s.getObject();
			if (object.isResource()) {
				Resource attribute = object.as(Resource.class);
				if (attribute.hasProperty(RDF.type, attributeClass)) {
					if (attribute.hasProperty(SIO.has_value)) {
						RDFNode value = attribute.getProperty(SIO.has_value).getObject();
						if(value.isLiteral()) {
							String datatypeUri = value.asLiteral().getDatatypeURI();
							if(datatypeUri == null || datatypeUri.equals(XSD.xstring.getURI())) {
								values.add(value.asLiteral().getLexicalForm());
							} 
						}
					} else {
						log.warn(String.format("encountered %s attribute for %s with no 'has value' property'", attributeClass, root));
					}
				}
			}
		}
		
		return values;
	}

}
