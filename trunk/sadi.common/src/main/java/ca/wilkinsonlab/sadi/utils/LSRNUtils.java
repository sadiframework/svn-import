package ca.wilkinsonlab.sadi.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Utility class encapsulating methods useful for converting simple LSRN
 * database records to and from RDF/OWL.
 * @author Luke McCarthy
 */
public class LSRNUtils
{
	public static final Pattern NS_PATTERN = Pattern.compile("http://purl.oclc.org/SADI/LSRN/(.+?)_Record");
	public static final Pattern URI_PATTEN = Pattern.compile("http://lsrn.org/([^:]+):(.+)");
	public static final String OUTPUT_URI_PATTERN = "http://lsrn.org/$NS:$ID";
	
	public static boolean isLSRNType(Resource type)
	{
		return type.isURIResource() && isLSRNType(type.getURI());
	}

	public static boolean isLSRNType(String uri)
	{
		return NS_PATTERN.matcher(uri).matches(); 
	}
	
	/**
	 * Return a Resource that is an instance of the specified type with the 
	 * specified ID.  The Resource will be created with an appropriate URI
	 * (@link {@link #OUTPUT_URI_PATTERN}).
	 * @param type the type (class) of the new instance
	 * @param id the id of the new instance
	 * @return a Resource view of the new instance
	 */
	public static Resource createInstance(Resource type, String id)
	{
		return createInstance(ModelFactory.createDefaultModel(), type, id);
	}
	
	/**
	 * Return a Resource in the specified model that is an instance of the 
	 * specified type with the specified ID.  The Resource will be created 
	 * with an appropriate URI (@link {@link #OUTPUT_URI_PATTERN}).
	 * @param model the model in which to create the new Resource
	 * @param type the type (class) of the new instance
	 * @param id the id of the new instance
	 * @return a Resource view of the new instance
	 */
	public static Resource createInstance(Model model, Resource type, String id)
	{
		Matcher matcher = NS_PATTERN.matcher(type.getURI());
		if (matcher.find()) {
			String uri = OUTPUT_URI_PATTERN.replace("$NS", matcher.group(1)).replace("$ID", id);
			return model.createResource(uri, type);
		} else {
			throw new IllegalArgumentException("at present this method only works with LSRN database record classes");
		}
	}
}
