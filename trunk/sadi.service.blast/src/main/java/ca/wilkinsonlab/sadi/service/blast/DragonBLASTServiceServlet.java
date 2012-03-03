package ca.wilkinsonlab.sadi.service.blast;


import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceCall;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.utils.blast.BLASTUtils;
import ca.wilkinsonlab.sadi.utils.blast.SequenceWriter;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Resource;

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

//	@Override
//	public int getInputBatchSize()
//	{
//		return super.getInputBatchSize();
//	}

	@Override
	protected void processInputBatch(ServiceCall call)
	{
		// TODO deal with parameters...
//		Resource parameters = call.getParameters();
		
		try {
			Process p = Runtime.getRuntime().exec(new String[]{
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
			BLASTUtils.parseBLAST(p.getInputStream(), call.getOutputModel());
		} catch (Exception e) {
			// TODO change the hierarchy so I can throw a real exception here...
			log.error(e.toString(), e);
			throw new RuntimeException("error running BLAST", e);
		}
	}
}
