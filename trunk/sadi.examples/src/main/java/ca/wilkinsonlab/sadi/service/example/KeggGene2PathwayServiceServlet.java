package ca.wilkinsonlab.sadi.service.example;

import java.rmi.RemoteException;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Resource;

@SuppressWarnings("serial")
public class KeggGene2PathwayServiceServlet extends KeggServiceServlet
{
	private static final Log log = LogFactory.getLog(KeggGene2PathwayServiceServlet.class);
	private static final Pattern PATHWAY_ID_PREFIX = Pattern.compile("^path:", Pattern.CASE_INSENSITIVE);
	
	@Override
	protected void processInput(Resource input, Resource output)
	{
		String keggGeneId = ServiceUtils.getDatabaseId(input, LSRN.KEGG.Gene);
		
		if(keggGeneId == null) {
			log.error(String.format("unable to determine KEGG gene ID for %s", input));
			return;
		}
		
		String[] singleGeneArray = { keggGeneId };
		String[] keggPathwayIds;
		try {
			keggPathwayIds = getKeggService().get_pathways_by_genes(singleGeneArray);
		} catch(ServiceException e) {
			throw new RuntimeException("error initializing KEGG API service:", e);
		} catch(RemoteException e) {
			throw new RuntimeException("error invoking KEGG API service:", e);
		}
		
		for(String keggPathwayId : keggPathwayIds) {
			keggPathwayId = PATHWAY_ID_PREFIX.matcher(keggPathwayId).replaceFirst("");
			Resource keggPathwayNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.KEGG.Pathway, keggPathwayId);
			output.addProperty(SIO.is_participant_in, keggPathwayNode);
		}
	}
	
}
