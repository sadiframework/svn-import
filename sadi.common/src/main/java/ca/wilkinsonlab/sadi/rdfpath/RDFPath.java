package ca.wilkinsonlab.sadi.rdfpath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author Luke McCarthy
 */
public class RDFPath extends ArrayList<RDFPathElement>
{
	private static final long serialVersionUID = 2L;
	private static final Pattern stringParsePattern = Pattern.compile("(.*) some (.*)");
	
	public boolean reuseExistingNodes;
	
	/**
	 * Constructs a new empty RDFPath.
	 */
	public RDFPath()
	{
		super();
		reuseExistingNodes = true;
	}
	
	/**
	 * Constructs a new RDFPath from the specified chain of property/class pairs.
	 * @param path the chain of properties/class pairs
	 */
	public RDFPath(List<RDFPathElement> path)
	{
		super(path);
		reuseExistingNodes = true;
	}
	
//	/**
//	 * Constructs a new RDFPath from the specified chain of properties/classes.
//	 * @param path the chain of properties/classes
//	 */
//	public RDFPath(List<Resource> path)
//	{
//		super((path.size()+1)/2);
//		reuseExistingNodes = true;
//		
//		if (path.size() % 2 > 0)
//			throw new IllegalArgumentException("path must contain an even number of elements in property/class pairs");
//		
//		for (int i=0; i<path.size(); ) {
//			Property property = path.get(i++).as(Property.class);
//			Resource type = path.get(i++);
//			add(new RDFPathElement(property, type));
//		}
//	}
	
	/* (non-Javadoc)
	 * @see java.util.AbstractList#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o)
	{
		/* we shouldn't actually need to do this; Collection.equals() and
		 * RDFPathElement.equals() do the right thing...
		 */
		return super.equals(o);
	}

	@Override
	public int hashCode()
	{
		/* we shouldn't actually need to do this; Collection.hashCode() and
		 * RDFPathElement.hashCode() do the right thing...
		 */
		return super.hashCode();
	}

	/**
	 * Constructs a new RDFPath from the specified chain of properties and
	 * classes.
	 * @param resources
	 */
	public RDFPath(Resource... resources)
	{
		super((resources.length+1)/2);
		reuseExistingNodes = true;
		
		if (resources.length % 2 > 0)
			throw new IllegalArgumentException("path must contain an even number of elements in property/class pairs");
		
		for (int i=0; i<resources.length; ) {
			Property property = resources[i++].as(Property.class);
			Resource type = resources[i++];
			add(new RDFPathElement(property, type));
		}
	}
	
	/**
	 * Constructs a new RDFPath from the specified chain of property/class URIs.
	 * Each string can also be a property/class URI pair in the format
	 * "propertyURI some classURI". If the path consists of a single string,
	 * that string will be split on commas before being processed.
	 * @param path the chain of property/class URIs
	 */
	public RDFPath(String... path)
	{
		super(path.length);
		reuseExistingNodes = true;
		
		if (path.length == 1) {
			if (path[0].isEmpty())
				return;
			else if (path[0] != null)
				path = Pattern.compile("\\s*,\\s*").split(path[0]);
		}
		
		int i=0;
		while (i<path.length) {
			String propertyURI = path[i++];
			String classURI = null;
			Matcher matcher = stringParsePattern.matcher(propertyURI);
			if (matcher.matches()) {
				propertyURI = matcher.group(1);
				classURI = matcher.group(2);
			} else {
				if (i<path.length)
					classURI = path[i++];
			}
			Property property = ResourceFactory.createProperty(propertyURI);
			if (classURI == null || classURI.equals("*"))
				add(new RDFPathElement(property));
			else
				add(new RDFPathElement(property, ResourceFactory.createResource(classURI)));
		}
	}
	
