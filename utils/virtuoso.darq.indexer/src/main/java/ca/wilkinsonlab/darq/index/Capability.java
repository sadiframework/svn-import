package ca.wilkinsonlab.darq.index;

import java.util.HashSet;
import java.util.Set;

public class Capability 
{
	public final String predicateURI;
	
	// these sets aren't thread safe...
	public Set<String> subjectRegexes = new HashSet<String>();
	public Set<String> objectRegexes = new HashSet<String>();
	public long numTriples = 0;
	
	public Capability(String predicateURI) 
	{
		this.predicateURI = predicateURI;
	}

	
}
