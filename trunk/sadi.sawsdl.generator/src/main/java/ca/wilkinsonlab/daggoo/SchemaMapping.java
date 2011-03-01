package ca.wilkinsonlab.daggoo;

public class SchemaMapping {

    private String template;

    private String name;

    public SchemaMapping() {
	setTemplate("");
	setName("");
    }

    public String getTemplate() {
	return template;
    }

    public void setTemplate(String template) {
	this.template = template == null ? "" : template;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name == null ? "" : name.trim();
    }
}
