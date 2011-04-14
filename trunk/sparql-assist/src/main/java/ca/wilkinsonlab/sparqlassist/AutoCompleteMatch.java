package ca.wilkinsonlab.sparqlassist;

public class AutoCompleteMatch
{
	String uri;
	String label;
	String description;
	String value;
	
	public AutoCompleteMatch setUri(String uri) { this.uri = uri; return this; }
	public AutoCompleteMatch setLabel(String label) { this.label = label; return this; }
	public AutoCompleteMatch setDescription(String description) { this.description = description; return this; }
	public AutoCompleteMatch setValue(String value) { this.value = value; return this; }
}
