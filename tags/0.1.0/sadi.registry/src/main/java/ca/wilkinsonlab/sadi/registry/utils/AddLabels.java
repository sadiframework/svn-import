package ca.wilkinsonlab.sadi.registry.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.registry.Registry;
import ca.wilkinsonlab.sadi.utils.OwlUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

public class AddLabels
{
	private static final Logger log = Logger.getLogger(AddLabels.class);
	
	public static void main(String[] args)
	{
		Registry registry = null;
		try{
			registry = Registry.getVirtuosoRegistry(
				"http://sadiframework.org/registry/",
				"jdbc:virtuoso://localhost:1111",
				"sadi",
				"Kl4m92"
			);
		} catch (SADIException e) { 
			log.error(e.toString(), e);
			System.exit(1);
		}
		
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
		Set<Resource> seen = new HashSet<Resource>();
		for (Iterator<Statement> i = registry.getModel().listStatements(null, OWL.onProperty, (RDFNode)null); i.hasNext(); ) {
			try {
				Statement statement = i.next();
				Resource r = statement.getResource();
				if (seen.contains(r))
					continue;
				else
					seen.add(r);
				log.info(String.format("found property %s", r));
				OntProperty p = OwlUtils.getOntPropertyWithLoad(model, r.getURI());
				String label = OwlUtils.getLabel(p);
				log.info(String.format("adding label %s to property %s", label, p));
				r.addLiteral(RDFS.label, label);
			} catch (Exception e) {
				log.error(String.format("registry contains a value of owl:onProperty that isn't a uri resource"), e);
			}
		}
		
		registry.getModel().close();
	}
}
