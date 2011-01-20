package ca.wilkinsonlab.sadi.service.example;

import java.rmi.RemoteException;

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
public class KeggPathway2CompoundServiceServlet extends SimpleAsynchronousServiceServlet
{
	private static final Log log = LogFactory.getLog(KeggPathway2CompoundServiceServlet.class);
	
	@Override
	protected void processInput(Resource input, Resource output)
	{
		String keggPathwayId = ServiceUtils.getDatabaseId(input, KEGG.PATHWAY_IDENTIFIER, KeggUtils.PATHWAY_URI_PATTERNS);
		
		if(keggPathwayId == null) {
			log.error(String.format("unable to determine KEGG pathway ID for %s", input));
			return;
		}
		
		String[] keggCompoundIds;
		try {
			// "new KEGGLocator().getKEGGPort()" doesn't do any network stuff,
			// so it is okay to repeat it for each input.
			keggCompoundIds = new KEGGLocator().getKEGGPort().get_compounds_by_pathway(String.format("path:%s", keggPathwayId));
		} catch(ServiceException e) {
			throw new RuntimeException("error initializing KEGG API service:", e);
		} catch(RemoteException e) {
			throw new RuntimeException("error invoking KEGG API service:", e);
		}
		
		for(String keggCompoundId : keggCompoundIds) {
			Resource keggCompoundNode = createKeggCompoundNode(output.getModel(), keggCompoundId);
			output.addProperty(SIO.has_participant, keggCompoundNode);
		}
	}
	
	protected Resource createKeggCompoundNode(Model model, String keggCompoundId) 
	{
		String oldURI = String.format("%s%s", KEGG.OLD_COMPOUND_PREFIX, keggCompoundId);
		String URI = String.format("%s%s", KEGG.COMPOUND_PREFIX, keggCompoundId);
		
		Resource keggGeneNode = model.createResource(URI, KEGG.COMPOUND_TYPE);
		// add SIO identifier structure 
		SIOUtils.createAttribute(keggGeneNode, KEGG.COMPOUND_IDENTIFIER, keggCompoundId);
		// add link to old URI scheme
		keggGeneNode.addProperty(OWL.sameAs, model.createResource(oldURI));
		
		return keggGeneNode;
	}
}
