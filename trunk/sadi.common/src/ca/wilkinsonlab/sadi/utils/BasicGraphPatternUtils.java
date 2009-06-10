package ca.wilkinsonlab.sadi.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.math.NumberUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.algorithms.shortestpath.DistanceStatistics;

public class BasicGraphPatternUtils {

	public final static Log LOGGER = LogFactory.getLog(BasicGraphPatternUtils.class);

	static public String getSPARQLQueryString(List<Triple> basicGraphPattern) throws URIException
	{
		StringBuilder query = new StringBuilder();
		query.append("SELECT * WHERE { ");
		for(Triple triple : basicGraphPattern) {
			query.append(getTriplePattern(triple));
			query.append(" ");	
		}
		query.append("}");
		String queryStr = query.toString();

		LOGGER.trace("Generated query string: ");
		LOGGER.trace(queryStr);
		
		return queryStr;
	}

	public static String getTriplePattern(Triple triple) throws URIException
	{
		StringBuilder template = new StringBuilder();
		Node sNode = triple.getSubject();
		Node pNode = triple.getPredicate();
		Node oNode = triple.getObject();
		String s = RdfUtils.getPlainString(sNode);
		String p = RdfUtils.getPlainString(pNode);
		String o = RdfUtils.getPlainString(oNode);
		
		if(sNode.isURI())
			template.append("%u% ");
		else
			template.append("%v% ");
		
		if(pNode.isURI())
			template.append("%u% ");
		else
			template.append("%v% ");
		
		if(oNode.isURI())
			template.append("%u% .");
		else if(oNode.isVariable()) 
			template.append("%v% .");
		else {
			if(NumberUtils.isNumber(o))
				template.append("%v% .");
			else
				template.append("%s% .");
		}
			
		return SPARQLStringUtils.strFromTemplate(template.toString(), s, p, o);
	}
	
	static public int getNumConstants(Collection<Triple> basicGraphPattern) 
	{
		Set<String> constants = new HashSet<String>();
		for(Triple triple : basicGraphPattern) {
			Node s = triple.getSubject();
			Node o = triple.getObject();
 			if(s.isURI() || s.isLiteral()) 
				constants.add(RdfUtils.getPlainString(s));
			if(o.isURI() || o.isLiteral())
				constants.add(RdfUtils.getPlainString(o));
		}
		return constants.size();
	}	
	
	static public int getDiameter(Collection<Triple> basicGraphPattern) 
	{
		UndirectedGraph<Node,JUNGEdge> g = getJUNGUndirectedGraph(basicGraphPattern);
		return (int)DistanceStatistics.diameter(g);
	}
	
	static protected UndirectedGraph<Node,JUNGEdge> getJUNGUndirectedGraph(Collection<Triple> basicGraphPattern)
	{
		UndirectedGraph<Node,JUNGEdge> g = new UndirectedSparseGraph<Node,JUNGEdge>();
		for(Triple triple : basicGraphPattern) {
			Node s = triple.getSubject();
			Node p = triple.getPredicate();
			Node o = triple.getObject();
			g.addVertex(s);
			g.addVertex(o);
			g.addEdge(new JUNGEdge(p), s, o);
		}
		return g;
	}
	
	/**
	 * Represents an edge in a graph for the JUNG graph algorithms/visualization
	 * library.  This class encapsulates a Jena node representing the same edge.  This 
	 * encapsulation is necessary because each edge in a JUNG graph must be 
	 * unique, whereas in an RDF graph, the same edge (predicate) may be reused
	 * many times.
	 * @author ben
	 */
	protected static class JUNGEdge 
	{
		Node predicate;
		public JUNGEdge(Node predicate)	{ this.predicate = predicate; }
		public Node getPredicate() { return this.predicate; }
	}
}
