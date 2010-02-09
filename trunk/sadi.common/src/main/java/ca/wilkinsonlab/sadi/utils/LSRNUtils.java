package ca.wilkinsonlab.sadi.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.ontology.OntClass;
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
	public static final Pattern LSRN_NS_PATTERN = Pattern.compile("http://purl.oclc.org/SADI/LSRN/(.+?)_Record");
	public static final String LSRN_URI_PATTERN = "http://lsrn.org/$NS:$ID";
	
	/**
	 * Return a Resource that is an instance of the specified class with
	 * the specified ID.  Creates an appropriate URI automatically and
	 * types the node.
	 * @param c the class to create an instance of
	 * @param id the id of the new instance
	 * @return a Resource view of the new instance
	 */
	public static Resource getInstance(OntClass c, String id)
	{
		Matcher matcher = LSRN_NS_PATTERN.matcher(c.getURI());
		if (matcher.find()) {
			String uri = LSRN_URI_PATTERN.replace("$NS", matcher.group(1)).replace("$ID", id);
			Model model = ModelFactory.createDefaultModel();
			return model.createResource(uri, c);
		} else {
			throw new IllegalArgumentException("at present this method only works with LSRN database record classes");
		}
	}
}
