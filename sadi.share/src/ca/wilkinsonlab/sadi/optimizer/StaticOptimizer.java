package ca.wilkinsonlab.sadi.optimizer;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.optimizer.PrimOptimizer.AdjacencyList;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;

public abstract class StaticOptimizer {

	public final static Log log = LogFactory.getLog(StaticOptimizer.class);

	abstract public List<Triple> optimize(List<Triple> triples, OntModel propertiesModel); 
	abstract public List<Triple> optimize(List<Triple> triples, OntModel propertiesModel, AdjacencyList adjacencyList);
	
}
