package ca.wilkinsonlab.sadi.service;

import javax.servlet.http.HttpServletRequest;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * A class that encapsulates all of the data required to process a service
 * call.  This includes the input model, the output model and the original
 * HTTP request object.
 * 
 * @author Luke McCarthy
 */
public class ServiceCall
{
	public Model inputModel;
	public Model outputModel;
	public HttpServletRequest request;

	public ServiceCall()
	{
	}
	
	public ServiceCall(Model inputModel, Model outputModel, HttpServletRequest request)
	{
		this();
		
		this.inputModel = inputModel;
		this.outputModel = outputModel;
		this.request = request;
	}

	public Model getInputModel()
	{
		return inputModel;
	}

	public void setInputModel(Model inputModel)
	{
		this.inputModel = inputModel;
	}

	public Model getOutputModel()
	{
		return outputModel;
	}

	public void setOutputModel(Model outputModel)
	{
		this.outputModel = outputModel;
	}

	public HttpServletRequest getRequest()
	{
		return request;
	}

	public void setRequest(HttpServletRequest request)
	{
		this.request = request;
	}
}
