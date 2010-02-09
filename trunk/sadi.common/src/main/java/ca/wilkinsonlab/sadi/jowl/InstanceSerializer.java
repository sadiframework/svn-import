package ca.wilkinsonlab.sadi.jowl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author Luke McCarthy
 */
public class InstanceSerializer
{
	private static final Logger log = Logger.getLogger(InstanceSerializer.class);
	
	protected Model model;
	protected String namespace;
	
	public InstanceSerializer()
	{
		this(ModelFactory.createDefaultModel());
	}
	
	public InstanceSerializer(Model model)
	{
		this.model = model;
	}
	
	public Model getModel()
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
	
	public Resource asResource(Object o) throws SerializationException
	{
		Class<?> c = o.getClass();
		Resource r;
		try {
			Method getURL = c.getMethod("getURL", new Class[0]);
			String url = getURL.invoke(o, new Object[0]).toString();
			r = model.createResource(url);
		} catch (NoSuchMethodException e) {
			log.info(String.format("instance %s does not implement getURL(); creating anonymous Resource", o));
			r = model.createResource();
		} catch (Exception e) {
			throw new SerializationException(String.format("error calling getURL() on instance %s: %s", o, e.toString()));
		}
		
		String classURI = JOWLUtils.getClassURI(c, namespace);
		r.addProperty(RDF.type, model.createResource(classURI));
		addFieldProperties(r, o, c);
		
		return r;
	}
	
	public void addFieldProperties(Resource subject, Object javaInstance, Class<?> javaClass)
	{
		for (Field f: javaClass.getDeclaredFields()) {
			String propertyURI = JOWLUtils.getPropertyURI(f, namespace);
			Property p = model.getProperty(propertyURI);
			
			Object value;
			try {
				value = f.get(javaInstance);
			} catch (IllegalAccessException e) {
				log.error(String.format("unable to access field %s on instance %s", f, javaInstance));
				continue;
			}
			
			RDFNode object;
			RDFDatatype datatype = TypeMapper.getInstance().getTypeByClass(f.getType());
			if (datatype != null) {
				object = model.createTypedLiteral(value, datatype);
			} else {
				try {
					object = asResource(value);
				} catch (SerializationException e) {
					log.error(String.format("unable to serialize value of field %s on instance %s (%s)", f, javaInstance, value));
					continue;
				}
			}
			
			subject.addProperty(p, object);
		}
		
		Class<?> superClass = javaClass.getSuperclass();
		if (superClass != null && !superClass.equals(Object.class))
			addFieldProperties(subject, javaInstance, superClass);
	}
	
	public static Resource asResource(Object o, String namespace) throws SerializationException
	{
		InstanceSerializer serializer = new InstanceSerializer();
		serializer.setNamespace(namespace);
		return serializer.asResource(o);
	}
}
