package org.sadiframework.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.sadiframework.tasks.Task;
import org.sadiframework.tasks.TaskManager;
import org.sadiframework.vocab.SADI;

import ca.elmonline.util.BatchIterator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public abstract class AsynchronousServiceServlet extends ServiceServlet
{
	private static final Logger log = Logger.getLogger(AsynchronousServiceServlet.class);
	public static final String POLL_PARAMETER = "poll";
	private static final long serialVersionUID = 1L;
	
	@Override
	public void init() throws ServletException
	{
		super.init();
		
		if (getServiceURL() == null) {
			log.warn("this asynchronous service has no explicit service URL; this will cause problems if the service is behind a proxy");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.service.ServiceServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String taskId = request.getParameter(POLL_PARAMETER);
		if (taskId != null) {
			/* set the content type on the response so that methods that only 
			 * have access to the response object know what to output...
			 */
			response.setContentType(getContentType(request).getHTTPHeader());
			
			InputProcessingTask task = (InputProcessingTask)TaskManager.getInstance().getTask(taskId);
			if (task == null) {
				outputErrorResponse(response, new Exception(String.format("no such task ID %s", taskId)));
			} else if (task.isFinished()) {
				Throwable error = task.getError();
				if (error != null) {
					outputErrorResponse(response, error);
				} else {
					super.outputSuccessResponse(response, task.getOutputModel());
					TaskManager.getInstance().disposeTask(taskId);
				}
			} else {
				String redirectUrl = getPollUrl(request, task.getId());
				long waitTime = getSuggestedWaitTime(task);
				outputInterimResponse(response, redirectUrl, waitTime);
			}
		} else {
			super.doGet(request, response);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.service.ServiceServlet#outputSuccessResponse(javax.servlet.http.HttpServletResponse, com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	protected void outputSuccessResponse(HttpServletResponse response, Model outputModel) throws IOException
	{
		response.setStatus(HttpServletResponse.SC_ACCEPTED);
		super.outputSuccessResponse(response, outputModel);
	}

	private void outputInterimResponse(HttpServletResponse response, String redirectUrl, long waitTime) throws IOException
	{
		/* according to spec, "response SHOULD contain a short hypertext note with a hyperlink to the new URI(s)",
		 * but Tomcat doesn't want us to send a body with the redirect; this probably won't be a problem...
		Model model = modelMaker.createFreshModel();
		model.createResource(redirectUrl, outputClass);
		model.write(response.getWriter());
		 */
		// continue to set old header until new client library is rolled out...
		response.setHeader("Pragma", String.format("%s = %s", SADI.ASYNC_HEADER, waitTime));
		response.setHeader("Retry-After", String.valueOf(waitTime/1000));
		response.sendRedirect(redirectUrl);
	}
	
	/* (non-Javadoc)
	 * @see org.sadiframework.service.ServiceServlet#processInput(org.sadiframework.service.ServiceCall)
	 */
	@Override
	protected void processInput(ServiceCall call)
	{
		for (Iterator<Collection<Resource>> batches = getInputBatches(call); batches.hasNext(); ) {
			Collection<Resource> batch = batches.next();
			
			/* create a new model for each input batch so that the task
			 * can dispose of it when it's done (and we can dispose of the
			 * encompassing model now...)
			 */
			Model subInputModel = createInputModel();
			Collection<Resource> newBatch = new ArrayList<Resource>(batch.size());
			for (Resource inputNode: batch) {
				subInputModel.add(ResourceUtils.reachableClosure(inputNode));
				newBatch.add(inputNode.inModel(subInputModel).as(Resource.class));
			}
			
			/* add secondary parameters to the input model...
			 */
			Resource parameters = call.getParameters();
			subInputModel.add(ResourceUtils.reachableClosure(parameters));
			parameters = parameters.inModel(subInputModel);
			
			/* process each input batch in it's own task thread...
			 */
			ServiceCall batchCall = new ServiceCall();
			batchCall.setInputModel(subInputModel);
			batchCall.setInputNodes(newBatch);
			batchCall.setOutputModel(createOutputModel());
			batchCall.setParameters(parameters);
			InputProcessingTask task = getInputProcessingTask(batchCall);
			TaskManager.getInstance().startTask(task);
			
			/* add the poll location data to the output that will be returned immediately...
			 */
			Model outputModel = call.getOutputModel();
			Resource pollResource = outputModel.createResource(getPollUrl(call.getRequest(), task.getId()));
			for (Resource inputNode: newBatch) {
				Resource outputNode = outputModel.getResource(inputNode.getURI());
				outputNode.addProperty(RDFS.isDefinedBy, pollResource);
			}
		}
	}
	
	protected String getPollUrl(HttpServletRequest request, String taskId)
	{
		/* TODO allow override of request URL?
		 * not really useful if proxies are set up properly...
		 */
		return String.format("%s?%s=%s", getServiceURL() == null ? request.getRequestURL().toString() : getServiceURL(), POLL_PARAMETER, taskId);
	}
	
	/**
	 * Returns the estimated time in ms to completion of the specified task.
	 * @param task
	 * @return the estimated time in ms to completion of the specified task.
	 */
	protected long getSuggestedWaitTime(Task task)
	{
		return 5000;
	}
	
	/**
	 * Returns the number of input instances in each batch.
	 * If -1 is returned, the input will be processed in a single batch.
	 * @return
	 */
	public int getInputBatchSize()
	{
		return -1;
	}
	
	protected Iterator<Collection<Resource>> getInputBatches(ServiceCall call)
	{
		if (getInputBatchSize() > 0) {
			return BatchIterator.batches(call.getInputNodes(), getInputBatchSize());
		} else {
			return Collections.singleton(call.getInputNodes()).iterator();
		}
	}
	
	/**
	 * Process an input batch, reading properties from input nodes and
	 * creating corresponding output nodes.
	 * @param call a ServiceCall representing the input batch
	 */
	protected void processInputBatch(ServiceCall call) throws Exception
	{
		Resource parameters = call.getParameters();
		boolean needsParameters = !parameters.hasProperty(RDF.type, OWL.Nothing);
		for (Resource inputNode: call.getInputNodes()) {
			Resource outputNode = call.getOutputModel().getResource(inputNode.getURI());
			if (needsParameters)
				processInput(inputNode, outputNode, parameters);
			else
				processInput(inputNode, outputNode);
		}
	}
	
	/**
	 * Process a single input, reading properties from an input node and 
	 * attaching properties to the corresponding output node.
	 * @param input the input node
	 * @param output the output node
	 */
	public void processInput(Resource input, Resource output) throws Exception
	{
	}
	
	/**
	 * Process a single input, reading properties from an input node and 
	 * attaching properties to the corresponding output node.
	 * @param input the input node
	 * @param output the output node
	 * @param parameters the populated parameters object
	 */
	public void processInput(Resource input, Resource output, Resource parameters) throws Exception
	{
	}
	
	/**
	 * Create a new InputProcessingTask from the specified ServiceCall.
	 * Note that this ServiceCall will not have valid 
	 * @param call
	 * @return
	 */
	protected InputProcessingTask getInputProcessingTask(ServiceCall call)
	{
		return new InputProcessingTask(call);
	}
	
	protected class InputProcessingTask extends Task
	{
		protected ServiceCall call;
		
		public InputProcessingTask(ServiceCall call)
		{
			this.call = call;
		}

		public void dispose()
		{
			call.getInputNodes().clear();
			closeInputModel(call.getInputModel());
			closeOutputModel(call.getOutputModel());
		}
		
		public Model getOutputModel()
		{
			return call.getOutputModel();
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			try {
				processInputBatch(call);
				success();
			} catch (Exception e) {
				fatalError(e);
			}
		}
	}
}
