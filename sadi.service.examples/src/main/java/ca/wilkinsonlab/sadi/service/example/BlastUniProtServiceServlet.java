package ca.wilkinsonlab.sadi.service.example;

import org.apache.log4j.Logger;

import uk.ac.ebi.kraken.interfaces.blast.Hit;
import uk.ac.ebi.kraken.interfaces.blast.LocalAlignment;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.model.blast.JobInformation;
import uk.ac.ebi.kraken.model.blast.JobStatus;
import uk.ac.ebi.kraken.model.blast.parameters.DatabaseOptions;
import uk.ac.ebi.kraken.model.blast.parameters.MaxNumberResultsOptions;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;
import uk.ac.ebi.kraken.uuw.services.remoting.blast.BlastData;
import uk.ac.ebi.kraken.uuw.services.remoting.blast.BlastHit;
import uk.ac.ebi.kraken.uuw.services.remoting.blast.BlastInput;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.service.simple.SimpleAsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.utils.LSRNUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.Properties;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * BLAST an amino acid sequence against the UniProt database,
 * including proteins from all organisms in the search.
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
@URI("http://sadiframework.org/examples/blastUniprot")
@Name("UniProt BLAST")
@Description("Issues a BLAST query against the UniProt database using BLASTP, similarity matrix BLOSUM_62, and an expect threshold of 10. A maximum 500 BLAST hits are returned, if the expectation cutoff is not reached. All organisms are included in the search.")
@ContactEmail("info@sadiframework.org")
@InputClass("http://semanticscience.org/resource/SIO_010015") // protein sequence
@OutputClass("http://sadiframework.org/examples/blast-uniprot.owl#UniProtBLASTedSequence")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/blastUniprot-input.rdf", 
				output = "http://sadiframework.org/examples/t/blastUniprot.output.1.rdf"
		)
)
public class BlastUniProtServiceServlet extends SimpleAsynchronousServiceServlet 
{
	private static final Logger log = Logger.getLogger(BlastUniProtServiceServlet.class);
	private static final long serialVersionUID = 1L;
	private static final int BLAST_POLLING_INTERVAL = 5000;

	@Override
	protected Model createOutputModel()
	{
		Model model = super.createOutputModel();
		model.setNsPrefix("ublast", "http://sadiframework.org/examples/blast-uniprot.owl#");
		model.setNsPrefix("blast", "http://sadiframework.org/ontologies/blast.owl#");
		model.setNsPrefix("sio", "http://semanticscience.org/resource/");
		return model;
	}

	@Override
	public void processInput(Resource input, Resource output)
	{
		String sequence = input.getRequiredProperty(SIO.has_value).getString();
		log.debug(String.format("processing input %s with sequence\n%s", input, sequence));
		BlastData<UniProtEntry> blastResults = runBlast(sequence, output);
		Resource blastProcessNode = createBlastProcessNode(output.getModel(), blastResults);
		for (BlastHit<UniProtEntry> blastHit : blastResults.getBlastHits()) {
			Resource blastHitNode = createBlastHit(output, blastHit.getHit());
			blastHitNode.addProperty(SIO.is_output_of, blastProcessNode);
		}
	}

	protected static BlastData<UniProtEntry> runBlast(String sequence, Resource output)
	{
		UniProtQueryService service = UniProtJAPI.factory.getUniProtQueryService();
		BlastInput input = new BlastInput(DatabaseOptions.UNIPROTKB, sequence, MaxNumberResultsOptions.FIVE_HUNDRED);
		
		log.debug("submitting job to UniProt BLAST service");
		String jobid = service.submitBlast(input);
		log.debug(String.format("submitted job id %s", jobid));

		while (service.checkStatus(jobid) != JobStatus.FINISHED) {
			log.debug(String.format("polling job id %s", jobid));
			try {
				Thread.sleep(BLAST_POLLING_INTERVAL);
			} catch (InterruptedException e) {
				log.warn(String.format("thread %d: ignoring InterruptedException", Thread.currentThread().getId()));
			}
		}
		log.debug(String.format("finished job id %s", jobid));
		return service.getResults(jobid);
	}
	
	private Resource createBlastProcessNode(Model model, BlastData<?> blastResults)
	{
		Resource blastProcessNode = model.createResource(null, SIO.software_execution);	
		Resource blastProgramNode = model.createResource(null, SIO.software_application);
		blastProcessNode.addProperty(SIO.has_agent, blastProgramNode);
		JobInformation info = blastResults.getJobInformation();
		blastProgramNode.addLiteral(RDFS.label, info.getProgram());
		SIOUtils.createAttribute(blastProgramNode, SIO.version_identifier, info.getVersion());
		String citation = info.getCitation();
		if (citation != null) {
			Resource citationNode = model.createResource();
			citationNode.addLiteral(RDFS.label, citation);
			blastProgramNode.addProperty(SIO.has_reference, citationNode);
		}
		String database = info.getDatabase();
		if (database != null) {
			Resource databaseNode = model.createResource();
			databaseNode.addLiteral(RDFS.label, database);
			blastProcessNode.addProperty(SIO.has_input, databaseNode);
		}
		Literal startTime = model.createTypedLiteral(info.getStartTime(), XSDDatatype.XSDdateTime);
		SIOUtils.createAttribute(blastProcessNode, SIO.start_time, startTime);
		Literal stopTime = model.createTypedLiteral(info.getStartTime(), XSDDatatype.XSDdateTime);
		SIOUtils.createAttribute(blastProcessNode, SIO.end_time, stopTime);
		return blastProcessNode;
	}
	
