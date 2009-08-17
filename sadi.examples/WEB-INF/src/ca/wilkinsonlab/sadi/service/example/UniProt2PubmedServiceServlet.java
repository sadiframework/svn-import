package ca.wilkinsonlab.sadi.service.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.Citation;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtAccession;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.Author;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.AuthoringGroup;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.JournalArticle;
import uk.ac.ebi.kraken.interfaces.uniprot.citations.PubMedId;
import uk.ac.ebi.kraken.uuw.services.remoting.Query;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryBuilder;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;
import ca.elmonline.util.BatchIterator;
import ca.wilkinsonlab.sadi.service.ServiceServlet;
import ca.wilkinsonlab.sadi.tasks.Task;
import ca.wilkinsonlab.sadi.tasks.TaskManager;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class UniProt2PubmedServiceServlet extends ServiceServlet
{
	private static final Log log = LogFactory.getLog(UniProt2PubmedServiceServlet.class);
	
	private static final String UNIPROT_PREFIX = "http://purl.uniprot.org/uniprot/";
	private static final String PUBMED_PREFIX = "http://biordf.net/moby/PMID/";
	
	private final Property hasReference;
	private final Resource PubMed_Record;
	
	public UniProt2PubmedServiceServlet()
	{
		super();

		hasReference = ontologyModel.getProperty("http://ontology.dumontierlab.com/hasReference");
		PubMed_Record = ontologyModel.createResource("http://purl.oclc.org/SADI/LSRN/PubMed_Record");
	}
	
	@Override
	public void processInput(Map<Resource, Resource> inputOutputMap)
	{
		/* break up the input into batches of 1024 ids as required by
		 * the UniProt API and send off those requests...
		 */
		Collection<UniProt2PubMedTask> tasks = new ArrayList<UniProt2PubMedTask>();
		for (Collection<Resource> batch: BatchIterator.batches(inputOutputMap.keySet(), 1024)) {
			Collection<String> ids = new ArrayList<String>(batch.size());
			for (Resource uniprot: batch) {
				String uniprotId = getUniprotId(uniprot);
				log.trace(String.format("extracted ID %s from %s", uniprotId, uniprot));
				ids.add(uniprotId);
			}

			log.debug(String.format("starting new task with %d ids", ids.size()));
			UniProt2PubMedTask task = new UniProt2PubMedTask(ids);
			TaskManager.getInstance().startTask(task);
			tasks.add(task);
		}
		
		/* wait for the tasks to finish; since this is a synchronous
		 * service, we have to wait for all of them anyway...
		 */
		while (!tasks.isEmpty()) {
			Collection<UniProt2PubMedTask> finishedTasks = new ArrayList<UniProt2PubMedTask>(tasks.size());
			
			for (UniProt2PubMedTask task: tasks) {
				if (task.isFinished()) {
					log.debug("adding go terms from task " + task);
					for (Resource input: inputOutputMap.keySet()) {
						List<Citation> cites = task.results.get(getUniprotId(input));
						if (cites != null) {
							Resource output = inputOutputMap.get(input);
							for (Citation cite: cites) {
								if (cite instanceof JournalArticle)
									attachCitation(output, (JournalArticle)cite);
							}
						}
					}
					TaskManager.getInstance().disposeTask(task.getId());
					finishedTasks.add(task);
				}
			}
			
			tasks.removeAll(finishedTasks);
		}
	}
	
	private void attachCitation(Resource uniprotNode, JournalArticle article)
	{
		PubMedId pmId = article.getPubMedId();
		if (pmId == null)
			return;
		
		Resource goNode = uniprotNode.getModel().createResource(getPubMedUri(pmId), PubMed_Record);
		goNode.addProperty(RDFS.label, getPubMedLabel(article));
		uniprotNode.addProperty(hasReference, goNode);
	}
	
	private static String getPubMedUri(PubMedId pmId)
	{
		String pdbId = pmId.getValue();
		return String.format("%s%s", PUBMED_PREFIX, pdbId);
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

	private static String getUniprotId(Resource uniprotNode)
	{
		String uri = uniprotNode.getURI();
		if (uri.startsWith(UNIPROT_PREFIX)) {
			return uri.substring(UNIPROT_PREFIX.length());
		} else {
			// best guess...
			return StringUtils.substringAfterLast(uri, "/");
		}
	}
	
	private static class UniProt2PubMedTask extends Task
	{
		Collection<String> uniprotIds;
		Map<String, List<Citation>> results;
		
		public UniProt2PubMedTask(Collection<String> uniprotIds)
		{
			this.uniprotIds = uniprotIds;
			
			results = new HashMap<String, List<Citation>>(uniprotIds.size());
		}
		
		public void run()
		{
			log.debug("calling UniProt query service");
			UniProtQueryService uniProtQueryService = UniProtJAPI.factory.getUniProtQueryService();
			log.trace("building UniProt query");
			Query query = UniProtQueryBuilder.buildIDListQuery((List<String>)uniprotIds);
			log.trace("executing UniProt query");
			for (UniProtEntry entry : uniProtQueryService.getEntryIterator(query)) {
				results.put(entry.getPrimaryUniProtAccession().getValue(), entry.getCitations());
				for (UniProtAccession acc: entry.getSecondaryUniProtAccessions())
					results.put(acc.getValue(), entry.getCitations());
			}
			success();
		}
	}
}
