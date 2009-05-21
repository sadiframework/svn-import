package ca.wilkinsonlab.sadi.optimizer;

import java.util.List;

import com.hp.hpl.jena.graph.Triple;

public abstract class StaticOptimizer {

	abstract public List<Triple> optimize(List<Triple> triples); 
}
