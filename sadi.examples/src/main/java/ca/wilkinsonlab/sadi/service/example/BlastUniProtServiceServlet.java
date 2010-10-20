package ca.wilkinsonlab.sadi.service.example;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.blast.LocalAlignment;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.model.blast.JobStatus;
import uk.ac.ebi.kraken.model.blast.parameters.DatabaseOptions;
import uk.ac.ebi.kraken.model.blast.parameters.ExpectedThreshold;
import uk.ac.ebi.kraken.model.blast.parameters.MaxNumberResultsOptions;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;
import uk.ac.ebi.kraken.uuw.services.remoting.blast.BlastData;
import uk.ac.ebi.kraken.uuw.services.remoting.blast.BlastHit;
import uk.ac.ebi.kraken.uuw.services.remoting.blast.BlastInput;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;

import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.Properties;
import ca.wilkinsonlab.sadi.vocab.SIO;

/**
 * BLAST an amino acid sequence against the UniProt database,
 * including proteins from all organisms in the search.
 * 
 * TODO: This service was implemented in a hurry. A lot 
 * of the interesting data from the BLAST hits (e.g. the string 
 * representation of the alignment) is not included in the 
 * output RDF. 
 *
 * TODO: Currently, this service submits one BLAST query at a time, and 
 * waits for the results before submitting the next BLAST query. 
 * However, the UniProt BLAST API is asynchronous, and so we could 
 * potentially speed things up by issuing multiple requests simultaneously
 * (so long as we don't push our luck).
 * 
 * TODO: Currently there is no way for the caller to specify 
 * parameters for the BLAST invocation(s).
 * 
 * @author Ben Vandervalk
 */
@SuppressWarnings("serial")
public class BlastUniProtServiceServlet extends AsynchronousServiceServlet 
{
	private static final Log log = LogFactory.getLog(BlastUniProtServiceServlet.class);
	private static final int BLAST_POLLING_INTERVAL = 5000;
	
	private static class Vocab
	{
		// prefixes
		public static final String OLD_UNIPROT_PREFIX = "http://biordf.net/moby/UniProt/";
		public static final String UNIPROT_PREFIX = "http://lsrn.org/UniProt:";
		public static final String LSRN_PREFIX = "http://purl.oclc.org/SADI/LSRN/";
		public static final String ONT_PREFIX = "http://sadiframework.org/examples/blastUniprot.owl#"; 
		
		// predicates
		public static final Property BLAST_HIT = ResourceFactory.createProperty(ONT_PREFIX + "blast-hit");
		public static final Property MATCH_PROTEIN = ResourceFactory.createProperty(ONT_PREFIX + "match-protein");
		public static final Property EXPECTATION_VALUE = ResourceFactory.createProperty(ONT_PREFIX + "expectation-value");
		
		// classes
		public static final Resource PROTEIN_BLAST_HIT = ResourceFactory.createResource(ONT_PREFIX + "ProteinBlastHit");
		public static final Resource EXPECTATION_VALUE_TYPE = ResourceFactory.createResource(ONT_PREFIX + "ExpectationValue");
		public static final Resource UniProt_Type = ResourceFactory.createResource(LSRN_PREFIX + "UniProt_Record");
		public static final Resource UniProt_Identifier = ResourceFactory.createResource(LSRN_PREFIX + "UniProt_Identifier");
	}
	
	@Override
	protected InputProcessingTask getInputProcessingTask(Model inputModel, Collection<Resource> inputNodes) 
	{
		return new BlastInputProcessingTask(inputModel, inputNodes);
	}

	private static void processInput(Resource input, Resource output)
	{
		log.info(String.format("processing input %s", input.getURI()));
		
		// if the input protein has multiple sequences attached to it 
		// (e.g. isoforms), blast them all
		for(String sequence : getSequences(input)) {
			BlastData<UniProtEntry> blastResults = runBlast(sequence, output);
			attachBlastResults(output, blastResults);
		}
	}
	
