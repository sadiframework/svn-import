package org.sadiframework.service;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

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
	public HttpServletResponse response;
	public Collection<Resource> inputNodes;
	public Resource parameters;

	/**
	 * Constructs a new empty service call.
	 */
	public ServiceCall()
	{
		inputNodes = new ArrayList<Resource>();
	}

	/**
	 * Returns the input model of this service call.
	 * Don't close this model; ServiceServlet will do it for you at the
	 * appropriate time.
	 * @return the input model
	 */
	public Model getInputModel()
	{
		return inputModel;
	}

	public void setInputModel(Model inputModel)
	{
		this.inputModel = inputModel;
	}

	/**
	 * Returns the output model of this service call.
	 * You should be able to access everything you need through
	 * getInputNodes() and getParameters(), but this method is useful if
	 * you need access to the entire model, possibly to send it to some
	 * external tool.
	 * Don't close this model; ServiceServlet will do it for you at the
	 * appropriate time.
	 * @return the output model
	 */
	public Model getOutputModel()
	{
		return outputModel;
	}

	public void setOutputModel(Model outputModel)
	{
		this.outputModel = outputModel;
	}

	/**
	 * Returns the HttpServletRequest associated with this service call.
	 * This is notably useful to access the request URI, for example to
	 * generate the polling URLs for asynchronous service requests.
	 * @return the HttpServletRequest
	 */
	public HttpServletRequest getRequest()
	{
		return request;
	}
	
	public void setRequest(HttpServletRequest request)
	{
		this.request = request;
	}

	/**
	 * Returns the HttpServletResponse associated with this service call.
	 * This is notably useful to access the output stream directly, though
	 * you should be careful in doing so because many of the other methods
	 * of ServiceServlet will be assuming it's empty and uncommitted.
	 * @return the HttpServletResponse
	 */
	public HttpServletResponse getResponse()
	{
		return response;
	}

	public void setResponse(HttpServletResponse response)
	{
		this.response = response;
	}

	/**
	 * Returns a collection view of the input nodes in this service call.
	 */
	public Collection<Resource> getInputNodes()
	{
		return inputNodes;
	}

	public void setInputNodes(Collection<Resource> inputNodes)
	{
		this.inputNodes = inputNodes;
	}

	/**
	 * Returns the parameters for this service call.
	 * The node returned will contain the default values unless a new value
	 * been specified in the input model.
	 */
	public Resource getParameters()
	{
		return parameters;
	}

	public void setParameters(Resource parameters)
	{
		this.parameters = parameters;
	}
}