	private static Resource createBlastHit(Resource querySequenceNode, Hit hit)
	{
		Model model = querySequenceNode.getModel();
		Resource uniprotSequenceNode = getUniProtSequenceNode(model, hit);
		Resource blastHitNode = model.createResource(null, Vocab.blast_hit);
		for (LocalAlignment alignment: hit.getAlignments()) {
			Resource alignmentNode = createAlignmentNode(querySequenceNode, uniprotSequenceNode, alignment);
			blastHitNode.addProperty(SIO.has_part, alignmentNode);
		}
		return blastHitNode;
	}
	
	private static Resource getUniProtSequenceNode(Model model, Hit hit)
	{
		String uri = getUniProtUri(hit.getAc());
		Resource uniprotNode = model.getResource(uri);
		if (!uniprotNode.hasProperty(RDF.type)) {
			// first time here, so create the id structure...
			uniprotNode = LSRNUtils.createInstance(model, LSRN.UniProt.getRecordTypeURI(), hit.getAc());
			for (String taxonID: hit.getOrganisms()) {
				Resource taxon = model.getResource(LSRNUtils.getURI("taxon", taxonID));
				if (!taxon.hasProperty(RDF.type)) {
					// first time here, so create the id structure...
					taxon = LSRNUtils.createInstance(model.getResource(LSRNUtils.getClassURI("taxon")), taxonID);
					uniprotNode.addProperty(Properties.fromOrganism, taxon);
				}
			}
		}
		Resource uniprotSequenceNode = RdfUtils.getPropertyValue(uniprotNode, SIO.has_attribute, SIO.protein_sequence);
		if (uniprotSequenceNode == null) {
			uniprotSequenceNode = model.createResource(null, SIO.protein_sequence);
			uniprotNode.addProperty(SIO.has_attribute, uniprotSequenceNode);
			uniprotSequenceNode.addProperty(SIO.is_attribute_of, uniprotNode);
		}
		return uniprotSequenceNode;
	}

	private static Resource createAlignmentNode(Resource querySequence, Resource matchSequence, LocalAlignment alignment)
	{
		Resource querySubsequence = createSubsequence(querySequence, 
				alignment.getStartQuerySeq(), 
				alignment.getEndQuerySeq(), 
				alignment.getQuerySeq());
		
		Resource matchSubsequence = createSubsequence(matchSequence,
				alignment.getStartMatchSeq(),
				alignment.getEndMatchSeq(),
				alignment.getMatchSeq());
		
		Resource alignmentNode = querySequence.getModel().createResource(null, Vocab.sequence_alignment);
		/* e value has to be decimal because doubles aren't precise enough;
		 * the fact that the API returns a double is a bug...
		 */
		Literal e = ResourceFactory.createTypedLiteral(alignment.getExpectation().toString(), XSDDatatype.XSDdecimal);
		SIOUtils.createAttribute(alignmentNode, Vocab.expectation, e);
		SIOUtils.createAttribute(alignmentNode, Vocab.identity, alignment.getIdentity());
		SIOUtils.createAttribute(alignmentNode, Vocab.bits, alignment.getBits());
		SIOUtils.createAttribute(alignmentNode, Vocab.score, alignment.getScore());
		SIOUtils.createAttribute(alignmentNode, Vocab.consensus_sequence, alignment.getPattern());
		alignmentNode.addProperty(SIO.has_part, querySubsequence);
		alignmentNode.addProperty(SIO.has_part, matchSubsequence);
		return alignmentNode;
	}
	
	private static Resource createSubsequence(Resource sequenceNode, int start, int stop, String subsequence)
	{
		Resource subsequenceNode = sequenceNode.getModel().createResource(null, SIO.protein_sequence);
		sequenceNode.addProperty(SIO.has_part, subsequenceNode);
		subsequenceNode.addProperty(SIO.is_part_of, sequenceNode);
		SIOUtils.createAttribute(subsequenceNode, SIO.sequence_start_position, start);
		SIOUtils.createAttribute(subsequenceNode, SIO.sequence_stop_position, stop);
		subsequenceNode.addProperty(SIO.has_value, subsequence);
		return subsequenceNode;
	}
	
	private static String getUniProtUri(String uniprotId)
	{
		return String.format("%s%s", Vocab.UNIPROT_PREFIX, uniprotId);
	}
	
	private static class Vocab
	{
		public static final String UNIPROT_PREFIX = "http://lsrn.org/UniProt:";
		
		public static final Resource blast_hit = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#BlastHit");
		public static final Resource sequence_alignment = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#SequenceAlignment");
		public static final Resource expectation = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#expectation");
		public static final Resource identity = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#identity");
		public static final Resource bits = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#bits");
		public static final Resource score = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#score");
//		public static final Resource subsequence = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#SubSequence");
		public static final Resource consensus_sequence = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#Consensus");
		
//		// these will be replaced by SIO types soon...
//		public static final Resource start_position = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#startPosition");
//		public static final Resource stop_position = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#stopPosition");
	}
}
