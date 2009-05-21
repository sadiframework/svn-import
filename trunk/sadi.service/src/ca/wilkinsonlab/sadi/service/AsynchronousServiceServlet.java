package ca.wilkinsonlab.sadi.service;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.wilkinsonlab.sadi.tasks.Task;
import ca.wilkinsonlab.sadi.tasks.TaskManager;
import ca.wilkinsonlab.sadi.utils.DurationUtils;
import ca.wilkinsonlab.sadi.vocab.SADI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public abstract class AsynchronousServiceServlet extends ServiceServlet
{
	private static final String POLL_PARAMETER = "poll";

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
				outputInterimResponse(response, task);
			}
		} else {
			outputServiceModel(response);
		}
	}

	private void outputInterimResponse(HttpServletResponse response, InputProcessingTask task) throws IOException
	{
		String redirectUrl = getPollUrl(task.getId());
		
		/* according to spec, "response SHOULD contain a short hypertext note with a hyperlink to the new URI(s)",
		 * but Tomcat doesn't want us to send a body with the redirect; this probably won't be a problem...
		Model model = modelMaker.createFreshModel();
		model.createResource(redirectUrl, outputClass);
		model.write(response.getWriter());
		 */
		response.setHeader("Pragma", String.format("%s = %s", SADI.ASYNC_HEADER, DurationUtils.format(getSuggestedWaitTime(task))));
		response.sendRedirect(redirectUrl);
	}
	
	protected long getSuggestedWaitTime(Task task)
	{
		return 5000;
	}
	
	@Override
	public void processInput(Map<Resource, Resource> inputOutputMap)
	{
		for (Resource input: inputOutputMap.keySet()) {
			/* each output will be processed and returned individually, so copy each of them
			 * to a fresh model before creating its task...
			 */
			Resource output = inputOutputMap.get(input);
			Model outputModel = createOutputModel();
			outputModel.add(output.listProperties());
			Task task = new InputProcessingTask(input, outputModel.getResource(output.getURI()), getInputProcessor());
			TaskManager.getInstance().startTask(task);
			
			/* add the poll location data to the output that will be returned immediately...
			 */
			Resource pollResource = output.getModel().createResource(getPollUrl(task.getId()));
			output.addProperty(RDFS.isDefinedBy, pollResource);
		}
	}
	
	private String getPollUrl(String taskId)
	{
		/* TODO use request.getRequestURL instead?
		 */
		return String.format("%s?%s=%s", serviceUrl, POLL_PARAMETER, taskId);
	}
	
	private static class InputProcessingTask extends Task
	{
		private Resource input;
		private Resource output;
		private InputProcessor processor;
		
		public InputProcessingTask(Resource input, Resource output, InputProcessor processor)
		{
			this.input = input;
			this.output = output;
			this.processor = processor;
		}
		
		public void run()
		{
			processor.processInput(input, output);
			success();
		}
		
		public Model getOutputModel()
		{
			return output.getModel();
		}
	}
}
