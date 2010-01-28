package ca.wilkinsonlab.sadi.vocab;

/**
 * 
 * @author Ben Vandervalk
 */
public class PredicateStats
{
	public static final int INFINITY = -1;

	public static final String NS = "http://biordf.net/cardioSHARE/predicatestats.owl#";
	
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
	 * The selectivity of the predicate, i.e. how many object URIs/values are connected 
	 * to one subject URI via the predicate.  (Or vice versa if isForwardSelectivity is false.) 
	 */
	public static final String PREDICATE_SELECTIVITY = NS + "selectivity";
	/**
	 * The average forward selectivity of all predicates in the database.
	 */
	public static final String PREDICATE_AVG_FORWARD_SELECTIVITY = NS + "avgForwardSelectivity";
	/**
	 * The average reverse selectivity of all predicates in the database.
	 */
	public static final String PREDICATE_AVG_REVERSE_SELECTIVITY = NS + "avgReverseSelectivity";
	/** 
	 * The time (in milliseconds) that it took to resolve the predicate with the given subject/object URI. 
	 */
	public static final String PREDICATE_TIME = NS + "time";
	/**
	 * The average forward delay time of all predicates in the database. 
	 */
	public static final String PREDICATE_AVG_FORWARD_TIME = NS + "avgForwardTime";
	/**
	 * The average reverse delay time of all predicates in the database. 
	 */
	public static final String PREDICATE_AVG_REVERSE_TIME = NS + "avgReverseTime";
	/**
	 * Timestamp indicating when this stat was recorded.  The timestamp is needed so that
	 * older stats can be flushed while newer ones are kept.
	 */
	public static final String PREDICATE_TIMESTAMP = NS + "timestamp";

}
