package ca.wilkinsonlab.sadi.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.share.Config;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class ResourceTyper
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog( ResourceTyper.class );
	
	private static final ResourceTyper theInstance = new ResourceTyper();
	
	public static ResourceTyper getResourceTyper()
	{
		return theInstance;
	}
	
	List<PatternSubstitution> patterns;
	
	protected ResourceTyper()
	{
		patterns = new ArrayList<PatternSubstitution>();
		for (Object compoundPattern: Config.getConfiguration().getList("share.typePattern")) {
			String[] splitPattern = ((String)compoundPattern).split(" ");
			patterns.add( new PatternSubstitution(splitPattern[0], splitPattern[1]) );
		}
	}
	
	public String getType(String uri)
	{
		return getType(ResourceFactory.createResource(uri));
	}
	
	public String getType(Resource resource)
	{
		for (PatternSubstitution pattern: patterns)
			if (pattern.matches(resource.getURI()))
				return pattern.execute(resource.getURI());
		return null;
	}
	
	public void attachType(Resource resource)
	{
		if (resource == null)
			throw new IllegalArgumentException("resource cannot be null");
		
		String typeUri = getType(resource);
		if (typeUri == null) {
			log.warn(String.format("Failed to attach type to node %s", resource));
			return;
		}
		
		Resource type = resource.getModel().createResource(typeUri);
		resource.addProperty(RDF.type, type);
	}
}