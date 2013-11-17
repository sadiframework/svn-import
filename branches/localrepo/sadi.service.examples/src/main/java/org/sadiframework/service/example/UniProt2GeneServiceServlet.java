package org.sadiframework.service.example;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.utils.LSRNUtils;
import org.sadiframework.utils.UniProtUtils;
import org.sadiframework.vocab.LSRN;
import org.sadiframework.vocab.SIO;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

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

	private static Set<Resource> lsrnGeneRecordTypes;
	static {
		Set<Resource> set = new HashSet<Resource>();
		set.add(LSRNUtils.getClass(LSRN.Namespace.ENSEMBL));
		set.add(LSRNUtils.getClass(LSRN.Namespace.FLYBASE));
		set.add(LSRNUtils.getClass(LSRN.Namespace.ENTREZ_GENE));
		set.add(LSRNUtils.getClass(LSRN.Namespace.HGNC));
		set.add(LSRNUtils.getClass(LSRN.Namespace.KEGG_GENE));
		set.add(LSRNUtils.getClass(LSRN.Namespace.MGI));
		set.add(LSRNUtils.getClass(LSRN.Namespace.RGD));
		set.add(LSRNUtils.getClass(LSRN.Namespace.SGD));
		set.add(LSRNUtils.getClass(LSRN.Namespace.ZFIN));
		lsrnGeneRecordTypes = Collections.synchronizedSet(Collections.unmodifiableSet(set));
	}

	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (DatabaseCrossReference xref: input.getDatabaseCrossReferences()) {

			String lsrnNamespace = UniProtUtils.getLSRNNamespace(xref.getDatabase());
			if (lsrnNamespace == null)
				continue;

			Resource type = LSRNUtils.getClass(lsrnNamespace);
			if (!lsrnGeneRecordTypes.contains(type))
				continue;

			String databaseId = UniProtUtils.getDatabaseId(xref);
			if (databaseId == null)
				continue;

			Resource xrefNode = LSRNUtils.createInstance(output.getModel(), type, databaseId);
			output.addProperty(SIO.is_encoded_by, xrefNode);

		}
	}

}
