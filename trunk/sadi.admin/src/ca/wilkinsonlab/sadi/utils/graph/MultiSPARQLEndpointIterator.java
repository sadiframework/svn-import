package ca.wilkinsonlab.sadi.utils.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.Service.ServiceStatus;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.client.virtual.sparql.SPARQLRegistry;
import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_URI;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.test.NodeCreateUtils;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class MultiSPARQLEndpointIterator implements Iterator<Triple>
{
	protected final static Logger log = Logger.getLogger(MultiSPARQLEndpointIterator.class);
	
	protected SPARQLRegistry registry;
	protected BreadthFirstIterator<Triple> internalIterator;
	protected int maxTraversalDepth;
	protected int maxVisitsPerType;
	protected TypeTracker typeTracker;
	
	public MultiSPARQLEndpointIterator(SPARQLRegistry registry, Node_URI startNode, int maxTraversalDepth, int maxVisitsPerType) throws SADIException, IOException {
		this(registry, Collections.singleton(startNode), maxTraversalDepth, maxVisitsPerType);
	}
	
	public MultiSPARQLEndpointIterator(SPARQLRegistry registry, Collection<Node_URI> startNodes, int maxTraversalDepth, int maxVisitsPerType) throws SADIException, IOException
	{
		setRegistry(registry);
		setMaxTraversalDepth(maxTraversalDepth);
		setMaxVisitsPerType(maxVisitsPerType);
		setTypeTracker(new TypeTracker(maxVisitsPerType));
		initIterator(startNodes);
	}
	
	/**
	 * Restart the iterator with a new starting node. The same behaviour can  
	 * be achieved by instantiating a new MultiSPARQLEndpointIterator; however,
	 * the reset() method is useful because the iterator remembers visited rdf:types
	 * from previous iterations.
	 *   
	 * @param startNode
	 * @throws IOException
	 */
	public void reset(Node_URI startNode) throws SADIException, IOException {
		initIterator(Collections.singleton(startNode));
	}
	
	/**
	 * Restart the iterator with a new set of starting nodes. The same behaviour can  
	 * be achieved by instantiating a new MultiSPARQLEndpointIterator; however,
	 * the reset() method is useful because the iterator remembers visited rdf:types
	 * from previous iterations.
	 *   
	 * @param startNodes
	 * @throws IOException
	 */
	public void reset(Collection<Node_URI> startNodes) throws SADIException, IOException {
		initIterator(startNodes);
	}
	
	protected SPARQLRegistry getRegistry() {
		return registry;
	}

	protected void setRegistry(SPARQLRegistry registry) {
		this.registry = registry;
	}

	protected int getMaxTraversalDepth() {
		return maxTraversalDepth;
	}

	protected void setMaxTraversalDepth(int maxTraversalDepth) {
		this.maxTraversalDepth = maxTraversalDepth;
	}

	protected int getMaxVisitsPerType() {
		return maxVisitsPerType;
	}

	protected void setMaxVisitsPerType(int maxVisitsPerType) {
		this.maxVisitsPerType = maxVisitsPerType;
	}
	
	protected TypeTracker getTypeTracker() {
		return typeTracker;
	}

	protected void setTypeTracker(TypeTracker typeTracker) {
		this.typeTracker = typeTracker;
	}

	protected BreadthFirstIterator<Triple> getInternalIterator() {
		return internalIterator;
	}

	protected void setInternalIterator(BreadthFirstIterator<Triple> internalIterator) {
		this.internalIterator = internalIterator;
	}

	public Collection<Triple> getTriples(Collection<Node_URI> startNodes) throws SADIException, IOException
	{ 
		Collection<Triple> triples = new ArrayList<Triple>();
		for(Node node : startNodes) {

			Triple queryPattern = new Triple(node, NodeCreateUtils.create("?p"), NodeCreateUtils.create("?o"));
			String query = SPARQLStringUtils.getConstructQueryString(Collections.singletonList(queryPattern), Collections.singletonList(queryPattern));

			for(SPARQLEndpoint endpoint : getRegistry().findEndpointsByTriplePattern(queryPattern)) {
				if(getRegistry().getServiceStatus(endpoint.getURI()) == ServiceStatus.DEAD) {
					continue;
				}
				try {
					triples.addAll(endpoint.constructQuery(query));
				} 
				catch(IOException e) {
					log.trace(String.format("failed to query endpoint %s", endpoint), e);
				}
			}
		}
		return triples;
	}
	
	protected void initIterator(Collection<Node_URI> startNodes) throws SADIException, IOException
	{
		Collection<Triple> startTriples = getTriples(startNodes);
		Collection<MultiSPARQLEndpointSearchNode> searchNodes = new ArrayList<MultiSPARQLEndpointSearchNode>();
		for(Triple triple : startTriples) {
			searchNodes.add(new TypeTrackingSearchNode(getRegistry(), triple, getTypeTracker()));
		}
		setInternalIterator(new BoundedBreadthFirstIterator<Triple>(searchNodes, getMaxTraversalDepth()));
	}
	
	protected static class TypeTrackingSearchNode extends MultiSPARQLEndpointSearchNode 
	{
		TypeTracker typeTracker;
		
		public TypeTrackingSearchNode(SPARQLRegistry registry, Triple triple, TypeTracker typeTracker) {
			super(registry, triple);
			setTypeTracker(typeTracker);
		}
		
		public TypeTracker getTypeTracker() {
			return typeTracker;
		}

		public void setTypeTracker(TypeTracker typeTracker) {
			this.typeTracker = typeTracker;
		}

		@Override
		public Set<SearchNode<Triple>> getSuccessors() {
			
			Set<SearchNode<Triple>> successors = super.getSuccessors();
			
			// We only want to visit the same rdf:type a limited number of times.
			// Check if the subject URI of the new triples has an rdf:type that
			// has already been exhausted, and if so don't return any successors.

			Set<Resource> types = new HashSet<Resource>();
			boolean hasExhaustedType = false;
			Resource exhaustedType = null;
			for(SearchNode<Triple> successor : successors) {
				Triple triple = successor.getNode();
				if(triple.getPredicate().equals(RDF.type.asNode())) {
					Resource type = ResourceFactory.createResource(triple.getObject().getURI());
					if(!getTypeTracker().typeIsVisitable(type)) {
						hasExhaustedType = true;
						exhaustedType = type;
					}
					types.add(type);
				}
			}
			
			if(hasExhaustedType) {
				Node subject = successors.iterator().next().getNode().getSubject();
				log.trace(String.format("skipping triples with subject %s, which has exhausted rdf:type %s", subject, exhaustedType));
				return new HashSet<SearchNode<Triple>>();
			} 
			
			// increment visit count for all rdf:types
			for(Resource type : types) {
				getTypeTracker().visitType(type);
			}
			
			// convert successors to DataMapperSearchNodes
			Set<SearchNode<Triple>> typeTrackingSuccessors = new HashSet<SearchNode<Triple>>();
			for(SearchNode<Triple> successor : successors) {
				typeTrackingSuccessors.add(new TypeTrackingSearchNode(getRegistry(), successor.getNode(), getTypeTracker()));
			}
			
			return typeTrackingSuccessors;
		}
	}	
	
	protected static class TypeTracker 
	{
		/** The number instances that have been visited for each rdf:type. */
		Map<Resource,Integer> typeCount = new HashMap<Resource,Integer>();
		int maxVisitsPerType;
		
		public TypeTracker(int maxVisitsPerType) {
			setMaxVisitsPerType(maxVisitsPerType);
		}

		protected int getMaxVisitsPerType() {
			return maxVisitsPerType;
		}

		protected void setMaxVisitsPerType(int maxVisitsPerType) {
			this.maxVisitsPerType = maxVisitsPerType;
		}
		
		public boolean typeIsVisitable(Resource type) {
			return (typeCount.containsKey(type) ? (typeCount.get(type) < getMaxVisitsPerType()) : true);
		}

		public void visitType(Resource type) {
			if(!typeCount.containsKey(type)) {
				typeCount.put(type, 1);
			} else {
				if(typeCount.get(type) > getMaxVisitsPerType()) {
					throw new RuntimeException("not allowed to visit an exhausted rdf:type!");
				}
				typeCount.put(type, typeCount.get(type) + 1);
			}
		}
	}

	@Override
	public boolean hasNext() {
		return getInternalIterator().hasNext();
	}

	@Override
	public Triple next() {
		return getInternalIterator().next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove() is not implemented for this iterator");
	}
	
}
