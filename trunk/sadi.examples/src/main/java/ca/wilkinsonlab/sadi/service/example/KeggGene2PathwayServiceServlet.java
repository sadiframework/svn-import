package ca.wilkinsonlab.sadi.service.example;

import java.rmi.RemoteException;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import keggapi.KEGGLocator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

import ca.wilkinsonlab.sadi.service.simple.SimpleAsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.utils.KeggUtils;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.KEGG;
import ca.wilkinsonlab.sadi.vocab.SIO;

@SuppressWarnings("serial")
public class KeggGene2PathwayServiceServlet extends SimpleAsynchronousServiceServlet
{
	private static final Log log = LogFactory.getLog(KeggGene2PathwayServiceServlet.class);
	private static final Pattern PATHWAY_ID_PREFIX = Pattern.compile("^path:", Pattern.CASE_INSENSITIVE);
	
	@Override
	protected void processInput(Resource input, Resource output)
	{
		String keggGeneId = ServiceUtils.getDatabaseId(input, KEGG.GENE_IDENTIFIER, KeggUtils.GENE_URI_PATTERNS);
		
		if(keggGeneId == null) {
			log.error(String.format("unable to determine KEGG gene ID for %s", input));
			return;
		}
		
		String[] singleGeneArray = { keggGeneId };
		String[] keggPathwayIds;
		try {
			// "new KEGGLocator().getKEGGPort()" doesn't do any network stuff,
			// so it is okay to repeat it for each input.
			keggPathwayIds = new KEGGLocator().getKEGGPort().get_pathways_by_genes(singleGeneArray);
		} catch(ServiceException e) {
			throw new RuntimeException("error initializing KEGG API service:", e);
		} catch(RemoteException e) {
			throw new RuntimeException("error invoking KEGG API service:", e);
		}
		
		for(String keggPathwayId : keggPathwayIds) {
			keggPathwayId = PATHWAY_ID_PREFIX.matcher(keggPathwayId).replaceFirst("");
			Resource keggPathwayNode = createKeggPathwayNode(output.getModel(), keggPathwayId);
			output.addProperty(SIO.is_participant_in, keggPathwayNode);
		}
	}
	
	protected Resource createKeggPathwayNode(Model model, String keggPathwayId) 
	{
		String oldURI = String.format("%s%s", KEGG.OLD_PATHWAY_PREFIX, keggPathwayId);
		String URI = String.format("%s%s", KEGG.PATHWAY_PREFIX, keggPathwayId);
		
		Resource keggPathwayNode = model.createResource(URI, KEGG.PATHWAY_TYPE);
		// add SIO identifier structure 
		SIOUtils.createAttribute(keggPathwayNode, KEGG.PATHWAY_IDENTIFIER, keggPathwayId);
		// add link to old URI scheme
		keggPathwayNode.addProperty(OWL.sameAs, model.createResource(oldURI));
		
		return keggPathwayNode;
	}

}
