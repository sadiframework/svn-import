package ca.wilkinsonlab.daggoo;

public class LoweringSchemaMapping extends SchemaMapping {

    private String sparqlQuery;

    public LoweringSchemaMapping() {
	super();
	setSparqlQuery("");
    }

    public String getSparqlQuery() {
	return sparqlQuery;
    }

    public void setSparqlQuery(String sparqlQuery) {
	this.sparqlQuery = sparqlQuery == null ? "" : sparqlQuery.trim();
    }

}