	private static Collection<String> getSequences(Resource input) 
	{
		Collection<String> sequences = new HashSet<String>();

		for(Statement statement : input.listProperties(Properties.hasSequence).toList()) {
			if(!statement.getObject().isResource()) {
				log.warn(String.format("value of %s is not a resource, ignoring triple %s", Properties.hasSequence, statement));
				continue;
			}
			Resource sequenceNode = statement.getObject().asResource();
			if(sequenceNode.getProperty(SIO.has_value) == null) {
				log.warn(String.format("sequence node %s does not have an attached sequence string", sequenceNode));
				continue;
			}
			sequences.add(sequenceNode.getProperty(SIO.has_value).getObject().asLiteral().getLexicalForm());
		}
		
		return sequences;
	}
	
	private static BlastData<UniProtEntry> runBlast(String sequence, Resource output)
	{
		UniProtQueryService service = UniProtJAPI.factory.getUniProtQueryService();
		BlastInput input = new BlastInput(DatabaseOptions.UNIPROTKB, sequence, MaxNumberResultsOptions.FIVE_HUNDRED);
		
		log.info("submitting job to UniProt BLAST service...");
		String jobid = service.submitBlast(input);

		while (service.checkStatus(jobid) != JobStatus.DONE) {
			log.info("polling UniProt BLAST service..");
			try {
				Thread.sleep(BLAST_POLLING_INTERVAL);
			} catch (InterruptedException e) {
				log.warn(String.format("thread %d: ignoring InterruptedException", Thread.currentThread().getId()));
			}
		}
		return service.getResults(jobid);
	}
	
	private static void attachBlastResults(Resource output, BlastData<UniProtEntry> blastResults)
	{
		Model model = output.getModel();
		for(BlastHit<UniProtEntry> blastHit : blastResults.getBlastHits()) {
			
			Resource blastHitNode = model.createResource(Vocab.PROTEIN_BLAST_HIT);
			
			// add UniProt record for hit 
			Resource uniprotNode = createUniProtNode(model, blastHit.getHit().getAc());
			blastHitNode.addProperty(Vocab.MATCH_PROTEIN, uniprotNode);

			// add expectation value for hit
			SIOUtils.createAttribute(blastHitNode, Vocab.EXPECTATION_VALUE, Vocab.EXPECTATION_VALUE_TYPE, getExpectationValue(blastHit));

			// link input node to blast hit
			output.addProperty(Vocab.BLAST_HIT, blastHitNode);
			
			// link input node to UniProt record for hit
			output.addProperty(SIO.is_homologous_to, uniprotNode);
			
		}
	}

	private static Resource createUniProtNode(Model model, String uniprotId)
	{
		Resource uniprotNode = model.createResource(getUniProtUri(uniprotId), Vocab.UniProt_Type);
		
		// add identifier structure
		SIOUtils.createAttribute(uniprotNode, Vocab.UniProt_Identifier, uniprotId);
		
		// add relationship to old URI scheme
		Resource oldUniprotNode = model.createResource(getOldUniProtUri(uniprotId));
		uniprotNode.addProperty(OWL.sameAs, oldUniprotNode);

		return uniprotNode;
	}
	
	private static double getExpectationValue(BlastHit<UniProtEntry> blastHit)
	{
		// TODO: I don't understand how there can be multiple
		// alignments for a single BLAST hit. For now, return
		// the best (lowest) expection value over all of the 
		// alignments. -- Ben
		
		boolean firstValue = true;
		double lowest = 0;
		for(LocalAlignment alignment : blastHit.getHit().getAlignments()) {
			if(firstValue || alignment.getExpectation() < lowest) {
				lowest = alignment.getExpectation();
				firstValue = false;
			}
		}
		return lowest;
	}
	
	private static String getUniProtUri(String uniprotId)
	{
		return String.format("%s%s", Vocab.UNIPROT_PREFIX, uniprotId);
	}
	
	private static String getOldUniProtUri(String uniprotId)
	{
		return String.format("%s%s", Vocab.OLD_UNIPROT_PREFIX, uniprotId);
	}
	
	private class BlastInputProcessingTask extends InputProcessingTask
	{	
		public BlastInputProcessingTask(Model inputModel, Collection<Resource> inputNodes)
		{
			super(inputModel, inputNodes);
		}

		public void run()
		{
			for(Resource inputNode : inputNodes) {
				Resource outputNode = outputModel.getResource(inputNode.getURI());
				processInput(inputNode, outputNode);
			}
			success();
		}
	}
	
}
