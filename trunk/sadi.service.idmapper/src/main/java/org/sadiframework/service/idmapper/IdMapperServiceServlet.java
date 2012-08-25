package org.sadiframework.service.idmapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.ServiceDescription;
import org.sadiframework.beans.ServiceBean;
import org.sadiframework.service.AsynchronousServiceServlet;
import org.sadiframework.service.ServiceCall;
import org.sadiframework.service.annotations.URI;
import org.sadiframework.utils.LSRNUtils;
import org.sadiframework.utils.UniProtIdMapperClient;
import org.stringtemplate.v4.ST;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

@URI("http://sadiframework.org/sadi-idmapper/")
public class IdMapperServiceServlet extends AsynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(IdMapperServiceServlet.class);

	protected String inputLSRNNamespace;
	protected String outputLSRNNamespace;
	protected String relationshipURI;
	protected String serviceOWL;

	public IdMapperServiceServlet(String inputLSRNNamespace, String outputLSRNNamespace) throws IOException
	{
		this.inputLSRNNamespace = inputLSRNNamespace;
		this.outputLSRNNamespace = outputLSRNNamespace;

		Config config = Config.getInstance();
		this.relationshipURI = config.getRelationshipURI(inputLSRNNamespace, outputLSRNNamespace);
		if (this.relationshipURI == null) {
			throw new RuntimeException(String.format("no relationship URI configured in %s for %s => %s",
					Config.CONFIG_FILENAME, inputLSRNNamespace, outputLSRNNamespace));
		}

		String templateOWL;
		try {
			templateOWL = IOUtils.toString(IdMapperServiceServlet.class.getResourceAsStream("/service.owl.template"));
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		ST templater = new ST(templateOWL, '$', '$');
		templater.add("relationshipURI", relationshipURI);
		templater.add("outputLSRNTypeURI", LSRNUtils.getClassURI(outputLSRNNamespace));
		this.serviceOWL = templater.render();
	}

	public String getServiceName() {
		return String.format("%s-to-%s", inputLSRNNamespace, outputLSRNNamespace);
	}

	@Override
	protected Model createOutputModel()
	{
		Model model = super.createOutputModel();
		model.setNsPrefix("sio", "http://semanticscience.org/resource/");
		model.setNsPrefix("lsrn", "http://purl.oclc.org/SADI/LSRN/");
		model.setNsPrefix("sadi", "http://sadiframework.org/ontologies/properties.owl#");
		return model;
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (request.getServletPath().endsWith(".owl")) {
			try {
				response.setContentType("application/rdf+xml");
				response.getWriter().print(serviceOWL);
			} catch (IOException e) {
				log.error(String.format("error sending owl for %s", request.getServletPath()), e);
			}
		} else {
			super.doGet(request, response);
		}
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ServiceServlet#createServiceDescription()
	 */
	@Override
	protected ServiceDescription createServiceDescription()
	{
		ServiceBean service = new ServiceBean();
		service.setURI(getServiceURL());
		service.setName(getServiceName());
		service.setDescription(String.format("This service maps %s IDs to %s IDs, using UniProt's RESTful ID mapping service.", inputLSRNNamespace, outputLSRNNamespace));
		service.setContactEmail("info@sadiframework.org");
		service.setAuthoritative(false);
		service.setInputClassURI(LSRNUtils.getClassURI(inputLSRNNamespace));
		service.setOutputClassURI(String.format("%s%s.owl#OutputClass", getClass().getAnnotation(URI.class).value(), getServiceName()));
		return service;
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ServiceServlet#getServiceURL()
	 */
	@Override
	protected String getServiceURL()
	{
		String url = super.getServiceURL();
		return url == null ? null : url.concat(getServiceName());
	}


	@Override
	public int getInputBatchSize()
	{
		/*
		 * The UniProt people recommend keeping the input number of input IDs < 100,000
		 * because the ID mapper service is synchronous and may timeout otherwise.
		 * We err on the side of caution here.
		 */
		return 10000;
	}

	@Override
	protected void processInputBatch(ServiceCall call)
	{
		Config config = Config.getInstance();
		Model outputModel = call.getOutputModel();
		String inputUniprotNamespace = config.getUniprotNamespace(inputLSRNNamespace);
		String outputUniprotNamespace = config.getUniprotNamespace(outputLSRNNamespace);
		Property relationship = outputModel.getProperty(config.getRelationshipURI(inputLSRNNamespace, outputLSRNNamespace));

		// get input IDs, remember ID => output node mapping

		Collection<Resource> inputNodes = call.getInputNodes();
		Collection<String> inputIds = new ArrayList<String>();
		Map<String,Resource> idToOutputNode = new HashMap<String,Resource>();
		for (Resource inputNode : inputNodes) {
			if (inputNode.isAnon()) {
				log.info(String.format("skipping blank node input %s", inputNode.getId()));
				continue;
			}
			String id = LSRNUtils.getID(inputNode, LSRNUtils.getIdentifierClass(inputLSRNNamespace));
			if (id == null) {
				log.trace(String.format("skipping input %s, unable to determine %s ID", inputNode.getURI(), inputLSRNNamespace));
				continue;
			}
			inputIds.add(id);
			idToOutputNode.put(id, outputModel.getResource(inputNode.getURI()));
		}

		// call the UniProt ID mapping service

		Map<String,String> mappings = null;

		try {
			mappings = UniProtIdMapperClient.invoke(inputUniprotNamespace, outputUniprotNamespace, inputIds);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// add the mappings to the output model

		for (String fromId : mappings.keySet()) {
			String toId = mappings.get(fromId);
			if (!idToOutputNode.containsKey(fromId)) {
				log.warn(String.format("no input URI corresponding to %s:%s, omitting mapping %s:%s => %s:%s from results",
						inputLSRNNamespace, fromId, outputLSRNNamespace, toId, inputLSRNNamespace, fromId));
				continue;
			}
			Resource fromNode = idToOutputNode.get(fromId);
			Resource toNode = LSRNUtils.createInstance(outputModel, LSRNUtils.getClass(outputLSRNNamespace), toId);
			log.trace(String.format("adding mapping (%s, %s, %s)", fromNode.getURI(), relationship.getURI(), toNode.getURI()));
			outputModel.add(fromNode, relationship, toNode);
		}

	}

}
