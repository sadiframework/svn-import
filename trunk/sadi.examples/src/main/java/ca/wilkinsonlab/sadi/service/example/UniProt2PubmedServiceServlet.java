package ca.wilkinsonlab.sadi.service.example;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.Citation;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.Author;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.AuthoringGroup;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.JournalArticle;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.PubMedId;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

@SuppressWarnings("serial")
public class UniProt2PubmedServiceServlet extends UniProtServiceServlet
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UniProt2PubmedServiceServlet.class);
	
	private static final String OLD_PUBMED_PREFIX = "http://biordf.net/moby/PMID/";
	private static final String PUBMED_PREFIX = "http://lsrn.org/PMID:";
	
	private final Property hasReference = ResourceFactory.createProperty("http://ontology.dumontierlab.com/hasReference");
	private final Resource PubMed_Record = ResourceFactory.createResource("http://purl.oclc.org/SADI/LSRN/PubMed_Record");
	
	public UniProt2PubmedServiceServlet()
	{
		super();
	}
	
	@Override
	public void processInput(UniProtEntry input, Resource output)
	{
		for (Citation cite: input.getCitations()) {
			if (cite instanceof JournalArticle) {
				attachCitation(output, (JournalArticle)cite);
			}
		}
	}
	
	private void attachCitation(Resource uniprotNode, JournalArticle article)
	{
		PubMedId pmId = article.getPubMedId();
		if (pmId == null || pmId.getValue().isEmpty())
			return;
		
		Resource pubmedNode = uniprotNode.getModel().createResource(getPubMedUri(pmId), PubMed_Record);
		pubmedNode.addProperty(OWL.sameAs, uniprotNode.getModel().createResource(getOldPubMedUri(pmId)));
		pubmedNode.addProperty(RDFS.label, getPubMedLabel(article));
		uniprotNode.addProperty(hasReference, pubmedNode);
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

	private static String getPubMedLabel(JournalArticle article)
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
