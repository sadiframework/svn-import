package ca.wilkinsonlab.sadi.client.virtual.sparql;

import com.hp.hpl.jena.rdf.model.Model;

public class ConstructQueryResult extends SPARQLQueryResult {
	
	Model resultModel;

	public ConstructQueryResult(String originalQuery, Model resultTriples) {
		super(originalQuery);
		setResultModel(resultTriples);
	}
	
	public ConstructQueryResult(String originalQuery, Exception exception) {
		super(originalQuery, exception);
	}

	public Model getResultModel() {
		return resultModel;
	}

	public void setResultModel(Model resultTriples) {
		this.resultModel = resultTriples;
	}

}
