package ca.wilkinsonlab.sadi;

/**
 * An interface for objects that have a modifiable URI.
 * @author Luke McCarthy
 */
public interface HasModifableURI extends HasURI
{
	/**
	 * Sets the URI of the object.
	 * @param uri the new URI
	 */
	void setURI(String uri);
}
