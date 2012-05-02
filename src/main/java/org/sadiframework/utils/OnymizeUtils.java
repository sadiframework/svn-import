package org.sadiframework.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.sadiframework.rdfpath.RDFPath;
import org.sadiframework.rdfpath.RDFPathElement;
import org.sadiframework.utils.LabelUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;


/**
 * To onymize is to add a URI to an anonymous resource, ideally in a manner
 * that is reversible (so that the anonymous resource can be retrieved from
 * the URI). SADI does this by creating a URN based on the label of the
 * resource (or a predictable text description of the resource if there is 
 * no label).
 * 
 * @author Luke McCarthy
 */
public class OnymizeUtils
{
	/**
	 * Returns a URI based on the specified label.
	 * @param label the label
	 * @param charset the character set to use when URI-encoding the label
	 * @return a URI based on the label
	 * @throws UnsupportedEncodingException
	 */
	public static String createLabelURI(String label, String charset) throws UnsupportedEncodingException
	{
		return String.format("urn:label:%s", URLEncoder.encode(label, charset));
	}

	public static boolean isLabelURI(String uri)
	{
		return uri.startsWith("urn:label:");
	}
	
	public static String labelURItoLabel(String uri, String charset) throws UnsupportedEncodingException
	{
		return URLDecoder.decode(uri.substring(10), charset);
	}

	public static RDFPath onymizePath(RDFPath path, String charset) throws UnsupportedEncodingException
	{
		RDFPath onymousPath = new RDFPath();
		for (RDFPathElement element: path) {
			onymousPath.add(onymizePathElement(element, charset));
		}
		return onymousPath;
	}

	public static RDFPathElement onymizePathElement(RDFPathElement element, String charset) throws UnsupportedEncodingException
	{
		RDFPathElement onymousElement = new RDFPathElement();
		if (element.getProperty() != null && element.getProperty().isAnon())
			onymousElement.setProperty(ResourceFactory.createProperty(createLabelURI(LabelUtils.getLabel(element.getProperty()), charset)));
		else
			onymousElement.setProperty(element.getProperty());
		if (element.getType() != null && element.getType().isAnon())
			onymousElement.setType(ResourceFactory.createResource(createLabelURI(LabelUtils.getLabel(element.getType()), charset)));
		else
			onymousElement.setType(element.getType());
		return onymousElement;
	}

	public static RDFPath deonymizePath(OntModel model, RDFPath path, String charset) throws UnsupportedEncodingException
	{
		RDFPath pathInModel = new RDFPath();
		for (RDFPathElement element: path) {
			pathInModel.add(deonymizePathElement(model, element, charset));
		}
		return pathInModel;
	}

	public static RDFPathElement deonymizePathElement(OntModel model, RDFPathElement element, String charset) throws UnsupportedEncodingException
	{
		RDFPathElement elementInModel = new RDFPathElement();
		elementInModel.setProperty(element.getProperty().inModel(model));
		Property property = element.getProperty();
		if (property.isURIResource() && isLabelURI(property.getURI())) {
			elementInModel.setProperty(getPropertyByLabel(model, labelURItoLabel(property.getURI(), charset)));
		}
		if (elementInModel.getProperty() == null)
			elementInModel.setProperty(element.getProperty().inModel(model));
		Resource type = element.getType();
		if (type != null) {
			if (type.isURIResource() && isLabelURI(type.getURI())) {
				elementInModel.setType(getClassByLabel(model, labelURItoLabel(type.getURI(), charset)));
			}
			if (elementInModel.getType() == null)
				elementInModel.setType(element.getType().inModel(model));
		}
		return elementInModel;
	}

	public static OntClass getClassByLabel(OntModel model, String label)
	{
		ExtendedIterator<OntClass> classes = model.listClasses();
		try {
			while (classes.hasNext()) {
				OntClass c = classes.next();
				if (LabelUtils.getLabel(c).equals(label))
					return c;
			}
			return null;
		} finally {
			classes.close();
		}
	}

	public static OntProperty getPropertyByLabel(OntModel model, String label)
	{
		ExtendedIterator<OntProperty> properties = model.listOntProperties();
		try {
			while (properties.hasNext()) {
				OntProperty p = properties.next();
				if (LabelUtils.getLabel(p).equals(label))
					return p;
			}
			return null;
		} finally {
			properties.close();
		}
	}
}
