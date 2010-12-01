package ca.wilkinsonlab.sadi.service.ontology;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Collections;

import ca.wilkinsonlab.sadi.ServiceDescription;
import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.rdfpath.RDFPath;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.PropertyNotFoundException;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author Luke McCarthy
 */
public abstract class AbstractServiceOntologyHelper implements ServiceOntologyHelper
{
	public AbstractServiceOntologyHelper()
	{	
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper#getServiceClass()
	 */
	@Override
	public Resource getServiceClass()
	{
		ServiceClass annotation = getClass().getAnnotation(ServiceClass.class);
		return annotation == null ? null : ResourceFactory.createResource(annotation.value());
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper#getServiceBean(com.hp.hpl.jena.rdf.model.Resource)
	 */
	@Override
	public ServiceDescription getServiceDescription(Resource serviceRoot) throws ServiceOntologyException
	{
		return copyServiceDescription(serviceRoot, new ServiceBean());
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper#copyServiceDescription(com.hp.hpl.jena.rdf.model.Resource, ca.wilkinsonlab.sadi.beans.ServiceBean)
	 */
	@Override
	public ServiceBean copyServiceDescription(Resource serviceRoot, ServiceBean serviceBean) throws ServiceOntologyException
	{
		if (serviceRoot.isURIResource())
			serviceBean.setURI(serviceRoot.getURI());
		else
			throw new ServiceOntologyException("specified root node has no URI");
		
		RDFNode name = getSinglePropertyValue(serviceRoot, getNamePath(), false);
		if (name != null)
			serviceBean.setName(RdfUtils.getPlainString(name));
		
		RDFNode description = getSinglePropertyValue(serviceRoot, getDescriptionPath(), false);
		if (description != null)
			serviceBean.setDescription(RdfUtils.getPlainString(description));
		
		RDFNode provider = getSinglePropertyValue(serviceRoot, getServiceProviderPath(), false);
		if (provider != null)
			serviceBean.setServiceProvider(RdfUtils.getPlainString(provider));
		
		// TODO eventually we'd like this to be required...
		RDFNode email = getSinglePropertyValue(serviceRoot, getContactEmailPath(), false);
		if (email != null) // technically not possible because of "true" above...
			serviceBean.setContactEmail(RdfUtils.getPlainString(email));
		
		RDFNode authoritative = getSinglePropertyValue(serviceRoot, getAuthoritativePath(), false);
		if (authoritative != null) {
			try {
				serviceBean.setAuthoritative(authoritative.asLiteral().getBoolean());
			} catch (Exception e) {
				throw new ServiceOntologyException(String.format("authoritative value '%s' can't be interpreted as a boolean", authoritative));
			}
		}
		
		RDFNode inputClass = null;
		try {
			inputClass = getSinglePropertyValue(serviceRoot, getInputClassPath(), true);
		} catch (ServiceOntologyException e) {
			throw new ServiceOntologyException("service description missing input class", e);
		}
		if (inputClass != null && inputClass.isURIResource()) {
			Resource inputClassNode = inputClass.asResource();
			serviceBean.setInputClassURI(inputClassNode.getURI());
			Statement label = inputClassNode.getProperty(RDFS.label);
			if (label != null)
				serviceBean.setInputClassLabel(label.getString());
		} else {
			throw new ServiceOntologyException(String.format("input class '%s' has no URI", inputClass));
		}
		
		RDFNode outputClass = null;
		try {
			outputClass = getSinglePropertyValue(serviceRoot, getOutputClassPath(), true);
		} catch (ServiceOntologyException e) {
			throw new ServiceOntologyException("service description missing output class", e);
		}
		if (outputClass != null && outputClass.isURIResource()) {
			Resource outputClassNode = outputClass.asResource();
			serviceBean.setOutputClassURI(outputClassNode.getURI());
			Statement label = outputClassNode.getProperty(RDFS.label);
			if (label != null)
				serviceBean.setOutputClassLabel(label.getString());
		} else {
			throw new ServiceOntologyException(String.format("output class '%s' has no URI", outputClass));
		}
		
		RDFNode secondaryParameterClass = getSinglePropertyValue(serviceRoot, getParameterClassPath(), false);
		if (secondaryParameterClass != null && secondaryParameterClass.isURIResource()) {
			Resource secondaryParameterClassNode = secondaryParameterClass.asResource();
			serviceBean.setParameterClassURI(secondaryParameterClassNode.getURI());
			Statement label = secondaryParameterClassNode.getProperty(RDFS.label);
			if (label != null)
				serviceBean.setParameterClassLabel(label.getString());
		}
		
		return serviceBean;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper#createServiceNode(ca.wilkinsonlab.sadi.ServiceDescription)
	 */
	@Override
	public Resource createServiceNode(ServiceDescription service)
	{
		return createServiceNode(service, ModelFactory.createDefaultModel());
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ontology.ServiceOntologyHelper#getServiceNode(com.hp.hpl.jena.rdf.model.Model, ca.wilkinsonlab.sadi.ServiceDescription)
	 */
	@Override
	public Resource createServiceNode(ServiceDescription service, Model model)
	{
		Resource serviceNode = model.createResource(service.getURI());
		
		Resource serviceClass = getServiceClass();
		if (serviceClass != null)
			serviceNode.addProperty(RDF.type, serviceClass);
		
		String name = service.getName();
		if (name != null)
			getNamePath().addValueRootedAt(serviceNode, model.createTypedLiteral(name));
		
		String description = service.getDescription();
		if (description != null)
			getDescriptionPath().addValueRootedAt(serviceNode, model.createTypedLiteral(description));
		
		String provider = service.getServiceProvider();
		if (provider != null)
			getServiceProviderPath().addValueRootedAt(serviceNode, model.createTypedLiteral(provider));
		
		String email = service.getContactEmail();
		if (email != null)
			getContactEmailPath().addValueRootedAt(serviceNode, model.createTypedLiteral(email));
		
		getAuthoritativePath().addValueRootedAt(serviceNode, model.createTypedLiteral(service.isAuthoritative()));
		
		String inputClassURI = service.getInputClassURI();
		if (inputClassURI != null)
			getInputClassPath().addValueRootedAt(serviceNode, model.createResource(inputClassURI));
		
		String outputClassURI = service.getOutputClassURI();
		if (outputClassURI != null)
			getOutputClassPath().addValueRootedAt(serviceNode, model.createResource(outputClassURI));
		
		String parameterClassURI = service.getParameterClassURI();
		if (parameterClassURI != null)
			getParameterClassPath().addValueRootedAt(serviceNode, model.createResource(parameterClassURI));
		
		return serviceNode;
	}

	public RDFPath getNamePath()
	{
		Name annotation = getClass().getAnnotation(Name.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}
	
	public RDFPath getDescriptionPath()
	{
		Description annotation = getClass().getAnnotation(Description.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}

	public RDFPath getServiceProviderPath()
	{
		ServiceProvider annotation = getClass().getAnnotation(ServiceProvider.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}
	
	public RDFPath getContactEmailPath()
	{
		ContactEmail annotation = getClass().getAnnotation(ContactEmail.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}

	public RDFPath getAuthoritativePath()
	{
		Authoritative annotation = getClass().getAnnotation(Authoritative.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}

	public RDFPath getInputClassPath()
	{
		InputClass annotation = getClass().getAnnotation(InputClass.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}

//	public RDFPath getInputClassLabelPath()
//	{
//		return new RDFPath(getInputClassPath(), RDFS.label);
//	}

	public RDFPath getOutputClassPath()
	{
		OutputClass annotation = getClass().getAnnotation(OutputClass.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}

//	public RDFPath getOutputClassLabelPath()
//	{
//		return new RDFPath(getOutputClassPath(), RDFS.label);
//	}
	
	public RDFPath getParameterClassPath()
	{
		ParameterClass annotation = getClass().getAnnotation(ParameterClass.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}
	
	public RDFPath getParameterInstancePath()
	{
		ParameterInstance annotation = getClass().getAnnotation(ParameterInstance.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}
	
	public RDFPath getTestCasePath()
	{
		TestCase annotation = getClass().getAnnotation(TestCase.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}
	
	public RDFPath getTestInputPath()
	{
		TestInput annotation = getClass().getAnnotation(TestInput.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}
	
	public RDFPath getTestOutputPath()
	{
		TestOutput annotation = getClass().getAnnotation(TestOutput.class);
		return annotation == null ? null : new RDFPath(annotation.value());
	}
	
	private static RDFNode getSinglePropertyValue(Resource root, RDFPath path, boolean required) throws ServiceOntologyException
	{
		Collection<RDFNode> values;
		try {
			if (path == null)
				values = Collections.emptySet();
			else
				values = path.getValuesRootedAt(root, required);
		} catch (PropertyNotFoundException e) {
//			throw new ServiceOntologyException(String.format("missing required property %s from %s", e.getMessage(), path.toString(root)));
			throw new ServiceOntologyException(String.format("missing required property %s", e.getMessage()));
		} catch (Exception e) {
			throw new ServiceOntologyException(String.format("no values from path %s: %s", path, e.getMessage()));
		}
		
		if (values.isEmpty()) {
			if (required)
				throw new ServiceOntologyException(String.format("no leaf nodes from %s", path.toString(root)));
			else
				return null;
		} else if (values.size() > 1) {
			throw new ServiceOntologyException(String.format("more than one leaf node from %s", path.toString(root)));
		} else {
			return values.iterator().next();
		}
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface ServiceClass
	{
		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Name
	{
		String[] value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface Description
	{
		String[] value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface ServiceProvider
	{
		String[] value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContactEmail
	{
		String[] value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface Authoritative
	{
		String[] value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface InputClass
	{
		String[] value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface OutputClass
	{
		String[] value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface ParameterClass
	{
		String[] value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@interface ParameterInstance
	{
		String[] value();
	}
	
	/**
	 * The path to one or more test cases.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface TestCase
	{
		String[] value();
	}
	
	/**
	 * The path to the test input data, rooted at the test case.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface TestInput
	{
		String[] value();
	}
	
	/**
	 * The path to the test output data, rooted at the test case.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface TestOutput
	{
		String[] value();
	}
}
