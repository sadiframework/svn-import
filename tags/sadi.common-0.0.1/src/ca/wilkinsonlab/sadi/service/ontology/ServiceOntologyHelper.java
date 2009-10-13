package ca.wilkinsonlab.sadi.service.ontology;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * An interface that reads/writes service configuration from/to an RDF model.
 * Implementing classes will read/write data according to a specific ontology.
 *  
 * @author Luke McCarthy
 */
public interface ServiceOntologyHelper
{
	public String getName();
	public void setName(String string);

	public String getDescription();
	public void setDescription(String string);

	public Resource getInputClass();
	public void setInputClass(String string);

	public Resource getOutputClass();
	public void setOutputClass(String string);
}
