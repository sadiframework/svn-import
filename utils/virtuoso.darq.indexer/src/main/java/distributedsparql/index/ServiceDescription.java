package distributedsparql.index;

import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

import distributedsparql.vocab.DARQ;
import distributedsparql.vocab.DARQExt;


public class ServiceDescription 
{
	public final static int NO_RESULTS_LIMIT = -1;
	
	public final String label;
	public final String description;
	public long numTriples = 0;
	public final long resultsLimit;
	
	public Map<String,NamedGraphIndex> namedGraphs = new HashMap<String,NamedGraphIndex>();

	public ServiceDescription() 
	{
		this(null, null, NO_RESULTS_LIMIT);
	}
	
	public ServiceDescription(String label, String description, long resultsLimit) 
	{ 
		this.label = label;
		this.description = description;
		this.resultsLimit = resultsLimit;
	}
	
	public Model asModel() 
	{
		Model model = ModelFactory.createDefaultModel();
		
		Resource indexRootNode = model.createResource();
		
		if(label != null && !label.trim().isEmpty()) {
			indexRootNode.addProperty(RDFS.label, model.createTypedLiteral(label));
		}

		if(description != null && !description.trim().isEmpty()) {
			indexRootNode.addProperty(RDFS.comment, model.createTypedLiteral(description));
		}
		
		if(resultsLimit != NO_RESULTS_LIMIT) {
			indexRootNode.addProperty(DARQExt.resultsLimit, model.createTypedLiteral(resultsLimit));
		}
		
		indexRootNode.addProperty(DARQ.totalTriples, model.createTypedLiteral(numTriples));
		
		for(String graphURI : namedGraphs.keySet()) {
			
			NamedGraphIndex namedGraphIndex = namedGraphs.get(graphURI);
			
			Resource graphIndexNode = model.createResource();
			graphIndexNode.addProperty(DARQExt.URI, graphURI);
			
			indexRootNode.addProperty(DARQExt.graph, graphIndexNode);
			
			for(String predicateURI : namedGraphIndex.keySet()) {
				
				Capability capability = namedGraphIndex.get(predicateURI);
				
				Resource capabilityNode = model.createResource();
				graphIndexNode.addProperty(DARQ.capability, capabilityNode);
				
				capabilityNode.addProperty(DARQ.predicate, model.createResource(predicateURI));
				capabilityNode.addProperty(DARQ.triples, model.createTypedLiteral(capability.numTriples));
				
				for(String URIPrefix : capability.subjectRegexes) {
					capabilityNode.addProperty(DARQExt.subjectRegex, model.createTypedLiteral(URIPrefix));
				}

				for(String URIPrefix : capability.objectRegexes) {
					capabilityNode.addProperty(DARQExt.objectRegex, model.createTypedLiteral(URIPrefix));
				}
				
			}
			
		}
		
		return model;
	}
}
