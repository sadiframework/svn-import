package ca.wilkinsonlab.sadi.vocab;

/**
 * 
 * @author Ben Vandervalk
 */
public class SPARQLRegistryOntology
{
	public static final String URI = "http://sadiframework.org/ontologies/sparqlregistry.owl";
	public static final String NS = URI + "#";

	/**
	 * True if the index contains all predicates that occur in the endpoint
	 */
	public static final String PREDICATE_LIST_IS_COMPLETE = NS + "predicateListIsComplete";
	/**
	 * Status of the endpoint, the last time it was contacted.  Possible values: ALIVE, SLOW, or DEAD.
	 */
	public static final String ENDPOINT_STATUS = NS + "endpointStatus";
	/**
	 * The endpoint contains triples with the given predicate.
	 */
	public static final String HAS_PREDICATE = NS + "hasPredicate";
	/**
	 * The endpoint contains the given number of RDF triples.
	 */
	public static final String NUM_TRIPLES = NS + "numTriples";
	
	/**
	 * The endpoint contains the given number of triples, or more.
	 */
	public static final String NUM_TRIPLES_LOWER_BOUND = NS + "numTriplesLowerBound";
	
	/**
	 * Some or all subject URIs in the endpoint match the given regex. 
	 */
	public static final String SUBJECT_REGEX = NS + "subjectRegEx";
	/**
	 * The regex for the subject URIs is complete.
	 */
	public static final String SUBJECT_REGEX_IS_COMPLETE = NS + "subjectRegExIsComplete";
	/**
	 * Some or all object URIs in the endpoint match the given regex. 
	 */
	public static final String OBJECT_REGEX = NS + "objectRegEx";
	/**
	 * The regex for the object URIs is complete.
	 */
	public static final String OBJECT_REGEX_IS_COMPLETE = NS + "objectRegExIsComplete";
	/**
	 * The date that the endpoint index was last updated.
	 */
	public static final String LAST_UPDATED = NS + "lastUpdated";

	public static final int NO_VALUE_AVAILABLE = -1;
	
}
