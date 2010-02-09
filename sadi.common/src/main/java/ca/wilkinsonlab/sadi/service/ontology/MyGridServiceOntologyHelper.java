package ca.wilkinsonlab.sadi.service.ontology;

import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PropertyNotFoundException;
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
	
	public MyGridServiceOntologyHelper(Resource serviceNode) throws ServiceOntologyException
	{
		this(serviceNode, false);
	}
	
	public MyGridServiceOntologyHelper(Model model, String serviceUrl) throws ServiceOntologyException
	{
		this(model, serviceUrl, true);
	}
	
	public MyGridServiceOntologyHelper(Model model, String serviceUrl, boolean createResources) throws ServiceOntologyException
	{
		this(model.createResource(StringUtils.defaultString(serviceUrl)), createResources);
	}
	
	public MyGridServiceOntologyHelper(Resource serviceNode, boolean createResources) throws ServiceOntologyException
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
			try {
				operation = root.getRequiredProperty(opProperty).getResource();
			} catch (PropertyNotFoundException e) {
				throw new ServiceOntologyException(String.format("service node %s missing required property %s", root, opProperty));
			}
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
			try {
				input = operation.getRequiredProperty(inputProperty).getResource();
				output = operation.getRequiredProperty(outputProperty).getResource();
			} catch (PropertyNotFoundException e) {
				throw new ServiceOntologyException(String.format("operation node %s missing required property %s", operation, e.getMessage()));
			}
		}
		
		nameProperty = model.createProperty(NS + "hasServiceNameText");
		descProperty = model.createProperty(NS + "hasServiceDescriptionText");
		objectTypeProperty = model.createProperty(NS + "objectType");
	}
	
	public String getName()
	{
		try {
			return root.getRequiredProperty(nameProperty).getLiteral().getLexicalForm();
		} catch (PropertyNotFoundException e) {
			return "";
		}
	}

	public void setName(String name)
	{
		Literal nameValue = model.createTypedLiteral(StringUtils.defaultString(name));
		root.addProperty(nameProperty, nameValue);
	}
	
	public String getDescription()
	{
		try {
			return root.getRequiredProperty(descProperty).getLiteral().getLexicalForm();
		} catch (PropertyNotFoundException e) {
			return "";
		}
	}

	public void setDescription(String description)
	{
		Literal descValue = model.createLiteral(StringUtils.defaultString(description));
		root.addProperty(descProperty, descValue);
	}
	
	public Resource getInputClass() throws ServiceOntologyException
	{
		try {
			return input.getRequiredProperty(objectTypeProperty).getResource();
		} catch (PropertyNotFoundException e) {
			throw new ServiceOntologyException(String.format("input node %s missing required property %s", input, objectTypeProperty));
		}
	}

	public void setInputClass(String inputClass)
	{
		Resource inputValue = model.createResource(inputClass);
		input.addProperty(objectTypeProperty, inputValue);
	}
	
	public Resource getOutputClass() throws ServiceOntologyException
	{
		try {
			return output.getRequiredProperty(objectTypeProperty).getResource();
		} catch (PropertyNotFoundException e) {
			throw new ServiceOntologyException(String.format("output node %s missing required property %s", output, objectTypeProperty));
		}
	}

	public void setOutputClass(String outputClass)
	{
		Resource outputValue = model.createResource(outputClass);
		output.addProperty(objectTypeProperty, outputValue);
	}
}
