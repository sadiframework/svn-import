package ca.wilkinsonlab.sadi.vocab;

/**
 * 
 * @author Ben Vandervalk
 */
public class SPARQLRegistry
{
	public static final String DEFAULT_REGISTRY_ENDPOINT = "http://dev.biordf.net/sparql";

	/** 
	 * The named graph where the index data for each participating endpoint is stored.
	 */
	public static final String DEFAULT_INDEX_GRAPH = "http://sparqlreg/endpoints/";
	/**
	 * The named graph where the type (object property or datatype property) of each 
	 * predicate is stored.
	 */
	public static final String DEFAULT_ONTOLOGY_GRAPH = "http://sparqlreg/ontology/";

	public static final String NS = "http://sadiframework.org/sparqlregistry.owl#";
	
	/**
	 * points from endpointURI => boolean (true if index information is available)
	 */
	public static final String PREDICATE_COMPUTEDINDEX = NS + "computedIndex";
	/**
	 * points from endpointURI => SPARQL endpoint status (OK, SLOW, or DEAD)
	 */
	public static final String PREDICATE_ENDPOINTSTATUS = NS + "endpointStatus";
	/**
	 * points from endpointURI => predicateURI
	 */
	public static final String PREDICATE_HASPREDICATE = NS + "hasPredicate";
	/** 
	 * points from endpointURI => graphURI (not currently used)
	 */
	public static final String PREDICATE_HASGRAPH =  NS + "hasGraph";
}
