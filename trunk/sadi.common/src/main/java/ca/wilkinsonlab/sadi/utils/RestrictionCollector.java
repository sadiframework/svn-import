package ca.wilkinsonlab.sadi.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.decompose.RestrictionVisitor;

import com.hp.hpl.jena.ontology.Restriction;

public class RestrictionCollector implements RestrictionVisitor
{
	private static final Logger log = Logger.getLogger(RestrictionCollector.class);
	
	private Set<Restriction> restrictions;
	
	/* if an OntClass comes from a model with reasoning, we can find
	 * several "copies" of the same restriction from artifact equivalent
	 * classes; we don't want to store these, so maintain our own table
	 * of restrictions we've seen...
	 */
	private Set<String> seen;
	
	public RestrictionCollector()
	{
		restrictions = new HashSet<Restriction>();
		seen = new HashSet<String>();
	}
	
	public Set<Restriction> getRestrictions()
	{
		return restrictions;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.decompose.RestrictionVisitor#visit(com.hp.hpl.jena.ontology.Restriction)
	 */
	public void visit(Restriction restriction)
	{
		log.trace(String.format("found restriction %s", LabelUtils.getRestrictionString(restriction)));
		String key = getHashKey(restriction);
		if (!seen.contains(key)) {
			restrictions.add(restriction);
			seen.add(key);
		}
	}
	
	private String getHashKey(Restriction restriction)
	{
		/* TODO this is a pretty costly way of doing this; 
		 * could be a performance issue...
		 */
		return LabelUtils.getRestrictionString(restriction);
	}
}