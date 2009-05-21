package ca.wilkinsonlab.sadi.vocab;

/**
 * 
 * @author Ben Vandervalk
 */
public class PredicateStats
{
	public static final String GRAPH_PREDSTATS = "http://predstats/";
	
	public static final String NS = "http://sadiframework.org/predicatestats.owl#";
	
	/**
	 * Points from a predicate URI to a node that represents a selectivity sample.
	 */
	public static final String PREDICATE_SELECTIVITYSAMPLE = NS + "selectivitySample";
	/**
	 * Points from a predicate URI to a node that represents a time sample.
	 */
	public static final String PREDICATE_TIMESAMPLE = NS + "timeSample";
	/** 
	 * Boolean which indicates whether the selectivity of the predicate was measured
	 * in the forward direction or the reverse direction
	 */ 
	public static final String PREDICATE_DIRECTION_IS_FORWARD = NS + "directionIsForward";
	/** 
	 * The selectivity of the predicate, i.e. how many object URIs are connected 
	 * to one subject URI via the predicate.  (Or vice versa if isForwardSelectivity is false.) 
	 */
	public static final String PREDICATE_SELECTIVITY = NS + "selectivity";
	/** 
	 * The time (in milliseconds) that it took to resolve the predicate with the given subject/object URI. 
	 */
	public static final String PREDICATE_TIME = NS + "time";
	/**
	 * Timestamp indicating when this stat was recorded.  The timestamp is needed so that
	 * older stats can be flushed while newer ones are kept.
	 */
	public static final String PREDICATE_TIMESTAMP = NS + "timestamp";

	public static final String DEFAULT_PREDSTATSDB_URI = "http://localhost:8890/sparql";
}
