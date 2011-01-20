package ca.wilkinsonlab.sadi.service.example;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import keggapi.LinkDBRelation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

import ca.wilkinsonlab.sadi.utils.KeggUtils;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.vocab.KEGG;
import ca.wilkinsonlab.sadi.vocab.Properties;
import ca.wilkinsonlab.sadi.vocab.PubChem;

@SuppressWarnings("serial")
public class KeggCompound2PubChemServiceServlet extends KeggServiceServlet
{
	private static final Log log = LogFactory.getLog(KeggCompound2PubChemServiceServlet.class);
	private static final Pattern PUBCHEM_ID_PREFIX = Pattern.compile("^pubchem:", Pattern.CASE_INSENSITIVE);
	protected static final int RESULTS_LIMIT_PER_QUERY = 200;
	
	@Override
	protected void processInput(Resource input, Resource output)
	{
		String keggCompoundId = ServiceUtils.getDatabaseId(input, KEGG.COMPOUND_IDENTIFIER, KeggUtils.COMPOUND_URI_PATTERNS);
		
		if(keggCompoundId == null) {
			log.error(String.format("unable to determine KEGG compound ID for %s", input));
			return;
		}
		
		List<LinkDBRelation> results = new ArrayList<LinkDBRelation>();
		try {
			
			LinkDBRelation[] resultsChunk;
			int startEntry = 1;
			do {
				resultsChunk = getKeggService().get_linkdb_by_entry(keggCompoundId, "pubchem", startEntry, RESULTS_LIMIT_PER_QUERY);
				results.addAll(Arrays.asList(resultsChunk));
				startEntry += resultsChunk.length;
			} 
			while (resultsChunk.length >= RESULTS_LIMIT_PER_QUERY);
			
		} catch(ServiceException e) {
			throw new RuntimeException("error initializing KEGG API service:", e);
		} catch(RemoteException e) {
			throw new RuntimeException("error invoking KEGG API service:", e);
		}
		
		for(LinkDBRelation crossref : results) {
			String pubChemId = PUBCHEM_ID_PREFIX.matcher(crossref.getEntry_id2()).replaceFirst("");
			Resource keggPubChemNode = createPubChemNode(output.getModel(), pubChemId);
			output.addProperty(Properties.isSubstance, keggPubChemNode);
		}
	}
	
	protected Resource createPubChemNode(Model model, String pubchemId) 
	{
		String oldURI = String.format("%s%s", PubChem.OLD_SUBSTANCE_PREFIX, pubchemId);
		String URI = String.format("%s%s", PubChem.OLD_SUBSTANCE_PREFIX, pubchemId);
		
		Resource pubChemNode = model.createResource(URI, PubChem.SUBSTANCE_TYPE);
		// add SIO identifier structure 
		SIOUtils.createAttribute(pubChemNode, PubChem.SUBSTANCE_IDENTIFIER, pubchemId);
		// add link to old URI scheme
		pubChemNode.addProperty(OWL.sameAs, model.createResource(oldURI));
		
		return pubChemNode;
	}
}
