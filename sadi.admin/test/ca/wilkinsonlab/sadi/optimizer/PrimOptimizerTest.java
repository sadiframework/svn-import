package ca.wilkinsonlab.sadi.optimizer;

import java.util.List;

import aterm.ATermAppl;
import ca.wilkinsonlab.sadi.optimizer.PelletOptimizer.UnresolvablePredicateException;
import ca.wilkinsonlab.sadi.utils.Pellet2JenaUtils;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryPattern;

/**
 * Test a static query optimizer using a mock query,
 * composed of mock predicates with associated mock statistics.
 * For a graphical representation of the test query see
 * testquery.png, in ca.wilkinsonlab.sadi.optimizer.resources.
 *
 * @author Ben Vandervalk
 */
public class PrimOptimizerTest extends StaticOptimizerTest {

	@Test
	public void testOptimize() 
	{

		PelletOptimizer optimizer = new PelletOptimizer(testKB);
		optimizer.setStaticOptimizer(new PrimOptimizer());
		Query optimizedQuery = optimizer.optimize(testQuery);
				
		List<QueryPattern> patterns = optimizedQuery.getQueryPatterns();

		assertTrue(patterns.size() == 7);
		
		ATermAppl a = Pellet2JenaUtils.getProperty("pred:a");
		ATermAppl b = Pellet2JenaUtils.getProperty("pred:b");
		ATermAppl c = Pellet2JenaUtils.getProperty("pred:c");
		ATermAppl d = Pellet2JenaUtils.getProperty("pred:d");
		ATermAppl e = Pellet2JenaUtils.getProperty("pred:e");
		ATermAppl f = Pellet2JenaUtils.getProperty("pred:f");
		ATermAppl g = Pellet2JenaUtils.getProperty("pred:g");
		
		ATermAppl p0 = patterns.get(0).getPredicate();
		ATermAppl p1 = patterns.get(1).getPredicate();
		ATermAppl p2 = patterns.get(2).getPredicate();
		ATermAppl p3 = patterns.get(3).getPredicate();
		ATermAppl p4 = patterns.get(4).getPredicate();
		ATermAppl p5 = patterns.get(5).getPredicate();
		ATermAppl p6 = patterns.get(6).getPredicate();
		
		// Test the expected output order of the triples patterns .
		assertTrue(p0.equals(a));
		assertTrue(p1.equals(b));
		assertTrue(p2.equals(d));
		assertTrue(p3.equals(f));
		assertTrue(p4.equals(e));
		// Last two predicates should be c and g, but may occur in either order.
		assertTrue( (p5.equals(c) && p6.equals(g)) || (p5.equals(g) && p6.equals(c)) );
		
	}
	
}
