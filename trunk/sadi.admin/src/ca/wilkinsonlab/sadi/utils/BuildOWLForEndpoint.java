package ca.wilkinsonlab.sadi.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpointFactory;
import ca.wilkinsonlab.sadi.sparql.SPARQLEndpoint.AmbiguousPropertyTypeException;
import ca.wilkinsonlab.sadi.utils.graph.BoundedBreadthFirstIterator;
import ca.wilkinsonlab.sadi.utils.graph.RDFTypeConstraint;
import ca.wilkinsonlab.sadi.utils.graph.SPARQLSearchNode;
import ca.wilkinsonlab.sadi.utils.graph.SearchNode;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Small command line utility that builds an OWL file for a SPARQL endpoint.
 * The OWL file defines each predicate that occurs in the endpoint as either
 * an owl:ObjectProperty or an owl:DatatypeProperty.
 * 
 * @author Ben Vandervalk
 */

public class BuildOWLForEndpoint {

	public final static Logger log = Logger.getLogger(BuildOWLForEndpoint.class);

	protected static OntModel buildOntologyByQuery(SPARQLEndpoint endpoint) throws IOException 
	{
		OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		Set<String> properties = endpoint.getPredicates();
		for(String property : properties) {
			try {
				if(endpoint.isDatatypeProperty(property, false)) {
					log.trace("adding datatype property " + property);
					ontology.createDatatypeProperty(property);
				} else {
					log.trace("adding object property " + property);
					ontology.createObjectProperty(property);
				}
			} catch(AmbiguousPropertyTypeException e) {
				log.warn("skipping property " + property + " (has both literal and non-literal object values)");
			}
		}
		return ontology;
	}
	
	protected static OntModel buildOntologyByTraversal(SPARQLEndpoint endpoint, List<String> rootURIs, int maxTraversalDepth) throws IOException {
		
		OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		RDFTypeConstraint typeConstraint = new RDFTypeConstraint(endpoint);
		Set<String> properties = new HashSet<String>();
		
		for(String rootURI : rootURIs) {
			
			Resource root = ResourceFactory.createResource(rootURI);
			SearchNode<Resource> startNode = new SPARQLSearchNode(endpoint, root);
			Iterator<Resource> i = new BoundedBreadthFirstIterator<Resource>(startNode, typeConstraint, maxTraversalDepth);
			
			log.trace("starting traversal at root URI " + rootURI);
			while(i.hasNext()) {
				Resource subject = i.next();
				
				if(subject.isURIResource()) {
					String uri = subject.getURI();
					log.trace("visiting subject " + uri);

					String query = "CONSTRUCT { %u% ?p ?o } WHERE { %u% ?p ?o }";
					query = SPARQLStringUtils.strFromTemplate(query, uri, uri);
					Collection<Triple> triples = endpoint.constructQuery(query);

					for(Triple triple : triples) {
						String property = triple.getPredicate().getURI();
						Node o = triple.getObject();

						if(!properties.contains(property)) {
							properties.add(property);
							try {
								if(endpoint.isDatatypeProperty(property, false)) {
									log.trace("adding datatype property " + property);
									ontology.createDatatypeProperty(property);
								} else {
									log.trace("adding object property " + property);
									ontology.createObjectProperty(property);
								}
							} catch(AmbiguousPropertyTypeException e) {
								log.warn("skipping property " + property + " (has both literal and non-literal object values)");
							}
						}
						if(property.equals(RDF.type.getURI()) && o.isURI()) {
							Resource rdfType = ResourceFactory.createResource(o.getURI());
							typeConstraint.setTypeAsVisited(rdfType);
						}						
					}
				}
			}
		}

		return ontology;
	}
	
	public static class CommandLineOptions {

		public List<String> rootURIs = new ArrayList<String>();
		
		@Argument(required = true, index = 0, usage = "SPARQL endpoint URL")
		public String endpointURL = null;
		
		@Option(name = "-r", usage = "Root URI for depth first traversal of triples")
		public void addRootURI(String rootURI) { rootURIs.add(rootURI); }

		@Option(name = "-m", usage = "Max traversal depth (only applies when -r is used); default value is 7")
		public int maxTraversalDepth = 7;

		@Option(name = "-o", usage = "Output OWL filename")
		public String outputOWLFile = null;
	}
	

	public static void main(String[] args) 
	{
		// Parse command line args
		
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
		}
		catch(CmdLineException e) {
			log.error(e.getMessage());
			log.error("buildOWLForEndpoint [options...] endpointURL");
			log.error(BuildOWLForEndpoint.getUsageString(parser));
			System.exit(1);
		}
		
		// Open output file (defaults to STDOUT)
		
		Writer outputWriter = new PrintWriter(System.out);
				
		if(options.outputOWLFile != null) {
			try {
				outputWriter = new BufferedWriter(new FileWriter(options.outputOWLFile));
			}
			catch(IOException e) {
				log.error("unable to open output file: " + e.getMessage());
				System.exit(1);
			}
		}
		
		// Build the OWL ontology
		
		SPARQLEndpoint endpoint = SPARQLEndpointFactory.createEndpoint(options.endpointURL);
		OntModel ontology = null;
		
		try {
			if(options.rootURIs.size() == 0) {
				ontology = buildOntologyByQuery(endpoint);
			}
			else {
				ontology = buildOntologyByTraversal(endpoint, options.rootURIs, options.maxTraversalDepth);
			}
		} catch(IOException e) {
			log.error("error querying endpoint: " + e.getMessage());
			System.exit(1);
		}

		// Write out the OWL file

		ontology.write(outputWriter);
	}
	
	protected static String getUsageString(CmdLineParser parser) 
	{
		StringWriter writer = new StringWriter();
		parser.printUsage(writer, null);
		return writer.toString();
	}
}
