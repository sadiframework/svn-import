package ca.wilkinsonlab.sadi.service.example;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

import ca.wilkinsonlab.sadi.utils.KeggUtils;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.KEGG;
import ca.wilkinsonlab.sadi.vocab.SIO;

@SuppressWarnings("serial")
public class KeggPathway2GeneServiceServlet extends KeggServiceServlet
{
	private static final Log log = LogFactory.getLog(KeggPathway2GeneServiceServlet.class);
	
	@Override
	protected void processInput(Resource input, Resource output)
	{
		String keggPathwayId = ServiceUtils.getDatabaseId(input, KEGG.PATHWAY_IDENTIFIER, KeggUtils.PATHWAY_URI_PATTERNS);
		
		if(keggPathwayId == null) {
			log.error(String.format("unable to determine KEGG pathway ID for %s", input));
			return;
		}
		
		String[] keggGeneIds;
		try {
			keggGeneIds = getKeggService().get_genes_by_pathway(String.format("path:%s", keggPathwayId));
		} catch(ServiceException e) {
			throw new RuntimeException("error initializing KEGG API service:", e);
		} catch(RemoteException e) {
			throw new RuntimeException("error invoking KEGG API service:", e);
		}
		
		for(String keggGeneId : keggGeneIds) {
			Resource keggGeneNode = createKeggGeneNode(output.getModel(), keggGeneId);
			output.addProperty(SIO.has_participant, keggGeneNode);
		}
	}
	
	protected Resource createKeggGeneNode(Model model, String keggGeneId) 
	{
		String oldURI = String.format("%s%s", KEGG.OLD_GENE_PREFIX, keggGeneId);
		String URI = String.format("%s%s", KEGG.GENE_PREFIX, keggGeneId);
		
		Resource keggGeneNode = model.createResource(URI, KEGG.GENE_TYPE);
		// add SIO identifier structure 
		SIOUtils.createAttribute(keggGeneNode, KEGG.GENE_IDENTIFIER, keggGeneId);
		// add link to old URI scheme
		keggGeneNode.addProperty(OWL.sameAs, model.createResource(oldURI));
		
		return keggGeneNode;
	}
}
