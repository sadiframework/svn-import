package org.sadiframework.service.example;

import java.util.Collections;

import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.Description;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.service.annotations.URI;
import org.sadiframework.utils.SIOUtils;
import org.sadiframework.utils.UniProtUtils;
import org.sadiframework.vocab.SIO;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

import com.hp.hpl.jena.rdf.model.Resource;

@URI("http://sadiframework.org/examples/blastUniprotById")
@Name("UniProt BLAST by UniProt ID")
@Description("Issues a BLAST query against the UniProt database, using the \"canonical\" sequence of the input UniProt ID. Uses BLASTP, similarity matrix BLOSUM_62, and an expect threshold of 10. A maximum 500 BLAST hits are returned, if the expectation cutoff is not reached. All organisms are included in the search.")
@ContactEmail("info@sadiframework.org")
@InputClass("http://purl.oclc.org/SADI/LSRN/UniProt_Record")
@OutputClass("http://sadiframework.org/examples/blast-uniprot.owl#BlastByIDOutputClass")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/blastUniprotById.input.1.rdf",
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
