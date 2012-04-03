package ca.wilkinsonlab.sadi.service.example;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.ServiceUtils;
import ca.wilkinsonlab.sadi.utils.UniProtUtils;
import ca.wilkinsonlab.sadi.vocab.LSRN;
import ca.wilkinsonlab.sadi.vocab.SIO;
import ca.wilkinsonlab.sadi.vocab.LSRN.LSRNRecordType;

import com.hp.hpl.jena.rdf.model.Resource;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/uniprot2gene.input.1.rdf",
				output = "http://sadiframework.org/examples/t/uniprot2gene.output.1.rdf"
		)
)
public class UniProt2GeneServiceServlet extends UniProtServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2GeneServiceServlet.class);

	private static Set<LSRNRecordType> lsrnGeneRecordTypes;
	static {
		Set<LSRNRecordType> set = new HashSet<LSRNRecordType>();
		set.add(LSRN.Ensembl);
		set.add(LSRN.FlyBase);
		set.add(LSRN.Entrez.Gene);
		set.add(LSRN.HGNC);
		set.add(LSRN.KEGG.Gene);
		set.add(LSRN.MGI);
		set.add(LSRN.RGD);
		set.add(LSRN.SGD);
		set.add(LSRN.ZFIN);
		lsrnGeneRecordTypes = Collections.synchronizedSet(Collections.unmodifiableSet(set));
	}

	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (DatabaseCrossReference xref: input.getDatabaseCrossReferences()) {

			LSRNRecordType lsrnRecordType = UniProtUtils.getLSRNType(xref.getDatabase());
			if (lsrnRecordType == null || !lsrnGeneRecordTypes.contains(lsrnRecordType))
				continue;

			String databaseId = UniProtUtils.getDatabaseId(xref);
			if (databaseId == null)
				continue;

			Resource xrefNode = ServiceUtils.createLSRNRecordNode(output.getModel(), lsrnRecordType, databaseId);

			output.addProperty(SIO.is_encoded_by, xrefNode);

		}
	}

}
