package ca.wilkinsonlab.sadi.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class RegistrySearchCriteria
{
	enum Target { SERVICE, ATTACHED_PROPERTY }
	
	Target target;
	Set<Property> attachedProperties;
	Set<Resource> inputClasses;
	Set<Resource> connectedClasses;
	
	private RegistrySearchCriteria(Target target)
	{
		this.target = target;
		
		attachedProperties = new HashSet<Property>();
		inputClasses = new HashSet<Resource>();
	}
	
	public static RegistrySearchCriteria findService()
	{
		return new RegistrySearchCriteria(Target.SERVICE);
	}
	
	public static RegistrySearchCriteria findAttachedProperty()
	{
		return new RegistrySearchCriteria(Target.ATTACHED_PROPERTY);
	}

	public RegistrySearchCriteria addAttachedProperty(Property property)
	{
		return addAttachedProperty(property, false);
	}

	public RegistrySearchCriteria addAttachedProperty(Property property, boolean direct)
	{
		attachedProperties.add(property);
		if (!direct && property.canAs(OntProperty.class)) {
			for (Iterator<? extends Property> i = property.as(OntProperty.class).listSubProperties(); i.hasNext(); ) {
				attachedProperties.add(i.next());
			}
		}
		return this;
	}

	public RegistrySearchCriteria addInputClass(Resource clazz)
	{
		return addInputClass(clazz, false);
	}
	
	public RegistrySearchCriteria addInputClass(Resource clazz, boolean direct)
	{
		inputClasses.add(clazz);
		if (!direct && clazz.canAs(OntClass.class)) {
			for (Iterator<? extends OntClass> i = clazz.as(OntClass.class).listSubClasses(); i.hasNext(); ) {
				inputClasses.add(i.next());
			}
		}
		return this;
	}
	
	public RegistrySearchCriteria addConnectedClass(Resource clazz)
	{
		return addConnectedClass(clazz, false);
	}

	public RegistrySearchCriteria addConnectedClass(Resource clazz, boolean direct)
	{
		connectedClasses.add(clazz);
		if (!direct && clazz.canAs(OntClass.class)) {
			for (Iterator<? extends OntClass> i = clazz.as(OntClass.class).listSubClasses(); i.hasNext(); ) {
				connectedClasses.add(i.next());
			}
		}
		return this;
	}

	public Target getTarget()
	{
		return target;
	}

	public Set<Property> getAttachedProperties()
	{
		return attachedProperties;
	}

	public Set<Resource> getInputClasses()
	{
		return inputClasses;
	}

	public Set<Resource> getConnectedClasses()
	{
		return connectedClasses;
	}
}
