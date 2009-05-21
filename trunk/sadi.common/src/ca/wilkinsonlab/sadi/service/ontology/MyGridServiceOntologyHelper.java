package ca.wilkinsonlab.sadi.service.ontology;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * An implementation of ServiceOntologyHelper that reads/writes service
 * configuration according to the myGrid service ontology.
 * 
 * TODO better error checking so we can use this class for validation
 * 
 * @author Luke McCarthy
 */
public class MyGridServiceOntologyHelper implements ServiceOntologyHelper
{
	private static final String NS = "http://www.mygrid.org.uk/mygrid-moby-service#";
	
	private Property nameProperty;
	private Property descProperty;
	private Property objectTypeProperty;
	
	private Model model;
	private Resource root;
	private Resource operation;
	private Resource input;
	private Resource output;
	
	public MyGridServiceOntologyHelper(Resource serviceNode)
	{
		this(serviceNode, false);
	}
	
	public MyGridServiceOntologyHelper(Model model, String serviceUrl)
	{
		this(model.createResource(serviceUrl), true);
	}
	
	public MyGridServiceOntologyHelper(Resource serviceNode, boolean createResources)
	{
		root = serviceNode;
		model = serviceNode.getModel();
		String serviceUrl = root.getURI();
		
		if (createResources) {
			Resource serviceType = model.createResource(NS + "serviceDescription");
			root.addProperty(RDF.type, serviceType);
		}
		
		Property opProperty = model.createProperty(NS + "hasOperation");
		if (createResources) {
			Resource opType = model.createResource(NS + "operation");
			operation = model.createResource(serviceUrl + "#operation", opType);
			root.addProperty(opProperty, operation);
		} else {
			operation = root.getProperty(opProperty).getResource();
		}

		Property inputProperty = model.createProperty(NS + "inputParameter");
		Property outputProperty = model.createProperty(NS + "outputParameter");
		if (createResources) {
			Resource parameterType = model.createResource(NS + "parameter");
			input = model.createResource(serviceUrl + "#input", parameterType);
			operation.addProperty(inputProperty, input);
			output = model.createResource(serviceUrl + "#output", parameterType);
			operation.addProperty(outputProperty, output);
		} else {
			input = operation.getProperty(inputProperty).getResource();
			output = operation.getProperty(outputProperty).getResource();
		}
		
		nameProperty = model.createProperty(NS + "hasServiceNameText");
		descProperty = model.createProperty(NS + "hasServiceDescriptionText");
		objectTypeProperty = model.createProperty(NS + "objectType");
	}
	
	public String getName()
	{
		return ((Literal)root.getProperty(nameProperty).getObject()).getLexicalForm();
	}

	public void setName(String name)
	{
		Literal nameValue = model.createLiteral(name);
		root.addProperty(nameProperty, nameValue);
	}
	
	public String getDescription()
	{
		return ((Literal)root.getProperty(descProperty).getObject()).getLexicalForm();
	}

	public void setDescription(String description)
	{
		Literal descValue = model.createLiteral(description);
		root.addProperty(descProperty, descValue);
	}
	
	public Resource getInputClass()
	{
		return (Resource)input.getProperty(objectTypeProperty).getObject();
	}

	public void setInputClass(String inputClass)
	{
		Resource inputValue = model.createResource(inputClass);
		input.addProperty(objectTypeProperty, inputValue);
	}
	
	public Resource getOutputClass()
	{
		return (Resource)output.getProperty(objectTypeProperty).getObject();
	}

	public void setOutputClass(String outputClass)
	{
		Property objectTypeProperty = model.createProperty(NS + "objectType");
		Resource outputValue = model.createResource(outputClass);
		output.addProperty(objectTypeProperty, outputValue);
	}
}
