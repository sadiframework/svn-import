package ca.wilkinsonlab.sadi.utils.blast;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.biojava3.core.util.XMLHelper;
import org.stringtree.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class BLASTUtils 
{
	private static Logger log = Logger.getLogger(BLASTUtils.class);
	
	public static void parseBLAST(InputStream inputStream, Model model) throws Exception
	{
		model.setNsPrefix("sio", "http://semanticscience.org/resource/");
		model.setNsPrefix("sadi", "http://sadiframework.org/ontologies/properties.owl#");
		model.setNsPrefix("blast", "http://sadiframework.org/ontologies/blast.owl#");
		if (log.isTraceEnabled()) {
			String content = StreamUtils.readStream(inputStream);
			log.trace(String.format("parsing BLAST report:\n%s", content));
			inputStream = new ByteArrayInputStream(content.getBytes());
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document blastDoc = db.parse(inputStream, "http://www.ncbi.nlm.nih.gov/dtd/");
        blastDoc.getDocumentElement().normalize();
//		Document blastDoc = XMLHelper.inputStreamToDocument(inputStream);
		
		// TODO create blastProcessNode...
		Resource blastProcessNode = model.createResource();
		
        for (Element iteration: XMLHelper.selectElements(blastDoc.getDocumentElement(), "BlastOutput_iterations/Iteration")) {
            Resource querySequenceNode = getQuerySequenceNode(model, iteration);
            Element iterationHits = XMLHelper.selectSingleElement(iteration, "Iteration_hits");
            for (Element hit: XMLHelper.selectElements(iterationHits, "Hit")) {
            	Resource blastHitNode = createBlastHit(querySequenceNode, hit);
    			blastHitNode.addProperty(SIO.is_output_of, blastProcessNode);
            }
        }
        if (log.isTraceEnabled())
        	log.trace(String.format("BLAST model after parsing:\n%s", RdfUtils.logModel(model)));
	}

	public static Resource createBlastHit(Resource querySequenceNode, Element hit) throws Exception
	{
		Model model = querySequenceNode.getModel();
		Resource hitSequenceNode = getHitSequenceNode(model, hit);
		Resource blastHitNode = model.createResource(null, Vocab.blast_hit);
        Element hitHSPs = XMLHelper.selectSingleElement(hit, "Hit_hsps");
        for (Element hsp: XMLHelper.selectElements(hitHSPs, "Hsp")) {
			Resource alignmentNode = createAlignmentNode(querySequenceNode, hitSequenceNode, hsp);
			blastHitNode.addProperty(SIO.has_part, alignmentNode);
        }
		return blastHitNode;
	}

	private static Resource createAlignmentNode(Resource querySequence, Resource hitSequence, Element hsp) throws Exception
	{
		
		Resource querySubsequence = createSubsequence(querySequence, 
				getQueryStart(hsp), 
				getQueryEnd(hsp), 
				getQuerySeq(hsp));
		
		Resource hitSubsequence = createSubsequence(hitSequence,
				getHitStart(hsp),
				getHitEnd(hsp),
				getHitSeq(hsp));
		
		Resource alignmentNode = querySequence.getModel().createResource(null, Vocab.sequence_alignment);
		Literal e = ResourceFactory.createTypedLiteral(getExpectation(hsp), XSDDatatype.XSDdecimal);
		SIOUtils.createAttribute(alignmentNode, Vocab.expectation, e);
		SIOUtils.createAttribute(alignmentNode, Vocab.identity, getIdentity(hsp));
		SIOUtils.createAttribute(alignmentNode, Vocab.bits, getBits(hsp));
		SIOUtils.createAttribute(alignmentNode, Vocab.score, getScore(hsp));
		SIOUtils.createAttribute(alignmentNode, Vocab.consensus_sequence, getConsensus(hsp));
		alignmentNode.addProperty(SIO.has_part, querySubsequence);
		alignmentNode.addProperty(SIO.has_part, hitSubsequence);
		return alignmentNode;
	}
	
	private static String getElementValueAsString(Element root, String selector) throws Exception
	{
		return XMLHelper.selectSingleElement(root, selector).getTextContent();
	}
	
	private static Integer getElementValueAsInteger(Element root, String selector) throws Exception
	{
		return Integer.parseInt(getElementValueAsString(root, selector));
	}
	
	private static Double getElementValueAsDouble(Element root, String selector) throws Exception
	{
		return Double.parseDouble(getElementValueAsString(root, selector));
	}
	
	public static int getQueryStart(Element hsp) throws Exception
	{
		return getElementValueAsInteger(hsp, "Hsp_query-from");
	}
	
	public static int getQueryEnd(Element hsp) throws Exception
	{
		return getElementValueAsInteger(hsp, "Hsp_query-to");
	}
	
	public static String getQuerySeq(Element hsp) throws Exception
	{
		return getElementValueAsString(hsp, "Hsp_qseq");
	}
	
	public static int getHitStart(Element hsp) throws Exception
	{
		return getElementValueAsInteger(hsp, "Hsp_hit-from");
	}
	
	public static int getHitEnd(Element hsp) throws Exception
	{
		return getElementValueAsInteger(hsp, "Hsp_hit-to");
	}
	
	public static String getHitSeq(Element hsp) throws Exception
	{
		return getElementValueAsString(hsp, "Hsp_hseq");
	}
	
	/* e value has to be decimal because doubles aren't precise enough;
	 * the fact that the API returns a double is a bug...
	 */
	public static String getExpectation(Element hsp) throws Exception
	{
		return getElementValueAsString(hsp, "Hsp_evalue");
	}
	
	public static double getIdentity(Element hsp) throws Exception
	{
		return getElementValueAsDouble(hsp, "Hsp_identity")/getElementValueAsDouble(hsp, "Hsp_align-len");
	}
	
	public static double getBits(Element hsp) throws Exception
	{
		return getElementValueAsDouble(hsp, "Hsp_bit-score");
	}
	
	public static int getScore(Element hsp) throws Exception
	{
		return getElementValueAsInteger(hsp, "Hsp_score");
	}
	
	public static String getConsensus(Element hsp) throws Exception
	{
		return getElementValueAsString(hsp, "Hsp_midline");
	}

	public static Resource createSubsequence(Resource sequenceNode, int start, int stop, String subsequence)
	{
		Resource subsequenceNode = sequenceNode.getModel().createResource(null, getSequenceType(sequenceNode));
		sequenceNode.addProperty(SIO.has_part, subsequenceNode);
		subsequenceNode.addProperty(SIO.is_part_of, sequenceNode);
		SIOUtils.createAttribute(subsequenceNode, SIO.sequence_start_position, start);
		SIOUtils.createAttribute(subsequenceNode, SIO.sequence_stop_position, stop);
		subsequenceNode.addProperty(SIO.has_value, subsequence);
		return subsequenceNode;
	}

	public static Resource getSequenceType(Resource sequenceNode)
	{
		if (sequenceNode.hasProperty(RDF.type, SIO.nucleic_acid_sequence))
			return SIO.nucleic_acid_sequence;
		else if (sequenceNode.hasProperty(RDF.type, SIO.protein_sequence))
			return SIO.protein_sequence;
		else
			return SIO.biopolymer_sequence;
	}

	public static Resource getQuerySequenceNode(Model model, Element iteration) throws Exception
	{
		Element query_def = XMLHelper.selectSingleElement(iteration, "Iteration_query-def");
		if (query_def == null) {
			query_def = XMLHelper.selectSingleElement(
					(Element)iteration.getParentNode().getParentNode(), "BlastOutput_query-def");
		}
		// TODO this is a bit specific to our use-case...
		// see FIXME in NCBIBLASTClient for the alternative...
		String uri = URLDecoder.decode(query_def.getTextContent(), "UTF-8");
		return model.getResource(uri);
	}
	
	public static Resource getHitSequenceNode(Model model, Element hit) throws Exception
	{
        Element accession = XMLHelper.selectSingleElement(hit, "Hit_accession");
        String acc = accession.getTextContent();
        Resource hitSequence = model.getResource(getHitURI(acc));
        if (!hitSequence.hasProperty(RDF.type)) {
        	hitSequence.addProperty(RDF.type, SIO.nucleic_acid_sequence);
        	hitSequence.addProperty(Vocab.from_organism, getTaxonResource(model));
        }
        return hitSequence;
	}
	
	private static Resource getTaxonResource(Model model)
	{
		return model.getResource("http://lsrn.org/taxon:4151");
	}

	public static String getHitURI(String acc)
	{
		return String.format("http://lsrn.org/DragonDB_DNA:%s", acc);
	}
	
	public static class Vocab
	{
		public static final Resource blast_hit = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#BlastHit");
		public static final Resource sequence_alignment = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#SequenceAlignment");
		public static final Resource expectation = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#expectation");
		public static final Resource identity = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#identity");
		public static final Resource bits = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#bits");
		public static final Resource score = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#score");
//		public static final Resource subsequence = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#SubSequence");
		public static final Resource consensus_sequence = ResourceFactory.createResource("http://sadiframework.org/ontologies/blast.owl#Consensus");
		public static final Property from_organism = ResourceFactory.createProperty("http://sadiframework.org/ontologies/properties.owl#fromOrganism");
	}
}