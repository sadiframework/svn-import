package ca.wilkinsonlab.sadi.service.example;

import java.util.Collections;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.utils.UniProtUtils;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Resource;

@URI("http://sadiframework.org/examples/blastUniprotById")
@Name("UniProt BLAST by UniProt ID")
@Description("Issues a BLAST query against the UniProt database, using the \"canonical\" sequence of the input UniProt ID. Uses BLASTP, similarity matrix BLOSUM_62, and an expect threshold of 10. A maximum 500 BLAST hits are returned, if the expectation cutoff is not reached. All organisms are included in the search.")
@ContactEmail("mccarthy@elmonline.ca")
@InputClass("http://purl.oclc.org/SADI/LSRN/UniProt_Record")
@OutputClass("http://sadiframework.org/examples/blast-uniprot.owl#BlastByIDOutputClass")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/blastUniprotById-input.rdf", 
				output = "http://sadiframework.org/examples/t/blastUniprotById.output.1.rdf"
		)
)
public class BlastUniProtByIdServiceServlet extends BlastUniProtServiceServlet 
{
	private static final long serialVersionUID = 1L;

	@Override
	public void processInput(Resource input, Resource output)
	{
		String uniprotId = UniProtUtils.getUniProtId(input);

		// TODO: This service can be made more efficient by
		// retrieving the UniProt entries for all of the inputs
		// in batch, as is done in UniProtServiceServlet.
		UniProtEntry uniprotEntry = UniProtUtils.getUniProtEntries(Collections.singleton(uniprotId)).get(uniprotId);
		
		String sequence = uniprotEntry.getSequence().getValue();
		Resource sequenceNode = SIOUtils.createAttribute(output, SIO.protein_sequence, sequence);
		super.processInput(sequenceNode, sequenceNode);
	}
}
