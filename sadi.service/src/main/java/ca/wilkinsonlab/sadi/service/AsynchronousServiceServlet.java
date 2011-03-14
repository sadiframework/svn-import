package ca.wilkinsonlab.sadi.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import ca.elmonline.util.BatchIterator;
import ca.wilkinsonlab.sadi.tasks.Task;
import ca.wilkinsonlab.sadi.tasks.TaskManager;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ResourceUtils;
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
	 * @see ca.wilkinsonlab.sadi.service.ServiceServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String taskId = request.getParameter(POLL_PARAMETER);
		if (taskId != null) {
			InputProcessingTask task = (InputProcessingTask)TaskManager.getInstance().getTask(taskId);
			if (task == null) {
				/* TODO send error response...
				 */
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
		response.setHeader("Retry-After", String.valueOf(waitTime/1000));
		response.sendRedirect(redirectUrl);
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ServiceServlet#processInput(ca.wilkinsonlab.sadi.service.ServiceCall)
	 */
	@Override
	protected void processInput(ServiceCall call)
	{
		Model inputModel = call.getInputModel();
		Model outputModel = call.getOutputModel();
		
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
			
			/* process each input batch in it's own task thread...
			 */
			InputProcessingTask task = getInputProcessingTask(subInputModel, newBatch);
			TaskManager.getInstance().startTask(task);
			
			/* add the poll location data to the output that will be returned immediately...
			 */
			Resource pollResource = outputModel.createResource(getPollUrl(call.getRequest(), task.getId()));
			for (Resource inputNode: newBatch) {
				Resource outputNode = outputModel.getResource(inputNode.getURI());
				outputNode.addProperty(RDFS.isDefinedBy, pollResource);
			}
		}
	
		/* input model is partitioned among the input processing tasks, so
		 * we can dispose of it here...
		 */
		closeInputModel(inputModel);
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
	
	protected abstract InputProcessingTask getInputProcessingTask(Model inputModel, Collection<Resource> inputNodes);
	
	protected abstract class InputProcessingTask extends Task
	{
		protected Model inputModel;
		protected Collection<Resource> inputNodes;
		protected Model outputModel;
		
		public InputProcessingTask(Model inputModel, Collection<Resource> inputNodes)
		{
			this.inputModel = inputModel;
			this.inputNodes = inputNodes;
			outputModel = createOutputModel();
		}
		
		public void dispose()
		{
			inputNodes.clear();
			closeInputModel(inputModel);
			closeOutputModel(outputModel);
		}
		
		public Collection<Resource> getInputNodes()
		{
			return inputNodes;
		}
		
		public Model getOutputModel()
		{
			return outputModel;
		}
	}
}
