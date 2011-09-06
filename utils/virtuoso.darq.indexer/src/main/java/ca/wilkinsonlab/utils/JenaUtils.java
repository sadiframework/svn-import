package ca.wilkinsonlab.utils;

import java.util.ArrayList;
import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class JenaUtils 
{
	public static Collection<Resource> getResourceValues(Resource s, Property p) 
	{
		Collection<Resource> values = new ArrayList<Resource>();
		for(Statement statement : s.listProperties(p).toList()) {
			RDFNode object = statement.getObject();
			if(object.isResource()) {
				values.add(object.asResource());
			}
		}
		return values;
	}
}