//	/**
//	 * Constructs a new RDFPath from the specified string.
//	 * The string should be in the format produced by RDFPath.toString().
//	 * @param path the path string
//	 */
//	public RDFPath(String path)
//	{
//		super();
//		
//		Scanner scanner = new Scanner(path);
//	}
	
	/**
	 * Constructs a new RDFPath created by appending the specified property
	 * and class to the specified parent path
	 * @param parent the parent path
	 * @param property the property to append
	 * @param type the class to append
	 */
	public RDFPath(RDFPath parent, Property property, Resource type)
	{
		super(parent);
		add(new RDFPathElement(property, type));
		reuseExistingNodes = parent.reuseExistingNodes;
	}
	
	public void add(Property property, Resource type)
	{
		add(new RDFPathElement(property, type));
	}
	
	/* (non-Javadoc)
	 * @see java.util.Object#toString()
	 */
	@Override
	public String toString()
	{
		return StringUtils.join(this, ", ");
	}
	
	public String toString(Object root)
	{
		StringBuilder buf = new StringBuilder();
		buf.append("[");
		buf.append(root);
		buf.append("]");
		for (Iterator<RDFPathElement> i = this.iterator(); i.hasNext(); ) {
			RDFPathElement element = i.next();
			buf.append(" =[");
			buf.append(element);
			buf.append("]=>");
		}
		return buf.toString();
	}
	
