package ca.wilkinsonlab.sadi.service.example;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.Citation;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.Author;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.AuthoringGroup;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.JournalArticle;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.PubMedId;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.utils.SIOUtils;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/uniprot2pubmed-input.rdf", 
				output = "http://sadiframework.org/examples/t/uniprot2pubmed.output.1.rdf"
		)
)
@SuppressWarnings("deprecation") // PubMed interface is deprecated?
public class UniProt2PubmedServiceServlet extends UniProtServiceServlet
{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2PubmedServiceServlet.class);
	
	private static final String OLD_PUBMED_PREFIX = "http://biordf.net/moby/PMID/";
	private static final String PUBMED_PREFIX = "http://lsrn.org/PMID:";
	
	private final Resource PubMed_Type = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/PMID_Record");
	private final Resource PubMed_Identifier = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/PMID_Identifier");
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (Citation cite: input.getCitations()) {
			if (cite instanceof JournalArticle) {
				Resource pubmedNode = createPubMedNode(output.getModel(), (JournalArticle)cite);
				if (pubmedNode != null)
					output.addProperty(SIO.has_reference, pubmedNode);
			}
		}
	}
	
	private Resource createPubMedNode(Model model, JournalArticle article)
	{
		PubMedId pmId = article.getPubMedId();
		if (pmId == null || StringUtils.isEmpty(pmId.getValue()))
			return null;
		
		Resource pubmedNode = model.createResource(getPubMedUri(pmId), PubMed_Type);
		
		// add identifier structure...
		SIOUtils.createAttribute(pubmedNode, PubMed_Identifier, pmId.getValue());

		// add label...
		pubmedNode.addProperty(RDFS.label, getLabel(article));
		
		// add relationship to old URI scheme...
		pubmedNode.addProperty(OWL.sameAs, model.createResource(getOldPubMedUri(pmId)));
		
		return pubmedNode;
	}

	private static String getPubMedUri(PubMedId pmId)
	{
		String pdbId = pmId.getValue();
		return String.format("%s%s", PUBMED_PREFIX, pdbId);
	}
	
	private String getOldPubMedUri(PubMedId pmId)
	{
		String pdbId = pmId.getValue();
		return String.format("%s%s", OLD_PUBMED_PREFIX, pdbId);
	}

	private static String getLabel(JournalArticle article)
	{
		return String.format("%s, %s", formatAuthorList(article), article.getTitle());
	}
	
	private static String formatAuthorList(JournalArticle article)
	{
		List<Author> authors = article.getAuthors();
		AuthoringGroup authorGroup = article.getAuthoringGroup();
		if (authors != null && !authors.isEmpty()) {
			Iterator<Author> i = authors.iterator();
			StringBuilder buf = new StringBuilder();
			Author firstAuthor = i.next();
			buf.append(firstAuthor.getValue());
			if (authors.size() > 3) {
				buf.append(" et al");
				return buf.toString();
			}
			while (i.hasNext()) {
				Author author = i.next();
				if (i.hasNext()) {
					buf.append(", ");
					buf.append(author.getValue());
				} else {
					buf.append(" & ");
					buf.append(author.getValue());
				}
			}
			return buf.toString();
		} else if (authorGroup != null) {
			return authorGroup.getValue();
		} else {
			return "unknown author";
		}
	}
}
