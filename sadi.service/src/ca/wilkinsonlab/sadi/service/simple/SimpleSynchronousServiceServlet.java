package ca.wilkinsonlab.sadi.service.simple;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import ca.wilkinsonlab.sadi.service.SynchronousServiceServlet;

@SuppressWarnings("serial")
public abstract class SimpleSynchronousServiceServlet extends SynchronousServiceServlet
{
	@Override
	protected void processInput(Model inputModel, Model outputModel)
	{
		for (ResIterator i = inputModel.listSubjectsWithProperty(RDF.type, inputClass); i.hasNext(); ) {
			Resource inputNode = i.next();
			Resource outputNode = outputModel.getResource(inputNode.getURI());
			processInput(inputNode, outputNode);
		}
	}

	protected abstract void processInput(Resource input, Resource output);
}
