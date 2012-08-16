package org.sadiframework.service.example;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.utils.ServiceUtils;
import org.sadiframework.utils.UniProtUtils;
import org.sadiframework.vocab.LSRN;
import org.sadiframework.vocab.SIO;
import org.sadiframework.vocab.LSRN.LSRNRecordType;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

import com.hp.hpl.jena.rdf.model.Resource;

@ContactEmail("info@sadiframework.org")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/uniprot2EntrezGene.input.1.rdf",
				output = "http://sadiframework.org/examples/t/uniprot2EntrezGene.output.1.rdf"
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
