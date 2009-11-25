package ca.wilkinsonlab.sadi.optimizer;

import java.util.List;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.optimizer.PrimOptimizer.AdjacencyList;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;

public abstract class StaticOptimizer
{
	protected final static Logger log = Logger.getLogger(StaticOptimizer.class);

	abstract public List<Triple> optimize(List<Triple> triples, OntModel propertiesModel); 
	abstract public List<Triple> optimize(List<Triple> triples, OntModel propertiesModel, AdjacencyList adjacencyList);
	
}
