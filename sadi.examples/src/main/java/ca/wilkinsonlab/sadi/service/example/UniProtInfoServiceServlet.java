package ca.wilkinsonlab.sadi.service.example;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.Organism;
import uk.ac.ebi.kraken.interfaces.uniprot.ProteinDescription;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.description.Field;
import uk.ac.ebi.kraken.interfaces.uniprot.description.FieldType;
import uk.ac.ebi.kraken.interfaces.uniprot.description.Name;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

@SuppressWarnings("serial")
public class UniProtInfoServiceServlet extends UniProtServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProtInfoServiceServlet.class);
	
	public static final String ONT_PREFIX = "http://sadiframework.org/ontologies/predicates.owl#";
	public static final Property hasName = ResourceFactory.createProperty( ONT_PREFIX + "hasName" );
	public static final Property hasDescription = ResourceFactory.createProperty( ONT_PREFIX + "hasDescription" );
	public static final Property belongsToOrganism = ResourceFactory.createProperty( ONT_PREFIX + "belongsToOrganism" );
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		ProteinDescription description = input.getProteinDescription();
		if (description != null) {
			if (description.hasRecommendedName()) {
				attachName(output, description.getRecommendedName());
			}
			if (description.hasAlternativeNames()) {
				for (Name name: description.getAlternativeNames()) {
					attachName(output, name);
				}
			}
		}
		
		Organism organism = input.getOrganism();
		if (organism != null) {
			attachOrganism(output, organism);
		}
	}

//	private void attachDescription(Resource uniProtNode, ProteinDescription description)
//	{
//		uniProtNode.addProperty(hasDescription, getDescriptionString(description));
//	}
//	
//	private String getDescriptionString(ProteinDescription description)
//	{
//		StringBuilder buf = new StringBuilder();
//		buf.append("Includes: ");
//		List<Section> includes = description.getIncludes();
//		if (!includes.isEmpty()) {
//			for (Section section: includes) {
//				buf.append(getSectionString(section));
//				buf.append("; ");
//			}
//		}
//		List<Section> contains = description.getContains();
//		if (!contains.isEmpty()) {
//			buf.append("Contains: ");
//			for (Section section: contains) {
//				buf.append(getSectionString(section));
//				buf.append("; ");
//			}
//		}
//		return buf.toString();
//	}
//	
//	private String getSectionString(Section section)
//	{
//		StringBuilder buf = new StringBuilder();
//		for (Name name: section.getNames()) {
//			if (buf.length() > 0)
//				buf.append(", ");
//			buf.append(getNameString(name));
//		}
//		return buf.toString();
//	}

	private void attachName(Resource uniProtNode, Name name)
	{
		uniProtNode.addProperty(hasName, getNameString(name));
	}

	private String getNameString(Name name)
	{
		if (name == null) {
			return null;
		} else {
			StringBuilder buf = new StringBuilder();
			String fullName = getFullName(name);
			if (fullName != null) {
				buf.append(fullName);
			}
			String shortName = getShortName(name);
			if (shortName != null) {
				buf.append(buf.length() > 0 ? String.format(" (%s)", shortName) : shortName);
			}
			return buf.length() > 0 ? buf.toString() : "unknown";
		}
	}
	
	private String getFullName(Name name)
	{
		return getFieldString(name.getFieldsByType(FieldType.FULL));
	}
	
	private String getShortName(Name name)
	{
		return getFieldString(name.getFieldsByType(FieldType.SHORT));
	}
	
	private String getFieldString(List<Field> fields)
	{
		if (fields.isEmpty())
			return null;
		else if (fields.size() == 1)
			return fields.get(0).getValue();
		else
			return StringUtils.join(fields, ", ");
	}
	
	private void attachOrganism(Resource uniProtNode, Organism organism)
	{
		uniProtNode.addProperty(belongsToOrganism, nullSafeToString(organism.getScientificName()));
	}
	
	private String nullSafeToString(Object o)
	{
		if (o == null)
			return "null";
		else
			return o.toString();
	}
}
