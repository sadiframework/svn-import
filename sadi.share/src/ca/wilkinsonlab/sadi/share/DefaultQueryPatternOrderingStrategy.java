package ca.wilkinsonlab.sadi.share;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * This class attempts to order the patterns of a SPARQL query so that when
 * each pattern is encountered, it contains at most one new variable; i.e.: 
 * one variable that has not occurred in a previous pattern.
 * 
 * @author Ben Vandervalk
 */
public class DefaultQueryPatternOrderingStrategy implements QueryPatternOrderingStrategy
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( DefaultQueryPatternOrderingStrategy.class );
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.share.QueryPatternOrderingStrategy#orderPatterns(java.util.List)
	 */
	public List<Triple> orderPatterns(List<Triple> patterns) throws UnresolvableQueryException
	{
		List<Triple> patternsRemaining = new ArrayList<Triple>(patterns);
		List<Triple> patternsOrdered = new ArrayList<Triple>(patterns.size());
		Set<Node> boundVars = new HashSet<Node>();
		
		while (!patternsRemaining.isEmpty()) {
   			boolean bFoundNextPattern = false;
			for (Iterator<Triple> i = patternsRemaining.iterator(); i.hasNext(); ) {
				Triple pattern = i.next();
				Node s = pattern.getSubject();
				Node o = pattern.getObject();
				boolean bSubjIsUnboundVar = s.isVariable() && !boundVars.contains(s); 
        		boolean bObjIsUnboundVar = o.isVariable() && !boundVars.contains(o);
        		if (!bSubjIsUnboundVar || !bObjIsUnboundVar) {
        			if (bSubjIsUnboundVar)
        				boundVars.add(s);
        			if (bObjIsUnboundVar)
        				boundVars.add(o);
        			i.remove();
        			patternsOrdered.add(pattern);
        			bFoundNextPattern = true;
        			break;
        		}
			}
			if (!bFoundNextPattern) {
				throw new UnresolvableQueryException("Query cannot be ordered so that there is never more than one free variable in each pattern");
			}
		}
		
		return patternsOrdered;
	}
}
