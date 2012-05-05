package org.sadiframework.client.example;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.sadiframework.rdfpath.RDFPath;
import org.sadiframework.rdfpath.RDFPathUtils;
import org.sadiframework.utils.OwlUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;

/** 
 * This class showcases some of the features available in the SADI toolkit.
 * We start by creating an individual that has some properties.  Using this
 * individual as a template, we string some library functions together to
 * create an OWL class with property restrictions that match the data on
 * the individual so that any other individual with similar properties can
 * be dynamically identified as a member of the class.
 * 
 * @author Luke McCarthy
 */
public class GenerateClassFromIndividual 
{
	public static void main(String[] args)
	{
		/* this would be more useful if you had an individual from some 
		 * other source, but here we'll construct it programatically...
 		 */
		Model model = ModelFactory.createDefaultModel();
		Resource guy = model.createResource("http://example.com/guy", FOAF.Person);
		guy.addProperty(FOAF.name, "Guy Incognito");
		guy.addProperty(FOAF.birthday, model.createTypedLiteral(Calendar.getInstance()));
		System.out.println("here is the individual");
		model.write(System.out, "N3");
		
		/* we need an ontology model, but we won't be doing any actual
		 * reasoning, so we can use the OWL_MEM OntModelSpec;
		 * see http://jena.sourceforge.net/ontology/ for more information
		 * about ontologies in Jena...
		 */
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		
		/* create a list of OWL property restrictions that describe the
		 * data attached to our individual resource...
		 */
		List<Restriction> restrictions = new ArrayList<Restriction>();
		for (RDFPath path: RDFPathUtils.getLeafPaths(guy)) {
			restrictions.add(OwlUtils.createRestrictions(ontModel, path));
		}
		
		/* now create a class that encapsulates those restrictions.
		 * if there's more than one restriction, we need to create a
		 * class that's the union of them; otherwise, we create a
		 * class that's equivalent to the single restriction...
		 * we use equivalent/union classes so that the restrictions are
		 * necessary and sufficient for class membership;
		 * see http://elmonline.ca/sw/owl1.html for more info...
		 */
		OntClass guyClass;
		if (restrictions.size() > 1) {
			RDFList members = ontModel.createList(restrictions.iterator());
			guyClass = ontModel.createIntersectionClass("http://example.com/GuyClass", members);
		} else {
			guyClass = ontModel.createClass("http://example.com/GuyClass");
			guyClass.setEquivalentClass(restrictions.get(0));
		}
		System.out.println("and here is an OWL class that describes them");
		ontModel.write(System.out, "RDF/XML-ABBREV");
	}
}
