package ca.wilkinsonlab.sadi.service.blast;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.log4j.Logger;
import org.stringtree.util.StreamUtils;
import org.w3c.dom.Node;

import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceCall;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.utils.LSRNUtils;
import ca.wilkinsonlab.sadi.utils.blast.AbstractBLASTParser;
import ca.wilkinsonlab.sadi.utils.blast.SequenceWriter;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

@URI("http://tomcat.dev.biordf.net/sadi-blast/snapdragon")
@Name("antirrhinum.net BLAST")
@Description("Issues a BLAST query against antirrhinum.net.")
@ContactEmail("mccarthy@elmonline.ca")
@InputClass("http://semanticscience.org/resource/SIO_010018") // DNA sequence
@OutputClass("http://sadiframework.org/examples/blast/ncbi-blast.owl#SnapdragonBLASTedSequence")
//@TestCases({
//		@TestCase(
//				input = "http://sadiframework.org/examples/blast/t/dragon.input.1.rdf", 
//				output = "http://sadiframework.org/examples/blast/t/dragon.output.1.rdf"
//		),
//		@TestCase(
//				input = "http://sadiframework.org/examples/blast/t/dragon.input.2.rdf", 
//				output = "http://sadiframework.org/examples/blast/t/dragon.output.2.rdf"
//		)
//})
public class DragonBLASTServiceServlet extends AsynchronousServiceServlet
{
	private static final Logger log = Logger.getLogger(DragonBLASTServiceServlet.class);
	private static final long serialVersionUID = 1L;

	private static final BLASTParser parser = new BLASTParser();
	
//	@Override
//	public int getInputBatchSize()
//	{
//		return super.getInputBatchSize();
//	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ServiceServlet#createOutputModel()
	 */
	@Override
	protected Model createOutputModel()
	{
		Model model = super.createOutputModel();
		model.setNsPrefix("ncbi-blast", "http://sadiframework.org/examples/blast-uniprot.owl#");
		model.setNsPrefix("blast", "http://sadiframework.org/ontologies/blast.owl#");
		model.setNsPrefix("sio", "http://semanticscience.org/resource/");
		return model;
	}

	@Override
	protected void processInputBatch(ServiceCall call)
	{
		// TODO deal with parameters...
		//Resource parameters = call.getParameters();
		
		try {
			Process p = Runtime.getRuntime().exec(new String[]{
//					"ssh", "dev.biordf.net",
					"blastall",
					"-p", "blastn",
					"-d", "/var/www/BLAST/db/Sept_2007.dna",
					"-e", "10e-10",
					"-m", "7"
			});
			SequenceWriter writer = new SequenceWriter(p.getOutputStream());
			for (Resource inputNode: call.getInputNodes()) {
				String id = inputNode.getURI();
				String sequence = inputNode.getRequiredProperty(SIO.has_value).getLiteral().getLexicalForm();
				writer.addSequence(id, sequence);
			}
			new Thread(writer).start();
			InputStream is = p.getInputStream();
			if (log.isTraceEnabled()) {
				String s = StreamUtils.readStream(is);
				log.trace(String.format("read BLAST report:\n%s", s));
				is = new ByteArrayInputStream(s.getBytes());
			}
			parser.parseBLAST(call.getOutputModel(), is);
		} catch (Exception e) {
			// TODO change the hierarchy so I can throw a real exception here...
			log.error(e.toString(), e);
			throw new RuntimeException("error running BLAST", e);
		}
	}
	
	public static class BLASTParser extends AbstractBLASTParser
	{
		@Override
		protected Resource getQuerySequence(Model model, Node iteration)
		{
			String query_def = getSingleValue(iteration, "Iteration_query-def");
			if (query_def == null) {
				query_def = getSingleValue(iteration.getParentNode().getParentNode(), "BlastOutput_query-def");
			}
			/* reverse the URI encoding performed above...
			 */
			String uri;
			try {
				uri = URLDecoder.decode(query_def, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// UTF-8 is unsupported? really?
				uri = query_def;
			}
			return model.getResource(uri);
		}

		@Override
		protected Resource getHitSequence(Model model, Node hit)
		{
			String acc = getSingleValue(hit, "Hit_accession");
			String uri = LSRNUtils.getURI("DragonDB_DNA", acc);
			Resource seq = model.getResource(uri);
			if (!seq.hasProperty(RDF.type)) { // first time; create it...
				seq = LSRNUtils.createInstance(model, 
						model.getResource(LSRNUtils.getClassURI("DragonDB_DNA")), acc);
				seq.addProperty(fromOrganism, getOrganism(model));
			}
			return seq;
		}
		
		private Resource getOrganism(Model model)
		{
			Resource org = model.getResource(LSRNUtils.getURI("taxon", "4151"));
			if (!org.hasProperty(RDF.type)) { // first time; create it...
				org = LSRNUtils.createInstance(model, 
						model.getResource(LSRNUtils.getClassURI("taxon")), "4151");
			}
			return org;
		}
		
		private static final Property fromOrganism = ResourceFactory.createProperty("http://sadiframework.org/ontologies/properties.owl#fromOrganism");
	}
}
