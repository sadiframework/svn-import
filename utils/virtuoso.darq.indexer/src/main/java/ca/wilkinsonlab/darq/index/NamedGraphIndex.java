package ca.wilkinsonlab.darq.index;

import java.util.HashMap;


/**
 * This class is defined merely to create a type alias (to 
 * improve code readability).
 */
public class NamedGraphIndex extends HashMap<String,Capability>  
{
	private static final long serialVersionUID = 1L;
	
	public NamedGraphIndex() { 
		super();
	}
}
