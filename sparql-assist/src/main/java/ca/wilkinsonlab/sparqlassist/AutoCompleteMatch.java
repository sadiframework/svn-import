package ca.wilkinsonlab.sparqlassist;

public class AutoCompleteMatch
{
	private String uri;
	private String label;
	private String description;
	private String value;

	public String getUri() { return uri; }
	public AutoCompleteMatch setUri(String uri) { this.uri = uri; return this; }
	public String getLabel() { return label; }
	public AutoCompleteMatch setLabel(String label) { this.label = label; return this; }
	public String getDescription() { return description; }
	public AutoCompleteMatch setDescription(String description) { this.description = description; return this; }
	public String getValue() { return value; }
	public AutoCompleteMatch setValue(String value) { this.value = value; return this; }
}