//	public String toSPARQLClause()
//	{
//		return toSPARQLClause("?root", new StringBuilder());
//	}
//	
//	private String toSPARQLClause(String variable, StringBuilder buf)
//	{
//		if (isEmpty())
//			return buf.toString();
//		
//		buf.append(variable);
//		buf.append(" ");
//		Property p = getProperty();
//		if (p.isURIResource()) {
//			buf.append("<");
//			buf.append(p.getURI());
//			buf.append(">");
//		} else {
//			// TODO this should really be an error...
//			buf.append("[]");
//		}
//		buf.append(" ");
//		return buf.toString();
//	}

	/* TODO catch UnsupportedPolymorphismException and
	 * ArrayIndexOutOfBoundsException and re-throw with more info?
	 */
	public Property getProperty()
	{
		return get(0).getProperty();
	}
	public Resource getType()
	{
		return get(0).getType();
	}
	
	public RDFPathElement getLastPathElement()
	{
		return isEmpty() ? null: get(size()-1);
	}
	
	/**
	 * Returns the current path with the first element removed.
	 * @return the current path with the first element removed
	 */
	public RDFPath getChildPath()
	{
		if (size() <= 1) {
			RDFPath childPath = new RDFPath();
			childPath.reuseExistingNodes = reuseExistingNodes;
			return childPath;
		} else {
			RDFPath childPath = new RDFPath(subList(1, size()));
			childPath.reuseExistingNodes = reuseExistingNodes;
			return childPath;
		}
	}
	
	/**
	 * Return the leaf nodes of the path starting from the specified node.
	 * @param root the root node
	 * @return the leaf nodes
	 */
	public Collection<RDFNode> getValuesRootedAt(Resource root)
	{
		return getValuesRootedAt(Collections.singleton(root));
	}
	
	/**
	 * Return the leaf nodes of the path starting from the specified node,
	 * potentially throwing a runtime exception if any property in the path
	 * is missing.
	 * @param root the root node
	 * @param required if true, throw an exception
	 * @return the leaf nodes
	 */
	public Collection<RDFNode> getValuesRootedAt(Resource root, boolean required)
	{
		return getValuesRootedAt(Collections.singleton(root), required);
	}
	
	/**
	 * Return the leaf nodes of the path starting from the specified nodes.
	 * @param roots the root nodes
	 * @return the leaf nodes
	 */
	public Collection<RDFNode> getValuesRootedAt(Iterable<Resource> roots)
	{
		return getValuesRootedAt(roots.iterator());
	}
	
	/**
	 * Return the leaf nodes of the path starting from the specified nodes,
	 * potentially throwing a runtime exception if any property in the path
	 * is missing.
	 * @param root the root nodes
	 * @param required if true, throw an exception
	 * @return the leaf nodes
	 */
	public Collection<RDFNode> getValuesRootedAt(Iterable<Resource> roots, boolean required)
	{
		return getValuesRootedAt(roots.iterator(), required);
	}
	
	/**
	 * Return the leaf nodes of the path starting from the specified nodes.
	 * @param roots the root nodes
	 * @return the leaf nodes
	 */
	public Collection<RDFNode> getValuesRootedAt(Iterator<Resource> roots)
	{
		return getValuesRootedAt(roots, false);
	}
	
	/**
	 * Return the leaf nodes of the path starting from the specified nodes,
	 * potentially throwing a runtime exception if any property in the path
	 * is missing.
	 * @param root the root nodes
	 * @param required if true, throw an exception
	 * @return the leaf nodes
	 */
	public Collection<RDFNode> getValuesRootedAt(Iterator<Resource> roots, boolean required)
	{
		return accumulateValuesRootedAt(roots, new ArrayList<RDFNode>(), required);
	}
	
	/* Computes the leaf nodes of the path starting from the specified root nodes
	 * and adds them to the specified collection, returning that collection.
	 * TODO this is pretty inefficient, creating multiple copies of the path as
	 * it recurses through; if this becomes a problem, just change it to loop
	 * rather than recurse at the cost of a little readability...
	 */
	Collection<RDFNode> accumulateValuesRootedAt(Iterator<? extends RDFNode> roots, Collection<RDFNode> values, boolean required)
	{
		if (isEmpty()) {
			// meaningless operation; we have to have at least one property in the path...
			return values;
		}

		Property p = getProperty();
		Resource type = getType(); // might be null...
		Collection<RDFNode> matchingChildren = new ArrayList<RDFNode>();
		while (roots.hasNext()) {
			RDFNode rootNode = roots.next();
			if (rootNode.isResource()) {
				Resource root = rootNode.asResource();
				collectNodesOfType(new PropertyValueIterator(root.listProperties(p)), type, matchingChildren);
			}
		}
		if (required && matchingChildren.isEmpty())
			throw new RuntimeException(String.format("no values of '%s' that are instances of '%s'", p, type)); // TODO throw same exception as above...
		
		RDFPath childPath = getChildPath();
		if (childPath.isEmpty())
			values.addAll(matchingChildren);
		else
			childPath.accumulateValuesRootedAt(matchingChildren.iterator(), values, required);
		
		return values;
	}
	
	static void collectNodesOfType(Iterator<? extends RDFNode> nodes, Resource type, Collection<RDFNode> matches)
	{
		while (nodes.hasNext()) {
			RDFNode node = nodes.next();
			if ((type == null) || // wild card...
				(node.isResource() && node.asResource().hasProperty(RDF.type, type)) ||
				(node.isLiteral() && matchesDatatype(node.asLiteral(), type))) {
					matches.add(node);
			}
		}
	}

	private static boolean matchesDatatype(Literal literal, Resource datatype)
	{
		if (datatype.equals(RDFS.Literal))
			return true;
		else if (datatype.isURIResource() && datatype.getURI().equals(literal.getDatatypeURI()))
			return true;
		else
			return false;
	}
	
	/**
	 * 
	 * @param root
	 * @param value
	 */
	public void addValueRootedAt(Resource root, RDFNode value)
	{
		addValuesRootedAt(root, Collections.singleton(value));
	}
	
	/**
	 * 
	 * @param root
	 * @param values
	 */
	public void addValuesRootedAt(Resource root, Iterable<RDFNode> values)
	{
		addValuesRootedAt(root, values.iterator());
	}

	/**
	 * @param root
	 * @param values
	 */
	public void addValuesRootedAt(Resource root, Iterator<RDFNode> values)
	{
		if (isEmpty()) {
			// meaningless operation; we have to have at least one property in the path...
			return;
		}
		
		Property p = getProperty();
		Resource type = getType(); // might be null...
		RDFPath childPath = getChildPath();
		if (childPath.isEmpty()) {
			while (values.hasNext()) {
				root.addProperty(p, values.next());
			}
		} else {
			Collection<RDFNode> matchingChildren = new ArrayList<RDFNode>();
			if (reuseExistingNodes)
				collectNodesOfType(new PropertyValueIterator(root.listProperties(p)), type, matchingChildren);
			Resource next = null;
			if (matchingChildren.isEmpty()) {
				if (type == null)
					next = root.getModel().createResource();
				else
					next = root.getModel().createResource(type);
				root.addProperty(p, next);
			} else {
				/* TODO
				 * should we freak out if there's more than one?
				 * should we recurse into each branch?
				 */
				next = matchingChildren.iterator().next().asResource();
			}
			getChildPath().addValuesRootedAt(next, values);
		}
	}
	
	/**
	 * Replace a leaf node with the specified value.
	 * TODO unify this somehow with addValuesRootedAt
	 * @param root
	 * @param value
	 */
	public void setValueRootedAt(Resource root, RDFNode value)
	{
		if (isEmpty()) {
			// meaningless operation; we have to have at least one property in the path...
			return;
		}
		
		Property p = getProperty();
		Resource type = getType(); // might be null...
		RDFPath childPath = getChildPath();
		if (childPath.isEmpty()) {
			root.removeAll(p);
			root.addProperty(p, value);
			if (type != null && value.isResource()) {
				value.asResource().inModel(root.getModel()).addProperty(RDF.type, type);
			}
		} else {
			Collection<RDFNode> matchingChildren = new ArrayList<RDFNode>();
			collectNodesOfType(new PropertyValueIterator(root.listProperties(p)), type, matchingChildren);
			Resource next = null;
			if (matchingChildren.isEmpty()) {
				if (type == null)
					next = root.getModel().createResource();
				else
					next = root.getModel().createResource(type);
				root.addProperty(p, next);
			} else {
				/* TODO
				 * should we freak out if there's more than one?
				 * should we recurse into each branch?
				 */
				next = matchingChildren.iterator().next().asResource();
			}
			getChildPath().setValueRootedAt(next, value);
		}
	}
	
	/**
	 * Create a new resource as a leaf node of the path.
	 * If the path specified a type as the final element, the new resource
	 * will have that type.
	 * @param uri the URI of the new resource, or null for a bnode
	 * @return the new resource
	 */
	public Resource createResourceRootedAt(Resource root, String uri)
	{
		Resource node;
		if (isEmpty()) {
			node = root.getModel().createResource(uri);
		} else {
			Resource type = get(size()-1).getType();
			if (type == null)
				node = root.getModel().createResource(uri);
			else
				node = root.getModel().createResource(uri, type);
		}
		addValueRootedAt(root, node);
		return node;
	}
	
	/**
	 * Create a new literal as a leaf node of the path.
	 * Reads the datatype from the last element of the path.
	 * @param value the value of the new literal
	 * @return the new literal
	 */
	public Literal createLiteralRootedAt(Resource root, String value)
	{
		Literal literal;
		if (isEmpty()) {
			literal = RdfUtils.createTypedLiteral(value);
		} else {
			Resource type = get(size()-1).getType();
			if (type == null) {
				literal = RdfUtils.createTypedLiteral(value);
			} else {
				RDFDatatype datatype = TypeMapper.getInstance().getTypeByName(type.getURI());
				literal = ResourceFactory.createTypedLiteral(value, datatype);
			}
		}
		addValueRootedAt(root, literal);
		return literal;
	}

	/**
	 * An iterator that extracts the objects from a statement iterator.
	 * Sadly necessary because OntResource.listPropertyValues() isn't on
	 * the Resource interface.
	 * @author Luke McCarthy
	 */
	static class PropertyValueIterator implements Iterator<RDFNode>
	{
		private Iterator<Statement> i;
		
		/**
		 * Constructs a PropertyValueIterator from the specified Statement
		 * Iterator.
		 * @param i the Statement Iterator
		 */
		public PropertyValueIterator(Iterator<Statement> i)
		{
			this.i = i;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext()
		{
			return i.hasNext();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public RDFNode next()
		{
			return i.next().getObject();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		@Override
		public void remove()
		{
			i.remove();
		}
	}
}
