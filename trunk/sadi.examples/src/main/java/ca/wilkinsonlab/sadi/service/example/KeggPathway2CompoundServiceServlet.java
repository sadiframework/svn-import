package ca.wilkinsonlab.sadi.service.example;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Resource;

@SuppressWarnings("serial")
public class KeggPathway2CompoundServiceServlet extends KeggServiceServlet
{
	private static final Log log = LogFactory.getLog(KeggPathway2CompoundServiceServlet.class);
	
	@Override
	protected void processInput(Resource input, Resource output)
	{
		String keggPathwayId = ServiceUtils.getDatabaseId(input, LSRN.KEGG.Pathway);
		
		if(keggPathwayId == null) {
			log.error(String.format("unable to determine KEGG pathway ID for %s", input));
			return;
		}
		
		String[] keggCompoundIds;
		try {
			keggCompoundIds = getKeggService().get_compounds_by_pathway(String.format("path:%s", keggPathwayId));
		} catch(ServiceException e) {
			throw new RuntimeException("error initializing KEGG API service:", e);
		} catch(RemoteException e) {
			throw new RuntimeException("error invoking KEGG API service:", e);
		}
		
		for(String keggCompoundId : keggCompoundIds) {
			Resource keggCompoundNode = ServiceUtils.createLSRNRecordNode(output.getModel(), LSRN.KEGG.Compound, keggCompoundId);
			output.addProperty(SIO.has_participant, keggCompoundNode);
		}
	}
}
