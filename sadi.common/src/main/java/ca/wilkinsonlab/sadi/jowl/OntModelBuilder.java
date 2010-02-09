package ca.wilkinsonlab.sadi.jowl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author Luke McCarthy
 */
public class OntModelBuilder
{
	private static final Logger log = Logger.getLogger(OntModelBuilder.class);
	
	protected OntModel model;
	protected String namespace;
	
	public OntModelBuilder()
	{
		this(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
	}
	
	public OntModelBuilder(OntModelSpec spec)
	{
		this(ModelFactory.createOntologyModel(spec));
	}
	
	public OntModelBuilder(OntModel model)
	{
		this.model = model;
	}
	
	public OntModel getOntModel()
	{
		return model;
	}
	
	public String getNamespace()
	{
		return namespace;
	}
	
	public void setNamespace(String namespace)
	{
		this.namespace = namespace;
	}
	
	public OntClass addJavaClass(Class<?> c)
	{
		OntClass ontClass = getDatatypeClass(c);
		if (ontClass != null) {
			log.debug(String.format("skipping datatype %s", ontClass));
			return ontClass;
		}
		
		String classURI = getClassURI(c);
		ontClass = model.getOntClass(classURI);
		if (ontClass != null) {
			log.debug(String.format("skipping previously-defined OWL class %s", classURI));
			return ontClass;
		}
		
		log.debug(String.format("creating Java class %s as OWL class %s", c, classURI));
		ontClass = createOntClass(classURI, c);
		return ontClass;
	}
	
	public OntProperty addJavaField(Field f)
	{
		String propertyURI = getPropertyURI(f);
		OntProperty p = model.getOntProperty(propertyURI);
		if (p == null) {
			log.debug(String.format("creating Java field %s as OWL property %s", f, propertyURI));
			p = createOntProperty(propertyURI, f);
		}
		return p;
	}
	
	public OntClass getDatatypeClass(Class<?> javaClass)
	{
		RDFDatatype datatype = TypeMapper.getInstance().getTypeByClass(javaClass);
		if (datatype != null) {
			OntClass owlClass = model.getOntClass(datatype.getURI());
			if (owlClass != null) {
				return owlClass;
			} else {
				log.warn(String.format("Java class %s mapped to datatype %s, but no corresponding OWL class could be identified", javaClass, datatype));
			}
		}
		return null;
	}
	
	public String getClassURI(Class<?> c)
	{
		OWLClass owlClassAnnot = c.getAnnotation(OWLClass.class);
		if (owlClassAnnot != null) {
			return owlClassAnnot.value();
		} else {
			String className = c.getSimpleName();
			if (className != null)
				return namespace + c.getSimpleName();
			else
				throw new IllegalArgumentException("unable to auto-generate class URI for class with no OWLClass annotation");
		}
	}
	
	public OntClass createOntClass(String classURI, Class<?> c)
	{	
		Collection<OntClass> equivalentClasses = new ArrayList<OntClass>();
		for (Field f: c.getDeclaredFields()) {
			log.debug(String.format("found field %s.%s", c, f));
			OntProperty p = addJavaField(f);
			OntClass valuesFrom = addJavaClass(f.getType());
			equivalentClasses.add(model.createSomeValuesFromRestriction(null, p, valuesFrom));
		}
		
		Class<?> superClass = c.getSuperclass();
		if (superClass != null && !superClass.equals(Object.class)) {
			OntClass ontSuperClass = addJavaClass(superClass);
			equivalentClasses.add(ontSuperClass);
		}
		
		if (equivalentClasses.size() == 1) {
			OntClass owlClass = model.createClass(classURI);
			owlClass.addEquivalentClass(equivalentClasses.iterator().next());
			return owlClass;
		} else if (equivalentClasses.size() > 1) {
			return model.createUnionClass(classURI, model.createList(equivalentClasses.iterator()));
		} else {
			return model.createClass(classURI);
		}
	}
	
	public String getPropertyURI(Field f)
	{
		OWLProperty owlPropertyAnnot = f.getAnnotation(OWLProperty.class);
		if (owlPropertyAnnot != null) {
			return owlPropertyAnnot.value();
		} else {
			return namespace + f.getName();
		}
	}
	
	public OntProperty createOntProperty(String propertyURI, Field f)
	{
		if (f.getType().isPrimitive()) {
			log.debug(String.format("creating Java field %s as OWL datatype property %s", f, propertyURI));
			return model.createDatatypeProperty(propertyURI);
		} else {
			log.debug(String.format("creating Java field %s as OWL datatype property %s", f, propertyURI));
			return model.createObjectProperty(propertyURI);
		}
	}
	
	public static OntModel buildOntModel(String namespace, Class<?> ... classes)
	{
		OntModelBuilder builder = new OntModelBuilder();
		builder.setNamespace(namespace);
		for (Class<?> c: classes) {
			builder.addJavaClass(c);
		}
		return builder.getOntModel();
	}
}
