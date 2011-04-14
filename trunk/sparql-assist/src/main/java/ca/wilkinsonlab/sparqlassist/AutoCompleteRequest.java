package ca.wilkinsonlab.sparqlassist;

import org.apache.log4j.Logger;

public class AutoCompleteRequest
{
	private static final Logger log = Logger.getLogger(AutoCompleteRequest.class);
	
	public enum Category { DEFAULT, NAMESPACE, INDIVIDUAL, PROPERTY }
	
	String query;
	Category category;
	String sparql;
	int caret;
	
	public AutoCompleteRequest()
	{
		caret = 0;
	}

	public String getQuery() { return query != null ? query: ""; }
	public void setQuery(String query) { this.query = query; }

	public Category getCategory() { return category != null ? category : Category.DEFAULT; }
	public void setCategory(Category category) { this.category = category; }

	public String getSPARQL() { return sparql != null ? sparql : ""; }
	public void setSPARQL(String sparql) { this.sparql = sparql; }

	public int getCaret() { return caret; }
	public void setCaret(int caret) { this.caret = caret; }
	public void setCaret(String caret)
	{
		try {
			this.caret = Integer.parseInt(caret);
		} catch (NumberFormatException e) {
			log.warn(String.format("ignoring non-integer value '%s' for caret", caret));
		}
	}

	@Override
	public String toString()
	{
		return new StringBuilder("{")
			.append("query: \"").append(getQuery())
			.append("\", category: ").append(getCategory())
			.append("\", sparql: \"")
				.append(getSPARQL().substring(0, getCaret()))
				.append("^")
				.append(getSPARQL().substring(getCaret(), getSPARQL().length()))
			.append("\"}")
			.toString();
	}
}
