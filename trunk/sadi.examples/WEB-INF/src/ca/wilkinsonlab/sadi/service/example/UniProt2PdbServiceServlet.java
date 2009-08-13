package ca.wilkinsonlab.sadi.service.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtAccession;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.pdb.Pdb;
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

public class UniProt2PdbServiceServlet extends ServiceServlet
{
	private static final Log log = LogFactory.getLog(UniProt2PdbServiceServlet.class);
	
	private static final String UNIPROT_PREFIX = "http://purl.uniprot.org/uniprot/";
	private static final String PDB_PREFIX = "http://biordf.net/moby/PDB/";
	
	private final Property hasPDBId;
	private final Resource PDB_Record;
	
	public UniProt2PdbServiceServlet()
	{
		super();

		hasPDBId = ontologyModel.getProperty("http://es-01.chibi.ubc.ca/~benv/predicates.owl#has3DStructure");
		PDB_Record = ontologyModel.createResource("http://purl.oclc.org/SADI/LSRN/PDB_Record");
	}
	
	@Override
	public void processInput(Map<Resource, Resource> inputOutputMap)
	{
		/* break up the input into batches of 1024 ids as required by
		 * the UniProt API and send off those requests...
		 */
		Collection<UniProt2GoTask> tasks = new ArrayList<UniProt2GoTask>();
		for (Collection<Resource> batch: BatchIterator.batches(inputOutputMap.keySet(), 1024)) {
			Collection<String> ids = new ArrayList<String>(batch.size());
			for (Resource uniprot: batch) {
				String uniprotId = getUniprotId(uniprot);
				log.trace(String.format("extracted ID %s from %s", uniprotId, uniprot));
				ids.add(uniprotId);
			}

			log.debug(String.format("starting new task with %d ids", ids.size()));
			UniProt2GoTask task = new UniProt2GoTask(ids);
			TaskManager.getInstance().startTask(task);
			tasks.add(task);
		}
		
		/* wait for the tasks to finish; since this is a synchronous
		 * service, we have to wait for all of them anyway...
		 */
		while (!tasks.isEmpty()) {
			Collection<UniProt2GoTask> finishedTasks = new ArrayList<UniProt2GoTask>(tasks.size());
			
			for (UniProt2GoTask task: tasks) {
				if (task.isFinished()) {
					log.debug("adding go terms from task " + task);
					for (Resource input: inputOutputMap.keySet()) {
						List<DatabaseCrossReference> terms = task.results.get(getUniprotId(input));
						if (terms != null) {
							Resource output = inputOutputMap.get(input);
							for (DatabaseCrossReference term: terms)
								attachPdbId(output, (Pdb)term);
						}
					}
					TaskManager.getInstance().disposeTask(task.getId());
					finishedTasks.add(task);
				}
			}
			
			tasks.removeAll(finishedTasks);
		}
	}
	
	private void attachPdbId(Resource uniprotNode, Pdb pdb)
	{
		Resource goNode = uniprotNode.getModel().createResource(getPdbId(pdb), PDB_Record);
		goNode.addProperty(RDFS.label, getPdbLabel(pdb));
		uniprotNode.addProperty(hasPDBId, goNode);
	}
	
	private static String getPdbId(Pdb pdb)
	{
		String pdbId = pdb.getPdbAccessionNumber().getValue();
		return String.format("%s%s", PDB_PREFIX, pdbId);
	}

	private static String getPdbLabel(Pdb pdb)
	{
		return pdb.toString();
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
	
	private static class UniProt2GoTask extends Task
	{
		Collection<String> uniprotIds;
		Map<String, List<DatabaseCrossReference>> results;
		
		public UniProt2GoTask(Collection<String> uniprotIds)
		{
			this.uniprotIds = uniprotIds;
			
			results = new HashMap<String, List<DatabaseCrossReference>>(uniprotIds.size());
		}
		
		public void run()
		{
			log.debug("calling UniProt query service");
			UniProtQueryService uniProtQueryService = UniProtJAPI.factory.getUniProtQueryService();
			log.trace("building UniProt query");
			Query query = UniProtQueryBuilder.buildIDListQuery((List<String>)uniprotIds);
			log.trace("executing UniProt query");
			for (UniProtEntry entry : uniProtQueryService.getEntryIterator(query)) {
				results.put(entry.getPrimaryUniProtAccession().getValue(), entry.getDatabaseCrossReferences(DatabaseType.PDB));
				for (UniProtAccession acc: entry.getSecondaryUniProtAccessions())
					results.put(acc.getValue(), entry.getDatabaseCrossReferences(DatabaseType.PDB));
			}
			success();
		}
	}
}
