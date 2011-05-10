package ca.wilkinsonlab.sadi.share;

import java.util.List;

import com.hp.hpl.jena.graph.Triple;

/**
 * An interface for classes that sort the triple patterns of a SPARQL query
 * for optimal resolution.
 * 
 * @author Luke McCarthy
 */
public interface QueryPatternOrderingStrategy
{
	/**
	 * 
	 * @param patterns
	 * @return
	 * @throws UnresolvableQueryException
	 */
	public List<Triple> orderPatterns(List<Triple> patterns) throws UnresolvableQueryException;
}
