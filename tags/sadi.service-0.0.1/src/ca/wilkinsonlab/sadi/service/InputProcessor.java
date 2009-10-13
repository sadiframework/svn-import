package ca.wilkinsonlab.sadi.service;

import com.hp.hpl.jena.rdf.model.Resource;

public interface InputProcessor
{
	public abstract void processInput(Resource input, Resource output);
}