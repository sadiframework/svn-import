package ca.wilkinsonlab.sadi.utils;

import java.util.UUID;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;

public class ResourceFactory
{

	/**
	 * Creates an instance of the specified type with the specified id
	 * in the specified model.
	 * @param type
	 * @param id
	 * @return
	 */
	public static Resource createInstance(Resource type, String id)
	{
		return createInstance(ModelFactory.createDefaultModel(), type, id);
	}
	
	/**
	 * Creates an instance of the specified type with the specified id
	 * in the specified model.
	 * @param model
	 * @param type
	 * @param id
	 * @return
	 */
	public static Resource createInstance(Model model, Resource type, String id)
	{
		if (LSRNUtils.isLSRNType(type))
			return LSRNUtils.createInstance(model, type, id);
		
		String uri = String.format("urn:uuid:%s", UUID.nameUUIDFromBytes(id.getBytes()));
		Resource r = type != null ? model.createResource(uri, type) : model.createResource(uri);
		r.addProperty(DC.identifier, id);
		return r;
	}
}
