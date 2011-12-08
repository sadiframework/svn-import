package ca.wilkinsonlab.sadi.service.opencitations;

import com.hp.hpl.jena.rdf.model.Model;

import ca.wilkinsonlab.sadi.service.simple.SimpleSynchronousServiceServlet;

public abstract class OpenCitationsServiceServlet extends SimpleSynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;

	protected static final String SPARQL_ENDPOINT = "http://opencitations.net/sparql/";

	@Override
	protected Model prepareOutputModel(Model inputModel)
	{
		Model model = super.prepareOutputModel(inputModel);
		model.setNsPrefix("sio", "http://semanticscience.org/resource/");
		model.setNsPrefix("lsrn", "http://purl.oclc.org/SADI/LSRN/");
		model.setNsPrefix("oc", "http://sadiframework.org/ontologies/opencitations.owl#");
		return model;
	}
}
