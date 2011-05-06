package ca.wilkinsonlab.sadi.rdfpath;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author Luke McCarthy
 */
public class RDFPathUtils
{
	public static Collection<RDFPath> getLeafPaths(Resource root)
	{
		Set<RDFPath> leafPaths = new HashSet<RDFPath>();
		addLeafPaths(root, new RDFPath(), leafPaths);
		return leafPaths;
	}
	
	private static void addLeafPaths(Resource root, RDFPath pathToRoot, Set<RDFPath> accum)
	{
		StmtIterator statements = root.listProperties();
		if (statements.hasNext()) {
			while (statements.hasNext()) {
				Statement statement = statements.next();
				if (statement.getPredicate().equals(RDF.type))
					continue;
				RDFPath childPath = getChildPath(pathToRoot, statement);
				RDFNode child = statement.getObject();
				if (child.isResource()) { 
					addLeafPaths(child.asResource(), childPath, accum);
				} else {
					accum.add(childPath);
				}
			}
		} else {
			accum.add(pathToRoot);
		}
	}
	
	private static RDFPath getChildPath(RDFPath parentPath, Statement statement)
	{
		RDFPath childPath = new RDFPath(parentPath);
		Property p = statement.getPredicate();
		Resource type = null;
		RDFNode o = statement.getObject();
		if (o.isResource()) {
			type = o.asResource().getPropertyResourceValue(RDF.type);
		} else {
			Literal literal = o.asLiteral();
			RDFDatatype datatype = literal.getDatatype();
			if (datatype != null)
				type = o.getModel().createResource(datatype.getURI());
		}
		childPath.add(p);
		childPath.add(type);
		return childPath;
	}
}
