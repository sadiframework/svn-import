package org.sadiframework.service.example;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.service.AsynchronousServiceServlet;
import org.sadiframework.service.ServiceCall;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.utils.KeggUtils;
import org.sadiframework.utils.LSRNUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/entrezGene2Kegg.input.1.rdf",
				output = "http://sadiframework.org/examples/t/entrezGene2Kegg.output.1.rdf"
		)
)
public class EntrezGene2KeggServiceServlet extends AsynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(EntrezGene2KeggServiceServlet.class);

	@Override
	public int getInputBatchSize()
	{
		// override input batch size to 100, the maximum for the KEGG API...
		return 100;
	}

	@Override
	protected void processInputBatch(ServiceCall call) throws IOException
	{
		Collection<Resource> inputNodes = call.getInputNodes();
		Model outputModel = call.getOutputModel();
		// build map: Entrez Gene IDs => output RDF nodes

		Map<String, Resource> idToOutputNode = new HashMap<String, Resource>(inputNodes.size());

		for (Resource inputNode: inputNodes) {
			String id = LSRNUtils.getID(inputNode, LSRNUtils.getIdentifierClass("GeneID"));
			id = String.format("ncbi-geneid:%s", id);
			idToOutputNode.put(id, outputModel.getResource(inputNode.getURI()));
		}

		// retrieve the id mappings
		Map<String, String> idMap = KeggUtils.getKeggIdMap("genes", idToOutputNode.keySet());
		for (Map.Entry<String, String> entry: idMap.entrySet()) {
			String entrezGeneId = entry.getKey();
			String keggGeneId = entry.getValue();

			Resource keggGeneNode = LSRNUtils.createInstance(outputModel, LSRNUtils.getClass("KEGG"), keggGeneId);
			idToOutputNode.get(entrezGeneId).addProperty(OWL.sameAs, keggGeneNode);

		}
	}
}
