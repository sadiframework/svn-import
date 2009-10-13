package ca.wilkinsonlab.sadi.vocab;

/**
 * 
 * @author Ben Vandervalk
 */
public class SPARQLRegistryOntology
{
	public static final String NS = "http://sadiframework.org/ontologies/sparqlregistry.owl#";

	/**
	 * points from endpointURI => boolean (true if the predicate list for the endpoint is complete)
	 */
	public static final String PREDICATE_PREDICATE_LIST_IS_COMPLETE = NS + "predicateListIsComplete";
	/**
	 * points from endpointURI => SPARQL endpoint status (OK, SLOW, or DEAD)
	 */
	public static final String PREDICATE_ENDPOINTSTATUS = NS + "endpointStatus";
	/**
	 * points from endpointURI => predicateURI
	 */
	public static final String PREDICATE_HASPREDICATE = NS + "hasPredicate";
	/**
	 * points from endpointURI => long (the number of triples the endpoint contains)
	 */
	public static final String PREDICATE_NUMTRIPLES = NS + "numTriples";
	
	/**
	 * points from endpointURI => long (the number of triples the endpoint contains).
	 * In the case that we cannot determine the exact number of triples in an endpoint,
	 * this will be set to the largest possible lower bound on that number.
	 */
	public static final String PREDICATE_NUMTRIPLES_LOWER_BOUND = NS + "numTriplesLowerBound";
	
	/**
	 * points from endpointURI => regular expression for subject URIs
	 */
	public static final String PREDICATE_SUBJECT_REGEX = NS + "subjectRegEx";
	/**
	 * points from endpointURI => boolean (true if subject regular expression covers all subject URIs in endpoint)
	 */
	public static final String PREDICATE_SUBJECT_REGEX_IS_COMPLETE = NS + "subjectRegExIsComplete";
	/**
	 * points from endpointURI => regular expression for object URIs (does not include literals)
	 */
	public static final String PREDICATE_OBJECT_REGEX = NS + "objectRegEx";
	/**
	 * points from endpointURI => boolean (true if object regular expression covers all object URIs in endpoint)
	 */
	public static final String PREDICATE_OBJECT_REGEX_IS_COMPLETE = NS + "objectRegExIsComplete";

	public static final int NO_VALUE_AVAILABLE = -1;
	
}
