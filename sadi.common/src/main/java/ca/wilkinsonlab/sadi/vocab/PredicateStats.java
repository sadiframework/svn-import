package ca.wilkinsonlab.sadi.vocab;

public class PredicateStats 
{
	public static final String NS = "http://sadiframework.org/ontologies/predicatestats.owl#";

	public static final String PREDICATE = NS + "predicate";

	/* Predicates that describe samples */
	
	public static final String SAMPLE_RDF_TYPE = NS + "sample";
	public static final String DIRECTION_IS_FORWARD = NS + "directionIsForward";
	public static final String TIMESTAMP = NS + "timestamp";
	public static final String NUM_INPUTS = NS + "numInputs";
	public static final String RESPONSE_TIME = NS + "responseTime";
	
	/* Predicates that describe statistics computed from samples */

	public static final String ESTIMATED_BASE_TIME_FORWARD = NS + "baseTimeForward";
	public static final String ESTIMATED_TIME_PER_INPUT_FORWARD = NS + "timePerInputForward";
	public static final String NUM_SAMPLES_FORWARD = NS + "numSamplesForward";

	public static final String ESTIMATED_BASE_TIME_REVERSE = NS + "baseTimeReverse";
	public static final String ESTIMATED_TIME_PER_INPUT_REVERSE = NS + "timePerInputReverse";
	public static final String NUM_SAMPLES_REVERSE = NS + "numSamplesReverse";
	
}
