package ca.wilkinsonlab.sadi.service.proxy;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceFactory;
import ca.wilkinsonlab.sadi.client.ServiceImpl;
import ca.wilkinsonlab.sadi.tasks.Task;

public class ServiceProxyTask extends Task
{
	private static final Logger log = Logger.getLogger(ServiceProxyTask.class);
	
	private String serviceURI;
	private Model inputModel;
	private Model outputModel;
	private String callback;
	
	public ServiceProxyTask(String serviceURI, Model inputModel)
	{
		this(serviceURI, inputModel, null);
	}
	
	public ServiceProxyTask(String serviceURI, Model inputModel, String callback)
	{
		super();
		
		this.serviceURI = serviceURI;
		this.inputModel = inputModel;
		this.callback = callback;
	}

	@Override
	public void run()
	{
		try {
			Service service = ServiceFactory.createService(serviceURI);
			outputModel = ((ServiceImpl)service).invokeServiceUnparsed(inputModel);
			success();
		} catch (SADIException e) {
			log.error(String.format("error invoking service %s", serviceURI), e);
			fatalError(e);
		}
	}
	
	
	
	@Override
	public void dispose()
	{
		if (inputModel != null) {
			inputModel.close();
			inputModel = null;
		}
		if (outputModel != null) {
			outputModel.close();
			outputModel = null;
		}
	}

	public Model getModel()
	{
		return outputModel;
	}
	
	public String getCallback()
	{
		return callback;
	}
}
