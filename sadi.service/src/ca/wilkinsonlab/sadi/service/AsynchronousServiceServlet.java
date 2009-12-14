package ca.wilkinsonlab.sadi.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.elmonline.util.BatchIterator;
import ca.wilkinsonlab.sadi.tasks.Task;
import ca.wilkinsonlab.sadi.tasks.TaskManager;
import ca.wilkinsonlab.sadi.utils.DurationUtils;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

@SuppressWarnings("serial")
public abstract class AsynchronousServiceServlet extends ServiceServlet
{
	private static final String POLL_PARAMETER = "poll";
	private static final String INPUT_BATCH_SIZE_KEY = "inputBatchSize";
	
	protected int inputBatchSize;
	
	public AsynchronousServiceServlet()
	{
		super();
		
		inputBatchSize = config.getInteger(INPUT_BATCH_SIZE_KEY, -1);	
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
					outputSuccessResponse(response, task.getOutputModel());
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

	private void outputInterimResponse(HttpServletResponse response, String redirectUrl, long waitTime) throws IOException
	{
		/* according to spec, "response SHOULD contain a short hypertext note with a hyperlink to the new URI(s)",
		 * but Tomcat doesn't want us to send a body with the redirect; this probably won't be a problem...
		Model model = modelMaker.createFreshModel();
		model.createResource(redirectUrl, outputClass);
		model.write(response.getWriter());
		 */
		response.setHeader("Pragma", String.format("%s = %s", SADI.ASYNC_HEADER, DurationUtils.format(waitTime)));
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
		
		for (Iterator<Collection<Resource>> batches = getInputBatches(inputModel); batches.hasNext(); ) {
			/* process each input batch in it's own task thread...
			 */
			Collection<Resource> batch = batches.next();
			InputProcessingTask task = getInputProcessingTask(batch);
			TaskManager.getInstance().startTask(task);
			
			/* add the poll location data to the output that will be returned immediately...
			 */
			Resource pollResource = outputModel.createResource(getPollUrl(call.getRequest(), task.getId()));
			for (Resource inputNode: batch) {
				Resource outputNode = outputModel.getResource(inputNode.getURI());
				outputNode.addProperty(RDFS.isDefinedBy, pollResource);
			}
		}
	}
	
	protected String getPollUrl(HttpServletRequest request, String taskId)
	{
		return String.format("%s?%s=%s", serviceUrl == null ? request.getRequestURL().toString() : serviceUrl, POLL_PARAMETER, taskId);
	}
	
	protected long getSuggestedWaitTime(Task task)
	{
		return 5000;
	}
	
	protected Iterator<Collection<Resource>> getInputBatches(Model inputModel)
	{
		if (inputBatchSize > 0) {
			return BatchIterator.batches(inputModel.listSubjectsWithProperty(RDF.type, inputClass), inputBatchSize);
		} else {
			return Collections.singleton((Collection<Resource>)inputModel.listSubjectsWithProperty(RDF.type, inputClass).toList()).iterator();
		}
	}
	
	protected abstract InputProcessingTask getInputProcessingTask(Collection<Resource> inputNodes);
	
	protected abstract class InputProcessingTask extends Task
	{
		protected Collection<Resource> inputNodes;
		protected Model outputModel;
		
		public InputProcessingTask(Collection<Resource> inputNodes)
		{
			this.inputNodes = inputNodes;
			outputModel = createOutputModel();
		}
		
		public void dispose()
		{
			outputModel.close();
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
